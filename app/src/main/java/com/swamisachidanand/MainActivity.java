package com.swamisachidanand;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int SCROLL_THRESHOLD_PX = 8;
    private static final int NAV_ANIM_DURATION_MS = 220;

    private BottomNavigationView bottomNavigation;
    private View bottomNavContainer;
    private boolean bottomNavVisible = false;
    private boolean navAnimating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        View fragmentContainer = findViewById(R.id.fragment_container);
        if (fragmentContainer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(fragmentContainer, (v, windowInsets) -> {
                androidx.core.graphics.Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(insets.left, insets.top, insets.right, 0);
                return windowInsets;
            });
            fragmentContainer.requestApplyInsets();
        }

        BookChapterScanner.scanAllAndSave(this);

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
                        if (itemId == R.id.nav_home) selectedFragment = new HomeFragment();
                        else if (itemId == R.id.nav_books) selectedFragment = new BooksFragment();
                        else if (itemId == R.id.nav_audio) {
                            selectedFragment = new ServerAudioFragment();
                        }
                        else if (itemId == R.id.nav_videos) selectedFragment = new VideosFragment();
                        else if (itemId == R.id.nav_about) selectedFragment = new AboutFragment();
                        if (selectedFragment != null && !isFinishing()) {
                            getSupportFragmentManager().beginTransaction()
                                .setCustomAnimations(R.anim.fragment_fade_in, R.anim.fragment_fade_out)
                                .replace(R.id.fragment_container, selectedFragment)
                                .commit();
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
            Fragment f = AudioBookDetailFragment.newInstance(book);
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                    .replace(R.id.fragment_container, f)
                    .addToBackStack("audio_book")
                    .commit();
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

    // Method for BooksFragment/HomeFragment to open books
    public void openBook(Book book) {
        try {
            if (book == null) return;
            Intent intent = new Intent(this, PdfViewerActivity.class);
            String pdfUrl = book.getPdfUrl();
            if (pdfUrl != null && !pdfUrl.trim().isEmpty()) {
                intent.putExtra("pdf_url", pdfUrl.trim());
                intent.putExtra("book_name", book.getName() != null ? book.getName() : "");
                String thumb = book.getThumbnailUrl();
                if (thumb != null && !thumb.trim().isEmpty()) intent.putExtra("thumbnail_url", thumb.trim());
            } else {
                String fileName = book.getFileName();
                if (fileName == null || (fileName = fileName.trim()).isEmpty()) return;
                intent.putExtra("book_name", fileName);
            }
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Throwable t) {
            Log.e(TAG, "openBook failed", t);
            android.widget.Toast.makeText(this, "બુક ખોલી શકાઈ નહીં.", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

}
