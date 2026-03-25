package com.swamisachidanand;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * Simple in-app player for a single pravachan audio (Telegram URL).
 */
public class PravachanPlayerFragment extends Fragment {

    private static final String ARG_ITEM = "item";

    private PravachanItem item;
    private ExoPlayer player;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable seekRunnable;
    private boolean userSeeking;
    // When user navigates away (bottom nav), this fragment can remain in backstack.
    // We pause audio whenever fragment is not in foreground, so other pages pe audio nahi chale.
    private boolean wasPlayingBeforePause = false;
    /** Pauses playback immediately (used when user switches bottom-nav pages). */
    public void pausePlayback() {
        if (player == null) return;
        try {
            wasPlayingBeforePause = player.getPlayWhenReady();
            // Stop is important because this fragment can stay in backstack,
            // and some devices may keep audio running if only pause is called.
            player.setPlayWhenReady(false);
            player.pause();
            player.stop();
            updatePlayIcon();
        } catch (Throwable t) {
            // ignore
        }
        stopSeekUpdates();
    }

    /** Optional helper to resume after pause if needed. */
    private void resumePlaybackIfNeeded() {
        if (player == null) return;
        if (wasPlayingBeforePause) {
            player.setPlayWhenReady(true);
            updatePlayIcon();
            startSeekUpdates();
        }
    }

    private TextView titleView;
    private TextView timeCurrent;
    private TextView timeTotal;
    private SeekBar seekBar;
    private ImageButton playBtn;

    public static PravachanPlayerFragment newInstance(@NonNull PravachanItem item) {
        PravachanPlayerFragment f = new PravachanPlayerFragment();
        Bundle b = new Bundle();
        b.putParcelable(ARG_ITEM, item);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            item = getArguments().getParcelable(ARG_ITEM);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_pravachan_player, container, false);
        titleView = root.findViewById(R.id.pravachan_player_title);
        timeCurrent = root.findViewById(R.id.pravachan_player_time_current);
        timeTotal = root.findViewById(R.id.pravachan_player_time_total);
        seekBar = root.findViewById(R.id.pravachan_player_seek);
        playBtn = root.findViewById(R.id.pravachan_player_play);

        if (item != null && titleView != null) {
            titleView.setText(item.title);
        }

        if (playBtn != null) {
            playBtn.setOnClickListener(v -> {
                if (player == null) {
                    startPlayback();
                } else {
                    boolean playing = player.getPlayWhenReady();
                    player.setPlayWhenReady(!playing);
                    updatePlayIcon();
                }
            });
        }
        if (seekBar != null) {
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) { userSeeking = true; }

                @Override public void onStopTrackingTouch(SeekBar seekBar) { userSeeking = false; }

                @Override
                public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                    if (fromUser && player != null) {
                        long duration = player.getDuration();
                        if (duration > 0) {
                            long pos = (long) (progress / 1000.0 * duration);
                            player.seekTo(pos);
                        }
                    }
                }
            });
        }

        startPlayback();
        return root;
    }

    private void startPlayback() {
        if (item == null || item.audioUrl == null || item.audioUrl.isEmpty()) {
            Toast.makeText(requireContext(), R.string.audio_error, Toast.LENGTH_SHORT).show();
            return;
        }
        String raw = item.audioUrl.trim();
        playUrl(raw, encodeUrl(raw), raw, true);
    }

    private void playUrl(String rawUrl, String urlToPlay, String rawForRetry, boolean allowRawFallback) {
        if (player != null) {
            player.release();
            player = null;
        }

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
                        if (playBtn != null) playBtn.setImageResource(android.R.drawable.ic_media_play);
                    });
                }
            }

            @Override
            public void onPlayerError(@NonNull androidx.media3.common.PlaybackException error) {
                if (allowRawFallback && rawForRetry != null && !rawForRetry.equals(urlToPlay)) {
                    mainHandler.post(() -> playUrl(rawForRetry, rawForRetry, rawForRetry, false));
                    return;
                }
                mainHandler.post(() -> {
                    Toast.makeText(requireContext(), R.string.audio_play_error, Toast.LENGTH_LONG).show();
                    stopSeekUpdates();
                });
            }
        });

        Uri uri = Uri.parse(urlToPlay);
        MediaItem mediaItem = isWavUrl(urlToPlay)
                ? new MediaItem.Builder().setUri(uri).setMimeType("audio/wav").build()
                : MediaItem.fromUri(uri);
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

    private void startSeekUpdates() {
        stopSeekUpdates();
        seekRunnable = new Runnable() {
            @Override
            public void run() {
                if (player == null || seekBar == null || userSeeking) {
                    mainHandler.postDelayed(this, 500);
                    return;
                }
                long dur = player.getDuration();
                long pos = player.getCurrentPosition();
                if (dur > 0) {
                    int progress = (int) (1000.0 * pos / dur);
                    seekBar.setProgress(Math.min(progress, 1000));
                }
                if (timeCurrent != null) timeCurrent.setText(formatTime(pos));
                if (timeTotal != null) timeTotal.setText(formatTime(dur));
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
        if (playBtn == null || player == null) return;
        boolean playing = player.getPlayWhenReady();
        playBtn.setImageResource(playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
    }

    private static String formatTime(long ms) {
        if (ms < 0) ms = 0;
        long sec = ms / 1000;
        long min = sec / 60;
        sec = sec % 60;
        return String.format("%02d:%02d", min, sec);
    }

    /** Same as AudioPravachanFragment — preserve ?query (signed URLs). */
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

    @Override
    public void onDestroyView() {
        stopSeekUpdates();
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroyView();
    }

    @Override
    public void onPause() {
        super.onPause();
        pausePlayback();
    }

    @Override
    public void onStop() {
        super.onStop();
        pausePlayback();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (player != null) {
            resumePlaybackIfNeeded();
        } else if (item != null) {
            startPlayback();
        }
    }
}

