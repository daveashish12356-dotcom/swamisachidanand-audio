package com.swamisachidanand;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.datasource.RawResourceDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Splash — sound pehle (bada DP decode sound ko block na kare), phir image.
 * Raw MP3 waveform ~30% (ffmpeg) + player 0.3f — dono mil kar ~30% × 30% ≈ halka intro; media volume alag.
 * Play: MediaPlayer+FD → ExoPlayer + DefaultMediaSourceFactory.
 * Intro sound sirf pehli baar (prefs {@code splash_intro_sound_done}).
 */
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final String KEY_SPLASH_INTRO_SOUND_DONE = "splash_intro_sound_done";
    private static final int SPLASH_DURATION = 4000;
    /** Player left/right ~30% (0.3f); file bhi ~30% gain — bahut halka splash */
    private static final float SPLASH_AUDIO_VOLUME = 0.3f;

    private MediaPlayer splashMediaPlayer;
    private ExoPlayer splashExoPlayer;
    private AudioFocusRequest splashFocusRequest;
    private AudioManager audioManager;
    private boolean splashFocusRequested;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        try {
            setContentView(R.layout.activity_splash);
        } catch (Throwable t) {
            finish();
            startActivity(new Intent(this, MainActivity.class));
            return;
        }

        // Image pehle (DP jaisa pehle tha); sound next frame par — warna Samsung par playback window
        // attach hone se pehle start ho kar speaker route nahi hota (log me start dikhta, kaan me nahi).
        loadSplashImageFromAssets();
        final android.view.View decor = getWindow().getDecorView();
        decor.post(this::maybeStartSplashSoundOnce);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                if (!isFinishing()) {
                    SharedPreferences prefs = getSharedPreferences(AppTourActivity.PREFS_NAME, MODE_PRIVATE);
                    boolean tourDone = prefs.getBoolean(AppTourActivity.KEY_TOUR_DONE, false);
                    Intent next = new Intent(SplashActivity.this, MainActivity.class);
                    if (!tourDone) {
                        next.putExtra(MainActivity.EXTRA_INTERACTIVE_TOUR, true);
                    }
                    startActivity(next);
                    finish();
                }
            } catch (Throwable t) {
                try {
                    startActivity(new Intent(this, MainActivity.class));
                } catch (Throwable t2) {
                    finish();
                }
            }
        }, SPLASH_DURATION);
    }

    private void loadSplashImageFromAssets() {
        ImageView splashImage = findViewById(R.id.splash_image);
        if (splashImage == null) return;
        String[] tryImages = {"splash.jpg", "FOYHY6rVIAYd03r.jpg", "home_photo2.jpg", "splash.png"};
        boolean loaded = false;
        for (String name : tryImages) {
            try (InputStream is = getAssets().open(name)) {
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                if (bitmap != null) {
                    splashImage.setImageBitmap(bitmap);
                    loaded = true;
                    break;
                }
            } catch (IOException ignored) { }
        }
        if (!loaded) {
            try {
                splashImage.setImageResource(android.R.drawable.ic_menu_gallery);
            } catch (Throwable ignored) {}
        }
        try {
            splashImage.setAlpha(0f);
            splashImage.animate().alpha(1f).setDuration(500).start();
        } catch (Throwable ignored) {}
    }

    private void requestSplashAudioFocus() {
        if (splashFocusRequested) return;
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager == null) return;
        AudioAttributes aa = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        splashFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(aa)
                .setOnAudioFocusChangeListener(focusChange -> { /* splash chhota clip */ })
                .build();
        int r = audioManager.requestAudioFocus(splashFocusRequest);
        splashFocusRequested = (r == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        if (!splashFocusRequested) {
            Log.w(TAG, "Audio focus not granted: " + r);
        }
    }

    private void abandonSplashAudioFocus() {
        if (!splashFocusRequested || audioManager == null || splashFocusRequest == null) return;
        try {
            audioManager.abandonAudioFocusRequest(splashFocusRequest);
        } catch (Throwable ignored) { }
        splashFocusRequested = false;
        splashFocusRequest = null;
    }

    /** Pehli successful open par hi sound; prefs me save. */
    private void maybeStartSplashSoundOnce() {
        if (isDestroyed()) return;
        SharedPreferences prefs = getSharedPreferences(AppTourActivity.PREFS_NAME, MODE_PRIVATE);
        if (prefs.getBoolean(KEY_SPLASH_INTRO_SOUND_DONE, false)) {
            return;
        }
        startSplashSound(prefs);
    }

    private void startSplashSound(SharedPreferences prefsToMarkOnSuccess) {
        if (isDestroyed()) return;
        requestSplashAudioFocus();
        try {
            int cur = audioManager != null ? audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) : -1;
            int max = audioManager != null ? audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) : -1;
            Log.e(TAG, "STREAM_MUSIC volume=" + cur + "/" + max);
            // Device par aksar yahi issue: ringtone alag, media 0 — splash MP3 STREAM_MUSIC se bajta hai
            if (audioManager != null && cur == 0 && max > 0) {
                Toast.makeText(this, "Media volume 0 thi — music slider up karo (ringtone alag hoti hai)", Toast.LENGTH_LONG).show();
                try {
                    audioManager.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_RAISE,
                            AudioManager.FLAG_SHOW_UI);
                } catch (Throwable ignored) { }
            }
        } catch (Throwable ignored) { }
        if (tryStartSplashMediaPlayerFd()) {
            Log.e(TAG, "Splash OK: MediaPlayer+raw fd");
            markSplashIntroDone(prefsToMarkOnSuccess);
            return;
        }
        if (tryStartSplashExoPlayer()) {
            Log.e(TAG, "Splash OK: ExoPlayer raw");
            markSplashIntroDone(prefsToMarkOnSuccess);
            return;
        }
        Log.e(TAG, "Splash FAIL: MediaPlayer + ExoPlayer dono");
    }

    private static void markSplashIntroDone(SharedPreferences prefs) {
        if (prefs == null) return;
        prefs.edit().putBoolean(KEY_SPLASH_INTRO_SOUND_DONE, true).apply();
    }

    /** Recommended raw path — FD close setDataSource ke baad safe hai. */
    private boolean tryStartSplashMediaPlayerFd() {
        AssetFileDescriptor afd = null;
        try {
            afd = getResources().openRawResourceFd(R.raw.swamiji_splash_intro);
            if (afd == null) return false;
            releaseSplashPlayersOnly();
            splashMediaPlayer = new MediaPlayer();
            splashMediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build());
            splashMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            afd = null;
            splashMediaPlayer.setVolume(SPLASH_AUDIO_VOLUME, SPLASH_AUDIO_VOLUME);
            splashMediaPlayer.setOnCompletionListener(mp -> releaseSplashAudio());
            splashMediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer err what=" + what + " extra=" + extra);
                releaseSplashAudio();
                return true;
            });
            splashMediaPlayer.prepare();
            splashMediaPlayer.start();
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "MediaPlayer+fd failed", t);
            if (afd != null) {
                try {
                    afd.close();
                } catch (IOException ignored) { }
            }
            releaseSplashPlayersOnly();
            return false;
        }
    }

    private boolean tryStartSplashExoPlayer() {
        try {
            releaseSplashPlayersOnly();
            Uri uri = RawResourceDataSource.buildRawResourceUri(R.raw.swamiji_splash_intro);
            splashExoPlayer = new ExoPlayer.Builder(this)
                    .setMediaSourceFactory(new DefaultMediaSourceFactory(this))
                    .build();
            // Focus pehle requestSplashAudioFocus() se — yahan dobara Exo se mat mango
            splashExoPlayer.setAudioAttributes(
                    new androidx.media3.common.AudioAttributes.Builder()
                            .setUsage(C.USAGE_MEDIA)
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                            .build(),
                    false);
            splashExoPlayer.setVolume(SPLASH_AUDIO_VOLUME);
            splashExoPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_ENDED) {
                        runOnUiThread(() -> releaseSplashAudio());
                    }
                }
            });
            splashExoPlayer.setMediaItem(MediaItem.fromUri(uri));
            splashExoPlayer.prepare();
            splashExoPlayer.setPlayWhenReady(true);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "ExoPlayer splash failed", t);
            releaseSplashPlayersOnly();
            return false;
        }
    }

    private void releaseSplashPlayersOnly() {
        if (splashMediaPlayer != null) {
            try {
                splashMediaPlayer.release();
            } catch (Throwable ignored) { }
            splashMediaPlayer = null;
        }
        if (splashExoPlayer != null) {
            try {
                splashExoPlayer.release();
            } catch (Throwable ignored) { }
            splashExoPlayer = null;
        }
    }

    private void releaseSplashAudio() {
        releaseSplashPlayersOnly();
        abandonSplashAudioFocus();
    }

    @Override
    protected void onDestroy() {
        releaseSplashAudio();
        super.onDestroy();
    }
}
