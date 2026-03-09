package com.swamisachidanand;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;

/**
 * Audio pustako page – Books page jaisa structure: search, Continue Listening, Popular, All.
 */
public class ServerAudioFragment extends Fragment implements AudioBookCardAdapter.OnAudioBookClickListener {

    private static final String PREFS_AUDIO = "audio_prefs";
    private static final String KEY_LAST_PART_ID = "last_part_id_";
    private static final int POPULAR_COUNT = 6;
    private static final int REQUEST_VOICE = 1002;

    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout;
    private androidx.recyclerview.widget.RecyclerView booksRecycler;
    private RecyclerView popularAudioRecycler;
    private View popularAudioSection;
    private android.widget.ProgressBar progressBar;
    private android.widget.TextView errorText;
    private TextInputEditText searchInput;
    private android.widget.ImageView clearSearch;
    private android.widget.ImageView micButton;
    private android.widget.ImageView filterButton;
    private RecyclerView categoryChipsRecycler;
    private CategoryChipAdapter chipAdapter;
    private String selectedCategoryId = "all";
    private Map<String, Integer> durationCache = new HashMap<>();
    private final AudioDurationLoader durationLoader = new AudioDurationLoader();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<ServerAudioBook> allBooks = new ArrayList<>();
    private AudioBookCardAdapter adapter;
    private AudioBookCardAdapter popularAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_server_audio, container, false);

        swipeRefreshLayout = root.findViewById(R.id.audio_swipe_refresh);
        booksRecycler = root.findViewById(R.id.audio_books_card_recycler);
        popularAudioRecycler = root.findViewById(R.id.popular_audio_recycler);
        popularAudioSection = root.findViewById(R.id.popular_audio_section);
        progressBar = root.findViewById(R.id.audio_progress);
        errorText = root.findViewById(R.id.audio_error_text);
        searchInput = root.findViewById(R.id.global_search_input);
        if (searchInput != null) searchInput.setHint(R.string.search_audio_hint);
        clearSearch = root.findViewById(R.id.global_clear_search);
        micButton = root.findViewById(R.id.global_mic_button);
        filterButton = root.findViewById(R.id.global_filter_button);
        if (filterButton != null) filterButton.setVisibility(View.VISIBLE);
        categoryChipsRecycler = root.findViewById(R.id.audio_category_chips_recycler);

        View avatar = root.findViewById(R.id.global_profile_avatar);
        if (avatar != null) {
            avatar.setOnClickListener(v -> {
                android.app.Activity act = getActivity();
                if (act instanceof MainActivity) ((MainActivity) act).openSwamiInfoPage();
            });
        }

        Context ctx = requireContext();
        setupCategoryChips(ctx);
        if (booksRecycler != null) {
            booksRecycler.setLayoutManager(new GridLayoutManager(ctx, 2));
            // setHasFixedSize omitted
            booksRecycler.setItemAnimator(new DefaultItemAnimator());
            booksRecycler.setItemViewCacheSize(20);
            adapter = new AudioBookCardAdapter();
            adapter.setOnAudioBookClickListener(this);
            booksRecycler.setAdapter(adapter);
        }
        if (popularAudioRecycler != null) {
            popularAudioRecycler.setLayoutManager(new LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false));
            // setHasFixedSize omitted
            popularAudioRecycler.setItemAnimator(new DefaultItemAnimator());
            popularAdapter = new AudioBookCardAdapter();
            popularAdapter.setUseCompactLayout(true);
            popularAdapter.setOnAudioBookClickListener(this);
            popularAudioRecycler.setAdapter(popularAdapter);
        }

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadBooksAsync);
        }

        View audioNestedScroll = root.findViewById(R.id.audio_nested_scroll);
        if (audioNestedScroll != null && getActivity() instanceof MainActivity) {
            audioNestedScroll.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).onScrolled(scrollY - oldScrollY);
            });
        }

        if (clearSearch != null) {
            clearSearch.setOnClickListener(v -> {
                if (searchInput != null) searchInput.setText("");
            });
        }

        if (micButton != null) {
            micButton.setOnClickListener(v -> {
                try {
                    android.content.Intent i = new android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "gu-IN");
                    i.putExtra(RecognizerIntent.EXTRA_PROMPT, "ઓડિયો શોધવા બોલો...");
                    startActivityForResult(i, REQUEST_VOICE);
                } catch (Exception e) {
                    android.widget.Toast.makeText(ctx, "Voice search not available", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String q = s != null ? s.toString().trim() : "";
                    if (clearSearch != null) clearSearch.setVisibility(q.isEmpty() ? View.GONE : View.VISIBLE);
                    filterBooks(q);
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
            searchInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    openGlobalSearch();
                    return true;
                }
                return false;
            });
        }

        loadBooksAsync();

        return root;
    }

    private void showLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing() && !loading) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void showError(@Nullable String message) {
        if (errorText != null) {
            if (message == null || message.isEmpty()) {
                errorText.setVisibility(View.GONE);
            } else {
                errorText.setText(message);
                errorText.setVisibility(View.VISIBLE);
            }
        }
    }

    private void loadBooksAsync() {
        showError(null);
        showLoading(true);
        new Thread(() -> {
            List<ServerAudioBook> loaded = new ArrayList<>();
            try {
                android.app.Activity act = getActivity();
                if (act != null) {
                    String base = act.getString(R.string.server_books_base_url);
                    if (base == null) base = "";
                    base = base.trim();
                    if (!base.isEmpty() && !base.endsWith("/")) base += "/";
                    // Audio pustako ka main JSON: audio_list.json
                    String url = base + "audio_list.json";

                    // Pehle server se full list (20+ books) lane ki koshish
                    try {
                        OkHttpClient client = new OkHttpClient();
                        Request request = new Request.Builder().url(url).build();
                        Response response = client.newCall(request).execute();
                        if (response.isSuccessful() && response.body() != null) {
                            String body = response.body().string();
                            loaded = ServerAudioParser.parseBooks(body);
                        }
                    } catch (Throwable ignored) {
                        // Network fail – niche fallback use hoga
                    }

                    // Agar network se kuch na mila to assets se (agar app me copy ho)
                    if (loaded == null || loaded.isEmpty()) {
                        try (BufferedReader r = new BufferedReader(
                                new InputStreamReader(act.getAssets().open("audio_list_main.json"), StandardCharsets.UTF_8))) {
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = r.readLine()) != null) sb.append(line);
                            loaded = ServerAudioParser.parseBooks(sb.toString());
                        } catch (Throwable ignored) {
                            // Fallback: hardcoded small list so page kabhi blank na ho
                            loaded = ServerAudioParser.demoBooks();
                        }
                    }

                    // Yahan par books page (PDF list) se thumbnail link karo + Africa book synthesize karo agar server list se missing ho.
                    try {
                        List<Book> pdfBooks = ServerBookLoader.load(act);
                        HashMap<String, String> thumbByTitle = new HashMap<>();
                        if (pdfBooks != null) {
                            for (Book b : pdfBooks) {
                                if (b == null) continue;
                                String name = b.getName();
                                String tUrl = b.getThumbnailUrl();
                                if (name == null || tUrl == null || tUrl.trim().isEmpty()) continue;
                                String key = name.trim().toLowerCase();
                                if (!key.isEmpty()) thumbByTitle.put(key, tUrl.trim());
                            }
                        }

                        // 1) Agar Africa book server list me missing hai to local parts JSON se synthesize karo
                        try {
                            boolean hasAfrica = false;
                            if (loaded != null) {
                                for (ServerAudioBook ab : loaded) {
                                    if (ab == null) continue;
                                    String id = ab.getId();
                                    String t = ab.getTitle();
                                    if ("africa_sansmaran".equals(id) ||
                                            (t != null && normalizeTitle(t).contains(normalizeTitle("આફ્રિકા-પ્રવાસનાં સંસ્મરણો")))) {
                                        hasAfrica = true;
                                        break;
                                    }
                                }
                            }
                            if (!hasAfrica) {
                                List<ServerAudioPart> africaParts = new ArrayList<>();
                                try (BufferedReader r = new BufferedReader(
                                        new InputStreamReader(act.getAssets().open("africa_sansmaran_parts.json"), StandardCharsets.UTF_8))) {
                                    StringBuilder sb = new StringBuilder();
                                    String line;
                                    while ((line = r.readLine()) != null) sb.append(line);
                                    JSONArray arr = new JSONArray(sb.toString());
                                    for (int i = 0; i < arr.length(); i++) {
                                        org.json.JSONObject p = arr.optJSONObject(i);
                                        if (p == null) continue;
                                        String pid = p.optString("id", "");
                                        String ptitle = p.optString("title", "");
                                        String purl = p.optString("url", "");
                                        africaParts.add(new ServerAudioPart(pid, ptitle, purl));
                                    }
                                }
                                String africaTitle = "આફ્રિકા-પ્રવાસનાં સંસ્મરણો";
                                // Thumbnail strictly server se: thumbnails/આફ્રિકા-પ્રવાસનાં સંસ્મરણો.jpg (encoded), same pattern as books page.
                                String africaThumb = null;
                                try {
                                    String pdfName = "આફ્રિકા-પ્રવાસનાં સંસ્મરણો.pdf";
                                    String thumbName = pdfName.replace(".pdf", ".jpg");
                                    String encodedThumb = URLEncoder.encode(thumbName, StandardCharsets.UTF_8.name()).replace("+", "%20");
                                    africaThumb = base + "thumbnails/" + encodedThumb;
                                } catch (Exception ignored3) {
                                }
                                if (loaded == null) loaded = new ArrayList<>();
                                loaded.add(new ServerAudioBook("africa_sansmaran", africaTitle, africaParts, africaThumb));
                            }
                        } catch (Throwable ignored2) {
                            // Africa synth fail hua to bhi baki list normal chale
                        }

                        // 2) Bachi hui sab books ke liye, jinka thumbnailUrl empty hai, Books se thumbnail map karo
                        if (loaded != null && !loaded.isEmpty()) {
                            List<ServerAudioBook> fixed = new ArrayList<>(loaded.size());
                            for (ServerAudioBook ab : loaded) {
                                if (ab == null) continue;
                                String thumb = ab.getThumbnailUrl();
                                if (thumb == null || thumb.isEmpty()) {
                                    String title = ab.getTitle();
                                    String fromBooks = null;
                                    // Africa audio book: agar server list me aayi ho bina thumbnailUrl ke,
                                    // to direct books/thumbnail naming se hi thumbnail set karo so that
                                    // audio card bhi Books page jaisa cover dikhaye.
                                    if ("africa_sansmaran".equals(ab.getId())) {
                                        try {
                                            String pdfName = "આફ્રિકા-પ્રવાસનાં સંસ્મરણો.pdf";
                                            String thumbName = pdfName.replace(".pdf", ".jpg");
                                            String encodedThumb = URLEncoder.encode(thumbName, StandardCharsets.UTF_8.name()).replace("+", "%20");
                                            fromBooks = base + "thumbnails/" + encodedThumb;
                                        } catch (Exception ignored4) {
                                        }
                                    }
                                    if (title != null) {
                                        String tKey = title.trim().toLowerCase();
                                        if (fromBooks == null || fromBooks.isEmpty()) {
                                            fromBooks = thumbByTitle.get(tKey);
                                        }
                                        if ((fromBooks == null || fromBooks.isEmpty()) && pdfBooks != null) {
                                            String normAudio = normalizeTitle(title);
                                            for (Book b : pdfBooks) {
                                                if (b == null || b.getName() == null) continue;
                                                String normBook = normalizeTitle(b.getName());
                                                if (!normBook.isEmpty() && !normAudio.isEmpty()
                                                        && (normBook.equals(normAudio) || normBook.contains(normAudio) || normAudio.contains(normBook))) {
                                                    String tUrl = b.getThumbnailUrl();
                                                    if (tUrl != null && !tUrl.trim().isEmpty()) {
                                                        fromBooks = tUrl.trim();
                                                    }
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    if (fromBooks != null && !fromBooks.isEmpty()) {
                                        fixed.add(new ServerAudioBook(ab.getId(), ab.getTitle(), ab.getParts(), fromBooks));
                                        continue;
                                    }
                                }
                                fixed.add(ab);
                            }
                            loaded = fixed;
                        }
                    } catch (Throwable ignored) {
                        // Agar mapping fail ho jaye to bhi audio list normal chale
                    }
                }
            } catch (Throwable ignored) {
                // handled below
            }
            // Name-wise sort (title ascending) so list stable, Play Store jaisa
            if (loaded != null && !loaded.isEmpty()) {
                java.util.Collections.sort(loaded, (a, b) -> {
                    String ta = a != null && a.getTitle() != null ? a.getTitle() : "";
                    String tb = b != null && b.getTitle() != null ? b.getTitle() : "";
                    return ta.compareToIgnoreCase(tb);
                });
            }
            List<ServerAudioBook> finalLoaded = loaded;
            Map<String, Integer> progressMap = loadProgressMap(finalLoaded);
            mainHandler.post(() -> {
                showLoading(false);
                allBooks.clear();
                if (finalLoaded != null) {
                    allBooks.addAll(finalLoaded);
                }
                loadPopular(allBooks, progressMap);
                if (adapter != null) {
                    adapter.setProgressMap(progressMap);
                    adapter.setDurationCache(durationCache);
                }
                applyFilters();
                fetchDurationsIfNeeded(finalLoaded);
                if (booksRecycler != null) {
                    booksRecycler.setVisibility(allBooks.isEmpty() ? View.GONE : View.VISIBLE);
                }
                if (allBooks.isEmpty()) {
                    showError(getString(R.string.audio_empty));
                } else {
                    showError(null);
                }
            });
        }).start();
    }

    /** Normalize Gujarati/English titles for loose matching (remove extra spaces, some punctuation). */
    private static String normalizeTitle(String s) {
        if (s == null) return "";
        String out = s.toLowerCase().trim();
        out = out.replaceAll("[\\u2013\\u2014\\-]+", " "); // dashes to space
        out = out.replaceAll("[\\s]+", " ");
        return out;
    }

    private Map<String, Integer> loadProgressMap(List<ServerAudioBook> books) {
        Map<String, Integer> map = new HashMap<>();
        if (books == null) return map;
        try {
            android.app.Activity act = getActivity();
            if (act == null) return map;
            SharedPreferences prefs = act.getSharedPreferences(PREFS_AUDIO, Context.MODE_PRIVATE);
            for (ServerAudioBook b : books) {
                if (b == null || b.getId() == null) continue;
                String lastId = prefs.getString(KEY_LAST_PART_ID + b.getId(), null);
                if (lastId == null) continue;
                List<ServerAudioPart> parts = b.getParts();
                if (parts == null || parts.isEmpty()) continue;
                int idx = -1;
                for (int i = 0; i < parts.size(); i++) {
                    if (lastId.equals(parts.get(i).getId())) {
                        idx = i;
                        break;
                    }
                }
                if (idx >= 0 && idx < parts.size() - 1) {
                    int pct = (int) ((idx + 1) * 100.0 / parts.size());
                    if (pct > 0 && pct < 100) map.put(b.getId(), pct);
                }
            }
        } catch (Exception ignored) {}
        return map;
    }

    private void fetchDurationsIfNeeded(List<ServerAudioBook> books) {
        if (books == null) return;
        boolean needsFetch = false;
        for (ServerAudioBook b : books) {
            if (b != null && b.getTotalDurationSeconds() <= 0) {
                needsFetch = true;
                break;
            }
        }
        if (!needsFetch) return;
        durationLoader.loadDurations(books, result -> {
            if (result == null) return;
            durationCache.putAll(result);
            if (adapter != null) adapter.setDurationCache(durationCache);
            if (popularAdapter != null) popularAdapter.setDurationCache(durationCache);
            if (adapter != null) adapter.notifyDataSetChanged();
            if (popularAdapter != null) popularAdapter.notifyDataSetChanged();
        });
    }

    private void loadPopular(List<ServerAudioBook> books, Map<String, Integer> progressMap) {
        if (popularAdapter == null || popularAudioSection == null || books == null) return;
        List<ServerAudioBook> popular = new ArrayList<>();
        if (books.size() <= POPULAR_COUNT) {
            popular.addAll(books);
            Collections.reverse(popular);
        } else {
            for (int i = books.size() - 1; i >= books.size() - POPULAR_COUNT; i--) {
                popular.add(books.get(i));
            }
        }
        popularAdapter.setProgressMap(progressMap);
        popularAdapter.setDurationCache(durationCache);
        popularAdapter.setBooks(popular, true);
        popularAudioSection.setVisibility(popular.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void setupCategoryChips(Context ctx) {
        if (categoryChipsRecycler == null) return;
        List<String> labels = new ArrayList<>();
        labels.add("બધાં");
        labels.add("ભક્તિ");
        labels.add("યાત્રા");
        labels.add("જીવન");
        labels.add("ઉપદેશ");
        labels.add("લોકપ્રિય");
        List<String> ids = new ArrayList<>();
        ids.add("all");
        ids.add("Bhakti");
        ids.add("Yatra");
        ids.add("Jeevan");
        ids.add("Updesh");
        ids.add("popular");
        chipAdapter = new CategoryChipAdapter(labels, ids);
        chipAdapter.setListener(catId -> {
            selectedCategoryId = catId;
            applyFilters();
        });
        categoryChipsRecycler.setLayoutManager(new LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false));
        categoryChipsRecycler.setAdapter(chipAdapter);
        if (filterButton != null) {
            filterButton.setOnClickListener(v -> {
                if (chipAdapter != null) chipAdapter.setSelectedIndex(0);
                selectedCategoryId = "all";
                applyFilters();
            });
        }
    }

    private void applyFilters() {
        String query = searchInput != null ? searchInput.getText().toString().trim() : "";
        filterBooks(query);
    }

    private static String detectAudioCategory(String title) {
        if (title == null) return "Updesh";
        String lower = title.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("ભક્તિ") || lower.contains("bhakti") || lower.contains("ભજન") || lower.contains("bhajan") ||
            lower.contains("ભાગવત") || lower.contains("bhagwat") || lower.contains("વિષ્ણુ") || lower.contains("vishnu") ||
            lower.contains("રામાયણ") || lower.contains("ramayan") || lower.contains("રામ") || lower.contains("ram") ||
            lower.contains("કૃષ્ણ") || lower.contains("krishna") || lower.contains("ભર્તૃહરિ") || lower.contains("bhartrihari") ||
            lower.contains("શતક") || lower.contains("shata") || lower.contains("સહસ્રનામ") || lower.contains("sahasranam")) {
            return "Bhakti";
        }
        if (lower.contains("યાત્રા") || lower.contains("yatra") || lower.contains("પ્રવાસ") || lower.contains("travel") ||
            lower.contains("પ્રવાસનાં") || lower.contains("પ્રવાસની") || lower.contains("તીર્થ") || lower.contains("tirth") ||
            lower.contains("મુલાકાત") || lower.contains("mulakat") || lower.contains("આફ્રિકા") || lower.contains("africa") ||
            lower.contains("યુરોપ") || lower.contains("europe") || lower.contains("ટર્કી") || lower.contains("turkey") ||
            lower.contains("ઈજિપ્ત") || lower.contains("egypt") || lower.contains("આંદામાન") || lower.contains("andaman")) {
            return "Yatra";
        }
        if (lower.contains("જીવન") || lower.contains("jeevan") || lower.contains("ચરિત્ર") || lower.contains("charitra") ||
            lower.contains("જીવનકથા") || lower.contains("jeevankatha") || lower.contains("અનુભવ") || lower.contains("anubhav") ||
            lower.contains("બાયપાસ") || lower.contains("bypass")) {
            return "Jeevan";
        }
        return "Updesh";
    }

    private List<ServerAudioBook> filterByCategory(List<ServerAudioBook> list) {
        if ("all".equals(selectedCategoryId)) return list;
        if ("popular".equals(selectedCategoryId)) {
            if (list.size() <= POPULAR_COUNT) {
                List<ServerAudioBook> out = new ArrayList<>(list);
                Collections.reverse(out);
                return out;
            }
            List<ServerAudioBook> out = new ArrayList<>();
            for (int i = list.size() - 1; i >= list.size() - POPULAR_COUNT; i--) {
                out.add(list.get(i));
            }
            return out;
        }
        List<ServerAudioBook> out = new ArrayList<>();
        for (ServerAudioBook b : list) {
            if (b != null && selectedCategoryId.equals(detectAudioCategory(b.getTitle()))) {
                out.add(b);
            }
        }
        return out;
    }

    private void filterBooks(String query) {
        if (adapter == null) return;
        List<ServerAudioBook> source = filterByCategory(allBooks);
        if (query == null || query.trim().isEmpty()) {
            adapter.setBooks(source, true);
            return;
        }
        String q = query.trim();
        List<ServerAudioBook> filtered = new ArrayList<>();
        for (ServerAudioBook b : source) {
            if (b == null) continue;
            String t = b.getTitle() != null ? b.getTitle() : "";
            if (SearchHelper.matches(t, q)) filtered.add(b);
        }
        adapter.setBooks(filtered, true);
    }

    private void openGlobalSearch() {
        String q = searchInput != null && searchInput.getText() != null ? searchInput.getText().toString().trim() : "";
        if (q.isEmpty()) return;
        android.content.Intent i = new android.content.Intent(requireContext(), SearchResultActivity.class);
        i.putExtra(SearchResultActivity.EXTRA_QUERY, q);
        startActivity(i);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_VOICE && resultCode == android.app.Activity.RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty() && searchInput != null) {
                String spoken = results.get(0);
                searchInput.setText(spoken);
                if (!spoken.trim().isEmpty()) openGlobalSearch();
            }
        }
    }

    @Override
    public void onAudioBookClick(ServerAudioBook book) {
        if (book == null || getActivity() == null) return;
        Fragment f = AudioBookDetailFragment.newInstance(book);
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, f)
                .addToBackStack(null)
                .commit();
    }
}

