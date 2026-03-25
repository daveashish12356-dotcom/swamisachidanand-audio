package com.swamisachidanand;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;

import java.util.ArrayDeque;
import java.util.Deque;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int SCROLL_THRESHOLD_PX = 8;
    private static final int NAV_ANIM_DURATION_MS = 220;
    private static final int REQUEST_APP_UPDATE = 101;

    private AppUpdateManager appUpdateManager;

    private BottomNavigationView bottomNavigation;
    private View bottomNavContainer;
    private boolean bottomNavVisible = false;
    private boolean navAnimating = false;
    private final Deque<Integer> tabBackStack = new ArrayDeque<>();
    private int currentTabId = R.id.nav_home;
    private boolean suppressTabBackStackPush = false;
    @Nullable
    private InteractiveAppTourHelper interactiveTour;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // સામાન્ય વિન્ડો: સ્ટેટસ બાર / નીચેનું નૅવ બાર — કન્ટેન્ટ પાછળ નહીં (ફુલસ્ક્રીન નહીં)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        setContentView(R.layout.activity_main);

        // PDF chapter scan ભારી છે — શરૂઆત પછી થોડી વારે બેકગ્રાઉન્ડમાં ચલાવો જેથી UI સ્મૂથ રહે
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> BookChapterScanner.scanAllAndSave(getApplicationContext()), 2800);

        // Android 13+ par suvichar notification ke liye permission maango
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != 0) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 2000);
            }
        }

        // In-App Update: Immediate — app kholte hi update mandatory; ek tap "Update" → download → auto restart
        initImmediateAppUpdate();

        // Pehle fragment turant load karo — white screen na dikhe (commitNow = sync)
        if (savedInstanceState == null) {
            try {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commitNow();
            } catch (Throwable t) {
                Log.e(TAG, "Error loading HomeFragment", t);
                // Fallback: use async commit
                try {
                    getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
                } catch (Throwable t2) {
                    Log.e(TAG, "Error loading HomeFragment (async)", t2);
                }
            }
        }

        bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavContainer = bottomNavigation; // flat nav – container = view itself
        if (bottomNavigation != null) {
            // Icons apni intrinsic color dikhayen (Books = #F9A825, #FBC02D) – tint null
            bottomNavigation.setItemIconTintList(null);
            try {
                bottomNavigation.setOnItemSelectedListener(item -> {
                    try {
                        Fragment selectedFragment = null;
                        int itemId = item.getItemId();

                        // User pravachan tab se bahar ja raha hai -> active PravachanPlayerFragment ko STOP/Pause karo.
                        // Player fragment backstack me reh sakta hai, isliye sirf fragment_container ka current fragment check karna enough nahi hota.
                        if (itemId != R.id.nav_pravachan) {
                            try {
                                for (Fragment f : getSupportFragmentManager().getFragments()) {
                                    if (f instanceof PravachanPlayerFragment) {
                                        ((PravachanPlayerFragment) f).pausePlayback();
                                    }
                                }
                            } catch (Throwable t) {
                                Log.e(TAG, "pause all pravachan players failed", t);
                            }
                        }

                        if (itemId == R.id.nav_home) selectedFragment = new HomeFragment();
                        else if (itemId == R.id.nav_books) selectedFragment = new BooksFragment();
                        else if (itemId == R.id.nav_audio) {
                            selectedFragment = new ServerAudioFragment();
                        } else if (itemId == R.id.nav_pravachan) {
                            AudioPravachanFragment frag = new AudioPravachanFragment();
                            try {
                                if (pendingPravachanStartItem != null) {
                                    Bundle b = new Bundle();
                                    b.putParcelable(AudioPravachanFragment.ARG_START_ITEM, pendingPravachanStartItem);
                                    frag.setArguments(b);
                                    pendingPravachanStartItem = null;
                                }
                            } catch (Throwable t) {
                                // Ignore args issues; still open Pravachan page.
                            }
                            selectedFragment = frag;
                        } else if (itemId == R.id.nav_videos) selectedFragment = new VideosFragment();
                        else if (itemId == R.id.nav_about) selectedFragment = new AboutFragment();
                        if (selectedFragment != null && !isFinishing()) {
                            if (!suppressTabBackStackPush && itemId != currentTabId) {
                                tabBackStack.push(currentTabId);
                            }
                            getSupportFragmentManager().beginTransaction()
                                .setCustomAnimations(R.anim.fragment_fade_in, R.anim.fragment_fade_out)
                                .replace(R.id.fragment_container, selectedFragment)
                                .commit();
                            currentTabId = itemId;
                            // Home page par hide, baaki sab page par show (immediate)
                            setBottomNavVisible(itemId != R.id.nav_home);
                            return true;
                        }
                    } catch (Throwable t) {
                        Log.e(TAG, "nav click", t);
                    }
                    return false;
                });
            } catch (Throwable t) {
                Log.e(TAG, "setOnItemSelectedListener", t);
            }
            // Start me Home dikh raha hai, isliye Home par nav bar hide
            setBottomNavVisible(false);
            bottomNavVisible = false;
        }

        // Handle intent: open Books tab with filter (from Book Store section tap) or other target tab
        try {
            Intent intent = getIntent();
            if (intent != null) {
                int tabId = intent.getIntExtra(EXTRA_TARGET_TAB, 0);
                String booksFilterId = intent.getStringExtra(EXTRA_BOOKS_FILTER_ID);
                if (tabId == R.id.nav_books && booksFilterId != null && !booksFilterId.isEmpty()) {
                    Fragment f = new BooksFragment();
                    Bundle args = new Bundle();
                    args.putString(BooksFragment.ARG_FILTER_ID, booksFilterId);
                    f.setArguments(args);
                    getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fragment_fade_in, R.anim.fragment_fade_out)
                        .replace(R.id.fragment_container, f)
                        .commit();
                    if (bottomNavigation != null) bottomNavigation.setSelectedItemId(R.id.nav_books);
                    setBottomNavVisible(true);
                    intent.removeExtra(EXTRA_BOOKS_FILTER_ID);
                } else if (tabId != 0) {
                    switchToTab(tabId);
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "handle initial EXTRA_TARGET_TAB", t);
        }

        // First-run interactive tour (Splash passes EXTRA_INTERACTIVE_TOUR)
        try {
            if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_INTERACTIVE_TOUR, false)) {
                getIntent().removeExtra(EXTRA_INTERACTIVE_TOUR);
                getWindow().getDecorView().postDelayed(() -> {
                    if (!isFinishing()) {
                        launchInteractiveTour();
                    }
                }, 550);
            }
        } catch (Throwable t) {
            Log.e(TAG, "interactive tour intent", t);
        }

    }

    /** Immediate update: app kholte hi update zaroori — 1 tap Update, phir download+install+restart apne aap. */
    private void initImmediateAppUpdate() {
        try {
            appUpdateManager = AppUpdateManagerFactory.create(this);
            appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                AppUpdateType.IMMEDIATE,
                                this,
                                REQUEST_APP_UPDATE);
                    } catch (Exception e) {
                        Log.e(TAG, "startUpdateFlowForResult failed", e);
                    }
                }
            }).addOnFailureListener(e -> Log.w(TAG, "App update check failed", e));
        } catch (Throwable t) {
            Log.w(TAG, "initImmediateAppUpdate", t);
        }
    }

    /** First launch: real tabs + spotlight on bottom nav (see SplashActivity). */
    public static final String EXTRA_INTERACTIVE_TOUR = "interactive_tour";

    public static final String EXTRA_TARGET_TAB = "target_tab";
    /** Open Books tab with this category filter (e.g. "new"). Passed to BooksFragment via arguments. */
    public static final String EXTRA_BOOKS_FILTER_ID = "books_filter_id";

    // Used by Home page "Latest Pravachan" section to auto-open + start the same item.
    private static PravachanItem pendingPravachanStartItem;

    public static void queuePravachanStart(PravachanItem item) {
        pendingPravachanStartItem = item;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        try {
            if (intent == null) return;
            int tabId = intent.getIntExtra(EXTRA_TARGET_TAB, 0);
            String booksFilterId = intent.getStringExtra(EXTRA_BOOKS_FILTER_ID);
            if (tabId == R.id.nav_books && booksFilterId != null && !booksFilterId.isEmpty()) {
                Fragment f = new BooksFragment();
                Bundle args = new Bundle();
                args.putString(BooksFragment.ARG_FILTER_ID, booksFilterId);
                f.setArguments(args);
                getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fragment_fade_in, R.anim.fragment_fade_out)
                    .replace(R.id.fragment_container, f)
                    .commit();
                if (bottomNavigation != null) bottomNavigation.setSelectedItemId(R.id.nav_books);
                setBottomNavVisible(true);
                intent.removeExtra(EXTRA_BOOKS_FILTER_ID);
            } else if (tabId != 0) {
                switchToTab(tabId);
            }
            if (intent.getBooleanExtra(EXTRA_INTERACTIVE_TOUR, false)) {
                intent.removeExtra(EXTRA_INTERACTIVE_TOUR);
                getWindow().getDecorView().postDelayed(() -> {
                    if (!isFinishing()) {
                        launchInteractiveTour();
                    }
                }, 400);
            }
        } catch (Throwable t) {
            Log.e(TAG, "onNewIntent", t);
        }
    }

    /**
     * YouTube-style: scroll down = hide nav, scroll up = show nav (only when on a tab that shows nav).
     * Fragments call this from their scroll listener.
     */
    public void onScrollDirection(boolean scrollingDown) {
        if (navAnimating || bottomNavContainer == null) return;
        // Sirf tab par jahan nav dikh raha hai (Books/Audio/Videos/About)
        if (!bottomNavVisible) return;
        if (scrollingDown && bottomNavContainer.getVisibility() == View.VISIBLE) {
            setBottomNavVisibleAnimated(false);
        } else if (!scrollingDown && bottomNavContainer.getVisibility() == View.VISIBLE) {
            // Already visible, no need to animate
        } else if (!scrollingDown && bottomNavContainer.getVisibility() != View.VISIBLE) {
            setBottomNavVisibleAnimated(true);
        }
    }

    /**
     * Call from fragments: dy from scroll (positive = scrolling down). Uses threshold to avoid jitter.
     */
    public void onScrolled(int dy) {
        if (dy > SCROLL_THRESHOLD_PX) onScrollDirection(true);
        else if (dy < -SCROLL_THRESHOLD_PX) onScrollDirection(false);
    }

    private void setBottomNavVisibleAnimated(boolean visible) {
        if (bottomNavContainer == null) return;
        View fragmentContainer = findViewById(R.id.fragment_container);
        if (fragmentContainer == null || fragmentContainer.getLayoutParams() == null) return;
        if (!(fragmentContainer.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) return;
        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) fragmentContainer.getLayoutParams();
        int totalHeight = getResources().getDimensionPixelSize(R.dimen.bottom_nav_total_height);
        int targetMargin = visible ? totalHeight : 0;
        int startMargin = mlp.bottomMargin;
        if (startMargin == targetMargin) return;

        navAnimating = true;
        int navHeight = bottomNavContainer.getHeight();
        if (navHeight <= 0) {
            bottomNavContainer.post(() -> setBottomNavVisibleAnimated(visible));
            navAnimating = false;
            return;
        }
        float startTy = visible ? navHeight : 0;
        float endTy = visible ? 0 : navHeight;

        if (visible) {
            bottomNavContainer.setVisibility(View.VISIBLE);
            bottomNavContainer.setTranslationY(startTy);
            bottomNavContainer.setAlpha(0f);
        }

        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(NAV_ANIM_DURATION_MS);
        anim.addUpdateListener(animation -> {
            float t = (float) animation.getAnimatedValue();
            mlp.bottomMargin = (int) (startMargin + t * (targetMargin - startMargin));
            fragmentContainer.setLayoutParams(fragmentContainer.getLayoutParams());
            bottomNavContainer.setTranslationY(startTy + t * (endTy - startTy));
            bottomNavContainer.setAlpha(visible ? t : 1f - t);
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                navAnimating = false;
                if (!visible) {
                    bottomNavContainer.setVisibility(View.GONE);
                    bottomNavContainer.setTranslationY(0);
                    bottomNavContainer.setAlpha(1f);
                }
            }
        });
        anim.start();
    }

    /** Switch to a tab by id (e.g. R.id.nav_videos). Used by Home "View All". */
    public void switchToTab(int itemId) {
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(itemId);
        }
    }

    /** Replay tour from About / settings. Clears detail back stack first. */
    public void launchInteractiveTour() {
        try {
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        } catch (Throwable t) {
            Log.e(TAG, "launchInteractiveTour popBackStack", t);
        }
        if (interactiveTour == null) {
            interactiveTour = new InteractiveAppTourHelper(this);
        }
        interactiveTour.start();
    }

    void setBottomNavVisibleForTour(boolean visible) {
        setBottomNavVisible(visible);
    }

    void setSuppressTabBackStackPushForTour(boolean suppress) {
        suppressTabBackStackPush = suppress;
    }

    BottomNavigationView getBottomNavigation() {
        return bottomNavigation;
    }

    /** Open in-app Swami profile page (avatar click). */
    public void openSwamiInfoPage() {
        try {
            if (isFinishing()) return;
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                            R.anim.slide_out_left, R.anim.slide_in_right)
                    .replace(R.id.fragment_container, new SwamiInfoFragment())
                    .addToBackStack("swami_info")
                    .commit();
        } catch (Throwable t) {
            Log.e(TAG, "openSwamiInfoPage", t);
        }
    }

    /** Open specific audio book detail (from history). */
    public void openAudioBook(ServerAudioBook book) {
        try {
            if (book == null || isFinishing()) return;
            final ServerAudioBook b = book;
            AdLoadingOverlay.show(this);
            InterstitialAdHelper.showIfAllowed(this, () -> {
                AdLoadingOverlay.dismiss(this);
                try {
                    if (isFinishing()) return;
                    Fragment f = AudioBookDetailFragment.newInstance(b);
                    getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                            .replace(R.id.fragment_container, f)
                            .addToBackStack("audio_book")
                            .commit();
                } catch (Throwable t) {
                    Log.e(TAG, "openAudioBook failed", t);
                }
            });
        } catch (Throwable t) {
            Log.e(TAG, "openAudioBook failed", t);
        }
    }

    private void setBottomNavVisible(boolean visible) {
        try {
            bottomNavVisible = visible;
            View fragmentContainer = findViewById(R.id.fragment_container);
            View navContainer = bottomNavContainer != null ? bottomNavContainer : (bottomNavigation != null ? (View) bottomNavigation.getParent() : null);
            if (navContainer != null) {
                navContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
                navContainer.setTranslationY(0);
                navContainer.setAlpha(1f);
            }
            if (fragmentContainer != null) {
                ViewGroup.LayoutParams lp = fragmentContainer.getLayoutParams();
                if (lp instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                    int bottom = visible
                            ? getResources().getDimensionPixelSize(R.dimen.bottom_nav_total_height)
                            : 0;
                    mlp.bottomMargin = bottom;
                    fragmentContainer.setLayoutParams(mlp);
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "setBottomNavVisible", t);
        }
    }

    @Override
    public void onBackPressed() {
        try {
            if (interactiveTour != null && interactiveTour.isShowing() && interactiveTour.onBackPressed()) {
                return;
            }
            FragmentManager fm = getSupportFragmentManager();
            // 1) Detail fragments/pages opened via addToBackStack should close first.
            if (fm.getBackStackEntryCount() > 0) {
                fm.popBackStack();
                return;
            }

            // 2) Bottom tab history: go to previous tab instead of closing app.
            while (!tabBackStack.isEmpty()) {
                int prevTab = tabBackStack.pop();
                if (prevTab != currentTabId) {
                    suppressTabBackStackPush = true;
                    switchToTab(prevTab);
                    suppressTabBackStackPush = false;
                    return;
                }
            }

            // 3) No history left -> default behavior (exit on Home).
            super.onBackPressed();
        } catch (Throwable t) {
            Log.e(TAG, "onBackPressed", t);
            super.onBackPressed();
        }
    }

    // Method for BooksFragment/HomeFragment to open books in in-app PDF reader
    public void openBook(Book book) {
        try {
            if (book == null) return;
            String pdfUrl = book.getPdfUrl();
            if (pdfUrl != null && !pdfUrl.trim().isEmpty()) {
                Intent intent = new Intent(this, ModernPdfActivity.class);
                intent.putExtra("pdf_url", pdfUrl.trim());
                intent.putExtra("book_name", book.getName() != null ? book.getName() : "");
                String thumb = book.getThumbnailUrl();
                if (thumb != null && !thumb.trim().isEmpty()) {
                    intent.putExtra("thumbnail_url", thumb.trim());
                }
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            } else {
                String fileName = book.getFileName();
                if (fileName == null || (fileName = fileName.trim()).isEmpty()) {
                    android.widget.Toast.makeText(this, "આ વર્ઝનમાં PDF વાંચન એપમાં ઉપલબ્ધ નથી.", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(this, ModernPdfActivity.class);
                intent.putExtra("book_name", fileName);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        } catch (Throwable t) {
            Log.e(TAG, "openBook failed", t);
            android.widget.Toast.makeText(this, "બુક ખોલી શકાઈ નહીં.", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Immediate update: agar user flow dismiss karke wapas aaya to dubara check
        if (appUpdateManager != null) {
            appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo, AppUpdateType.IMMEDIATE, this, REQUEST_APP_UPDATE);
                    } catch (Exception e) {
                        Log.e(TAG, "onResume startUpdateFlow", e);
                    }
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        if (interactiveTour != null) {
            interactiveTour.destroy();
            interactiveTour = null;
        }
        super.onDestroy();
    }

}
