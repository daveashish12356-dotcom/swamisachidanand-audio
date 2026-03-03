package com.swamisachidanand;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Searches YouTube by query – used by SearchResultActivity for global video search.
 */
public class YouTubeSearchLoader {

    private static final String TAG = "YouTubeSearchLoader";
    private static final int MAX_RESULTS = 25;
    private static final int TIMEOUT_SEC = 15;

    public interface Callback {
        void onResults(List<HomeVideoLoader.HomeVideoItem> videos);
    }

    public static void search(Context context, String query, Callback callback) {
        if (query == null || query.trim().isEmpty()) {
            if (callback != null) callback.onResults(new ArrayList<>());
            return;
        }
        ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.execute(() -> {
            try {
                String apiKey = BuildConfig.YOUTUBE_API_KEY;
                if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("YOUR_")) {
                    Log.w(TAG, "YouTube API key not configured");
                    postEmpty(callback);
                    return;
                }
                String url = "https://www.googleapis.com/youtube/v3/search"
                    + "?part=snippet&q=" + Uri.encode(query.trim())
                    + "&order=relevance&type=video&maxResults=" + MAX_RESULTS
                    + "&key=" + apiKey;

                OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(TIMEOUT_SEC, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(TIMEOUT_SEC, java.util.concurrent.TimeUnit.SECONDS)
                    .build();

                Request.Builder rb = new Request.Builder().url(url);
                if (context != null) {
                    rb.addHeader("X-Android-Package", context.getPackageName());
                    String cert = getSignatureSha1(context);
                    if (cert != null) rb.addHeader("X-Android-Cert", cert);
                }
                Response response = client.newCall(rb.build()).execute();
                if (response.code() == 403) {
                    response.close();
                    response = client.newCall(new Request.Builder().url(url)
                        .addHeader("User-Agent", "SwamiSachidanand/1.0").build()).execute();
                }
                List<HomeVideoLoader.HomeVideoItem> out = new ArrayList<>();
                try (Response r = response) {
                    if (!r.isSuccessful() || r.body() == null) {
                        postEmpty(callback);
                        return;
                    }
                    String body = r.body().string();
                    JSONObject root = new JSONObject(body);
                    JSONArray items = root.optJSONArray("items");
                    if (items != null) {
                        for (int i = 0; i < items.length(); i++) {
                            JSONObject item = items.optJSONObject(i);
                            if (item == null) continue;
                            JSONObject idObj = item.optJSONObject("id");
                            if (idObj == null) continue;
                            String videoId = idObj.optString("videoId", null);
                            if (videoId == null || videoId.isEmpty()) continue;
                            JSONObject snippet = item.optJSONObject("snippet");
                            if (snippet == null) continue;
                            String title = snippet.optString("title", "");
                            String publishedAt = snippet.optString("publishedAt", "");
                            String thumbUrl = null;
                            JSONObject thumbs = snippet.optJSONObject("thumbnails");
                            if (thumbs != null) {
                                JSONObject medium = thumbs.optJSONObject("medium");
                                if (medium != null) thumbUrl = medium.optString("url", null);
                                if ((thumbUrl == null || thumbUrl.isEmpty()) && thumbs.optJSONObject("high") != null) {
                                    thumbUrl = thumbs.optJSONObject("high").optString("url", null);
                                }
                            }
                            if (thumbUrl == null || thumbUrl.isEmpty()) {
                                thumbUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
                            }
                            HomeVideoLoader.HomeVideoItem v = new HomeVideoLoader.HomeVideoItem();
                            v.videoId = videoId;
                            v.title = title;
                            v.thumbnailUrl = thumbUrl;
                            v.publishedAt = publishedAt;
                            v.durationSeconds = -1;
                            v.viewCount = -1;
                            out.add(v);
                        }
                    }
                }
                postResults(callback, out);
            } catch (Throwable t) {
                Log.e(TAG, "YouTube search failed", t);
                postEmpty(callback);
            }
        });
    }

    private static void postEmpty(Callback callback) {
        if (callback != null) {
            android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
            h.post(() -> callback.onResults(new ArrayList<>()));
        }
    }

    private static void postResults(Callback callback, List<HomeVideoLoader.HomeVideoItem> list) {
        if (callback != null) {
            android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
            h.post(() -> callback.onResults(list));
        }
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
                for (byte b : digest) {
                    sb.append(String.format("%02X", b));
                }
                return sb.toString();
            }
        } catch (Exception ignored) {}
        return null;
    }
}
