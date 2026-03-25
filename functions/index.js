/**
 * Cloud Function: sendSuvichar
 * POST body: { text: string, author?: string, key: string }
 * Set secret: firebase functions:config:set admin.key="YOUR_SECRET"
 */
const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");
const express = require("express");
const fetch = require("node-fetch");
const axios = require("axios");
const crypto = require("crypto");

// Firestore + optional Firebase Storage for permanent audio hosting.
admin.initializeApp();
const db = admin.firestore();

const app = express();
app.use(express.json());

app.use((req, res, next) => {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  res.set("Access-Control-Allow-Headers", "Content-Type");
  if (req.method === "OPTIONS") {
    res.status(204).send("");
    return;
  }
  next();
});

// POST: save latest suvichar and send notification to topic "suvichar"
app.post("/", async (req, res) => {
  const body = req.body || {};
  const expectedKey = functions.config().admin && functions.config().admin.key;
  if (expectedKey && body.key !== expectedKey) {
    res.status(403).json({ ok: false, error: "Invalid key" });
    return;
  }
  const text = body.text ? String(body.text).trim() : "";
  if (!text) {
    res.status(400).json({ ok: false, error: "text required" });
    return;
  }
  const author = body.author ? String(body.author).trim() : "";
  try {
    // Send FCM notification
    await admin.messaging().send({
      topic: "suvichar",
      data: { text, author: author || "", kind: "suvichar" },
      android: {
        priority: "high",
        notification: {
          title: "આજનું ચિંતન",
          // FCM notification body has size limit – keep it short,
          // full text is sent via data.text and shown by the app.
          body: text.length > 240 ? text.slice(0, 237) + "…" : text,
        },
      },
    });

    // Store latest suvichar for app to read
    await db.collection("suvichar")
      .doc("current")
      .set(
        {
          text,
          author: author || "",
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        },
        { merge: true }
      );

    res.status(200).json({ ok: true, message: "Notification + store OK" });
  } catch (e) {
    console.error("FCM send failed", e);
    res.status(500).json({ ok: false, error: (e && e.message) || "Send failed" });
  }
});

// GET: return latest suvichar as JSON (GitHub-style schema)
app.get("/", async (req, res) => {
  try {
    const snap = await db.collection("suvichar").doc("current").get();
    if (!snap.exists) {
      res.json({ suvicharEnabled: false, suvichar: [] });
      return;
    }
    const data = snap.data() || {};
    const text = (data.text || "").trim();
    const author = (data.author || "").trim();
    const enabled = text.length > 0;
    if (!enabled) {
      res.json({ suvicharEnabled: false, suvichar: [] });
      return;
    }
    res.json({
      suvicharEnabled: true,
      suvichar: [{ text, author }],
    });
  } catch (e) {
    console.error("GET / suvichar failed", e);
    res.status(500).json({ suvicharEnabled: false, suvichar: [] });
  }
});

app.all("*", (req, res) => {
  res.status(405).json({ ok: false, error: "Use GET or POST" });
});

exports.sendSuvichar = functions.https.onRequest(app);

// ---------- Scheduled YouTube poller for new videos (server-side FCM, app open OR closed) ----------
// Same @handles as Android VideosFragment (Aacharya channel excluded on purpose).
const DEFAULT_YOUTUBE_HANDLES = [
  "Sachchidanand-Dantali",
  "swamisachchidanandji",
  "SwamiSachchidanand",
];

/** Optional override: firebase functions:config:set youtube.handles="handle1,handle2" */
function handlesToPoll(cfg) {
  const custom = cfg.youtube && cfg.youtube.handles;
  if (custom && String(custom).trim()) {
    return String(custom)
      .split(",")
      .map((s) => s.trim())
      .filter(Boolean);
  }
  return DEFAULT_YOUTUBE_HANDLES;
}

async function resolveHandleToChannelId(apiKey, handle) {
  const h = String(handle || "").trim();
  if (!h) return null;
  const forHandle = h.startsWith("@") ? h.slice(1) : h;
  const url =
    "https://www.googleapis.com/youtube/v3/channels" +
    "?part=id&forHandle=" +
    encodeURIComponent(forHandle) +
    "&key=" +
    encodeURIComponent(apiKey);
  const res = await fetch(url);
  if (!res.ok) {
    console.warn("resolveHandleToChannelId failed", forHandle, res.status);
    return null;
  }
  const json = await res.json();
  const id = json.items && json.items[0] && json.items[0].id;
  return id || null;
}

async function fetchLatestVideos(apiKey, channelId) {
  const url =
    "https://www.googleapis.com/youtube/v3/search" +
    "?part=snippet,id&order=date&type=video&maxResults=8&channelId=" +
    encodeURIComponent(channelId) +
    "&key=" +
    encodeURIComponent(apiKey);
  const res = await fetch(url);
  if (!res.ok) {
    throw new Error("YouTube API error " + res.status);
  }
  const json = await res.json();
  return Array.isArray(json.items) ? json.items : [];
}

function mapVideoForFeed(item, channelIndex) {
  const id = item && item.id && item.id.videoId ? String(item.id.videoId) : "";
  if (!id) return null;
  const sn = item.snippet || {};
  const thumbs = sn.thumbnails || {};
  const thumbUrl =
    (thumbs.high && thumbs.high.url) ||
    (thumbs.medium && thumbs.medium.url) ||
    (thumbs.default && thumbs.default.url) ||
    "https://img.youtube.com/vi/" + id + "/hqdefault.jpg";
  return {
    videoId: id,
    title: sn.title || "",
    publishedAt: sn.publishedAt || "",
    thumbnailUrl: thumbUrl || "",
    channelIndex: Number.isFinite(channelIndex) ? channelIndex : -1,
    durationSeconds: -1,
    viewCount: -1,
  };
}

/**
 * Runs all channel polls, merges yt_feed/latest for the app, sends FCM topic "new_video" for new uploads.
 * Works when app is killed (system tray) or foreground (MyFirebaseService + data payload).
 */
async function runYouTubePollOnce() {
  const cfg = functions.config();
  const apiKey = cfg.youtube && cfg.youtube.key;
  if (!apiKey) {
    const msg = "youtube.key not set — run: firebase functions:config:set youtube.key=\"YOUR_YT_DATA_API_KEY\"";
    console.warn("runYouTubePollOnce:", msg);
    return { ok: false, error: msg };
  }

  const handles = handlesToPoll(cfg);
  const resolved = [];
  for (const h of handles) {
    const id = await resolveHandleToChannelId(apiKey, h);
    if (id) resolved.push({ id, label: h });
    else console.warn("runYouTubePollOnce: could not resolve handle", h);
  }

  if (!resolved.length) {
    return { ok: false, error: "No channel IDs resolved from handles" };
  }

  const mergedById = new Map();
  let notifyCount = 0;

  for (let idx = 0; idx < resolved.length; idx++) {
    const ch = resolved[idx];
    try {
      const latest = await fetchLatestVideos(apiKey, ch.id);
      if (!latest.length) continue;

      for (const item of latest) {
        const mapped = mapVideoForFeed(item, idx);
        if (mapped && !mergedById.has(mapped.videoId)) {
          mergedById.set(mapped.videoId, mapped);
        }
      }

      const docRef = db.collection("yt_channels").doc(ch.id);
      const snap = await docRef.get();
      let lastTs = 0;
      if (snap.exists) {
        const data = snap.data() || {};
        if (data.lastPublishedAt) {
          lastTs = new Date(data.lastPublishedAt).getTime();
        }
      }

      const fresh = [];
      for (const item of latest) {
        const id = item.id && item.id.videoId;
        const sn = item.snippet || {};
        const publishedAt = sn.publishedAt ? Date.parse(sn.publishedAt) : 0;
        if (!id || !publishedAt) continue;
        if (publishedAt > lastTs) {
          const thumbs = sn.thumbnails || {};
          const high = thumbs.high && thumbs.high.url;
          const medium = thumbs.medium && thumbs.medium.url;
          const def = thumbs.default && thumbs.default.url;
          const thumbUrl = high || medium || def || "";
          fresh.push({
            videoId: id,
            title: sn.title || "",
            publishedAt,
            thumbUrl,
          });
        }
      }

      if (fresh.length) {
        fresh.sort((a, b) => a.publishedAt - b.publishedAt);
        const notifTitle = "નવો વિડિયો ઉમેરાયો છે";

        for (const v of fresh) {
          const bodyLine =
            (ch.label ? ch.label + " — " : "") + (v.title || "નવો વિડિયો");
          const textForData = (v.title || "નવું વિડિયો").slice(0, 240);

          await admin.messaging().send({
            topic: "new_video",
            data: {
              kind: "new_video",
              title: notifTitle,
              text: textForData,
              videoId: v.videoId,
              channelId: ch.id,
              channelLabel: ch.label || "",
              thumbUrl: v.thumbUrl || "",
            },
            android: {
              priority: "high",
              notification: {
                title: notifTitle,
                body: bodyLine.slice(0, 200),
              },
            },
          });
          notifyCount += 1;
        }

        const newest = fresh[fresh.length - 1];
        await docRef.set(
          {
            lastPublishedAt: new Date(newest.publishedAt).toISOString(),
            lastVideoId: newest.videoId,
          },
          { merge: true }
        );
      }
    } catch (e) {
      console.error("runYouTubePollOnce error for channel", ch.id, e);
    }
  }

  try {
    const mergedList = Array.from(mergedById.values());
    mergedList.sort((a, b) =>
      String(b.publishedAt || "").localeCompare(String(a.publishedAt || ""))
    );
    const top = mergedList.slice(0, 50);
    if (top.length) {
      await db.collection("yt_feed").doc("latest").set(
        {
          source: "youtube_api",
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          videos: top,
          channelCount: resolved.length,
        },
        { merge: true }
      );
    }
  } catch (e) {
    console.error("yt_feed merge write failed", e);
  }

  return { ok: true, channels: resolved.length, fcmNotifications: notifyCount };
}

exports.pollYouTubeNewVideos = functions.pubsub
  .schedule("every 30 minutes")
  .timeZone("Asia/Kolkata")
  .onRun(async () => {
    await runYouTubePollOnce();
    return null;
  });

/** Manual run (test / force sync). GET or POST ?key=ADMIN_KEY or body.key */
exports.triggerYouTubePollNow = functions.https.onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  res.set("Access-Control-Allow-Headers", "Content-Type");
  if (req.method === "OPTIONS") {
    res.status(204).send("");
    return;
  }
  try {
    const expectedKey = functions.config().admin && functions.config().admin.key;
    const qk = req.query && req.query.key;
    const bk = req.body && req.body.key;
    const key = qk || bk;
    if (expectedKey && key !== expectedKey) {
      res.status(403).json({ ok: false, error: "Invalid key" });
      return;
    }
    const out = await runYouTubePollOnce();
    res.status(200).json(out);
  } catch (e) {
    console.error("triggerYouTubePollNow", e);
    res.status(500).json({ ok: false, error: (e && e.message) || "error" });
  }
});

// ----------------- Telegram Audio Pravachan webhook -----------------
// NOTE: Bot token directly in code so Cloud Functions runtime always has it.
// If you rotate the bot token, update this value and redeploy.
const BOT_TOKEN = "8752969355:AAGuOLnNWl-NGbsbh7BfmvFvGKj7Bsyi-Y4";
const TELEGRAM_API = `https://api.telegram.org/bot${BOT_TOKEN}`;
const FUNCTION_REGION = "us-central1";

function sanitizeFileName(name, fallback) {
  const raw = String(name || "").trim();
  const safe = raw.replace(/[^\w.\-() ]+/g, "_").replace(/\s+/g, " ").trim();
  if (safe) return safe;
  return fallback || "pravachan.mp3";
}

function extFromMime(mime) {
  const m = String(mime || "").toLowerCase();
  if (m.includes("mpeg") || m.includes("mp3")) return ".mp3";
  if (m.includes("mp4") || m.includes("m4a")) return ".m4a";
  if (m.includes("wav")) return ".wav";
  if (m.includes("ogg")) return ".ogg";
  if (m.includes("aac")) return ".aac";
  return ".mp3";
}

async function uploadTelegramAudioToStorage({ filePath, fileId, fileName, mimeType }) {
  if (!filePath || !fileId) throw new Error("uploadTelegramAudioToStorage missing filePath/fileId");
  const bucket = admin.storage().bucket(getPravachanStorageBucketName());
  const ext = extFromMime(mimeType);
  const fallbackName = `audio_${fileId}${ext}`;
  const safeName = sanitizeFileName(fileName, fallbackName);
  const objectPath = `pravachan/${fileId}/${safeName}`;
  const token = crypto.randomUUID();
  const tgFileUrl = `https://api.telegram.org/file/bot${BOT_TOKEN}/${filePath}`;

  const storageFile = bucket.file(objectPath);
  const fileResp = await axios.get(tgFileUrl, {
    responseType: "stream",
    timeout: 15 * 60 * 1000,
    maxBodyLength: Infinity,
    maxContentLength: Infinity,
    validateStatus: () => true,
  });
  if ((fileResp.status || 500) >= 400) {
    throw new Error(`telegram_download_failed_${fileResp.status || 500}`);
  }

  await new Promise((resolve, reject) => {
    const write = storageFile.createWriteStream({
      resumable: true,
      metadata: {
        contentType: mimeType || guessAudioMimeFromPath(filePath),
        cacheControl: "public,max-age=31536000,immutable",
        metadata: {
          firebaseStorageDownloadTokens: token,
          telegramFileId: String(fileId),
          telegramFilePath: String(filePath),
        },
      },
      validation: false,
    });
    fileResp.data.on("error", reject);
    write.on("error", reject);
    write.on("finish", resolve);
    fileResp.data.pipe(write);
  });

  const encodedPath = encodeURIComponent(objectPath);
  const publicUrl =
    `https://firebasestorage.googleapis.com/v0/b/${bucket.name}/o/${encodedPath}` +
    `?alt=media&token=${token}`;
  return { publicUrl, objectPath };
}

function getPravachanStorageBucketName() {
  const cfg = functions.config();
  const byConfig = cfg && cfg.pravachan && cfg.pravachan.storage_bucket;
  if (byConfig) return String(byConfig).trim();
  const byEnv = process.env.PRAVACHAN_STORAGE_BUCKET;
  if (byEnv) return String(byEnv).trim();
  const projectId = process.env.GCLOUD_PROJECT || "swami-sachidanand";
  return `${projectId}.appspot.com`;
}

function buildPravachanProxyUrl(fileId) {
  const projectId = process.env.GCLOUD_PROJECT || "swami-sachidanand";
  return `https://${FUNCTION_REGION}-${projectId}.cloudfunctions.net/telegramPravachanStream?fid=${encodeURIComponent(fileId)}`;
}

function extractTelegramPathFromUrl(url) {
  const u = String(url || "").trim();
  if (!u) return "";
  const marker = "/file/bot";
  const idx = u.indexOf(marker);
  if (idx < 0) return "";
  const after = u.substring(idx + marker.length);
  const slash = after.indexOf("/");
  if (slash < 0) return "";
  return after.substring(slash + 1).split("?")[0] || "";
}

function guessAudioMimeFromPath(path) {
  const p = String(path || "").toLowerCase();
  if (p.endsWith(".mp3")) return "audio/mpeg";
  if (p.endsWith(".m4a") || p.endsWith(".mp4")) return "audio/mp4";
  if (p.endsWith(".wav")) return "audio/wav";
  if (p.endsWith(".aac")) return "audio/aac";
  if (p.endsWith(".ogg") || p.endsWith(".oga")) return "audio/ogg";
  if (p.endsWith(".opus")) return "audio/opus";
  return "audio/mpeg";
}

function makePravachanDocId(seed) {
  const s = String(seed || `${Date.now()}_${Math.random()}`);
  return crypto.createHash("sha1").update(s).digest("hex").slice(0, 32);
}

function buildFirebaseStorageMediaUrl(bucketName, objectPath, token) {
  const encodedPath = encodeURIComponent(String(objectPath || ""));
  return `https://firebasestorage.googleapis.com/v0/b/${bucketName}/o/${encodedPath}?alt=media&token=${encodeURIComponent(String(token || ""))}`;
}

/** Telegram 429 par thodi der wait karke dubara (album = kai sendMessage ek saath). */
async function sendTelegramMessage(chatId, text, attempt = 0) {
  try {
    await axios.post(`${TELEGRAM_API}/sendMessage`, {
      chat_id: chatId,
      text,
    });
  } catch (e) {
    const status = e && e.response && e.response.status;
    const retryAfter =
      e &&
      e.response &&
      e.response.data &&
      e.response.data.parameters &&
      e.response.data.parameters.retry_after;
    if (status === 429 && attempt < 4) {
      const waitMs = Math.min(60000, (Number(retryAfter) || 2) * 1000 + attempt * 500);
      await new Promise((r) => setTimeout(r, waitMs));
      return sendTelegramMessage(chatId, text, attempt + 1);
    }
    throw e;
  }
}

exports.telegramPravachanWebhook = functions.https.onRequest(async (req, res) => {
  if (!BOT_TOKEN || !TELEGRAM_API) {
    console.warn("telegramPravachanWebhook: BOT_TOKEN not set");
    res.status(200).send("Bot token not configured");
    return;
  }

  try {
    const update = req.body || {};
    const msg = update.message || {};
    const audio = msg.audio || msg.document;

    if (!audio) {
      res.status(200).send("No audio/document, ignored");
      return;
    }

    const mediaGroupId = msg.media_group_id;
    if (mediaGroupId) {
      console.log(
        "telegramPravachanWebhook: media_group_id=",
        mediaGroupId,
        "file_name=",
        audio.file_name || audio.file_id
      );
    }

    const mime = audio.mime_type || "";
    const fileName = audio.file_name || "";
    const lowerName = fileName.toLowerCase();

    // Accept:
    // - Telegram audio (mime starts with audio/)
    // - Document where mime may be generic but filename looks like audio (.mp3/.m4a/.wav)
    const looksLikeAudio =
      (mime && mime.startsWith("audio/")) ||
      lowerName.endsWith(".mp3") ||
      lowerName.endsWith(".m4a") ||
      lowerName.endsWith(".wav");

    if (!looksLikeAudio) {
      res.status(200).send("Not an audio file, ignored");
      return;
    }

    const fileId = audio.file_id;
    const titleRaw = msg.caption || fileName || "Pravachan";
    const title = String(titleRaw).trim() || "Pravachan";

    // 1) Get file path info from Telegram
    const fileInfo = await axios.get(`${TELEGRAM_API}/getFile`, {
      params: { file_id: fileId },
    });
    const filePath =
      fileInfo &&
      fileInfo.data &&
      fileInfo.data.result &&
      fileInfo.data.result.file_path;
    if (!filePath) {
      console.error("telegramPravachanWebhook: missing file_path");
      res.status(200).send("file_path missing");
      return;
    }

    // 2) Prefer permanent Firebase Storage URL; fallback to proxy if storage unavailable.
    let audioUrl = "";
    let source = "telegram_proxy";
    try {
      const uploaded = await uploadTelegramAudioToStorage({
        filePath,
        fileId,
        fileName,
        mimeType: mime || guessAudioMimeFromPath(filePath),
      });
      audioUrl = uploaded.publicUrl;
      source = "firebase_storage";
    } catch (storageErr) {
      console.warn("telegramPravachanWebhook: storage upload failed, using proxy", storageErr && storageErr.message ? storageErr.message : storageErr);
      audioUrl = buildPravachanProxyUrl(fileId);
      source = "telegram_proxy";
    }

    // Use stable document id based on Telegram file_id so duplicates overwrite
    const docId = fileId || audio.file_unique_id || String(Date.now());
    const docRef = db.collection("pravachan").doc(docId);
    const prevSnap = await docRef.get();
    const wasNew = !prevSnap.exists;

    await docRef.set({
      title,
      speaker: "સ્વામી સચ્ચિદાનંદ",
      audioUrl,
      source,
      telegramFileId: fileId || "",
      telegramFilePath: filePath || "",
      durationSec: 0,
      tags: ["pravachan"],
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });

    // Send notification like "new_video" notifications:
    // App subscribes to topic "new_audio" and routes kind != "suvichar" to ContentUpdateNotificationHelper.
    if (wasNew) {
      try {
        await admin.messaging().send({
          topic: "new_audio",
          data: {
            kind: "new_pravachan",
            title: "પ્રવચન ઉમેરાયું છે",
            audioId: docId,
            text: title || "પ્રવચન",
            thumbUrl: "",
          },
          android: {
            priority: "high",
            // data-only message: app will show the notification via MyFirebaseService
          },
        });
      } catch (e) {
        console.error("telegramPravachanWebhook: FCM new_audio send failed", e);
      }
    }

    if (msg.chat && msg.chat.id) {
      try {
        await sendTelegramMessage(
          msg.chat.id,
          `પ્રવચન મળ્યું અને સેઇવ થયું:\n${title}`
        );
      } catch (e) {
        console.warn("telegramPravachanWebhook: sendMessage failed after retries", e);
      }
    }

    res.status(200).send("OK");
  } catch (e) {
    console.error("telegramPravachanWebhook error", e);
    res.status(200).send("Error");
  }
});

/**
 * Manual upload flow (Telegram bypass):
 * 1) Call createPravachanUploadUrl -> receive signed PUT URL + objectPath
 * 2) Upload audio bytes directly to signed URL
 * 3) Call finalizePravachanUpload to create/update Firestore pravachan doc
 */
exports.createPravachanUploadUrl = functions.https.onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  res.set("Access-Control-Allow-Headers", "Content-Type");
  if (req.method === "OPTIONS") {
    res.status(204).send("");
    return;
  }
  try {
    const expectedKey = functions.config().admin && functions.config().admin.key;
    const qk = req.query && req.query.key;
    const bk = req.body && req.body.key;
    const key = qk || bk;
    if (expectedKey && key !== expectedKey) {
      res.status(403).json({ ok: false, error: "Invalid key" });
      return;
    }

    const rawName = (req.body && req.body.fileName) || (req.query && req.query.fileName) || "pravachan.mp3";
    const mimeType = String((req.body && req.body.mimeType) || (req.query && req.query.mimeType) || "audio/mpeg").trim();
    const titleRaw = (req.body && req.body.title) || (req.query && req.query.title) || rawName;
    const title = String(titleRaw || "").trim() || "Pravachan";
    const safeName = sanitizeFileName(rawName, `pravachan_${Date.now()}${extFromMime(mimeType)}`);
    const bucketName = getPravachanStorageBucketName();
    const token = crypto.randomUUID();
    const docId = makePravachanDocId(`${title}_${safeName}_${Date.now()}`);
    const objectPath = `pravachan/manual/${docId}/${safeName}`;
    const bucket = admin.storage().bucket(bucketName);
    const storageFile = bucket.file(objectPath);

    const expiresAt = Date.now() + 30 * 60 * 1000; // 30 min
    const signed = await storageFile.getSignedUrl({
      version: "v4",
      action: "write",
      expires: expiresAt,
      contentType: mimeType || "audio/mpeg",
    });

    await storageFile.setMetadata({
      contentType: mimeType || "audio/mpeg",
      cacheControl: "public,max-age=31536000,immutable",
      metadata: {
        firebaseStorageDownloadTokens: token,
        uploadFlow: "manual_signed_url",
        title,
      },
    });

    const publicUrl = buildFirebaseStorageMediaUrl(bucketName, objectPath, token);
    res.status(200).json({
      ok: true,
      bucket: bucketName,
      objectPath,
      uploadUrl: signed && signed[0] ? signed[0] : "",
      uploadMethod: "PUT",
      uploadContentType: mimeType || "audio/mpeg",
      uploadExpiresAt: new Date(expiresAt).toISOString(),
      suggestedDocId: docId,
      title,
      publicUrl,
    });
  } catch (e) {
    console.error("createPravachanUploadUrl", e);
    res.status(500).json({ ok: false, error: (e && e.message) || "error" });
  }
});

exports.finalizePravachanUpload = functions.https.onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  res.set("Access-Control-Allow-Headers", "Content-Type");
  if (req.method === "OPTIONS") {
    res.status(204).send("");
    return;
  }
  try {
    const expectedKey = functions.config().admin && functions.config().admin.key;
    const qk = req.query && req.query.key;
    const bk = req.body && req.body.key;
    const key = qk || bk;
    if (expectedKey && key !== expectedKey) {
      res.status(403).json({ ok: false, error: "Invalid key" });
      return;
    }

    const objectPath = String((req.body && req.body.objectPath) || (req.query && req.query.objectPath) || "").trim();
    if (!objectPath) {
      res.status(400).json({ ok: false, error: "objectPath required" });
      return;
    }
    const bucketName = getPravachanStorageBucketName();
    const bucket = admin.storage().bucket(bucketName);
    const file = bucket.file(objectPath);
    const exists = await file.exists();
    if (!exists || !exists[0]) {
      res.status(404).json({ ok: false, error: "Uploaded file not found" });
      return;
    }
    const metaArr = await file.getMetadata();
    const meta = (metaArr && metaArr[0]) || {};
    const custom = meta.metadata || {};
    const token = String(custom.firebaseStorageDownloadTokens || "").trim() || crypto.randomUUID();
    if (!custom.firebaseStorageDownloadTokens) {
      await file.setMetadata({
        metadata: {
          ...custom,
          firebaseStorageDownloadTokens: token,
        },
      });
    }

    const docIdRaw = String((req.body && req.body.docId) || (req.query && req.query.docId) || "").trim();
    const titleRaw =
      (req.body && req.body.title) ||
      (req.query && req.query.title) ||
      custom.title ||
      objectPath.split("/").pop() ||
      "Pravachan";
    const title = String(titleRaw || "").trim() || "Pravachan";
    const speakerRaw = (req.body && req.body.speaker) || (req.query && req.query.speaker) || "સ્વામી સચ્ચિદાનંદ";
    const speaker = String(speakerRaw || "").trim() || "સ્વામી સચ્ચિદાનંદ";
    const mime = String(meta.contentType || guessAudioMimeFromPath(objectPath));
    const audioUrl = buildFirebaseStorageMediaUrl(bucketName, objectPath, token);
    const docId = docIdRaw || makePravachanDocId(objectPath);

    await db.collection("pravachan").doc(docId).set({
      title,
      speaker,
      audioUrl,
      source: "firebase_storage",
      storageBucket: bucketName,
      storagePath: objectPath,
      durationSec: 0,
      tags: ["pravachan"],
      mimeType: mime,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });

    try {
      await admin.messaging().send({
        topic: "new_audio",
        data: {
          kind: "new_pravachan",
          title: "પ્રવચન ઉમેરાયું છે",
          audioId: docId,
          text: title || "પ્રવચન",
          thumbUrl: "",
        },
        android: {
          priority: "high",
        },
      });
    } catch (e) {
      console.error("finalizePravachanUpload FCM failed", e);
    }

    res.status(200).json({
      ok: true,
      docId,
      title,
      source: "firebase_storage",
      audioUrl,
      storagePath: objectPath,
      bucket: bucketName,
    });
  } catch (e) {
    console.error("finalizePravachanUpload", e);
    res.status(500).json({ ok: false, error: (e && e.message) || "error" });
  }
});

/**
 * Public GitHub audio stream proxy for audiobook parts.
 * Helps when some mobile networks fail DNS/redirects for github.com release URLs.
 * Usage:
 *   /githubAudioStream?u=<encoded https://github.com/.../releases/download/.../file.mp3>
 */
exports.githubAudioStream = functions.https.onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Cache-Control", "no-store");
  try {
    const raw = String((req.query && req.query.u) || "").trim();
    if (!raw) {
      res.status(400).send("Missing u");
      return;
    }
    let target = raw;
    try {
      target = decodeURIComponent(raw);
    } catch (_) {}
    target = String(target || "").trim();
    const lower = target.toLowerCase();
    const ok =
      lower.startsWith("https://github.com/") ||
      lower.startsWith("http://github.com/") ||
      lower.startsWith("https://objects.githubusercontent.com/") ||
      lower.startsWith("https://github-releases.githubusercontent.com/");
    if (!ok) {
      res.status(400).send("Unsupported host");
      return;
    }

    const headers = {};
    const inRange = req.headers && req.headers.range ? String(req.headers.range) : "";
    if (inRange) headers.Range = inRange;

    const upstream = await axios.get(target, {
      responseType: "stream",
      timeout: 180000,
      headers,
      maxBodyLength: Infinity,
      maxContentLength: Infinity,
      validateStatus: () => true,
    });

    const status = upstream.status || 200;
    if (status >= 400) {
      res.status(status).send("Upstream unavailable");
      return;
    }

    res.status(status);
    const h = upstream.headers || {};
    res.set("Content-Type", String(h["content-type"] || guessAudioMimeFromPath(target)));
    if (h["content-length"]) res.set("Content-Length", String(h["content-length"]));
    if (h["accept-ranges"]) res.set("Accept-Ranges", String(h["accept-ranges"]));
    if (h["content-range"]) res.set("Content-Range", String(h["content-range"]));
    if (h["etag"]) res.set("ETag", String(h["etag"]));
    if (h["last-modified"]) res.set("Last-Modified", String(h["last-modified"]));

    upstream.data.on("error", (err) => {
      console.error("githubAudioStream upstream stream error", err && err.message ? err.message : err);
      try { res.end(); } catch (_) {}
    });
    upstream.data.pipe(res);
  } catch (e) {
    console.error("githubAudioStream", e);
    res.status(502).send("Proxy error");
  }
});

/**
 * Public stream proxy:
 * - Prefer cached telegramFilePath from Firestore (works even when getFile returns "file is too big")
 * - Fallback to Telegram getFile(file_id) when path is missing
 * - Streams audio bytes directly (no 302 redirect), so app player can consume stable mime headers.
 */
exports.telegramPravachanStream = functions.https.onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Cache-Control", "no-store");
  try {
    const fid = String((req.query && req.query.fid) || "").trim();
    if (!fid) {
      res.status(400).send("Missing fid");
      return;
    }
    let filePath = "";
    try {
      const d = await db.collection("pravachan").doc(fid).get();
      if (d.exists) {
        const row = d.data() || {};
        filePath = String(row.telegramFilePath || "").trim();
        if (!filePath) {
          filePath = extractTelegramPathFromUrl(row.audioUrl || "");
        }
      }
    } catch (_) {}

    if (!filePath) {
      const fileInfo = await axios.get(`${TELEGRAM_API}/getFile`, {
        params: { file_id: fid },
        timeout: 30000,
      });
      filePath =
        fileInfo &&
        fileInfo.data &&
        fileInfo.data.result &&
        fileInfo.data.result.file_path;
    }
    if (!filePath) {
      res.status(404).send("File path not found");
      return;
    }
    const url = `https://api.telegram.org/file/bot${BOT_TOKEN}/${filePath}`;

    // Pass-through range header for seeking support in ExoPlayer.
    const upstreamHeaders = {};
    const inRange = req.headers && req.headers.range ? String(req.headers.range) : "";
    if (inRange) upstreamHeaders.Range = inRange;

    const upstream = await axios.get(url, {
      responseType: "stream",
      timeout: 180000,
      headers: upstreamHeaders,
      validateStatus: () => true,
    });

    const status = upstream.status || 200;
    if (status >= 400) {
      res.status(status).send("Upstream file unavailable");
      return;
    }

    // Copy important playback headers.
    const ct = guessAudioMimeFromPath(filePath);
    res.status(status);
    res.set("Content-Type", ct);
    if (upstream.headers["content-length"]) {
      res.set("Content-Length", String(upstream.headers["content-length"]));
    }
    if (upstream.headers["accept-ranges"]) {
      res.set("Accept-Ranges", String(upstream.headers["accept-ranges"]));
    }
    if (upstream.headers["content-range"]) {
      res.set("Content-Range", String(upstream.headers["content-range"]));
    }
    if (upstream.headers["etag"]) {
      res.set("ETag", String(upstream.headers["etag"]));
    }
    if (upstream.headers["last-modified"]) {
      res.set("Last-Modified", String(upstream.headers["last-modified"]));
    }

    upstream.data.on("error", (err) => {
      console.error("telegramPravachanStream upstream stream error", err && err.message ? err.message : err);
      try { res.end(); } catch (_) {}
    });
    upstream.data.pipe(res);
  } catch (e) {
    console.error("telegramPravachanStream", e);
    res.status(502).send("Upstream error");
  }
});

/**
 * One-time repair endpoint: migrates existing pravachan docs to Storage URL (preferred)
 * or stable proxy URL (fallback).
 * Usage:
 *   GET/POST .../repairPravachanAudioUrls?key=ADMIN_KEY&limit=50
 * Optional:
 *   onlyTelegram=1  (default true)
 */
exports.repairPravachanAudioUrls = functions.https.onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  res.set("Access-Control-Allow-Headers", "Content-Type");
  if (req.method === "OPTIONS") {
    res.status(204).send("");
    return;
  }
  try {
    const expectedKey = functions.config().admin && functions.config().admin.key;
    const qk = req.query && req.query.key;
    const bk = req.body && req.body.key;
    const key = qk || bk;
    if (expectedKey && key !== expectedKey) {
      res.status(403).json({ ok: false, error: "Invalid key" });
      return;
    }

    const limitRaw = Number((req.query && req.query.limit) || (req.body && req.body.limit) || 200);
    const limit = Number.isFinite(limitRaw) ? Math.max(1, Math.min(1000, limitRaw)) : 200;
    const onlyTelegram =
      String((req.query && req.query.onlyTelegram) || (req.body && req.body.onlyTelegram) || "1") !== "0";

    const snap = await db.collection("pravachan")
      .orderBy("createdAt", "desc")
      .limit(limit)
      .get();

    let scanned = 0;
    let updated = 0;
    let skipped = 0;
    const failures = [];

    for (const doc of snap.docs) {
      scanned += 1;
      const d = doc.data() || {};
      const oldUrl = String(d.audioUrl || "");
      const isTelegramUrl = oldUrl.includes("api.telegram.org/file/bot");
      if (onlyTelegram && !isTelegramUrl) {
        skipped += 1;
        continue;
      }

      const fileId = String(d.telegramFileId || doc.id || "").trim();
      if (!fileId) {
        skipped += 1;
        continue;
      }

      try {
        let legacyPath = String(d.telegramFilePath || "").trim() || extractTelegramPathFromUrl(oldUrl);
        if (!legacyPath) {
          try {
            const fileInfo = await axios.get(`${TELEGRAM_API}/getFile`, {
              params: { file_id: fileId },
              timeout: 30000,
            });
            legacyPath =
              fileInfo &&
              fileInfo.data &&
              fileInfo.data.result &&
              fileInfo.data.result.file_path;
          } catch (e) {
            // If Telegram cannot return file path (e.g., very large files), keep proxy fallback.
            legacyPath = "";
          }
        }
        let nextAudioUrl = buildPravachanProxyUrl(fileId);
        let nextSource = "telegram_proxy";

        if (legacyPath) {
          try {
            const uploaded = await uploadTelegramAudioToStorage({
              filePath: legacyPath,
              fileId,
              fileName: d.title || `audio_${fileId}`,
              mimeType: guessAudioMimeFromPath(legacyPath),
            });
            nextAudioUrl = uploaded.publicUrl;
            nextSource = "firebase_storage";
          } catch (storageErr) {
            console.warn("repairPravachanAudioUrls storage fallback", doc.id, storageErr && storageErr.message ? storageErr.message : storageErr);
          }
        }

        await doc.ref.set({
          audioUrl: nextAudioUrl,
          source: nextSource,
          telegramFileId: fileId,
          telegramFilePath: legacyPath || "",
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        }, { merge: true });
        updated += 1;
      } catch (e) {
        failures.push({
          id: doc.id,
          reason: (e && e.message) || "unknown",
        });
      }
    }

    res.status(200).json({
      ok: true,
      scanned,
      updated,
      skipped,
      failed: failures.length,
      failures: failures.slice(0, 25),
    });
  } catch (e) {
    console.error("repairPravachanAudioUrls", e);
    res.status(500).json({ ok: false, error: (e && e.message) || "error" });
  }
});

/**
 * Server-only diagnostic: inspect latest pravachan docs and verify audio URL/proxy health.
 * Usage:
 *   GET .../diagnosePravachanUrls?key=ADMIN_KEY&limit=20
 */
exports.diagnosePravachanUrls = functions.https.onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  res.set("Access-Control-Allow-Headers", "Content-Type");
  if (req.method === "OPTIONS") {
    res.status(204).send("");
    return;
  }
  try {
    const expectedKey = functions.config().admin && functions.config().admin.key;
    const qk = req.query && req.query.key;
    const bk = req.body && req.body.key;
    const key = qk || bk;
    if (expectedKey && key !== expectedKey) {
      res.status(403).json({ ok: false, error: "Invalid key" });
      return;
    }

    const limitRaw = Number((req.query && req.query.limit) || (req.body && req.body.limit) || 20);
    const limit = Number.isFinite(limitRaw) ? Math.max(1, Math.min(100, limitRaw)) : 20;
    const projectId = process.env.GCLOUD_PROJECT || "swami-sachidanand";
    const proxyBase = `https://${FUNCTION_REGION}-${projectId}.cloudfunctions.net/telegramPravachanStream?fid=`;

    const snap = await db.collection("pravachan")
      .orderBy("createdAt", "desc")
      .limit(limit)
      .get();

    const rows = [];
    let proxyCount = 0;
    let directTelegramCount = 0;
    let missingUrlCount = 0;

    for (const doc of snap.docs) {
      const d = doc.data() || {};
      const fid = String(d.telegramFileId || doc.id || "").trim();
      const audioUrl = String(d.audioUrl || "").trim();
      const source = String(d.source || "").trim();
      const title = String(d.title || "").trim();
      const isProxy = audioUrl.startsWith(proxyBase);
      const isDirectTelegram = audioUrl.includes("api.telegram.org/file/bot");
      if (isProxy) proxyCount += 1;
      if (isDirectTelegram) directTelegramCount += 1;
      if (!audioUrl) missingUrlCount += 1;

      let probe = { status: null, contentType: "", contentLength: "" };
      if (fid) {
        const proxyUrl = buildPravachanProxyUrl(fid);
        try {
          const r = await axios.get(proxyUrl, {
            timeout: 20000,
            responseType: "stream",
            validateStatus: () => true,
            headers: { Range: "bytes=0-1" },
          });
          probe = {
            status: r.status || null,
            contentType: String((r.headers && r.headers["content-type"]) || ""),
            contentLength: String((r.headers && r.headers["content-length"]) || ""),
          };
          try {
            if (r.data && typeof r.data.destroy === "function") r.data.destroy();
          } catch (_) {}
        } catch (e) {
          probe = {
            status: null,
            contentType: "",
            contentLength: "",
            error: (e && e.message) || "probe_failed",
          };
        }
      }

      rows.push({
        id: doc.id,
        title,
        source,
        telegramFileId: fid,
        hasAudioUrl: !!audioUrl,
        isProxyUrl: isProxy,
        isDirectTelegramUrl: isDirectTelegram,
        audioUrlPreview: audioUrl ? audioUrl.slice(0, 140) : "",
        proxyProbe: probe,
      });
    }

    res.status(200).json({
      ok: true,
      checked: rows.length,
      summary: {
        proxyCount,
        directTelegramCount,
        missingUrlCount,
      },
      rows,
    });
  } catch (e) {
    console.error("diagnosePravachanUrls", e);
    res.status(500).json({ ok: false, error: (e && e.message) || "error" });
  }
});

