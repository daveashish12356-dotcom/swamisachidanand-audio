package com.swamisachidanand;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PdfYearExtractor {
    private static final String TAG = "PdfYearExtractor";
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    
    public interface YearExtractListener {
        void onYearExtracted(String fileName, String year);
    }
    
    public static void extractYear(Context context, String fileName, YearExtractListener listener) {
        executor.execute(() -> {
            try {
                String year = extractYearFromPdf(context, fileName);
                if (listener != null) {
                    listener.onYearExtracted(fileName, year);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error extracting year from " + fileName, e);
                if (listener != null) {
                    listener.onYearExtracted(fileName, null);
                }
            }
        });
    }
    
    private static String extractYearFromPdf(Context context, String fileName) {
        File pdfFile = null;
        ParcelFileDescriptor fileDescriptor = null;
        PdfRenderer pdfRenderer = null;
        
        try {
            // Copy PDF to cache if needed
            pdfFile = new File(context.getCacheDir(), "year_extract_" + fileName);
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
            
            int pageCount = pdfRenderer.getPageCount();
            int pagesToCheck = Math.min(3, pageCount); // Check first 3 pages
            
            // Pattern to match years (1900-2099)
            Pattern yearPattern = Pattern.compile("\\b(19[0-9]{2}|20[0-2][0-9])\\b");
            
            // Check first few pages for year
            for (int i = 0; i < pagesToCheck; i++) {
                PdfRenderer.Page page = pdfRenderer.openPage(i);
                
                // Render page to bitmap
                int width = (int) (page.getWidth() * 0.5); // Smaller for faster processing
                int height = (int) (page.getHeight() * 0.5);
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                
                // Use ML Kit or simple OCR approach
                // For now, we'll use a simple approach - check filename or metadata
                // Since PdfRenderer doesn't extract text, we'll use a fallback
                
                page.close();
                bitmap.recycle();
            }
            
            // Fallback: Try to extract from filename
            String yearFromName = extractYearFromFileName(fileName);
            if (yearFromName != null) {
                return yearFromName;
            }
            
            // Try to get from PDF metadata if available
            // Note: PdfRenderer doesn't provide metadata access
            // We'll use a simple heuristic
            
            return null;
            
        } catch (IOException e) {
            Log.e(TAG, "Error reading PDF: " + fileName, e);
            return null;
        } finally {
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
            // Clean up temp file
            if (pdfFile != null && pdfFile.exists()) {
                pdfFile.delete();
            }
        }
    }
    
    private static String extractYearFromFileName(String fileName) {
        // Try to find year in filename (4 digits)
        Pattern yearPattern = Pattern.compile("\\b(19[0-9]{2}|20[0-2][0-9])\\b");
        Matcher matcher = yearPattern.matcher(fileName);
        
        if (matcher.find()) {
            String year = matcher.group(1);
            // Validate year is reasonable (1900-2024)
            int yearInt = Integer.parseInt(year);
            if (yearInt >= 1900 && yearInt <= 2024) {
                return year;
            }
        }
        
        return null;
    }
    
    // Simple method using filename pattern - most reliable
    public static String extractYearSimple(String fileName) {
        return extractYearFromFileName(fileName);
    }
}


