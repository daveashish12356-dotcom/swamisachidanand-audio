package com.swamisachidanand;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One tab in SearchResultActivity – Books, Audio, or Videos.
 * Loads and filters data by query.
 */
public class SearchResultFragment extends Fragment implements BookAdapter.OnBookClickListener {

    private static final String ARG_TYPE = "type";
    private static final String ARG_QUERY = "query";

    static final int TYPE_BOOKS = 0;
    static final int TYPE_AUDIO = 1;
    static final int TYPE_VIDEOS = 2;

    private int type;
    private String query;

    private RecyclerView recycler;
    private View emptyState;
    private TextView emptyMessage;
    private ProgressBar loading;

    private BookAdapter bookAdapter;
    private AudioBookCardAdapter audioAdapter;
    private HomeVideoAdapter videoAdapter;

    static SearchResultFragment newInstance(int type, String query) {
        SearchResultFragment f = new SearchResultFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_TYPE, type);
        b.putString(ARG_QUERY, query != null ? query : "");
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle b = getArguments();
        type = b != null ? b.getInt(ARG_TYPE, TYPE_BOOKS) : TYPE_BOOKS;
        query = b != null ? b.getString(ARG_QUERY, "") : "";
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_search_results, container, false);
        recycler = root.findViewById(R.id.search_results_recycler);
        emptyState = root.findViewById(R.id.search_empty_state);
        emptyMessage = root.findViewById(R.id.search_empty_message);
        loading = root.findViewById(R.id.search_loading);

        Context ctx = getContext();
        if (ctx == null) return root;

        // setHasFixedSize omitted for wrap_content layout
        recycler.setItemAnimator(new DefaultItemAnimator());
        switch (type) {
            case TYPE_BOOKS:
                bookAdapter = new BookAdapter(new ArrayList<>(), this);
                bookAdapter.setUseCompactLayout(false);
                recycler.setLayoutManager(new GridLayoutManager(ctx, 2));
                recycler.setAdapter(bookAdapter);
                loadBooks();
                break;
            case TYPE_AUDIO:
                audioAdapter = new AudioBookCardAdapter();
                audioAdapter.setOnAudioBookClickListener(this::onAudioClick);
                recycler.setLayoutManager(new GridLayoutManager(ctx, 2));
                recycler.setAdapter(audioAdapter);
                loadAudio();
                break;
            case TYPE_VIDEOS:
                videoAdapter = new HomeVideoAdapter();
                recycler.setLayoutManager(new LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false));
                recycler.setAdapter(videoAdapter);
                loadVideos();
                break;
            default:
                showEmpty();
        }
        return root;
    }

    private void openBook(Book book) {
        if (getContext() == null || book == null) return;
        Intent i = new Intent(getContext(), PdfViewerActivity.class);
        String pdfUrl = book.getPdfUrl();
        if (pdfUrl != null && !pdfUrl.trim().isEmpty()) {
            i.putExtra("pdf_url", pdfUrl.trim());
            i.putExtra("book_name", book.getName() != null ? book.getName() : "");
            if (book.getThumbnailUrl() != null && !book.getThumbnailUrl().trim().isEmpty())
                i.putExtra("thumbnail_url", book.getThumbnailUrl().trim());
        } else {
            String fileName = book.getFileName();
            if (fileName != null && !fileName.trim().isEmpty())
                i.putExtra("book_name", fileName.trim());
        }
        startActivity(i);
    }

    private void onAudioClick(ServerAudioBook book) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).switchToTab(R.id.nav_audio);
        }
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private void loadBooks() {
        showLoading(true);
        new Thread(() -> {
            List<Book> all = ServerBookLoader.load(getContext());
            String q = query != null ? query.toLowerCase(Locale.ROOT).trim() : "";
            List<Book> filtered = new ArrayList<>();
            if (all != null) {
                for (Book b : all) {
                    if (b == null) continue;
                    String name = b.getName();
                    String searchable = b.getSearchableText();
                    if (name != null && name.toLowerCase(Locale.ROOT).contains(q)) filtered.add(b);
                    else if (searchable != null && searchable.toLowerCase(Locale.ROOT).contains(q)) filtered.add(b);
                }
            }
            List<Book> finalList = filtered;
            new Handler(Looper.getMainLooper()).post(() -> {
                showLoading(false);
                if (bookAdapter != null) bookAdapter.updateBooks(finalList);
                showEmptyOrList(finalList != null ? finalList.size() : 0);
                notifyCount(TYPE_BOOKS, finalList != null ? finalList.size() : 0);
            });
        }).start();
    }

    private void loadAudio() {
        showLoading(true);
        new Thread(() -> {
            List<ServerAudioBook> all = loadAllAudio();
            String q = query != null ? query.trim() : "";
            List<ServerAudioBook> filtered = new ArrayList<>();
            if (all != null) {
                for (ServerAudioBook b : all) {
                    if (b == null) continue;
                    String title = b.getTitle();
                    if (title != null && SearchHelper.matches(title, q)) filtered.add(b);
                }
            }
            List<ServerAudioBook> finalList = filtered;
            new Handler(Looper.getMainLooper()).post(() -> {
                showLoading(false);
                if (audioAdapter != null) audioAdapter.setBooks(finalList);
                showEmptyOrList(finalList != null ? finalList.size() : 0);
                notifyCount(TYPE_AUDIO, finalList != null ? finalList.size() : 0);
            });
        }).start();
    }

    private List<ServerAudioBook> loadAllAudio() {
        try {
            android.app.Activity act = getActivity();
            if (act == null) return new ArrayList<>();
            String base = act.getString(R.string.server_books_base_url);
            if (base == null) base = "";
            base = base.trim();
            if (!base.isEmpty() && !base.endsWith("/")) base += "/";
            String url = base + "audio_list.json";
            List<ServerAudioBook> loaded = null;
            try {
                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
                okhttp3.Request request = new okhttp3.Request.Builder().url(url).build();
                try (okhttp3.Response resp = client.newCall(request).execute()) {
                    if (resp.isSuccessful() && resp.body() != null) {
                        loaded = ServerAudioParser.parseBooks(resp.body().string());
                    }
                }
            } catch (Throwable ignored) {}
            if (loaded == null || loaded.isEmpty()) {
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(act.getAssets().open("audio_list_main.json"), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                    loaded = ServerAudioParser.parseBooks(sb.toString());
                } catch (Throwable ignored) {
                    loaded = ServerAudioParser.demoBooks();
                }
            }
            return loaded != null ? loaded : new ArrayList<>();
        } catch (Throwable t) {
            return new ArrayList<>();
        }
    }

    private void loadVideos() {
        showLoading(true);
        YouTubeSearchLoader.search(getContext(), query, results -> {
            showLoading(false);
            if (videoAdapter != null) videoAdapter.setItems(results != null ? results : new ArrayList<>());
            int c = results != null ? results.size() : 0;
            showEmptyOrList(c);
            notifyCount(TYPE_VIDEOS, c);
        });
    }

    private void showLoading(boolean show) {
        if (loading != null) loading.setVisibility(show ? View.VISIBLE : View.GONE);
        if (recycler != null) recycler.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showEmptyOrList(int count) {
        if (emptyState != null) emptyState.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        if (recycler != null) recycler.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        if (emptyMessage != null) emptyMessage.setText(R.string.search_no_results);
    }

    private void notifyCount(int tabType, int count) {
        if (getActivity() instanceof SearchResultActivity) {
            ((SearchResultActivity) getActivity()).reportCount(tabType, count);
        }
    }

    private void showEmpty() {
        showLoading(false);
        showEmptyOrList(0);
    }

    @Override
    public void onBookClick(Book book) {
        openBook(book);
    }
}
