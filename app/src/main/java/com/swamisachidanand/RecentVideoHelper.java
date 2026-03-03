package com.swamisachidanand;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/** Saves and returns recently watched video IDs for home history. */
public final class RecentVideoHelper {

    private static final String PREFS_NAME = "reading_progress";
    private static final String KEY_RECENT_VIDEO_IDS = "recent_video_ids";
    private static final int MAX_RECENT = 5;

    public static void saveRecentVideoId(Context context, String videoId) {
        if (context == null || videoId == null || videoId.isEmpty()) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String existing = prefs.getString(KEY_RECENT_VIDEO_IDS, "");
            List<String> list = new LinkedList<>(Arrays.asList(existing.split(",")));
            list.removeIf(id -> videoId.equals(id.trim()));
            list.add(0, videoId);
            while (list.size() > MAX_RECENT) list.remove(list.size() - 1);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(list.get(i).trim());
            }
            prefs.edit().putString(KEY_RECENT_VIDEO_IDS, sb.toString()).apply();
            RecentActivityHelper.saveActivity(context, RecentActivityHelper.TYPE_VIDEO, videoId);
        } catch (Exception ignored) {}
    }

    public static List<String> getRecentVideoIds(Context context) {
        if (context == null) return new ArrayList<>();
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String s = prefs.getString(KEY_RECENT_VIDEO_IDS, "");
            if (s.isEmpty()) return new ArrayList<>();
            List<String> out = new ArrayList<>();
            for (String id : s.split(",")) {
                id = id.trim();
                if (!id.isEmpty()) out.add(id);
            }
            return out;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
