package com.swamisachidanand;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.initialization.AdapterStatus;
import com.google.android.gms.ads.initialization.InitializationStatus;

import java.util.Map;

/**
 * Single log tag for all AdMob events — filter logcat: {@code adb logcat -s SwamiAds}
 */
public final class AdLog {

    public static final String TAG = "SwamiAds";

    private AdLog() {}

    public static void d(String sub, String msg) {
        Log.d(TAG, "[" + sub + "] " + msg);
    }

    public static void w(String sub, String msg) {
        Log.w(TAG, "[" + sub + "] " + msg);
    }

    public static void i(String sub, String msg) {
        Log.i(TAG, "[" + sub + "] " + msg);
    }

    public static String formatLoadError(@Nullable LoadAdError e) {
        if (e == null) return "null";
        return "code=" + e.getCode() + " domain=" + e.getDomain() + " msg=" + e.getMessage();
    }

    /** Call right before {@code adView.loadAd(request)} */
    public static void bannerRequest(String placement) {
        d("BANNER", placement + " loadAd()");
    }

    /**
     * Wraps your {@link AdListener} and logs loaded / failed / opened / closed / click / impression.
     */
    public static AdListener wrapBannerListener(String placement, @Nullable AdListener inner) {
        return new AdListener() {
            @Override
            public void onAdLoaded() {
                d("BANNER", placement + " onAdLoaded");
                if (inner != null) inner.onAdLoaded();
            }

            @Override
            public void onAdFailedToLoad(LoadAdError e) {
                w("BANNER", placement + " onAdFailedToLoad " + formatLoadError(e));
                if (inner != null) inner.onAdFailedToLoad(e);
            }

            @Override
            public void onAdOpened() {
                d("BANNER", placement + " onAdOpened");
                if (inner != null) inner.onAdOpened();
            }

            @Override
            public void onAdClosed() {
                d("BANNER", placement + " onAdClosed");
                if (inner != null) inner.onAdClosed();
            }

            @Override
            public void onAdClicked() {
                d("BANNER", placement + " onAdClicked");
                if (inner != null) inner.onAdClicked();
            }

            @Override
            public void onAdImpression() {
                d("BANNER", placement + " onAdImpression");
                if (inner != null) inner.onAdImpression();
            }
        };
    }

    public static void logSdkInit(@Nullable InitializationStatus status) {
        if (status == null) {
            w("SDK", "InitializationStatus null");
            return;
        }
        for (Map.Entry<String, AdapterStatus> e : status.getAdapterStatusMap().entrySet()) {
            AdapterStatus as = e.getValue();
            d("SDK", e.getKey() + " state=" + as.getInitializationState() + " desc=" + as.getDescription());
        }
    }
}
