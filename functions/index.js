/**
 * Cloud Function: sendSuvichar
 * POST body: { text: string, author?: string, key: string }
 * Set secret: firebase functions:config:set admin.key="YOUR_SECRET"
 */
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const express = require("express");
const fetch = require("node-fetch");

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

// ---------- Scheduled YouTube poller for new videos ----------

const YT_CHANNELS = [
  // Only this channel will send new video notifications now
  { id: "UCba78apJ7Rw8crHxVPq9dow", label: "SWAMI SACHCHIDANANDJI_ OFFICIAL" },
];

async function fetchLatestVideos(apiKey, channelId) {
  const url =
    "https://www.googleapis.com/youtube/v3/search" +
    "?part=snippet,id&order=date&type=video&maxResults=5&channelId=" +
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

exports.pollYouTubeNewVideos = functions.pubsub
  .schedule("every 30 minutes")
  .onRun(async () => {
    const cfg = functions.config();
    const apiKey = cfg.youtube && cfg.youtube.key;
    if (!apiKey) {
      console.warn("pollYouTubeNewVideos: youtube.key not set in functions config");
      return null;
    }

    for (const ch of YT_CHANNELS) {
      try {
        const latest = await fetchLatestVideos(apiKey, ch.id);
        if (!latest.length) continue;

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
            const thumbs = (sn.thumbnails) || {};
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

        if (!fresh.length) continue;
        fresh.sort((a, b) => a.publishedAt - b.publishedAt);

        for (const v of fresh) {
          await admin.messaging().send({
            topic: "new_video",
            data: {
              kind: "new_video",
              videoId: v.videoId,
              channelId: ch.id,
              channelLabel: ch.label || "",
              thumbUrl: v.thumbUrl || "",
            },
            android: {
              priority: "high",
              notification: {
                title: "નવો વિડિયો ઉમેરાયો છે",
                body: (ch.label ? ch.label + " — " : "") + (v.title || "નવો વિડિયો"),
              },
            },
          });
        }

        const newest = fresh[fresh.length - 1];
        await docRef.set(
          {
            lastPublishedAt: new Date(newest.publishedAt).toISOString(),
            lastVideoId: newest.videoId,
          },
          { merge: true }
        );
      } catch (e) {
        console.error("pollYouTubeNewVideos error for channel", ch.id, e);
      }
    }

    return null;
  });
