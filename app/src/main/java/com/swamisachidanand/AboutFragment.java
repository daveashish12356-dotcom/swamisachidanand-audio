package com.swamisachidanand;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
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

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;

public class AboutFragment extends Fragment {

    private static final String TAG = "AboutFragment";
    private static final String SAMPARK_GALLERY_BASE = "https://daveashish12356-dotcom.github.io/swamisachidanand-audio/public/gallery/";
    private static final String SAMPARK_GALLERY_LIST_URL = SAMPARK_GALLERY_BASE + "list.json?v=2";
    private static final long SAMPARK_FIRST_PHOTO_MS = 10_000L;
    // Sampark photo slideshow – 6 seconds per photo
    private static final int SAMPARK_SLIDESHOW_INTERVAL_MS = 6000;

    private AdView bottomBannerAd;
    private ImageView samparkPhoto;
    private View samparkFirstPhotoCard;
    private ViewPager2 samparkSlideshow;
    private View samparkSlideshowCard;
    private GallerySlideshowPagerAdapter samparkSlideshowAdapter;
    private final List<String> samparkPhotoUrls = new ArrayList<>();
    private final List<Integer> samparkOrder = new ArrayList<>();
    private int samparkOrderIndex = 0;
    private final Handler samparkHandler = new Handler(Looper.getMainLooper());
    private Runnable samparkFirstDoneRunnable;
    private Runnable samparkAdvanceRunnable;

    public AboutFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = null;
        try {
            view = inflater.inflate(R.layout.fragment_about, container, false);
            if (view == null) return container != null ? new View(container.getContext()) : null;
        } catch (Throwable t) {
            Log.e(TAG, "onCreateView inflate", t);
            return container != null ? new View(container.getContext()) : null;
        }

        // પુસ્તક ઓર્ડર – Call, WhatsApp, Book Store link
        bindBookOrderSection(view);

        samparkPhoto = view.findViewById(R.id.sampark_photo);
        samparkFirstPhotoCard = view.findViewById(R.id.sampark_first_photo_card);
        samparkSlideshow = view.findViewById(R.id.sampark_photo_slideshow);
        samparkSlideshowCard = view.findViewById(R.id.sampark_slideshow_card);
        setupSamparkSlideshow(view);

        // Sampark: YouTube, Facebook, WhatsApp, Telegram – website (swamisachchidanandji.org) links
        bindSocialLink(view, R.id.sampark_youtube, getString(R.string.url_youtube));
        bindSocialLink(view, R.id.sampark_facebook, getString(R.string.url_facebook));
        bindSocialLink(view, R.id.sampark_whatsapp, getString(R.string.url_whatsapp));
        bindSocialLink(view, R.id.sampark_telegram, getString(R.string.url_telegram));
        bindSocialLink(view, R.id.sampark_instagram, getString(R.string.url_instagram));

        View galleryLink = view.findViewById(R.id.sampark_photo_gallery_link);
        if (galleryLink != null) {
            galleryLink.setOnClickListener(v -> {
                if (getContext() != null) startActivity(new Intent(getContext(), PhotoGalleryActivity.class));
            });
        }

        View tourBtn = view.findViewById(R.id.about_app_tour_btn);
        if (tourBtn != null) {
            tourBtn.setOnClickListener(v -> {
                try {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).launchInteractiveTour();
                    } else if (getContext() != null) {
                        Intent i = new Intent(getContext(), MainActivity.class);
                        i.putExtra(MainActivity.EXTRA_INTERACTIVE_TOUR, true);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(i);
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "open interactive tour", t);
                }
            });
        }

        // Hidden suvichar admin page – 10 taps on top photo
        if (samparkPhoto != null) {
            ImageView photo = samparkPhoto;
            final int[] tapCount = {0};
            final long[] lastTapTime = {0L};
            photo.setOnClickListener(v -> {
                long now = System.currentTimeMillis();
                if (now - lastTapTime[0] > 4000) {
                    tapCount[0] = 0;
                }
                lastTapTime[0] = now;
                tapCount[0]++;
                if (tapCount[0] >= 10) {
                    tapCount[0] = 0;
                    try {
                        if (getContext() != null) {
                            Intent intent = new Intent(getContext(), SuvicharAdminActivity.class);
                            startActivity(intent);
                        }
                    } catch (Throwable t) {
                        Log.e(TAG, "open SuvicharAdminActivity", t);
                    }
                }
            });
        }

        // Ashram cards - bind each directly
        bindAshram(view, R.id.ashram1, "શ્રી ભક્તિ નિકેતન આશ્રમ, દંતાલી",
                "પ.પૂ.મહર્ષિ સ્વામી શ્રીસચ્ચિદાનંદજી પરમહંસ (પદ્મભૂષણશ્રી)",
                "પેટલાદ જી. આણંદ, ગુજરાત – ૩૮૮૪૫૦",
                "9428013551, 9824112625", "swamisachchidanandji.org",
                "https://maps.app.goo.gl/NEmwiYD8SiUvcMs49", R.drawable.bhakti_niketan_ashram);

        bindAshram(view, R.id.ashram2, "સાધનાશ્રમ, કોબા",
                "સચ્ચિદાનંદ સેવા સમાજ ટ્રસ્ટ, કોબા",
                "કોબા હાઇવે, કોબા, જી. ગાંધીનગર, ગુજરાત – ૩૮૨૪૨૬",
                "8238142042", "swamisachchidanandji.org",
                "https://maps.app.goo.gl/hF8P8aTpqA1mhK9C9", R.drawable.sadhana_ashram);

        bindAshram(view, R.id.ashram3, "વૃધ્ધાશ્રમ, ઊંઝા",
                "શ્રી સચ્ચિદાનંદ સેવા સમાજ ટ્રસ્ટ ઊંઝા",
                "ઊંઝા, જી. મહેસાણા, ગુજરાત – ૩૮૪૧૭૦",
                "9879104099", "swamisachchidanandji.org",
                "https://maps.app.goo.gl/6UFWKWH9DfVVBFhA8", R.drawable.vridhashram);

        bindAshram(view, R.id.ashram4, "સુઈગામ",
                "સ્વામી શ્રી સચ્ચિદાનંદજી સેવા સમાજ ટ્રસ્ટ, સુઈગામ",
                "સુઈગામ, જી. બનાસકાંઠા, ગુજરાત – ૩૮૫૫૭૦",
                "", "swamisachchidanandji.org",
                "https://maps.app.goo.gl/mLbBT1F5i2vjSMPCA", 0);

        View scrollView = view.findViewById(R.id.about_scroll_view);
        if (scrollView != null && getActivity() instanceof MainActivity) {
            scrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).onScrolled(scrollY - oldScrollY);
            });
        }

        return view;
    }

    private void setupSamparkSlideshow(View root) {
        if (samparkSlideshow == null) return;
        samparkSlideshowAdapter = new GallerySlideshowPagerAdapter();
        samparkSlideshowAdapter.setListener(new GallerySlideshowPagerAdapter.Listener() {
            @Override
            public void onSlideClick(int position) {
                if (getContext() != null) startActivity(new Intent(getContext(), PhotoGalleryActivity.class));
            }
            @Override
            public void onSlideLoadFailed(int position) { }
        });
        samparkSlideshow.setAdapter(samparkSlideshowAdapter);
        samparkSlideshow.setOffscreenPageLimit(1);
        // Sampark page ke liye हल्की depth + slide animation
        final float density = getResources().getDisplayMetrics().density;
        samparkSlideshow.setPageTransformer(new ViewPager2.PageTransformer() {
            @Override
            public void transformPage(@NonNull View page, float position) {
                float abs = Math.abs(position);
                int w = page.getWidth();
                if (w <= 0) return;

                page.setCameraDistance(density * 9000f);
                page.setPivotX(w * 0.5f);
                page.setPivotY(page.getHeight() * 0.5f);

                // थोड़ा ज्यादा side slide
                page.setTranslationX(position * w * 0.3f);

                // halki depth rotation
                page.setRotationY(position * -28f);

                float scale = 1f - 0.18f * abs;
                page.setScaleX(scale);
                page.setScaleY(scale);

                float alpha = 1f - 0.5f * abs;
                page.setAlpha(Math.max(0.35f, alpha));
            }
        });
        loadSamparkGalleryUrls();
        samparkFirstDoneRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded()) return;
                synchronized (samparkPhotoUrls) {
                    if (samparkPhotoUrls.isEmpty()) {
                        samparkHandler.postDelayed(this, 2000);
                        return;
                    }
                }
                if (samparkFirstPhotoCard != null) samparkFirstPhotoCard.setVisibility(View.GONE);
                if (samparkSlideshowCard != null) samparkSlideshowCard.setVisibility(View.VISIBLE);
                if (samparkSlideshowAdapter == null) return;
                List<String> copy;
                synchronized (samparkPhotoUrls) { copy = new ArrayList<>(samparkPhotoUrls); }
                samparkSlideshowAdapter.setUrls(copy);
                if (samparkOrder.size() != copy.size()) {
                    samparkOrder.clear();
                    for (int i = 0; i < copy.size(); i++) samparkOrder.add(i);
                    Collections.shuffle(samparkOrder);
                    samparkOrderIndex = 0;
                }
                if (samparkSlideshow != null && !samparkOrder.isEmpty()) {
                    int pos = samparkOrder.get(samparkOrderIndex);
                    samparkSlideshow.setCurrentItem(pos, false);
                    startSamparkAdvance();
                }
            }
        };
        samparkHandler.postDelayed(samparkFirstDoneRunnable, SAMPARK_FIRST_PHOTO_MS);
    }

    private void loadSamparkGalleryUrls() {
        Activity activity = getActivity();
        if (activity == null) return;
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();
                try (Response resp = client.newCall(new Request.Builder().url(SAMPARK_GALLERY_LIST_URL).build()).execute()) {
                    if (resp.isSuccessful() && resp.body() != null) {
                        String json = resp.body().string();
                        if (json != null && json.startsWith("\uFEFF")) json = json.substring(1);
                        JSONArray arr = new JSONArray(json);
                        synchronized (samparkPhotoUrls) {
                            samparkPhotoUrls.clear();
                            for (int i = 0; i < arr.length(); i++) {
                                String name = arr.optString(i, "").trim();
                                if (!name.isEmpty()) samparkPhotoUrls.add(SAMPARK_GALLERY_BASE + name);
                            }
                        }
                    }
                }
            } catch (Throwable t) { Log.e(TAG, "loadSamparkGalleryUrls", t); }
        }).start();
    }

    private void startSamparkAdvance() {
        if (samparkAdvanceRunnable != null) samparkHandler.removeCallbacks(samparkAdvanceRunnable);
        if (samparkSlideshow == null || samparkSlideshowAdapter == null || samparkOrder.isEmpty()) return;
        samparkAdvanceRunnable = new Runnable() {
            @Override
            public void run() {
                if (samparkSlideshow == null || samparkSlideshowAdapter == null || !isAdded()) return;
                int total = samparkOrder.size();
                if (total == 0) return;
                samparkOrderIndex++;
                if (samparkOrderIndex >= total) {
                    samparkOrder.clear();
                    for (int i = 0; i < samparkSlideshowAdapter.getItemCount(); i++) samparkOrder.add(i);
                    Collections.shuffle(samparkOrder);
                    samparkOrderIndex = 0;
                }
                int nextPos = samparkOrder.get(samparkOrderIndex);
                if (nextPos < 0 || nextPos >= samparkSlideshowAdapter.getItemCount()) {
                    samparkOrderIndex = 0;
                    nextPos = samparkOrder.isEmpty() ? 0 : samparkOrder.get(0);
                }
                // अगला Sampark photo jab tak ready na ho, tab tak slide आगे मत बढ़ाओ
                if (!samparkSlideshowAdapter.isLoaded(nextPos)) {
                    samparkHandler.postDelayed(this, 500);
                    return;
                }
                samparkSlideshow.setCurrentItem(nextPos, true);
                samparkHandler.postDelayed(this, SAMPARK_SLIDESHOW_INTERVAL_MS);
            }
        };
        samparkHandler.postDelayed(samparkAdvanceRunnable, SAMPARK_SLIDESHOW_INTERVAL_MS);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (samparkFirstDoneRunnable != null) samparkHandler.removeCallbacks(samparkFirstDoneRunnable);
        if (samparkAdvanceRunnable != null) samparkHandler.removeCallbacks(samparkAdvanceRunnable);
        samparkPhoto = null;
        samparkFirstPhotoCard = null;
        samparkSlideshow = null;
        samparkSlideshowCard = null;
        samparkSlideshowAdapter = null;
    }

    private void bindBookOrderSection(View root) {
        View storeLink = root.findViewById(R.id.book_order_store_link);
        if (storeLink != null) {
            storeLink.setOnClickListener(v -> {
                try {
                    if (getContext() != null) {
                        startActivity(new Intent(getContext(), BookStoreActivity.class));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Book Store", e);
                }
            });
        }
    }

    private void bindSocialLink(View root, int viewId, String url) {
        View v = root.findViewById(viewId);
        if (v != null && url != null && !url.isEmpty()) {
            v.setOnClickListener(v1 -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Exception e) {
                    Log.e(TAG, "Open link", e);
                }
            });
        }
    }

    private void bindAshram(View root, int cardId, String title, String desc, String address,
                            String phone, String website, String mapUrl, int thumbResId) {
        View card = root.findViewById(cardId);
        if (card == null) return;
        TextView t = card.findViewById(R.id.ashram_title);
        TextView d = card.findViewById(R.id.ashram_desc);
        TextView a = card.findViewById(R.id.ashram_address);
        TextView p = card.findViewById(R.id.ashram_phone);
        TextView w = card.findViewById(R.id.ashram_website);
        TextView m = card.findViewById(R.id.ashram_map);
        ImageView img = card.findViewById(R.id.ashram_thumbnail);
        View phoneRow = card.findViewById(R.id.ashram_phone_row);
        View websiteRow = card.findViewById(R.id.ashram_website_row);
        View mapRow = card.findViewById(R.id.ashram_map_row);

        if (t != null) t.setText(title);
        if (d != null) { d.setText(desc); d.setVisibility(desc != null && !desc.isEmpty() ? View.VISIBLE : View.GONE); }
        if (a != null) a.setText(address);
        if (p != null) p.setText(phone);
        if (w != null) w.setText(website);
        if (m != null) m.setText(mapUrl);
        if (phoneRow != null) phoneRow.setVisibility(phone != null && !phone.isEmpty() ? View.VISIBLE : View.GONE);
        if (websiteRow != null) websiteRow.setVisibility(website != null && !website.isEmpty() ? View.VISIBLE : View.GONE);
        if (mapRow != null) mapRow.setVisibility(mapUrl != null && !mapUrl.isEmpty() ? View.VISIBLE : View.GONE);
        if (img != null) {
            if (thumbResId != 0) {
                img.setImageResource(thumbResId);
                img.setVisibility(View.VISIBLE);
            } else {
                img.setVisibility(View.GONE);
            }
        }

        if (p != null && phone != null && !phone.isEmpty()) {
            p.setOnClickListener(v -> {
                String tel = phone.replaceAll("[^0-9+]", "").split(",")[0].trim();
                if (!tel.startsWith("+")) tel = "+91" + tel;
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + tel)));
            });
        }
        if (w != null && website != null && !website.isEmpty()) {
            w.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(website.startsWith("http") ? website : "https://" + website))));
        }
        if (m != null && mapUrl != null && !mapUrl.isEmpty()) {
            m.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mapUrl))));
        }
    }

    private void setupBottomBannerAd(View root) {
        try {
            bottomBannerAd = root.findViewById(R.id.about_bottom_banner);
            if (bottomBannerAd == null) return;
            AdRequest request = new AdRequest.Builder().build();
            bottomBannerAd.setAdListener(AdLog.wrapBannerListener("about_bottom", new AdListener() {
                @Override
                public void onAdLoaded() {
                    try {
                        if (bottomBannerAd.getVisibility() != View.VISIBLE) {
                            bottomBannerAd.setAlpha(0f);
                            bottomBannerAd.setTranslationY(12f);
                            bottomBannerAd.setVisibility(View.VISIBLE);
                            bottomBannerAd.animate()
                                    .translationY(0f)
                                    .alpha(1f)
                                    .setDuration(400L)
                                    .setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f))
                                    .start();
                        }
                    } catch (Throwable t) {
                        bottomBannerAd.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onAdFailedToLoad(LoadAdError adError) {
                    try {
                        String code = adError != null ? String.valueOf(adError.getCode()) : "null";
                        String msg = adError != null ? adError.getMessage() : "null";
                        String domain = adError != null ? adError.getDomain() : "null";
                        Log.w(TAG, "about banner failed: code=" + code + ", domain=" + domain + ", message=" + msg);
                        if (bottomBannerAd != null) {
                            bottomBannerAd.setVisibility(View.VISIBLE);
                            bottomBannerAd.setAlpha(0.25f); // show the slot for debugging
                        }
                    } catch (Throwable ignore) {}
                }
            }));
            AdLog.bannerRequest("about_bottom");
            BannerAdHelper.loadWhenReady(requireContext(), bottomBannerAd, request);
        } catch (Throwable t) {
            Log.e(TAG, "setupBottomBannerAd", t);
        }
    }
}

