package com.swamisachidanand;

import android.content.Context;
import android.util.Log;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDDocumentCatalog;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Scans all PDF books from assets, extracts chapter/outline (ભૂમિકા, અધ્યાય names) from each,
 * saves to cache file. PdfViewerActivity loads from this cache.
 */
public class BookChapterScanner {

    private static final String TAG = "BookChapterScanner";
    private static final String CACHE_FILE = "book_chapters_cache.json";

    public static void scanAllAndSave(Context context) {
        if (context == null) return;
        new Thread(() -> {
            try {
                PDFBoxResourceLoader.init(context.getApplicationContext());
            } catch (Throwable t) {
                Log.e(TAG, "PDFBox init failed", t);
                return;
            }
            try {
                String[] list = context.getAssets().list("");
                if (list == null) return;
                JSONObject root = new JSONObject();
                int count = 0;
                for (String name : list) {
                    if (name == null || (!name.endsWith(".pdf") && !name.endsWith(".PDF"))) continue;
                    try {
                        JSONArray chapters = extractOutline(context, name);
                        if (chapters != null && chapters.length() > 0) {
                            root.put(name, chapters);
                            count++;
                        }
                    } catch (Throwable t) {
                        Log.d(TAG, "Skip outline for " + name, t);
                    }
                }
                File out = new File(context.getFilesDir(), CACHE_FILE);
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    fos.write(root.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
                Log.d(TAG, "Saved chapters for " + count + " books to " + out.getAbsolutePath());
            } catch (Throwable t) {
                Log.e(TAG, "scanAllAndSave failed", t);
            }
        }).start();
    }

    private static JSONArray extractOutline(Context context, String assetFileName) {
        File temp = null;
        try {
            temp = new File(context.getCacheDir(), "scan_" + System.currentTimeMillis() + "_" + assetFileName.replace("/", "_").replace("\\", "_"));
            try (InputStream in = context.getAssets().open(assetFileName);
                 OutputStream out = new FileOutputStream(temp)) {
                byte[] buf = new byte[32768];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            }
            return extractOutlineFromFile(temp, assetFileName);
        } catch (Throwable t) {
            Log.d(TAG, "No outline for " + assetFileName, t);
            return null;
        } finally {
            if (temp != null && temp.exists()) try { temp.delete(); } catch (Exception ignored) {}
        }
    }

    private static JSONArray extractOutlineFromFile(File pdfFile, String assetFileName) {
        try {
            PDDocument doc = PDDocument.load(pdfFile);
            try {
                PDDocumentCatalog catalog = doc.getDocumentCatalog();
                if (catalog == null) return null;
                PDDocumentOutline outline = catalog.getDocumentOutline();
                if (outline == null) return null;
                List<JSONObject> list = new ArrayList<>();
                PDOutlineItem first = outline.getFirstChild();
                if (first != null) collectOutlineItems(first, doc, list);
                if (list.isEmpty()) return null;
                JSONArray arr = new JSONArray();
                for (JSONObject o : list) arr.put(o);
                return arr;
            } finally {
                doc.close();
            }
        } catch (Throwable t) {
            Log.d(TAG, "No outline for " + assetFileName, t);
            return null;
        }
    }

    private static void collectOutlineItems(PDOutlineItem item, PDDocument doc, List<JSONObject> out) {
        if (item == null) return;
        try {
            String title = item.getTitle();
            if (title != null && !title.trim().isEmpty()) {
                PDPage page = item.findDestinationPage(doc);
                int pageIndex = 0;
                if (page != null) {
                    try {
                        pageIndex = doc.getDocumentCatalog().getPages().indexOf(page);
                        if (pageIndex < 0) pageIndex = 0;
                    } catch (Exception e) {
                        pageIndex = 0;
                    }
                }
                JSONObject o = new JSONObject();
                o.put("t", title.trim());
                o.put("p", pageIndex);
                out.add(o);
            }
            collectOutlineItems(item.getFirstChild(), doc, out);
            collectOutlineItems(item.getNextSibling(), doc, out);
        } catch (Throwable t) {
            Log.d(TAG, "collectOutlineItems skip", t);
        }
    }

    public static File getCacheFile(Context context) {
        return context == null ? null : new File(context.getFilesDir(), CACHE_FILE);
    }
}
