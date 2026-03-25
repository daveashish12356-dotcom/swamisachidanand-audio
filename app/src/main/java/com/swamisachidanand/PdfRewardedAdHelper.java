package com.swamisachidanand;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

/**
 * Rewarded ad while PDF downloads in background; user sees book after ad closes.
 */
public final class PdfRewardedAdHelper {

    private static final String TAG = "PdfRewardedAd";
    private static final String L = "REWARDED_PDF";

    private PdfRewardedAdHelper() {}

    /**
     * Loads rewarded ad and shows when ready. Calls onFlowFinished on dismiss, show failure, or load failure (main thread).
     */
    public static void loadAndShow(Activity activity, Runnable onFlowFinished) {
        if (activity == null || activity.isFinishing()) {
            if (onFlowFinished != null) onFlowFinished.run();
            return;
        }
        if (!BuildConfig.ADS_ENABLED) {
            runDone(activity, onFlowFinished);
            return;
        }
        String unitId = activity.getString(R.string.admob_rewarded_pdf_unit_id);
        AdLog.d(L, "loadAndShow start unitId=" + unitId);
        AdRequest request = new AdRequest.Builder().build();
        RewardedAd.load(
                activity,
                unitId,
                request,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        AdLog.i(L, "onAdLoaded");
                        if (activity.isFinishing()) {
                            runDone(activity, onFlowFinished);
                            return;
                        }
                        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                AdLog.d(L, "onAdDismissedFullScreenContent");
                                AdVolumeHelper.restoreAfterAd(activity);
                                ad.setFullScreenContentCallback(null);
                                runDone(activity, onFlowFinished);
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                                Log.w(TAG, "Rewarded show failed: " + adError.getMessage());
                                AdLog.w(L, "onAdFailedToShowFullScreenContent code=" + adError.getCode() + " msg=" + adError.getMessage());
                                runDone(activity, onFlowFinished);
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                AdLog.i(L, "onAdShowedFullScreenContent");
                                AdVolumeHelper.muteForAd(activity);
                            }
                        });
                        activity.runOnUiThread(() -> {
                            if (activity.isFinishing()) {
                                runDone(activity, onFlowFinished);
                                return;
                            }
                            AdLog.d(L, "ad.show()");
                            ad.show(activity, rewardItem -> AdLog.d(L, "onUserEarnedReward type=" + rewardItem.getType() + " amount=" + rewardItem.getAmount()));
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.w(TAG, "Rewarded load failed: " + loadAdError.getMessage());
                        AdLog.w(L, "onAdFailedToLoad " + AdLog.formatLoadError(loadAdError));
                        runDone(activity, onFlowFinished);
                    }
                });
    }

    private static void runDone(Activity activity, Runnable onFlowFinished) {
        if (onFlowFinished == null) return;
        if (activity != null && !activity.isFinishing()) {
            activity.runOnUiThread(onFlowFinished);
        } else {
            onFlowFinished.run();
        }
    }
}
