package com.swamisachidanand;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.appbar.MaterialToolbar;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * ફોટા ગેલેરી – 100% સર્વરથી. સર્વર પર add/delete કરો, list.json અપડેટ કરો → એપમાં તરત (અથવા pull-to-refresh) દેખાશે.
 */
public class PhotoGalleryActivity extends AppCompatActivity {

    private static final String GALLERY_BASE = "https://daveashish12356-dotcom.github.io/swamisachidanand-audio/public/gallery/";
    private static final String LIST_URL = GALLERY_BASE + "list.json?v=2";

    private static List<String> galleryUrls = new ArrayList<>();

    /** Full-screen slideshow uses this list; only start index is passed via Intent. */
    public static List<String> getGalleryUrls() {
        return galleryUrls != null ? galleryUrls : new ArrayList<>();
    }

    // Slideshow interval – 6 seconds so next photo has time to load
    private static final int SLIDESHOW_INTERVAL_MS = 6000;

    private RecyclerView recycler;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private ViewPager2 slideshowPager;
    private PhotoGalleryAdapter adapter;
    private GallerySlideshowPagerAdapter slideshowAdapter;
    private final Handler slideshowHandler = new Handler(Looper.getMainLooper());
    private Runnable slideshowRunnable;

    // Slideshow order – દરેક ફોટો એકવાર દેખાડો, પછી જ ફરી
    private final List<Integer> slideshowOrder = new ArrayList<>();
    private int slideshowOrderIndex = 0;
    // જે ફોટા લોડ ન થાય (ટુટી URL વગેરે), તેમને અહીં યાદ રાખી ને પછીના ઓર્ડરમાં કાઢી નાખીએ
    private final java.util.Set<Integer> badSlides = new java.util.HashSet<>();

    // Persistent keys so that app બંધ/ખુલા પછી પણ ક્રમ ચાલુ રહે
    private static final String PREF_SLIDESHOW = "photo_gallery_slideshow";
    private static final String KEY_ORDER = "order";
    private static final String KEY_ORDER_INDEX = "order_index";
    private static final String KEY_LIST_HASH = "list_hash";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_gallery);

        MaterialToolbar toolbar = findViewById(R.id.photo_gallery_toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        swipeRefresh = findViewById(R.id.photo_gallery_swipe_refresh);
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(this::loadGalleryList);
        }
        progressBar = findViewById(R.id.photo_gallery_progress);
        slideshowPager = findViewById(R.id.photo_gallery_slideshow);
        if (slideshowPager != null) {
            slideshowPager.setVisibility(View.GONE);
            slideshowAdapter = new GallerySlideshowPagerAdapter();
            slideshowAdapter.setListener(new GallerySlideshowPagerAdapter.Listener() {
                @Override
                public void onSlideClick(int position) {
                    Intent i = new Intent(PhotoGalleryActivity.this, FullScreenSlideshowActivity.class);
                    i.putExtra(FullScreenSlideshowActivity.EXTRA_START_INDEX, position);
                    startActivity(i);
                }

                @Override
                public void onSlideLoadFailed(int position) {
                    // આ ફોટો વારંવાર error ન આપે માટે slideshowમાંથી કાઢી નાંખીએ
                    badSlides.add(position);
                    rebuildRandomOrder(slideshowAdapter.getItemCount());
                    if (!slideshowOrder.isEmpty()) {
                        slideshowOrderIndex = 0;
                    }
                    saveSlideshowState();
                }
            });
            slideshowPager.setAdapter(slideshowAdapter);
            slideshowPager.setOffscreenPageLimit(2);
            // ફોટા ઓલબમ જેવી એનિમેશન – સાઇડથી ફોટા આવે, સેન્ટર પર મોટો દેખાય, 3D ડેપ્થ
            final float density = getResources().getDisplayMetrics().density;
            slideshowPager.setPageTransformer(new ViewPager2.PageTransformer() {
                @Override
                public void transformPage(@NonNull View page, float position) {
                    float abs = Math.abs(position);
                    int w = page.getWidth();
                    int h = page.getHeight();
                    if (w <= 0) return;

                    // 3D પરસ્પેક્ટિવ દેખાય એ માટે કેમેરા દૂરી
                    page.setCameraDistance(density * 12000f);

                    page.setPivotX(w * 0.5f);
                    page.setPivotY(h * 0.5f);

                    // સાઇડથી સ્લાઇડ – ફોટા ઓલબમમાં જમણી/ડાબી બાજુથી આવે તેવું
                    page.setTranslationX(position * w * 0.42f);

                    // થોડો ઉપર-નીચે પણ – ફોટા સ્ટેક જેવો ફील
                    page.setTranslationY(-abs * 12f * density);

                    // 3D ઘૂમવાનો ઇફેક્ટ – ફોટા ફલેટ નહીં, ઘૂમીને આવે
                    page.setRotationY(position * -42f);

                    // સેન્ટર ફોટો મોટો, બાજુના નાના – ઓલબમ જેવું
                    float scale = 1f - 0.22f * abs;
                    page.setScaleX(scale);
                    page.setScaleY(scale);

                    // બાજુના ફોટા હળવા, સેન્ટર પૂરા ચમકતો
                    float alpha = position == 0f ? 1f : (1f - 0.55f * abs);
                    page.setAlpha(Math.max(0.45f, alpha));
                }
            });
        }
        recycler = findViewById(R.id.photo_gallery_recycler);
        if (recycler != null) {
            int pad = (int) (8 * getResources().getDisplayMetrics().density);
            int screenW = getResources().getDisplayMetrics().widthPixels - pad * 2;
            int columnWidth = screenW / 3;
            recycler.setLayoutManager(new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL));
            adapter = new PhotoGalleryAdapter();
            adapter.setColumnWidthPx(columnWidth);
            adapter.setOnPhotoClickListener(position -> {
                Intent i = new Intent(PhotoGalleryActivity.this, FullScreenSlideshowActivity.class);
                i.putExtra(FullScreenSlideshowActivity.EXTRA_START_INDEX, position);
                startActivity(i);
            });
            recycler.setAdapter(adapter);
        }

        loadGalleryList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopSlideshowAutoAdvance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startSlideshowAutoAdvance();
    }

    private void startSlideshowAutoAdvance() {
        stopSlideshowAutoAdvance();
        if (slideshowPager == null || slideshowAdapter == null || slideshowAdapter.getItemCount() == 0) return;
        slideshowRunnable = new Runnable() {
            @Override
            public void run() {
                if (slideshowPager == null || slideshowAdapter == null) return;
                int total = slideshowOrder.size();
                if (total == 0) return;

                // એક ફોટો એકવાર – પછી જ ફરી
                slideshowOrderIndex++;
                if (slideshowOrderIndex >= total) {
                    // બધા ફોટા દેખાઈ ગયા – નવો random order બનાવો
                    rebuildRandomOrder(slideshowAdapter.getItemCount());
                    slideshowOrderIndex = 0;
                }
                int nextPos = slideshowOrder.get(slideshowOrderIndex);
                if (nextPos < 0 || nextPos >= slideshowAdapter.getItemCount()) {
                    rebuildRandomOrder(slideshowAdapter.getItemCount());
                    slideshowOrderIndex = 0;
                    nextPos = slideshowOrder.isEmpty() ? 0 : slideshowOrder.get(0);
                }
                // Jab tak अगला फोटो load न हो, तब तक अगला slide पर मत जाओ
                if (!slideshowAdapter.isLoaded(nextPos)) {
                    slideshowHandler.postDelayed(this, 500);
                    return;
                }

                slideshowPager.setCurrentItem(nextPos, true);
                saveSlideshowState();
                slideshowHandler.postDelayed(this, SLIDESHOW_INTERVAL_MS);
            }
        };
        slideshowHandler.postDelayed(slideshowRunnable, SLIDESHOW_INTERVAL_MS);
    }

    private void stopSlideshowAutoAdvance() {
        if (slideshowRunnable != null) {
            slideshowHandler.removeCallbacks(slideshowRunnable);
            slideshowRunnable = null;
        }
    }

    private void loadGalleryList() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (recycler != null) recycler.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build();
                Request req = new Request.Builder()
                    .url(LIST_URL)
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    if (resp.isSuccessful() && resp.body() != null) {
                        String json = resp.body().string();
                        if (json != null && json.startsWith("\uFEFF")) json = json.substring(1);
                        JSONArray arr = new JSONArray(json);
                        List<String> urls = new ArrayList<>();
                        for (int i = 0; i < arr.length(); i++) {
                            String name = arr.optString(i, "").trim();
                            if (!name.isEmpty()) {
                                urls.add(GALLERY_BASE + name);
                            }
                        }
                        runOnUiThread(() -> showPhotos(urls));
                        return;
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
            runOnUiThread(this::onLoadFailed);
        }).start();
    }

    private void onLoadFailed() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (recycler != null) recycler.setVisibility(View.VISIBLE);
        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
        Toast.makeText(this, "ફોટા લોડ થયા નથી. ઇન્ટરનેટ ચેક કરો અથવા નીચે ખેંચીને ફરી ચાલુ કરો.", Toast.LENGTH_LONG).show();
    }

    private void showPhotos(List<String> urls) {
        galleryUrls = urls != null ? new ArrayList<>(urls) : new ArrayList<>();
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (recycler != null) recycler.setVisibility(View.VISIBLE);
        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
        if (adapter != null) adapter.setImageUrls(urls);
        if (slideshowPager != null && slideshowAdapter != null) {
            slideshowAdapter.setUrls(urls);
            if (urls == null || urls.isEmpty()) {
                slideshowPager.setVisibility(View.GONE);
                stopSlideshowAutoAdvance();
                slideshowOrder.clear();
                slideshowOrderIndex = 0;
                badSlides.clear();
                saveSlideshowState();
            } else {
                slideshowPager.setVisibility(View.VISIBLE);

                // નવી લિસ્ટ માટે hash કાઢો – બદલાઈ ગઈ હોય તો નવો ક્રમ
                int count = urls.size();
                int hash = computeListHash(urls);
                SharedPreferences prefs = getSharedPreferences(PREF_SLIDESHOW, MODE_PRIVATE);
                int oldHash = prefs.getInt(KEY_LIST_HASH, 0);

                if (oldHash == hash) {
                    // પહેલા જે order/store હતું તે જ use કરો (app બંધ/ખુલા પછી પણ)
                    restoreSlideshowState(count);
                } else {
                    // નવા ફોટા અથવા અલગ લિસ્ટ – નવો random order, અને જૂના badSlides રીસેટ
                    badSlides.clear();
                    rebuildRandomOrder(count);
                    slideshowOrderIndex = 0;
                }

                if (slideshowOrder.isEmpty()) {
                    rebuildRandomOrder(count);
                    slideshowOrderIndex = 0;
                }

                int startPos = slideshowOrder.get(Math.max(0, Math.min(slideshowOrderIndex, slideshowOrder.size() - 1)));
                slideshowPager.setCurrentItem(startPos, false);
                saveSlideshowState();
                startSlideshowAutoAdvance();
            }
        }
    }

    private int computeListHash(List<String> urls) {
        int h = 1;
        if (urls != null) {
            for (String u : urls) {
                if (u != null) {
                    h = 31 * h + u.hashCode();
                }
            }
        }
        return h;
    }

    private void rebuildRandomOrder(int count) {
        slideshowOrder.clear();
        if (count <= 0) return;
        for (int i = 0; i < count; i++) {
            if (!badSlides.contains(i)) {
                slideshowOrder.add(i);
            }
        }
        java.util.Collections.shuffle(slideshowOrder);
    }

    private void saveSlideshowState() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREF_SLIDESHOW, MODE_PRIVATE);
            SharedPreferences.Editor e = prefs.edit();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < slideshowOrder.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(slideshowOrder.get(i));
            }
            e.putString(KEY_ORDER, sb.toString());
            e.putInt(KEY_ORDER_INDEX, slideshowOrderIndex);
            e.putInt(KEY_LIST_HASH, computeListHash(galleryUrls));
            e.apply();
        } catch (Throwable ignore) {
            // જો કંઈક persist ના થાય તો પણ slideshow ચાલે
        }
    }

    private void restoreSlideshowState(int expectedCount) {
        slideshowOrder.clear();
        slideshowOrderIndex = 0;
        try {
            SharedPreferences prefs = getSharedPreferences(PREF_SLIDESHOW, MODE_PRIVATE);
            String orderStr = prefs.getString(KEY_ORDER, "");
            int idx = prefs.getInt(KEY_ORDER_INDEX, 0);
            if (orderStr != null && !orderStr.isEmpty()) {
                String[] parts = orderStr.split(",");
                for (String p : parts) {
                    try {
                        int v = Integer.parseInt(p.trim());
                        if (v >= 0 && v < expectedCount) {
                            slideshowOrder.add(v);
                        }
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
            if (slideshowOrder.size() != expectedCount) {
                // લિસ્ટનું સાઇઝ બદલાઈ ગયું – નવો ક્રમ
                rebuildRandomOrder(expectedCount);
                slideshowOrderIndex = 0;
            } else {
                slideshowOrderIndex = Math.max(0, Math.min(idx, slideshowOrder.size() - 1));
            }
        } catch (Throwable ignore) {
            rebuildRandomOrder(expectedCount);
            slideshowOrderIndex = 0;
        }
    }
}
