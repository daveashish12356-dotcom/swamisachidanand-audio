package com.swamisachidanand;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Load 56 books from server list (books_server_list.json). Each book has pdfUrl and thumbnailUrl.
 * Used by BooksFragment and HomeFragment so PDFs are not bundled in app.
 */
public final class ServerBookLoader {

    public static List<Book> load(Context context) {
        List<Book> list = new ArrayList<>();
        if (context == null) return list;
        try {
            String baseUrl = context.getString(R.string.server_books_base_url);
            if (baseUrl == null) baseUrl = "";
            baseUrl = baseUrl.trim();
            if (!baseUrl.isEmpty() && !baseUrl.endsWith("/")) baseUrl += "/";
            String booksBase = baseUrl + "books/";
            String thumbBase = baseUrl + "thumbnails/";

            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(context.getAssets().open("books_server_list.json"), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }
            String jsonStr = sb.toString();
            if (jsonStr.startsWith("\uFEFF")) jsonStr = jsonStr.substring(1);
            JSONObject root = new JSONObject(jsonStr);
            JSONArray arr = root.optJSONArray("fileNames");
            if (arr == null) return list;
            for (int i = 0; i < arr.length(); i++) {
                String fileName = arr.optString(i, "").trim();
                if (fileName.isEmpty() || !fileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) continue;
                String displayName = fileName.replace(".pdf", "").replace(".PDF", "");
                String thumbName = fileName.replace(".pdf", ".jpg").replace(".PDF", ".jpg");
                String encodedPdf = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replace("+", "%20");
                String encodedThumb = URLEncoder.encode(thumbName, StandardCharsets.UTF_8.name()).replace("+", "%20");
                String pdfUrl = booksBase + encodedPdf;
                String thumbnailUrl = thumbBase + encodedThumb;
                Book book = new Book(displayName, fileName, 0);
                book.setPdfUrl(pdfUrl);
                book.setThumbnailUrl(thumbnailUrl);
                String year = PdfYearExtractor.extractYearSimple(fileName);
                if (year != null) book.setPublishYear(year);
                book.setCategory(detectCategory(displayName));
                list.add(book);
            }
            android.util.Log.d("ServerBookLoader", "loaded " + list.size() + " books, baseUrl=" + baseUrl);
            if (!list.isEmpty()) {
                Book first = list.get(0);
                android.util.Log.d("ServerBookLoader", "sample thumbnailUrl=" + (first != null ? first.getThumbnailUrl() : ""));
                android.util.Log.d("ServerBookLoader", "sample pdfUrl=" + (first != null ? first.getPdfUrl() : ""));
            }
            checkServerFromApp(context, baseUrl, list);
        } catch (Exception e) {
            android.util.Log.e("ServerBookLoader", "load failed: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Check if server is reachable from app: try one thumbnail URL, log result.
     * Run in background so UI is not blocked.
     */
    public static void checkServerFromApp(Context context, String baseUrl, List<Book> books) {
        if (context == null || books == null || books.isEmpty()) return;
        new Thread(() -> {
            try {
                Book first = books.get(0);
                String thumbUrl = first != null ? first.getThumbnailUrl() : null;
                if (thumbUrl == null || thumbUrl.isEmpty()) {
                    android.util.Log.w("ServerBookLoader", "Server check: no thumbnail URL to check");
                    return;
                }
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(thumbUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("HEAD");
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.connect();
                    int code = conn.getResponseCode();
                    if (code >= 200 && code < 400) {
                        android.util.Log.i("ServerBookLoader", "Server check: OK (HTTP " + code + ") – thumbnail reachable: " + thumbUrl);
                    } else {
                        android.util.Log.w("ServerBookLoader", "Server check: HTTP " + code + " for " + thumbUrl + " – check server has thumbnails/ with same file names");
                    }
                } finally {
                    if (conn != null) conn.disconnect();
                }
            } catch (Exception e) {
                android.util.Log.w("ServerBookLoader", "Server check: failed – " + e.getMessage() + " (no internet or server down? baseUrl=" + baseUrl + ")");
            }
        }).start();
    }

    private static String detectCategory(String bookName) {
        String lowerName = bookName.toLowerCase(Locale.ROOT);
        if (lowerName.contains("ભક્તિ") || lowerName.contains("bhakti") || lowerName.contains("ભજન") || lowerName.contains("bhajan") ||
            lowerName.contains("ભાગવત") || lowerName.contains("bhagwat") || lowerName.contains("વિષ્ણુ") || lowerName.contains("vishnu") ||
            lowerName.contains("રામાયણ") || lowerName.contains("ramayan") || lowerName.contains("રામ") || lowerName.contains("ram") ||
            lowerName.contains("કૃષ્ણ") || lowerName.contains("krishna") || lowerName.contains("ભર્તૃહરિ") || lowerName.contains("bhartrihari") ||
            lowerName.contains("શતક") || lowerName.contains("shata") || lowerName.contains("સહસ્રનામ") || lowerName.contains("sahasranam")) {
            return "Bhakti";
        }
        if (lowerName.contains("યાત્રા") || lowerName.contains("yatra") || lowerName.contains("પ્રવાસ") || lowerName.contains("travel") ||
            lowerName.contains("પ્રવાસનાં") || lowerName.contains("પ્રવાસની") || lowerName.contains("તીર્થ") || lowerName.contains("tirth") ||
            lowerName.contains("મુલાકાત") || lowerName.contains("mulakat") || lowerName.contains("આફ્રિકા") || lowerName.contains("africa") ||
            lowerName.contains("યુરોપ") || lowerName.contains("europe") || lowerName.contains("ટર્કી") || lowerName.contains("turkey") ||
            lowerName.contains("ઈજિપ્ત") || lowerName.contains("egypt") || lowerName.contains("આંદામાન") || lowerName.contains("andaman")) {
            return "Yatra";
        }
        if (lowerName.contains("જીવન") || lowerName.contains("jeevan") || lowerName.contains("ચરિત્ર") || lowerName.contains("charitra") ||
            lowerName.contains("જીવનકથા") || lowerName.contains("jeevankatha") || lowerName.contains("અનુભવ") || lowerName.contains("anubhav") ||
            lowerName.contains("બાયપાસ") || lowerName.contains("bypass")) {
            return "Jeevan";
        }
        return "Updesh";
    }
}
