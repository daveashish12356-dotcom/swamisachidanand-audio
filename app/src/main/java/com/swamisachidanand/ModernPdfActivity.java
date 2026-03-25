package com.swamisachidanand;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Modern in-app PDF reader with top bar, bottom progress,
 * day/night toggle, basic bookmarks and chapters.
 */
public class ModernPdfActivity extends AppCompatActivity {

    private static final String TAG = "ModernPdfActivity";
    private static final String KEY_CURRENT_PAGE = "current_page_index";
    private static final int MAX_BITMAP_WIDTH = 1200;
    private static final int MAX_BITMAP_HEIGHT = 1800;

    private static final String PREFS_NAME = "reading_progress";
    private static final String KEY_RECENT_BOOKS = "recent_books_list";
    private static final String KEY_BOOKMARKS_PREFIX = "pdf_bookmarks_";
    private static final int MAX_RECENT_BOOKS = 10;

    private String bookName;
    private String pdfUrl;
    private String thumbnailUrl;

    private File pdfFile;
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor fileDescriptor;

    private View pdfFrameLayout;
    private View pdfContentContainer;
    private View pdfPageCard;
    private ImageView pdfSinglePageImage;
    private View pdfDimOverlay;
    private ImageView pdfLoadingThumbnail;
    private android.widget.ProgressBar pdfLoadingProgress;
    private TextView pdfLoadingOnlineText;

    private LinearLayout bottomProgressBar;
    private SeekBar progressSeekBar;
    private TextView pagesLeftText;
    private TextView pageNumberText;
    private ImageButton chaptersMenuButton;
    private TextView progressWatermark;

    private View pdfTopBar;
    private ImageButton pdfBackBtn;
    private TextView pdfBookTitle;
    private TextView pdfPageIndicator;
    private ImageButton pdfSearchBtn;
    private ImageButton pdfBookmarkBtn;
    private ImageButton pdfTopMoreBtn;
    private ImageButton dayNightToggleBtn;

    private View pdfFloatingTools;
    private ImageButton pdfZoomInBtn;
    private ImageButton pdfZoomOutBtn;
    private ImageButton pdfJumpBtn;
    private ImageButton pdfShareBtn;

    private View textOverlayCard;
    private ScrollView textScrollView;
    private TextView extractedTextView;
    private ImageButton textFontSmallerBtn;
    private ImageButton textFontBiggerBtn;
    private android.widget.Button closeTextOverlayButton;

    // Keep original page scale by default (no zoom) to avoid clipping/cut.
    private float zoomLevel = 1.0f;
    private static final float ZOOM_MIN = 0.7f;
    private static final float ZOOM_MAX = 2.0f;

    private int pageCount = 0;
    private int currentPageIndex = 0;

    private boolean isDayMode = true;
    private boolean isControlsVisible = true;

    private GestureDetector gestureDetector;

    private SharedPreferences readingProgressPrefs;
    private final Set<Integer> bookmarkedPages = new HashSet<>();

    private android.os.Handler mainHandler;
    private Runnable autoHideRunnable;
    private static final int CONTROLS_ANIM_DURATION = 450;
    private static final int AUTO_HIDE_DELAY_MS = 5000;

    private final Object rendererLock = new Object();
    private volatile boolean rendering = false;

    private static class Chapter {
        String title;
        int pageNumber;
        Chapter(String title, int pageNumber) {
            this.title = title;
            this.pageNumber = pageNumber;
        }
    }

    private List<Chapter> chapters;

    /** PDF file ready on disk; rewarded flow finished → then openPdfSafely. */
    private volatile boolean pdfBytesReady = false;
    private volatile boolean rewardedFlowFinished = false;
    private boolean pdfRevealAttempted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_modern_pdf);

            bookName = getIntent().getStringExtra("book_name");
            pdfUrl = getIntent().getStringExtra("pdf_url");
            thumbnailUrl = getIntent().getStringExtra("thumbnail_url");
            if (pdfUrl != null) pdfUrl = pdfUrl.trim();
            if (thumbnailUrl != null && thumbnailUrl.trim().isEmpty()) thumbnailUrl = null;
            else if (thumbnailUrl != null) thumbnailUrl = thumbnailUrl.trim();
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

            bindViews();
            setupTopBar();
            setupBottomBar();
            setupFloatingTools();
            setupTextOverlay();
            setupGestures();

            mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            autoHideRunnable = () -> { if (!isFinishing() && !isDestroyed()) hideControls(); };
            if (pdfContentContainer != null && bottomProgressBar != null) {
                mainHandler.postDelayed(autoHideRunnable, AUTO_HIDE_DELAY_MS);
            }

            showLoadingOverlay();
            pdfContentContainer.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (pdfLoadingOnlineText != null) pdfLoadingOnlineText.setText("પ્રસ્તુત થાય છે...");
                startPdfDownloadInBackground();
                PdfRewardedAdHelper.loadAndShow(ModernPdfActivity.this, this::onRewardedFlowFinished);
            });
        } catch (Exception e) {
            Log.e(TAG, "Fatal error in onCreate", e);
            Toast.makeText(this, "App error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void bindViews() {
        pdfDimOverlay = findViewById(R.id.pdf_dim_overlay);
        pdfFrameLayout = findViewById(R.id.pdf_frame_layout);
        pdfContentContainer = findViewById(R.id.pdf_content_container);
        pdfPageCard = findViewById(R.id.pdf_page_card);
        pdfSinglePageImage = findViewById(R.id.pdf_single_page_image);
        pdfLoadingThumbnail = findViewById(R.id.pdf_loading_thumbnail);
        pdfLoadingProgress = findViewById(R.id.pdf_loading_progress);
        pdfLoadingOnlineText = findViewById(R.id.pdf_loading_online_text);

        bottomProgressBar = findViewById(R.id.bottom_progress_bar);
        progressSeekBar = findViewById(R.id.progress_seekbar);
        pagesLeftText = findViewById(R.id.pages_left_text);
        pageNumberText = findViewById(R.id.page_number_text);
        chaptersMenuButton = findViewById(R.id.chapters_menu_button);
        progressWatermark = findViewById(R.id.progress_watermark);

        pdfTopBar = findViewById(R.id.pdf_top_bar);
        pdfBackBtn = findViewById(R.id.pdf_back_btn);
        pdfBookTitle = findViewById(R.id.pdf_book_title);
        pdfPageIndicator = findViewById(R.id.pdf_page_indicator);
        pdfSearchBtn = findViewById(R.id.pdf_search_btn);
        pdfBookmarkBtn = findViewById(R.id.pdf_bookmark_btn);
        pdfTopMoreBtn = findViewById(R.id.pdf_top_more_btn);
        dayNightToggleBtn = findViewById(R.id.day_night_toggle_btn);

        // Floating tools (zoom/jump/share) removed from UI.
        pdfFloatingTools = null;
        pdfZoomInBtn = null;
        pdfZoomOutBtn = null;
        pdfJumpBtn = null;
        pdfShareBtn = null;

        textOverlayCard = findViewById(R.id.text_overlay_card);
        textScrollView = findViewById(R.id.text_scroll_view);
        extractedTextView = findViewById(R.id.extracted_text_view);
        textFontSmallerBtn = findViewById(R.id.text_font_smaller_btn);
        textFontBiggerBtn = findViewById(R.id.text_font_bigger_btn);
        closeTextOverlayButton = findViewById(R.id.close_text_overlay_button);

        if (pdfContentContainer == null || pdfSinglePageImage == null) {
            Toast.makeText(this, "Layout error.", Toast.LENGTH_LONG).show();
            finish();
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(0xFFF5F0E6);
            getWindow().setNavigationBarColor(0xFFF5F0E6);
        }
        getWindow().getDecorView().setBackgroundColor(0xFFF5F0E6);

        // Apply a slightly zoomed default so page feels more full-screen.
        // Don't auto-zoom; keep original page scale.
        applyZoom();

        // Enter immersive full-screen (hide system navigation/status bars).
        enterImmersiveMode();
    }

    private void setupTopBar() {
        String title = bookName != null ? bookName.replace(".pdf", "").replace(".PDF", "").trim() : "";
        if (pdfBookTitle != null) pdfBookTitle.setText(title);
        if (pdfBackBtn != null) pdfBackBtn.setOnClickListener(v -> finish());
        if (pdfSearchBtn != null) {
            pdfSearchBtn.setOnClickListener(v ->
                    Toast.makeText(this, "PDF શોધ જલ્દી ઉપલબ્ધ થશે", Toast.LENGTH_SHORT).show());
        }
        loadBookmarks();
        updateBookmarkButton();
        if (pdfBookmarkBtn != null) {
            pdfBookmarkBtn.setOnClickListener(v -> toggleBookmark());
        }

        syncDayNightFromSystem();
        updateDayNightToggleUi();
        applyDayNightMode();
        if (dayNightToggleBtn != null) {
            dayNightToggleBtn.setOnClickListener(v -> {
                isDayMode = !isDayMode;
                applyDayNightMode();
            });
        }
        if (pdfTopMoreBtn != null) pdfTopMoreBtn.setOnClickListener(v -> showPdfTopMoreMenu());
    }

    private void setupBottomBar() {
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
        if (chaptersMenuButton != null) chaptersMenuButton.setOnClickListener(v -> showChaptersDialog());
    }

    private void setupFloatingTools() {
        if (pdfZoomInBtn != null) pdfZoomInBtn.setOnClickListener(v -> zoomIn());
        if (pdfZoomOutBtn != null) pdfZoomOutBtn.setOnClickListener(v -> zoomOut());
        if (pdfJumpBtn != null) pdfJumpBtn.setOnClickListener(v -> showJumpToPageDialog());
        if (pdfShareBtn != null) pdfShareBtn.setOnClickListener(v -> shareCurrentPage());
    }

    private void setupTextOverlay() {
        if (extractedTextView != null) {
            float textSizePx = extractedTextView.getTextSize();
            textFontSizeSp = textSizePx / getResources().getDisplayMetrics().scaledDensity;
        }
        if (textFontSmallerBtn != null) {
            textFontSmallerBtn.setOnClickListener(v -> adjustTextFontSize(false));
        }
        if (textFontBiggerBtn != null) {
            textFontBiggerBtn.setOnClickListener(v -> adjustTextFontSize(true));
        }
        if (closeTextOverlayButton != null) {
            closeTextOverlayButton.setOnClickListener(v -> hideTextOverlay());
        }
    }

    private void setupGestures() {
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

    private void showLoadingOverlay() {
        if (pdfDimOverlay != null) {
            pdfDimOverlay.setVisibility(View.VISIBLE);
            pdfDimOverlay.setBackgroundColor(0xE6000000);
            pdfDimOverlay.bringToFront();
        }
        if (pdfLoadingOnlineText != null) {
            pdfLoadingOnlineText.setText("બુક લોડ થાય છે...");
            pdfLoadingOnlineText.setVisibility(View.VISIBLE);
        }
        if (pdfLoadingProgress != null) {
            pdfLoadingProgress.setVisibility(View.VISIBLE);
            pdfLoadingProgress.setAlpha(1f);
        }
        if (pdfLoadingProgress != null) pdfLoadingProgress.setVisibility(View.VISIBLE);
        if (pdfLoadingThumbnail != null) {
            pdfLoadingThumbnail.setVisibility(View.VISIBLE);
            if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                Glide.with(this).load(thumbnailUrl)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .placeholder(R.drawable.book_placeholder)
                        .error(R.drawable.book_placeholder)
                        .centerCrop()
                        .into(pdfLoadingThumbnail);
            } else {
                pdfLoadingThumbnail.setImageResource(R.drawable.book_placeholder);
            }
        }
    }

    private void hideLoadingOverlay() {
        runOnUiThread(() -> {
            if (pdfLoadingProgress != null) pdfLoadingProgress.setVisibility(View.GONE);
            if (pdfLoadingOnlineText != null) pdfLoadingOnlineText.setVisibility(View.GONE);
            if (pdfLoadingThumbnail != null) pdfLoadingThumbnail.setVisibility(View.GONE);
            if (pdfDimOverlay != null) pdfDimOverlay.setVisibility(View.GONE);
        });
    }

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
                    .withEndAction(() -> {
                        topBar.setVisibility(View.GONE);
                        topBar.setAlpha(1f);
                        topBar.setTranslationY(0f);
                    })
                    .start();
        }
        // Floating tools removed from design – nothing to hide here.
        if (bottomBar != null && bottomBar.getVisibility() == View.VISIBLE) {
            bottomBar.animate()
                    .alpha(0f)
                    .translationY(bottomBar.getHeight())
                    .setDuration(CONTROLS_ANIM_DURATION)
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
                        .start();
            });
        }
        // Floating tools removed from design – nothing to show here.
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

    private static final int PAGE_SLIDE_DURATION_MS = 220;
    private static final int PAGE_SLIDE_IN_DURATION_MS = 200;

    private void animatePageTurn(boolean forward, Runnable onMidTurn) {
        if (pdfSinglePageImage == null) {
            if (onMidTurn != null) onMidTurn.run();
            return;
        }
        try {
            final View v = pdfSinglePageImage;
            v.animate().cancel();
            v.setAlpha(1f);
            v.setTranslationX(0f);

            float density = getResources().getDisplayMetrics().density;
            float travel = 40f * density;
            float exitX = forward ? -travel : travel;
            float enterX = -exitX;

            v.animate()
                    .translationX(exitX)
                    .alpha(0.0f)
                    .setDuration(PAGE_SLIDE_DURATION_MS)
                    .withEndAction(() -> {
                        if (onMidTurn != null) onMidTurn.run();
                        v.setTranslationX(enterX);
                        v.setAlpha(0.0f);
                        v.animate()
                                .translationX(0f)
                                .alpha(1.0f)
                                .setDuration(PAGE_SLIDE_IN_DURATION_MS)
                                .start();
                    })
                    .start();
        } catch (Throwable t) {
            if (onMidTurn != null) onMidTurn.run();
        }
    }

    private static final String CACHE_PDF_FILENAME = "current_book.pdf";

    private void startPdfDownloadInBackground() {
        if (pdfUrl != null && !pdfUrl.isEmpty()) loadPdfFromUrl(pdfUrl);
        else loadPdfFromAssets();
    }

    private void onRewardedFlowFinished() {
        rewardedFlowFinished = true;
        tryRevealPdfIfReady();
    }

    private void tryRevealPdfIfReady() {
        if (isFinishing() || isDestroyed()) return;
        if (pdfRevealAttempted) return;
        if (!pdfBytesReady || !rewardedFlowFinished) return;
        pdfRevealAttempted = true;
        openPdfSafely();
    }

    private void markPdfBytesReady() {
        pdfBytesReady = true;
        tryRevealPdfIfReady();
    }

    private void loadPdfFromUrl(String url) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(180, TimeUnit.SECONDS)
                        .build();
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", "Mozilla/5.0 (Android) SwamiApp")
                        .addHeader("Accept", "application/pdf,*/*")
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
                    File cacheDir = getCacheDir();
                    if (cacheDir == null) {
                        runOnUiThread(() -> { hideLoadingOverlay(); finish(); });
                        return;
                    }
                    pdfFile = new File(cacheDir, CACHE_PDF_FILENAME);
                    try (InputStream in = response.body().byteStream();
                         FileOutputStream out = new FileOutputStream(pdfFile)) {
                        byte[] buf = new byte[65536];
                        int n;
                        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                    }
                    runOnUiThread(this::markPdfBytesReady);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading PDF from URL", e);
                runOnUiThread(() -> {
                    hideLoadingOverlay();
                    Toast.makeText(this, "બુક લોડ થઈ નહીં.", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    private void loadPdfFromAssets() {
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
                File tempFile = new File(cacheDir, "temp_book_" + System.currentTimeMillis() + ".pdf");
                pdfFile = new File(cacheDir, CACHE_PDF_FILENAME);
                outputStream = new FileOutputStream(tempFile);
                byte[] buffer = new byte[16384];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.close();
                inputStream.close();
                outputStream = null;
                inputStream = null;
                if (tempFile.exists() && tempFile.length() > 0) {
                    try (InputStream in = new java.io.FileInputStream(tempFile);
                         FileOutputStream out = new FileOutputStream(pdfFile)) {
                        byte[] buf = new byte[16384];
                        int n;
                        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                    }
                }
                tempFile.delete();
                runOnUiThread(this::markPdfBytesReady);
            } catch (Throwable e) {
                Log.e(TAG, "Error loading PDF: " + assetName, e);
                try { if (outputStream != null) outputStream.close(); } catch (IOException ignored) {}
                try { if (inputStream != null) inputStream.close(); } catch (IOException ignored) {}
                runOnUiThread(() -> {
                    hideLoadingOverlay();
                    Toast.makeText(this, "બુક લોડ થતાં ભૂલ.", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    private void openPdfSafely() {
        if (isFinishing() || isDestroyed()) return;
        try {
            openPdf();
        } catch (Throwable e) {
            Log.e(TAG, "Error opening PDF", e);
            hideLoadingOverlay();
            Toast.makeText(this, "બુક ખોલતાં ભૂલ.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void openPdf() throws IOException {
        if (pdfFile == null || !pdfFile.exists()) {
            hideLoadingOverlay();
            Toast.makeText(this, "PDF file not found", Toast.LENGTH_LONG).show();
            throw new IOException("PDF missing");
        }
        try {
            fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (IOException e) {
            hideLoadingOverlay();
            Toast.makeText(this, "બુક ખોલી શકાઈ નહીં.", Toast.LENGTH_LONG).show();
            throw e;
        }
        if (fileDescriptor == null) {
            hideLoadingOverlay();
            throw new IOException("fileDescriptor null");
        }
        try {
            pdfRenderer = new PdfRenderer(fileDescriptor);
        } catch (IOException e) {
            try { fileDescriptor.close(); } catch (IOException ignored) {}
            hideLoadingOverlay();
            Toast.makeText(this, "બુક ફોરમેટ ભૂલ.", Toast.LENGTH_LONG).show();
            throw e;
        }
        pageCount = pdfRenderer.getPageCount();
        Log.d(TAG, "openPdf: pageCount=" + pageCount + " currentPageIndex=" + currentPageIndex + " pdfName=" + bookName);
        if (pageCount <= 0) {
            pdfRenderer.close();
            hideLoadingOverlay();
            Toast.makeText(this, "બુકમાં પાનાં નથી.", Toast.LENGTH_LONG).show();
            throw new IOException("No pages");
        }
        if (currentPageIndex >= pageCount) currentPageIndex = pageCount - 1;
        if (currentPageIndex < 0) currentPageIndex = 0;

        hideLoadingOverlay();
        if (progressSeekBar != null && pageCount > 0) {
            progressSeekBar.setMax(100);
        }
        if (!loadChaptersFromCache(bookName)) {
            createDefaultChapters();
        }
        updateBottomProgressBar();
        renderCurrentPage();
    }

    private void renderCurrentPage() {
        renderCurrentPage(null);
    }

    private void renderCurrentPage(Runnable afterSetBitmap) {
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
        } catch (Exception ignored) {}
        final int finalMaxW = maxW;
        final int finalMaxH = maxH;
        final Runnable runAfter = afterSetBitmap;
        rendering = true;
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
                        Log.d(TAG, "render pageIndex=" + pageIndex + " rawW=" + w + " rawH=" + h + " maxW=" + finalMaxW + " maxH=" + finalMaxH);
                        if (w <= 0 || h <= 0) {
                            if (page != null) page.close();
                            runOnUiThread(() -> rendering = false);
                            return;
                        }
                        float scale = Math.min((float) finalMaxW / w, (float) finalMaxH / h);
                        int bw = Math.max(1, (int) (w * scale));
                        int bh = Math.max(1, (int) (h * scale));
                        Log.d(TAG, "render bitmapW=" + bw + " bitmapH=" + bh + " scale=" + scale);
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
                    Log.e(TAG, "render: bitmap null/finishing pageIndex=" + pageIndex);
                    runOnUiThread(() -> rendering = false);
                    return;
                }
                // Show original rendered page as-is (no auto-crop / reflow / filters)
                final Bitmap toShow = bitmap;
                runOnUiThread(() -> {
                    rendering = false;
                    if (isFinishing() || isDestroyed() || pdfSinglePageImage == null) return;
                    try {
                        Log.d(TAG, "setImageBitmap for pageIndex=" + pageIndex + " bmp=" + (toShow != null ? (toShow.getWidth() + "x" + toShow.getHeight()) : "null"));
                        pdfSinglePageImage.setImageBitmap(toShow);
                        if (runAfter != null) runAfter.run();
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
            ColorMatrix m = new ColorMatrix(new float[]{
                    -1, 0, 0, 0, 255,
                    0, -1, 0, 0, 255,
                    0, 0, -1, 0, 255,
                    0, 0, 0, 1, 0
            });
            Paint p = new Paint();
            p.setColorFilter(new ColorMatrixColorFilter(m));
            c.drawBitmap(original, 0, 0, p);
            return out;
        } catch (Exception e) {
            return original;
        }
    }

    /**
     * Roughly detect outer white margins and crop them so that
     * the visible text area fills more of the screen.
     */
    private Bitmap cropPageMarginsIfNeeded(Bitmap src) {
        if (src == null || src.isRecycled()) return src;
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= 4 || h <= 4) return src;

        int left = 0, right = w - 1, top = 0, bottom = h - 1;
        int bgThreshold = 245; // consider near‑white as margin

        // find left
        outer:
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y += 4) {
                int c = src.getPixel(x, y);
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;
                if (r < bgThreshold || g < bgThreshold || b < bgThreshold) {
                    left = x;
                    break outer;
                }
            }
        }

        // find right
        outer:
        for (int x = w - 1; x >= 0; x--) {
            for (int y = 0; y < h; y += 4) {
                int c = src.getPixel(x, y);
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;
                if (r < bgThreshold || g < bgThreshold || b < bgThreshold) {
                    right = x;
                    break outer;
                }
            }
        }

        // find top
        outer:
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x += 4) {
                int c = src.getPixel(x, y);
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;
                if (r < bgThreshold || g < bgThreshold || b < bgThreshold) {
                    top = y;
                    break outer;
                }
            }
        }

        // find bottom
        outer:
        for (int y = h - 1; y >= 0; y--) {
            for (int x = 0; x < w; x += 4) {
                int c = src.getPixel(x, y);
                int r = (c >> 16) & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = c & 0xFF;
                if (r < bgThreshold || g < bgThreshold || b < bgThreshold) {
                    bottom = y;
                    break outer;
                }
            }
        }

        // safety: ensure crop rect is valid and removes at least a bit of margin
        int minCrop = 4;
        if (right - left <= minCrop || bottom - top <= minCrop) {
            return src;
        }
        try {
            return Bitmap.createBitmap(src, left, top, right - left + 1, bottom - top + 1);
        } catch (IllegalArgumentException e) {
            return src;
        }
    }

    private void updateBottomProgressBar() {
        if (isFinishing() || isDestroyed() || pageCount <= 0
                || progressSeekBar == null || pageNumberText == null || pagesLeftText == null) return;
        try {
            int percentage = (int) (((currentPageIndex + 1) * 100.0) / pageCount);
            progressSeekBar.setProgress(percentage);
            int pagesLeft = pageCount - (currentPageIndex + 1);
            pagesLeftText.setText(pagesLeft + " પાનાં બાકી");
            String pageStr = "Page " + (currentPageIndex + 1) + " / " + pageCount;
            pageNumberText.setText((currentPageIndex + 1) + " / " + pageCount);
            if (pdfPageIndicator != null) pdfPageIndicator.setText(pageStr);
            if (progressWatermark != null) progressWatermark.setText(percentage + "%");
            updateBookmarkButton();
        } catch (Exception e) {
            Log.e(TAG, "updateBottomProgressBar error", e);
        }
    }

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
        enterImmersiveMode();
    }

    private void applyDayNightMode() {
        updateDayNightToggleUi();
        if (pdfFrameLayout != null) {
            if (isDayMode) {
                pdfFrameLayout.setBackgroundResource(R.drawable.bg_pdf_reader_gradient);
            } else {
                pdfFrameLayout.setBackgroundColor(Color.BLACK);
            }
        }
        int bgColor = isDayMode ? 0xFFF5F0E6 : Color.BLACK;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(bgColor);
            getWindow().setNavigationBarColor(bgColor);
        }
        getWindow().getDecorView().setBackgroundColor(bgColor);
        if (bottomProgressBar != null) {
            bottomProgressBar.setBackgroundColor(isDayMode ? 0xFFFAF9F6 : 0xFF2D2D2D);
        }
        renderCurrentPage();

        // Keep immersive mode even when theme changes.
        enterImmersiveMode();
    }

    private float textFontSizeSp = 16f;

    private void adjustTextFontSize(boolean bigger) {
        if (extractedTextView == null) return;
        float delta = bigger ? 2f : -2f;
        textFontSizeSp = Math.max(12f, Math.min(26f, textFontSizeSp + delta));
        extractedTextView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, textFontSizeSp);
    }

    private void zoomIn() {
        if (zoomLevel >= ZOOM_MAX) return;
        zoomLevel = Math.min(ZOOM_MAX, zoomLevel + 0.25f);
        applyZoom();
    }

    private void zoomOut() {
        if (zoomLevel <= ZOOM_MIN) return;
        zoomLevel = Math.max(ZOOM_MIN, zoomLevel - 0.25f);
        applyZoom();
    }

    private void applyZoom() {
        if (pdfSinglePageImage == null) return;
        pdfSinglePageImage.post(() -> {
            if (pdfSinglePageImage == null) return;
            int w = pdfSinglePageImage.getWidth();
            int h = pdfSinglePageImage.getHeight();
            if (w > 0 && h > 0) {
                pdfSinglePageImage.setPivotX(w / 2f);
                pdfSinglePageImage.setPivotY(h / 2f);
            }
            pdfSinglePageImage.setScaleX(zoomLevel);
            pdfSinglePageImage.setScaleY(zoomLevel);
        });
    }

    private void updateDayNightToggleUi() {
        if (dayNightToggleBtn != null) {
            dayNightToggleBtn.setImageResource(isDayMode ? R.drawable.ic_toggle_on : R.drawable.ic_toggle_off);
        }
    }

    private void showPdfTopMoreMenu() {
        if (pdfTopMoreBtn == null) return;
        android.widget.PopupMenu p = new android.widget.PopupMenu(this, pdfTopMoreBtn);
        p.getMenu().add(0, 1, 0, "અધ્યાયો");
        p.getMenu().add(0, 2, 0, "Reading progress");
        p.getMenu().add(0, 3, 0, "Jump to page...");
        p.getMenu().add(0, 4, 0, "Share page");
        p.getMenu().add(0, 5, 0, "Saved bookmarks");
        p.getMenu().add(0, 6, 0, "ટેક્સ્ટ મોડ");
        p.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) { showChaptersDialog(); return true; }
            if (id == 2) { showProgressDialog(); return true; }
            if (id == 3) { showJumpToPageDialog(); return true; }
            if (id == 4) { shareCurrentPage(); return true; }
            if (id == 5) { showBookmarksList(); return true; }
            if (id == 6) { toggleTextMode(); return true; }
            return false;
        });
        p.show();
    }

    private void showJumpToPageDialog() {
        if (pageCount <= 0) return;
        android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("1 - " + pageCount);
        input.setText(String.valueOf(currentPageIndex + 1));
        new AlertDialog.Builder(this)
                .setTitle("Jump to page")
                .setView(input)
                .setPositiveButton("Go", (d, w) -> {
                    try {
                        int pNum = Integer.parseInt(input.getText().toString().trim());
                        if (pNum >= 1 && pNum <= pageCount) {
                            currentPageIndex = pNum - 1;
                            saveReadingProgress();
                            updateBottomProgressBar();
                            renderCurrentPage();
                        } else {
                            Toast.makeText(this, "1 - " + pageCount + " દાખલ કરો", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "અમાન્ય પાનું", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void shareCurrentPage() {
        if (bookName == null || pageCount <= 0) return;
        String shareText = (bookName.replace(".pdf", "").replace(".PDF", "")) +
                " – Page " + (currentPageIndex + 1) + " / " + pageCount;
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(share, "Share"));
    }

    private void toggleTextMode() {
        if (textOverlayCard == null) return;
        if (textOverlayCard.getVisibility() == View.VISIBLE) {
            hideTextOverlay();
        } else {
            loadTextOverlay();
        }
    }

    private void hideTextOverlay() {
        if (textOverlayCard != null) textOverlayCard.setVisibility(View.GONE);
        if (pdfPageCard != null) pdfPageCard.setVisibility(View.VISIBLE);
        if (bottomProgressBar != null) bottomProgressBar.setVisibility(View.VISIBLE);
        if (pdfFloatingTools != null) pdfFloatingTools.setVisibility(View.VISIBLE);
    }

    private void loadTextOverlay() {
        if (extractedTextView == null || textOverlayCard == null) {
            Toast.makeText(this, "આ બુક માટે ટેક્સ્ટ ઉપલબ્ધ નથી.", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            String text = null;
            try {
                // First, try to extract flowing text directly from the current PDF file.
                if (pdfFile != null && pdfFile.exists()) {
                    PDDocument document = null;
                    try {
                        document = PDDocument.load(pdfFile);
                        PDFTextStripper stripper = new PDFTextStripper();
                        // Set a simple title header so it feels like a reader.
                        stripper.setStartPage(1);
                        stripper.setEndPage(document.getNumberOfPages());
                        text = stripper.getText(document);
                    } finally {
                        if (document != null) {
                            try { document.close(); } catch (IOException ignored) {}
                        }
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "Failed to extract text from PDF", t);
            }

            // Fallback: look for pre-generated text assets if any.
            if (text == null || text.trim().isEmpty()) {
                try {
                    if (bookName != null) {
                        String baseName = bookName.replace(".pdf", "").replace(".PDF", "").trim();
                        String[] candidates = new String[] {
                                baseName + "_text.txt",
                                baseName + ".txt",
                                "text/" + baseName + ".txt"
                        };
                        for (String candidate : candidates) {
                            try (InputStream in = getAssets().open(candidate);
                                 BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                                StringBuilder sb = new StringBuilder();
                                String line;
                                while ((line = r.readLine()) != null) sb.append(line).append('\n');
                                text = sb.toString();
                                if (!text.trim().isEmpty()) break;
                            } catch (IOException ignored) {}
                        }
                    }
                } catch (Throwable ignored) {}
            }

            final String finalText = text;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (finalText == null || finalText.trim().isEmpty()) {
                    Toast.makeText(this, "આ બુક માટે ટેક્સ્ટ ઉપલબ્ધ નથી.", Toast.LENGTH_SHORT).show();
                    return;
                }
                extractedTextView.setText(finalText.trim());
                if (textScrollView != null) textScrollView.scrollTo(0, 0);
                if (pdfPageCard != null) pdfPageCard.setVisibility(View.GONE);
                if (bottomProgressBar != null) bottomProgressBar.setVisibility(View.GONE);
                if (pdfFloatingTools != null) pdfFloatingTools.setVisibility(View.GONE);
                textOverlayCard.setVisibility(View.VISIBLE);
            });
        }).start();
    }

    private void loadBookmarks() {
        bookmarkedPages.clear();
        if (readingProgressPrefs == null || bookName == null) return;
        String s = readingProgressPrefs.getString(KEY_BOOKMARKS_PREFIX + bookName, "");
        if (s.isEmpty()) return;
        for (String p : s.split(",")) {
            try {
                bookmarkedPages.add(Integer.parseInt(p.trim()));
            } catch (NumberFormatException ignored) {}
        }
    }

    private void saveBookmarks() {
        if (readingProgressPrefs == null || bookName == null) return;
        StringBuilder sb = new StringBuilder();
        for (Integer p : bookmarkedPages) {
            if (sb.length() > 0) sb.append(",");
            sb.append(p);
        }
        readingProgressPrefs.edit()
                .putString(KEY_BOOKMARKS_PREFIX + bookName, sb.toString())
                .apply();
    }

    private void updateBookmarkButton() {
        if (pdfBookmarkBtn == null) return;
        boolean bookmarked = bookmarkedPages.contains(currentPageIndex);
        pdfBookmarkBtn.setImageResource(
                bookmarked ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off
        );
    }

    private void toggleBookmark() {
        if (pageCount <= 0) return;
        int page = currentPageIndex;
        if (bookmarkedPages.contains(page)) {
            bookmarkedPages.remove(page);
            Toast.makeText(this, "Bookmark દૂર", Toast.LENGTH_SHORT).show();
        } else {
            bookmarkedPages.add(page);
            Toast.makeText(this, "પાનું બુકમાર્ક થયું", Toast.LENGTH_SHORT).show();
        }
        saveBookmarks();
        updateBookmarkButton();
    }

    private void showBookmarksList() {
        if (bookmarkedPages.isEmpty()) {
            Toast.makeText(this, "હજુ બુકમાર્ક નથી", Toast.LENGTH_SHORT).show();
            return;
        }
        List<Integer> sorted = new ArrayList<>(bookmarkedPages);
        java.util.Collections.sort(sorted);
        List<String> labels = new ArrayList<>();
        for (Integer p : sorted) labels.add("Page " + (p + 1));
        android.widget.ListView listView = new android.widget.ListView(this);
        android.widget.ArrayAdapter<String> adapter =
                new android.widget.ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);
        listView.setAdapter(adapter);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Saved bookmarks")
                .setView(listView)
                .setNegativeButton("બંધ કરો", null)
                .create();
        listView.setOnItemClickListener((parent, view, position, id) -> {
            currentPageIndex = sorted.get(position);
            saveReadingProgress();
            updateBottomProgressBar();
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
            int estimatedMinutes = Math.max(1, (int) Math.round(pagesRemaining * 1.5));
            String message = "Reading Progress:\n\nPages Read: " + pagesRead + " / " + pageCount +
                    "\nPages Remaining: " + pagesRemaining +
                    "\nProgress: " + percentage + "%" +
                    "\nEstimated time left: " + estimatedMinutes + " min";
            new AlertDialog.Builder(this)
                    .setTitle("Reading Progress")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show();
        } else {
            Toast.makeText(this, "Progress not available", Toast.LENGTH_SHORT).show();
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
            RecentActivityHelper.saveActivity(this, RecentActivityHelper.TYPE_BOOK, fileName);
        } catch (Exception e) {
            Log.e(TAG, "addToRecentBooks error", e);
        }
    }

    private boolean loadChaptersFromCache(String bookFileName) {
        if (chapters == null) chapters = new ArrayList<>();
        chapters.clear();
        if (bookFileName == null || pageCount <= 0) return false;

        // 1) Try original curated chapters from assets/book_chapters.json (what you had earlier).
        if (loadChaptersFromAssetsJson(bookFileName)) {
            return true;
        }

        // 2) Fall back to auto‑scanned cache generated by BookChapterScanner.
        try {
            File cacheFile = BookChapterScanner.getCacheFile(this);
            if (cacheFile == null || !cacheFile.exists()) return false;
            String json = readFileToString(cacheFile);
            if (json == null || json.isEmpty()) return false;
            JSONObject root = new JSONObject(json);
            if (!root.has(bookFileName)) return false;
            JSONArray arr = root.getJSONArray(bookFileName);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String title = o.optString("t", o.optString("title", ""));
                int page = o.optInt("p", o.optInt("page", 0));
                if (page < 0) page = 0;
                if (page >= pageCount) continue;
                chapters.add(new Chapter(title, page));
            }
            return !chapters.isEmpty();
        } catch (Throwable t) {
            Log.d(TAG, "No cache or no entry for " + bookFileName, t);
            return false;
        }
    }

    private boolean loadChaptersFromAssetsJson(String bookFileName) {
        try {
            InputStream in = getAssets().open("book_chapters.json");
            String json;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
                json = sb.toString();
            }
            if (json == null || json.isEmpty()) return false;
            JSONObject root = new JSONObject(json);

            // Try different possible keys so we match how the JSON was authored.
            String key1 = bookFileName;
            String base = bookFileName.replace(".pdf", "").replace(".PDF", "");
            String key2 = base + ".pdf";
            String key3 = base + ".PDF";

            JSONArray arr = null;
            if (root.has(key1)) arr = root.getJSONArray(key1);
            else if (root.has(key2)) arr = root.getJSONArray(key2);
            else if (root.has(key3)) arr = root.getJSONArray(key3);

            if (arr == null) return false;

            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String title = o.optString("t", o.optString("title", ""));
                int page = o.optInt("p", o.optInt("page", 0));
                if (page < 0) page = 0;
                if (page >= pageCount) continue;
                chapters.add(new Chapter(title, page));
            }
            return !chapters.isEmpty();
        } catch (Throwable t) {
            Log.d(TAG, "No assets book_chapters.json entry for " + bookFileName, t);
            return false;
        }
    }

    private void showChaptersDialog() {
        if (chapters == null || chapters.isEmpty()) {
            Toast.makeText(this, "અધ્યાયોની માહિતી ઉપલબ્ધ નથી.", Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> titles = new ArrayList<>();
        for (Chapter c : chapters) {
            String t = c.title != null && !c.title.trim().isEmpty()
                    ? c.title.trim()
                    : "પાનું " + (c.pageNumber + 1);
            titles.add(t);
        }
        android.widget.ListView listView = new android.widget.ListView(this);
        android.widget.ArrayAdapter<String> adapter =
                new android.widget.ArrayAdapter<>(this, android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(adapter);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("અધ્યાયો")
                .setView(listView)
                .setNegativeButton("બંધ કરો", null)
                .create();
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < chapters.size()) {
                currentPageIndex = chapters.get(position).pageNumber;
                saveReadingProgress();
                updateBottomProgressBar();
                renderCurrentPage();
            }
            dialog.dismiss();
        });
        dialog.show();
    }

    private static String readFileToString(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(new java.io.FileInputStream(file), StandardCharsets.UTF_8))) {
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

    private void saveReadingProgress() {
        if (readingProgressPrefs == null || bookName == null) return;
        readingProgressPrefs.edit().putInt(bookName + "_page", currentPageIndex).apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pdfRenderer != null) {
            try { pdfRenderer.close(); } catch (Exception ignored) {}
            pdfRenderer = null;
        }
        if (fileDescriptor != null) {
            try { fileDescriptor.close(); } catch (IOException ignored) {}
            fileDescriptor = null;
        }
    }

    private void enterImmersiveMode() {
        try {
            View decorView = getWindow().getDecorView();
            if (Build.VERSION.SDK_INT >= 30) {
                WindowInsetsController controller = decorView.getWindowInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    controller.setSystemBarsBehavior(
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                int flags = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                decorView.setSystemUiVisibility(flags);
            }
        } catch (Throwable t) {
            Log.e(TAG, "enterImmersiveMode failed", t);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_PAGE, currentPageIndex);
    }
}

