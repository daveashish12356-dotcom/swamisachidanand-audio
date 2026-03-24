package com.swamisachidanand;

import android.Manifest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.inputmethod.EditorInfo;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.LoadAdError;

import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;

/**
 * Audio Pravachan tab – lists pravachan entries from Firebase Firestore.
 */
public class AudioPravachanFragment extends Fragment implements PravachanAdapter.Listener {
    private static final String TAG = "AudioPravachanFragment";

    // Used when Home page "Latest Pravachan" wants to auto-open + start a specific MP3.
    public static final String ARG_START_ITEM = "start_item";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private PravachanAdapter adapter;

    // Inline player state (same page, under selected card)
    private ExoPlayer player;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable seekRunnable;
    private boolean userSeeking = false;

    private PravachanItem currentItem;
    private String currentItemId;
    private String playingItemId;
    private boolean wasPlayingBeforePause = false;
    private long lastPositionMs = 0L;

    // Inline UI refs from adapter
    private ImageButton inlinePlayBtn;
    private SeekBar inlineSeekBar;
    private TextView inlineTimeCurrent;
    private TextView inlineTimeTotal;

    // Search UI
    private TextInputEditText searchInput;
    private ImageView clearSearch;
    private ImageView micButton;
    private TextView searchResultsTitle;

    private RecyclerView dainikRecycler;
    private DainikPravachanCategoryAdapter dainikAdapter;
    private ViewGroup dainikContainer;
    private View dainikDetailHeader;
    private ImageButton dainikDetailBack;
    private TextView dainikDetailTitle;
    private PravachanAdapter dainikTracksAdapter;
    private final List<PravachanItem> dainikServerTracksAll = new ArrayList<>();
    private boolean dainikShowingServerTracks;
    private ChipGroup filterChipGroup;
    private CharSequence defaultEmptyText = "";

    private enum PravachanTopicFilter {
        ALL, RAMAYAN, GEETA, SWADHYAY, DHARM, DAINIK
    }

    private PravachanTopicFilter topicFilter = PravachanTopicFilter.ALL;

    private static final int REQUEST_CODE_VOICE_SEARCH = 10001;
    private String currentQuery = "";
    private final List<PravachanItem> allPravachans = new ArrayList<>();

    // Pagination — 100 docs/request = ~50 requests for 5000 pravachan (Firestore max 1 query batch)
    private static final int PRAVACHAN_PAGE_SIZE = 100;
    /** Local “new pravachan” detection (newest Firestore doc by createdAt — before title reorder). */
    private static final String PRAVACHAN_NOTIFY_PREFS = "pravachan_notify";
    private static final String PREF_LAST_NEWEST_PRAVACHAN_ID = "last_newest_pravachan_doc_id";
    private boolean isLoadingMore = false;
    private boolean isEndReached = false;
    private DocumentSnapshot lastPravachanDoc = null;
    /**
     * Incremented on every first-page snapshot reload. Pagination must only apply if epoch
     * still matches — do NOT compare {@link DocumentSnapshot} with == (new listener events
     * create new instances for the same doc and would falsely discard every page).
     */
    private int pravachanDataEpoch = 0;
    /**
     * False until first snapshot's items are committed to {@link #allPravachans} on the main thread.
     * Prevents pagination from merging while {@link #allPravachans} is still empty (async reorder),
     * which would then be overwritten by the first-page callback and look like "only 4 items".
     */
    private boolean firstPageListReady = false;
    /** Dedupe by Firestore document id (not URL) so Part A / Part B with same file URL both show. */
    private final java.util.HashSet<String> seenDocumentIdsGlobal = new java.util.HashSet<>();

    private AdView pravachanBottomBannerAd;

    // Pending autoplay (set from MainActivity when switching from Home).
    private PravachanItem pendingStartItem;
    private boolean pendingAutoPlayStarted = false;

    /** Heavy reorder off main thread so playback / taps stay responsive with 1000+ rows */
    private ExecutorService pravachanReorderExecutor;
    private final AtomicInteger reorderGeneration = new AtomicInteger(0);

    /** Auto-fetch extra Firestore pages until list is “tall enough” (no scroll required). */
    private static final int BACKING_PREFETCH_MIN_ITEMS = 72;
    private static final int MAX_BACKING_BUFFER_TICKS = 120;
    private int backingBufferSafetyTicks = 0;
    private final Runnable ensureBackingBufferRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isAdded()) return;
            if (backingBufferSafetyTicks++ > MAX_BACKING_BUFFER_TICKS) {
                Log.w(TAG, "pravachan backing buffer prefetch stopped (safety cap)");
                return;
            }
            if (currentQuery != null && !currentQuery.trim().isEmpty()) return;
            if (!firstPageListReady || isEndReached) return;
            if (allPravachans.size() >= BACKING_PREFETCH_MIN_ITEMS) return;
            if (isLoadingMore) {
                mainHandler.postDelayed(this, 120);
                return;
            }
            loadNextPageIfNeeded();
            mainHandler.postDelayed(this, 220);
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pravachanReorderExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "pravachan-reorder");
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
    }

    @Override
    public void onDestroy() {
        if (pravachanReorderExecutor != null) {
            pravachanReorderExecutor.shutdownNow();
            pravachanReorderExecutor = null;
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_audio_pravachan_empty, container, false);

        // If opened from Home, this might be set to auto-play the same item.
        try {
            Bundle args = getArguments();
            if (args != null) {
                pendingStartItem = args.getParcelable(ARG_START_ITEM);
            }
        } catch (Throwable ignore) {
        }

        recyclerView = root.findViewById(R.id.pravachan_recycler);
        progressBar = root.findViewById(R.id.pravachan_progress);
        emptyView = root.findViewById(R.id.pravachan_empty);

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            // Extra bottom space so last rows + inline player aren’t under banner / hard to tap
            int extraBottom = (int) (requireContext().getResources().getDisplayMetrics().density * 96f);
            recyclerView.setPadding(
                    recyclerView.getPaddingLeft(),
                    recyclerView.getPaddingTop(),
                    recyclerView.getPaddingRight(),
                    extraBottom);
            adapter = new PravachanAdapter();
            adapter.setListener(this);
            bindInlinePlayerToPravachanAdapter(adapter);
            recyclerView.setAdapter(adapter);
            recyclerView.setItemViewCacheSize(24);

            // Bottom nav hide/show animation on scroll (like other pages)
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                    super.onScrolled(rv, dx, dy);
                    try {
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).onScrolled(dy);
                        }
                    } catch (Throwable ignore) {}

                    maybeLoadMorePravachanPages(rv);
                }

                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        maybeLoadMorePravachanPages(recyclerView);
                    }
                }
            });
        }

        if (emptyView != null) {
            defaultEmptyText = emptyView.getText();
        }

        dainikContainer = root.findViewById(R.id.pravachan_dainik_container);
        dainikRecycler = root.findViewById(R.id.pravachan_dainik_recycler);
        dainikDetailHeader = root.findViewById(R.id.pravachan_dainik_detail_header);
        dainikDetailBack = root.findViewById(R.id.pravachan_dainik_detail_back);
        dainikDetailTitle = root.findViewById(R.id.pravachan_dainik_detail_title);
        filterChipGroup = root.findViewById(R.id.pravachan_filter_chips);
        if (dainikRecycler != null) {
            dainikRecycler.setLayoutManager(new GridLayoutManager(requireContext(), 2));
            int dpad = (int) (requireContext().getResources().getDisplayMetrics().density * 88f);
            dainikRecycler.setPadding(
                    dainikRecycler.getPaddingLeft(),
                    dainikRecycler.getPaddingTop(),
                    dainikRecycler.getPaddingRight(),
                    dpad);
            dainikAdapter = new DainikPravachanCategoryAdapter();
            dainikAdapter.setListener(title -> {
                if (!isAdded()) return;
                String slug = DainikPravachanServer.slugForCategoryTitle(title);
                if (slug != null) {
                    loadDainikServerTracks(slug, title);
                } else {
                    Toast.makeText(requireContext(), R.string.dainik_pravachan_coming_soon, Toast.LENGTH_SHORT).show();
                }
            });
            dainikRecycler.setAdapter(dainikAdapter);
            dainikRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                    super.onScrolled(rv, dx, dy);
                    try {
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).onScrolled(dy);
                        }
                    } catch (Throwable ignore) {
                    }
                }
            });
        }
        if (dainikDetailBack != null) {
            dainikDetailBack.setOnClickListener(v -> {
                resetDainikToCategoryGrid();
                bindDainikCategories(currentQuery);
            });
        }
        setupFilterChips();

        setupPravachanSearch(root);
        loadFirstPageWithRealtime();
        setupPravachanBottomBannerAd(root);

        return root;
    }

    private void bindInlinePlayerToPravachanAdapter(@NonNull PravachanAdapter pravachanAdapter) {
        pravachanAdapter.setInlinePlayerViewsListener((playBtn, back10Btn, forward10Btn, seekBar, timeCurrent, timeTotal) -> {
            inlinePlayBtn = playBtn;
            final ImageButton localBack10Btn = back10Btn;
            final ImageButton localForward10Btn = forward10Btn;
            inlineSeekBar = seekBar;
            inlineTimeCurrent = timeCurrent;
            inlineTimeTotal = timeTotal;

            inlineSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    userSeeking = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    userSeeking = false;
                }

                @Override
                public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                    if (!fromUser || player == null) return;
                    long duration = player.getDuration();
                    if (duration <= 0) return;
                    long pos = (long) (progress / 1000.0 * duration);
                    player.seekTo(pos);
                }
            });

            updatePlayIcon();
            if (player != null) startSeekUpdates();

            if (localBack10Btn != null) {
                localBack10Btn.setOnClickListener(v -> {
                    if (player == null) return;
                    long pos = player.getCurrentPosition();
                    long next = Math.max(0, pos - 10_000);
                    player.seekTo(next);
                    updatePlayIcon();
                    startSeekUpdates();
                });
            }

            if (localForward10Btn != null) {
                localForward10Btn.setOnClickListener(v -> {
                    if (player == null) return;
                    long pos = player.getCurrentPosition();
                    long next = pos + 10_000;
                    player.seekTo(next);
                    updatePlayIcon();
                    startSeekUpdates();
                });
            }
        });
    }

    private void setupPravachanBottomBannerAd(View root) {
        try {
            pravachanBottomBannerAd = root.findViewById(R.id.pravachan_bottom_banner);
            if (pravachanBottomBannerAd == null) return;

            AdRequest request = new AdRequest.Builder().build();
            pravachanBottomBannerAd.setAdListener(AdLog.wrapBannerListener("pravachan_bottom", new AdListener() {
                @Override
                public void onAdLoaded() {
                    try {
                        if (pravachanBottomBannerAd.getVisibility() != View.VISIBLE) {
                            pravachanBottomBannerAd.setAlpha(0f);
                            pravachanBottomBannerAd.setTranslationY(12f);
                            pravachanBottomBannerAd.setVisibility(View.VISIBLE);
                            pravachanBottomBannerAd.animate()
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
                        Log.w(TAG, "pravachan bottom banner failed: code=" + code + ", domain=" + domain + ", message=" + msg);
                        pravachanBottomBannerAd.setVisibility(View.VISIBLE);
                        pravachanBottomBannerAd.setAlpha(0.25f);
                    } catch (Throwable ignore) {}
                }
            }));
            AdLog.bannerRequest("pravachan_bottom");
            BannerAdHelper.loadWhenReady(requireContext(), pravachanBottomBannerAd, request);
        } catch (Throwable t) {
            Log.e(TAG, "setupPravachanBottomBannerAd", t);
        }
    }

    /** Load next Firestore page when user nears list end (scroll up/down or idle). */
    private void maybeLoadMorePravachanPages(@NonNull RecyclerView rv) {
        try {
            if (topicFilter == PravachanTopicFilter.DAINIK) return;
            if (!(rv.getLayoutManager() instanceof LinearLayoutManager)) return;
            LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
            if (lm == null) return;
            int totalCount = lm.getItemCount();
            if (totalCount <= 0) return;
            int lastVisible = lm.findLastVisibleItemPosition();
            int lastComplete = lm.findLastCompletelyVisibleItemPosition();
            int anchor = Math.max(lastVisible, lastComplete);
            int remaining = totalCount - 1 - anchor;
            if (remaining <= 12) {
                loadNextPageIfNeeded();
            }
        } catch (Throwable ignore) {
        }
    }

    private void loadFirstPageWithRealtime() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (emptyView != null) emptyView.setVisibility(View.GONE);

        // Reset pagination state
        isLoadingMore = false;
        isEndReached = false;
        lastPravachanDoc = null;
        firstPageListReady = false;
        seenDocumentIdsGlobal.clear();
        if (allPravachans != null) allPravachans.clear();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        mainHandler.removeCallbacks(ensureBackingBufferRunnable);

        // createdAt tie (Telegram album = same second) par bina __name__ ke pagination docs chhod deti hai
        final Query firstQuery = db.collection("pravachan")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
                .limit(PRAVACHAN_PAGE_SIZE);

        // One-shot fetch avoids cache+server double snapshots breaking pagination / list state.
        firstQuery.get(Source.SERVER)
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    onFirstPageLoaded(snap);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "pravachan first page SERVER failed, trying default source", e);
                    firstQuery.get(Source.DEFAULT)
                            .addOnSuccessListener(snap -> {
                                if (!isAdded()) return;
                                onFirstPageLoaded(snap);
                            })
                            .addOnFailureListener(e2 -> {
                                Log.e(TAG, "pravachan first page load failed", e2);
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                                if (emptyView != null) {
                                    emptyView.setText("પ્રવચન લોડ થઈ શક્યાં નહીં.");
                                    emptyView.setVisibility(View.VISIBLE);
                                }
                            });
                });
    }

    private void startEnsureBackingBuffer() {
        backingBufferSafetyTicks = 0;
        mainHandler.removeCallbacks(ensureBackingBufferRunnable);
        mainHandler.post(ensureBackingBufferRunnable);
    }

    private void onFirstPageLoaded(@Nullable QuerySnapshot snap) {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        // Invalidate any in-flight paginated fetch (first page query window actually changed).
        pravachanDataEpoch++;
        // Block pagination until this snapshot is merged into allPravachans (avoids race with async reorder).
        firstPageListReady = false;
        // First page realtime reload -> recompute from scratch.
        isLoadingMore = false;
        isEndReached = false;
        lastPravachanDoc = null;

        List<PravachanItem> list = parsePravachanDocs(snap, true);
        int rawDocCountLog = (snap != null && snap.getDocuments() != null) ? snap.getDocuments().size() : 0;
        Log.i(TAG, "pravachan first page: rawFirestoreDocs=" + rawDocCountLog + " parsedRows=" + list.size());
        if (!list.isEmpty()) {
            Context ctx = getContext();
            if (ctx != null) {
                checkAndNotifyIfNewestPravachanChanged(ctx, list.get(0));
            }
        }

        // Update last doc for pagination
        if (snap != null && snap.getDocuments() != null && !snap.getDocuments().isEmpty()) {
            lastPravachanDoc = snap.getDocuments().get(snap.getDocuments().size() - 1);
            if (snap.getDocuments().size() < PRAVACHAN_PAGE_SIZE) {
                isEndReached = true;
            }
        } else {
            isEndReached = true;
        }

        if (list.isEmpty()) {
            allPravachans.clear();
            firstPageListReady = true;
            applySearchFilter(currentQuery);
            tryAutoPlayPending();
            return;
        }

        final int gen = reorderGeneration.incrementAndGet();
        final List<PravachanItem> toSort = new ArrayList<>(list);
        ExecutorService ex = pravachanReorderExecutor;
        if (ex == null || ex.isShutdown()) {
            allPravachans.clear();
            allPravachans.addAll(reorderPravachansByBaseAndPart(toSort));
            firstPageListReady = true;
            applySearchFilter(currentQuery);
            tryAutoPlayPending();
            postPrefetchIfShort();
            return;
        }
        ex.execute(() -> {
            List<PravachanItem> sorted;
            try {
                sorted = reorderPravachansByBaseAndPart(toSort);
            } catch (Throwable t) {
                Log.e(TAG, "reorder first page failed", t);
                sorted = toSort;
            }
            final List<PravachanItem> out = sorted;
            mainHandler.post(() -> {
                if (!isAdded() || gen != reorderGeneration.get()) return;
                allPravachans.clear();
                allPravachans.addAll(out);
                firstPageListReady = true;
                applySearchFilter(currentQuery);
                tryAutoPlayPending();
                postPrefetchIfShort();
            });
        });
    }

    private void postPrefetchIfShort() {
        if (!isEndReached && allPravachans.size() < BACKING_PREFETCH_MIN_ITEMS) {
            startEnsureBackingBuffer();
        }
    }

    /**
     * When newest Firestore pravachan (by {@code createdAt} desc) changes, show the same notification as
     * FCM {@code kind=new_pravachan} — opens Pravachan tab. First run only stores baseline id.
     */
    private void checkAndNotifyIfNewestPravachanChanged(@NonNull Context ctx, @NonNull PravachanItem newest) {
        if (newest.id == null || newest.id.isEmpty()) return;
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PRAVACHAN_NOTIFY_PREFS, Context.MODE_PRIVATE);
            String prev = sp.getString(PREF_LAST_NEWEST_PRAVACHAN_ID, null);
            String topId = newest.id;
            if (prev == null) {
                sp.edit().putString(PREF_LAST_NEWEST_PRAVACHAN_ID, topId).apply();
                Log.d(TAG, "pravachan notify: baseline newest id=" + topId);
                return;
            }
            if (prev.equals(topId)) return;

            sp.edit().putString(PREF_LAST_NEWEST_PRAVACHAN_ID, topId).apply();
            Log.d(TAG, "pravachan notify: new top " + topId + " (was " + prev + ")");

            boolean canPost = true;
            if (Build.VERSION.SDK_INT >= 33) {
                canPost = ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED;
                if (!canPost) {
                    Log.w(TAG, "pravachan notify: POST_NOTIFICATIONS not granted — id updated, no local notification");
                }
            }
            if (!canPost) return;

            String title = ctx.getString(R.string.new_pravachan_notification_title);
            String body = (newest.title != null && !newest.title.trim().isEmpty())
                    ? newest.title.trim()
                    : topId;
            ContentUpdateNotificationHelper.showContentUpdate(ctx, "new_pravachan", title, body, null);
        } catch (Throwable t) {
            Log.e(TAG, "checkAndNotifyIfNewestPravachanChanged", t);
        }
    }

    private void tryAutoPlayPending() {
        try {
            if (pendingStartItem == null) return;
            if (pendingAutoPlayStarted) return;
            if (adapter == null || allPravachans == null) return;

            if (topicFilter == PravachanTopicFilter.DAINIK && filterChipGroup != null) {
                filterChipGroup.check(R.id.chip_pravachan_all);
            }

            // If user came with a search query, clear it so the item definitely appears in list.
            if (currentQuery != null && !currentQuery.trim().isEmpty()) {
                currentQuery = "";
                if (searchInput != null) {
                    try {
                        searchInput.setText("");
                    } catch (Throwable ignore) {
                    }
                }
                applySearchFilter("");
            }

            for (int i = 0; i < allPravachans.size(); i++) {
                PravachanItem p = allPravachans.get(i);
                if (p == null || p.id == null) continue;
                boolean idMatch = pendingStartItem.id != null && pendingStartItem.id.equals(p.id);
                boolean urlMatch = pendingStartItem.audioUrl != null && pendingStartItem.audioUrl.equals(p.audioUrl);
                if (idMatch || urlMatch) {
                    pendingAutoPlayStarted = true;
                    PravachanItem toPlay = p;
                    pendingStartItem = null; // consume once
                    onPravachanClick(toPlay, i);
                    break;
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "tryAutoPlayPending failed", t);
        }
    }

    /** Firestore may store createdAt as Timestamp, millis Long, or legacy Double. */
    private static long readPravachanCreatedAtMillis(@Nullable DocumentSnapshot d) {
        if (d == null) return 0L;
        try {
            com.google.firebase.Timestamp ts = d.getTimestamp("createdAt");
            if (ts != null) return ts.toDate().getTime();
        } catch (Throwable ignore) {
        }
        try {
            Long lng = d.getLong("createdAt");
            if (lng != null) return lng;
        } catch (Throwable ignore) {
        }
        try {
            Double dbl = d.getDouble("createdAt");
            if (dbl != null) return dbl.longValue();
        } catch (Throwable ignore) {
        }
        try {
            java.util.Date dt = d.getDate("createdAt");
            if (dt != null) return dt.getTime();
        } catch (Throwable ignore) {
        }
        return 0L;
    }

    private List<PravachanItem> parsePravachanDocs(@Nullable QuerySnapshot snap, boolean resetSeenUrls) {
        if (resetSeenUrls) seenDocumentIdsGlobal.clear();
        List<PravachanItem> list = new ArrayList<>();
        if (snap == null || snap.getDocuments() == null) return list;

        for (DocumentSnapshot d : snap.getDocuments()) {
            String id = d.getId();
            String title = d.getString("title");
            String url = d.getString("audioUrl");
            if (url == null || url.trim().isEmpty()) {
                url = d.getString("audio_url");
            }
            String speaker = d.getString("speaker");
            long createdAt = readPravachanCreatedAtMillis(d);

            if (title == null || url == null || title.trim().isEmpty() || url.trim().isEmpty()) continue;

            String cleanTitle = cleanPravachanTitle(title.trim());
            String cleanUrl = url.trim();

            // Skip test / old entries like "aud 2026 ..."
            String lowerTitle = cleanTitle.toLowerCase();
            if (lowerTitle.startsWith("aud 2026") || lowerTitle.startsWith("aud_2026")) continue;

            // Avoid duplicate Firestore docs only (same id), not same URL across different parts
            if (seenDocumentIdsGlobal.contains(id)) continue;
            seenDocumentIdsGlobal.add(id);

            list.add(new PravachanItem(id, cleanTitle, cleanUrl, speaker, createdAt));
        }
        return list;
    }

    private void loadNextPageIfNeeded() {
        // Don't paginate during search (filtered list may cause confusion)
        if (currentQuery != null && !currentQuery.trim().isEmpty()) return;
        if (!firstPageListReady) return;
        if (isLoadingMore || isEndReached) return;
        if (lastPravachanDoc == null) return;

        isLoadingMore = true;
        final int epochAtStart = pravachanDataEpoch;
        final DocumentSnapshot pageCursor = lastPravachanDoc;
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Query q = db.collection("pravachan")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
                .startAfter(pageCursor)
                .limit(PRAVACHAN_PAGE_SIZE);

        // Show nothing fancy; keep smooth scrolling.
        q.get()
                .addOnSuccessListener(snap -> {
                    try {
                        if (snap == null || snap.getDocuments() == null || snap.getDocuments().isEmpty()) {
                            isLoadingMore = false;
                            isEndReached = true;
                            return;
                        }

                        List<PravachanItem> newItems = parsePravachanDocs(snap, false);
                        final List<PravachanItem> combined = new ArrayList<>(allPravachans);
                        combined.addAll(newItems);
                        final DocumentSnapshot newLast = snap.getDocuments().get(snap.getDocuments().size() - 1);
                        final int rawDocCount = snap.getDocuments().size();

                        ExecutorService ex = pravachanReorderExecutor;
                        if (ex == null || ex.isShutdown()) {
                            isLoadingMore = false;
                            if (!isAdded()) return;
                            if (pravachanDataEpoch != epochAtStart) return;
                            List<PravachanItem> out = reorderPravachansByBaseAndPart(combined);
                            allPravachans.clear();
                            allPravachans.addAll(out);
                            applySearchFilter(currentQuery);
                            tryAutoPlayPending();
                            lastPravachanDoc = newLast;
                            if (rawDocCount < PRAVACHAN_PAGE_SIZE) isEndReached = true;
                            Log.d(TAG, "pravachan page merge (sync) epoch=" + epochAtStart + " totalItems=" + allPravachans.size());
                            startEnsureBackingBuffer();
                            return;
                        }
                        ex.execute(() -> {
                            List<PravachanItem> sorted;
                            try {
                                sorted = reorderPravachansByBaseAndPart(combined);
                            } catch (Throwable t) {
                                Log.e(TAG, "reorder next page failed", t);
                                sorted = combined;
                            }
                            final List<PravachanItem> out = sorted;
                            mainHandler.post(() -> {
                                isLoadingMore = false;
                                if (!isAdded()) return;
                                if (pravachanDataEpoch != epochAtStart) return;
                                allPravachans.clear();
                                allPravachans.addAll(out);
                                applySearchFilter(currentQuery);
                                tryAutoPlayPending();
                                lastPravachanDoc = newLast;
                                if (rawDocCount < PRAVACHAN_PAGE_SIZE) {
                                    isEndReached = true;
                                }
                                Log.d(TAG, "pravachan page merge (async) epoch=" + epochAtStart + " totalItems=" + allPravachans.size());
                                startEnsureBackingBuffer();
                            });
                        });
                    } catch (Throwable t) {
                        isLoadingMore = false;
                        Log.e(TAG, "loadNextPage success handler", t);
                    }
                })
                .addOnFailureListener(e -> {
                    isLoadingMore = false;
                    Log.e(TAG, "loadNextPage Firestore failed", e);
                });
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
            // Within group: Part 1,2,3… then Part A,B,C… then undated order by time
            g.items.sort((a, b) -> comparePartsInGroup(a.title, b.title, a.createdAtMillis, b.createdAtMillis));
            out.addAll(g.items);
        }

        return out;
    }

    /**
     * Sort key for one pravachan title: numeric parts first (Part 1 … Part 4000), then letter parts.
     */
    private static class PartInfo {
        /** Grouping key: title without trailing part suffix */
        final String baseKey;
        /** e.g. (Part 7) → 7; null if not numeric */
        final Integer numericPart;
        /** (Part C) → 2; trailing " B" → 1; -1 = not a letter part */
        final int letterPartIndex;

        PartInfo(String baseKey, Integer numericPart, int letterPartIndex) {
            this.baseKey = baseKey != null ? baseKey : "";
            this.numericPart = numericPart;
            this.letterPartIndex = letterPartIndex;
        }
    }

    private static int comparePartsInGroup(String titleA, String titleB, long createdA, long createdB) {
        PartInfo ia = parsePartInfo(titleA);
        PartInfo ib = parsePartInfo(titleB);
        if (ia.numericPart != null && ib.numericPart != null) {
            int c = ia.numericPart.compareTo(ib.numericPart);
            if (c != 0) return c;
            return Long.compare(createdB, createdA);
        }
        if (ia.numericPart != null) return -1;
        if (ib.numericPart != null) return 1;
        if (ia.letterPartIndex >= 0 && ib.letterPartIndex >= 0) {
            int c = Integer.compare(ia.letterPartIndex, ib.letterPartIndex);
            if (c != 0) return c;
            return Long.compare(createdB, createdA);
        }
        if (ia.letterPartIndex >= 0) return -1;
        if (ib.letterPartIndex >= 0) return 1;
        return Long.compare(createdB, createdA);
    }

    private static PartInfo parsePartInfo(String title) {
        if (title == null) return new PartInfo("", null, -1);
        String t = title.trim();

        // "... (Part 123)" — supports any positive integer (Part 1 … Part 4000+)
        java.util.regex.Matcher mn = java.util.regex.Pattern
                .compile("\\(\\s*Part\\s+(\\d{1,6})\\s*\\)\\s*$", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(t);
        if (mn.find()) {
            try {
                int num = Integer.parseInt(mn.group(1));
                String base = t.substring(0, mn.start()).trim();
                String baseKey = base.toLowerCase().replaceAll("\\s+", " ");
                return new PartInfo(baseKey, num, -1);
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }

        // "... (Part A)" … "(Part Z)"
        java.util.regex.Matcher ml = java.util.regex.Pattern
                .compile("\\(\\s*Part\\s+([A-Za-z])\\s*\\)\\s*$", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(t);
        if (ml.find()) {
            char c = Character.toUpperCase(ml.group(1).charAt(0));
            int letterIdx = c - 'A';
            if (letterIdx >= 0 && letterIdx <= 25) {
                String base = t.substring(0, ml.start()).trim();
                String baseKey = base.toLowerCase().replaceAll("\\s+", " ");
                return new PartInfo(baseKey, null, letterIdx);
            }
        }

        // Title ends with single letter A–Z as part (e.g. "... A", "... Z")
        java.util.regex.Matcher m2 = java.util.regex.Pattern
                .compile("(^|\\s|_)([A-Za-z])\\s*$", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(t);
        if (m2.find()) {
            char c = Character.toUpperCase(m2.group(2).charAt(0));
            int letterIdx = c - 'A';
            if (letterIdx >= 0 && letterIdx <= 25) {
                String base = t.substring(0, m2.start(2)).trim();
                String baseKey = base.toLowerCase().replaceAll("\\s+", " ");
                return new PartInfo(baseKey, null, letterIdx);
            }
        }

        String baseKey = t.toLowerCase().replaceAll("\\s+", " ");
        return new PartInfo(baseKey, null, -1);
    }

    private void setupPravachanSearch(@NonNull View root) {
        // Search bar (baki pages jaisa)
        searchInput = root.findViewById(R.id.global_search_input);
        clearSearch = root.findViewById(R.id.global_clear_search);
        micButton = root.findViewById(R.id.global_mic_button);
        searchResultsTitle = root.findViewById(R.id.pravachan_search_results_title);
        View avatar = root.findViewById(R.id.global_profile_avatar);

        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String q = s != null ? s.toString().trim() : "";
                    currentQuery = q;
                    if (clearSearch != null) clearSearch.setVisibility(q.isEmpty() ? View.GONE : View.VISIBLE);
                    applySearchFilter(q);
                    if (q.isEmpty()) {
                        // Hide results title
                        if (searchResultsTitle != null) searchResultsTitle.setVisibility(View.GONE);
                    } else {
                        if (searchResultsTitle != null) searchResultsTitle.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            searchInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    // Stay on same page; list already filtered.
                    try {
                        android.view.inputmethod.InputMethodManager imm =
                                (android.view.inputmethod.InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    } catch (Throwable ignore) {}
                    return true;
                }
                return false;
            });
        }

        if (clearSearch != null) {
            clearSearch.setOnClickListener(v -> {
                try {
                    if (searchInput != null) searchInput.setText("");
                } catch (Throwable ignore) {}
            });
        }

        if (micButton != null) {
            micButton.setOnClickListener(v -> startVoiceSearch());
        }

        // Swami avatar next to search bar -> open swami info page
        if (avatar != null) {
            avatar.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openSwamiInfoPage();
                }
            });
        }
    }

    private void startVoiceSearch() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "gu-IN");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "gu-IN");
            intent.putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, new String[]{"gu-IN", "en-IN", "hi-IN"});
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "પ્રવચન શોધવા બોલો… / Search something…");
            startActivityForResult(intent, REQUEST_CODE_VOICE_SEARCH);
        } catch (Throwable t) {
            Toast.makeText(getContext(), "Voice search not available", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "voice search error", t);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CODE_VOICE_SEARCH) return;
        if (resultCode != android.app.Activity.RESULT_OK) return;
        if (data == null) return;

        ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (results == null || results.isEmpty()) return;
        String spokenText = results.get(0);
        if (spokenText == null) return;
        if (searchInput != null) {
            searchInput.setText(spokenText);
            searchInput.setSelection(spokenText.length());
        }
    }

    private void setupFilterChips() {
        if (filterChipGroup == null) return;
        filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!isAdded()) return;
            if (checkedIds == null || checkedIds.isEmpty()) return;
            int id = checkedIds.get(0).intValue();
            PravachanTopicFilter next = PravachanTopicFilter.ALL;
            if (id == R.id.chip_pravachan_ramayan) {
                next = PravachanTopicFilter.RAMAYAN;
            } else if (id == R.id.chip_pravachan_geeta) {
                next = PravachanTopicFilter.GEETA;
            } else if (id == R.id.chip_pravachan_swadhyay) {
                next = PravachanTopicFilter.SWADHYAY;
            } else if (id == R.id.chip_pravachan_dharm) {
                next = PravachanTopicFilter.DHARM;
            } else if (id == R.id.chip_pravachan_dainik) {
                next = PravachanTopicFilter.DAINIK;
            }
            if (next == PravachanTopicFilter.DAINIK) {
                resetDainikToCategoryGrid();
            }
            topicFilter = next;
            updateListVisibilityForTopic();
            applySearchFilter(currentQuery);
        });
    }

    private void updateListVisibilityForTopic() {
        boolean dainik = topicFilter == PravachanTopicFilter.DAINIK;
        if (recyclerView != null) {
            recyclerView.setVisibility(dainik ? View.GONE : View.VISIBLE);
        }
        if (dainikContainer != null) {
            dainikContainer.setVisibility(dainik ? View.VISIBLE : View.GONE);
        }
        if (!dainik) {
            resetDainikToCategoryGrid();
        }
    }

    private void resetDainikToCategoryGrid() {
        dainikShowingServerTracks = false;
        dainikServerTracksAll.clear();
        if (dainikDetailHeader != null) {
            dainikDetailHeader.setVisibility(View.GONE);
        }
        if (dainikRecycler != null && dainikAdapter != null && getContext() != null) {
            dainikRecycler.setLayoutManager(new GridLayoutManager(requireContext(), 2));
            int dpad = (int) (requireContext().getResources().getDisplayMetrics().density * 88f);
            dainikRecycler.setPadding(
                    dainikRecycler.getPaddingLeft(),
                    dainikRecycler.getPaddingTop(),
                    dainikRecycler.getPaddingRight(),
                    dpad);
            dainikRecycler.setAdapter(dainikAdapter);
        }
    }

    private void loadDainikServerTracks(@NonNull final String slug, @NonNull final String displayTitle) {
        if (dainikRecycler == null || getContext() == null) return;
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        final String url = DainikPravachanServer.listUrl(slug);
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(25, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .build();
                Request req = new Request.Builder().url(url).build();
                try (Response resp = client.newCall(req).execute()) {
                    String body = resp.body() != null ? resp.body().string() : "";
                    if (!resp.isSuccessful()) {
                        throw new IOException("HTTP " + resp.code());
                    }
                    List<PravachanItem> items = DainikPravachanServer.parseTrackList(body, slug);
                    mainHandler.post(() -> {
                        if (!isAdded()) return;
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        if (topicFilter != PravachanTopicFilter.DAINIK) return;
                        showDainikTrackList(displayTitle, items);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "dainik list load url=" + url, e);
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    Toast.makeText(requireContext(), R.string.dainik_pravachan_load_error, Toast.LENGTH_LONG).show();
                });
            }
        }, "dainik-audio-json").start();
    }

    private void showDainikTrackList(@NonNull String displayTitle, @Nullable List<PravachanItem> items) {
        dainikShowingServerTracks = true;
        dainikServerTracksAll.clear();
        if (items != null) {
            dainikServerTracksAll.addAll(items);
        }
        if (dainikDetailHeader != null) {
            dainikDetailHeader.setVisibility(View.VISIBLE);
        }
        if (dainikDetailTitle != null) {
            dainikDetailTitle.setText(displayTitle);
        }
        if (getContext() == null || dainikRecycler == null) return;
        if (dainikTracksAdapter == null) {
            dainikTracksAdapter = new PravachanAdapter();
            dainikTracksAdapter.setListener(this);
            bindInlinePlayerToPravachanAdapter(dainikTracksAdapter);
        }
        int extraBottom = (int) (requireContext().getResources().getDisplayMetrics().density * 96f);
        dainikRecycler.setPadding(
                dainikRecycler.getPaddingLeft(),
                dainikRecycler.getPaddingTop(),
                dainikRecycler.getPaddingRight(),
                extraBottom);
        dainikRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        dainikRecycler.setAdapter(dainikTracksAdapter);
        dainikRecycler.setItemViewCacheSize(24);
        filterDainikServerTracks(currentQuery);
    }

    private void filterDainikServerTracks(@NonNull String query) {
        if (dainikTracksAdapter == null) return;
        String q = query.trim();
        List<PravachanItem> filtered = new ArrayList<>();
        if (q.isEmpty()) {
            filtered.addAll(dainikServerTracksAll);
        } else {
            String qLower = q.toLowerCase();
            for (PravachanItem p : dainikServerTracksAll) {
                if (p == null) continue;
                String t = p.title != null ? p.title : "";
                String searchable = BookTransliterator.getSearchableText(t);
                String sl = searchable != null ? searchable.toLowerCase() : "";
                if (t.toLowerCase().contains(qLower) || sl.contains(qLower) || SearchHelper.matches(t, q)) {
                    filtered.add(p);
                }
            }
        }
        dainikTracksAdapter.setItems(filtered);
        if (emptyView != null) {
            if (filtered.isEmpty()) {
                emptyView.setText(q.isEmpty()
                        ? getString(R.string.dainik_pravachan_empty_tracks)
                        : getString(R.string.pravachan_dainik_no_match));
                emptyView.setVisibility(View.VISIBLE);
            } else {
                emptyView.setVisibility(View.GONE);
            }
        }
    }

    private static boolean matchesTopicFilter(@NonNull PravachanItem p, @NonNull PravachanTopicFilter f) {
        if (f == PravachanTopicFilter.ALL) return true;
        String title = p.title != null ? p.title : "";
        String speaker = p.speaker != null ? p.speaker : "";
        String combined = title + " " + speaker;
        String searchable = BookTransliterator.getSearchableText(combined);
        String hay = (searchable != null ? searchable : combined).toLowerCase();
        String tLower = title.toLowerCase();
        switch (f) {
            case RAMAYAN:
                return tLower.contains("રામાયણ") || hay.contains("ramayan") || hay.contains("ramayana");
            case GEETA:
                return tLower.contains("ગીતા") || hay.contains("gita") || hay.contains("geeta")
                        || tLower.contains("ભગવદ્") || tLower.contains("ભગવદ");
            case SWADHYAY:
                return tLower.contains("સ્વાધ્યાય") || hay.contains("swadhyay");
            case DHARM:
                return tLower.contains("ધર્મ") || hay.contains("dharm");
            default:
                return true;
        }
    }

    @NonNull
    private List<PravachanItem> filterByTopic(@Nullable List<PravachanItem> src) {
        List<PravachanItem> empty = new ArrayList<>();
        if (src == null) return empty;
        if (topicFilter == PravachanTopicFilter.ALL || topicFilter == PravachanTopicFilter.DAINIK) {
            return new ArrayList<>(src);
        }
        List<PravachanItem> out = new ArrayList<>();
        for (PravachanItem p : src) {
            if (p != null && matchesTopicFilter(p, topicFilter)) {
                out.add(p);
            }
        }
        return out;
    }

    private void bindDainikCategories(@NonNull String query) {
        if (dainikAdapter == null) return;
        if (dainikShowingServerTracks) return;
        String q = query.trim().toLowerCase();
        List<DainikPravachanCategoryAdapter.Row> rows = new ArrayList<>();
        String[] titles = DainikPravachanCategories.TITLES;
        for (int i = 0; i < titles.length; i++) {
            String t = titles[i];
            if (!q.isEmpty()) {
                String searchable = BookTransliterator.getSearchableText(t);
                String sl = searchable != null ? searchable.toLowerCase() : "";
                String tl = t.toLowerCase();
                if (!tl.contains(q) && !sl.contains(q) && !SearchHelper.matches(t, query.trim())) {
                    continue;
                }
            }
            rows.add(new DainikPravachanCategoryAdapter.Row(t, DainikPravachanCategories.colorAt(i)));
        }
        dainikAdapter.setRows(rows);
        if (emptyView != null) {
            if (rows.isEmpty()) {
                emptyView.setText(q.isEmpty() ? defaultEmptyText : getString(R.string.pravachan_dainik_no_match));
                emptyView.setVisibility(View.VISIBLE);
            } else {
                emptyView.setVisibility(View.GONE);
            }
        }
    }

    private void applySearchFilter(@NonNull String query) {
        if (adapter == null) return;

        String q = query.trim();
        currentQuery = q;

        if (topicFilter == PravachanTopicFilter.DAINIK) {
            if (searchResultsTitle != null) {
                if (q.isEmpty()) {
                    searchResultsTitle.setVisibility(View.GONE);
                } else {
                    searchResultsTitle.setVisibility(View.VISIBLE);
                }
            }
            if (dainikShowingServerTracks) {
                filterDainikServerTracks(q);
                return;
            }
            bindDainikCategories(q);
            return;
        }

        if (emptyView != null) {
            emptyView.setText(defaultEmptyText);
        }

        List<PravachanItem> source = filterByTopic(allPravachans != null ? allPravachans : new ArrayList<>());

        List<PravachanItem> filtered = new ArrayList<>();
        if (!q.isEmpty()) {
            String qLower = q.toLowerCase();
            for (PravachanItem p : source) {
                if (p == null) continue;
                String title = p.title != null ? p.title : "";
                String speaker = p.speaker != null ? p.speaker : "";

                String hayTitle = title.toLowerCase();
                String haySpeaker = speaker.toLowerCase();

                // 1) Best: original + transliterated search text
                String combined = title + " " + speaker;
                String searchable = BookTransliterator.getSearchableText(combined);
                String searchableLower = searchable != null ? searchable.toLowerCase() : (combined != null ? combined.toLowerCase() : "");

                boolean matched = false;
                if (!searchableLower.isEmpty() && searchableLower.contains(qLower)) {
                    matched = true;
                } else if (hayTitle.contains(qLower) || haySpeaker.contains(qLower)) {
                    matched = true;
                } else if (SearchHelper.matches(title, q) || SearchHelper.matches(speaker, q)) {
                    matched = true;
                }

                if (matched) filtered.add(p);
            }
        } else {
            filtered.addAll(source);
        }

        adapter.setItems(filtered);
        if (emptyView != null) emptyView.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);

        // If expanded item is not in filtered list, collapse it.
        if (adapter != null && playingItemId != null && !q.isEmpty()) {
            boolean stillExists = false;
            for (PravachanItem p : filtered) {
                if (p != null && playingItemId.equals(p.id)) {
                    stillExists = true;
                    break;
                }
            }
            if (!stillExists) {
                adapter.setExpandedItemId(null);
            }
        }

        // "search kare to list upper aa jaye"
        if (recyclerView != null) {
            try {
                recyclerView.smoothScrollToPosition(0);
            } catch (Throwable ignore) {}
        }
    }

    @Override
    public void onPravachanClick(@NonNull PravachanItem item, int adapterPosition) {
        if (item == null || item.id == null || item.audioUrl == null) return;
        currentItem = item;
        currentItemId = item.id;

        if (adapter != null) adapter.setExpandedItemId(item.id);

        // Smooth scroll so user sees the expanded inline player.
        try {
            if (dainikShowingServerTracks && dainikRecycler != null) {
                dainikRecycler.smoothScrollToPosition(adapterPosition);
            } else if (recyclerView != null) {
                recyclerView.smoothScrollToPosition(adapterPosition);
            }
        } catch (Throwable ignore) {
        }

        boolean sameItem = playingItemId != null && playingItemId.equals(item.id);
        if (!sameItem || player == null) {
            playingItemId = item.id;
            final PravachanItem toPlay = item;
            // Defer until RecyclerView rebinds expanded row so inline SeekBar / buttons attach correctly
            Runnable doPlay = () -> {
                if (toPlay.id == null || currentItemId == null || !currentItemId.equals(toPlay.id)) return;
                startPlayback(toPlay);
            };
            if (recyclerView != null) {
                recyclerView.post(doPlay);
            } else {
                mainHandler.post(doPlay);
            }
        } else {
            // Toggle play/pause for the same item.
            boolean playing = player.getPlayWhenReady();
            player.setPlayWhenReady(!playing);
            updatePlayIcon();
            if (!playing) startSeekUpdates();
            else stopSeekUpdates();
        }
    }

    private void startPlayback(@NonNull PravachanItem item) {
        String raw = item.audioUrl != null ? item.audioUrl.trim() : "";
        if (raw.isEmpty()) {
            Toast.makeText(requireContext(), R.string.audio_play_error, Toast.LENGTH_LONG).show();
            return;
        }
        String encoded = encodeUrl(raw);
        playPravachanUrl(item, encoded, raw, true);
    }

    /**
     * @param allowRawFallback if true and this URL fails, retry once with raw Firestore URL (no path encoding)
     */
    private void playPravachanUrl(@NonNull PravachanItem item, String urlToPlay, String rawUrl, boolean allowRawFallback) {
        stopSeekUpdates();
        playingItemId = item.id;

        if (player != null) {
            try {
                player.release();
            } catch (Throwable ignore) {
            }
            player = null;
        }

        if (getContext() == null) return;
        Toast.makeText(requireContext(), R.string.audio_loading, Toast.LENGTH_SHORT).show();

        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/octet-stream,audio/*,*/*");

        DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(45_000)
                .setReadTimeoutMs(120_000)
                .setDefaultRequestProperties(headers);

        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(requireContext(), httpFactory);
        DefaultMediaSourceFactory mediaSourceFactory =
                new DefaultMediaSourceFactory(requireContext()).setDataSourceFactory(dataSourceFactory);

        player = new ExoPlayer.Builder(requireContext())
                .setMediaSourceFactory(mediaSourceFactory)
                .build();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    mainHandler.post(() -> {
                        updatePlayIcon();
                        startSeekUpdates();
                    });
                } else if (state == Player.STATE_ENDED) {
                    mainHandler.post(() -> {
                        stopSeekUpdates();
                        updatePlayIcon();
                    });
                }
            }

            @Override
            public void onPlayerError(@NonNull androidx.media3.common.PlaybackException error) {
                Log.e(TAG, "Playback failed tryUrl=" + urlToPlay
                        + " raw=" + rawUrl
                        + " allowFallback=" + allowRawFallback
                        + " err=" + (error != null ? error.getMessage() : "null")
                        + " cause=" + (error != null && error.getCause() != null ? error.getCause().getMessage() : "null"));
                if (allowRawFallback && rawUrl != null && !rawUrl.equals(urlToPlay)) {
                    mainHandler.post(() -> playPravachanUrl(item, rawUrl, rawUrl, false));
                    return;
                }
                mainHandler.post(() -> {
                    Toast.makeText(requireContext(), R.string.audio_play_error, Toast.LENGTH_LONG).show();
                    stopSeekUpdates();
                    updatePlayIcon();
                });
            }
        });

        MediaItem mediaItem = buildPravachanMediaItem(urlToPlay);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlayWhenReady(true);
        updatePlayIcon();
    }

    private static boolean isWavUrl(String url) {
        if (url == null) return false;
        int q = url.indexOf('?');
        String path = q >= 0 ? url.substring(0, q) : url;
        return path.toLowerCase().endsWith(".wav");
    }

    /** Hint MIME so ExoPlayer / servers that sniff badly still play MP3/M4A etc. */
    private static MediaItem buildPravachanMediaItem(String urlToPlay) {
        Uri uri = Uri.parse(urlToPlay);
        int q = urlToPlay.indexOf('?');
        String path = q >= 0 ? urlToPlay.substring(0, q) : urlToPlay;
        String lower = path.toLowerCase();
        MediaItem.Builder b = new MediaItem.Builder().setUri(uri);
        if (lower.endsWith(".wav")) b.setMimeType("audio/wav");
        else if (lower.endsWith(".mp3")) b.setMimeType("audio/mpeg");
        else if (lower.endsWith(".m4a") || lower.endsWith(".mp4")) b.setMimeType("audio/mp4");
        else if (lower.endsWith(".aac")) b.setMimeType("audio/aac");
        else if (lower.endsWith(".ogg") || lower.endsWith(".oga")) b.setMimeType("audio/ogg");
        else if (lower.endsWith(".opus")) b.setMimeType("audio/opus");
        return b.build();
    }

    private void startSeekUpdates() {
        stopSeekUpdates();
        if (player == null) return;
        if (inlineSeekBar == null && inlineTimeCurrent == null && inlineTimeTotal == null) return;

        seekRunnable = new Runnable() {
            @Override
            public void run() {
                if (player == null || inlineSeekBar == null || userSeeking) {
                    mainHandler.postDelayed(this, 500);
                    return;
                }
                long dur = player.getDuration();
                long pos = player.getCurrentPosition();
                if (dur > 0) {
                    int progress = (int) (1000.0 * pos / dur);
                    inlineSeekBar.setProgress(Math.min(progress, 1000));
                }
                if (inlineTimeCurrent != null) inlineTimeCurrent.setText(formatTime(pos));
                if (inlineTimeTotal != null) inlineTimeTotal.setText(formatTime(dur));
                mainHandler.postDelayed(this, 500);
            }
        };
        mainHandler.post(seekRunnable);
    }

    private void stopSeekUpdates() {
        if (seekRunnable != null) {
            mainHandler.removeCallbacks(seekRunnable);
            seekRunnable = null;
        }
    }

    private void updatePlayIcon() {
        if (inlinePlayBtn == null || player == null) return;
        boolean playing = player.getPlayWhenReady();
        inlinePlayBtn.setImageResource(playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
    }

    private static String formatTime(long ms) {
        if (ms < 0) ms = 0;
        long sec = ms / 1000;
        long min = sec / 60;
        sec = sec % 60;
        return String.format("%02d:%02d", min, sec);
    }

    /**
     * Encode only the last path segment for spaces; never touch ?query (signed URLs, tokens).
     */
    private static String encodeUrl(String url) {
        if (url == null || url.isEmpty()) return url;
        String query = "";
        String pathPart = url;
        int qi = url.indexOf('?');
        if (qi >= 0) {
            pathPart = url.substring(0, qi);
            query = url.substring(qi);
        }
        int lastSlash = pathPart.lastIndexOf('/');
        if (lastSlash < 0) return url;
        String base = pathPart.substring(0, lastSlash + 1);
        String filename = pathPart.substring(lastSlash + 1);
        if (filename.isEmpty()) return url;
        // Path already percent-encoded — keep whole URL
        if (java.util.regex.Pattern.compile("%[0-9A-Fa-f]{2}").matcher(filename).find()) {
            return url;
        }
        try {
            String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8.name()).replace("+", "%20");
            return base + encoded + query;
        } catch (Exception e) {
            return url;
        }
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
                // Make sure base isn't empty.
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

    @Override
    public void onPause() {
        super.onPause();
        stopSeekUpdates();
        if (player != null) {
            try {
                wasPlayingBeforePause = player.getPlayWhenReady();
                lastPositionMs = player.getCurrentPosition();
            } catch (Throwable ignore) {
            }
            try {
                player.setPlayWhenReady(false);
                player.stop();
            } catch (Throwable ignore) {
            }
            try {
                player.release();
            } catch (Throwable ignore) {
            }
            player = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Agar user wapas Audio Pravachan aaye aur pehle play chal raha tha,
        // to inline player ko same item me show karke dobara start kar do.
        if (adapter != null && currentItem != null && currentItemId != null) {
            adapter.setExpandedItemId(currentItemId);
            if (wasPlayingBeforePause) startPlayback(currentItem);
        }
    }

    @Override
    public void onDestroyView() {
        stopSeekUpdates();
        if (player != null) {
            try {
                player.release();
            } catch (Throwable ignore) {
            }
            player = null;
        }
        mainHandler.removeCallbacks(ensureBackingBufferRunnable);
        super.onDestroyView();
    }
}

