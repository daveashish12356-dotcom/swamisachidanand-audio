package com.swamisachidanand;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Load 56 books from server list (books_server_list.json). Each book has pdfUrl and thumbnailUrl.
 * Used by BooksFragment and HomeFragment so PDFs are not bundled in app.
 */
public final class ServerBookLoader {

    private static List<Book> cachedBooks;
    private static long cacheTimeMs = 0;
    private static final long CACHE_TTL_MS = 2 * 60 * 1000;

    public static void clearCache() {
        cachedBooks = null;
        cacheTimeMs = 0;
    }

    public static List<Book> load(Context context) {
        if (cachedBooks != null && (System.currentTimeMillis() - cacheTimeMs) < CACHE_TTL_MS) {
            return new ArrayList<>(cachedBooks);
        }
        List<Book> list = new ArrayList<>();
        if (context == null) return list;
        try {
            String baseUrl = context.getString(R.string.server_books_base_url);
            if (baseUrl == null) baseUrl = "";
            baseUrl = baseUrl.trim();
            if (!baseUrl.isEmpty() && !baseUrl.endsWith("/")) baseUrl += "/";
            // Server repo has PDFs and thumbnails under public/ so live URLs are base + "public/books/" and "public/thumbnails/"
            String booksBase = baseUrl + "public/books/";
            String thumbBase = baseUrl + "public/thumbnails/";

            // 1) Try to load books_server_list.json from server (OkHttp for faster fetch).
            String jsonStr = null;
            if (!baseUrl.isEmpty()) {
                try {
                    OkHttpClient client = new OkHttpClient.Builder()
                            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                            .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                            .build();
                    Request request = new Request.Builder()
                            .url(baseUrl + "books_server_list.json")
                            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")
                            .addHeader("Accept", "application/json")
                            .build();
                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful() && response.body() != null) {
                        jsonStr = response.body().string();
                        // GitHub Pages can return HTML (e.g. 404/403 page); only treat as JSON if it looks like JSON
                        if (jsonStr != null && !jsonStr.trim().startsWith("{")) {
                            android.util.Log.w("ServerBookLoader", "Server response not JSON (got " + (jsonStr.length() > 50 ? jsonStr.substring(0, 50) + "..." : jsonStr) + "), using assets");
                            jsonStr = null;
                        } else if (jsonStr != null && !jsonStr.isEmpty()) {
                            android.util.Log.i("ServerBookLoader", "books_server_list.json loaded from server OK");
                        }
                    } else {
                        android.util.Log.w("ServerBookLoader", "Server response not successful: " + (response != null ? response.code() : "null"));
                    }
                } catch (Exception e) {
                    android.util.Log.w("ServerBookLoader", "Server books_server_list.json load failed, falling back to assets: " + e.getMessage());
                }
            }

            // 2) Fallback: bundled assets/books_server_list.json (same format), so offline/old APK still works.
            boolean fromServer = (jsonStr != null && !jsonStr.isEmpty());
            if (jsonStr == null || jsonStr.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(context.getAssets().open("books_server_list.json"), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                }
                jsonStr = sb.toString();
            }
            if (jsonStr == null) jsonStr = "";
            if (jsonStr.startsWith("\uFEFF")) jsonStr = jsonStr.substring(1);
            JSONObject root;
            try {
                root = new JSONObject(jsonStr);
            } catch (Exception parseEx) {
                android.util.Log.w("ServerBookLoader", "Parse failed, retrying from assets: " + parseEx.getMessage());
                if (fromServer) {
                    try (BufferedReader r = new BufferedReader(
                            new InputStreamReader(context.getAssets().open("books_server_list.json"), StandardCharsets.UTF_8))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = r.readLine()) != null) sb.append(line);
                        jsonStr = sb.toString();
                        if (jsonStr.startsWith("\uFEFF")) jsonStr = jsonStr.substring(1);
                        root = new JSONObject(jsonStr);
                    } catch (Exception e2) {
                        android.util.Log.e("ServerBookLoader", "Assets fallback failed: " + e2.getMessage());
                        return list;
                    }
                } else return list;
            }
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
            if (fromServer && list.isEmpty()) {
                android.util.Log.w("ServerBookLoader", "Server returned empty list, using assets");
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(context.getAssets().open("books_server_list.json"), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                    jsonStr = sb.toString();
                    if (jsonStr.startsWith("\uFEFF")) jsonStr = jsonStr.substring(1);
                    root = new JSONObject(jsonStr);
                    arr = root.optJSONArray("fileNames");
                    if (arr != null) {
                        list.clear();
                        for (int i = 0; i < arr.length(); i++) {
                            String fileName = arr.optString(i, "").trim();
                            if (fileName.isEmpty() || !fileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) continue;
                            String displayName = fileName.replace(".pdf", "").replace(".PDF", "");
                            String thumbName = fileName.replace(".pdf", ".jpg").replace(".PDF", ".jpg");
                            String encodedPdf = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replace("+", "%20");
                            String encodedThumb = URLEncoder.encode(thumbName, StandardCharsets.UTF_8.name()).replace("+", "%20");
                            Book book = new Book(displayName, fileName, 0);
                            book.setPdfUrl(booksBase + encodedPdf);
                            book.setThumbnailUrl(thumbBase + encodedThumb);
                            String year = PdfYearExtractor.extractYearSimple(fileName);
                            if (year != null) book.setPublishYear(year);
                            book.setCategory(detectCategory(displayName));
                            list.add(book);
                        }
                    }
                } catch (Exception e) {
                    android.util.Log.e("ServerBookLoader", "Assets fallback for empty server list failed", e);
                }
            }
            android.util.Log.d("ServerBookLoader", "loaded " + list.size() + " books, baseUrl=" + baseUrl);
            if (!list.isEmpty()) {
                cachedBooks = new ArrayList<>(list);
                cacheTimeMs = System.currentTimeMillis();
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
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36");
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
