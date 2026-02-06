package com.swamisachidanand;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.LruCache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PdfThumbnailLoader {
    private static final String TAG = "PdfThumbnailLoader";
    private static final int THUMBNAIL_WIDTH = 300;
    private static final int THUMBNAIL_HEIGHT = 400;
    
    private static PdfThumbnailLoader instance;
    private LruCache<String, Bitmap> thumbnailCache;
    private ExecutorService executorService;
    
    private PdfThumbnailLoader() {
        // Initialize cache with 1/8 of available memory
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        
        thumbnailCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                if (bitmap == null || bitmap.isRecycled()) return 0;
                try {
                    return bitmap.getByteCount() / 1024;
                } catch (Exception e) {
                    return 0;
                }
            }
        };
        
        executorService = Executors.newFixedThreadPool(4);
    }
    
    public static PdfThumbnailLoader getInstance() {
        if (instance == null) {
            instance = new PdfThumbnailLoader();
        }
        return instance;
    }
    
    public interface ThumbnailLoadListener {
        void onThumbnailLoaded(Bitmap thumbnail);
    }
    
    public void loadThumbnail(Context context, String fileName, ThumbnailLoadListener listener) {
        if (context == null || fileName == null || listener == null) return;
        // Check cache first
        Bitmap cached = thumbnailCache.get(fileName);
        if (cached != null && !cached.isRecycled()) {
            listener.onThumbnailLoaded(cached);
            return;
        }

        // Load in background
        executorService.execute(() -> {
            try {
                Bitmap thumbnail = generateThumbnail(context, fileName);
                if (thumbnail != null) {
                    thumbnailCache.put(fileName, thumbnail);
                }
                
                // Post result on main thread
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                Bitmap finalThumbnail = thumbnail;
                mainHandler.post(() -> listener.onThumbnailLoaded(finalThumbnail));
            } catch (Exception e) {
                Log.e(TAG, "Error loading thumbnail for: " + fileName, e);
                // Post null on main thread
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> listener.onThumbnailLoaded(null));
            }
        });
    }
    
    private Bitmap generateThumbnail(Context context, String fileName) {
        ParcelFileDescriptor fileDescriptor = null;
        PdfRenderer pdfRenderer = null;
        PdfRenderer.Page page = null;
        
        try {
            // Copy PDF to cache if needed
            File pdfFile = new File(context.getCacheDir(), fileName);
            if (!pdfFile.exists()) {
                InputStream inputStream = context.getAssets().open(fileName);
                FileOutputStream outputStream = new FileOutputStream(pdfFile);
                
                byte[] buffer = new byte[16384];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                
                inputStream.close();
                outputStream.close();
            }
            
            // Open PDF
            fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(fileDescriptor);
            
            if (pdfRenderer.getPageCount() == 0) {
                return null;
            }
            
            // Render first page
            page = pdfRenderer.openPage(0);
            
            // Calculate scale to fit thumbnail size
            float pageWidth = page.getWidth();
            float pageHeight = page.getHeight();
            
            float scaleX = THUMBNAIL_WIDTH / pageWidth;
            float scaleY = THUMBNAIL_HEIGHT / pageHeight;
            float scale = Math.min(scaleX, scaleY);
            
            int bitmapWidth = (int) (pageWidth * scale);
            int bitmapHeight = (int) (pageHeight * scale);
            
            Bitmap bitmap = Bitmap.createBitmap(
                    bitmapWidth,
                    bitmapHeight,
                    Bitmap.Config.ARGB_8888
            );
            
            // Render page to bitmap
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            
            return bitmap;
            
        } catch (Throwable e) {
            Log.e(TAG, "Error generating thumbnail: " + fileName, e);
            return null;
        } finally {
            if (page != null) {
                page.close();
            }
            if (pdfRenderer != null) {
                pdfRenderer.close();
            }
            if (fileDescriptor != null) {
                try {
                    fileDescriptor.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing file descriptor", e);
                }
            }
        }
    }
    
    public void clearCache() {
        thumbnailCache.evictAll();
    }
}

