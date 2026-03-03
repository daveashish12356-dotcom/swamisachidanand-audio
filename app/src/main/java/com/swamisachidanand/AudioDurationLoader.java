package com.swamisachidanand;

import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fetches total duration for audio books by summing all parts' durations.
 * When JSON has no duration, fetches from MediaMetadataRetriever (max 5 parts per book).
 */
public final class AudioDurationLoader {

    private static final int MAX_PARTS_TO_FETCH = 5;
    private static final int FETCH_TIMEOUT_MS = 8000;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onDurationsLoaded(Map<String, Integer> bookIdToTotalSeconds);
    }

    public void loadDurations(List<ServerAudioBook> books, Callback callback) {
        if (books == null || callback == null) return;
        executor.execute(() -> {
            Map<String, Integer> result = new HashMap<>();
            for (ServerAudioBook book : books) {
                if (book == null) continue;
                int total = book.getTotalDurationSeconds();
                if (total > 0) {
                    result.put(book.getId(), total);
                    continue;
                }
                List<ServerAudioPart> parts = book.getParts();
                if (parts == null || parts.isEmpty()) continue;
                int sum = 0;
                int count = 0;
                for (ServerAudioPart p : parts) {
                    if (p == null) continue;
                    String url = p.getUrl();
                    if (url == null || url.trim().isEmpty()) continue;
                    if (count >= MAX_PARTS_TO_FETCH) break;
                    int dur = fetchDurationFromUrl(url);
                    if (dur > 0) {
                        sum += dur;
                        count++;
                    }
                }
                if (sum > 0 && parts.size() > count) {
                    sum = (int) (sum * (double) parts.size() / count);
                }
                if (sum > 0) {
                    result.put(book.getId(), sum);
                }
            }
            Map<String, Integer> finalResult = result;
            mainHandler.post(() -> callback.onDurationsLoaded(finalResult));
        });
    }

    private static int fetchDurationFromUrl(String url) {
        MediaMetadataRetriever retriever = null;
        try {
            retriever = new MediaMetadataRetriever();
            retriever.setDataSource(url);
            String durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durStr != null) {
                return Integer.parseInt(durStr) / 1000;
            }
        } catch (Exception ignored) {
        } finally {
            try {
                if (retriever != null) retriever.release();
            } catch (Exception ignored) {}
        }
        return 0;
    }
}
