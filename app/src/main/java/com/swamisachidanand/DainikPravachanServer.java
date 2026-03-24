package com.swamisachidanand;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * GitHub Pages audio for “દૈનિક પ્રવચન” categories: {@code public/audio_pravachan/&lt;slug&gt;/list.json}.
 */
public final class DainikPravachanServer {
    private DainikPravachanServer() {}

    public static final String BASE =
            "https://daveashish12356-dotcom.github.io/swamisachidanand-audio/public/audio_pravachan/";
    /** Bump when list.json changes (cache bust). */
    private static final String LIST_QUERY = "list.json?v=1";

    /**
     * Map exact {@link DainikPravachanCategories} title to URL-safe folder name under {@code public/audio_pravachan/}.
     */
    @Nullable
    public static String slugForCategoryTitle(@Nullable String title) {
        if (title == null) return null;
        if ("મારા_અનુભવો".equals(title)) {
            return "mara_anubhavo";
        }
        return null;
    }

    @NonNull
    public static String listUrl(@NonNull String slug) {
        return BASE + slug + "/" + LIST_QUERY;
    }

    @NonNull
    public static String folderBaseUrl(@NonNull String slug) {
        String s = slug.endsWith("/") ? slug.substring(0, slug.length() - 1) : slug;
        return BASE + s + "/";
    }

    /**
     * Parse server JSON into playable items. Supports:
     * <ul>
     *   <li>{@code [ {"title":"…","file":"a.mp3"}, … ]}</li>
     *   <li>{@code { "tracks": [ … ] } }</li>
     * </ul>
     * Each object: {@code title} required; {@code file} (relative to folder) or {@code url} (absolute).
     */
    @NonNull
    public static List<PravachanItem> parseTrackList(@NonNull String json, @NonNull String slug)
            throws JSONException {
        List<PravachanItem> out = new ArrayList<>();
        String trimmed = json.trim();
        JSONArray arr;
        if (trimmed.startsWith("{")) {
            JSONObject root = new JSONObject(trimmed);
            if (root.has("tracks")) {
                arr = root.getJSONArray("tracks");
            } else {
                return out;
            }
        } else {
            arr = new JSONArray(trimmed);
        }
        String folder = folderBaseUrl(slug);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            String title = o.optString("title", "").trim();
            String url = o.optString("url", "").trim();
            String file = o.optString("file", "").trim();
            if (title.isEmpty()) continue;
            String audioUrl;
            if (!url.isEmpty()) {
                audioUrl = url;
            } else if (!file.isEmpty()) {
                audioUrl = folder + encodeFileNameForUrl(file);
            } else {
                continue;
            }
            String id = "dainik_" + slug + "_" + i + "_" + (file.isEmpty() ? Integer.toHexString(audioUrl.hashCode()) : file);
            out.add(new PravachanItem(id, title, audioUrl, "", 0L));
        }
        return out;
    }

    /** Encode path segments for URL (spaces etc.). */
    @NonNull
    static String encodeFileNameForUrl(@NonNull String file) {
        if (file.contains("/")) {
            String[] parts = file.split("/");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) sb.append('/');
                sb.append(encodeOneSegment(parts[i]));
            }
            return sb.toString();
        }
        return encodeOneSegment(file);
    }

    @NonNull
    private static String encodeOneSegment(@NonNull String segment) {
        try {
            return URLEncoder.encode(segment, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (Exception e) {
            return segment;
        }
    }
}
