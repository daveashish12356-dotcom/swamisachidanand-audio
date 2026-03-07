package com.swamisachidanand;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.VideoView;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.text.TextWatcher;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputEditText;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HomeFragment extends Fragment implements BookAdapter.OnBookClickListener {

    private static final String TAG = "HomeFragment";
    private static final String PREFS_NAME = "reading_progress";
    private static final String KEY_RECENT_BOOK = "recent_book_name";
    private static final String KEY_RECENT_BOOKS = "recent_books_list";
    private static final String PREFS_AUDIO = "audio_prefs";
    private static final String KEY_LAST_PART_ID = "last_part_id_";
    private static final int REQUEST_CODE_VOICE_SEARCH = 1001;

    private RecyclerView photoViewPager;
    private View homeHistorySection;
    private RecyclerView homeHistoryRecycler;
    private RecyclerView bestBooksRecycler;
    private RecyclerView bhaktiBooksRecycler;
    private RecyclerView homeVideosRecycler;
    private RecyclerView homeAudioRecycler;
    private com.google.android.material.textfield.TextInputEditText searchInput;
    private ImageView clearSearch, micButton;
    private LinearLayout searchResultsSection;
    private RecyclerView searchResultsRecycler;
    private TextView searchNoResults;
    private BookAdapter searchResultsAdapter;
    private List<Book> allBooksForSearch = new ArrayList<>();

    private HomeHistoryAdapter homeHistoryAdapter;
    private BookAdapter bestBooksAdapter;
    private BookAdapter bhaktiBooksAdapter;
    private HomeVideoAdapter homeVideoAdapter;
    private AudioBookCardAdapter homeAudioAdapter;
    
    private Handler autoScrollHandler;
    private Runnable autoScrollRunnable;
    private PhotoCarouselAdapter photoCarouselAdapter;
    private boolean videoPlaybackInProgress;

    // Simple hero video at top (Padma Bhushan clip) – FitVideoView for fit-without-zoom
    private FitVideoView heroVideoView;
    /** Last me hero box me photo animation, phir ye hi stay (suvichar → video → photo) */
    private ImageView heroFinalPhoto;
    private static final long HERO_VIDEO_TO_PHOTO_MS = 45_000L;
    private final Handler heroPhotoHandler = new Handler(Looper.getMainLooper());
    private Runnable heroPhotoRunnable;
    private boolean heroPhotoShown = false;

    // Best books = first N from assets (no fixed list). Purani books hatao, nayi PDFs dalo assets me — wahi dikhengi.
    private static final int BEST_BOOKS_COUNT = 8;

    private static final long SUVICHAR_DISPLAY_MS = 10_000L;
    private static final long SUVICHAR_DISPLAY_LONG_MS = 30_000L;
    /** Badha suvichar = text length above this → 30 sec, else 10 sec */
    private static final int SUVICHAR_LONG_THRESHOLD = 150;
    private View suvicharContainer;
    private RecyclerView suvicharRecycler;
    private final List<SuvicharItem> suvicharList = new ArrayList<>();
    private SuvicharAdapter suvicharAdapter;
    private final Handler suvicharHandler = new Handler(Looper.getMainLooper());
    private Runnable suvicharHideRunnable;
    private boolean suvicharShowing = false;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = null;
        try {
            view = inflater.inflate(R.layout.fragment_home, container, false);
            
            photoViewPager = null;
            heroVideoView = view.findViewById(R.id.hero_video_view);
            heroFinalPhoto = view.findViewById(R.id.hero_final_photo);
            setupPhotoBannerAnimation(view);

            homeHistorySection = view.findViewById(R.id.home_history_section);
            homeHistoryRecycler = view.findViewById(R.id.home_history_recycler);
            bestBooksRecycler = view.findViewById(R.id.best_books_recycler);
            bhaktiBooksRecycler = view.findViewById(R.id.bhakti_books_recycler);
            homeVideosRecycler = view.findViewById(R.id.home_videos_recycler);
            homeAudioRecycler = view.findViewById(R.id.home_audio_recycler);

            setupHomeVideos();
            setupHomeAudio();
            setupHistoryRow(view);
            setupViewAllClicks(view);
            searchInput = view.findViewById(R.id.global_search_input);
            clearSearch = view.findViewById(R.id.global_clear_search);
            micButton = view.findViewById(R.id.global_mic_button);
            View avatar = view.findViewById(R.id.global_profile_avatar);
            searchResultsSection = view.findViewById(R.id.search_results_section);
            searchResultsRecycler = view.findViewById(R.id.search_results_recycler);
            searchNoResults = view.findViewById(R.id.search_no_results);

            // Quick nav buttons row (Books / Audio / Videos / Sampark)
            View quickBooks = view.findViewById(R.id.home_quick_books);
            View quickAudio = view.findViewById(R.id.home_quick_audio);
            View quickVideos = view.findViewById(R.id.home_quick_videos);
            View quickContact = view.findViewById(R.id.home_quick_contact);
            if (quickBooks != null) {
                quickBooks.setOnClickListener(v -> switchToBottomNavTab(R.id.nav_books));
            }
            if (quickAudio != null) {
                quickAudio.setOnClickListener(v -> switchToBottomNavTab(R.id.nav_audio));
            }
            if (quickVideos != null) {
                quickVideos.setOnClickListener(v -> switchToBottomNavTab(R.id.nav_videos));
            }
            if (quickContact != null) {
                quickContact.setOnClickListener(v -> switchToBottomNavTab(R.id.nav_about));
            }

        if (searchResultsRecycler != null && getContext() != null) {
            searchResultsRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            // setHasFixedSize omitted: layout may use wrap_content in scroll direction
            searchResultsRecycler.setItemAnimator(new DefaultItemAnimator());
            int spacing = (int) (10 * getResources().getDisplayMetrics().density);
            searchResultsRecycler.addItemDecoration(new HorizontalSpacingItemDecoration(spacing));
            searchResultsAdapter = new BookAdapter(new ArrayList<>(), this);
                searchResultsAdapter.setUseCompactLayout(true);
                searchResultsRecycler.setAdapter(searchResultsAdapter);
            }

            setupSuvichar(view);
            setupSearchBar();
            if (avatar != null) {
                avatar.setOnClickListener(v -> openSwamiInfoPage());
            }

            if (view != null) {
                view.post(() -> {
                    if (isAdded() && heroVideoView != null) setupHeroVideo();
                });
                view.postDelayed(() -> {
                    if (!isAdded() || getContext() == null) return;
                    loadUnifiedHistory();
                    loadBestBooks();
                    loadBhaktiBooks();
                }, 500);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error in onCreateView", t);
            if (view == null && inflater != null && container != null) {
                view = new android.widget.FrameLayout(container.getContext());
            }
        }
        return view != null ? view : (container != null ? new android.widget.FrameLayout(container.getContext()) : null);
    }

    private void switchToBottomNavTab(int itemId) {
        Activity act = getActivity();
        if (act instanceof MainActivity) {
            ((MainActivity) act).switchToTab(itemId);
        }
    }

    private void setupSuvichar(View root) {
        if (root == null) return;
        Log.d(TAG, "suvichar: setupSuvichar start");
        suvicharContainer = root.findViewById(R.id.suvichar_container);
        suvicharRecycler = root.findViewById(R.id.suvichar_recycler);
        if (suvicharRecycler != null) {
            suvicharRecycler.setLayoutManager(new LinearLayoutManager(root.getContext()));
            suvicharRecycler.setNestedScrollingEnabled(true);
            suvicharAdapter = new SuvicharAdapter(suvicharList);
            suvicharRecycler.setAdapter(suvicharAdapter);
        }
        List<SuvicharItem> local = loadSuvicharFromAssets();
        if (local != null && !local.isEmpty()) {
            SuvicharItem item = local.get(local.size() - 1);
            int len = item.text != null ? item.text.length() : 0;
            long displayMs = suvicharDisplayMsFor(item);
            Log.d(TAG, "suvichar: from assets count=" + local.size() + " textLen=" + len + " displayMs=" + displayMs);
            suvicharList.clear();
            suvicharList.addAll(onlyLatest(local));
            if (suvicharAdapter != null) suvicharAdapter.notifyDataSetChanged();
            if (suvicharContainer != null) suvicharContainer.setVisibility(View.VISIBLE);
            suvicharShowing = true;
            suvicharHandler.removeCallbacks(suvicharHideRunnable);
            suvicharHideRunnable = () -> {
                Log.d(TAG, "suvichar: hide runnable (assets)");
                suvicharShowing = false;
                hideSuvichar();
            };
            suvicharHandler.postDelayed(suvicharHideRunnable, displayMs);
        } else {
            Log.d(TAG, "suvichar: no assets, will try server");
        }
        // Hamesha server se fetch karo – website par change aaye to app me bhi aaye
        loadSuvichar();
    }

    private static List<SuvicharItem> onlyLatest(List<SuvicharItem> list) {
        if (list == null || list.isEmpty()) return list;
        return Collections.singletonList(list.get(list.size() - 1));
    }

    private List<SuvicharItem> loadSuvicharFromAssets() {
        try {
            android.content.res.AssetManager am = getContext() != null ? getContext().getAssets() : null;
            if (am == null) return null;
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(am.open("suvichar.json"), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
            JSONArray arr = new JSONArray(sb.toString());
            List<SuvicharItem> list = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null)
                    list.add(new SuvicharItem(o.optString("text", ""), o.optString("author", "")));
            }
            Log.d(TAG, "suvichar: loadSuvicharFromAssets ok count=" + list.size());
            return list;
        } catch (Exception e) {
            Log.e(TAG, "suvichar: loadSuvicharFromAssets failed", e);
            return null;
        }
    }

    private void loadSuvichar() {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing()) return;
        final String baseUrl;
        try {
            String b = getString(R.string.server_books_base_url);
            b = b != null ? b.trim() : "";
            baseUrl = b.isEmpty() ? "" : (b.endsWith("/") ? b : b + "/");
        } catch (Exception e) {
            return;
        }
        final String primaryUrl = baseUrl + "public/suvichar_config.json";
        final String fallbackUrl = buildSuvicharRawUrl(baseUrl);
        Log.d(TAG, "suvichar: loadSuvichar fetch primaryUrl=" + primaryUrl);
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(6, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();
                String body = null;
                Request req1 = new Request.Builder().url(primaryUrl)
                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")
                        .addHeader("Accept", "application/json")
                        .build();
                try (Response response = client.newCall(req1).execute()) {
                    if (response.isSuccessful() && response.body() != null)
                        body = response.body().string();
                }
                if (body == null && fallbackUrl != null) {
                    try (Response response = client.newCall(new Request.Builder().url(fallbackUrl)
                            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")
                            .addHeader("Accept", "application/json")
                            .build()).execute()) {
                        if (response.isSuccessful() && response.body() != null)
                            body = response.body().string();
                    }
                }
                if (body == null || !body.trim().startsWith("{")) {
                    Log.w(TAG, "suvichar: server body null or not JSON, keep assets display");
                    return;
                }
                JSONObject root = new JSONObject(body);
                boolean enabled = root.optBoolean("suvicharEnabled", false);
                JSONArray arr = root.optJSONArray("suvichar");
                List<SuvicharItem> list = new ArrayList<>();
                if (enabled && arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.optJSONObject(i);
                        if (o != null)
                            list.add(new SuvicharItem(o.optString("text", ""), o.optString("author", "")));
                    }
                }
                List<SuvicharItem> finalList = onlyLatest(list);
                int textLen = finalList.isEmpty() ? 0 : (finalList.get(0).text != null ? finalList.get(0).text.length() : 0);
                long displayMs = finalList.isEmpty() ? SUVICHAR_DISPLAY_MS : suvicharDisplayMsFor(finalList.get(0));
                Log.d(TAG, "suvichar: server enabled=" + enabled + " listSize=" + list.size() + " finalCount=" + finalList.size() + " textLen=" + textLen + " displayMs=" + displayMs);
                final long displayMsFinal = displayMs;
                activity.runOnUiThread(() -> {
                    if (suvicharShowing && !finalList.isEmpty()) {
                        suvicharList.clear();
                        suvicharList.addAll(finalList);
                        if (suvicharAdapter != null) suvicharAdapter.notifyDataSetChanged();
                        suvicharHandler.removeCallbacks(suvicharHideRunnable);
                        suvicharHideRunnable = () -> {
                            suvicharShowing = false;
                            hideSuvichar();
                        };
                        suvicharHandler.postDelayed(suvicharHideRunnable, displayMsFinal);
                        return;
                    }
                    if (finalList.isEmpty()) {
                        // Server empty – keep assets suvichar as is, do nothing
                        return;
                    }
                    suvicharList.clear();
                    suvicharList.addAll(finalList);
                    if (suvicharAdapter != null) suvicharAdapter.notifyDataSetChanged();
                    if (suvicharContainer != null) suvicharContainer.setVisibility(View.VISIBLE);
                    suvicharShowing = true;
                    suvicharHandler.removeCallbacks(suvicharHideRunnable);
                    suvicharHideRunnable = () -> {
                        suvicharShowing = false;
                        hideSuvichar();
                    };
                    suvicharHandler.postDelayed(suvicharHideRunnable, displayMs);
                });
            } catch (Throwable t) {
                Log.e(TAG, "loadSuvichar error", t);
                // Do not hide – keep assets suvichar visible
            }
        }).start();
    }

    private static String buildSuvicharRawUrl(String baseUrl) {
        if (baseUrl == null || !baseUrl.contains("github.io")) return null;
        try {
            int start = baseUrl.indexOf("://") + 3;
            int end = baseUrl.indexOf(".github.io");
            if (start < 3 || end <= start) return null;
            String user = baseUrl.substring(start, end);
            int repoStart = end + 11;
            int repoEnd = baseUrl.indexOf("/", repoStart);
            String repo = repoEnd > repoStart ? baseUrl.substring(repoStart, repoEnd) : baseUrl.substring(repoStart).replace("/", "");
            if (user.isEmpty() || repo.isEmpty()) return null;
            return "https://raw.githubusercontent.com/" + user + "/" + repo + "/main/public/suvichar_config.json";
        } catch (Exception e) {
            return null;
        }
    }

    /** Badha suvichar → 30 sec, chhota → 10 sec */
    private static long suvicharDisplayMsFor(SuvicharItem item) {
        int len = item != null && item.text != null ? item.text.length() : 0;
        return len > SUVICHAR_LONG_THRESHOLD ? SUVICHAR_DISPLAY_LONG_MS : SUVICHAR_DISPLAY_MS;
    }

    private void hideSuvichar() {
        Log.d(TAG, "suvichar: hideSuvichar");
        if (suvicharContainer != null && suvicharContainer.getVisibility() == View.VISIBLE) {
            suvicharContainer.animate().alpha(0f).setDuration(400).withEndAction(() -> {
                reallyHideSuvicharAndShowVideo();
            }).start();
        } else {
            reallyHideSuvicharAndShowVideo();
        }
    }

    private void reallyHideSuvicharAndShowVideo() {
        if (suvicharContainer != null) {
            suvicharContainer.setVisibility(View.GONE);
            suvicharContainer.setAlpha(1f);
        }
        suvicharList.clear();
        if (suvicharAdapter != null) suvicharAdapter.notifyDataSetChanged();
        showHeroVideoWithAnimation();
    }

    /** Slideshow: 5 sec interval, 1 sec visible animation, alag alag (crossfade / slide+fade). */
    private void setupPhotoBannerAnimation(View root) {
        ImageView photo1 = root != null ? root.findViewById(R.id.photo_banner_1) : null;
        ImageView photo2 = root != null ? root.findViewById(R.id.photo_banner_2) : null;
        if (photo1 == null || photo2 == null) return;
        photo1.setAlpha(1f);
        photo2.setAlpha(0f);
        photo1.setTranslationX(0f);
        photo2.setTranslationX(0f);
        final int intervalMs = 5000;
        final int animDurationMs = 1000;
        final float slideDp = 40f;
        Handler h = new Handler(Looper.getMainLooper());
        final int[] step = { 0 };
        Runnable tick = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || photo1 == null || photo2 == null) return;
                float density = getResources() != null ? getResources().getDisplayMetrics().density : 2f;
                float slidePx = slideDp * density;
                boolean showFirst = (photo1.getAlpha() > 0.5f);
                step[0]++;
                boolean useSlide = (step[0] % 2 == 0);
                if (showFirst) {
                    photo2.setTranslationX(useSlide ? slidePx : 0f);
                    photo2.setAlpha(0f);
                    if (useSlide) {
                        photo1.animate().translationX(-slidePx).alpha(0f).setDuration(animDurationMs).start();
                        photo2.animate().translationX(0f).alpha(1f).setDuration(animDurationMs).withEndAction(() -> {
                            if (photo1 != null) photo1.setTranslationX(0f);
                            if (photo2 != null) photo2.setTranslationX(0f);
                        }).start();
                    } else {
                        photo1.animate().alpha(0f).setDuration(animDurationMs).start();
                        photo2.animate().alpha(1f).setDuration(animDurationMs).start();
                    }
                } else {
                    photo1.setTranslationX(useSlide ? -slidePx : 0f);
                    photo1.setAlpha(0f);
                    if (useSlide) {
                        photo2.animate().translationX(slidePx).alpha(0f).setDuration(animDurationMs).start();
                        photo1.animate().translationX(0f).alpha(1f).setDuration(animDurationMs).withEndAction(() -> {
                            if (photo1 != null) photo1.setTranslationX(0f);
                            if (photo2 != null) photo2.setTranslationX(0f);
                        }).start();
                    } else {
                        photo2.animate().alpha(0f).setDuration(animDurationMs).start();
                        photo1.animate().alpha(1f).setDuration(animDurationMs).start();
                    }
                }
                h.postDelayed(this, intervalMs);
            }
        };
        h.postDelayed(tick, intervalMs);
    }

    /** Hero video: auto-play, muted, loop. If suvichar is showing, prepare but don't show/start – video will show after suvichar fade. */
    private void setupHeroVideo() {
        if (heroVideoView == null || getContext() == null) return;
        try {
            String uriString = "android.resource://" + getContext().getPackageName() + "/" + R.raw.padma_bhushan_video;
            heroVideoView.setVisibility(suvicharShowing ? View.GONE : View.VISIBLE);
            heroVideoView.setVideoURI(android.net.Uri.parse(uriString));
            heroVideoView.setOnPreparedListener(mp -> {
                try {
                    heroVideoView.setVideoDimensions(mp.getVideoWidth(), mp.getVideoHeight());
                } catch (Throwable ignored) {}
                try {
                    mp.setLooping(true);
                    mp.setVolume(0f, 0f);
                } catch (Throwable ignored) {}
                if (suvicharShowing) return;
                try {
                    heroVideoView.seekTo(1);
                    heroVideoView.start();
                    scheduleHeroPhotoAfterVideo();
                } catch (Throwable t) {
                    Log.e(TAG, "Hero VideoView start error", t);
                }
            });
            heroVideoView.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "Hero VideoView error " + what + " " + extra);
                return true;
            });
        } catch (Throwable t) {
            Log.e(TAG, "setupHeroVideo error", t);
        }
    }

    /** Suvichar ke baad hero video fade-in animation se dikhao aur start karo; 45 sec baad photo animation, phir photo stay. */
    private void showHeroVideoWithAnimation() {
        if (heroVideoView == null) return;
        heroVideoView.setVisibility(View.VISIBLE);
        heroVideoView.setAlpha(0f);
        heroVideoView.animate().alpha(1f).setDuration(400).withEndAction(() -> {
            if (heroVideoView != null) {
                try {
                    heroVideoView.seekTo(1);
                    heroVideoView.start();
                    scheduleHeroPhotoAfterVideo();
                } catch (Throwable t) {
                    Log.e(TAG, "Hero video start after suvichar", t);
                }
            }
        }).start();
    }

    /** Video ke 45 sec baad photo fade-in, video hide; photo last me stay. */
    private void scheduleHeroPhotoAfterVideo() {
        if (heroPhotoRunnable != null) heroPhotoHandler.removeCallbacks(heroPhotoRunnable);
        heroPhotoRunnable = () -> {
            if (heroPhotoShown || !isAdded()) return;
            heroPhotoShown = true;
            if (heroVideoView != null) {
                try { heroVideoView.stopPlayback(); } catch (Throwable ignored) {}
                heroVideoView.animate().alpha(0f).setDuration(600).withEndAction(() -> {
                    if (heroVideoView != null) heroVideoView.setVisibility(View.GONE);
                    if (heroFinalPhoto != null) {
                        heroFinalPhoto.setVisibility(View.VISIBLE);
                        heroFinalPhoto.setAlpha(0f);
                        heroFinalPhoto.animate().alpha(1f).setDuration(600).start();
                    }
                }).start();
            } else if (heroFinalPhoto != null) {
                heroFinalPhoto.setVisibility(View.VISIBLE);
                heroFinalPhoto.setAlpha(1f);
            }
        };
        heroPhotoHandler.postDelayed(heroPhotoRunnable, HERO_VIDEO_TO_PHOTO_MS);
    }

    private void setupPhotoCarousel() {
        if (getActivity() == null || isDetached() || photoViewPager == null || getContext() == null) return;
        try {
            photoCarouselAdapter = new PhotoCarouselAdapter(getActivity().getAssets(), getActivity());
        } catch (Throwable t) {
            Log.e(TAG, "Error creating PhotoCarouselAdapter", t);
            return; // Don't setup carousel if adapter creation fails
        }
        photoCarouselAdapter.setOnVideoPlayListener((assetPath, videoItemView) -> {
            Log.d(TAG, "onPlayVideo called assetPath=" + assetPath);
            if (getContext() != null) {
                android.widget.Toast.makeText(getContext(), "Video play start...", android.widget.Toast.LENGTH_SHORT).show();
            }
            if (isAdded() && getContext() != null) playVideoInline(assetPath, videoItemView);
        });
        android.content.Context ctx = getContext();
        if (ctx == null || photoCarouselAdapter == null) return;
        try {
            LinearLayoutManager layoutManager = new LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false);
            photoViewPager.setLayoutManager(layoutManager);
            photoViewPager.setAdapter(photoCarouselAdapter);
            Log.d(TAG, "Carousel adapter set, itemCount=" + photoCarouselAdapter.getItemCount() + " (0=video)");
        } catch (Throwable t) {
            Log.e(TAG, "Error setting up photo carousel", t);
            return;
        }

        try {
            PagerSnapHelper snapHelper = new PagerSnapHelper();
            snapHelper.attachToRecyclerView(photoViewPager);
        } catch (Throwable t) {
            Log.e(TAG, "Error attaching PagerSnapHelper", t);
        }

        photoViewPager.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) return;
                checkVideoSlideAndPlay();
            }
        });
        // VIDEO PAHLE DIKHE: hamesha position 0 (video slide) pe scroll
        photoViewPager.scrollToPosition(0);
        photoViewPager.post(() -> {
            if (isAdded() && photoViewPager != null) {
                photoViewPager.scrollToPosition(0);
                checkVideoSlideAndPlay();
            }
        });
        photoViewPager.postDelayed(() -> {
            if (isAdded() && photoViewPager != null) {
                photoViewPager.scrollToPosition(0);
                checkVideoSlideAndPlay();
            }
        }, 150);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded() && photoViewPager != null) {
                photoViewPager.scrollToPosition(0);
                checkVideoSlideAndPlay();
            }
        }, 2000);

        // Auto-scroll with slower speed so animation is visible (6.5 sec per photo)
        autoScrollHandler = new Handler(Looper.getMainLooper());
        final int photoDelay = 6500;

        autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (getActivity() == null || isDetached() || photoViewPager == null || photoCarouselAdapter == null) return;
                    if (photoCarouselAdapter.getItemCount() == 0) return;

                    LinearLayoutManager lm = (LinearLayoutManager) photoViewPager.getLayoutManager();
                    if (lm == null) return;

                    int firstVisible = lm.findFirstVisibleItemPosition();
                    int itemCount = photoCarouselAdapter.getItemCount();
                    if (itemCount <= 0) return;

                    if (firstVisible == 0) {
                        // Video slide: don't auto-scroll, wait for video to finish (scroll happens in onCompletion)
                        if (autoScrollHandler != null && !isDetached()) {
                            autoScrollHandler.postDelayed(this, photoDelay);
                        }
                        return;
                    }

                    int targetPos = (firstVisible >= itemCount - 1) ? 0 : (firstVisible + 1);
                    if (targetPos >= 0 && targetPos < itemCount) {
                        smoothScrollToPosition(photoViewPager, lm, targetPos);
                    }
                    if (autoScrollHandler != null && !isDetached()) {
                        autoScrollHandler.postDelayed(this, photoDelay);
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "Auto-scroll error", t);
                }
            }
        };

        if (autoScrollHandler != null) {
            autoScrollHandler.postDelayed(autoScrollRunnable, photoDelay);
        }

        preCopyVideoToCache();
    }

    private void preCopyVideoToCache() {
        android.app.Activity act = getActivity();
        if (act == null || photoCarouselAdapter == null) return;
        String assetPath = photoCarouselAdapter.getVideoAssetPath();
        java.io.File cacheFile = new java.io.File(act.getCacheDir(), "carousel_video.mp4");
        Log.d(TAG, "preCopyVideoToCache started asset=" + assetPath);
        new Thread(() -> {
            try {
                java.io.InputStream in = act.getAssets().open(assetPath);
                Log.d(TAG, "preCopyVideoToCache asset opened OK");
                try (java.io.FileOutputStream out = new java.io.FileOutputStream(cacheFile)) {
                    byte[] buf = new byte[32768];
                    int n;
                    long total = 0;
                    while ((n = in.read(buf)) > 0) {
                        out.write(buf, 0, n);
                        total += n;
                    }
                    Log.d(TAG, "preCopyVideoToCache done bytes=" + total + " file=" + cacheFile.length());
                } finally {
                    in.close();
                }
                act.runOnUiThread(() -> {
                    if (!isAdded() || photoViewPager == null) return;
                    LinearLayoutManager lm = (LinearLayoutManager) photoViewPager.getLayoutManager();
                    if (lm != null) photoViewPager.scrollToPosition(0);
                    photoViewPager.postDelayed(() -> {
                        if (isAdded() && photoViewPager != null) {
                            Log.d(TAG, "preCopyVideoToCache calling checkVideoSlideAndPlay");
                            checkVideoSlideAndPlay();
                        }
                    }, 300);
                });
            } catch (Throwable t) {
                Log.e(TAG, "preCopyVideoToCache error: " + t.getClass().getName() + " " + t.getMessage(), t);
            }
        }).start();
    }

    private void scrollToNextAfterVideo() {
        if (photoViewPager == null || isDetached()) return;
        LinearLayoutManager lm = (LinearLayoutManager) photoViewPager.getLayoutManager();
        if (lm == null) return;
        smoothScrollToPosition(photoViewPager, lm, 1);
    }

    private void checkVideoSlideAndPlay() {
        if (photoViewPager == null || photoCarouselAdapter == null || !isAdded()) return;
        LinearLayoutManager lm = (LinearLayoutManager) photoViewPager.getLayoutManager();
        if (lm == null) return;
        int firstVisible = lm.findFirstVisibleItemPosition();
        View videoItemView = lm.findViewByPosition(0);
        Log.d(TAG, "checkVideoSlideAndPlay firstVisible=" + firstVisible + " videoItemView=" + (videoItemView != null));
        if (firstVisible == 0 && videoItemView != null) {
            VideoView vv = videoItemView.findViewById(R.id.carousel_video_view);
            if (vv != null && vv.getVisibility() != View.VISIBLE && !videoPlaybackInProgress) {
                Log.d(TAG, "checkVideoSlideAndPlay: starting play");
                playVideoInline(photoCarouselAdapter.getVideoAssetPath(), videoItemView);
            }
        } else if (videoItemView != null) {
            videoPlaybackInProgress = false;
            stopVideoInline(videoItemView);
        }
    }

    private void playVideoInline(String assetPath, View videoItemView) {
        android.app.Activity act = getActivity();
        if (act == null || getContext() == null || videoItemView == null || photoViewPager == null) {
            Log.e(TAG, "playVideoInline skip: act=" + (act != null) + " videoItem=" + (videoItemView != null) + " pager=" + (photoViewPager != null));
            return;
        }
        java.io.File cacheFile = new java.io.File(act.getCacheDir(), "carousel_video.mp4");
        Log.d(TAG, "playVideoInline cache exists=" + cacheFile.exists() + " length=" + cacheFile.length());
        if (cacheFile.exists() && cacheFile.length() > 0) {
            Log.d(TAG, "Video cache hit, starting playback");
            LinearLayoutManager lm = (LinearLayoutManager) photoViewPager.getLayoutManager();
            if (lm != null) {
                View currentVideoItem = lm.findViewByPosition(0);
                if (currentVideoItem != null) {
                    startVideoPlayback(cacheFile.getAbsolutePath(), currentVideoItem);
                } else {
                    Log.d(TAG, "Cache hit but view null, scroll and retry");
                    photoViewPager.scrollToPosition(0);
                    photoViewPager.postDelayed(() -> {
                        if (!isAdded() || photoViewPager == null) return;
                        View v = lm.findViewByPosition(0);
                        Log.d(TAG, "Cache hit retry view=" + (v != null));
                        if (v != null) startVideoPlayback(cacheFile.getAbsolutePath(), v);
                    }, 400);
                }
            }
            return;
        }
        Log.d(TAG, "Video cache miss, copying asset=" + assetPath);
        new Thread(() -> {
            try {
                java.io.InputStream in = act.getAssets().open(assetPath);
                Log.d(TAG, "playVideoInline asset opened, copying...");
                try (java.io.FileOutputStream out = new java.io.FileOutputStream(cacheFile)) {
                    byte[] buf = new byte[32768];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                } finally {
                    in.close();
                }
                Log.d(TAG, "playVideoInline copy done size=" + cacheFile.length());
                act.runOnUiThread(() -> {
                    if (!isAdded() || getContext() == null || photoViewPager == null) return;
                    LinearLayoutManager lm = (LinearLayoutManager) photoViewPager.getLayoutManager();
                    if (lm == null) return;
                    photoViewPager.scrollToPosition(0);
                    photoViewPager.postDelayed(() -> {
                        if (!isAdded() || photoViewPager == null) return;
                        View currentVideoItem = lm.findViewByPosition(0);
                        if (currentVideoItem != null) {
                            Log.d(TAG, "Video copy done, starting playback");
                            startVideoPlayback(cacheFile.getAbsolutePath(), currentVideoItem);
                        } else {
                            Log.e(TAG, "Video copy done but findViewByPosition(0)=null");
                        }
                    }, 350);
                });
            } catch (Throwable t) {
                Log.e(TAG, "playVideoInline copy error: " + t.getClass().getName() + " " + t.getMessage(), t);
                act.runOnUiThread(() -> {
                    if (isAdded() && getContext() != null) {
                        android.widget.Toast.makeText(getContext(), "Video error: " + t.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    private void startVideoPlayback(String cachePath, View videoItemView) {
        if (videoItemView == null) {
            Log.e(TAG, "startVideoPlayback videoItemView=null");
            return;
        }
        VideoView vv = videoItemView.findViewById(R.id.carousel_video_view);
        ImageView thumb = videoItemView.findViewById(R.id.photo_image);
        ImageView playOverlay = videoItemView.findViewById(R.id.play_overlay);
        if (vv == null) {
            Log.e(TAG, "startVideoPlayback VideoView=null");
            return;
        }
        Log.d(TAG, "startVideoPlayback path=" + cachePath);
        videoPlaybackInProgress = true;
        vv.setVisibility(View.GONE);
        if (thumb != null) thumb.setVisibility(View.VISIBLE);
        vv.setVideoPath(cachePath);
        vv.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "Video error what=" + what + " extra=" + extra);
            if (getContext() != null) {
                android.widget.Toast.makeText(getContext(), "Video play error: " + what, android.widget.Toast.LENGTH_SHORT).show();
            }
            stopVideoInline(videoItemView);
            return true;
        });
        vv.setOnPreparedListener(mp -> {
            try {
                mp.setVolume(0f, 0f);
            } catch (Exception e) {
                Log.e(TAG, "Mute video", e);
            }
            vv.setVisibility(View.VISIBLE);
            if (thumb != null) thumb.setVisibility(View.GONE);
            if (playOverlay != null) playOverlay.setVisibility(View.GONE);
            Log.d(TAG, "Video prepared, showing");
        });
        vv.setOnCompletionListener(mp -> {
            stopVideoInline(videoItemView);
            scrollToNextAfterVideo();
        });
        vv.setOnClickListener(v -> stopVideoInline(videoItemView));
        vv.start();
        Log.d(TAG, "Video start() called");
    }

    private void stopVideoInline(View videoItemView) {
        if (videoItemView == null) return;
        videoPlaybackInProgress = false;
        VideoView vv = videoItemView.findViewById(R.id.carousel_video_view);
        ImageView thumb = videoItemView.findViewById(R.id.photo_image);
        ImageView playOverlay = videoItemView.findViewById(R.id.play_overlay);
        if (vv != null) {
            vv.stopPlayback();
            vv.setVisibility(View.GONE);
        }
        if (thumb != null) thumb.setVisibility(View.VISIBLE);
        if (playOverlay != null) playOverlay.setVisibility(View.VISIBLE);
    }

    private void smoothScrollToPosition(RecyclerView rv, LinearLayoutManager lm, int position) {
        try {
            if (getContext() == null || rv == null || lm == null || isDetached()) return;
            if (position < 0) return;
            LinearSmoothScroller scroller = new LinearSmoothScroller(getContext()) {
                @Override
                protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                    return 120f / displayMetrics.densityDpi;
                }
            };
            scroller.setTargetPosition(position);
            lm.startSmoothScroll(scroller);
        } catch (Throwable t) {
            Log.e(TAG, "smoothScrollToPosition error", t);
        }
    }

    private void setupSearchBar() {
        if (searchInput != null) {
            searchInput.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String q = s.toString().trim();
                    if (clearSearch != null) clearSearch.setVisibility(q.isEmpty() ? View.GONE : View.VISIBLE);
                    if (searchResultsSection != null) searchResultsSection.setVisibility(q.isEmpty() ? View.GONE : View.VISIBLE);
                    filterBooks(q);
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
            searchInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    openGlobalSearch();
                    return true;
                }
                return false;
            });
        }

        if (clearSearch != null) {
            clearSearch.setOnClickListener(v -> {
                if (searchInput != null) {
                    searchInput.setText("");
                    clearSearch.setVisibility(View.GONE);
                    if (searchResultsSection != null) searchResultsSection.setVisibility(View.GONE);
                }
            });
        }

        if (micButton != null) {
            micButton.setOnClickListener(v -> startVoiceSearch());
        }
    }

    private void openGlobalSearch() {
        String q = searchInput != null && searchInput.getText() != null ? searchInput.getText().toString().trim() : "";
        if (q.isEmpty()) return;
        android.content.Intent i = new android.content.Intent(requireContext(), SearchResultActivity.class);
        i.putExtra(SearchResultActivity.EXTRA_QUERY, q);
        startActivity(i);
    }

    private void startVoiceSearch() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN");
            intent.putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, new String[]{"en-IN", "gu-IN", "hi-IN"});
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say book name to search...");
            startActivityForResult(intent, REQUEST_CODE_VOICE_SEARCH);
        } catch (Exception e) {
            android.widget.Toast.makeText(getContext(), "Voice search not available", android.widget.Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Voice search error", e);
        }
    }

    private void openSwamiInfoPage() {
        android.app.Activity act = getActivity();
        if (act instanceof MainActivity && !act.isFinishing()) {
            ((MainActivity) act).openSwamiInfoPage();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_VOICE_SEARCH && resultCode == Activity.RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spoken = results.get(0);
                if (searchInput != null) searchInput.setText(spoken);
                if (!spoken.trim().isEmpty()) {
                    openGlobalSearch();
                }
            }
        }
    }

    private void filterBooks(String query) {
        if (searchResultsAdapter == null) return;

        List<Book> filtered = new ArrayList<>();
        if (query.isEmpty()) {
            if (searchNoResults != null) searchNoResults.setVisibility(View.GONE);
            if (searchResultsRecycler != null) searchResultsRecycler.setVisibility(View.GONE);
            searchResultsAdapter.updateBooks(filtered);
            return;
        }

        String queryLower = query.trim().toLowerCase();
        if (queryLower.isEmpty()) {
            searchResultsAdapter.updateBooks(filtered);
            return;
        }

        for (Book book : allBooksForSearch) {
            String name = book.getName();
            String fName = book.getFileName();
            if (name == null) name = "";
            if (fName == null) fName = "";
            String bookName = name.toLowerCase();
            String fileName = fName.toLowerCase();
            String searchableText = book.getSearchableText();
            boolean matches = false;

            if (searchableText != null && searchableText.contains(queryLower)) matches = true;
            else if (bookName.contains(queryLower) || fileName.contains(queryLower)) matches = true;
            else {
                String[] words = queryLower.split("\\s+");
                for (String w : words) {
                    w = w.trim();
                    if (w.length() >= 2 && ((searchableText != null && searchableText.contains(w)) || bookName.contains(w) || fileName.contains(w))) {
                        matches = true;
                        break;
                    }
                }
            }
            if (!matches && ((searchableText != null && fuzzyMatch(queryLower, searchableText)) || fuzzyMatch(queryLower, bookName) || fuzzyMatch(queryLower, fileName)))
                matches = true;

            if (matches) filtered.add(book);
        }

        // Start-with matches first, then by name
        Collections.sort(filtered, (a, b) -> {
            String an = a.getName() != null ? a.getName() : "";
            String bn = b.getName() != null ? b.getName() : "";
            boolean aStart = an.toLowerCase().startsWith(queryLower) || (a.getSearchableText() != null && a.getSearchableText().toLowerCase().startsWith(queryLower));
            boolean bStart = bn.toLowerCase().startsWith(queryLower) || (b.getSearchableText() != null && b.getSearchableText().toLowerCase().startsWith(queryLower));
            if (aStart != bStart) return aStart ? -1 : 1;
            return an.compareToIgnoreCase(bn);
        });
        searchResultsAdapter.updateBooks(filtered);

        if (filtered.isEmpty()) {
            if (searchNoResults != null) { searchNoResults.setVisibility(View.VISIBLE); }
            if (searchResultsRecycler != null) { searchResultsRecycler.setVisibility(View.GONE); }
        } else {
            if (searchNoResults != null) { searchNoResults.setVisibility(View.GONE); }
            if (searchResultsRecycler != null) { searchResultsRecycler.setVisibility(View.VISIBLE); }
        }
    }

    /** Map thumbnails for audio books from PDF books (same logic as ServerAudioFragment). */
    private static List<ServerAudioBook> mapAudioThumbnails(Activity act, List<ServerAudioBook> loaded, String base) {
        if (loaded == null || loaded.isEmpty()) return loaded;
        if (act == null) return loaded;
        if (base == null) base = "";
        base = base.trim();
        if (!base.isEmpty() && !base.endsWith("/")) base += "/";
        List<Book> pdfBooks = ServerBookLoader.load(act);
        HashMap<String, String> thumbByTitle = new HashMap<>();
        if (pdfBooks != null) {
            for (Book b : pdfBooks) {
                if (b == null) continue;
                String name = b.getName();
                String tUrl = b.getThumbnailUrl();
                if (name == null || tUrl == null || tUrl.trim().isEmpty()) continue;
                String key = name.trim().toLowerCase();
                if (!key.isEmpty()) thumbByTitle.put(key, tUrl.trim());
            }
        }
        List<ServerAudioBook> fixed = new ArrayList<>(loaded.size());
        for (ServerAudioBook ab : loaded) {
            if (ab == null) { fixed.add(ab); continue; }
            String thumb = ab.getThumbnailUrl();
            if (thumb != null && !thumb.isEmpty()) {
                fixed.add(ab);
                continue;
            }
            String title = ab.getTitle();
            String fromBooks = null;
            if ("africa_sansmaran".equals(ab.getId())) {
                try {
                    String thumbName = "આફ્રિકા-પ્રવાસનાં સંસ્મરણો.jpg";
                    String encoded = URLEncoder.encode(thumbName, StandardCharsets.UTF_8.name()).replace("+", "%20");
                    fromBooks = base + "public/thumbnails/" + encoded;
                } catch (Exception ignored) {}
            }
            if (title != null) {
                String tKey = title.trim().toLowerCase();
                if (fromBooks == null || fromBooks.isEmpty()) fromBooks = thumbByTitle.get(tKey);
                if ((fromBooks == null || fromBooks.isEmpty()) && pdfBooks != null) {
                    String normAudio = normalizeTitle(title);
                    for (Book b : pdfBooks) {
                        if (b == null || b.getName() == null) continue;
                        String normBook = normalizeTitle(b.getName());
                        if (!normBook.isEmpty() && !normAudio.isEmpty()
                                && (normBook.equals(normAudio) || normBook.contains(normAudio) || normAudio.contains(normBook))) {
                            String tUrl = b.getThumbnailUrl();
                            if (tUrl != null && !tUrl.trim().isEmpty()) fromBooks = tUrl.trim();
                            break;
                        }
                    }
                }
            }
            if (fromBooks != null && !fromBooks.isEmpty()) {
                fixed.add(new ServerAudioBook(ab.getId(), ab.getTitle(), ab.getParts(), fromBooks));
            } else {
                fixed.add(ab);
            }
        }
        return fixed;
    }

    private static String normalizeTitle(String s) {
        if (s == null) return "";
        String out = s.toLowerCase().trim();
        out = out.replaceAll("[\\u2013\\u2014\\-]+", " ");
        out = out.replaceAll("[\\s]+", " ");
        return out;
    }

    private static int safeCompare(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        return a.compareToIgnoreCase(b);
    }

    private boolean fuzzyMatch(String query, String text) {
        if (query == null || text == null || query.isEmpty()) return false;
        query = query.replaceAll("[^\\w\\s]", " ").replaceAll("\\s+", " ").trim();
        text = text.replaceAll("[^\\w\\s]", " ").replaceAll("\\s+", " ").trim();
        if (query.isEmpty() || text.isEmpty()) return false;
        int qi = 0, matched = 0;
        for (int i = 0; i < text.length() && qi < query.length(); i++) {
            if (Character.toLowerCase(text.charAt(i)) == Character.toLowerCase(query.charAt(qi))) { matched++; qi++; }
        }
        if (matched >= query.length() * 0.6) return true;
        String[] qw = query.split("\\s+");
        if (qw.length > 1) {
            for (String w : qw) {
                if (w.length() >= 3 && text.contains(w)) return true;
            }
        }
        return false;
    }

    /** Load books + audio (continue) + videos into one history row. */
    private void loadUnifiedHistory() {
        new Thread(() -> {
            try {
                android.app.Activity act = getActivity();
                if (act == null) return;
                List<HomeHistoryItem> audioList = new ArrayList<>();
                List<HomeHistoryItem> bookList = new ArrayList<>();
                List<HomeHistoryItem> videoList = new ArrayList<>();

                // Recent audio (continue listening) – first
                java.util.List<ServerAudioBook> loaded = new ArrayList<>();
                String base = act.getString(R.string.server_books_base_url);
                if (base != null) {
                    base = base.trim();
                    if (!base.isEmpty() && !base.endsWith("/")) base += "/";
                }
                String url = (base != null ? base : "") + "audio_list.json";
                OkHttpClient client = new OkHttpClient.Builder().connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS).readTimeout(20, java.util.concurrent.TimeUnit.SECONDS).build();
                okhttp3.Request req = new okhttp3.Request.Builder().url(url)
                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")
                        .addHeader("Accept", "application/json").build();
                try (okhttp3.Response resp = client.newCall(req).execute()) {
                    if (resp.isSuccessful() && resp.body() != null) {
                        String body = resp.body().string();
                        if (body != null && body.trim().startsWith("{"))
                            loaded = ServerAudioParser.parseBooks(body);
                    }
                } catch (Exception ignored) {}
                if (loaded == null || loaded.isEmpty()) {
                    for (String assetName : new String[]{"audio_list_main.json", "audio_list_fallback.json"}) {
                        try (java.io.BufferedReader r = new java.io.BufferedReader(
                                new java.io.InputStreamReader(act.getAssets().open(assetName), java.nio.charset.StandardCharsets.UTF_8))) {
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = r.readLine()) != null) sb.append(line);
                            loaded = ServerAudioParser.parseBooks(sb.toString());
                            if (loaded != null && !loaded.isEmpty()) break;
                        } catch (Exception ignored) { }
                    }
                }
                if (loaded == null) loaded = new ArrayList<>();
                // Map thumbnails from books (same as ServerAudioFragment) so audio history shows covers
                loaded = mapAudioThumbnails(act, loaded, base);
                Map<String, Integer> progressMap = loadAudioProgressMap(act, loaded);
                for (ServerAudioBook b : loaded) {
                    if (b != null && progressMap != null && progressMap.containsKey(b.getId())) {
                        audioList.add(new HomeHistoryItem(HomeHistoryItem.TYPE_AUDIO, b));
                        if (audioList.size() >= 3) break;
                    }
                }

                // Recent books
                List<Book> serverBooks = ServerBookLoader.load(act);
                SharedPreferences prefs = act.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String recentBooksStr = prefs.getString(KEY_RECENT_BOOKS, "");
                if (!recentBooksStr.isEmpty()) {
                    for (String name : recentBooksStr.split(",")) {
                        name = name.trim();
                        if (name.isEmpty()) continue;
                        for (Book b : serverBooks) {
                            if (name.equals(b.getFileName()) || name.equals(b.getName()) ||
                                name.equals(b.getFileName().replace(".pdf", "").replace(".PDF", ""))) {
                                bookList.add(new HomeHistoryItem(HomeHistoryItem.TYPE_BOOK, b));
                                break;
                            }
                        }
                        if (bookList.size() >= 3) break;
                    }
                }

                // Recent videos (from saved ids)
                List<String> videoIds = RecentVideoHelper.getRecentVideoIds(act);
                for (String id : videoIds) {
                    if (id == null || id.isEmpty()) continue;
                    HomeVideoLoader.HomeVideoItem v = new HomeVideoLoader.HomeVideoItem();
                    v.videoId = id;
                    v.title = "વિડિઓ";
                    v.thumbnailUrl = "https://img.youtube.com/vi/" + id + "/default.jpg";
                    v.durationSeconds = -1;
                    v.viewCount = -1;
                    videoList.add(new HomeHistoryItem(HomeHistoryItem.TYPE_VIDEO, v));
                    if (videoList.size() >= 3) break;
                }

                // Jo sabse last kiya wo pehle – sort by recent activity order
                java.util.Map<String, HomeHistoryItem> bookMap = new java.util.HashMap<>();
                for (HomeHistoryItem h : bookList) {
                    if (h.data instanceof Book) {
                        Book b = (Book) h.data;
                        String key = b.getFileName();
                        if (key != null) bookMap.put(key, h);
                        String alt = b.getFileName().replace(".pdf", "").replace(".PDF", "");
                        if (!alt.equals(key)) bookMap.put(alt, h);
                        if (b.getName() != null) bookMap.put(b.getName(), h);
                    }
                }
                java.util.Map<String, HomeHistoryItem> audioMap = new java.util.HashMap<>();
                for (HomeHistoryItem h : audioList) {
                    if (h.data instanceof ServerAudioBook)
                        audioMap.put(((ServerAudioBook) h.data).getId(), h);
                }
                java.util.Map<String, HomeHistoryItem> videoMap = new java.util.HashMap<>();
                for (HomeHistoryItem h : videoList) {
                    if (h.data instanceof HomeVideoLoader.HomeVideoItem)
                        videoMap.put(((HomeVideoLoader.HomeVideoItem) h.data).videoId, h);
                }
                List<HomeHistoryItem> combined = new ArrayList<>();
                java.util.Set<String> added = new java.util.HashSet<>();
                for (RecentActivityHelper.ActivityEntry ae : RecentActivityHelper.getRecentActivityOrder(act)) {
                    HomeHistoryItem item = null;
                    String key = ae.type + ":" + ae.id;
                    if (ae.type == RecentActivityHelper.TYPE_BOOK) item = bookMap.get(ae.id);
                    else if (ae.type == RecentActivityHelper.TYPE_AUDIO) item = audioMap.get(ae.id);
                    else if (ae.type == RecentActivityHelper.TYPE_VIDEO) item = videoMap.get(ae.id);
                    if (item != null && !added.contains(key)) {
                        combined.add(item);
                        added.add(key);
                    }
                    if (combined.size() >= 9) break;
                }
                for (HomeHistoryItem h : audioList) {
                    if (combined.size() >= 9) break;
                    if (h.data instanceof ServerAudioBook) {
                        String k = "1:" + ((ServerAudioBook) h.data).getId();
                        if (!added.contains(k)) { combined.add(h); added.add(k); }
                    }
                }
                for (HomeHistoryItem h : bookList) {
                    if (combined.size() >= 9) break;
                    if (h.data instanceof Book) {
                        String k = "0:" + ((Book) h.data).getFileName();
                        if (!added.contains(k)) { combined.add(h); added.add(k); }
                    }
                }
                for (HomeHistoryItem h : videoList) {
                    if (combined.size() >= 9) break;
                    if (h.data instanceof HomeVideoLoader.HomeVideoItem) {
                        String k = "2:" + ((HomeVideoLoader.HomeVideoItem) h.data).videoId;
                        if (!added.contains(k)) { combined.add(h); added.add(k); }
                    }
                }

                List<HomeHistoryItem> finalList = combined;
                act.runOnUiThread(() -> {
                    if (!isAdded() || homeHistoryAdapter == null || homeHistorySection == null) return;
                    homeHistoryAdapter.setAudioProgressMap(progressMap);
                    homeHistoryAdapter.setItems(finalList);
                    homeHistorySection.setVisibility(finalList.isEmpty() ? View.GONE : View.VISIBLE);
                });
            } catch (Exception e) {
                Log.e(TAG, "loadUnifiedHistory error", e);
            }
        }).start();
    }

    private void loadBestBooks() {
        new Thread(() -> {
            try {
                android.app.Activity act = getActivity();
                if (act == null) return;
                List<Book> bestBooks = ServerBookLoader.load(act);
                Collections.sort(bestBooks, (b1, b2) -> safeCompare(b1.getName(), b2.getName()));
                final List<Book> bestBooksToShow = bestBooks.size() > BEST_BOOKS_COUNT
                    ? new ArrayList<>(bestBooks.subList(0, BEST_BOOKS_COUNT)) : bestBooks;

                android.app.Activity a = getActivity();
                if (a != null && bestBooksRecycler != null) {
                    a.runOnUiThread(() -> {
                        if (!isAdded() || getContext() == null || bestBooksRecycler == null) return;
                        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
                        bestBooksRecycler.setLayoutManager(layoutManager);
                        bestBooksRecycler.setItemAnimator(new DefaultItemAnimator());
                        int spacing = (int) (10 * getResources().getDisplayMetrics().density);
                        bestBooksRecycler.addItemDecoration(new HorizontalSpacingItemDecoration(spacing));
                        bestBooksAdapter = new BookAdapter(bestBooksToShow, this);
                        bestBooksAdapter.setUseCompactLayout(true);
                        bestBooksRecycler.setAdapter(bestBooksAdapter);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading best books", e);
            }
        }).start();
    }

    private void loadBhaktiBooks() {
        new Thread(() -> {
            try {
                android.app.Activity act = getActivity();
                if (act == null) return;
                List<Book> allBooks = ServerBookLoader.load(act);
                List<Book> bhaktiBooks = new ArrayList<>();
                for (Book book : allBooks) {
                    if ("Bhakti".equals(book.getCategory())) bhaktiBooks.add(book);
                }
                Collections.sort(bhaktiBooks, (b1, b2) -> safeCompare(b1.getName(), b2.getName()));
                allBooksForSearch = new ArrayList<>(allBooks);

                android.app.Activity a = getActivity();
                if (a == null) return;
                a.runOnUiThread(() -> {
                    if (!isAdded() || getContext() == null || bhaktiBooksRecycler == null) return;
                    LinearLayoutManager lm = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
                    bhaktiBooksRecycler.setLayoutManager(lm);
                    bhaktiBooksRecycler.setItemAnimator(new DefaultItemAnimator());
                    int spacing = (int) (10 * getResources().getDisplayMetrics().density);
                    bhaktiBooksRecycler.addItemDecoration(new HorizontalSpacingItemDecoration(spacing));
                    bhaktiBooksAdapter = new BookAdapter(bhaktiBooks, this);
                    bhaktiBooksAdapter.setUseCompactLayout(true);
                    bhaktiBooksRecycler.setAdapter(bhaktiBooksAdapter);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading bhakti books", e);
            }
        }).start();
    }

    private void setupHomeVideos() {
        if (homeVideosRecycler == null || getContext() == null) return;
        homeVideosRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        homeVideosRecycler.setItemAnimator(new DefaultItemAnimator());
        int spacing = (int) (8 * getResources().getDisplayMetrics().density);
        homeVideosRecycler.addItemDecoration(new HorizontalSpacingItemDecoration(spacing));
        homeVideoAdapter = new HomeVideoAdapter();
        homeVideosRecycler.setAdapter(homeVideoAdapter);
        HomeVideoLoader.loadLatest(getContext(), videos -> {
            if (!isAdded() || homeVideoAdapter == null) return;
            if (videos != null && !videos.isEmpty()) {
                homeVideoAdapter.setItems(videos);
            } else {
                Log.d(TAG, "New videos empty from loader – fallback: search Sachchidanand Dantali");
                YouTubeSearchLoader.search(getContext(), "Sachchidanand Dantali Swami", results -> {
                    if (isAdded() && homeVideoAdapter != null && results != null && !results.isEmpty()) {
                        java.util.List<HomeVideoLoader.HomeVideoItem> top = results.size() > 5
                            ? results.subList(0, 5) : results;
                        homeVideoAdapter.setItems(top);
                    }
                });
            }
        });
    }

    private void setupHomeAudio() {
        if (homeAudioRecycler == null || getContext() == null) return;
        homeAudioRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        homeAudioRecycler.setItemAnimator(new DefaultItemAnimator());
        int spacing = (int) (10 * getResources().getDisplayMetrics().density);
        homeAudioRecycler.addItemDecoration(new HorizontalSpacingItemDecoration(spacing));
        homeAudioAdapter = new AudioBookCardAdapter();
        homeAudioAdapter.setUseCompactLayout(true);
        homeAudioAdapter.setOnAudioBookClickListener(book -> {
            if (getActivity() instanceof MainActivity && book != null) {
                ((MainActivity) getActivity()).openAudioBook(book);
            }
        });
        homeAudioRecycler.setAdapter(homeAudioAdapter);
        loadHomeAudioAsync();
    }

    private void setupHistoryRow(View root) {
        if (homeHistoryRecycler == null || getContext() == null) return;
        homeHistoryRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        homeHistoryRecycler.setItemAnimator(new DefaultItemAnimator());
        int spacing = (int) (10 * getResources().getDisplayMetrics().density);
        homeHistoryRecycler.addItemDecoration(new HorizontalSpacingItemDecoration(spacing));
        homeHistoryAdapter = new HomeHistoryAdapter();
        homeHistoryAdapter.setListener(new HomeHistoryAdapter.Listener() {
            @Override
            public void onBookClick(Book book) {
                if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).openBook(book);
            }
            @Override
            public void onAudioClick(ServerAudioBook book) {
                if (getActivity() instanceof MainActivity && book != null)
                    ((MainActivity) getActivity()).openAudioBook(book);
            }
            @Override
            public void onVideoClick(HomeVideoLoader.HomeVideoItem video) {
                if (video == null || video.videoId == null || getActivity() == null) return;
                try {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + video.videoId));
                    startActivity(i);
                } catch (Exception e) {
                    Log.e(TAG, "open video", e);
                }
            }
        });
        homeHistoryRecycler.setAdapter(homeHistoryAdapter);
    }

    private void loadHomeAudioAsync() {
        new Thread(() -> {
            try {
                android.app.Activity act = getActivity();
                if (act == null) return;
                java.util.List<ServerAudioBook> loaded = new ArrayList<>();
                String base = act.getString(R.string.server_books_base_url);
                if (base != null) { base = base.trim(); if (!base.isEmpty() && !base.endsWith("/")) base += "/"; }
                String url = (base != null ? base : "") + "audio_list.json";
                OkHttpClient client = new OkHttpClient.Builder().connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS).readTimeout(20, java.util.concurrent.TimeUnit.SECONDS).build();
                okhttp3.Request req = new okhttp3.Request.Builder().url(url)
                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")
                        .addHeader("Accept", "application/json").build();
                try (okhttp3.Response resp = client.newCall(req).execute()) {
                    if (resp.isSuccessful() && resp.body() != null) {
                        String body = resp.body().string();
                        if (body != null && body.trim().startsWith("{"))
                            loaded = ServerAudioParser.parseBooks(body);
                    }
                }
                if (loaded == null || loaded.isEmpty()) {
                    for (String assetName : new String[]{"audio_list_main.json", "audio_list_fallback.json"}) {
                        try (java.io.BufferedReader r = new java.io.BufferedReader(
                                new java.io.InputStreamReader(act.getAssets().open(assetName), java.nio.charset.StandardCharsets.UTF_8))) {
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = r.readLine()) != null) sb.append(line);
                            loaded = ServerAudioParser.parseBooks(sb.toString());
                            if (loaded != null && !loaded.isEmpty()) break;
                        } catch (Exception ignored) { }
                    }
                }
                if (loaded == null) loaded = ServerAudioParser.demoBooks();
                loaded = mapAudioThumbnails(act, loaded, base);
                java.util.Collections.sort(loaded, (a, b) -> (b.getTitle() != null ? b.getTitle() : "").compareTo(a.getTitle() != null ? a.getTitle() : ""));
                java.util.List<ServerAudioBook> toShow = loaded.size() > 6 ? loaded.subList(0, 6) : loaded;
                java.util.List<ServerAudioBook> finalList = toShow;
                act.runOnUiThread(() -> {
                    if (isAdded() && homeAudioAdapter != null) {
                        homeAudioAdapter.setBooks(finalList);
                    }
                });
            } catch (Throwable t) {
                Log.e(TAG, "loadHomeAudio error", t);
            }
        }).start();
    }

    private Map<String, Integer> loadAudioProgressMap(Activity act, List<ServerAudioBook> books) {
        Map<String, Integer> map = new HashMap<>();
        if (books == null || act == null) return map;
        try {
            SharedPreferences prefs = act.getSharedPreferences(PREFS_AUDIO, Context.MODE_PRIVATE);
            for (ServerAudioBook b : books) {
                if (b == null || b.getId() == null) continue;
                String lastId = prefs.getString(KEY_LAST_PART_ID + b.getId(), null);
                if (lastId == null) continue;
                List<ServerAudioPart> parts = b.getParts();
                if (parts == null || parts.isEmpty()) continue;
                int idx = -1;
                for (int i = 0; i < parts.size(); i++) {
                    if (lastId.equals(parts.get(i).getId())) {
                        idx = i;
                        break;
                    }
                }
                if (idx >= 0 && idx < parts.size() - 1) {
                    int pct = (int) ((idx + 1) * 100.0 / parts.size());
                    if (pct > 0 && pct < 100) map.put(b.getId(), pct);
                }
            }
        } catch (Exception ignored) {}
        return map;
    }

    private void setupViewAllClicks(View root) {
        TextView videosViewAll = root.findViewById(R.id.home_videos_view_all);
        TextView audioViewAll = root.findViewById(R.id.home_audio_view_all);
        TextView booksViewAll = root.findViewById(R.id.home_books_view_all);
        if (videosViewAll != null) {
            videosViewAll.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).switchToTab(R.id.nav_videos);
                }
            });
        }
        if (audioViewAll != null) {
            audioViewAll.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).switchToTab(R.id.nav_audio);
                }
            });
        }
        if (booksViewAll != null) {
            booksViewAll.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).switchToTab(R.id.nav_books);
                }
            });
        }
    }

    private String detectCategory(String bookName) {
        String lowerName = bookName.toLowerCase();
        
        // Bhakti category - more comprehensive keywords
        if (lowerName.contains("ભક્તિ") || lowerName.contains("bhakti") || 
            lowerName.contains("ભજન") || lowerName.contains("bhajan") ||
            lowerName.contains("ભાગવત") || lowerName.contains("bhagwat") ||
            lowerName.contains("વિષ્ણુ") || lowerName.contains("vishnu") ||
            lowerName.contains("રામાયણ") || lowerName.contains("ramayan") ||
            lowerName.contains("રામ") || lowerName.contains("ram") ||
            lowerName.contains("કૃષ્ણ") || lowerName.contains("krishna") ||
            lowerName.contains("ભર્તૃહરિ") || lowerName.contains("bhartrihari") ||
            lowerName.contains("શતક") || lowerName.contains("shata") ||
            lowerName.contains("સહસ્રનામ") || lowerName.contains("sahasranam")) {
            return "Bhakti";
        } else if (lowerName.contains("યાત્રા") || lowerName.contains("yatra") ||
                   lowerName.contains("પ્રવાસ") || lowerName.contains("travel") ||
                   lowerName.contains("પ્રવાસનાં") || lowerName.contains("પ્રવાસની") ||
                   lowerName.contains("તીર્થ") || lowerName.contains("tirth") ||
                   lowerName.contains("મુલાકાત") || lowerName.contains("mulakat") ||
                   lowerName.contains("આફ્રિકા") || lowerName.contains("africa") ||
                   lowerName.contains("યુરોપ") || lowerName.contains("europe") ||
                   lowerName.contains("ટર્કી") || lowerName.contains("turkey") ||
                   lowerName.contains("ઈજિપ્ત") || lowerName.contains("egypt") ||
                   lowerName.contains("આંદામાન") || lowerName.contains("andaman")) {
            return "Yatra";
        } else if (lowerName.contains("જીવન") || lowerName.contains("jeevan") ||
                   lowerName.contains("ચરિત્ર") || lowerName.contains("charitra") ||
                   lowerName.contains("જીવનકથા") || lowerName.contains("jeevankatha") ||
                   lowerName.contains("અનુભવ") || lowerName.contains("anubhav") ||
                   lowerName.contains("બાયપાસ") || lowerName.contains("bypass")) {
            return "Jeevan";
        }
        
        return "Updesh";
    }

    @Override
    public void onBookClick(Book book) {
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openBook(book);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded() && getContext() != null && homeHistoryRecycler != null) {
            loadUnifiedHistory();
        }
        // Try to (re)start hero video playback
        if (heroVideoView != null) {
            try {
                if (!heroVideoView.isPlaying()) {
                    setupHeroVideo();
                }
            } catch (Throwable ignored) {}
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (suvicharHideRunnable != null) suvicharHandler.removeCallbacks(suvicharHideRunnable);
        if (heroPhotoRunnable != null) heroPhotoHandler.removeCallbacks(heroPhotoRunnable);
        if (autoScrollHandler != null && autoScrollRunnable != null) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable);
        }
        if (heroVideoView != null) {
            try { heroVideoView.stopPlayback(); } catch (Throwable ignored) {}
        }
        suvicharContainer = null;
        suvicharRecycler = null;
        suvicharAdapter = null;
        heroFinalPhoto = null;
        photoViewPager = null;
        heroVideoView = null;
        homeHistorySection = null;
        homeHistoryRecycler = null;
        homeHistoryAdapter = null;
        bestBooksRecycler = null;
        bhaktiBooksRecycler = null;
        searchResultsRecycler = null;
    }

    private static class SuvicharItem {
        final String text;
        final String author;
        SuvicharItem(String text, String author) {
            this.text = text;
            this.author = author;
        }
    }

    private static class SuvicharAdapter extends RecyclerView.Adapter<SuvicharAdapter.ViewHolder> {
        private final List<SuvicharItem> list;
        SuvicharAdapter(List<SuvicharItem> list) { this.list = list; }
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_suvichar, parent, false);
            return new ViewHolder(v);
        }
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            SuvicharItem item = list.get(position);
            String t = item.text != null ? item.text : "";
            holder.text.setText(t);
            holder.author.setText(item.author != null ? item.author : "");
            // ScrollView ko scroll priority – parent (page) scroll na le
            if (holder.suvicharScroll != null) {
                holder.suvicharScroll.setOnTouchListener((v, event) -> {
                    int action = event.getActionMasked();
                    if (action == android.view.MotionEvent.ACTION_DOWN) {
                        android.view.ViewParent p = v.getParent();
                        while (p != null) {
                            p.requestDisallowInterceptTouchEvent(true);
                            p = p.getParent();
                        }
                    } else if (action == android.view.MotionEvent.ACTION_UP || action == android.view.MotionEvent.ACTION_CANCEL) {
                        android.view.ViewParent p = v.getParent();
                        while (p != null) {
                            p.requestDisallowInterceptTouchEvent(false);
                            p = p.getParent();
                        }
                    }
                    return false;
                });
            }
        }
        @Override
        public int getItemCount() { return list.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder {
            final android.widget.TextView text;
            final android.widget.TextView author;
            final android.widget.ScrollView suvicharScroll;
            ViewHolder(View itemView) {
                super(itemView);
                text = itemView.findViewById(R.id.suvichar_text);
                author = itemView.findViewById(R.id.suvichar_author);
                suvicharScroll = itemView.findViewById(R.id.suvichar_scroll);
            }
        }
    }
}

