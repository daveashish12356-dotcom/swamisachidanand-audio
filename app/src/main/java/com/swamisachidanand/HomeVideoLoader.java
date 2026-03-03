package com.swamisachidanand;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Fetches latest 5 videos for Home dashboard.
 * 1) Tries YouTube Data API by channel handle @Sachchidanand-Dantali (if API key set).
 * 2) Fallback: RSS feed by channel_id.
 */
public class HomeVideoLoader {

    private static final String TAG = "HomeVideoLoader";
    /** Channel handle: youtube.com/@Sachchidanand-Dantali */
    private static final String CHANNEL_HANDLE = "Sachchidanand-Dantali";
    private static final String[] CHANNEL_IDS_FALLBACK = {
            "UC8dFOaY-lLRR_hk0U8mXdhA"
    };

    private static final int MAX_VIDEOS = 5;
    private static final int TIMEOUT_SEC = 12;

    public interface Callback {
        void onVideosLoaded(List<HomeVideoItem> videos);
    }

    /** Prefer this: pass context so API can be used to resolve handle and get latest videos. */
    public static void loadLatest(Context context, Callback callback) {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        Handler main = new Handler(Looper.getMainLooper());
        exec.execute(() -> {
            List<HomeVideoItem> result = new ArrayList<>();
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(TIMEOUT_SEC, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(TIMEOUT_SEC, java.util.concurrent.TimeUnit.SECONDS)
                    .build();

            // 1) Try YouTube Data API by handle @Sachchidanand-Dantali
            String apiKey = BuildConfig.YOUTUBE_API_KEY;
            boolean apiKeyOk = apiKey != null && !apiKey.isEmpty() && !apiKey.startsWith("YOUR_");
            if (!apiKeyOk) {
                Log.d(TAG, "YouTube API key not set or placeholder – using RSS/search fallback");
            }
            if (context != null && apiKeyOk) {
                try {
                    List<HomeVideoItem> fromApi = fetchLatestViaApi(context, client, apiKey);
                    if (fromApi == null || fromApi.isEmpty()) {
                        fromApi = fetchLatestByChannelId(context, client, apiKey, CHANNEL_IDS_FALLBACK[0]);
                    }
                    if (fromApi != null && !fromApi.isEmpty()) {
                        final List<HomeVideoItem> toSend = fromApi.size() > MAX_VIDEOS ? fromApi.subList(0, MAX_VIDEOS) : fromApi;
                        Log.d(TAG, "Home videos from API: " + toSend.size());
                        main.post(() -> callback.onVideosLoaded(toSend));
                        return;
                    }
                    Log.d(TAG, "API returned no videos, trying RSS");
                } catch (Throwable t) {
                    Log.w(TAG, "API fetch failed, trying RSS", t);
                }
            }

            // 2) Fallback: RSS by channel_id
            for (String channelId : CHANNEL_IDS_FALLBACK) {
                if (result.size() >= MAX_VIDEOS) break;
                String url = "https://www.youtube.com/feeds/videos.xml?channel_id=" + channelId;
                Request req = new Request.Builder().url(url)
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .addHeader("Accept", "application/atom+xml, application/xml, text/xml, */*")
                        .build();
                try (Response resp = client.newCall(req).execute()) {
                    int code = resp.code();
                    if (!resp.isSuccessful() || resp.body() == null) {
                        Log.d(TAG, "RSS channel " + channelId + " response: " + code);
                        continue;
                    }
                    String xml = resp.body().string();
                    if (xml == null) continue;
                    List<HomeVideoItem> items = parseRss(xml);
                    Log.d(TAG, "RSS channel " + channelId + " parsed: " + items.size() + " videos");
                    for (HomeVideoItem v : items) {
                        if (result.size() >= MAX_VIDEOS) break;
                        boolean dup = false;
                        for (HomeVideoItem e : result) {
                            if (v.videoId != null && v.videoId.equals(e.videoId)) { dup = true; break; }
                        }
                        if (!dup) result.add(v);
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "RSS fetch failed for " + channelId, t);
                }
            }
            List<HomeVideoItem> finalResult = result.size() > MAX_VIDEOS ? result.subList(0, MAX_VIDEOS) : result;
            if (finalResult.isEmpty()) {
                Log.d(TAG, "No videos from API or RSS – HomeFragment will use search fallback");
            }
            main.post(() -> callback.onVideosLoaded(finalResult));
        });
    }

    /** Backward compatible: no context, RSS only. */
    public static void loadLatest(Callback callback) {
        loadLatest(null, callback);
    }

    /** Resolve handle to channel, get uploads playlist, fetch latest videos. */
    private static List<HomeVideoItem> fetchLatestViaApi(Context context, OkHttpClient client, String apiKey) throws Exception {
        String handle = CHANNEL_HANDLE.startsWith("@") ? CHANNEL_HANDLE.substring(1) : CHANNEL_HANDLE;
        String channelUrl = "https://www.googleapis.com/youtube/v3/channels"
                + "?part=id,contentDetails"
                + "&forHandle=" + Uri.encode(handle)
                + "&key=" + apiKey;
        Response r = callWithOptionalAndroidHeaders(context, client, channelUrl);
        if (r == null) return null;
        try {
            int code = r.code();
            if (!r.isSuccessful() || r.body() == null) {
                String errBody = "";
                try { if (r.body() != null) errBody = r.body().string(); } catch (Exception ignored) {}
                Log.d(TAG, "channels.list forHandle response: " + code + " " + errBody);
                return null;
            }
            String body = r.body().string();
            JSONObject root = new JSONObject(body);
            if (root.has("error")) {
                Log.w(TAG, "API error: " + root.optJSONObject("error").optString("message", ""));
                return null;
            }
            org.json.JSONArray items = root.optJSONArray("items");
            if (items == null || items.length() == 0) {
                Log.d(TAG, "channels.list: no channel for handle " + handle);
                return null;
            }
            JSONObject channel = items.getJSONObject(0);
            String channelId = channel.optString("id", null);
            if (channelId == null || channelId.isEmpty()) return null;
            JSONObject contentDetails = channel.optJSONObject("contentDetails");
            if (contentDetails == null) return null;
            org.json.JSONObject relatedPlaylists = contentDetails.optJSONObject("relatedPlaylists");
            if (relatedPlaylists == null) return null;
            String uploadsId = relatedPlaylists.optString("uploads", null);
            if (uploadsId == null || uploadsId.isEmpty()) return null;
            Log.d(TAG, "Channel " + channelId + " uploads playlist: " + uploadsId);

            String playlistUrl = "https://www.googleapis.com/youtube/v3/playlistItems"
                    + "?part=snippet"
                    + "&playlistId=" + Uri.encode(uploadsId)
                    + "&maxResults=" + MAX_VIDEOS
                    + "&key=" + apiKey;
            Response r2 = callWithOptionalAndroidHeaders(context, client, playlistUrl);
            if (r2 == null) return null;
            try {
                if (!r2.isSuccessful() || r2.body() == null) {
                    Log.d(TAG, "playlistItems response: " + r2.code());
                    return null;
                }
                return parsePlaylistItems(r2.body().string());
            } finally {
                r2.close();
            }
        } finally {
            r.close();
        }
    }

    /** Fetch latest videos by channel ID (when forHandle fails). */
    private static List<HomeVideoItem> fetchLatestByChannelId(Context context, OkHttpClient client, String apiKey, String channelId) {
        try {
            String channelUrl = "https://www.googleapis.com/youtube/v3/channels"
                    + "?part=contentDetails"
                    + "&id=" + Uri.encode(channelId)
                    + "&key=" + apiKey;
            Response r = callWithOptionalAndroidHeaders(context, client, channelUrl);
            if (r == null || !r.isSuccessful() || r.body() == null) return null;
            String body = r.body().string();
            r.close();
            JSONObject root = new JSONObject(body);
            org.json.JSONArray items = root.optJSONArray("items");
            if (items == null || items.length() == 0) return null;
            JSONObject contentDetails = items.getJSONObject(0).optJSONObject("contentDetails");
            if (contentDetails == null) return null;
            org.json.JSONObject relatedPlaylists = contentDetails.optJSONObject("relatedPlaylists");
            if (relatedPlaylists == null) return null;
            String uploadsId = relatedPlaylists.optString("uploads", null);
            if (uploadsId == null || uploadsId.isEmpty()) return null;
            String playlistUrl = "https://www.googleapis.com/youtube/v3/playlistItems"
                    + "?part=snippet&playlistId=" + Uri.encode(uploadsId)
                    + "&maxResults=" + MAX_VIDEOS + "&key=" + apiKey;
            Response r2 = callWithOptionalAndroidHeaders(context, client, playlistUrl);
            if (r2 == null || !r2.isSuccessful() || r2.body() == null) return null;
            List<HomeVideoItem> out = parsePlaylistItems(r2.body().string());
            r2.close();
            return out;
        } catch (Throwable t) {
            Log.w(TAG, "fetchLatestByChannelId failed", t);
            return null;
        }
    }

    /** Call API; on 403 retry without Android headers (for server-only API keys). */
    private static Response callWithOptionalAndroidHeaders(Context context, OkHttpClient client, String url) {
        Request.Builder rb = new Request.Builder().url(url);
        if (context != null) {
            rb.addHeader("X-Android-Package", context.getPackageName());
            String cert = getSignatureSha1(context);
            if (cert != null) rb.addHeader("X-Android-Cert", cert);
        }
        try {
            Response r = client.newCall(rb.build()).execute();
            if (r.code() == 403 && context != null) {
                r.close();
                r = client.newCall(new Request.Builder().url(url).build()).execute();
            }
            return r;
        } catch (Exception e) {
            Log.w(TAG, "API call failed: " + url, e);
            return null;
        }
    }

    private static List<HomeVideoItem> parsePlaylistItems(String json) {
        List<HomeVideoItem> out = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(json);
            org.json.JSONArray items = root.optJSONArray("items");
            if (items == null) return out;
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) continue;
                org.json.JSONObject snippet = item.optJSONObject("snippet");
                if (snippet == null) continue;
                org.json.JSONObject resourceId = snippet.optJSONObject("resourceId");
                if (resourceId == null) continue;
                String videoId = resourceId.optString("videoId", null);
                if (videoId == null || videoId.isEmpty()) continue;
                String title = snippet.optString("title", "");
                String publishedAt = snippet.optString("publishedAt", "");
                String thumbUrl = null;
                org.json.JSONObject thumbs = snippet.optJSONObject("thumbnails");
                if (thumbs != null) {
                    if (thumbs.optJSONObject("high") != null) thumbUrl = thumbs.optJSONObject("high").optString("url", null);
                    if ((thumbUrl == null || thumbUrl.isEmpty()) && thumbs.optJSONObject("medium") != null)
                        thumbUrl = thumbs.optJSONObject("medium").optString("url", null);
                }
                if (thumbUrl == null || thumbUrl.isEmpty()) thumbUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
                HomeVideoItem v = new HomeVideoItem();
                v.videoId = videoId;
                v.title = title;
                v.thumbnailUrl = thumbUrl;
                v.publishedAt = publishedAt;
                v.durationSeconds = -1;
                v.viewCount = -1;
                out.add(v);
            }
        } catch (Exception e) {
            Log.w(TAG, "parsePlaylistItems", e);
        }
        return out;
    }

    private static String getSignatureSha1(Context context) {
        try {
            android.content.pm.PackageInfo info = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), android.content.pm.PackageManager.GET_SIGNATURES);
            if (info.signatures != null && info.signatures.length > 0) {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
                md.update(info.signatures[0].toByteArray());
                byte[] digest = md.digest();
                StringBuilder sb = new StringBuilder();
                for (byte b : digest) sb.append(String.format("%02X", b));
                return sb.toString();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static List<HomeVideoItem> parseRss(String xml) {
        List<HomeVideoItem> out = new ArrayList<>();
        int idx = 0;
        while (idx < xml.length()) {
            int start = xml.indexOf("<entry>", idx);
            if (start == -1) break;
            int end = xml.indexOf("</entry>", start);
            if (end == -1) break;
            String entry = xml.substring(start, end);
            idx = end + 8;

            String videoId = extractTag(entry, "yt:videoId");
            if (videoId == null || videoId.isEmpty()) continue;
            String title = extractTag(entry, "title");
            String thumbUrl = extractAttr(entry, "media:thumbnail", "url");
            if (thumbUrl == null || thumbUrl.isEmpty())
                thumbUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";

            HomeVideoItem v = new HomeVideoItem();
            v.videoId = videoId;
            v.title = title != null ? title : "";
            v.thumbnailUrl = thumbUrl;
            v.publishedAt = extractTag(entry, "published");
            v.durationSeconds = -1;
            v.viewCount = -1;
            out.add(v);
        }
        return out;
    }

    private static String extractTag(String src, String tag) {
        if (src == null) return null;
        String open = "<" + tag + ">";
        String close = "</" + tag + ">";
        int start = src.indexOf(open);
        if (start == -1) return null;
        start += open.length();
        int end = src.indexOf(close, start);
        return end == -1 ? null : src.substring(start, end).trim();
    }

    public static String toRelativeTime(String publishedAt) {
        if (publishedAt == null || publishedAt.isEmpty()) return "";
        long now = System.currentTimeMillis();
        long pubMs = 0;
        try {
            if (publishedAt.contains("T") && publishedAt.contains("Z")) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US);
                sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                pubMs = sdf.parse(publishedAt).getTime();
            } else if (publishedAt.matches("\\d+")) {
                pubMs = Long.parseLong(publishedAt) * 1000L;
            } else {
                return publishedAt;
            }
        } catch (Exception e) {
            return publishedAt.replace('T', ' ').replace("Z", "");
        }
        long diff = now - pubMs;
        if (diff < 0) return publishedAt;
        long sec = diff / 1000, min = sec / 60, hr = min / 60, day = hr / 24;
        if (day >= 7) return (day / 7) + " weeks ago";
        if (day >= 2) return day + " days ago";
        if (day == 1) return "Yesterday";
        if (hr >= 1) return hr + " hour" + (hr > 1 ? "s" : "") + " ago";
        if (min >= 1) return min + " minute" + (min > 1 ? "s" : "") + " ago";
        return "Just now";
    }

    public static String formatViewCount(long count) {
        if (count < 0) return "";
        if (count >= 1_000_000) return String.format("%.1fM views", count / 1_000_000.0);
        if (count >= 1_000) return String.format("%.1fK views", count / 1_000.0);
        return count + " views";
    }

    public static String formatDuration(int seconds) {
        if (seconds < 0) return "";
        int h = seconds / 3600, m = (seconds % 3600) / 60, s = seconds % 60;
        if (h > 0) return String.format("%d:%02d:%02d", h, m, s);
        return String.format("%d:%02d", m, s);
    }

    private static String extractAttr(String src, String tag, String attrName) {
        if (src == null) return null;
        int start = src.indexOf("<" + tag);
        if (start == -1) return null;
        int end = src.indexOf(">", start);
        if (end == -1) return null;
        String chunk = src.substring(start, end);
        String key = attrName + "=\"";
        int aStart = chunk.indexOf(key);
        if (aStart == -1) return null;
        aStart += key.length();
        int aEnd = chunk.indexOf("\"", aStart);
        return aEnd == -1 ? null : chunk.substring(aStart, aEnd);
    }

    public static class HomeVideoItem {
        public String videoId;
        public String title;
        public String thumbnailUrl;
        public String publishedAt;
        public int durationSeconds;
        public long viewCount;
    }
}
