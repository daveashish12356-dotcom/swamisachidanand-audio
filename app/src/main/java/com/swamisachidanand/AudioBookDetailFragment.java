package com.swamisachidanand;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import com.google.android.material.appbar.AppBarLayout;

public class AudioBookDetailFragment extends Fragment implements AudioPartsAdapter.OnPartClickListener {

    private static final String TAG = "AudioBookDetail";
    private static final String ARG_BOOK = "book";
    private static final String PREFS_AUDIO = "audio_prefs";
    private static final String KEY_LAST_PART_ID = "last_part_id_";
    private static final String KEY_LAST_PART_TITLE = "last_part_title_";
    private static final String AMARAKANTAK_PDF = "અમરકંટક અને મધ્યપ્રદેશનો મહિમા.pdf";
    private static final String DURBALATAO_PDF = "આપણી દુર્બળતાઓ.pdf";
    private static final String MARA_ANUBHAVO_PDF = "મારા અનુભવો.pdf";
    private static final String AFRICA_SANSMARAN_PDF = "આફ્રિકા-પ્રવાસનાં સંસ્મરણો.pdf";
    private static final String AAVEGO_LAGANIYO_PDF = "આવેગો અને લાગણીઓ.pdf";
    private static final String MAHABHARAT_JEEVANKATHAO_PDF = "મહાભારતની જીવનકથાઓ.pdf";

    private ServerAudioBook book;
    /** Har book ke card me parts hamesha 1, 2, 3 ... N order me – isi list use karo display + playback. */
    private List<ServerAudioPart> sortedParts = new ArrayList<>();
    private ImageView thumbnailCompactView;
    private TextView partNameView; // Current playing part – player me dikhe
    private ImageButton playBtn;
    private SeekBar seekBar;
    private ImageButton prevBtn;
    private ImageButton nextBtn;
    private RecyclerView partsRecycler;
    private TextView timeCurrentText;
    private TextView timeTotalText;
    private View speedBtn;
    private TextView speedText;
    private float currentSpeed = 1f;
    private View miniPlayerBar;
    private ImageView miniThumbnail;
    private TextView miniPartName;
    private TextView miniTimeCurrent;
    private TextView miniTimeTotal;
    private SeekBar miniSeek;
    private ImageButton miniPlayBtn;
    private AppBarLayout appBarLayout;
    private PdfThumbnailLoader thumbnailLoader;

    private AudioPartsAdapter adapter;
    private ExoPlayer player;
    private ServerAudioPart currentPart;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable seekBarUpdateRunnable;
    private boolean userSeeking;

    public static AudioBookDetailFragment newInstance(ServerAudioBook book) {
        AudioBookDetailFragment f = new AudioBookDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_BOOK, book);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            book = getArguments().getParcelable(ARG_BOOK);
            sortedParts = book != null ? sortPartsByNumber(book.getParts()) : new ArrayList<>();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_audio_book_detail, container, false);

        thumbnailCompactView = root.findViewById(R.id.audio_detail_thumbnail_compact);
        partNameView = root.findViewById(R.id.audio_detail_part_name);
        TextView titleView = root.findViewById(R.id.audio_detail_title);
        appBarLayout = root.findViewById(R.id.audio_app_bar);
        miniPlayerBar = root.findViewById(R.id.audio_mini_player_bar);
        miniThumbnail = root.findViewById(R.id.audio_mini_thumbnail);
        miniPartName = root.findViewById(R.id.audio_mini_part_name);
        miniTimeCurrent = root.findViewById(R.id.audio_mini_time_current);
        miniTimeTotal = root.findViewById(R.id.audio_mini_time_total);
        miniSeek = root.findViewById(R.id.audio_mini_seek);
        miniPlayBtn = root.findViewById(R.id.audio_mini_play);
        playBtn = root.findViewById(R.id.audio_detail_play_btn);
        seekBar = root.findViewById(R.id.audio_detail_seek);
        prevBtn = root.findViewById(R.id.audio_detail_prev);
        nextBtn = root.findViewById(R.id.audio_detail_next);
        partsRecycler = root.findViewById(R.id.audio_detail_parts_recycler);
        timeCurrentText = root.findViewById(R.id.audio_detail_time_current);
        timeTotalText = root.findViewById(R.id.audio_detail_time_total);
        speedBtn = root.findViewById(R.id.audio_detail_speed_btn);
        speedText = root.findViewById(R.id.audio_detail_speed_text);

        updateSpeedButtonText();
        if (speedBtn != null) speedBtn.setOnClickListener(v -> showSpeedMenu());

        if (book != null) {
            if (titleView != null) titleView.setText(book.getTitle());
            loadThumbnail();
            if (playBtn != null) {
                playBtn.setOnClickListener(v -> {
                    if (player != null && currentPart != null) {
                        togglePlayPause();
                    } else {
                        playBtn.setEnabled(false);
                        playFirstOrResume();
                    }
                });
            }
            if (prevBtn != null) prevBtn.setOnClickListener(v -> playPrev());
            if (nextBtn != null) nextBtn.setOnClickListener(v -> playNext());
            SeekBar.OnSeekBarChangeListener seekListener = new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onStartTrackingTouch(SeekBar sb) { userSeeking = true; }
                @Override
                public void onStopTrackingTouch(SeekBar sb) { userSeeking = false; }
                @Override
                public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                    if (fromUser && player != null) {
                        long duration = player.getDuration();
                        if (duration > 0) player.seekTo((long) (progress / 1000.0 * duration));
                        if (seekBar != null && seekBar != sb) seekBar.setProgress(progress);
                        if (miniSeek != null && miniSeek != sb) miniSeek.setProgress(progress);
                    }
                }
            };
            if (seekBar != null) seekBar.setOnSeekBarChangeListener(seekListener);
            if (miniSeek != null) miniSeek.setOnSeekBarChangeListener(seekListener);
            if (partsRecycler != null) {
                if (sortedParts == null || sortedParts.isEmpty()) sortedParts = sortPartsByNumber(book.getParts());
                adapter = new AudioPartsAdapter();
                adapter.setOnPartClickListener(this);
                partsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
                partsRecycler.setItemAnimator(new DefaultItemAnimator());
                partsRecycler.setAdapter(adapter);
                adapter.setParts(sortedParts);
            }
            setupMiniPlayer();
            setupAppBarScrollAnimation();
        }

        return root;
    }

    private void loadThumbnail() {
        if (book == null) return;

        if (thumbnailCompactView != null) {
            thumbnailCompactView.setImageDrawable(null);
            thumbnailCompactView.setBackgroundResource(R.drawable.book_placeholder);
            loadThumbnailIntoView(thumbnailCompactView, book);
        }
        if (miniThumbnail != null) {
            miniThumbnail.setImageDrawable(null);
            miniThumbnail.setBackgroundResource(R.drawable.book_placeholder);
            loadThumbnailIntoView(miniThumbnail, book);
        }
        updatePartName();
    }
    
    private void loadThumbnailIntoView(ImageView view, ServerAudioBook book) {
        String thumbUrl = book.getThumbnailUrl();
        if (thumbUrl != null && !thumbUrl.isEmpty()) {
            Glide.with(requireContext())
                    .load(thumbUrl)
                    .apply(new RequestOptions().transform(new RoundedCorners(8)))
                    .placeholder(R.drawable.book_placeholder)
                    .error(R.drawable.book_placeholder)
                    .into(view);
        } else {
            String pdfName = getPdfNameForBook(book.getId());
            if (pdfName != null) {
                if (thumbnailLoader == null) thumbnailLoader = PdfThumbnailLoader.getInstance();
                thumbnailLoader.loadThumbnail(requireContext(), pdfName, thumb -> {
                    if (view != null && thumb != null && !thumb.isRecycled()) {
                        view.setImageBitmap(thumb);
                        view.setBackground(null);
                    }
                });
            }
        }
    }
    
    private String getPdfNameForBook(String id) {
        if (id == null) return null;
        switch (id) {
            case "amarakantak_madhyapradesh": return AMARAKANTAK_PDF;
            case "aapni_durbalatao": return DURBALATAO_PDF;
            case "mara_anubhavo": return MARA_ANUBHAVO_PDF;
            case "africa_sansmaran": return AFRICA_SANSMARAN_PDF;
            case "aavego_laganiyo": return AAVEGO_LAGANIYO_PDF;
            default: return null;
        }
    }
    
    private void updatePartName() {
        String text;
        if (currentPart != null) {
            int idx = currentPartIndex();
            String num = idx >= 0 ? String.valueOf(idx + 1) : "?";
            String title = currentPart.getTitle();
            if (title != null && !title.trim().isEmpty()) {
                text = "ભાગ " + num + " – " + title.trim();
            } else {
                text = "ભાગ " + num;
            }
        } else {
            text = book != null ? book.getTitle() : "ભાગ ચૂંટો";
        }
        if (partNameView != null) partNameView.setText(text);
        if (miniPartName != null) miniPartName.setText(text);
    }


    private void saveLastPlayed(ServerAudioPart part) {
        if (book == null || part == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_AUDIO, 0);
        prefs.edit()
                .putString(KEY_LAST_PART_ID + book.getId(), part.getId())
                .putString(KEY_LAST_PART_TITLE + book.getId(), part.getTitle())
                .apply();
        RecentActivityHelper.saveActivity(requireContext(), RecentActivityHelper.TYPE_AUDIO, book.getId());
    }

    /** Har book ke parts 1, 2, 3 ... N order me – id numeric sort. */
    private static List<ServerAudioPart> sortPartsByNumber(List<ServerAudioPart> parts) {
        if (parts == null || parts.isEmpty()) return new ArrayList<>();
        List<ServerAudioPart> list = new ArrayList<>(parts);
        Collections.sort(list, (a, b) -> {
            int na = parsePartNumber(a != null ? a.getId() : null);
            int nb = parsePartNumber(b != null ? b.getId() : null);
            if (na >= 0 && nb >= 0) return Integer.compare(na, nb);
            if (na >= 0) return -1;
            if (nb >= 0) return 1;
            String sa = a != null ? a.getId() : "";
            String sb = b != null ? b.getId() : "";
            return (sa != null ? sa : "").compareTo(sb != null ? sb : "");
        });
        return list;
    }

    private static int parsePartNumber(String id) {
        if (id == null || id.isEmpty()) return -1;
        id = id.trim();
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void playFirstOrResume() {
        if (book == null || sortedParts == null || sortedParts.isEmpty()) return;
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_AUDIO, 0);
        String lastId = prefs.getString(KEY_LAST_PART_ID + book.getId(), null);
        ServerAudioPart toPlay = null;
        if (lastId != null) {
            for (ServerAudioPart p : sortedParts) {
                if (lastId.equals(p.getId())) {
                    toPlay = p;
                    break;
                }
            }
        }
        if (toPlay == null) toPlay = sortedParts.get(0);
        releasePlayer();
        playPart(toPlay);
    }

    private void playPrev() {
        if (book == null || sortedParts == null || sortedParts.isEmpty()) return;
        int idx = currentPartIndex();
        if (idx > 0) {
            releasePlayer();
            playPart(sortedParts.get(idx - 1));
        } else if (player != null) {
            player.seekTo(0);
        }
    }

    private void playNext() {
        if (book == null || sortedParts == null || sortedParts.isEmpty()) return;
        int idx = currentPartIndex();
        if (idx >= 0 && idx < sortedParts.size() - 1) {
            releasePlayer();
            playPart(sortedParts.get(idx + 1));
        } else if (idx == sortedParts.size() - 1) {
            stopPlaying();
        }
    }

    private int currentPartIndex() {
        if (book == null || currentPart == null || sortedParts == null) return -1;
        for (int i = 0; i < sortedParts.size(); i++) {
            if (sortedParts.get(i).getId().equals(currentPart.getId())) return i;
        }
        return -1;
    }

    private ServerAudioPart getNextPart() {
        int idx = currentPartIndex();
        if (book == null || sortedParts == null || idx < 0 || idx >= sortedParts.size() - 1) return null;
        return sortedParts.get(idx + 1);
    }

    private void startSeekBarUpdates() {
        stopSeekBarUpdates();
        seekBarUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (player == null || seekBar == null || userSeeking) {
                    mainHandler.postDelayed(this, 500);
                    return;
                }
                long duration = player.getDuration();
                long position = player.getCurrentPosition();
                if (duration > 0) {
                    int progress = (int) (1000.0 * position / duration);
                    seekBar.setProgress(Math.min(progress, 1000));
                }
                if (timeCurrentText != null) timeCurrentText.setText(formatTime(position));
                if (timeTotalText != null) timeTotalText.setText(formatTime(duration));
                if (miniTimeCurrent != null) miniTimeCurrent.setText(formatTime(position));
                if (miniTimeTotal != null) miniTimeTotal.setText(formatTime(duration));
                if (miniSeek != null && !userSeeking) {
                    int p = (int) (1000.0 * position / duration);
                    miniSeek.setProgress(Math.min(p, 1000));
                }
                updateBigPlayButton();
                mainHandler.postDelayed(this, 500);
            }
        };
        mainHandler.post(seekBarUpdateRunnable);
    }

    private void stopSeekBarUpdates() {
        if (seekBarUpdateRunnable != null) {
            mainHandler.removeCallbacks(seekBarUpdateRunnable);
            seekBarUpdateRunnable = null;
        }
    }

    private void setupMiniPlayer() {
        if (miniPlayBtn != null) {
            miniPlayBtn.setOnClickListener(v -> {
                if (player != null && currentPart != null) togglePlayPause();
                else { playBtn.setEnabled(false); playFirstOrResume(); }
            });
        }
    }

    private void setupAppBarScrollAnimation() {
        if (appBarLayout == null || miniPlayerBar == null) return;
        appBarLayout.addOnOffsetChangedListener((bar, verticalOffset) -> {
            int totalRange = bar.getTotalScrollRange();
            if (totalRange <= 0) return;
            float fraction = (float) -verticalOffset / totalRange;
            fraction = Math.max(0f, Math.min(1f, fraction));
            if (fraction < 0.05f) {
                miniPlayerBar.setVisibility(View.GONE);
                miniPlayerBar.setAlpha(0f);
            } else {
                miniPlayerBar.setVisibility(View.VISIBLE);
                miniPlayerBar.animate().cancel();
                miniPlayerBar.animate().alpha(fraction).setDuration(150).start();
            }
        });
    }

    private void updateBigPlayButton() {
        boolean playing = player != null && player.getPlayWhenReady();
        if (playBtn != null) {
            playBtn.setImageResource(playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
        }
        if (miniPlayBtn != null) {
            miniPlayBtn.setImageResource(playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
        }
    }

    /** Format as 00:00 (two-digit minutes). */
    private static String formatTime(long ms) {
        if (ms < 0) ms = 0;
        long sec = ms / 1000;
        long min = sec / 60;
        sec = sec % 60;
        return String.format("%02d:%02d", min, sec);
    }

    private static final float[] SPEEDS = {0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f};
    private static final String[] SPEED_LABELS = {"0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x"};

    private void showSpeedMenu() {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("ઝડપ")
                .setItems(SPEED_LABELS, (d, which) -> {
                    if (which >= 0 && which < SPEEDS.length) setPlaybackSpeed(SPEEDS[which]);
                })
                .create();
        dialog.show();
        Window w = dialog.getWindow();
        if (w != null) {
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.width = (int) (180 * getResources().getDisplayMetrics().density);
            w.setAttributes(lp);
        }
    }

    private void setPlaybackSpeed(float speed) {
        currentSpeed = speed;
        if (player != null) player.setPlaybackSpeed(speed);
        updateSpeedButtonText();
    }

    private void updateSpeedButtonText() {
        if (speedText == null) return;
        String label = "1x";
        for (int i = 0; i < SPEEDS.length; i++) {
            if (Math.abs(SPEEDS[i] - currentSpeed) < 0.01f) {
                label = SPEED_LABELS[i];
                break;
            }
        }
        speedText.setText(label + " ▼");
    }

    @Override
    public void onDestroyView() {
        stopSeekBarUpdates();
        releasePlayer();
        playBtn = null;
        seekBar = null;
        prevBtn = null;
        nextBtn = null;
        timeCurrentText = null;
        timeTotalText = null;
        speedBtn = null;
        speedText = null;
        miniPlayerBar = null;
        miniThumbnail = null;
        miniPartName = null;
        miniTimeCurrent = null;
        miniTimeTotal = null;
        miniSeek = null;
        miniPlayBtn = null;
        appBarLayout = null;
        super.onDestroyView();
    }

    @Override
    public void onPartClick(ServerAudioPart part) {
        if (part == null || part.getUrl().isEmpty()) {
            Toast.makeText(requireContext(), R.string.audio_error, Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentPart != null && currentPart.getUrl().equals(part.getUrl())) {
            togglePlayPause();
            return;
        }
        releasePlayer();
        playPart(part);
    }

    /** URL encode filename for streaming. */
    private static String encodeAudioUrl(String url) {
        if (url == null || url.isEmpty()) return url;
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash < 0) return url;
        String base = url.substring(0, lastSlash + 1);
        String filename = url.substring(lastSlash + 1);
        try {
            String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8.name()).replace("+", "%20");
            return base + encoded;
        } catch (Exception e) {
            return url;
        }
    }

    /** Stream from server (YouTube-style) – no download. */
    private void playPart(ServerAudioPart part) {
        currentPart = part;
        saveLastPlayed(part);
        // Update adapter to highlight current part
        if (adapter != null && part != null) {
            adapter.setCurrentPlayingPartId(part.getId());
        }
        // Update part name in compact play menu
        updatePartName();
        if (playBtn != null) playBtn.setEnabled(false);
        String url = encodeAudioUrl(part.getUrl());
        Toast.makeText(requireContext(), R.string.audio_loading, Toast.LENGTH_SHORT).show();

        if (player == null) {
            java.util.Map<String, String> headers = new java.util.HashMap<>();
            headers.put("Accept", "application/octet-stream,*/*");
            DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory()
                    .setUserAgent("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")
                    .setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(30_000)
                    .setReadTimeoutMs(90_000)
                    .setDefaultRequestProperties(headers);
            DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(requireContext(), httpFactory);
            DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(requireContext()).setDataSourceFactory(dataSourceFactory);
            player = new ExoPlayer.Builder(requireContext()).setMediaSourceFactory(mediaSourceFactory).build();
            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_READY) {
                        mainHandler.post(() -> {
                            if (playBtn != null) playBtn.setEnabled(true);
                            startSeekBarUpdates();
                            updateBigPlayButton();
                        });
                    }
                    if (state == Player.STATE_ENDED) {
                        mainHandler.post(() -> {
                            stopSeekBarUpdates();
                            ServerAudioPart nextPart = getNextPart();
                            if (nextPart != null) {
                                currentPart = null;
                                if (adapter != null) adapter.setCurrentPlayingPartId(null);
                                playPart(nextPart);
                            } else {
                                if (playBtn != null) playBtn.setEnabled(true);
                                currentPart = null;
                                if (adapter != null) adapter.setCurrentPlayingPartId(null);
                                updateBigPlayButton();
                            }
                        });
                    }
                }

                @Override
                public void onPlayerError(androidx.media3.common.PlaybackException error) {
                    Log.e(TAG, "ExoPlayer error", error);
                    mainHandler.post(() -> {
                        Toast.makeText(requireContext(), R.string.audio_play_error, Toast.LENGTH_LONG).show();
                        if (playBtn != null) playBtn.setEnabled(true);
                        currentPart = null;
                        updateBigPlayButton();
                    });
                }
            });
        }

        Log.d(TAG, "playPart url=" + url);
        Uri uri = Uri.parse(url);
        MediaItem mediaItem = url.toLowerCase().endsWith(".wav")
                ? new MediaItem.Builder().setUri(uri).setMimeType("audio/wav").build()
                : MediaItem.fromUri(uri);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlaybackSpeed(currentSpeed);
        player.setPlayWhenReady(true);
        mainHandler.post(() -> {
            updateBigPlayButton();
            if (timeCurrentText != null) timeCurrentText.setText("0:00");
            if (timeTotalText != null) timeTotalText.setText("0:00");
        });
    }

    private void togglePlayPause() {
        if (player == null) return;
        boolean playing = player.getPlayWhenReady();
        player.setPlayWhenReady(!playing);
        updateBigPlayButton();
    }

    private void stopPlaying() {
        releasePlayer();
        currentPart = null;
        // Clear highlight when stopped
        if (adapter != null) {
            adapter.setCurrentPlayingPartId(null);
        }
    }

    private void releasePlayer() {
        stopSeekBarUpdates();
        if (player != null) {
            player.release();
            player = null;
        }
        if (playBtn != null) playBtn.setEnabled(true);
        updateBigPlayButton();
        if (timeCurrentText != null) timeCurrentText.setText("0:00");
        if (timeTotalText != null) timeTotalText.setText("0:00");
    }

}
