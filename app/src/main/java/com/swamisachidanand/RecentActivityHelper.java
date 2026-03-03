package com.swamisachidanand;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/** Unified recent activity for home history – jo sabse last kiya wo pehle dikhe. */
public final class RecentActivityHelper {

    private static final String PREFS_NAME = "reading_progress";
    private static final String KEY_RECENT_ACTIVITY = "recent_activity_order";
    private static final int MAX_ITEMS = 12;

    public static final int TYPE_BOOK = 0;
    public static final int TYPE_AUDIO = 1;
    public static final int TYPE_VIDEO = 2;

    public static class ActivityEntry {
        public final int type;
        public final String id;

        public ActivityEntry(int type, String id) {
            this.type = type;
            this.id = id != null ? id : "";
        }
    }

    public static void saveActivity(Context context, int type, String id) {
        if (context == null || id == null || id.trim().isEmpty()) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String current = prefs.getString(KEY_RECENT_ACTIVITY, "");
            LinkedList<String> list = new LinkedList<>();
            String entry = type + ":" + id.trim();
            if (!current.isEmpty()) {
                for (String s : current.split(",")) {
                    String t = s.trim();
                    if (!t.isEmpty() && !t.equals(entry)) list.add(t);
                }
            }
            list.addFirst(entry);
            while (list.size() > MAX_ITEMS) list.removeLast();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(list.get(i));
            }
            prefs.edit().putString(KEY_RECENT_ACTIVITY, sb.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static List<ActivityEntry> getRecentActivityOrder(Context context) {
        if (context == null) return new ArrayList<>();
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String s = prefs.getString(KEY_RECENT_ACTIVITY, "");
            if (s.isEmpty()) return new ArrayList<>();
            List<ActivityEntry> out = new ArrayList<>();
            for (String part : s.split(",")) {
                part = part.trim();
                if (part.isEmpty()) continue;
                int colon = part.indexOf(':');
                if (colon < 0) continue;
                try {
                    int type = Integer.parseInt(part.substring(0, colon));
                    String id = part.substring(colon + 1).trim();
                    if (!id.isEmpty() && type >= 0 && type <= 2) out.add(new ActivityEntry(type, id));
                } catch (NumberFormatException ignored) {}
            }
            return out;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
