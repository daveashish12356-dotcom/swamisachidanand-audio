package com.swamisachidanand;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Videos page – merges 4 YouTube channels into one list using playlistItems API.
 * Fetches 10 videos per channel, merges, sorts by date, dedupes.
 */
public class VideosFragment extends Fragment {

    private static final String TAG = "VideosFragment";
    private static final int VIDEOS_PER_PLAYLIST = 50;  // YouTube API max per request

    /** 3 channels – merged in playlist. Handles resolved to IDs at runtime. */
    private static final String[] CHANNEL_HANDLES = {
        "Sachchidanand-Dantali",   // SWAMI SACHCHIDANANDJI_ OFFICIAL
        "SwamiSachidanand",        // Swami Sachidanand
        "swamisachchidanandji"     // @swamisachchidanandji
    };

    /** Placeholder IDs – overwritten by resolveChannelIdFromHandle at runtime. */
    private static final String[] CHANNEL_IDS = {
        "UCk1V0R5Vn8X6HPy5Y8C7H9A",
        "UCba78apJ7Rw8crHxVPq9dow",
        "UCba78apJ7Rw8crHxVPq9dow"  // fallback if resolve fails
    };

    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile("\"videoId\"\\s*:\\s*\"([a-zA-Z0-9_-]{11})\"");
    private static final Pattern WATCH_VIDEO_PATTERN = Pattern.compile("/watch\\?v=([a-zA-Z0-9_-]{11})");
    private static final int REQUEST_CODE_VOICE_SEARCH = 9001;

    private ProgressBar loadingView;
    private View errorLayout;
    private TextView messageView;
    private RecyclerView recyclerView;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private VideosAdapter adapter;
    private final List<YouTubeVideo> allVideos = new ArrayList<>();
    private volatile boolean loadingMore = false;
    private String nextPageToken = null;
    private long lastLoadMoreTime = 0;
    private static final long LOAD_MORE_THROTTLE_MS = 3000;
    /** For load-more: which source succeeded. "piped", "invidious", "playlist", "search" */
    private String lastFetchSource = null;
    private String lastPipedBase = null;
    private String lastPipedNextpage = null;
    private String lastChannelId = null;
    private String lastPlaylistId = null;
    private long lastLoadVideosTime = 0;
    private static final long REFRESH_INTERVAL_MS = 2 * 60 * 1000;  // 2 min – refresh when returning to tab
    /** For playlist merge: multiple channels – (playlistId, nextToken) for load more */
    private final List<String> loadMorePlaylistIds = new ArrayList<>();
    private final List<String> loadMorePageTokens = new ArrayList<>();
    private TextInputEditText searchInput;
    private ImageView clearSearch;
    private ImageView micButton;
    private String currentSearchQuery = "";
    private boolean isSearchMode = false;
    private static final long SEARCH_DEBOUNCE_MS = 500;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    public @Nullable View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                                       @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_videos, container, false);
        loadingView = view.findViewById(R.id.videos_loading);
        errorLayout = view.findViewById(R.id.videos_error_layout);
        messageView = view.findViewById(R.id.videos_message);
        recyclerView = view.findViewById(R.id.videos_recycler);
        swipeRefresh = view.findViewById(R.id.videos_swipe_refresh);

        if (swipeRefresh != null) swipeRefresh.setOnRefreshListener(() -> {
            if (isSearchMode && currentSearchQuery != null && !currentSearchQuery.isEmpty()) {
                searchFromYouTube(currentSearchQuery);
            } else {
                loadVideos();
            }
        });
        View retryBtn = view.findViewById(R.id.videos_retry_btn);
        if (retryBtn != null) retryBtn.setOnClickListener(v -> loadVideos());

        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        // setHasFixedSize omitted for layout compatibility
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setItemViewCacheSize(20);
        adapter = new VideosAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                android.app.Activity a = getActivity();
                if (a instanceof MainActivity) ((MainActivity) a).onScrolled(dy);
                if (dy <= 0 || loadingMore) return;
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm == null) return;
                int last = lm.findLastVisibleItemPosition();
                int total = adapter != null ? adapter.getItemCount() : 0;
                if (total > 15 && last >= total - 8) loadMoreVideos();
            }
        });

        searchInput = view.findViewById(R.id.global_search_input);
        if (searchInput != null) searchInput.setHint(R.string.search_videos_hint);
        clearSearch = view.findViewById(R.id.global_clear_search);
        micButton = view.findViewById(R.id.global_mic_button);
        setupSearchView();

        View avatar = view.findViewById(R.id.global_profile_avatar);
        if (avatar != null) {
            avatar.setOnClickListener(v -> {
                android.app.Activity act = getActivity();
                if (act instanceof MainActivity) ((MainActivity) act).openSwamiInfoPage();
            });
        }

        setLoading(true);
        loadVideos();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Jab user Videos tab par wapas aaye (2 min ke baad) – naye videos ke liye refresh
        if (System.currentTimeMillis() - lastLoadVideosTime > REFRESH_INTERVAL_MS && !allVideos.isEmpty()) {
            loadVideos();
        }
    }

    private void setLoading(boolean loading) {
        if (loadingView != null) loadingView.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(loading ? View.GONE : View.VISIBLE);
        if (swipeRefresh != null) swipeRefresh.setRefreshing(loading);
    }

    private void showMessage(String msg) {
        if (messageView != null) messageView.setText(msg != null ? msg : "");
        if (errorLayout != null) {
            errorLayout.setVisibility(msg != null && !msg.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void setErrorState(String msg) {
        setLoading(false);
        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        showMessage(msg);
    }

    private void setupSearchView() {
        if (searchInput == null || clearSearch == null || micButton == null) return;
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                currentSearchQuery = query;
                if (query.isEmpty()) {
                    clearSearch.setVisibility(View.GONE);
                    if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                    isSearchMode = false;
                    applyDisplayVideos(new ArrayList<>(allVideos));
                } else {
                    clearSearch.setVisibility(View.VISIBLE);
                    if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                    searchRunnable = () -> searchFromYouTube(query);
                    searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_MS);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String q = searchInput != null ? searchInput.getText().toString().trim() : "";
                if (!q.isEmpty()) openGlobalSearch();
                return true;
            }
            return false;
        });
        clearSearch.setOnClickListener(v -> {
            if (searchInput != null) {
                searchInput.setText("");
                clearSearch.setVisibility(View.GONE);
                currentSearchQuery = "";
                isSearchMode = false;
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                applyDisplayVideos(new ArrayList<>(allVideos));
            }
        });
        micButton.setOnClickListener(v -> startVoiceSearch());
    }

    private void openGlobalSearch() {
        String q = searchInput != null && searchInput.getText() != null ? searchInput.getText().toString().trim() : "";
        if (q.isEmpty()) return;
        Intent i = new Intent(requireContext(), SearchResultActivity.class);
        i.putExtra(SearchResultActivity.EXTRA_QUERY, q);
        startActivity(i);
    }

    /** YouTube API se query ke hisaab se search – bilkul YouTube jaisa. */
    private void searchFromYouTube(String query) {
        if (query == null || query.trim().isEmpty()) return;
        final android.app.Activity activity = getActivity();
        if (activity == null || activity.isFinishing()) return;

        isSearchMode = true;
        setLoading(true);
        showMessage("");
        if (errorLayout != null) errorLayout.setVisibility(View.GONE);

        final String q = query.trim();
        new Thread(() -> {
            try {
                String apiKey = BuildConfig.YOUTUBE_API_KEY;
                if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("YOUR_")) {
                    activity.runOnUiThread(() -> {
                        setLoading(false);
                        showMessage("YouTube API key સેટ નથી.");
                    });
                    return;
                }
                OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(8, TimeUnit.SECONDS)
                    .readTimeout(12, TimeUnit.SECONDS)
                    .build();

                String[] resolvedIds = new String[CHANNEL_HANDLES.length];
                for (int i = 0; i < CHANNEL_HANDLES.length; i++) {
                    resolvedIds[i] = CHANNEL_IDS[i];
                    if (apiKey != null && !apiKey.isEmpty() && CHANNEL_HANDLES[i] != null) {
                        try {
                            String r = resolveChannelIdFromHandle(client, apiKey, CHANNEL_HANDLES[i]);
                            if (r != null && !r.isEmpty()) resolvedIds[i] = r;
                        } catch (Exception e) { Log.w(TAG, "Resolve " + CHANNEL_HANDLES[i] + " failed", e); }
                    }
                }

                List<YouTubeVideo> searchResults = new ArrayList<>();
                Set<String> seen = new HashSet<>();
                for (int i = 0; i < resolvedIds.length; i++) {
                    try {
                        fetchFromSearchByQuery(client, apiKey, q, resolvedIds[i], null, searchResults);
                    } catch (Exception e) {
                        Log.e(TAG, "Search failed for " + resolvedIds[i], e);
                    }
                }
                List<YouTubeVideo> deduped = new ArrayList<>();
                for (YouTubeVideo v : searchResults) {
                    if (v.videoId != null && !seen.contains(v.videoId)) {
                        seen.add(v.videoId);
                        deduped.add(v);
                    }
                }
                Collections.sort(deduped, (a, b) -> {
                    String pa = a.publishedAt != null ? a.publishedAt : "";
                    String pb = b.publishedAt != null ? b.publishedAt : "";
                    return pb.compareTo(pa);
                });

                final List<YouTubeVideo> result = deduped;
                Log.d(TAG, "YouTube search '" + q + "' found " + result.size() + " videos");
                activity.runOnUiThread(() -> {
                    if (recyclerView == null || adapter == null) return;
                    setLoading(false);
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    if (result.isEmpty()) {
                        showMessage("કોઈ વિડિઓ મળી નહીં. અલગ શબ્દો લખો.");
                        if (errorLayout != null) errorLayout.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        showMessage("");
                        if (errorLayout != null) errorLayout.setVisibility(View.GONE);
                        if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
                        adapter.setItems(result);
                    }
                });
            } catch (Throwable t) {
                Log.e(TAG, "searchFromYouTube error", t);
                activity.runOnUiThread(() -> {
                    setLoading(false);
                    showMessage("શોધ ભૂલ: " + (t.getMessage() != null ? t.getMessage() : "ફરી પ્રયાસ કરો."));
                    if (errorLayout != null) errorLayout.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    private void applyDisplayVideos(List<YouTubeVideo> list) {
        if (adapter == null) return;
        adapter.setItems(list);
        if (errorLayout != null) errorLayout.setVisibility(View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
        showMessage("");
    }

    private void startVoiceSearch() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "gu-IN");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "gu-IN");
            intent.putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, new String[]{"gu-IN", "hi-IN", "en-IN"});
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "વિડિઓ શોધવા માટે બોલો...");
            startActivityForResult(intent, REQUEST_CODE_VOICE_SEARCH);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Voice search not available", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Voice search error", e);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_VOICE_SEARCH && resultCode == android.app.Activity.RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty() && searchInput != null) {
                String spoken = results.get(0);
                searchInput.setText(spoken);
                if (!spoken.trim().isEmpty()) openGlobalSearch();
            }
        }
    }

    private void loadVideos() {
        final android.app.Activity activity = getActivity();
        if (activity == null || activity.isFinishing()) return;

        setLoading(true);
        showMessage("");

        new Thread(() -> {
            try {
                String apiKey = BuildConfig.YOUTUBE_API_KEY;
                if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("YOUR_")) {
                    activity.runOnUiThread(() -> setErrorState("YouTube API key સેટ નથી."));
                    return;
                }

                OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(8, TimeUnit.SECONDS)
                    .readTimeout(12, TimeUnit.SECONDS)
                    .build();

                List<YouTubeVideo> videos = new ArrayList<>();
                lastFetchSource = null;
                lastPipedBase = null;
                lastPipedNextpage = null;
                lastChannelId = null;
                nextPageToken = null;
                lastLoadMoreTime = 0;
                lastPlaylistId = null;
                loadMorePlaylistIds.clear();
                loadMorePageTokens.clear();

                // Resolve handles to channel IDs
                String[] resolvedIds = new String[CHANNEL_HANDLES.length];
                for (int i = 0; i < CHANNEL_HANDLES.length; i++) {
                    resolvedIds[i] = CHANNEL_IDS[i];
                    if (apiKey != null && !apiKey.isEmpty() && CHANNEL_HANDLES[i] != null) {
                        try {
                            String r = resolveChannelIdFromHandle(client, apiKey, CHANNEL_HANDLES[i]);
                            if (r != null && !r.isEmpty()) resolvedIds[i] = r;
                        } catch (Exception e) {
                            Log.w(TAG, "Resolve " + CHANNEL_HANDLES[i] + " failed", e);
                        }
                    }
                }

                // 1) Proxy (if set) – fast when working
                if (videos.isEmpty()) {
                    String proxyUrl = BuildConfig.YOUTUBE_PROXY_URL;
                    if (proxyUrl != null && !proxyUrl.isEmpty()) {
                        Log.d(TAG, "Trying proxy " + proxyUrl);
                        try {
                            fetchFromProxy(client, proxyUrl, resolvedIds, videos);
                            if (!videos.isEmpty()) lastFetchSource = "proxy";
                        } catch (Exception e) {
                            Log.e(TAG, "Proxy failed", e);
                        }
                    }
                }

                // 2) playlistItems API – fetch from BOTH channels and merge
                if (videos.isEmpty()) {
                    Log.d(TAG, "Trying playlistItems API (merged channels)");
                    for (int i = 0; i < CHANNEL_HANDLES.length; i++) {
                        String channelId = resolvedIds[i];
                        String handle = CHANNEL_HANDLES[i];
                        try {
                            String uploadsId = getUploadsPlaylistId(client, apiKey, channelId);
                            if ((uploadsId == null || uploadsId.isEmpty()) && handle != null) {
                                uploadsId = getUploadsPlaylistId(client, apiKey, handle);
                            }
                            String playlistId = uploadsId != null && !uploadsId.isEmpty()
                                ? uploadsId : "UU" + (channelId.length() > 2 ? channelId.substring(2) : "");
                            String np = fetchPlaylistVideos(client, apiKey, playlistId, null, videos, i);
                            for (int p = 0; p < 2 && np != null && !np.isEmpty(); p++) {
                                np = fetchPlaylistVideos(client, apiKey, playlistId, np, videos, i);
                            }
                            if (!videos.isEmpty()) lastFetchSource = "playlist";
                            lastChannelId = channelId;
                            lastPlaylistId = playlistId;
                            nextPageToken = (np != null && !np.isEmpty()) ? np : null;
                            if (np != null && !np.isEmpty()) {
                                loadMorePlaylistIds.add(playlistId);
                                loadMorePageTokens.add(np);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "playlist failed " + channelId, e);
                        }
                    }
                }

                // 3) Search API
                if (videos.isEmpty()) {
                    Log.d(TAG, "Trying search API");
                    for (int i = 0; i < CHANNEL_HANDLES.length; i++) {
                        String channelId = resolvedIds[i];
                        try {
                            String np = fetchFromSearchApi(client, apiKey, channelId, null, videos, i);
                            if (!videos.isEmpty()) {
                                lastFetchSource = "search";
                                lastChannelId = channelId;
                                nextPageToken = (np != null && !np.isEmpty()) ? np : null;
                                break;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "search API failed " + channelId, e);
                        }
                    }
                }

                // 4) RSS feed
                if (videos.isEmpty()) {
                    Log.d(TAG, "Trying RSS feed");
                    for (int i = 0; i < CHANNEL_HANDLES.length; i++) {
                        String channelId = resolvedIds[i];
                        try {
                            fetchFromRssFeed(client, channelId, videos, i);
                            if (!videos.isEmpty()) {
                                lastFetchSource = "rss";
                                lastChannelId = channelId;
                                break;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "RSS failed " + channelId, e);
                        }
                    }
                }

                // 5) HTML scrape
                if (videos.isEmpty()) {
                    Log.d(TAG, "Trying HTML scrape");
                    for (int i = 0; i < CHANNEL_HANDLES.length; i++) {
                        String channelId = resolvedIds[i];
                        try {
                            fetchFromChannelHtml(client, channelId, videos, i);
                            if (!videos.isEmpty()) {
                                lastFetchSource = "html";
                                lastChannelId = channelId;
                                break;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "HTML failed " + channelId, e);
                        }
                    }
                }

                // 6) Invidious API – fetches BOTH /videos and /shorts (all content)
                if (videos.isEmpty()) {
                    Log.d(TAG, "Trying Invidious API (videos + shorts)");
                    for (int i = 0; i < CHANNEL_HANDLES.length; i++) {
                        String channelId = resolvedIds[i];
                        String handle = CHANNEL_HANDLES[i];
                        try {
                            if (fetchFromInvidious(client, channelId, videos, i)) {
                                lastFetchSource = "invidious";
                                lastChannelId = channelId;
                                break;
                            }
                            if (handle != null && fetchFromInvidious(client, handle, videos, i)) {
                                lastFetchSource = "invidious";
                                lastChannelId = channelId;
                                break;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Invidious failed " + channelId, e);
                        }
                    }
                }

                // 7) Piped API – load-more (often blocked in India; returns Shorts tab by default)
                if (videos.isEmpty()) {
                    Log.d(TAG, "Trying Piped API");
                    try {
                        boolean pipedOk = false;
                        for (int i = 0; i < CHANNEL_HANDLES.length; i++) {
                            pipedOk = fetchFromPiped(client, CHANNEL_HANDLES[i], null, videos, true, i)
                                || fetchFromPiped(client, resolvedIds[i], null, videos, false, i);
                            if (pipedOk) break;
                        }
                        if (pipedOk) {
                            lastFetchSource = "piped";
                            lastChannelId = resolvedIds[0];
                            // Piped returns Shorts only – augment with long-form from multiple sources
                            Log.d(TAG, "Augmenting Piped with long-form videos (Invidious, RSS, HTML)");
                            for (int i = 0; i < CHANNEL_HANDLES.length; i++) {
                                fetchFromInvidiousVideosOnly(client, resolvedIds[i], videos, i);
                                try { fetchFromRssFeed(client, resolvedIds[i], videos, i); } catch (Exception e) { }
                                try { fetchFromChannelHtml(client, resolvedIds[i], videos, i); } catch (Exception e) { }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Piped failed", e);
                    }
                }

                // 8) Sample videos when all fail
                if (videos.isEmpty()) {
                    addSampleVideos(videos);
                }

                // Dedupe by videoId
                List<YouTubeVideo> deduped = new ArrayList<>();
                Set<String> seen = new HashSet<>();
                for (YouTubeVideo v : videos) {
                    if (v.videoId != null && !seen.contains(v.videoId)) {
                        seen.add(v.videoId);
                        deduped.add(v);
                    }
                }

                // Sort by publishedAt (latest first)
                Collections.sort(deduped, (a, b) -> {
                    String pa = a.publishedAt != null ? a.publishedAt : "";
                    String pb = b.publishedAt != null ? b.publishedAt : "";
                    return pb.compareTo(pa);
                });

                final List<YouTubeVideo> result = deduped;
                Log.d(TAG, "Videos loaded: source=" + lastFetchSource + " count=" + result.size());
                activity.runOnUiThread(() -> {
                    if (recyclerView == null || adapter == null) return;
                    setLoading(false);
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    showMessage("");
                    if (errorLayout != null) errorLayout.setVisibility(View.GONE);
                    allVideos.clear();
                    allVideos.addAll(result);
                    if (!isSearchMode) applyDisplayVideos(new ArrayList<>(allVideos));
                    lastLoadVideosTime = System.currentTimeMillis();
                });
            } catch (Throwable t) {
                Log.e(TAG, "loadVideos error", t);
                final android.app.Activity act = getActivity();
                if (act != null && !act.isFinishing()) {
                    act.runOnUiThread(() -> {
                        setErrorState("વિડિઓ લોડ થતાં ભૂલ આવી. નીચે ખેંચીને ફરી પ્રયાસ કરો.");
                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    });
                }
            }
        }).start();
    }

    /** Sample videos when all fetch methods fail – user can tap to open YouTube. */
    private void addSampleVideos(List<YouTubeVideo> out) {
        String[][] samples = {
            {"dQw4w9WgXcQ", "સેમ્પલ ૧ - ટેપ કરી યુટ્યુબ ખોલો"},
            {"9bZkp7q19f0", "સેમ્પલ ૨ - ટેપ કરી યુટ્યુબ ખોલો"},
            {"jNQXAC9IVRw", "સેમ્પલ ૩ - ટેપ કરી યુટ્યુબ ખોલો"},
        };
        for (String[] s : samples) {
            YouTubeVideo v = new YouTubeVideo();
            v.videoId = s[0];
            v.title = s[1];
            v.thumbnailUrl = "https://img.youtube.com/vi/" + s[0] + "/hqdefault.jpg";
            v.publishedAt = "";
            out.add(v);
        }
        Log.d(TAG, "Using " + samples.length + " sample videos (fetch failed)");
    }

    /** SHA-1 of app signing cert – required when API key is restricted to Android apps. */
    private static String getSignatureSha1(Context ctx) {
        if (ctx == null) return null;
        try {
            PackageInfo info = ctx.getPackageManager()
                .getPackageInfo(ctx.getPackageName(), PackageManager.GET_SIGNATURES);
            if (info == null || info.signatures == null || info.signatures.length == 0) return null;
            Signature sig = info.signatures[0];
            if (sig == null) return null;
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(sig.toByteArray());
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {
            return null;
        }
    }

    /** Resolve handle to channel ID via YouTube API. Returns UC id or null. */
    private String resolveChannelIdFromHandle(OkHttpClient client, String apiKey, String handle) throws Exception {
        if (handle == null || handle.isEmpty() || apiKey == null || apiKey.isEmpty()) return null;
        String h = handle.startsWith("@") ? handle : "@" + handle;
        String url = "https://www.googleapis.com/youtube/v3/channels"
            + "?part=id&forHandle=" + Uri.encode(h) + "&key=" + apiKey;
        Request.Builder rb = new Request.Builder().url(url);
        Context ctx = getContext();
        if (ctx != null) {
            rb.addHeader("X-Android-Package", ctx.getPackageName());
            String cert = getSignatureSha1(ctx);
            if (cert != null) rb.addHeader("X-Android-Cert", cert);
        }
        Response response = client.newCall(rb.build()).execute();
        if (response.code() == 403) {
            response.close();
            response = client.newCall(new Request.Builder().url(url)
                .addHeader("User-Agent", "SwamiSachidanand/1.0").build()).execute();
        }
        try (Response r = response) {
            if (!r.isSuccessful() || r.body() == null) return null;
            JSONObject root = new JSONObject(r.body().string());
            JSONArray items = root.optJSONArray("items");
            if (items == null || items.length() == 0) return null;
            String id = items.optJSONObject(0).optString("id", null);
            if (id != null && !id.isEmpty()) Log.d(TAG, "Resolved handle " + h + " -> " + id);
            return id;
        }
    }

    /** Get uploads playlist ID from channel via channels.list API. Supports UC id or @handle. */
    private String getUploadsPlaylistId(OkHttpClient client, String apiKey, String channelIdOrHandle) throws Exception {
        if (channelIdOrHandle == null || channelIdOrHandle.isEmpty()) return null;
        String param = (channelIdOrHandle.startsWith("UC") && channelIdOrHandle.length() == 24)
            ? "id=" + Uri.encode(channelIdOrHandle)
            : "forHandle=" + Uri.encode(channelIdOrHandle.startsWith("@") ? channelIdOrHandle : "@" + channelIdOrHandle);
        String url = "https://www.googleapis.com/youtube/v3/channels"
            + "?part=contentDetails&" + param + "&key=" + apiKey;
        Request.Builder rb = new Request.Builder().url(url);
        Context ctx = getContext();
        if (ctx != null) {
            rb.addHeader("X-Android-Package", ctx.getPackageName());
            String cert = getSignatureSha1(ctx);
            if (cert != null) rb.addHeader("X-Android-Cert", cert);
        }
        Response response = client.newCall(rb.build()).execute();
        if (response.code() == 403) {
            response.close();
            response = client.newCall(new Request.Builder().url(url)
                .addHeader("User-Agent", "SwamiSachidanand/1.0").build()).execute();
        }
        try (Response r = response) {
            if (!r.isSuccessful() || r.body() == null) return null;
            JSONObject root = new JSONObject(r.body().string());
            JSONArray items = root.optJSONArray("items");
            if (items == null || items.length() == 0) return null;
            JSONObject contentDetails = items.optJSONObject(0).optJSONObject("contentDetails");
            if (contentDetails == null) return null;
            JSONObject related = contentDetails.optJSONObject("relatedPlaylists");
            return related != null ? related.optString("uploads", null) : null;
        }
    }

    private void fetchFromProxy(OkHttpClient client, String baseUrl, String[] channelIds, List<YouTubeVideo> out) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < channelIds.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(channelIds[i]);
        }
        String url = baseUrl + (baseUrl.contains("?") ? "&" : "?") + "channel_ids=" + Uri.encode(sb.toString());
        Request request = new Request.Builder()
            .url(url)
            .addHeader("User-Agent", "SwamiSachidanand/1.0")
            .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return;
            String body = response.body().string();
            if (body == null || body.isEmpty()) return;
            JSONObject root = new JSONObject(body);
            JSONArray items = root.optJSONArray("videos");
            if (items == null) return;
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) continue;
                String videoId = item.optString("videoId", null);
                if (videoId == null || videoId.isEmpty()) continue;
                YouTubeVideo v = new YouTubeVideo();
                v.videoId = videoId;
                v.title = item.optString("title", "");
                v.thumbnailUrl = item.optString("thumbnailUrl", "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg");
                v.publishedAt = item.optString("publishedAt", "");
                out.add(v);
            }
        }
    }

    /** Fetches playlist videos. Returns nextPageToken from response, or null if no more. */
    private String fetchPlaylistVideos(OkHttpClient client, String apiKey, String playlistId,
                                       String pageToken, List<YouTubeVideo> out, int channelIndex) throws Exception {
        String url = "https://www.googleapis.com/youtube/v3/playlistItems"
            + "?part=snippet"
            + "&playlistId=" + Uri.encode(playlistId)
            + "&maxResults=" + VIDEOS_PER_PLAYLIST
            + "&key=" + apiKey;
        if (pageToken != null && !pageToken.isEmpty()) {
            url += "&pageToken=" + Uri.encode(pageToken);
        }

        // Try with Android headers first (for Android-restricted key)
        Request.Builder rb = new Request.Builder().url(url);
        Context ctx = getContext();
        if (ctx != null) {
            rb.addHeader("X-Android-Package", ctx.getPackageName());
            String cert = getSignatureSha1(ctx);
            if (cert != null) rb.addHeader("X-Android-Cert", cert);
        }
        Response response = client.newCall(rb.build()).execute();

        // On 403, retry without headers (for key with "None" restriction)
        if (response.code() == 403) {
            response.close();
            Log.d(TAG, "playlistItems 403, retrying without Android headers");
            response = client.newCall(new Request.Builder().url(url)
                .addHeader("User-Agent", "SwamiSachidanand/1.0").build()).execute();
        }

        try (Response r = response) {
            if (!r.isSuccessful() || r.body() == null) {
                String errBody = r.body() != null ? r.body().string() : "";
                Log.w(TAG, "playlistItems HTTP " + r.code() + " body=" + errBody);
                return null;
            }
            String body = r.body().string();
            JSONObject root = new JSONObject(body);
            JSONArray items = root.optJSONArray("items");
            if (items == null) return null;

            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) continue;
                JSONObject snippet = item.optJSONObject("snippet");
                if (snippet == null) continue;
                JSONObject resourceId = snippet.optJSONObject("resourceId");
                if (resourceId == null) continue;

                String videoId = resourceId.optString("videoId", null);
                if (videoId == null || videoId.isEmpty()) continue;

                String title = snippet.optString("title", "");
                String publishedAt = snippet.optString("publishedAt", "");

                String thumbUrl = null;
                JSONObject thumbs = snippet.optJSONObject("thumbnails");
                if (thumbs != null) {
                    JSONObject medium = thumbs.optJSONObject("medium");
                    if (medium != null) thumbUrl = medium.optString("url", null);
                    if ((thumbUrl == null || thumbUrl.isEmpty()) && thumbs.optJSONObject("high") != null) {
                        thumbUrl = thumbs.optJSONObject("high").optString("url", null);
                    }
                }
                if (thumbUrl == null || thumbUrl.isEmpty()) {
                    thumbUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
                }

                YouTubeVideo v = new YouTubeVideo();
                v.videoId = videoId;
                v.title = title;
                v.thumbnailUrl = thumbUrl;
                v.publishedAt = publishedAt;
                v.channelIndex = channelIndex;
                out.add(v);
            }
            return root.optString("nextPageToken", null);
        }
    }

    /** Fetches search results. Returns nextPageToken from response, or null if no more. */
    private String fetchFromSearchApi(OkHttpClient client, String apiKey, String channelId,
                                      String pageToken, List<YouTubeVideo> out, int channelIndex) throws Exception {
        String url = "https://www.googleapis.com/youtube/v3/search"
            + "?part=snippet&channelId=" + Uri.encode(channelId)
            + "&order=date&type=video&maxResults=" + VIDEOS_PER_PLAYLIST
            + "&key=" + apiKey;
        if (pageToken != null && !pageToken.isEmpty()) {
            url += "&pageToken=" + Uri.encode(pageToken);
        }

        Request.Builder rb = new Request.Builder().url(url);
        Context ctx = getContext();
        if (ctx != null) {
            rb.addHeader("X-Android-Package", ctx.getPackageName());
            String cert = getSignatureSha1(ctx);
            if (cert != null) rb.addHeader("X-Android-Cert", cert);
        }
        Response response = client.newCall(rb.build()).execute();
        if (response.code() == 403) {
            response.close();
            Log.d(TAG, "search API 403, retrying without Android headers");
            response = client.newCall(new Request.Builder().url(url)
                .addHeader("User-Agent", "SwamiSachidanand/1.0").build()).execute();
        }
        try (Response r = response) {
            if (!r.isSuccessful() || r.body() == null) {
                Log.w(TAG, "search API HTTP " + r.code());
                return null;
            }
            String body = r.body().string();
            JSONObject root = new JSONObject(body);
            JSONArray items = root.optJSONArray("items");
            if (items == null) return null;

            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) continue;
                JSONObject idObj = item.optJSONObject("id");
                if (idObj == null) continue;
                String videoId = idObj.optString("videoId", null);
                if (videoId == null || videoId.isEmpty()) continue;

                JSONObject snippet = item.optJSONObject("snippet");
                if (snippet == null) continue;
                String title = snippet.optString("title", "");
                String publishedAt = snippet.optString("publishedAt", "");

                String thumbUrl = null;
                JSONObject thumbs = snippet.optJSONObject("thumbnails");
                if (thumbs != null) {
                    JSONObject medium = thumbs.optJSONObject("medium");
                    if (medium != null) thumbUrl = medium.optString("url", null);
                    if ((thumbUrl == null || thumbUrl.isEmpty()) && thumbs.optJSONObject("high") != null) {
                        thumbUrl = thumbs.optJSONObject("high").optString("url", null);
                    }
                }
                if (thumbUrl == null || thumbUrl.isEmpty()) {
                    thumbUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
                }

                YouTubeVideo v = new YouTubeVideo();
                v.videoId = videoId;
                v.title = title;
                v.thumbnailUrl = thumbUrl;
                v.publishedAt = publishedAt;
                v.channelIndex = channelIndex;
                out.add(v);
            }
            return root.optString("nextPageToken", null);
        }
    }

    /** YouTube jaisa search – query se YouTube API se results fetch. */
    private String fetchFromSearchByQuery(OkHttpClient client, String apiKey, String query,
                                          String channelId, String pageToken, List<YouTubeVideo> out) throws Exception {
        if (query == null || query.trim().isEmpty()) return null;
        String url = "https://www.googleapis.com/youtube/v3/search"
            + "?part=snippet&q=" + Uri.encode(query.trim())
            + "&order=relevance&type=video&maxResults=" + Math.min(VIDEOS_PER_PLAYLIST, 25)
            + "&key=" + apiKey;
        if (channelId != null && !channelId.isEmpty()) {
            url += "&channelId=" + Uri.encode(channelId);
        }
        if (pageToken != null && !pageToken.isEmpty()) {
            url += "&pageToken=" + Uri.encode(pageToken);
        }
        Request.Builder rb = new Request.Builder().url(url);
        Context ctx = getContext();
        if (ctx != null) {
            rb.addHeader("X-Android-Package", ctx.getPackageName());
            String cert = getSignatureSha1(ctx);
            if (cert != null) rb.addHeader("X-Android-Cert", cert);
        }
        Response response = client.newCall(rb.build()).execute();
        if (response.code() == 403) {
            response.close();
            response = client.newCall(new Request.Builder().url(url)
                .addHeader("User-Agent", "SwamiSachidanand/1.0").build()).execute();
        }
        try (Response r = response) {
            if (!r.isSuccessful() || r.body() == null) return null;
            String body = r.body().string();
            JSONObject root = new JSONObject(body);
            JSONArray items = root.optJSONArray("items");
            if (items == null) return root.optString("nextPageToken", null);
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) continue;
                JSONObject idObj = item.optJSONObject("id");
                if (idObj == null) continue;
                String videoId = idObj.optString("videoId", null);
                if (videoId == null || videoId.isEmpty()) continue;
                JSONObject snippet = item.optJSONObject("snippet");
                if (snippet == null) continue;
                YouTubeVideo v = new YouTubeVideo();
                v.videoId = videoId;
                v.title = snippet.optString("title", "");
                v.publishedAt = snippet.optString("publishedAt", "");
                JSONObject thumbs = snippet.optJSONObject("thumbnails");
                if (thumbs != null) {
                    JSONObject med = thumbs.optJSONObject("medium");
                    if (med != null) v.thumbnailUrl = med.optString("url", null);
                    if ((v.thumbnailUrl == null || v.thumbnailUrl.isEmpty()) && thumbs.optJSONObject("high") != null) {
                        v.thumbnailUrl = thumbs.optJSONObject("high").optString("url", null);
                    }
                }
                if (v.thumbnailUrl == null || v.thumbnailUrl.isEmpty()) {
                    v.thumbnailUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
                }
                out.add(v);
            }
            return root.optString("nextPageToken", null);
        }
    }

    private void fetchFromChannelHtml(OkHttpClient client, String channelId, List<YouTubeVideo> out, int channelIndex) throws Exception {
        String url = "https://www.youtube.com/channel/" + channelId + "/videos";
        Request request = new Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-N975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .addHeader("Accept-Language", "en-US,en;q=0.9")
            .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                Log.w(TAG, "HTML fetch HTTP " + response.code() + " for " + channelId);
                return;
            }
            String html = response.body().string();
            if (html == null || html.isEmpty()) return;

            Set<String> seen = new HashSet<>();
            Matcher m = VIDEO_ID_PATTERN.matcher(html);
            while (m.find()) {
                String videoId = m.group(1);
                if (videoId != null && !seen.contains(videoId)) {
                    seen.add(videoId);
                    YouTubeVideo v = new YouTubeVideo();
                    v.videoId = videoId;
                    v.title = "Video " + videoId;
                    v.thumbnailUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
                    v.publishedAt = "";
                    v.channelIndex = channelIndex;
                    out.add(v);
                }
            }
            if (seen.isEmpty()) {
                m = WATCH_VIDEO_PATTERN.matcher(html);
                while (m.find()) {
                    String videoId = m.group(1);
                    if (videoId != null && !seen.contains(videoId)) {
                        seen.add(videoId);
                        YouTubeVideo v = new YouTubeVideo();
                        v.videoId = videoId;
                        v.title = "Video " + videoId;
                        v.thumbnailUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
                        v.publishedAt = "";
                        v.channelIndex = channelIndex;
                        out.add(v);
                    }
                }
            }
        }
    }

    /** Returns true if videos were added. Sets lastPipedBase, lastPipedNextpage on success.
     * @param useHandle if true, use /c/name (channel by handle); else use /channel/UCid */
    private boolean fetchFromPiped(OkHttpClient client, String channelIdOrHandle, String nextpage,
                                   List<YouTubeVideo> out, boolean useHandle, int channelIndex) throws Exception {
        OkHttpClient pipedClient = client;
        if (nextpage == null) {
            pipedClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .build();
        }
        String urlPath;
        if (nextpage != null && !nextpage.isEmpty()) {
            urlPath = "/nextpage/channel/" + channelIdOrHandle + "?nextpage=" + Uri.encode(nextpage);
        } else {
            urlPath = useHandle ? ("/c/" + Uri.encode(channelIdOrHandle)) : ("/channel/" + channelIdOrHandle);
        }
        String[] instances = {
            "https://pipedapi.kavin.rocks",
            "https://pipedapi.adminforge.de",
            "https://api.piped.yt",
            "https://pipedapi-libre.kavin.rocks"
        };
        for (String base : instances) {
            try {
                String url = base + urlPath;
                Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "SwamiSachidanand/1.0")
                    .build();
                try (Response response = pipedClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) continue;
                    String body = response.body().string();
                    if (body == null || body.isEmpty()) continue;
                    JSONObject root = new JSONObject(body);
                    JSONArray items = root.optJSONArray("relatedStreams");
                    if (items == null) continue;
                    String np = root.optString("nextpage", null);
                    if (np != null && !np.isEmpty()) {
                        lastPipedBase = base;
                        lastPipedNextpage = np;
                        nextPageToken = np;
                    } else {
                        lastPipedBase = base;
                        lastPipedNextpage = null;
                        nextPageToken = null;
                    }
                    for (int i = 0; i < Math.min(items.length(), 50); i++) {
                        JSONObject item = items.optJSONObject(i);
                        if (item == null) continue;
                        String urlStr = item.optString("url", "");
                        if (urlStr == null || !urlStr.contains("v=")) continue;
                        int vIdx = urlStr.indexOf("v=");
                        int end = urlStr.indexOf("&", vIdx);
                        String videoId = end > 0 ? urlStr.substring(vIdx + 2, end) : urlStr.substring(vIdx + 2);
                        if (videoId == null || videoId.length() != 11) continue;
                        String title = item.optString("title", "");
                        String uploadedDate = item.optString("uploadedDate", "");
                        String thumbUrl = item.optString("thumbnail", null);
                        if (thumbUrl == null || thumbUrl.isEmpty()) {
                            thumbUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
                        }
                        YouTubeVideo v = new YouTubeVideo();
                        v.videoId = videoId;
                        v.title = title != null ? title : "";
                        v.thumbnailUrl = thumbUrl;
                        v.publishedAt = uploadedDate != null ? uploadedDate : "";
                        v.channelIndex = channelIndex;
                        v.durationSeconds = item.optInt("duration", -1);
                        v.viewCount = item.optLong("views", -1);
                        out.add(v);
                    }
                    if (!out.isEmpty()) return true;
                }
            } catch (Exception e) {
                Log.w(TAG, "Piped " + base + " failed", e);
            }
        }
        return false;
    }

    /** Returns true if videos were added. Fetches from BOTH /videos and /shorts to get all content. */
    private boolean fetchFromInvidious(OkHttpClient client, String channelId, List<YouTubeVideo> out, int channelIndex) throws Exception {
        String[] instances = {
            "https://inv.nadeko.net",
            "https://yewtu.be",
            "https://vid.puffyan.us",
            "https://invidious.nerdvpn.de",
            "https://invidious.privacydev.net",
            "https://invidious.protokolla.fi"
        };
        for (String base : instances) {
            try {
                Set<String> seenIds = new HashSet<>();
                // Fetch from BOTH /videos (long-form) and /shorts – merge for complete channel content
                String[] tabs = {"/videos", "/shorts"};
                for (String tab : tabs) {
                    String url = base + "/api/v1/channels/" + channelId + tab;
                    Request request = new Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", "SwamiSachidanand/1.0")
                        .build();
                    try (Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful() || response.body() == null) continue;
                        String body = response.body().string();
                        if (body == null || body.isEmpty()) continue;
                        JSONObject root = new JSONObject(body);
                        JSONArray items = root.optJSONArray("videos");
                        if (items == null) continue;
                        for (int i = 0; i < Math.min(items.length(), 40); i++) {
                            JSONObject item = items.optJSONObject(i);
                            if (item == null) continue;
                            String videoId = item.optString("videoId", null);
                            if (videoId == null || videoId.isEmpty() || seenIds.contains(videoId)) continue;
                            seenIds.add(videoId);
                            String title = item.optString("title", "");
                            long published = item.optLong("published", 0);
                            String thumbUrl = null;
                            JSONArray thumbs = item.optJSONArray("videoThumbnails");
                            if (thumbs != null && thumbs.length() > 0) {
                                thumbUrl = thumbs.optJSONObject(0).optString("url", null);
                            }
                            if (thumbUrl == null || thumbUrl.isEmpty()) {
                                thumbUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
                            }
                            YouTubeVideo v = new YouTubeVideo();
                            v.videoId = videoId;
                            v.title = title != null ? title : "";
                            v.thumbnailUrl = thumbUrl;
                            v.publishedAt = published > 0 ? String.valueOf(published) : "";
                            v.channelIndex = channelIndex;
                            v.durationSeconds = item.optInt("lengthSeconds", -1);
                            v.viewCount = item.optLong("viewCount", -1);
                            out.add(v);
                        }
                    }
                }
                if (!out.isEmpty()) return true;
                // Fallback: channels/:id returns latestVideos
                String url2 = base + "/api/v1/channels/" + channelId;
                try (Response r2 = client.newCall(new Request.Builder().url(url2)
                    .addHeader("User-Agent", "SwamiSachidanand/1.0").build()).execute()) {
                    if (r2.isSuccessful() && r2.body() != null) {
                        JSONObject root2 = new JSONObject(r2.body().string());
                        JSONArray items2 = root2.optJSONArray("latestVideos");
                        if (items2 != null) {
                            for (int i = 0; i < Math.min(items2.length(), 40); i++) {
                                JSONObject item = items2.optJSONObject(i);
                                if (item == null) continue;
                                String videoId = item.optString("videoId", null);
                                if (videoId == null || videoId.isEmpty()) continue;
                                YouTubeVideo v = new YouTubeVideo();
                                v.videoId = videoId;
                                v.title = item.optString("title", "");
                                v.thumbnailUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
                                v.publishedAt = String.valueOf(item.optLong("published", 0));
                                v.channelIndex = channelIndex;
                                v.durationSeconds = item.optInt("lengthSeconds", -1);
                                v.viewCount = item.optLong("viewCount", -1);
                                out.add(v);
                            }
                            if (!out.isEmpty()) return true;
                        }
                    }
                } catch (Exception ignored) {}
            } catch (Exception e) {
                Log.w(TAG, "Invidious " + base + " failed", e);
            }
        }
        return false;
    }

    /** Fetches only /videos (long-form) from Invidious. Used to augment Piped when it returns Shorts-only. */
    private void fetchFromInvidiousVideosOnly(OkHttpClient client, String channelId, List<YouTubeVideo> out, int channelIndex) {
        Set<String> seenIds = new HashSet<>();
        for (YouTubeVideo v : out) {
            if (v.videoId != null) seenIds.add(v.videoId);
        }
        String[] instances = {
            "https://inv.nadeko.net", "https://yewtu.be", "https://vid.puffyan.us",
            "https://invidious.nerdvpn.de", "https://invidious.privacydev.net", "https://invidious.protokolla.fi"
        };
        for (String base : instances) {
            try {
                String url = base + "/api/v1/channels/" + channelId + "/videos";
                Request request = new Request.Builder().url(url)
                    .addHeader("User-Agent", "SwamiSachidanand/1.0").build();
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) continue;
                    String body = response.body().string();
                    if (body == null || body.isEmpty()) continue;
                    JSONObject root = new JSONObject(body);
                    JSONArray items = root.optJSONArray("videos");
                    if (items == null) continue;
                    int added = 0;
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.optJSONObject(i);
                        if (item == null) continue;
                        String videoId = item.optString("videoId", null);
                        if (videoId == null || videoId.isEmpty() || seenIds.contains(videoId)) continue;
                        seenIds.add(videoId);
                        String title = item.optString("title", "");
                        long published = item.optLong("published", 0);
                        String thumbUrl = null;
                        JSONArray thumbs = item.optJSONArray("videoThumbnails");
                        if (thumbs != null && thumbs.length() > 0) {
                            thumbUrl = thumbs.optJSONObject(0).optString("url", null);
                        }
                        if (thumbUrl == null || thumbUrl.isEmpty()) {
                            thumbUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
                        }
                        YouTubeVideo v = new YouTubeVideo();
                        v.videoId = videoId;
                        v.title = title != null ? title : "";
                        v.thumbnailUrl = thumbUrl;
                        v.publishedAt = published > 0 ? String.valueOf(published) : "";
                        v.channelIndex = channelIndex;
                        v.durationSeconds = item.optInt("lengthSeconds", -1);
                        v.viewCount = item.optLong("viewCount", -1);
                        out.add(v);
                        added++;
                    }
                    if (added > 0) {
                        Log.d(TAG, "Invidious /videos added " + added + " long-form videos");
                        return;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Invidious /videos augment failed " + base, e);
            }
        }
    }

    private void fetchFromRssFeed(OkHttpClient client, String channelId, List<YouTubeVideo> out, int channelIndex) throws Exception {
        String url = "https://www.youtube.com/feeds/videos.xml?channel_id=" + channelId;
        Request request = new Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0.0.0")
            .addHeader("Accept", "application/atom+xml, application/xml, */*")
            .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return;
            String xml = response.body().string();
            if (xml == null || xml.isEmpty()) return;

            int idx = 0;
            while (idx < xml.length()) {
                int start = xml.indexOf("<entry>", idx);
                if (start == -1) break;
                int end = xml.indexOf("</entry>", start);
                if (end == -1) break;
                String entry = xml.substring(start, end);
                idx = end + 8;

                String videoId = extractTag(entry, "yt:videoId");
                if (videoId == null || videoId.isEmpty()) continue;
                String title = extractTag(entry, "title");
                String published = extractTag(entry, "published");
                String thumbUrl = extractAttr(entry, "media:thumbnail", "url");
                if (thumbUrl == null || thumbUrl.isEmpty()) thumbUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";

                YouTubeVideo v = new YouTubeVideo();
                v.videoId = videoId;
                v.title = title != null ? title : "";
                v.thumbnailUrl = thumbUrl;
                v.publishedAt = published != null ? published : "";
                v.channelIndex = channelIndex;
                out.add(v);
            }
        }
    }

    private static String extractTag(String src, String tag) {
        if (src == null) return null;
        String open = "<" + tag + ">";
        String close = "</" + tag + ">";
        int start = src.indexOf(open);
        if (start == -1) return null;
        start += open.length();
        int end = src.indexOf(close, start);
        return end == -1 ? null : src.substring(start, end).trim();
    }

    private static String extractAttr(String src, String tag, String attrName) {
        if (src == null) return null;
        int start = src.indexOf("<" + tag);
        if (start == -1) return null;
        int end = src.indexOf(">", start);
        if (end == -1) return null;
        String chunk = src.substring(start, end);
        String key = attrName + "=\"";
        int aStart = chunk.indexOf(key);
        if (aStart == -1) return null;
        aStart += key.length();
        int aEnd = chunk.indexOf("\"", aStart);
        return aEnd == -1 ? null : chunk.substring(aStart, aEnd);
    }

    private static class YouTubeVideo {
        String videoId;
        String title;
        String thumbnailUrl;
        String publishedAt;
        int channelIndex = -1;  // 0=DANTALI, 1=OFFICIAL, 2=MAIN, 3=BLOG
        int durationSeconds = -1;
        long viewCount = -1;
    }

    /** Returns relative time string: "2 hours ago", "Yesterday", "3 days ago". */
    private static String toRelativeTime(String publishedAt) {
        if (publishedAt == null || publishedAt.isEmpty()) return "";
        long now = System.currentTimeMillis();
        long pubMs = 0;
        try {
            if (publishedAt.contains("T") && publishedAt.contains("Z")) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US);
                sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                pubMs = sdf.parse(publishedAt).getTime();
            } else if (publishedAt.matches("\\d+")) {
                pubMs = Long.parseLong(publishedAt) * 1000L;
            } else {
                return publishedAt;
            }
        } catch (Exception e) {
            return publishedAt.replace('T', ' ').replace("Z", "");
        }
        long diff = now - pubMs;
        if (diff < 0) return publishedAt;
        long sec = diff / 1000, min = sec / 60, hr = min / 60, day = hr / 24;
        if (day >= 7) return (day / 7) + " weeks ago";
        if (day >= 2) return day + " days ago";
        if (day == 1) return "Yesterday";
        if (hr >= 1) return hr + " hour" + (hr > 1 ? "s" : "") + " ago";
        if (min >= 1) return min + " minute" + (min > 1 ? "s" : "") + " ago";
        return "Just now";
    }

    private static String formatDuration(int seconds) {
        if (seconds < 0) return "";
        int h = seconds / 3600, m = (seconds % 3600) / 60, s = seconds % 60;
        if (h > 0) return String.format("%d:%02d:%02d", h, m, s);
        return String.format("%d:%02d", m, s);
    }

    private static String formatViewCount(long count) {
        if (count < 0) return "";
        if (count >= 1_000_000) return String.format("%.1fM views", count / 1_000_000.0);
        if (count >= 1_000) return String.format("%.1fK views", count / 1_000.0);
        return count + " views";
    }

    private class VideosAdapter extends RecyclerView.Adapter<VideosAdapter.VideoViewHolder> {
        private List<YouTubeVideo> items;

        VideosAdapter(List<YouTubeVideo> items) {
            this.items = items != null ? items : new ArrayList<>();
        }

        void setItems(List<YouTubeVideo> newItems) {
            this.items = newItems != null ? newItems : new ArrayList<>();
            notifyDataSetChanged();
        }

        @Override
        public VideoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new VideoViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video, parent, false));
        }

        @Override
        public void onBindViewHolder(VideoViewHolder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items != null ? items.size() : 0;
        }

        class VideoViewHolder extends RecyclerView.ViewHolder {
            final android.widget.ImageView thumbnailView;
            final android.widget.TextView titleView;
            final android.widget.TextView metaView;
            final android.widget.TextView durationBadge;

            VideoViewHolder(View itemView) {
                super(itemView);
                thumbnailView = itemView.findViewById(R.id.video_thumbnail);
                titleView = itemView.findViewById(R.id.video_title);
                metaView = itemView.findViewById(R.id.video_meta);
                durationBadge = itemView.findViewById(R.id.video_duration_badge);
            }

            void bind(YouTubeVideo video) {
                if (video == null) return;
                titleView.setText(video.title != null ? video.title : "");
                titleView.setTypeface(null, android.graphics.Typeface.BOLD);

                String relTime = toRelativeTime(video.publishedAt);
                String views = formatViewCount(video.viewCount);
                if (!relTime.isEmpty() && !views.isEmpty()) {
                    metaView.setText(relTime + " • " + views);
                } else {
                    metaView.setText(!relTime.isEmpty() ? relTime : views);
                }

                if (durationBadge != null) {
                    String dur = formatDuration(video.durationSeconds);
                    if (!dur.isEmpty()) {
                        durationBadge.setText(dur);
                        durationBadge.setVisibility(View.VISIBLE);
                    } else {
                        durationBadge.setVisibility(View.GONE);
                    }
                }

                if (thumbnailView != null) {
                    thumbnailView.setImageDrawable(null);
                    if (video.thumbnailUrl != null && !video.thumbnailUrl.isEmpty()) {
                        Glide.with(thumbnailView.getContext())
                            .load(video.thumbnailUrl)
                            .placeholder(R.drawable.book_placeholder)
                            .error(R.drawable.book_placeholder)
                            .centerCrop()
                            .into(thumbnailView);
                    } else {
                        thumbnailView.setImageResource(R.drawable.book_placeholder);
                    }
                }

                itemView.setOnClickListener(v -> openVideo(video.videoId));
            }
        }
    }

    private void loadMoreVideos() {
        if (loadingMore) return;
        long now = System.currentTimeMillis();
        if (now - lastLoadMoreTime < LOAD_MORE_THROTTLE_MS) return;
        boolean canLoad = false;
        if ("piped".equals(lastFetchSource) && lastPipedBase != null && lastPipedNextpage != null && lastChannelId != null) canLoad = true;
        else if ("playlist".equals(lastFetchSource)) {
            for (int i = 0; i < loadMorePageTokens.size(); i++) {
                if (loadMorePageTokens.get(i) != null && !loadMorePageTokens.get(i).isEmpty()) { canLoad = true; break; }
            }
        } else if ("search".equals(lastFetchSource) && lastChannelId != null && nextPageToken != null) canLoad = true;
        if (!canLoad) return;
        loadingMore = true;
        lastLoadMoreTime = now;
        final android.app.Activity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            loadingMore = false;
            return;
        }
        final String src = lastFetchSource;
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(8, TimeUnit.SECONDS)
                    .readTimeout(12, TimeUnit.SECONDS)
                    .build();
                List<YouTubeVideo> more = new ArrayList<>();
                String newNext = null;
                if ("piped".equals(src)) {
                    if (!fetchFromPiped(client, lastChannelId, lastPipedNextpage, more, false, -1)) {
                        activity.runOnUiThread(() -> loadingMore = false);
                        return;
                    }
                    newNext = lastPipedNextpage;
                } else if ("playlist".equals(src)) {
                    String apiKey = BuildConfig.YOUTUBE_API_KEY;
                    if (apiKey == null || apiKey.isEmpty()) {
                        activity.runOnUiThread(() -> loadingMore = false);
                        return;
                    }
                    for (int i = 0; i < loadMorePlaylistIds.size(); i++) {
                        String tok = loadMorePageTokens.get(i);
                        if (tok != null && !tok.isEmpty()) {
                            newNext = fetchPlaylistVideos(client, apiKey, loadMorePlaylistIds.get(i), tok, more, i);
                            loadMorePageTokens.set(i, (newNext != null && !newNext.isEmpty()) ? newNext : null);
                            break;
                        }
                    }
                } else if ("search".equals(src)) {
                    String apiKey = BuildConfig.YOUTUBE_API_KEY;
                    if (apiKey == null || apiKey.isEmpty()) {
                        activity.runOnUiThread(() -> loadingMore = false);
                        return;
                    }
                    newNext = fetchFromSearchApi(client, apiKey, lastChannelId, nextPageToken, more, -1);
                }
                final String finalNext = (newNext != null && !newNext.isEmpty()) ? newNext : null;
                if (more.isEmpty()) {
                    activity.runOnUiThread(() -> {
                        nextPageToken = null;
                        if ("piped".equals(src)) lastPipedNextpage = null;
                        loadingMore = false;
                    });
                    return;
                }
                Set<String> seen = new HashSet<>();
                for (YouTubeVideo v : allVideos) {
                    if (v.videoId != null) seen.add(v.videoId);
                }
                List<YouTubeVideo> toAdd = new ArrayList<>();
                for (YouTubeVideo v : more) {
                    if (v.videoId != null && !seen.contains(v.videoId)) {
                        seen.add(v.videoId);
                        toAdd.add(v);
                    }
                }
                if (toAdd.isEmpty()) {
                    activity.runOnUiThread(() -> {
                        nextPageToken = finalNext;
                        if ("piped".equals(src)) lastPipedNextpage = finalNext;
                        loadingMore = false;
                    });
                    return;
                }
                activity.runOnUiThread(() -> {
                    allVideos.addAll(toAdd);
                    Collections.sort(allVideos, (a, b) -> {
                        String pa = a.publishedAt != null ? a.publishedAt : "";
                        String pb = b.publishedAt != null ? b.publishedAt : "";
                        return pb.compareTo(pa);
                    });
                    if (adapter != null) adapter.setItems(allVideos);
                    nextPageToken = finalNext;
                    if ("piped".equals(src)) lastPipedNextpage = finalNext;
                    loadingMore = false;
                });
            } catch (Throwable t) {
                Log.e(TAG, "loadMoreVideos error", t);
                activity.runOnUiThread(() -> loadingMore = false);
            }
        }).start();
    }

    private void openVideo(String videoId) {
        if (videoId == null || videoId.isEmpty()) return;
        RecentVideoHelper.saveRecentVideoId(requireContext(), videoId);
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube://" + videoId));
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/watch?v=" + videoId));
                startActivity(intent);
            } catch (Exception e2) {
                Log.e(TAG, "openVideo failed", e2);
            }
        }
    }
}
