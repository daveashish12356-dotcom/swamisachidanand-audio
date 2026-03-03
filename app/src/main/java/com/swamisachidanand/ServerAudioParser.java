package com.swamisachidanand;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper to parse audio_list_main.json style data into ServerAudioBook / ServerAudioPart.
 * Very small subset just enough for 5-book demo.
 */
public final class ServerAudioParser {

    private ServerAudioParser() {}

    public static List<ServerAudioBook> parseBooks(String json) {
        List<ServerAudioBook> books = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return books;
        try {
            JSONObject root = new JSONObject(json);
            JSONArray arr = root.optJSONArray("books");
            if (arr == null) return books;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj == null) continue;
                String id = obj.optString("id", "");
                String title = obj.optString("title", "");
                String thumb = obj.optString("thumbnailUrl", null);
                JSONArray partsArr = obj.optJSONArray("parts");
                List<ServerAudioPart> parts = new ArrayList<>();
                if (partsArr != null) {
                    for (int j = 0; j < partsArr.length(); j++) {
                        JSONObject p = partsArr.optJSONObject(j);
                        if (p == null) continue;
                        String pid = p.optString("id", "");
                        String ptitle = p.optString("title", "");
                        String url = p.optString("url", "");
                        int dur = p.optInt("duration", p.optInt("durationSeconds", 0));
                        parts.add(new ServerAudioPart(pid, ptitle, url, dur));
                    }
                }
                books.add(new ServerAudioBook(id, title, parts, thumb));
            }
        } catch (Throwable ignored) {
        }
        return books;
    }

    /**
     * Fallback small demo list – 5 audio pustako so that page always shows content even if JSON missing.
     */
    public static List<ServerAudioBook> demoBooks() {
        List<ServerAudioBook> books = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {
            String id = "demo_book_" + i;
            String title = "ડેમો ઓડિયો પુસ્તક " + i;
            List<ServerAudioPart> parts = new ArrayList<>();
            parts.add(new ServerAudioPart("1", "ભાગ 1", ""));
            books.add(new ServerAudioBook(id, title, parts, null));
        }

        return books;
    }
}

