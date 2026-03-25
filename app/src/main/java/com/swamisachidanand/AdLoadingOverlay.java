package com.swamisachidanand;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import androidx.annotation.Nullable;

/**
 * Smooth loading overlay when interstitial/rewarded ad is loading.
 * Dismiss in onDone callback.
 */
public final class AdLoadingOverlay {

    private AdLoadingOverlay() {}

    private static View sOverlay;

    public static void show(Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        dismiss(activity);
        try {
            ViewGroup root = activity.findViewById(android.R.id.content);
            if (root == null) return;
            View overlay = LayoutInflater.from(activity).inflate(R.layout.ad_loading_overlay, root, false);
            overlay.setId(View.generateViewId());
            overlay.setOnClickListener(v -> { /* block touches */ });
            root.addView(overlay);
            sOverlay = overlay;
            overlay.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.fade_in));
        } catch (Throwable ignore) { /* no-op */ }
    }

    public static void dismiss(@Nullable Activity activity) {
        try {
            if (sOverlay != null && sOverlay.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) sOverlay.getParent();
                if (activity != null && !activity.isFinishing()) {
                    sOverlay.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.fade_out));
                    sOverlay.postDelayed(() -> {
                        try {
                            parent.removeView(sOverlay);
                        } catch (Throwable ignore) { }
                        sOverlay = null;
                    }, 150);
                } else {
                    parent.removeView(sOverlay);
                    sOverlay = null;
                }
            } else {
                sOverlay = null;
            }
        } catch (Throwable ignore) {
            sOverlay = null;
        }
    }
}
