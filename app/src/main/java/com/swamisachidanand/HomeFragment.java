package com.swamisachidanand;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import java.io.File;
import java.io.FileOutputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputEditText;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment implements BookAdapter.OnBookClickListener {

    private static final String TAG = "HomeFragment";
    private static final String PREFS_NAME = "reading_progress";
    private static final String KEY_RECENT_BOOK = "recent_book_name";
    private static final String KEY_RECENT_BOOKS = "recent_books_list";
    private static final int REQUEST_CODE_VOICE_SEARCH = 1001;

    private RecyclerView photoViewPager;
    private RecyclerView recentBooksRecycler;
    private RecyclerView bestBooksRecycler;
    private RecyclerView bhaktiBooksRecycler;
    private RecyclerView yatraBooksRecycler;
    private RecyclerView updeshBooksRecycler;
    private RecyclerView jeevanBooksRecycler;
    private com.google.android.material.textfield.TextInputEditText searchInput;
    private ImageView clearSearch, micButton;
    private LinearLayout searchResultsSection;
    private RecyclerView searchResultsRecycler;
    private TextView searchNoResults;
    private BookAdapter searchResultsAdapter;
    private List<Book> allBooksForSearch = new ArrayList<>();

    private BookAdapter recentBooksAdapter;
    private BookAdapter bestBooksAdapter;
    private BookAdapter bhaktiBooksAdapter;
    private BookAdapter yatraBooksAdapter;
    private BookAdapter updeshBooksAdapter;
    private BookAdapter jeevanBooksAdapter;
    
    private Handler autoScrollHandler;
    private Runnable autoScrollRunnable;
    private PhotoCarouselAdapter photoCarouselAdapter;
    private boolean videoPlaybackInProgress;

    // Simple hero video at top (Padma Bhushan clip) via VideoView
    private VideoView heroVideoView;

    // Best books = first N from assets (no fixed list). Purani books hatao, nayi PDFs dalo assets me — wahi dikhengi.
    private static final int BEST_BOOKS_COUNT = 8;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = null;
        try {
            view = inflater.inflate(R.layout.fragment_home, container, false);
            
            photoViewPager = null; // Old carousel disabled
            heroVideoView = view.findViewById(R.id.hero_video_view);

            recentBooksRecycler = view.findViewById(R.id.recent_books_recycler);
            bestBooksRecycler = view.findViewById(R.id.best_books_recycler);
            bhaktiBooksRecycler = view.findViewById(R.id.bhakti_books_recycler);
            yatraBooksRecycler = view.findViewById(R.id.yatra_books_recycler);
            updeshBooksRecycler = view.findViewById(R.id.updesh_books_recycler);
            jeevanBooksRecycler = view.findViewById(R.id.jeevan_books_recycler);
            searchInput = view.findViewById(R.id.search_input);
            clearSearch = view.findViewById(R.id.clear_search);
            micButton = view.findViewById(R.id.mic_button);
            searchResultsSection = view.findViewById(R.id.search_results_section);
            searchResultsRecycler = view.findViewById(R.id.search_results_recycler);
            searchNoResults = view.findViewById(R.id.search_no_results);

            if (searchResultsRecycler != null && getContext() != null) {
                searchResultsRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
                searchResultsAdapter = new BookAdapter(new ArrayList<>(), this);
                searchResultsAdapter.setUseCompactLayout(true);
                searchResultsRecycler.setAdapter(searchResultsAdapter);
            }

            setupSearchBar();

            // Start hero video after view is laid out so VideoView has size
            if (view != null) {
                view.post(() -> {
                    if (isAdded() && heroVideoView != null) setupHeroVideo();
                });
                view.postDelayed(() -> {
                    if (!isAdded() || getContext() == null) return;
                    loadRecentBooks();
                    loadBestBooks();
                    loadCategoryBooks();
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

    /**
     * Hero video using simple VideoView + raw resource URI.
     * Auto-play, loop, muted.
     */
    private void setupHeroVideo() {
        if (heroVideoView == null || getContext() == null) return;
        try {
            String uriString = "android.resource://" + getContext().getPackageName() + "/" + R.raw.padma_bhushan_video;
            android.net.Uri uri = android.net.Uri.parse(uriString);
            heroVideoView.setVisibility(View.VISIBLE);
            heroVideoView.setVideoURI(uri);
            heroVideoView.setOnPreparedListener(mp -> {
                try {
                    mp.setLooping(true);
                    mp.setVolume(0f, 0f);
                } catch (Throwable ignored) {}
                try {
                    // Force at least one frame before start to avoid black screen on some devices
                    heroVideoView.seekTo(1);
                    heroVideoView.start();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_VOICE_SEARCH && resultCode == Activity.RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spoken = results.get(0);
                if (searchInput != null) searchInput.setText(spoken);
                if (searchResultsSection != null) searchResultsSection.setVisibility(View.VISIBLE);
                filterBooks(spoken);
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

    private void loadRecentBooks() {
        new Thread(() -> {
            try {
                android.app.Activity act = getActivity();
                if (act == null) return;
                List<Book> serverBooks = ServerBookLoader.load(act);
                SharedPreferences prefs = act.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String recentBooksList = prefs.getString(KEY_RECENT_BOOKS, "");
                List<Book> recentBooks = new ArrayList<>();
                if (!recentBooksList.isEmpty()) {
                    String[] recentNames = recentBooksList.split(",");
                    for (String name : recentNames) {
                        name = name.trim();
                        if (name.isEmpty()) continue;
                        for (Book b : serverBooks) {
                            if (name.equals(b.getFileName()) || name.equals(b.getName()) ||
                                name.equals(b.getFileName().replace(".pdf", "").replace(".PDF", ""))) {
                                recentBooks.add(b);
                                break;
                            }
                        }
                        if (recentBooks.size() >= 3) break;
                    }
                }

                android.app.Activity a = getActivity();
                if (a != null) {
                    a.runOnUiThread(() -> {
                        if (!isAdded() || getContext() == null || recentBooksRecycler == null) return;
                        if (recentBooks.isEmpty()) {
                            recentBooksRecycler.setVisibility(View.GONE);
                        } else {
                            recentBooksRecycler.setVisibility(View.VISIBLE);
                            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
                            recentBooksRecycler.setLayoutManager(layoutManager);
                            recentBooksAdapter = new BookAdapter(recentBooks, this);
                            recentBooksAdapter.setUseCompactLayout(true);
                            recentBooksRecycler.setAdapter(recentBooksAdapter);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading recent books", e);
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

    private void loadCategoryBooks() {
        new Thread(() -> {
            try {
                android.app.Activity act = getActivity();
                if (act == null) return;
                List<Book> allBooks = ServerBookLoader.load(act);

                // Separate books by category
                List<Book> bhaktiBooks = new ArrayList<>();
                List<Book> yatraBooks = new ArrayList<>();
                List<Book> updeshBooks = new ArrayList<>();
                List<Book> jeevanBooks = new ArrayList<>();
                
                for (Book book : allBooks) {
                    String category = book.getCategory();
                    if ("Bhakti".equals(category)) {
                        bhaktiBooks.add(book);
                    } else if ("Yatra".equals(category)) {
                        yatraBooks.add(book);
                    } else if ("Updesh".equals(category)) {
                        updeshBooks.add(book);
                    } else if ("Jeevan".equals(category)) {
                        jeevanBooks.add(book);
                    }
                }
                
                Collections.sort(bhaktiBooks, (b1, b2) -> safeCompare(b1.getName(), b2.getName()));
                Collections.sort(yatraBooks, (b1, b2) -> safeCompare(b1.getName(), b2.getName()));
                Collections.sort(updeshBooks, (b1, b2) -> safeCompare(b1.getName(), b2.getName()));
                Collections.sort(jeevanBooks, (b1, b2) -> safeCompare(b1.getName(), b2.getName()));

                allBooksForSearch = new ArrayList<>(allBooks);

                android.app.Activity a = getActivity();
                if (a == null) return;
                a.runOnUiThread(() -> {
                    if (!isAdded() || getContext() == null) return;
                    if (bhaktiBooksRecycler == null || yatraBooksRecycler == null || updeshBooksRecycler == null || jeevanBooksRecycler == null) return;
                    LinearLayoutManager bhaktiLM = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
                    bhaktiBooksRecycler.setLayoutManager(bhaktiLM);
                    bhaktiBooksAdapter = new BookAdapter(bhaktiBooks, this);
                    bhaktiBooksAdapter.setUseCompactLayout(true);
                    bhaktiBooksRecycler.setAdapter(bhaktiBooksAdapter);
                    
                    // Setup Yatra books
                    LinearLayoutManager yatraLM = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
                    yatraBooksRecycler.setLayoutManager(yatraLM);
                    yatraBooksAdapter = new BookAdapter(yatraBooks, this);
                    yatraBooksAdapter.setUseCompactLayout(true);
                    yatraBooksRecycler.setAdapter(yatraBooksAdapter);
                    
                    // Setup Updesh books
                    LinearLayoutManager updeshLM = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
                    updeshBooksRecycler.setLayoutManager(updeshLM);
                    updeshBooksAdapter = new BookAdapter(updeshBooks, this);
                    updeshBooksAdapter.setUseCompactLayout(true);
                    updeshBooksRecycler.setAdapter(updeshBooksAdapter);
                    
                    // Setup Jeevan books
                    LinearLayoutManager jeevanLM = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
                    jeevanBooksRecycler.setLayoutManager(jeevanLM);
                    jeevanBooksAdapter = new BookAdapter(jeevanBooks, this);
                    jeevanBooksAdapter.setUseCompactLayout(true);
                    jeevanBooksRecycler.setAdapter(jeevanBooksAdapter);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading category books", e);
            }
        }).start();
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
        if (isAdded() && getContext() != null && recentBooksRecycler != null) {
            loadRecentBooks();
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
        if (autoScrollHandler != null && autoScrollRunnable != null) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable);
        }
        if (heroVideoView != null) {
            try { heroVideoView.stopPlayback(); } catch (Throwable ignored) {}
        }
        photoViewPager = null;
        heroVideoView = null;
        recentBooksRecycler = null;
        bestBooksRecycler = null;
        bhaktiBooksRecycler = null;
        yatraBooksRecycler = null;
        updeshBooksRecycler = null;
        jeevanBooksRecycler = null;
        searchResultsRecycler = null;
    }
}

