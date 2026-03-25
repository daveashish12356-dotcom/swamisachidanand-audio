package com.swamisachidanand;

import android.content.Context;
import android.media.AudioManager;

/**
 * Mute media volume when full-screen ad (interstitial/rewarded) shows; restore on dismiss.
 */
public final class AdVolumeHelper {

    private static int sSavedMusicVolume = -1;

    private AdVolumeHelper() {}

    /** Call when ad shows successfully — mutes STREAM_MUSIC. */
    public static void muteForAd(Context context) {
        if (context == null) return;
        try {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (am == null) return;
            sSavedMusicVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
            am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        } catch (Throwable t) {
            sSavedMusicVolume = -1;
        }
    }

    /** Call when ad dismisses — restores previous volume. */
    public static void restoreAfterAd(Context context) {
        if (context == null || sSavedMusicVolume < 0) return;
        try {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (am == null) return;
            int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            am.setStreamVolume(AudioManager.STREAM_MUSIC, Math.min(sSavedMusicVolume, max), 0);
        } catch (Throwable ignored) {
        } finally {
            sSavedMusicVolume = -1;
        }
    }
}
