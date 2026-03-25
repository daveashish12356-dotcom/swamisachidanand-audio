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

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputEditText;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;

public class HomeFragment extends Fragment implements BookAdapter.OnBookClickListener {

    private static final String TAG = "HomeFragment";
    private static final String PREFS_NAME = "reading_progress";
    private static final String KEY_RECENT_BOOK = "recent_book_name";
    private static final String KEY_RECENT_BOOKS = "recent_books_list";
    private static final String PREFS_SUVICHAR = "suvichar_prefs";
    private static final String KEY_LAST_SUVICHAR_TEXT = "last_suvichar_text";
    private static final String KEY_LAST_SUVICHAR_AUTHOR = "last_suvichar_author";
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
    private RecyclerView homePravachanRecycler;
    private com.google.android.material.textfield.TextInputEditText searchInput;
    private ImageView clearSearch, micButton;
    private LinearLayout searchResultsSection;
    private RecyclerView searchResultsRecycler;
    private TextView searchNoResults;
    private UnifiedSearchAdapter unifiedSearchAdapter;
    private List<Book> allBooksForSearch = new ArrayList<>();

    private HomeHistoryAdapter homeHistoryAdapter;
    private BookAdapter bestBooksAdapter;
    private BookAdapter bhaktiBooksAdapter;
    private HomeVideoAdapter homeVideoAdapter;
    private AudioBookCardAdapter homeAudioAdapter;
    private PravachanAdapter homePravachanAdapter;
    private TextView homePravachanViewAll;
    private final List<PravachanItem> allHomePravachans = new ArrayList<>();
    private List<ServerAudioBook> allHomeAudio;
    private List<HomeVideoLoader.HomeVideoItem> allHomeVideos;
    
    private Handler autoScrollHandler;
    private Runnable autoScrollRunnable;
    private PhotoCarouselAdapter photoCarouselAdapter;
    private boolean videoPlaybackInProgress;

    // Simple hero video at top (Padma Bhushan clip) – FitVideoView for fit-without-zoom
    private FitVideoView heroVideoView;
    /** Video ke baad: server gallery photos slideshow, nahi to static photo */
    private ImageView heroFinalPhoto;
    private ViewPager2 heroPhotoSlideshow;
    private GallerySlideshowPagerAdapter heroSlideshowAdapter;
    private final List<String> heroPhotoUrls = new ArrayList<>();
    private static final String HERO_GALLERY_BASE = "https://daveashish12356-dotcom.github.io/swamisachidanand-audio/public/gallery/";
    private static final String HERO_GALLERY_LIST_URL = HERO_GALLERY_BASE + "list.json?v=2";
    private static final long HERO_VIDEO_TO_PHOTO_MS = 45_000L;
    // Hero box slideshow – 6 seconds so अगला photo आराम से load हो सके
    private static final int HERO_SLIDESHOW_INTERVAL_MS = 6000;
    private final Handler heroPhotoHandler = new Handler(Looper.getMainLooper());
    private Runnable heroPhotoRunnable;
    private final Handler heroSlideshowHandler = new Handler(Looper.getMainLooper());
    private Runnable heroSlideshowRunnable;
    private boolean heroPhotoShown = false;

    // Best books = first N from assets (no fixed list). Purani books hatao, nayi PDFs dalo assets me — wahi dikhengi.
    private static final int BEST_BOOKS_COUNT = 8;

    /** Chota suvichar → 15 sec ruke, badha → 30 sec (jaise movie credits ke baad) */
    private static final long SUVICHAR_DISPLAY_MS = 15_000L;
    private static final long SUVICHAR_DISPLAY_LONG_MS = 30_000L;
    /** Badha suvichar = text length above this → 30 sec, else 15 sec */
    private static final int SUVICHAR_LONG_THRESHOLD = 150;
    /** Sirf beech wale text par: pehli line niche se last line upper tak dhire dhire scroll – duration (sirf ek baar) */
    private static final long SUVICHAR_TEXT_SCROLL_MS = 28_000L;
    private View suvicharContainer;
    private RecyclerView suvicharRecycler;
    private final List<SuvicharItem> suvicharList = new ArrayList<>();
    private SuvicharAdapter suvicharAdapter;
    private final Handler suvicharHandler = new Handler(Looper.getMainLooper());
    private Runnable suvicharHideRunnable;
    private boolean suvicharShowing = false;
    private AdView homeBottomBannerAd;

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
            heroPhotoSlideshow = view.findViewById(R.id.hero_photo_slideshow);
            setupHeroPhotoSlideshow();
            setupPhotoBannerAnimation(view);

            homeHistorySection = view.findViewById(R.id.home_history_section);
            homeHistoryRecycler = view.findViewById(R.id.home_history_recycler);
            bestBooksRecycler = view.findViewById(R.id.best_books_recycler);
            bhaktiBooksRecycler = view.findViewById(R.id.bhakti_books_recycler);
            homeVideosRecycler = view.findViewById(R.id.home_videos_recycler);
            homeAudioRecycler = view.findViewById(R.id.home_audio_recycler);
            homePravachanRecycler = view.findViewById(R.id.home_pravachan_recycler);

            setupHomeVideos();
            setupHomeAudio();
            setupHomePravachan(view);
            setupHistoryRow(view);
            setupViewAllClicks(view);
            setupBookStoreSection(view);
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
            View quickPravachan = view.findViewById(R.id.home_quick_pravachan);
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
            if (quickPravachan != null) {
                quickPravachan.setOnClickListener(v -> switchToBottomNavTab(R.id.nav_pravachan));
            }

            View photoGalleryLink = view.findViewById(R.id.home_photo_gallery_link);
            if (photoGalleryLink != null) {
                photoGalleryLink.setOnClickListener(v -> {
                    if (getContext() != null) {
                        startActivity(new android.content.Intent(getContext(), PhotoGalleryActivity.class));
                    }
                });
            }

            // Banner at the very bottom (scroll to see it)
            setupHomeBottomBannerAd(view);

        if (getContext() != null && searchResultsRecycler != null) {
            searchResultsRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            searchResultsRecycler.setItemAnimator(new DefaultItemAnimator());
            int spacing = (int) (10 * getResources().getDisplayMetrics().density);
            searchResultsRecycler.addItemDecoration(new HorizontalSpacingItemDecoration(spacing));
            unifiedSearchAdapter = new UnifiedSearchAdapter();
            unifiedSearchAdapter.setBookClickListener(this);
            unifiedSearchAdapter.setAudioClickListener(book -> {
                if (getActivity() instanceof MainActivity && book != null) {
                    ((MainActivity) getActivity()).openAudioBook(book);
                }
            });
            searchResultsRecycler.setAdapter(unifiedSearchAdapter);
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
                    loadBestAndBhaktiBooksCombined();
                }, 380);
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
            showSuvicharWithAnimation(displayMs);
        } else {
            Log.d(TAG, "suvichar: no assets, will try server");
        }
        // Hamesha server se fetch karo – website par change aaye to app me bhi aaye
        loadSuvichar();
    }

    private void setupHomeBottomBannerAd(View root) {
        try {
            homeBottomBannerAd = root.findViewById(R.id.home_bottom_banner);
            if (homeBottomBannerAd == null) return;
            AdRequest request = new AdRequest.Builder().build();
            homeBottomBannerAd.setAdListener(AdLog.wrapBannerListener("home_bottom", new AdListener() {
                @Override
                public void onAdLoaded() {
                    try {
                        if (homeBottomBannerAd.getVisibility() != View.VISIBLE) {
                            homeBottomBannerAd.setAlpha(0f);
                            homeBottomBannerAd.setTranslationY(12f);
                            homeBottomBannerAd.setVisibility(View.VISIBLE);
                            homeBottomBannerAd.animate()
                                    .translationY(0f)
                                    .alpha(1f)
                                    .setDuration(400L)
                                    .setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f))
                                    .start();
                        }
                    } catch (Throwable ignore) {}
                }

                @Override
                public void onAdFailedToLoad(LoadAdError adError) {
                    try {
                        String code = adError != null ? String.valueOf(adError.getCode()) : "null";
                        String msg = adError != null ? adError.getMessage() : "null";
                        String domain = adError != null ? adError.getDomain() : "null";
                        Log.w(TAG, "home bottom banner failed: code=" + code + ", domain=" + domain + ", message=" + msg);
                        homeBottomBannerAd.setVisibility(View.VISIBLE);
                        homeBottomBannerAd.setAlpha(0.25f);
                    } catch (Throwable ignore) {}
                }
            }));
            homeBottomBannerAd.postDelayed(() -> {
                try {
                    if (homeBottomBannerAd != null && isAdded()) {
                        AdLog.bannerRequest("home_bottom");
                        BannerAdHelper.loadWhenReady(requireContext(), homeBottomBannerAd, request);
                    }
                } catch (Throwable t2) {
                    Log.e(TAG, "bottom banner loadAd delayed", t2);
                }
            }, 1200);
        } catch (Throwable t) {
            Log.e(TAG, "setupHomeBottomBannerAd", t);
        }
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

        // 1) Try Firebase Cloud Function first (Firebase-only flow)
        final String firebaseUrl = "https://us-central1-swami-sachidanand.cloudfunctions.net/sendSuvichar";

        // 2) Old GitHub JSON (fallback only, in case Firebase not reachable)
        final String baseUrl;
        final String primaryUrl;
        final String fallbackUrl;
        try {
            String b = getString(R.string.server_books_base_url);
            b = b != null ? b.trim() : "";
            String normalized = b.isEmpty() ? "" : (b.endsWith("/") ? b : b + "/");
            baseUrl = normalized;
            primaryUrl = normalized + "public/suvichar_config.json";
            fallbackUrl = buildSuvicharRawUrl(normalized);
        } catch (Exception e) {
            return;
        }

        Log.d(TAG, "suvichar: loadSuvichar firebaseUrl=" + firebaseUrl + " primaryUrl=" + primaryUrl);
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(6, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();

                String body = null;

                // First try Firebase function (preferred)
                try (Response response = client.newCall(new Request.Builder().url(firebaseUrl).build()).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        body = response.body().string();
                        Log.d(TAG, "suvichar: firebase body ok, len=" + body.length());
                    } else {
                        Log.w(TAG, "suvichar: firebase response not ok code=" + (response != null ? response.code() : -1));
                    }
                } catch (Throwable tf) {
                    Log.e(TAG, "suvichar: firebase fetch failed", tf);
                }

                // If Firebase failed, fall back to GitHub JSON (old behaviour)
                if (body == null) {
                    Request req1 = new Request.Builder().url(primaryUrl).build();
                    try (Response response = client.newCall(req1).execute()) {
                        if (response.isSuccessful() && response.body() != null)
                            body = response.body().string();
                    }
                    if (body == null && fallbackUrl != null) {
                        try (Response response = client.newCall(new Request.Builder().url(fallbackUrl).build()).execute()) {
                            if (response.isSuccessful() && response.body() != null)
                                body = response.body().string();
                        }
                    }
                }
                if (body == null) {
                    Log.w(TAG, "suvichar: server body null, hide");
                    activity.runOnUiThread(this::hideSuvichar);
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

                // Server (Firebase) already sends suvichar notifications via FCM.
                // Yahan sirf latest text local prefs me remember karte hain (UI ke liye), extra notification nahi.
                if (enabled && !finalList.isEmpty()) {
                    try {
                        String latestText = finalList.get(0).text != null ? finalList.get(0).text.trim() : "";
                        String latestAuthor = finalList.get(0).author != null ? finalList.get(0).author.trim() : "";
                        if (!latestText.isEmpty()) {
                            SharedPreferences prefs = activity.getSharedPreferences(PREFS_SUVICHAR, Context.MODE_PRIVATE);
                            prefs.edit()
                                    .putString(KEY_LAST_SUVICHAR_TEXT, latestText)
                                    .putString(KEY_LAST_SUVICHAR_AUTHOR, latestAuthor)
                                    .apply();
                        }
                    } catch (Throwable t) {
                        Log.e(TAG, "suvichar: failed to store latest text", t);
                    }
                }

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
                        // Rebind ke baad layout hone do, phir scroll (pehli line niche → upper, phir 30 sec sdhir)
                        suvicharRecycler.postDelayed(() -> startSuvicharTextScrollAnimation(SUVICHAR_DISPLAY_LONG_MS), 120);
                        return;
                    }
                    suvicharList.clear();
                    if (finalList.isEmpty()) {
                        hideSuvichar();
                    } else {
                        suvicharList.addAll(finalList);
                        if (suvicharAdapter != null) suvicharAdapter.notifyDataSetChanged();
                        showSuvicharWithAnimation(displayMsFinal);
                    }
                });
            } catch (Throwable t) {
                Log.e(TAG, "loadSuvichar error", t);
                activity.runOnUiThread(this::hideSuvichar);
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

    /**
     * Suvichar dikhao – header/author static; sirf beech wala text pehli line se last line tak dhire dhire scroll.
     * Scroll khatam ke baad 15 sec (chota) ya 30 sec (badha) ruk ke hide.
     */
    private void showSuvicharWithAnimation(long displayMs) {
        if (suvicharContainer == null) return;
        suvicharShowing = true;
        suvicharHandler.removeCallbacks(suvicharHideRunnable);
        suvicharContainer.setVisibility(View.VISIBLE);
        suvicharContainer.setAlpha(1f);
        suvicharContainer.setTranslationY(0f);
        suvicharHideRunnable = () -> {
            suvicharShowing = false;
            hideSuvichar();
        };
        startSuvicharTextScrollAnimation(displayMs);
    }

    private void startSuvicharTextScrollAnimation(long displayMsAfterScroll) {
        startSuvicharTextScrollAnimation(displayMsAfterScroll, 0);
    }

    private void startSuvicharTextScrollAnimation(long displayMsAfterScroll, int retryCount) {
        if (suvicharRecycler == null) {
            suvicharHandler.postDelayed(suvicharHideRunnable, displayMsAfterScroll);
            return;
        }
        // RecyclerView par listener – server list update ke baad bhi recycler layout fire karega
        if (suvicharRecycler == null) {
            suvicharHandler.postDelayed(suvicharHideRunnable, displayMsAfterScroll);
            return;
        }
        // Capture view reference: field becomes null in onDestroyView while a pending layout callback may still run.
        final RecyclerView recyclerForLayout = suvicharRecycler;
        recyclerForLayout.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                try {
                    recyclerForLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } catch (Throwable ignored) {
                }
                if (!isAdded()) return;
                View itemView = recyclerForLayout.getChildAt(0);
                if (itemView == null && retryCount < 3) {
                    Log.d(TAG, "suvichar scroll: itemView null, retry " + (retryCount + 1));
                    suvicharHandler.postDelayed(() -> startSuvicharTextScrollAnimation(displayMsAfterScroll, retryCount + 1), 150);
                    return;
                }
                if (itemView == null) {
                    Log.d(TAG, "suvichar scroll: itemView null after retries, skip scroll");
                    suvicharHandler.postDelayed(suvicharHideRunnable, displayMsAfterScroll);
                    return;
                }
                android.widget.ScrollView scroll = itemView.findViewById(R.id.suvichar_scroll);
                if (scroll == null || scroll.getChildCount() == 0) {
                    Log.d(TAG, "suvichar scroll: scroll null or no child, skip");
                    suvicharHandler.postDelayed(suvicharHideRunnable, displayMsAfterScroll);
                    return;
                }
                View textChild = scroll.getChildAt(0);
                if (textChild.getHeight() == 0 || scroll.getHeight() == 0) {
                    scroll.post(() -> tryStartScrollAnim(scroll, displayMsAfterScroll));
                } else {
                    tryStartScrollAnim(scroll, displayMsAfterScroll);
                }
            }
        });
    }

    private void tryStartScrollAnim(android.widget.ScrollView scroll, long displayMsAfterScroll) {
        if (!isAdded() || scroll.getChildCount() == 0) return;
        View textChild = scroll.getChildAt(0);
        int contentHeight = textChild.getHeight();
        int scrollHeight = scroll.getHeight();
        scroll.setScrollY(0);
        // Pehla animation: first line NICHE se → scroll karke last line upper tak (same as credits)
        int range1 = Math.max(0, scrollHeight + contentHeight);
        if (range1 <= 0) {
            startSecondScrollAnim(textChild, scrollHeight, displayMsAfterScroll);
            return;
        }
        textChild.setTranslationY(scrollHeight);
        android.animation.ValueAnimator va1 = android.animation.ValueAnimator.ofInt(0, range1);
        va1.setDuration(SUVICHAR_TEXT_SCROLL_MS);
        va1.setInterpolator(new android.view.animation.LinearInterpolator());
        va1.addUpdateListener(anim -> {
            int y = (Integer) anim.getAnimatedValue();
            textChild.setTranslationY(scrollHeight - y);
        });
        va1.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                startSecondScrollAnim(textChild, scrollHeight, displayMsAfterScroll);
            }
        });
        va1.start();
    }

    /** Dusra animation: first line niche se upper tak, phir 30 sec sdhir */
    private void startSecondScrollAnim(View textChild, int scrollHeight, long displayMsAfterScroll) {
        if (!isAdded()) return;
        textChild.setTranslationY(scrollHeight);
        android.animation.ValueAnimator va2 = android.animation.ValueAnimator.ofInt(0, scrollHeight);
        va2.setDuration(SUVICHAR_TEXT_SCROLL_MS);
        va2.setInterpolator(new android.view.animation.LinearInterpolator());
        va2.addUpdateListener(anim -> {
            int y = (Integer) anim.getAnimatedValue();
            textChild.setTranslationY(scrollHeight - y);
        });
        va2.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                suvicharHandler.postDelayed(suvicharHideRunnable, displayMsAfterScroll);
            }
        });
        va2.start();
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
        // Slower interval so next photos get enough time to load (6 seconds instead of 5)
        final int intervalMs = 6000;
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

    /** Hero box me video ke baad server gallery photos slideshow – setup adapter, animation, load list. */
    private void setupHeroPhotoSlideshow() {
        if (heroPhotoSlideshow == null || getContext() == null) return;
        heroSlideshowAdapter = new GallerySlideshowPagerAdapter();
        heroSlideshowAdapter.setListener(new GallerySlideshowPagerAdapter.Listener() {
            @Override
            public void onSlideClick(int position) {
                if (getContext() != null) {
                    startActivity(new Intent(getContext(), PhotoGalleryActivity.class));
                }
            }
            @Override
            public void onSlideLoadFailed(int position) { /* ignore on home */ }
        });
        heroPhotoSlideshow.setAdapter(heroSlideshowAdapter);
        heroPhotoSlideshow.setOffscreenPageLimit(1);
        // हल्की zoom-out + fade animation – home hero ke liye soft feel
        final float density = getResources().getDisplayMetrics().density;
        heroPhotoSlideshow.setPageTransformer(new ViewPager2.PageTransformer() {
            @Override
            public void transformPage(@NonNull View page, float position) {
                float abs = Math.abs(position);
                int w = page.getWidth();
                if (w <= 0) return;

                page.setCameraDistance(density * 8000f);
                page.setPivotX(w * 0.5f);
                page.setPivotY(page.getHeight() * 0.5f);

                // हल्का side slide
                page.setTranslationX(position * w * 0.25f);

                // center बड़ा, side थोड़ा छोटा
                float scale = 1f - 0.15f * abs;
                page.setScaleX(scale);
                page.setScaleY(scale);

                // side images थोड़ा fade
                float alpha = 1f - 0.4f * abs;
                page.setAlpha(Math.max(0.4f, alpha));
            }
        });
        loadHeroGalleryUrls();
    }

    private void loadHeroGalleryUrls() {
        Activity activity = getActivity();
        if (activity == null) return;
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();
                Request req = new Request.Builder().url(HERO_GALLERY_LIST_URL).build();
                try (Response resp = client.newCall(req).execute()) {
                    if (resp.isSuccessful() && resp.body() != null) {
                        String json = resp.body().string();
                        if (json != null && json.startsWith("\uFEFF")) json = json.substring(1);
                        JSONArray arr = new JSONArray(json);
                        List<String> urls = new ArrayList<>();
                        for (int i = 0; i < arr.length(); i++) {
                            String name = arr.optString(i, "").trim();
                            if (!name.isEmpty()) urls.add(HERO_GALLERY_BASE + name);
                        }
                        synchronized (heroPhotoUrls) {
                            heroPhotoUrls.clear();
                            heroPhotoUrls.addAll(urls);
                        }
                        Log.d(TAG, "Hero gallery urls loaded: " + heroPhotoUrls.size());
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "loadHeroGalleryUrls error", t);
            }
        }).start();
    }

    private void startHeroSlideshowAutoAdvance() {
        stopHeroSlideshowAutoAdvance();
        if (heroPhotoSlideshow == null || heroSlideshowAdapter == null || heroSlideshowAdapter.getItemCount() < 2) return;
        heroSlideshowRunnable = new Runnable() {
            @Override
            public void run() {
                if (heroPhotoSlideshow == null || heroSlideshowAdapter == null || !isAdded()) return;
                int total = heroSlideshowAdapter.getItemCount();
                if (total < 2) return;
                int next = (heroPhotoSlideshow.getCurrentItem() + 1) % total;
                // अगला hero photo jab tak ready na ho, tab tak रुक के wait karo
                if (!heroSlideshowAdapter.isLoaded(next)) {
                    heroSlideshowHandler.postDelayed(this, 500);
                    return;
                }
                heroPhotoSlideshow.setCurrentItem(next, true);
                heroSlideshowHandler.postDelayed(this, HERO_SLIDESHOW_INTERVAL_MS);
            }
        };
        heroSlideshowHandler.postDelayed(heroSlideshowRunnable, HERO_SLIDESHOW_INTERVAL_MS);
    }

    private void stopHeroSlideshowAutoAdvance() {
        if (heroSlideshowRunnable != null) {
            heroSlideshowHandler.removeCallbacks(heroSlideshowRunnable);
            heroSlideshowRunnable = null;
        }
    }

    /** Hero video: in-app (res/raw), auto-play, muted, loop. */
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

    /** Video ke 45 sec baad photo dikhao: server gallery slideshow, nahi to static photo. */
    private void scheduleHeroPhotoAfterVideo() {
        if (heroPhotoRunnable != null) heroPhotoHandler.removeCallbacks(heroPhotoRunnable);
        heroPhotoRunnable = () -> {
            if (heroPhotoShown || !isAdded()) return;
            heroPhotoShown = true;
            if (heroVideoView != null) {
                try { heroVideoView.stopPlayback(); } catch (Throwable ignored) {}
                heroVideoView.animate().alpha(0f).setDuration(600).withEndAction(() -> {
                    if (heroVideoView != null) heroVideoView.setVisibility(View.GONE);
                    showHeroPhotoAfterVideo();
                }).start();
            } else {
                showHeroPhotoAfterVideo();
            }
        };
        heroPhotoHandler.postDelayed(heroPhotoRunnable, HERO_VIDEO_TO_PHOTO_MS);
    }

    private void showHeroPhotoAfterVideo() {
        List<String> copy;
        synchronized (heroPhotoUrls) {
            copy = heroPhotoUrls.isEmpty() ? null : new ArrayList<>(heroPhotoUrls);
        }
        if (copy != null && !copy.isEmpty() && heroPhotoSlideshow != null && heroSlideshowAdapter != null) {
            Collections.shuffle(copy);
            heroSlideshowAdapter.setUrls(copy);
            heroPhotoSlideshow.setCurrentItem(0, false);
            heroPhotoSlideshow.setVisibility(View.VISIBLE);
            heroPhotoSlideshow.setAlpha(0f);
            heroPhotoSlideshow.animate().alpha(1f).setDuration(600).start();
            if (heroFinalPhoto != null) heroFinalPhoto.setVisibility(View.GONE);
            startHeroSlideshowAutoAdvance();
        } else {
            if (heroPhotoSlideshow != null) heroPhotoSlideshow.setVisibility(View.GONE);
            if (heroFinalPhoto != null) {
                heroFinalPhoto.setVisibility(View.VISIBLE);
                heroFinalPhoto.setAlpha(0f);
                heroFinalPhoto.animate().alpha(1f).setDuration(600).start();
            }
        }
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
            // IME search par alag page nahi kholna – yahi par filter hoga
            searchInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    // keyboard hide kar do, results already filter ho chuke honge
                    try {
                        android.view.inputmethod.InputMethodManager imm =
                                (android.view.inputmethod.InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    } catch (Throwable ignored) {}
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
            }
        }
    }

    private void filterBooks(String query) {
        if (unifiedSearchAdapter == null || allBooksForSearch == null) return;

        String queryLower = query.trim().toLowerCase();
        if (queryLower.isEmpty()) {
            unifiedSearchAdapter.setItems(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
            if (searchResultsRecycler != null) searchResultsRecycler.setVisibility(View.GONE);
            if (searchNoResults != null) searchNoResults.setVisibility(View.GONE);
            return;
        }

        List<Book> filtered = new ArrayList<>();
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

        Collections.sort(filtered, (a, b) -> {
            String an = a.getName() != null ? a.getName() : "";
            String bn = b.getName() != null ? b.getName() : "";
            boolean aStart = an.toLowerCase().startsWith(queryLower) || (a.getSearchableText() != null && a.getSearchableText().toLowerCase().startsWith(queryLower));
            boolean bStart = bn.toLowerCase().startsWith(queryLower) || (b.getSearchableText() != null && b.getSearchableText().toLowerCase().startsWith(queryLower));
            if (aStart != bStart) return aStart ? -1 : 1;
            return an.compareToIgnoreCase(bn);
        });

        List<ServerAudioBook> filteredAudio = getFilteredAudioForSearch(queryLower);

        // Pehle books + audio dikhao; videos ke liye YouTube search (Ramayan jaisa query par videos aaye)
        unifiedSearchAdapter.setItems(filtered, filteredAudio, new ArrayList<>());
        int totalSoFar = filtered.size() + filteredAudio.size();
        if (searchResultsRecycler != null) {
            searchResultsRecycler.setVisibility(totalSoFar > 0 ? View.VISIBLE : View.GONE);
        }
        if (searchNoResults != null) {
            searchNoResults.setVisibility(totalSoFar == 0 ? View.VISIBLE : View.GONE);
        }

        final String queryForVideos = queryLower;
        Context ctx = getContext();
        if (ctx != null) {
            YouTubeSearchLoader.search(ctx, query.trim(), videoResults -> {
            if (!isAdded() || unifiedSearchAdapter == null) return;
            String current = searchInput != null && searchInput.getText() != null
                    ? searchInput.getText().toString().trim().toLowerCase() : "";
            if (!current.equals(queryForVideos)) return;
            List<HomeVideoLoader.HomeVideoItem> list = videoResults != null ? videoResults : new ArrayList<>();
            unifiedSearchAdapter.setItems(filtered, filteredAudio, list);
            int total = filtered.size() + filteredAudio.size() + list.size();
            if (searchResultsRecycler != null) searchResultsRecycler.setVisibility(total > 0 ? View.VISIBLE : View.GONE);
            if (searchNoResults != null) searchNoResults.setVisibility(total == 0 ? View.VISIBLE : View.GONE);
            });
        }
    }

    private List<ServerAudioBook> getFilteredAudioForSearch(String queryLower) {
        if (allHomeAudio == null) return new ArrayList<>();
        List<ServerAudioBook> filtered = new ArrayList<>();
        String q = queryLower != null ? queryLower.trim() : "";
        if (q.isEmpty()) return filtered;
        for (ServerAudioBook b : allHomeAudio) {
            if (b == null) continue;
            String title = b.getTitle() != null ? b.getTitle().toLowerCase() : "";
            String id = b.getId() != null ? b.getId().toLowerCase() : "";
            if (SearchHelper.matches(b.getTitle(), q)) { filtered.add(b); continue; }
            if (title.contains(q) || id.contains(q)) { filtered.add(b); continue; }
            String[] words = q.split("\\s+");
            for (String w : words) {
                if (w.length() >= 2 && (title.contains(w) || id.contains(w))) {
                    filtered.add(b);
                    break;
                }
            }
        }
        return filtered;
    }

    private List<HomeVideoLoader.HomeVideoItem> getFilteredVideosForSearch(String queryLower) {
        if (allHomeVideos == null) return new ArrayList<>();
        List<HomeVideoLoader.HomeVideoItem> filtered = new ArrayList<>();
        String q = queryLower != null ? queryLower.trim() : "";
        if (q.isEmpty()) return filtered;
        for (HomeVideoLoader.HomeVideoItem v : allHomeVideos) {
            if (v == null) continue;
            String title = v.title != null ? v.title.toLowerCase() : "";
            if (title.contains(q)) { filtered.add(v); continue; }
            String[] words = q.split("\\s+");
            for (String w : words) {
                if (w.length() >= 2 && title.contains(w)) {
                    filtered.add(v);
                    break;
                }
            }
        }
        return filtered;
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
                fixed.add(new ServerAudioBook(ab.getId(), ab.getTitle(), ab.getParts(), fromBooks, ab.isNew(), ab.getCategory()));
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
                String url = (base != null ? base : "") + "public/audio_list.json?v=10";
                OkHttpClient client = new OkHttpClient.Builder().connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS).build();
                try (okhttp3.Response resp = client.newCall(new okhttp3.Request.Builder().url(url).build()).execute()) {
                    if (resp.isSuccessful() && resp.body() != null)
                        loaded = ServerAudioParser.parseBooks(resp.body().string());
                } catch (Exception ignored) {}
                if (loaded == null || loaded.isEmpty()) {
                    try (java.io.BufferedReader r = new java.io.BufferedReader(
                            new java.io.InputStreamReader(act.getAssets().open("audio_list_main.json"), java.nio.charset.StandardCharsets.UTF_8))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = r.readLine()) != null) sb.append(line);
                        loaded = ServerAudioParser.parseBooks(sb.toString());
                    } catch (Exception ignored) {}
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

    /** એક જ નેટવર્ક લોડથી શ્રેષ્ઠ + ભક્તિ બંને — ઓછી થ્રેડ/ઓછું UI જંક */
    private void loadBestAndBhaktiBooksCombined() {
        new Thread(() -> {
            try {
                android.app.Activity act = getActivity();
                if (act == null) return;
                List<Book> allBooks = ServerBookLoader.load(act);
                List<Book> bestBooks = new ArrayList<>(allBooks);
                Collections.sort(bestBooks, (b1, b2) -> {
                    if (b1.isNew() != b2.isNew()) return b1.isNew() ? -1 : 1;
                    return safeCompare(b1.getName(), b2.getName());
                });
                final List<Book> bestSlice = bestBooks.size() > BEST_BOOKS_COUNT
                        ? new ArrayList<>(bestBooks.subList(0, BEST_BOOKS_COUNT)) : bestBooks;
                List<Book> bhaktiBooks = new ArrayList<>();
                for (Book book : allBooks) {
                    if ("Bhakti".equals(book.getCategory())) bhaktiBooks.add(book);
                }
                Collections.sort(bhaktiBooks, (b1, b2) -> safeCompare(b1.getName(), b2.getName()));
                final List<Book> bhaktiFinal = bhaktiBooks;
                act.runOnUiThread(() -> {
                    if (!isAdded() || getContext() == null) return;
                    allBooksForSearch = new ArrayList<>(allBooks);
                    if (bestBooksRecycler != null) {
                        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
                        bestBooksRecycler.setLayoutManager(layoutManager);
                        bestBooksRecycler.setItemAnimator(new DefaultItemAnimator());
                        int spacing = (int) (10 * getResources().getDisplayMetrics().density);
                        bestBooksRecycler.addItemDecoration(new HorizontalSpacingItemDecoration(spacing));
                        bestBooksAdapter = new BookAdapter(bestSlice, this);
                        bestBooksAdapter.setUseCompactLayout(true);
                        bestBooksRecycler.setAdapter(bestBooksAdapter);
                    }
                    if (bhaktiBooksRecycler != null) {
                        LinearLayoutManager lm = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
                        bhaktiBooksRecycler.setLayoutManager(lm);
                        bhaktiBooksRecycler.setItemAnimator(new DefaultItemAnimator());
                        int spacing = (int) (10 * getResources().getDisplayMetrics().density);
                        bhaktiBooksRecycler.addItemDecoration(new HorizontalSpacingItemDecoration(spacing));
                        bhaktiBooksAdapter = new BookAdapter(bhaktiFinal, this);
                        bhaktiBooksAdapter.setUseCompactLayout(true);
                        bhaktiBooksRecycler.setAdapter(bhaktiBooksAdapter);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "loadBestAndBhaktiBooksCombined", e);
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
                allHomeVideos = new ArrayList<>(videos);
                homeVideoAdapter.setItems(videos);
            } else {
                Log.d(TAG, "New videos empty from loader – fallback: search Sachchidanand Dantali");
                YouTubeSearchLoader.search(getContext(), "Sachchidanand Dantali Swami", results -> {
                    if (isAdded() && homeVideoAdapter != null && results != null && !results.isEmpty()) {
                        java.util.List<HomeVideoLoader.HomeVideoItem> top = results.size() > 5
                            ? results.subList(0, 5) : results;
                        allHomeVideos = new ArrayList<>(top);
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

    private void setupHomePravachan(View root) {
        if (homePravachanRecycler == null || getContext() == null) return;

        homePravachanRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        homePravachanRecycler.setItemAnimator(new DefaultItemAnimator());

        homePravachanAdapter = new PravachanAdapter();
        homePravachanAdapter.setListener((item, adapterPosition) -> {
            if (item == null) return;
            MainActivity.queuePravachanStart(item);
            switchToBottomNavTab(R.id.nav_pravachan);
        });
        homePravachanRecycler.setAdapter(homePravachanAdapter);

        homePravachanViewAll = root.findViewById(R.id.home_pravachan_view_all);
        if (homePravachanViewAll != null) {
            homePravachanViewAll.setOnClickListener(v -> switchToBottomNavTab(R.id.nav_pravachan));
        }

        loadHomePravachanAsync();
    }

    private void loadHomePravachanAsync() {
        if (!isAdded() || homePravachanAdapter == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Query q = db.collection("pravachan")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
                .limit(6);

        q.get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded() || homePravachanAdapter == null) return;

                    java.util.HashSet<String> seenUrls = new java.util.HashSet<>();
                    List<PravachanItem> list = new ArrayList<>();
                    if (snap != null && snap.getDocuments() != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot d : snap.getDocuments()) {
                            try {
                                String id = d.getId();
                                String title = d.getString("title");
                                String url = d.getString("audioUrl");
                                if (url == null || url.trim().isEmpty()) {
                                    url = d.getString("audio_url");
                                }
                                String speaker = d.getString("speaker");

                                long createdAt = 0L;
                                try {
                                    com.google.firebase.Timestamp ts = d.getTimestamp("createdAt");
                                    if (ts != null) createdAt = ts.toDate().getTime();
                                    else {
                                        Long lng = d.getLong("createdAt");
                                        if (lng != null) createdAt = lng;
                                    }
                                } catch (Throwable ignore) {
                                }

                                if (title == null || url == null) continue;
                                String cleanTitle = cleanPravachanTitle(title.trim());
                                String cleanUrl = url.trim();
                                if (cleanTitle.isEmpty() || cleanUrl.isEmpty()) continue;

                                // Skip test / old entries like "aud 2026 ..."
                                String lowerTitle = cleanTitle.toLowerCase();
                                if (lowerTitle.startsWith("aud 2026") || lowerTitle.startsWith("aud_2026")) continue;

                                // Avoid duplicates within this home section.
                                if (seenUrls.contains(cleanUrl)) continue;
                                seenUrls.add(cleanUrl);

                                list.add(new PravachanItem(id, cleanTitle, cleanUrl, speaker, createdAt));
                            } catch (Throwable ignore) {
                            }
                        }
                    }

                    // Ensure: Part A before Part B, and groups ordered by latest createdAt.
                    list = reorderPravachansByBaseAndPart(list);
                    allHomePravachans.clear();
                    allHomePravachans.addAll(list);
                    homePravachanAdapter.setItems(list);
                })
                .addOnFailureListener(e -> Log.e(TAG, "loadHomePravachan error", e));
    }

    private static String cleanPravachanTitle(String title) {
        if (title == null) return "";
        String t = title.trim();
        if (t.isEmpty()) return t;

        // If it already contains "(Part A/B)", just normalize spaces.
        if (t.matches("(?i).*\\(\\s*Part\\s+[AB]\\s*\\)\\s*$")) {
            return t.replaceAll("\\s+", " ").trim();
        }

        // Normalize filename-like strings: remove extension and convert suffix A/B to "(Part A/B)".
        t = t.replaceAll("(?i)\\.(mp3|wav)\\s*$", "").trim();

        // Detect trailing part letter: supports "..._A", "... A", "...-B", "...B" (as last token).
        java.util.regex.Matcher partMatcher = java.util.regex.Pattern
                .compile("(?i)(.*?)(?:[_\\s-]?)([AB])\\s*$")
                .matcher(t);

        String partLetter = null;
        String base = t;
        if (partMatcher.find()) {
            String maybeBase = partMatcher.group(1);
            String maybeLetter = partMatcher.group(2);
            if (maybeBase != null && maybeLetter != null) {
                String baseTrim = maybeBase.trim();
                if (!baseTrim.isEmpty()) {
                    base = baseTrim;
                    partLetter = maybeLetter.toUpperCase();
                }
            }
        }

        // Replace underscores with spaces for readability.
        base = base.replace('_', ' ').replaceAll("\\s+", " ").trim();
        if (partLetter != null && ("A".equals(partLetter) || "B".equals(partLetter))) {
            return base + " (Part " + partLetter + ")";
        }
        return base;
    }

    private static class PartInfo {
        final String baseKey;
        final int partIndex; // 0 = A, 1 = B, 2 = other/unknown
        PartInfo(String baseKey, int partIndex) {
            this.baseKey = baseKey;
            this.partIndex = partIndex;
        }
    }

    private static PartInfo parsePartInfo(String title) {
        if (title == null) return new PartInfo("", 2);
        String t = title.trim();

        // Already in format: "... (Part A)" / "... (Part B)"
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\(\\s*Part\\s+([AB])\\s*\\)\\s*$", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(t);
        if (m.find()) {
            String letter = m.group(1) != null ? m.group(1).toUpperCase() : "";
            int partIndex = "A".equals(letter) ? 0 : 1;
            String base = t.substring(0, m.start()).trim();
            String baseKey = base.toLowerCase().replaceAll("\\s+", " ");
            return new PartInfo(baseKey, partIndex);
        }

        // Fallback: title ends with "A" or "B" as a word
        java.util.regex.Matcher m2 = java.util.regex.Pattern
                .compile("(^|\\s|_)([AB])\\s*$", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(t);
        if (m2.find()) {
            String letter = m2.group(2) != null ? m2.group(2).toUpperCase() : "";
            int partIndex = "A".equals(letter) ? 0 : 1;
            String base = t.substring(0, m2.start(2)).trim();
            String baseKey = base.toLowerCase().replaceAll("\\s+", " ");
            return new PartInfo(baseKey, partIndex);
        }

        String baseKey = t.toLowerCase().replaceAll("\\s+", " ");
        return new PartInfo(baseKey, 2);
    }

    private static List<PravachanItem> reorderPravachansByBaseAndPart(List<PravachanItem> input) {
        if (input == null || input.isEmpty()) return new ArrayList<>();

        // Group by base title (title without "(Part A/B)")
        class Group {
            final String baseKey;
            final List<PravachanItem> items = new ArrayList<>();
            long groupLatest = 0L;
            Group(String baseKey) { this.baseKey = baseKey; }
        }

        java.util.LinkedHashMap<String, Group> groups = new java.util.LinkedHashMap<>();
        for (PravachanItem p : input) {
            if (p == null) continue;
            PartInfo info = parsePartInfo(p.title);
            String baseKey = info.baseKey != null ? info.baseKey : "";
            Group g = groups.get(baseKey);
            if (g == null) {
                g = new Group(baseKey);
                groups.put(baseKey, g);
            }
            g.items.add(p);
            g.groupLatest = Math.max(g.groupLatest, p.createdAtMillis);
        }

        // Sort groups by latest createdAt (desc) to keep “new items first”
        List<Group> groupList = new ArrayList<>(groups.values());
        groupList.sort((g1, g2) -> Long.compare(g2.groupLatest, g1.groupLatest));

        List<PravachanItem> out = new ArrayList<>();
        for (Group g : groupList) {
            // Within group: A then B then others, fallback by createdAt desc.
            g.items.sort((a, b) -> {
                PartInfo ia = parsePartInfo(a.title);
                PartInfo ib = parsePartInfo(b.title);
                int c = Integer.compare(ia.partIndex, ib.partIndex);
                if (c != 0) return c;
                return Long.compare(b.createdAtMillis, a.createdAtMillis);
            });
            out.addAll(g.items);
        }

        return out;
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
                final String vid = video.videoId;
                android.app.Activity act = getActivity();
                AdLoadingOverlay.show(act);
                InterstitialAdHelper.showIfAllowed(act, () -> {
                    AdLoadingOverlay.dismiss(act);
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + vid)));
                    } catch (Exception e) {
                        Log.e(TAG, "open video", e);
                    }
                });
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
                String url = (base != null ? base : "") + "public/audio_list.json?v=10";
                OkHttpClient client = new OkHttpClient.Builder().connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS).build();
                okhttp3.Request req = new okhttp3.Request.Builder().url(url).build();
                try (okhttp3.Response resp = client.newCall(req).execute()) {
                    if (resp.isSuccessful() && resp.body() != null) {
                        String body = resp.body().string();
                        loaded = ServerAudioParser.parseBooks(body);
                    }
                }
                if (loaded == null || loaded.isEmpty()) {
                    try (java.io.BufferedReader r = new java.io.BufferedReader(
                            new java.io.InputStreamReader(act.getAssets().open("audio_list_main.json"), java.nio.charset.StandardCharsets.UTF_8))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = r.readLine()) != null) sb.append(line);
                        loaded = ServerAudioParser.parseBooks(sb.toString());
                    }
                }
                if (loaded == null) loaded = ServerAudioParser.demoBooks();
                loaded = mapAudioThumbnails(act, loaded, base);
                // સર્વર "new": true વાળાં ઓડિયો પહેલાં, પછી ટાઇટલ – નવા હોમ પર પહેલાં દેખાશે
                java.util.Collections.sort(loaded, (a, b) -> {
                    if (a.isNew() != b.isNew()) return a.isNew() ? -1 : 1;
                    return (b.getTitle() != null ? b.getTitle() : "").compareTo(a.getTitle() != null ? a.getTitle() : "");
                });
                allHomeAudio = new ArrayList<>(loaded);
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

        TextView pravachanViewAll = root.findViewById(R.id.home_pravachan_view_all);
        if (pravachanViewAll != null) {
            pravachanViewAll.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).switchToTab(R.id.nav_pravachan);
                }
            });
        }
        TextView bookStoreViewAll = root.findViewById(R.id.home_book_store_view_all);
        if (bookStoreViewAll != null) {
            bookStoreViewAll.setOnClickListener(v -> {
                if (getActivity() != null) {
                    startActivity(new android.content.Intent(getActivity(), BookStoreActivity.class));
                }
            });
        }
    }

    private static final String BOOKS_STORE_URL = "https://daveashish12356-dotcom.github.io/swamisachidanand-audio/public/books_store.json?v=2";

    private void setupBookStoreSection(View root) {
        RecyclerView recycler = root.findViewById(R.id.home_book_store_recycler);
        if (recycler == null || getContext() == null) return;
        List<BookStoreItem> items = new ArrayList<>();
        try {
            java.io.InputStream is = getContext().getAssets().open("books_store_list.json");
            byte[] buf = new byte[is.available()];
            is.read(buf);
            is.close();
            String json = new String(buf, StandardCharsets.UTF_8);
            JSONArray arr = new JSONArray(json);
            int max = Math.min(arr.length(), 10);
            for (int i = 0; i < max; i++) {
                org.json.JSONObject o = arr.getJSONObject(i);
                BookStoreItem item = new BookStoreItem();
                item.id = o.optString("id", "");
                item.name = o.optString("name", "");
                item.price = o.optInt("price", 0);
                item.img = o.optString("img", "");
                items.add(item);
            }
        } catch (Exception e) {
            Log.e(TAG, "load books_store_list", e);
        }
        recycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recycler.setItemAnimator(new DefaultItemAnimator());
        int spacing = (int) (10 * getResources().getDisplayMetrics().density);
        recycler.addItemDecoration(new HorizontalSpacingItemDecoration(spacing));
        BookStoreAdapter adapter = new BookStoreAdapter();
        adapter.setItems(items);
        adapter.setOnBookStoreClickListener(item -> {
            if (getActivity() != null) {
                startActivity(new android.content.Intent(getActivity(), BookStoreActivity.class));
            }
        });
        recycler.setAdapter(adapter);

        // Load from server: "new": true વાળાં પુસ્તકો પહેલાં, પછી બાકી – સર્વરમાં new ઉમેરો તો હોમ પર આપમેળે દેખાશે
        new Thread(() -> {
            String baseUrl = "https://daveashish12356-dotcom.github.io/swamisachidanand-audio/public/";
            String coversBase = baseUrl + "book_covers/";
            List<BookStoreItem> serverItems = new ArrayList<>();
            List<BookStoreItem> newOnes = new ArrayList<>();
            List<BookStoreItem> others = new ArrayList<>();
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();
                Request req = new Request.Builder().url(BOOKS_STORE_URL).build();
                try (Response resp = client.newCall(req).execute()) {
                    if (resp.isSuccessful() && resp.body() != null) {
                        String js = resp.body().string();
                        JSONObject jsonRoot = new JSONObject(js);
                        JSONArray arr = jsonRoot.optJSONArray("books");
                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject o = arr.getJSONObject(i);
                                BookStoreItem item = new BookStoreItem();
                                item.id = o.optString("id", "");
                                item.name = o.optString("name", "");
                                item.price = o.optInt("price", 0);
                                item.img = o.optString("img", "");
                                item.imageUrl = (item.img != null && !item.img.isEmpty()) ? (coversBase + item.img) : null;
                                item.isNew = o.optBoolean("new", false);
                                // નવાં = સર્વર "new" અથવા અગાઉની નામવાળી (મહાન રામાનુજાચાર્ય, દેવાલય થી દેહાલય) – હોમ પર પહેલાં દેખાડો
                                boolean showFirst = item.isNew || isNewBookByName(item.name);
                                if (showFirst) newOnes.add(item); else others.add(item);
                            }
                            for (BookStoreItem b : newOnes) {
                                if (serverItems.size() >= 10) break;
                                serverItems.add(b);
                            }
                            for (BookStoreItem b : others) {
                                if (serverItems.size() >= 10) break;
                                serverItems.add(b);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "book store from server", e);
            }
            if (!serverItems.isEmpty() && getActivity() != null) {
                getActivity().runOnUiThread(() -> adapter.setItems(serverItems));
            }
        }).start();
    }

    /** અગાઉ book price પર નવાં પુસ્તકો માં હતાં એ નામ – હોમ પર પહેલાં દેખાડવા. */
    private static boolean isNewBookByName(String name) {
        if (name == null) return false;
        String n = name.trim();
        return n.contains("મહાન રામાનુજાચાર્ય") || n.contains("દેવાલય થી દેહાલય");
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
        stopHeroSlideshowAutoAdvance();
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
        heroPhotoSlideshow = null;
        heroSlideshowAdapter = null;
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

