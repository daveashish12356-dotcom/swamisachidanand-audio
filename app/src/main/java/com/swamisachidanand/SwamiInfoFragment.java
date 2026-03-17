package com.swamisachidanand;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * In-app profile page for Swami Sachchidanand.
 * Top photo: 10 sec static, then server gallery slideshow.
 */
public class SwamiInfoFragment extends Fragment {

    private static final String TAG = "SwamiInfoFragment";
    private static final String SWAMI_GALLERY_BASE = "https://daveashish12356-dotcom.github.io/swamisachidanand-audio/public/gallery/";
    private static final String SWAMI_GALLERY_LIST_URL = SWAMI_GALLERY_BASE + "list.json";
    private static final long FIRST_PHOTO_STATIC_MS = 10_000L;
    // Swami info photo slideshow interval – 6 seconds between slides
    private static final int SLIDESHOW_INTERVAL_MS = 6000;
    private static final String PREF_SWAMI_SLIDESHOW = "swami_info_slideshow";
    private static final String KEY_ORDER = "order";
    private static final String KEY_INDEX = "idx";
    private static final String KEY_HASH = "hash";

    private ImageView swamiInfoFirstPhoto;
    private ViewPager2 swamiInfoPhotoSlideshow;
    private View swamiInfoSlideshowCard;
    private GallerySlideshowPagerAdapter swamiInfoSlideshowAdapter;
    private final List<String> swamiInfoPhotoUrls = new ArrayList<>();
    private final List<Integer> slideshowOrder = new ArrayList<>();
    private int slideshowOrderIndex = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable firstPhotoDoneRunnable;
    private Runnable slideshowAdvanceRunnable;

    public SwamiInfoFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_swami_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swamiInfoFirstPhoto = view.findViewById(R.id.swami_info_first_photo);
        swamiInfoPhotoSlideshow = view.findViewById(R.id.swami_info_photo_slideshow);
        swamiInfoSlideshowCard = view.findViewById(R.id.swami_info_slideshow_card);
        setupSlideshow(view);
        View viewMorePhotos = view.findViewById(R.id.swami_view_more_photos);
        if (viewMorePhotos != null) {
            viewMorePhotos.setOnClickListener(v -> {
                if (getContext() != null) {
                    startActivity(new Intent(getContext(), PhotoGalleryActivity.class));
                }
            });
        }
        bindBookOrderSection(view);
    }

    private void setupSlideshow(View root) {
        if (swamiInfoPhotoSlideshow == null) return;
        swamiInfoSlideshowAdapter = new GallerySlideshowPagerAdapter();
        swamiInfoSlideshowAdapter.setListener(new GallerySlideshowPagerAdapter.Listener() {
            @Override
            public void onSlideClick(int position) {
                if (getContext() != null) {
                    startActivity(new Intent(getContext(), PhotoGalleryActivity.class));
                }
            }
            @Override
            public void onSlideLoadFailed(int position) { }
        });
        swamiInfoPhotoSlideshow.setAdapter(swamiInfoSlideshowAdapter);
        swamiInfoPhotoSlideshow.setOffscreenPageLimit(1);
        // Swami info page – thodi depth + zoom animation (gallery se thodi अलग)
        final float density = getResources().getDisplayMetrics().density;
        swamiInfoPhotoSlideshow.setPageTransformer(new ViewPager2.PageTransformer() {
            @Override
            public void transformPage(@NonNull View page, float position) {
                float abs = Math.abs(position);
                int w = page.getWidth();
                if (w <= 0) return;

                page.setCameraDistance(density * 10000f);
                page.setPivotX(w * 0.5f);
                page.setPivotY(page.getHeight() * 0.5f);

                // halka vertical lift
                page.setTranslationY(-abs * 6f * density);
                page.setTranslationX(position * w * 0.28f);

                float scale = 1f - 0.2f * abs;
                page.setScaleX(scale);
                page.setScaleY(scale);

                float alpha = 1f - 0.45f * abs;
                page.setAlpha(Math.max(0.4f, alpha));
            }
        });
        loadGalleryUrls();
        firstPhotoDoneRunnable = () -> {
            if (!isAdded() || swamiInfoFirstPhoto == null) return;
            swamiInfoFirstPhoto.setVisibility(View.GONE);
            if (swamiInfoSlideshowCard != null) swamiInfoSlideshowCard.setVisibility(View.VISIBLE);
            if (swamiInfoPhotoUrls.isEmpty() || swamiInfoSlideshowAdapter == null) return;
            swamiInfoSlideshowAdapter.setUrls(new ArrayList<>(swamiInfoPhotoUrls));
            int n = swamiInfoPhotoUrls.size();
            int hash = listHash(swamiInfoPhotoUrls);
            if (getContext() != null) {
                SharedPreferences prefs = getContext().getSharedPreferences(PREF_SWAMI_SLIDESHOW, Context.MODE_PRIVATE);
                if (prefs.getInt(KEY_HASH, 0) == hash) restoreOrder(prefs, n);
                else { rebuildOrder(n); slideshowOrderIndex = 0; }
                if (slideshowOrder.isEmpty()) rebuildOrder(n);
                slideshowOrderIndex = Math.min(Math.max(0, slideshowOrderIndex), Math.max(0, slideshowOrder.size() - 1));
            } else rebuildOrder(n);
            if (swamiInfoPhotoSlideshow != null && !slideshowOrder.isEmpty()) {
                int pos = slideshowOrder.get(slideshowOrderIndex);
                swamiInfoPhotoSlideshow.setCurrentItem(pos, false);
                saveState(hash);
                startSlideshowAdvance();
            }
        };
        handler.postDelayed(firstPhotoDoneRunnable, FIRST_PHOTO_STATIC_MS);
    }

    private void loadGalleryUrls() {
        Activity activity = getActivity();
        if (activity == null) return;
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();
                try (Response resp = client.newCall(new Request.Builder().url(SWAMI_GALLERY_LIST_URL).build()).execute()) {
                    if (resp.isSuccessful() && resp.body() != null) {
                        String json = resp.body().string();
                        if (json != null && json.startsWith("\uFEFF")) json = json.substring(1);
                        JSONArray arr = new JSONArray(json);
                        synchronized (swamiInfoPhotoUrls) {
                            swamiInfoPhotoUrls.clear();
                            for (int i = 0; i < arr.length(); i++) {
                                String name = arr.optString(i, "").trim();
                                if (!name.isEmpty()) swamiInfoPhotoUrls.add(SWAMI_GALLERY_BASE + name);
                            }
                        }
                        Log.d(TAG, "Swami info gallery loaded: " + swamiInfoPhotoUrls.size());
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "loadGalleryUrls", t);
            }
        }).start();
    }

    private void startSlideshowAdvance() {
        if (slideshowAdvanceRunnable != null) handler.removeCallbacks(slideshowAdvanceRunnable);
        if (swamiInfoPhotoSlideshow == null || swamiInfoSlideshowAdapter == null || slideshowOrder.isEmpty()) return;
        slideshowAdvanceRunnable = new Runnable() {
            @Override
            public void run() {
                if (swamiInfoPhotoSlideshow == null || swamiInfoSlideshowAdapter == null || !isAdded()) return;
                int total = slideshowOrder.size();
                if (total == 0) return;
                slideshowOrderIndex++;
                if (slideshowOrderIndex >= total) {
                    rebuildOrder(swamiInfoSlideshowAdapter.getItemCount());
                    slideshowOrderIndex = 0;
                }
                int nextPos = slideshowOrder.get(slideshowOrderIndex);
                if (nextPos < 0 || nextPos >= swamiInfoSlideshowAdapter.getItemCount()) {
                    rebuildOrder(swamiInfoSlideshowAdapter.getItemCount());
                    slideshowOrderIndex = 0;
                    nextPos = slideshowOrder.isEmpty() ? 0 : slideshowOrder.get(0);
                }
                // अगला photo jab tak ready na ho, tab tak yahi photo दिखाते रहो
                if (!swamiInfoSlideshowAdapter.isLoaded(nextPos)) {
                    handler.postDelayed(this, 500);
                    return;
                }
                swamiInfoPhotoSlideshow.setCurrentItem(nextPos, true);
                if (getContext() != null) saveState(listHash(swamiInfoPhotoUrls));
                handler.postDelayed(this, SLIDESHOW_INTERVAL_MS);
            }
        };
        handler.postDelayed(slideshowAdvanceRunnable, SLIDESHOW_INTERVAL_MS);
    }

    private int listHash(List<String> urls) {
        int h = 1;
        if (urls != null) for (String u : urls) h = 31 * h + (u != null ? u.hashCode() : 0);
        return h;
    }

    private void rebuildOrder(int count) {
        slideshowOrder.clear();
        for (int i = 0; i < count; i++) slideshowOrder.add(i);
        Collections.shuffle(slideshowOrder);
    }

    private void restoreOrder(SharedPreferences prefs, int count) {
        slideshowOrder.clear();
        String s = prefs.getString(KEY_ORDER, "");
        if (s != null && !s.isEmpty()) {
            for (String p : s.split(",")) {
                try {
                    int v = Integer.parseInt(p.trim());
                    if (v >= 0 && v < count) slideshowOrder.add(v);
                } catch (NumberFormatException ignored) { }
            }
        }
        if (slideshowOrder.size() != count) { rebuildOrder(count); slideshowOrderIndex = 0; return; }
        slideshowOrderIndex = Math.max(0, Math.min(prefs.getInt(KEY_INDEX, 0), count - 1));
    }

    private void saveState(int hash) {
        try {
            SharedPreferences prefs = getContext().getSharedPreferences(PREF_SWAMI_SLIDESHOW, Context.MODE_PRIVATE);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < slideshowOrder.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(slideshowOrder.get(i));
            }
            prefs.edit().putString(KEY_ORDER, sb.toString()).putInt(KEY_INDEX, slideshowOrderIndex).putInt(KEY_HASH, hash).apply();
        } catch (Throwable ignored) { }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (firstPhotoDoneRunnable != null) handler.removeCallbacks(firstPhotoDoneRunnable);
        if (slideshowAdvanceRunnable != null) handler.removeCallbacks(slideshowAdvanceRunnable);
        swamiInfoFirstPhoto = null;
        swamiInfoPhotoSlideshow = null;
        swamiInfoSlideshowCard = null;
        swamiInfoSlideshowAdapter = null;
    }

    private void bindBookOrderSection(View root) {
        View storeLink = root.findViewById(R.id.book_order_store_link);
        if (storeLink != null) {
            storeLink.setOnClickListener(v -> {
                try {
                    if (getContext() != null) {
                        startActivity(new Intent(getContext(), BookStoreActivity.class));
                    }
                } catch (Exception ignored) { }
            });
        }
    }
}

