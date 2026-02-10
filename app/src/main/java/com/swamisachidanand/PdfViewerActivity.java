package com.swamisachidanand;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import android.content.SharedPreferences;
import android.content.res.Configuration;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * PDF viewer - single page at a time (no ViewPager2/PdfPageAdapter to avoid crash).
 */
public class PdfViewerActivity extends AppCompatActivity {

    private static final String TAG = "PdfViewerActivity";
    private static final String KEY_CURRENT_PAGE = "current_page_index";
    /** Max bitmap size to avoid OOM; render at screen size up to this cap for sharp text */
    private static final int MAX_BITMAP_WIDTH = 1200;
    private static final int MAX_BITMAP_HEIGHT = 1800;

    private String bookName;
    private String pdfUrl;
    private File pdfFile;
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor fileDescriptor;
    private View pdfContentContainer;
    private ImageView pdfSinglePageImage;
    private View pdfDimOverlay;
    private TextView pdfLoadingOnlineText;
    private View pdfFrameLayout;

    private LinearLayout bottomProgressBar;
    private SeekBar progressSeekBar;
    private TextView pagesLeftText;
    private TextView pageNumberText;
    private ImageButton chaptersMenuButton;
    private TextView progressWatermark;

    private boolean isDayMode = true;
    private View pdfTopBar;
    private ImageButton dayNightToggleBtn;
    private TextView pdfBookTitle;
    private ImageButton pdfTopMoreBtn;
    private int pageCount = 0;
    private int currentPageIndex = 0;

    private SharedPreferences readingProgressPrefs;
    private static final String PREFS_NAME = "reading_progress";
    private static final String KEY_RECENT_BOOKS = "recent_books_list";
    private static final String KEY_LAST_PDF_URL = "last_pdf_url";
    private static final int MAX_RECENT_BOOKS = 10;
    private List<Chapter> chapters;
    private final Object rendererLock = new Object();
    private volatile boolean rendering = false;

    private android.os.Handler mainHandler;
    private Runnable autoHideRunnable;
    private boolean isControlsVisible = true;
    private GestureDetector gestureDetector;

    private static class Chapter {
        String title;
        int pageNumber;
        Chapter(String title, int pageNumber) {
            this.title = title;
            this.pageNumber = pageNumber;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_pdf_viewer);

            bookName = getIntent().getStringExtra("book_name");
            pdfUrl = getIntent().getStringExtra("pdf_url");
            if (pdfUrl != null) pdfUrl = pdfUrl.trim();
            if (bookName == null) bookName = "";
            bookName = bookName.trim();
            if (bookName.isEmpty() && (pdfUrl == null || pdfUrl.isEmpty())) {
                Toast.makeText(this, "બુક મળી નહીં.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            readingProgressPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            chapters = new ArrayList<>();
            addToRecentBooks(bookName);

            int savedPage = readingProgressPrefs.getInt(bookName + "_page", -1);
            if (savedInstanceState != null) {
                currentPageIndex = savedInstanceState.getInt(KEY_CURRENT_PAGE, savedPage >= 0 ? savedPage : 0);
            } else if (savedPage >= 0) {
                currentPageIndex = savedPage;
            }

            pdfDimOverlay = findViewById(R.id.pdf_dim_overlay);
            pdfFrameLayout = findViewById(R.id.pdf_frame_layout);
            pdfContentContainer = findViewById(R.id.pdf_content_container);
            pdfSinglePageImage = findViewById(R.id.pdf_single_page_image);
            if (pdfContentContainer == null || pdfSinglePageImage == null) {
                Toast.makeText(this, "Layout error.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            if (pdfDimOverlay != null) pdfDimOverlay.setVisibility(View.GONE);
            if (pdfFrameLayout != null) pdfFrameLayout.setBackgroundColor(Color.BLACK);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(Color.BLACK);
                getWindow().setNavigationBarColor(Color.BLACK);
            }
            getWindow().getDecorView().setBackgroundColor(Color.BLACK);

            pdfTopBar = findViewById(R.id.pdf_top_bar);
            dayNightToggleBtn = findViewById(R.id.day_night_toggle_btn);
            pdfBookTitle = findViewById(R.id.pdf_book_title);
            pdfTopMoreBtn = findViewById(R.id.pdf_top_more_btn);
            String title = bookName != null ? bookName.replace(".pdf", "").replace(".PDF", "").trim() : "";
            if (pdfBookTitle != null) pdfBookTitle.setText(title);
            syncDayNightFromSystem();
            updateDayNightToggleUi();
            if (dayNightToggleBtn != null) {
                dayNightToggleBtn.setOnClickListener(v -> {
                    isDayMode = !isDayMode;
                    animateDayNightToggle();
                    applyDayNightMode();
                });
            }
            if (pdfTopMoreBtn != null) pdfTopMoreBtn.setOnClickListener(v -> showPdfTopMoreMenu());

            bottomProgressBar = findViewById(R.id.bottom_progress_bar);
            progressSeekBar = findViewById(R.id.progress_seekbar);
            pagesLeftText = findViewById(R.id.pages_left_text);
            pageNumberText = findViewById(R.id.page_number_text);
            chaptersMenuButton = findViewById(R.id.chapters_menu_button);
            progressWatermark = findViewById(R.id.progress_watermark);
            if (chaptersMenuButton != null) chaptersMenuButton.setOnClickListener(v -> showChapterList());

            if (progressSeekBar != null) {
                progressSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser && pageCount > 0) {
                            int targetPage = (int) ((progress / 100.0) * pageCount);
                            if (targetPage >= pageCount) targetPage = pageCount - 1;
                            if (targetPage >= 0) {
                                currentPageIndex = targetPage;
                                saveReadingProgress();
                                updateBottomProgressBar();
                                renderCurrentPage();
                            }
                        }
                    }
                    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                });
            }

            pdfSinglePageImage.setBackgroundColor(Color.BLACK);
            setupSwipeAndTap();

            mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            autoHideRunnable = () -> { if (!isFinishing() && !isDestroyed()) hideControls(); };
            if (pdfContentContainer != null && bottomProgressBar != null) {
                mainHandler.postDelayed(autoHideRunnable, 5000);
            }

            if (pdfDimOverlay != null) {
                pdfDimOverlay.setVisibility(View.VISIBLE);
                pdfDimOverlay.setBackgroundColor(0xE6000000);
            }
            pdfLoadingOnlineText = findViewById(R.id.pdf_loading_online_text);
            if (pdfUrl != null && !pdfUrl.isEmpty() && pdfLoadingOnlineText != null) {
                pdfLoadingOnlineText.setText("ઇન્ટરનેટથી લોડ થાય છે...");
                pdfLoadingOnlineText.setVisibility(View.VISIBLE);
            }
            if (pdfUrl != null && !pdfUrl.isEmpty()) loadPdfFromUrl(pdfUrl);
            else loadPdfFromAssets();
        } catch (Exception e) {
            Log.e(TAG, "Fatal error in onCreate", e);
            Toast.makeText(this, "App error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void hideLoadingOverlay() {
        runOnUiThread(() -> {
            if (pdfLoadingOnlineText != null) pdfLoadingOnlineText.setVisibility(View.GONE);
            if (pdfDimOverlay != null) pdfDimOverlay.setVisibility(View.GONE);
        });
    }

    private static final int CONTROLS_ANIM_DURATION = 450;
    private static final int AUTO_HIDE_DELAY_MS = 5000;

    private void hideControls() {
        if (isFinishing() || isDestroyed()) return;
        isControlsVisible = false;
        final View topBar = pdfTopBar;
        final View bottomBar = bottomProgressBar;
        if (topBar != null && topBar.getVisibility() == View.VISIBLE) {
            topBar.animate()
                    .alpha(0f)
                    .translationY(-topBar.getHeight())
                    .setDuration(CONTROLS_ANIM_DURATION)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .withEndAction(() -> {
                        topBar.setVisibility(View.GONE);
                        topBar.setAlpha(1f);
                        topBar.setTranslationY(0f);
                    })
                    .start();
        }
        if (bottomBar != null && bottomBar.getVisibility() == View.VISIBLE) {
            bottomBar.animate()
                    .alpha(0f)
                    .translationY(bottomBar.getHeight())
                    .setDuration(CONTROLS_ANIM_DURATION)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .withEndAction(() -> {
                        bottomBar.setVisibility(View.GONE);
                        bottomBar.setAlpha(1f);
                        bottomBar.setTranslationY(0f);
                    })
                    .start();
        }
    }

    private void showControls() {
        isControlsVisible = true;
        if (pdfTopBar != null) {
            pdfTopBar.setVisibility(View.VISIBLE);
            pdfTopBar.setAlpha(0f);
            pdfTopBar.post(() -> {
                int h = pdfTopBar.getHeight();
                pdfTopBar.setTranslationY(h > 0 ? -h : 0);
                pdfTopBar.animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(CONTROLS_ANIM_DURATION)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .start();
            });
        }
        if (bottomProgressBar != null) {
            bottomProgressBar.setVisibility(View.VISIBLE);
            bottomProgressBar.setAlpha(0f);
            bottomProgressBar.post(() -> {
                int h = bottomProgressBar.getHeight();
                bottomProgressBar.setTranslationY(h > 0 ? h : 0);
                bottomProgressBar.animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(CONTROLS_ANIM_DURATION)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .start();
            });
        }
        updateBottomProgressBar();
    }

    private void toggleControlsVisibility() {
        if (isControlsVisible) hideControls();
        else {
            showControls();
            if (mainHandler != null && autoHideRunnable != null) {
                mainHandler.removeCallbacks(autoHideRunnable);
                mainHandler.postDelayed(autoHideRunnable, AUTO_HIDE_DELAY_MS);
            }
        }
    }

    /** Swipe left = next page, swipe right = prev page; tap = show/hide controls */
    private void setupSwipeAndTap() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                toggleControlsVisibility();
                return true;
            }
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null || pageCount <= 0) return false;
                float dx = e2.getX() - e1.getX();
                float absDx = Math.abs(dx);
                if (absDx < 80) return false;
                if (dx > 0) {
                    goToPrevPage();
                } else {
                    goToNextPage();
                }
                return true;
            }
        });
        if (pdfContentContainer != null) {
            pdfContentContainer.setOnTouchListener((v, event) -> {
                if (gestureDetector != null && gestureDetector.onTouchEvent(event)) return true;
                return false;
            });
        }
    }

    private void goToNextPage() {
        if (currentPageIndex >= pageCount - 1) return;
        final int targetPage = currentPageIndex + 1;
        animatePageTurn(true, () -> {
            currentPageIndex = targetPage;
            saveReadingProgress();
            updateBottomProgressBar();
            renderCurrentPage();
        });
    }

    private void goToPrevPage() {
        if (currentPageIndex <= 0) return;
        final int targetPage = currentPageIndex - 1;
        animatePageTurn(false, () -> {
            currentPageIndex = targetPage;
            saveReadingProgress();
            updateBottomProgressBar();
            renderCurrentPage();
        });
    }

    /** Book-style page turn (Google Play Books style): page flips from spine edge with 3D rotation. */
    private static final int PAGE_TURN_DURATION_MS = 320;
    private void animatePageTurn(boolean forward, Runnable onMidTurn) {
        if (pdfSinglePageImage == null) {
            if (onMidTurn != null) onMidTurn.run();
            return;
        }
        try {
            final View v = pdfSinglePageImage;
            int w = v.getWidth();
            int h = v.getHeight();
            if (w <= 0 || h <= 0) {
                if (onMidTurn != null) onMidTurn.run();
                return;
            }
            float density = getResources().getDisplayMetrics().density;
            v.setCameraDistance(12000f * density);
            v.animate().cancel();
            v.setTranslationX(0f);
            v.setTranslationZ(0f);
            v.setPivotY(h * 0.5f);
            if (forward) {
                v.setPivotX(w);
                v.setRotationY(0f);
                v.animate()
                        .rotationY(-90f)
                        .setDuration(PAGE_TURN_DURATION_MS)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator(1.2f))
                        .withEndAction(() -> {
                            if (onMidTurn != null) onMidTurn.run();
                            v.setPivotX(0f);
                            v.setRotationY(90f);
                            v.animate()
                                    .rotationY(0f)
                                    .setDuration(PAGE_TURN_DURATION_MS)
                                    .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                                    .withEndAction(() -> {
                                        v.setPivotX(w * 0.5f);
                                        v.setPivotY(h * 0.5f);
                                    })
                                    .start();
                        })
                        .start();
            } else {
                v.setPivotX(0f);
                v.setRotationY(0f);
                v.animate()
                        .rotationY(90f)
                        .setDuration(PAGE_TURN_DURATION_MS)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator(1.2f))
                        .withEndAction(() -> {
                            if (onMidTurn != null) onMidTurn.run();
                            v.setPivotX(w);
                            v.setRotationY(-90f);
                            v.animate()
                                    .rotationY(0f)
                                    .setDuration(PAGE_TURN_DURATION_MS)
                                    .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                                    .withEndAction(() -> {
                                        v.setPivotX(w * 0.5f);
                                        v.setPivotY(h * 0.5f);
                                    })
                                    .start();
                        })
                        .start();
            }
        } catch (Throwable t) {
            if (onMidTurn != null) onMidTurn.run();
        }
    }

    private static final String CACHE_PDF_FILENAME = "current_book.pdf";

    /** Download PDF from server URL and open. Uses cache if same URL was just opened (instant reopen). */
    private void loadPdfFromUrl(String url) {
        final long t0 = System.currentTimeMillis();
        Log.d(TAG, "[perf] loadPdfFromUrl start url=" + url);
        File cacheDir = getCacheDir();
        if (cacheDir != null) {
            String lastUrl = readingProgressPrefs != null ? readingProgressPrefs.getString(KEY_LAST_PDF_URL, "") : "";
            File cached = new File(cacheDir, CACHE_PDF_FILENAME);
            if (url != null && url.equals(lastUrl) && cached.exists() && cached.length() > 0) {
                Log.d(TAG, "[perf] using cached file, skip download");
                pdfFile = cached;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    try { openPdf(); } catch (Throwable e) {
                        Log.e(TAG, "Error opening cached PDF", e);
                        hideLoadingOverlay();
                        finish();
                    }
                });
                return;
            }
        }
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .build();
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        runOnUiThread(() -> {
                            hideLoadingOverlay();
                            Toast.makeText(this, "બુક લોડ થઈ નહીં. ઇન્ટરનેટ ચેક કરો.", Toast.LENGTH_LONG).show();
                            finish();
                        });
                        return;
                    }
                    File dir = getCacheDir();
                    if (dir == null) {
                        runOnUiThread(() -> { hideLoadingOverlay(); finish(); });
                        return;
                    }
                    pdfFile = new File(dir, CACHE_PDF_FILENAME);
                    try (java.io.InputStream in = response.body().byteStream();
                         FileOutputStream out = new FileOutputStream(pdfFile)) {
                        byte[] buf = new byte[65536];
                        int n;
                        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                    }
                    if (readingProgressPrefs != null && url != null) {
                        try { readingProgressPrefs.edit().putString(KEY_LAST_PDF_URL, url).apply(); } catch (Exception ignored) {}
                    }
                    Log.d(TAG, "[perf] download done in " + (System.currentTimeMillis() - t0) + " ms");
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        try {
                            openPdf();
                        } catch (Throwable e) {
                            Log.e(TAG, "Error opening PDF from URL", e);
                            hideLoadingOverlay();
                            Toast.makeText(this, "બુક ખોલતાં ભૂલ.", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading PDF from URL", e);
                runOnUiThread(() -> {
                    hideLoadingOverlay();
                    Toast.makeText(this, "બુક લોડ થઈ નહીં: " + (e.getMessage() != null ? e.getMessage() : ""), Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    /** Load PDF from a specific asset file (e.g. mahabharat_chintan.pdf). */
    private void loadPdfFromAssetName(String assetFileName) {
        final long t0 = System.currentTimeMillis();
        Log.d(TAG, "[perf] loadPdfFromAssetName start: " + assetFileName);
        new Thread(() -> {
            InputStream inputStream = null;
            FileOutputStream outputStream = null;
            try {
                File cacheDir = getCacheDir();
                if (cacheDir == null) {
                    runOnUiThread(() -> { hideLoadingOverlay(); finish(); });
                    return;
                }
                inputStream = getAssets().open(assetFileName);
                pdfFile = new File(cacheDir, CACHE_PDF_FILENAME);
                outputStream = new FileOutputStream(pdfFile);
                byte[] buffer = new byte[65536];
                int n;
                while ((n = inputStream.read(buffer)) != -1) outputStream.write(buffer, 0, n);
                outputStream.close();
                inputStream.close();
                Log.d(TAG, "[perf] asset copy done in " + (System.currentTimeMillis() - t0) + " ms");
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    try { openPdf(); } catch (Throwable e) {
                        Log.e(TAG, "Error opening PDF from asset", e);
                        hideLoadingOverlay();
                        Toast.makeText(this, "બુક ખોલતાં ભૂલ.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            } catch (Throwable e) {
                Log.e(TAG, "Error loading asset: " + assetFileName, e);
                try { if (outputStream != null) outputStream.close(); } catch (IOException ignored) {}
                try { if (inputStream != null) inputStream.close(); } catch (IOException ignored) {}
                runOnUiThread(() -> {
                    hideLoadingOverlay();
                    Toast.makeText(this, "બુક લોડ થઈ નહીં.", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    private void loadPdfFromAssets() {
        final long t0 = System.currentTimeMillis();
        Log.d(TAG, "[perf] loadPdfFromAssets start");
        final String assetName = bookName != null ? bookName.trim() : "";
        if (assetName.isEmpty()) {
            hideLoadingOverlay();
            Toast.makeText(this, "બુક નામ ખાલી.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        new Thread(() -> {
            InputStream inputStream = null;
            FileOutputStream outputStream = null;
            try {
                File cacheDir = getCacheDir();
                if (cacheDir == null) {
                    runOnUiThread(() -> { Toast.makeText(this, "બુક લોડ થતાં ભૂલ.", Toast.LENGTH_LONG).show(); finish(); });
                    return;
                }
                inputStream = getAssets().open(assetName);
                pdfFile = new File(cacheDir, CACHE_PDF_FILENAME);
                outputStream = new FileOutputStream(pdfFile);
                byte[] buffer = new byte[65536];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.close();
                inputStream.close();
                outputStream = null;
                inputStream = null;
                Log.d(TAG, "[perf] assets copy done in " + (System.currentTimeMillis() - t0) + " ms");
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    try {
                        openPdf();
                    } catch (Throwable e) {
                        Log.e(TAG, "Error opening PDF", e);
                        hideLoadingOverlay();
                        Toast.makeText(this, "બુક ખોલતાં ભૂલ.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            } catch (Throwable e) {
                Log.e(TAG, "Error loading PDF: " + assetName, e);
                try { if (outputStream != null) outputStream.close(); } catch (IOException ignored) {}
                try { if (inputStream != null) inputStream.close(); } catch (IOException ignored) {}
                runOnUiThread(() -> {
                    hideLoadingOverlay();
                    Toast.makeText(this, "બુક લોડ થતાં ભૂલ: " + (e.getMessage() != null ? e.getMessage() : assetName), Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    private void openPdf() {
        final long tOpen = System.currentTimeMillis();
        Log.d(TAG, "[perf] openPdf start");
        try {
            if (pdfFile == null || !pdfFile.exists()) {
                hideLoadingOverlay();
                Toast.makeText(this, "PDF file not found", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            try {
                fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            } catch (IOException e) {
                hideLoadingOverlay();
                Toast.makeText(this, "બુક ખોલી શકાઈ નહીં.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            if (fileDescriptor == null) {
                hideLoadingOverlay();
                finish();
                return;
            }
            try {
                pdfRenderer = new PdfRenderer(fileDescriptor);
            } catch (IOException e) {
                try { fileDescriptor.close(); } catch (IOException e2) {}
                hideLoadingOverlay();
                Toast.makeText(this, "બુક ફોરમેટ ભૂલ.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            pageCount = pdfRenderer.getPageCount();
            if (pageCount <= 0) {
                pdfRenderer.close();
                hideLoadingOverlay();
                Toast.makeText(this, "બુકમાં પાનાં નથી.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            if (currentPageIndex >= pageCount) currentPageIndex = pageCount - 1;
            if (currentPageIndex < 0) currentPageIndex = 0;

            hideLoadingOverlay();
            if (progressSeekBar != null && pageCount > 0) {
                progressSeekBar.setMax(100);
            }
            updateBottomProgressBar();
            Log.d(TAG, "[perf] openPdf ready to render in " + (System.currentTimeMillis() - tOpen) + " ms");
            renderCurrentPage();
            final String chapterKey = getIntent().getStringExtra("book_file_name") != null
                    ? getIntent().getStringExtra("book_file_name").trim() : (bookName != null ? bookName : "");
            final String key = chapterKey.isEmpty() ? bookName : chapterKey;
            new Thread(() -> {
                List<Chapter> list = loadChaptersFromAssetsAsList(key);
                if (list == null) list = loadChaptersFromCacheAsList(key);
                if (list == null) list = createDefaultChaptersList();
                if (list == null) return;
                List<Chapter> finalList = list;
                runOnUiThread(() -> {
                    if (chapters != null && !isFinishing() && !isDestroyed()) {
                        chapters.clear();
                        chapters.addAll(finalList);
                        if (pdfFile != null && pdfFile.exists()) loadChaptersFromDownloadedFileAsync();
                    }
                });
            }).start();
        } catch (Throwable t) {
            Log.e(TAG, "openPdf error", t);
            hideLoadingOverlay();
            Toast.makeText(this, "બુક ખોલતાં ભૂલ.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void renderCurrentPage() {
        if (pdfRenderer == null || pdfSinglePageImage == null || pageCount <= 0) return;
        if (rendering) return;
        final int pageIndex = currentPageIndex;
        if (pageIndex < 0 || pageIndex >= pageCount) return;

        int maxW = MAX_BITMAP_WIDTH;
        int maxH = MAX_BITMAP_HEIGHT;
        try {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            if (dm != null && dm.widthPixels > 0 && dm.heightPixels > 0) {
                maxW = Math.min(Math.max(dm.widthPixels, 720), MAX_BITMAP_WIDTH);
                maxH = Math.min(Math.max(dm.heightPixels, 1000), MAX_BITMAP_HEIGHT);
            }
        } catch (Exception e) { /* use constants */ }
        final int finalMaxW = maxW;
        final int finalMaxH = maxH;
        rendering = true;
        final long tRender = System.currentTimeMillis();
        new Thread(() -> {
            Bitmap bitmap = null;
            try {
                synchronized (rendererLock) {
                    if (pdfRenderer == null || isFinishing()) {
                        runOnUiThread(() -> rendering = false);
                        return;
                    }
                    PdfRenderer.Page page = null;
                    try {
                        page = pdfRenderer.openPage(pageIndex);
                        int w = page.getWidth();
                        int h = page.getHeight();
                        if (w <= 0 || h <= 0) {
                            if (page != null) page.close();
                            runOnUiThread(() -> rendering = false);
                            return;
                        }
                        float scale = Math.min((float) finalMaxW / w, (float) finalMaxH / h);
                        int bw = Math.max(1, (int) (w * scale));
                        int bh = Math.max(1, (int) (h * scale));
                        bitmap = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
                        if (bitmap != null) {
                            bitmap.eraseColor(0xFFFFFFFF);
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                        }
                    } finally {
                        if (page != null) try { page.close(); } catch (Exception e) { Log.e(TAG, "page.close", e); }
                    }
                }
                if (bitmap == null || isFinishing() || isDestroyed()) {
                    runOnUiThread(() -> rendering = false);
                    return;
                }
                final Bitmap toShow = isDayMode ? bitmap : applyNightFilter(bitmap);
                if (toShow != bitmap && bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
                runOnUiThread(() -> {
                    rendering = false;
                    Log.d(TAG, "[perf] first page on screen in " + (System.currentTimeMillis() - tRender) + " ms (render)");
                    if (isFinishing() || isDestroyed() || pdfSinglePageImage == null) return;
                    if (currentPageIndex != pageIndex) return;
                    try {
                        pdfSinglePageImage.setImageBitmap(toShow);
                        pdfSinglePageImage.setBackgroundColor(Color.BLACK);
                    } catch (Exception e) {
                        Log.e(TAG, "setImageBitmap", e);
                    }
                });
            } catch (Throwable t) {
                Log.e(TAG, "renderCurrentPage error", t);
                runOnUiThread(() -> rendering = false);
            }
        }).start();
    }

    private Bitmap applyNightFilter(Bitmap original) {
        if (original == null || original.isRecycled()) return null;
        try {
            Bitmap out = Bitmap.createBitmap(original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(out);
            ColorMatrix m = new ColorMatrix(new float[]{ -1,0,0,0,255, 0,-1,0,0,255, 0,0,-1,0,255, 0,0,0,1,0 });
            Paint p = new Paint();
            p.setColorFilter(new ColorMatrixColorFilter(m));
            c.drawBitmap(original, 0, 0, p);
            return out;
        } catch (Exception e) {
            return original;
        }
    }

    private void updateBottomProgressBar() {
        if (isFinishing() || isDestroyed() || pageCount <= 0 || progressSeekBar == null || pageNumberText == null || pagesLeftText == null) return;
        try {
            int percentage = (int) (((currentPageIndex + 1) * 100.0) / pageCount);
            progressSeekBar.setProgress(percentage);
            int pagesLeft = pageCount - (currentPageIndex + 1);
            pagesLeftText.setText(pagesLeft + " pages left");
            pageNumberText.setText((currentPageIndex + 1) + " / " + pageCount);
            if (progressWatermark != null) progressWatermark.setText(percentage + "%");
        } catch (Exception e) {
            Log.e(TAG, "updateBottomProgressBar error", e);
        }
    }

    /** Sync isDayMode with system dark/light mode: phone night = app night */
    private void syncDayNightFromSystem() {
        try {
            int uiMode = getResources().getConfiguration().uiMode;
            boolean systemNight = (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            isDayMode = !systemNight;
        } catch (Exception e) {
            Log.e(TAG, "syncDayNightFromSystem", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean wasDay = isDayMode;
        syncDayNightFromSystem();
        if (wasDay != isDayMode && dayNightToggleBtn != null) {
            updateDayNightToggleUi();
            renderCurrentPage();
        }
    }

    private void applyDayNightMode() {
        updateDayNightToggleUi();
        renderCurrentPage();
    }

    private void updateDayNightToggleUi() {
        if (dayNightToggleBtn != null) {
            dayNightToggleBtn.setImageResource(isDayMode ? R.drawable.ic_toggle_on : R.drawable.ic_toggle_off);
            dayNightToggleBtn.setScaleX(1f);
            dayNightToggleBtn.setScaleY(1f);
            dayNightToggleBtn.setAlpha(1f);
        }
    }

    /** Toggle switch animation: press-in -> swap image -> bounce back */
    private void animateDayNightToggle() {
        if (dayNightToggleBtn == null) return;
        dayNightToggleBtn.animate().cancel();
        final ImageButton btn = dayNightToggleBtn;
        // Phase 1: press-in (scale down, slight fade)
        btn.animate()
                .scaleX(0.88f).scaleY(0.88f)
                .alpha(0.85f)
                .setDuration(120)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .withEndAction(() -> {
                    if (btn == null) return;
                    btn.setImageResource(isDayMode ? R.drawable.ic_toggle_on : R.drawable.ic_toggle_off);
                    // Phase 2: release with bounce (overshoot)
                    btn.animate()
                            .scaleX(1f).scaleY(1f)
                            .alpha(1f)
                            .setDuration(280)
                            .setInterpolator(new android.view.animation.OvershootInterpolator(1.25f))
                            .start();
                })
                .start();
    }

    private void showPdfTopMoreMenu() {
        if (pdfTopMoreBtn == null) return;
        android.widget.PopupMenu p = new android.widget.PopupMenu(this, pdfTopMoreBtn);
        p.getMenu().add(0, 1, 0, "અધ્યાયો");
        p.getMenu().add(0, 2, 0, "Reading progress");
        p.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) { showChapterList(); return true; }
            if (item.getItemId() == 2) { showProgressDialog(); return true; }
            return false;
        });
        p.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pdfRenderer != null) {
            try { pdfRenderer.close(); } catch (Exception e) {}
            pdfRenderer = null;
        }
        if (fileDescriptor != null) {
            try { fileDescriptor.close(); } catch (IOException e) {}
            fileDescriptor = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_PAGE, currentPageIndex);
    }

    private void saveReadingProgress() {
        if (readingProgressPrefs != null && bookName != null && pageCount > 0) {
            readingProgressPrefs.edit()
                .putInt(bookName + "_page", currentPageIndex)
                .putInt(bookName + "_total_pages", pageCount)
                .putLong(bookName + "_last_read", System.currentTimeMillis())
                .apply();
        }
    }

    private void addToRecentBooks(String fileName) {
        if (readingProgressPrefs == null || fileName == null || fileName.trim().isEmpty()) return;
        try {
            String current = readingProgressPrefs.getString(KEY_RECENT_BOOKS, "");
            List<String> list = new ArrayList<>();
            if (!current.isEmpty()) {
                for (String s : current.split(",")) {
                    String t = s.trim();
                    if (!t.isEmpty() && !t.equals(fileName)) list.add(t);
                }
            }
            list.add(0, fileName);
            while (list.size() > MAX_RECENT_BOOKS) list.remove(list.size() - 1);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(list.get(i));
            }
            readingProgressPrefs.edit().putString(KEY_RECENT_BOOKS, sb.toString()).apply();
        } catch (Exception e) {
            Log.e(TAG, "addToRecentBooks error", e);
        }
    }

    private void showChapterList() {
        if (chapters == null) chapters = new ArrayList<>();
        if (chapters.isEmpty()) createDefaultChapters();

        List<String> chapterDisplayList = new ArrayList<>();
        for (int i = 0; i < chapters.size(); i++) {
            Chapter ch = chapters.get(i);
            int chapterNumber = i + 1; // 1, 2, 3... (not page number)
            int pageDisplay = ch.pageNumber + 1;
            chapterDisplayList.add(chapterNumber + ". " + ch.title + " (પૃષ્ઠ " + pageDisplay + ")");
        }
        android.widget.ListView listView = new android.widget.ListView(this);
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_list_item_1, chapterDisplayList);
        listView.setAdapter(adapter);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("અધ્યાયો (" + chapters.size() + ")");
        builder.setView(listView);
        builder.setNegativeButton("બંધ કરો", null);
        AlertDialog dialog = builder.create();
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Chapter selectedChapter = chapters.get(position);
            currentPageIndex = selectedChapter.pageNumber;
            updateBottomProgressBar();
            saveReadingProgress();
            renderCurrentPage();
            dialog.dismiss();
        });
        dialog.show();
    }

    private void showProgressDialog() {
        if (pageCount > 0) {
            int percentage = (int) (((currentPageIndex + 1) * 100.0) / pageCount);
            int pagesRead = currentPageIndex + 1;
            int pagesRemaining = pageCount - pagesRead;
            String message = "Reading Progress:\n\nPages Read: " + pagesRead + " / " + pageCount + "\nPages Remaining: " + pagesRemaining + "\nProgress: " + percentage + "%";
            new AlertDialog.Builder(this).setTitle("Reading Progress").setMessage(message).setPositiveButton("OK", null).show();
        } else {
            Toast.makeText(this, "Progress not available", Toast.LENGTH_SHORT).show();
        }
    }

    /** Load chapters from scanned cache (BookChapterScanner) - real names from PDF outline. Returns true if loaded. */
    private boolean loadChaptersFromCache(String bookFileName) {
        if (chapters == null) chapters = new ArrayList<>();
        chapters.clear();
        if (bookFileName == null || pageCount <= 0) return false;
        try {
            File cacheFile = BookChapterScanner.getCacheFile(this);
            if (cacheFile == null || !cacheFile.exists()) return false;
            String json = readFileToString(cacheFile);
            if (json == null || json.isEmpty()) return false;
            JSONObject root = new JSONObject(json);
            // Cache is keyed by asset PDF filename (e.g. "નામ.pdf"). Server books pass display name without .pdf.
            String cacheKey = bookFileName;
            if (!root.has(cacheKey)) {
                String withPdf = bookFileName.trim();
                if (!withPdf.endsWith(".pdf") && !withPdf.endsWith(".PDF")) withPdf = withPdf + ".pdf";
                if (root.has(withPdf)) cacheKey = withPdf;
                else if (bookFileName.endsWith(".pdf") && root.has(bookFileName.replace(".pdf", "").trim())) cacheKey = bookFileName.replace(".pdf", "").trim();
                else if (bookFileName.endsWith(".PDF") && root.has(bookFileName.replace(".PDF", "").trim())) cacheKey = bookFileName.replace(".PDF", "").trim();
            }
            if (!root.has(cacheKey)) return false;
            JSONArray arr = root.getJSONArray(cacheKey);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String title = o.optString("t", o.optString("title", ""));
                int page = o.optInt("p", o.optInt("page", 0));
                if (page < 0) page = 0;
                if (page >= pageCount) continue;
                chapters.add(new Chapter(title, page));
            }
            if (chapters.isEmpty()) return false;
            // Ensure first chapter always starts from page 0 so early pages are not "cut off"
            if (!chapters.isEmpty() && chapters.get(0).pageNumber > 0) {
                chapters.add(0, new Chapter("અધ્યાય ૧", 0));
            }
            return true;
        } catch (Throwable t) {
            Log.d(TAG, "No cache or no entry for " + bookFileName, t);
            return false;
        }
    }

    /** Load chapters from assets/book_chapters.json (override for server books). Returns null if not found. */
    private List<Chapter> loadChaptersFromAssetsAsList(String bookFileName) {
        if (bookFileName == null || bookFileName.isEmpty() || pageCount <= 0) return null;
        try (InputStream is = getAssets().open("book_chapters.json")) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }
            JSONObject root = new JSONObject(sb.toString());
            String key = bookFileName.trim();
            if (!root.has(key)) {
                if (!key.endsWith(".pdf") && !key.endsWith(".PDF")) key = key + ".pdf";
                if (!root.has(key)) return null;
            }
            JSONArray arr = root.getJSONArray(key);
            if (arr == null || arr.length() == 0) return null;
            List<Chapter> out = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String title = o.optString("t", o.optString("title", ""));
                int page = o.optInt("p", o.optInt("page", 0));
                if (page < 0) page = 0;
                if (page >= pageCount) continue;
                out.add(new Chapter(title, page));
            }
            if (out.isEmpty()) return null;
            if (out.get(0).pageNumber > 0) out.add(0, new Chapter("અધ્યાય ૧", 0));
            return out;
        } catch (Throwable t) {
            Log.d(TAG, "No assets chapters for " + bookFileName, t);
            return null;
        }
    }

    /** Same as loadChaptersFromCache but returns a new list (for background loading). */
    private List<Chapter> loadChaptersFromCacheAsList(String bookFileName) {
        if (bookFileName == null || pageCount <= 0) return null;
        try {
            File cacheFile = BookChapterScanner.getCacheFile(this);
            if (cacheFile == null || !cacheFile.exists()) return null;
            String json = readFileToString(cacheFile);
            if (json == null || json.isEmpty()) return null;
            JSONObject root = new JSONObject(json);
            String cacheKey = bookFileName;
            if (!root.has(cacheKey)) {
                String withPdf = bookFileName.trim();
                if (!withPdf.endsWith(".pdf") && !withPdf.endsWith(".PDF")) withPdf = withPdf + ".pdf";
                if (root.has(withPdf)) cacheKey = withPdf;
                else if (bookFileName.endsWith(".pdf") && root.has(bookFileName.replace(".pdf", "").trim())) cacheKey = bookFileName.replace(".pdf", "").trim();
                else if (bookFileName.endsWith(".PDF") && root.has(bookFileName.replace(".PDF", "").trim())) cacheKey = bookFileName.replace(".PDF", "").trim();
            }
            if (!root.has(cacheKey)) return null;
            JSONArray arr = root.getJSONArray(cacheKey);
            List<Chapter> out = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String title = o.optString("t", o.optString("title", ""));
                int page = o.optInt("p", o.optInt("page", 0));
                if (page < 0) page = 0;
                if (page >= pageCount) continue;
                out.add(new Chapter(title, page));
            }
            if (out.isEmpty()) return null;
            if (out.get(0).pageNumber > 0) out.add(0, new Chapter("અધ્યાય ૧", 0));
            return out;
        } catch (Throwable t) {
            Log.d(TAG, "No cache for " + bookFileName, t);
            return null;
        }
    }

    private static String readFileToString(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new java.io.FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private void createDefaultChapters() {
        if (chapters == null) chapters = new ArrayList<>();
        chapters.clear();
        if (pageCount <= 0) return;
        int numChapters = Math.min(50, Math.max(5, (pageCount + 4) / 5));
        int chapterSize = Math.max(1, pageCount / numChapters);
        for (int i = 0; i < numChapters; i++) {
            int pageNum = i * chapterSize;
            if (pageNum >= pageCount) break;
            String title = "અધ્યાય " + toGujaratiNumeral(i + 1);
            chapters.add(new Chapter(title, pageNum));
        }
        if (chapters.isEmpty()) {
            chapters.add(new Chapter("અધ્યાય ૧", 0));
        }
    }

    /** Returns default chapter list without modifying this.chapters (for background use). */
    private List<Chapter> createDefaultChaptersList() {
        if (pageCount <= 0) return new ArrayList<>();
        List<Chapter> out = new ArrayList<>();
        int numChapters = Math.min(50, Math.max(5, (pageCount + 4) / 5));
        int chapterSize = Math.max(1, pageCount / numChapters);
        for (int i = 0; i < numChapters; i++) {
            int pageNum = i * chapterSize;
            if (pageNum >= pageCount) break;
            out.add(new Chapter("અધ્યાય " + toGujaratiNumeral(i + 1), pageNum));
        }
        if (out.isEmpty()) out.add(new Chapter("અધ્યાય ૧", 0));
        return out;
    }

    /** Load real adhyayas from downloaded PDF (server book) - runs in background, updates chapters on UI thread. */
    private void loadChaptersFromDownloadedFileAsync() {
        if (pdfFile == null || !pdfFile.exists() || pageCount <= 0 || chapters == null) return;
        final File file = pdfFile;
        final int totalPages = pageCount;
        new Thread(() -> {
            try {
                JSONArray arr = BookChapterScanner.getOutlineFromFile(PdfViewerActivity.this, file);
                if (arr == null || arr.length() == 0) return;
                List<Chapter> list = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    String title = o.optString("t", o.optString("title", ""));
                    int page = o.optInt("p", o.optInt("page", 0));
                    if (page < 0) page = 0;
                    if (page >= totalPages) continue;
                    list.add(new Chapter(title, page));
                }
                if (list.isEmpty()) return;
                runOnUiThread(() -> {
                    if (chapters != null && !isFinishing() && !isDestroyed()) {
                        chapters.clear();
                        chapters.addAll(list);
                        if (chapters.get(0).pageNumber > 0) {
                            chapters.add(0, new Chapter("અધ્યાય ૧", 0));
                        }
                    }
                });
            } catch (Throwable t) {
                Log.d(TAG, "Outline from downloaded file failed", t);
            }
        }).start();
    }

    private static String toGujaratiNumeral(int n) {
        if (n <= 0 || n > 999) return String.valueOf(n);
        String[] g = { "૦", "૧", "૨", "૩", "૪", "૫", "૬", "૭", "૮", "૯" };
        if (n < 10) return g[n];
        StringBuilder sb = new StringBuilder();
        int num = n;
        int div = 100;
        boolean started = false;
        while (div >= 1) {
            int d = num / div;
            num = num % div;
            div = div / 10;
            if (d > 0 || started) {
                sb.append(g[d]);
                started = true;
            }
        }
        return sb.length() > 0 ? sb.toString() : g[0];
    }
}
