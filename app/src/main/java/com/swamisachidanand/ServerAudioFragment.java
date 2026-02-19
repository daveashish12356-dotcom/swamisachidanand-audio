package com.swamisachidanand;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import androidx.appcompat.app.AlertDialog;

/**
 * Audio books list: sirf app assets (audio_list_fallback.json) se. Har book = 1 card (title, thumbnail, parts).
 * Server/auto mode nahi – list app me hi fixed.
 */
public class ServerAudioFragment extends Fragment implements AudioBookCardAdapter.OnAudioBookClickListener {

    private static final String TAG = "ServerAudioFragment";
    private static final int REQUEST_CODE_VOICE_SEARCH = 1002;

    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private TextView errorText;
    private TextView bookCountText;
    private RecyclerView cardRecycler;
    private AudioBookCardAdapter cardAdapter;
    private com.google.android.material.textfield.TextInputEditText searchInput;
    private ImageView clearSearch, micButton;
    private List<ServerAudioBook> allAudioBooks = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_server_audio, container, false);
        swipeRefresh = root.findViewById(R.id.audio_swipe_refresh);
        progressBar = root.findViewById(R.id.audio_progress);
        errorText = root.findViewById(R.id.audio_error_text);
        bookCountText = root.findViewById(R.id.audio_book_count);
        cardRecycler = root.findViewById(R.id.audio_books_card_recycler);
        searchInput = root.findViewById(R.id.audio_search_input);
        clearSearch = root.findViewById(R.id.audio_clear_search);
        micButton = root.findViewById(R.id.audio_mic_button);

        int spanCount = getSpanCountForScreen();
        cardRecycler.setLayoutManager(new GridLayoutManager(requireContext(), spanCount));
        cardAdapter = new AudioBookCardAdapter();
        cardAdapter.setOnAudioBookClickListener(this);
        cardRecycler.setAdapter(cardAdapter);

        setupSearchBar();
        swipeRefresh.setOnRefreshListener(this::loadAudioList);
        loadAudioList();
        return root;
    }

    private int getSpanCountForScreen() {
        if (getContext() == null) return 2;
        float dp = getResources().getDisplayMetrics().density;
        float widthDp = getResources().getDisplayMetrics().widthPixels / dp;
        if (widthDp >= 600) return 3;
        if (widthDp >= 480) return 2;
        return 2;
    }

    /**
     * Pehle server se audio_list.json fetch karo. Agar mil jaye to usi list use karo (thumbnail, name, parts sab server se).
     * Server par nayi book daloge to app refresh/pull par nayi book le lega. Fail hone par assets fallback use karo.
     */
    private void loadAudioList() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (errorText != null) errorText.setVisibility(View.GONE);
        android.content.Context ctx = getContext();
        if (ctx == null) {
            applyFallbackAndShow();
            return;
        }
        new Thread(() -> {
            List<ServerAudioBook> books = fetchAudioListFromServer(ctx);
            if (books != null && !books.isEmpty()) {
                // Parse me koi book chut jaye to ensure se add karo – 100+ books par bhi sab dikhen
                ensureRamayanUpsanharValmikiUpanishadInList(books);
                List<Book> serverBooks = ServerBookLoader.load(ctx);
                for (ServerAudioBook b : books) {
                    if (b.getThumbnailUrl() == null || b.getThumbnailUrl().isEmpty()) {
                        String thumb = getServerThumbnailUrlForId(ctx, b.getId());
                        if (thumb == null) thumb = findThumbnailUrlForAudioBook(ctx, b.getId(), b.getTitle(), serverBooks);
                        if (thumb != null) {
                            int idx = books.indexOf(b);
                            books.set(idx, new ServerAudioBook(b.getId(), b.getTitle(), b.getParts(), thumb));
                        }
                    }
                }
                Collections.sort(books, new GujaratiAlphabeticalComparator());
                moveTestBookToTop(books);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> applyBooksAndHideProgress(books, true));
                } else {
                    applyFallbackAndShow();
                }
            } else {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(this::applyFallbackAndShow);
                } else {
                    applyFallbackAndShow();
                }
            }
        }).start();
    }

    /** Pehle main (raw) – sahi order. Fail hoy to baseUrl (gh-pages). Dono par ab sahi file. */
    private List<ServerAudioBook> fetchAudioListFromServer(android.content.Context ctx) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .cache(null)
                .build();
        String urlMain = "https://raw.githubusercontent.com/daveashish12356-dotcom/swamisachidanand-audio/main/audio_list.json";
        List<ServerAudioBook> list = fetchAudioListFromUrl(ctx, client, urlMain);
        if (list != null && !list.isEmpty()) {
            Log.d(TAG, "Server audio list OK from main");
            return list;
        }
        String baseUrl = ctx.getString(R.string.server_books_base_url);
        if (baseUrl != null && !baseUrl.isEmpty()) {
            baseUrl = baseUrl.trim();
            if (!baseUrl.endsWith("/")) baseUrl += "/";
            list = fetchAudioListFromUrl(ctx, client, baseUrl + "audio_list.json");
            if (list != null && !list.isEmpty()) {
                Log.d(TAG, "Server audio list OK from baseUrl");
                return list;
            }
        }
        return null;
    }

    private List<ServerAudioBook> fetchAudioListFromUrl(android.content.Context ctx, OkHttpClient client, String url) {
        try {
            // Force fresh fetch - no cache headers
            Request request = new Request.Builder()
                    .url(url)
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .header("Expires", "0")
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.w(TAG, "audio_list.json HTTP " + response.code() + " for " + url);
                    return null;
                }
                String json = response.body().string();
                if (json == null || json.isEmpty()) return null;
                if (json.startsWith("\uFEFF")) json = json.substring(1);
                List<Book> serverBooks = ServerBookLoader.load(ctx);
                return parseBooks(ctx, json, serverBooks);
            }
        } catch (Exception e) {
            Log.w(TAG, "fetch failed " + url + ": " + e.getMessage());
            return null;
        }
    }

    /** Server check dialog disabled – refresh/load par box na aave. */
    private void runServerCheckAndShowResult() {
        // No dialog on refresh – user requested no pop-up
    }

    private static void moveTestBookToTop(List<ServerAudioBook> books) {
        if (books == null || books.size() < 2) return;
        for (int i = 0; i < books.size(); i++) {
            if ("test_book".equals(books.get(i).getId())) {
                ServerAudioBook t = books.remove(i);
                books.add(0, t);
                break;
            }
        }
    }

    /** Fallback: assets se list + ensure aavego/valmiki/upanishad; then show on UI. Test book ko upar lao. */
    private void applyFallbackAndShow() {
        List<ServerAudioBook> books = loadFallbackFromAssets();
        ensureAavegoInList(books);
        ensureValmikiRamayanSarInList(books);
        ensureUpanishadKathaoChintanInList(books);
        ensureKrantikathaoInList(books);
        ensureRamayanUpsanharValmikiUpanishadInList(books);
        Collections.sort(books, new GujaratiAlphabeticalComparator());
        moveTestBookToTop(books);
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> applyBooksAndHideProgress(books, false));
        } else {
            applyBooksAndHideProgress(books, false);
        }
    }

    private void applyBooksAndHideProgress(List<ServerAudioBook> books, boolean fromServer) {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
        int count = books != null ? books.size() : 0;
        Log.d(TAG, String.format(Locale.US, "Audio books count: %d (fromServer=%b)", count, fromServer));
        if (bookCountText != null) {
            bookCountText.setText(String.format(Locale.getDefault(), "સર્વર પર %d પુસ્તકો", count));
            bookCountText.setVisibility(View.VISIBLE);
        }
        allAudioBooks = new ArrayList<>(books != null ? books : new ArrayList<>());
        if (books != null && !books.isEmpty()) {
            cardAdapter.setBooks(books, fromServer);
            if (cardRecycler != null) cardRecycler.setVisibility(View.VISIBLE);
            if (errorText != null) errorText.setVisibility(View.GONE);
            if (getContext() != null) {
                if (fromServer) {
                    boolean hasTestBook = false;
                    for (ServerAudioBook b : books) {
                        if ("test_book".equals(b.getId())) { hasTestBook = true; break; }
                    }
                    String msg = hasTestBook
                            ? "સર્વરથી " + count + " પુસ્તકો લોડ થયાં – ટેસ્ટ બુક દેખાશે"
                            : "સર્વરથી " + count + " પુસ્તકો લોડ થયાં";
                    android.widget.Toast.makeText(getContext(), msg, android.widget.Toast.LENGTH_SHORT).show();
                } else {
                    android.widget.Toast.makeText(getContext(), "લોકલ લિસ્ટ – " + count + " પુસ્તકો દેખાશે (ટેસ્ટ બુક ઉપર)", android.widget.Toast.LENGTH_LONG).show();
                }
            }
            // Server check dialog not shown on refresh/load – user requested no pop-up
        } else {
            if (errorText != null) {
                errorText.setText(R.string.audio_error);
                errorText.setVisibility(View.VISIBLE);
            }
        }
    }
    
    private void setupSearchBar() {
        if (searchInput != null) {
            searchInput.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s.toString().trim();
                    if (clearSearch != null) clearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                    filterAudioBooks(query);
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }

        if (clearSearch != null) {
            clearSearch.setOnClickListener(v -> {
                if (searchInput != null) {
                    searchInput.setText("");
                    clearSearch.setVisibility(View.GONE);
                }
            });
        }

        if (micButton != null) {
            micButton.setOnClickListener(v -> startVoiceSearch());
        }
    }
    
    private void startVoiceSearch() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "gu-IN");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "gu-IN");
            intent.putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, new String[]{"gu-IN", "hi-IN", "en-IN"});
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "ઓડિયો પુસ્તકનું નામ કહો...");
            startActivityForResult(intent, REQUEST_CODE_VOICE_SEARCH);
        } catch (Exception e) {
            android.widget.Toast.makeText(getContext(), "Voice search not available", android.widget.Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Voice search error", e);
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_VOICE_SEARCH && resultCode == Activity.RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spoken = results.get(0);
                if (searchInput != null) searchInput.setText(spoken);
                filterAudioBooks(spoken);
            }
        }
    }
    
    private void filterAudioBooks(String query) {
        if (cardAdapter == null || allAudioBooks == null) return;
        
        List<ServerAudioBook> filtered = new ArrayList<>();
        if (query.isEmpty()) {
            filtered.addAll(allAudioBooks);
        } else {
            String queryLower = query.trim().toLowerCase();
            for (ServerAudioBook book : allAudioBooks) {
                String title = book.getTitle() != null ? book.getTitle().toLowerCase() : "";
                if (title.contains(queryLower)) {
                    filtered.add(book);
                }
            }
        }
        
        cardAdapter.setBooks(filtered);
    }
    
    /**
     * Gujarati alphabetical comparator: sorts books by title in Gujarati order.
     * Order: અ, આ, ઇ, ઈ, ઉ, ઊ, એ, ઐ, ઓ, ઔ, ક, ખ, ગ, ઘ, ઙ, ચ, છ, જ, ઝ, ઞ, ટ, ઠ, ડ, ઢ, ણ, ત, થ, દ, ધ, ન, પ, ફ, બ, ભ, મ, ય, ર, લ, વ, શ, ષ, સ, હ
     */
    private static class GujaratiAlphabeticalComparator implements Comparator<ServerAudioBook> {
        // Gujarati alphabet order mapping
        private static final String GUJARATI_ORDER = "અઆઇઈઉઊએઐઓઔકખગઘઙચછજઝઞટઠડઢણતથદધનપફબભમયરલવશષસહ";
        
        @Override
        public int compare(ServerAudioBook a, ServerAudioBook b) {
            if (a == null && b == null) return 0;
            if (a == null) return 1;
            if (b == null) return -1;
            
            String titleA = a.getTitle() != null ? a.getTitle().trim() : "";
            String titleB = b.getTitle() != null ? b.getTitle().trim() : "";
            
            if (titleA.isEmpty() && titleB.isEmpty()) return 0;
            if (titleA.isEmpty()) return 1;
            if (titleB.isEmpty()) return -1;
            
            // Compare character by character
            int minLen = Math.min(titleA.length(), titleB.length());
            for (int i = 0; i < minLen; i++) {
                char charA = titleA.charAt(i);
                char charB = titleB.charAt(i);
                
                int posA = GUJARATI_ORDER.indexOf(charA);
                int posB = GUJARATI_ORDER.indexOf(charB);
                
                // If both are Gujarati characters, compare by position
                if (posA >= 0 && posB >= 0) {
                    if (posA != posB) {
                        return Integer.compare(posA, posB);
                    }
                } else if (posA >= 0) {
                    // Gujarati comes before non-Gujarati
                    return -1;
                } else if (posB >= 0) {
                    return 1;
                } else {
                    // Both non-Gujarati, use default comparison
                    if (charA != charB) {
                        return Character.compare(charA, charB);
                    }
                }
            }
            
            // If all characters match up to minLen, shorter string comes first
            return Integer.compare(titleA.length(), titleB.length());
        }
    }

    /** આવેગો અને લાગણીઓ: agar list me nahi to 1 card add karo – thumbnail = PDF (adapter), 33 parts = server URLs. */
    private void ensureAavegoInList(List<ServerAudioBook> books) {
        if (books == null) return;
        for (ServerAudioBook b : books) {
            if ("aavego_laganiyo".equals(b.getId())) return;
        }
        // Server se 33 parts: GitHub release aavego_laganiyo/1.mp3 ... 33.mp3
        String base = "https://github.com/daveashish12356-dotcom/swamisachidanand-audio/releases/download/aavego_laganiyo/";
        List<ServerAudioPart> parts = new ArrayList<>();
        for (int i = 1; i <= 33; i++) {
            parts.add(new ServerAudioPart(String.valueOf(i), "ભાગ " + i, base + i + ".mp3"));
        }
        android.content.Context ctx = getContext();
        String thumbUrl = ctx != null ? getServerThumbnailUrlForId(ctx, "aavego_laganiyo") : null;
        books.add(new ServerAudioBook("aavego_laganiyo", "આવેગો અને લાગણીઓ", parts, thumbUrl));
        Log.d(TAG, "ensureAavegoInList: added aavego_laganiyo card (33 parts from server)");
    }

    /** વાલ્મીકિ-રામાયણ-સાર: agar list me nahi to add karo – 110 parts, release valmiki_ramayan_sar. */
    private void ensureValmikiRamayanSarInList(List<ServerAudioBook> books) {
        if (books == null) return;
        for (ServerAudioBook b : books) {
            if ("valmiki_ramayan_sar".equals(b.getId())) return;
        }
        String base = "https://github.com/daveashish12356-dotcom/swamisachidanand-audio/releases/download/valmiki_ramayan_sar/";
        List<ServerAudioPart> parts = new ArrayList<>();
        for (int i = 1; i <= 110; i++) {
            parts.add(new ServerAudioPart(String.valueOf(i), "ભાગ " + i, base + i + ".mp3"));
        }
        android.content.Context ctx = getContext();
        String thumbUrl = ctx != null ? getServerThumbnailUrlForId(ctx, "valmiki_ramayan_sar") : null;
        books.add(new ServerAudioBook("valmiki_ramayan_sar", "વાલ્મીકિ-રામાયણ-સાર", parts, thumbUrl));
        Log.d(TAG, "ensureValmikiRamayanSarInList: added વાલ્મીકિ-રામાયણ-સાર (110 parts)");
    }

    /** ઉપનિષદોની કથાઓ અને ચિંતન: agar list me nahi to add karo – release upanishad_kathao_chintan. */
    private void ensureUpanishadKathaoChintanInList(List<ServerAudioBook> books) {
        if (books == null) return;
        for (ServerAudioBook b : books) {
            if ("upanishad_kathao_chintan".equals(b.getId())) return;
        }
        String base = "https://github.com/daveashish12356-dotcom/swamisachidanand-audio/releases/download/upanishad_kathao_chintan/";
        List<ServerAudioPart> parts = new ArrayList<>();
        int partCount = 55;
        for (int i = 1; i <= partCount; i++) {
            parts.add(new ServerAudioPart(String.valueOf(i), "ભાગ " + i, base + i + ".mp3"));
        }
        android.content.Context ctx = getContext();
        String thumbUrl = ctx != null ? getServerThumbnailUrlForId(ctx, "upanishad_kathao_chintan") : null;
        books.add(new ServerAudioBook("upanishad_kathao_chintan", "ઉપનિષદોની કથાઓ અને ચિંતન", parts, thumbUrl));
        Log.d(TAG, "ensureUpanishadKathaoChintanInList: added (" + parts.size() + " parts)");
    }

    /** ક્રાંતિકથાઓ: agar list me nahi to add karo – release krantikathao. */
    private void ensureKrantikathaoInList(List<ServerAudioBook> books) {
        if (books == null) return;
        for (ServerAudioBook b : books) {
            if ("krantikathao".equals(b.getId())) return;
        }
        String base = "https://github.com/daveashish12356-dotcom/swamisachidanand-audio/releases/download/krantikathao/";
        List<ServerAudioPart> parts = new ArrayList<>();
        int partCount = 84;
        for (int i = 1; i <= partCount; i++) {
            parts.add(new ServerAudioPart(String.valueOf(i), "ભાગ " + i, base + i + ".mp3"));
        }
        android.content.Context ctx = getContext();
        String thumbUrl = ctx != null ? getServerThumbnailUrlForId(ctx, "krantikathao") : null;
        books.add(new ServerAudioBook("krantikathao", "ક્રાંતિકથાઓ", parts, thumbUrl));
        Log.d(TAG, "ensureKrantikathaoInList: added ક્રાંતિકથાઓ (" + parts.size() + " parts)");
    }

    /** ramayan_chintan, upsanhar, valmiki, upanishad – parse me chut jaye to add karo. 100+ books par bhi sab dikhen. */
    private void ensureRamayanUpsanharValmikiUpanishadInList(List<ServerAudioBook> books) {
        ensureRamayanChintanInList(books);
        ensureUpsanharInList(books);
        ensureValmikiRamayanSarInList(books);
        ensureUpanishadKathaoChintanInList(books);
    }

    private void ensureRamayanChintanInList(List<ServerAudioBook> books) {
        if (books == null) return;
        for (ServerAudioBook b : books) { if ("ramayan_chintan".equals(b.getId())) return; }
        String base = "https://github.com/daveashish12356-dotcom/swamisachidanand-audio/releases/download/ramayan_chintan/";
        List<ServerAudioPart> parts = new ArrayList<>();
        for (int i = 1; i <= 66; i++) parts.add(new ServerAudioPart(String.valueOf(i), "ભાગ " + i, base + i + ".mp3"));
        android.content.Context ctx = getContext();
        String thumbUrl = ctx != null ? getServerThumbnailUrlForId(ctx, "ramayan_chintan") : null;
        books.add(new ServerAudioBook("ramayan_chintan", "રામાયણનું ચિંતન", parts, thumbUrl));
        Log.d(TAG, "ensureRamayanChintanInList: added");
    }

    private void ensureUpsanharInList(List<ServerAudioBook> books) {
        if (books == null) return;
        for (ServerAudioBook b : books) { if ("upsanhar".equals(b.getId())) return; }
        String base = "https://github.com/daveashish12356-dotcom/swamisachidanand-audio/releases/download/upsanhar/";
        List<ServerAudioPart> parts = new ArrayList<>();
        for (int i = 1; i <= 45; i++) parts.add(new ServerAudioPart(String.valueOf(i), "ભાગ " + i, base + i + ".mp3"));
        android.content.Context ctx = getContext();
        String thumbUrl = ctx != null ? getServerThumbnailUrlForId(ctx, "upsanhar") : null;
        books.add(new ServerAudioBook("upsanhar", "ઉપસંહાર", parts, thumbUrl));
        Log.d(TAG, "ensureUpsanharInList: added");
    }

    private List<ServerAudioBook> loadFallbackFromAssets() {
        List<ServerAudioBook> list = new ArrayList<>();
        try {
            android.content.Context ctx = getContext();
            if (ctx == null) return list;
            List<Book> serverBooks = ServerBookLoader.load(ctx);
            java.io.InputStream is = ctx.getAssets().open("audio_list_fallback.json");
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }
            String json = sb.toString();
            if (json.startsWith("\uFEFF")) json = json.substring(1); // BOM strip
            list = parseBooks(ctx, json, serverBooks);
        } catch (IOException e) {
            Log.e(TAG, "Fallback load failed", e);
        }
        return list;
    }

    /** Har book ke parts 1, 2, 3 ... N order me – server par galat order ho to bhi sahi. */
    private static List<ServerAudioPart> sortPartsByNumber(List<ServerAudioPart> parts) {
        if (parts == null || parts.isEmpty()) return new ArrayList<>();
        List<ServerAudioPart> list = new ArrayList<>(parts);
        Collections.sort(list, (a, b) -> {
            int na = parsePartNum(a != null ? a.getId() : null);
            int nb = parsePartNum(b != null ? b.getId() : null);
            if (na >= 0 && nb >= 0) return Integer.compare(na, nb);
            if (na >= 0) return -1;
            if (nb >= 0) return 1;
            String sa = a != null ? a.getId() : "";
            String sb = b != null ? b.getId() : "";
            return (sa != null ? sa : "").compareTo(sb != null ? sb : "");
        });
        return list;
    }

    private static int parsePartNum(String id) {
        if (id == null || id.isEmpty()) return -1;
        try { return Integer.parseInt(id.trim()); } catch (NumberFormatException e) { return -1; }
    }

    /** Audio page: jis book ka audio hai, ussi book ka thumbnail (PDF/Books page jaiso) dikhao – 56 books list se title match karke. */
    private List<ServerAudioBook> parseBooks(android.content.Context ctx, String jsonStr, List<Book> serverBooks) {
        List<ServerAudioBook> list = new ArrayList<>();
        try {
            if (jsonStr == null || jsonStr.isEmpty()) return list;
            if (jsonStr.startsWith("\uFEFF")) jsonStr = jsonStr.substring(1);
            JSONObject root = new JSONObject(jsonStr);
            JSONArray booksArr = root.optJSONArray("books");
            if (booksArr == null) {
                Log.w(TAG, "parseBooks: books array is null");
                return list;
            }
            Log.d(TAG, "parseBooks: found " + booksArr.length() + " books in JSON");
            for (int i = 0; i < booksArr.length(); i++) {
                JSONObject b = null;
                try {
                    b = booksArr.optJSONObject(i);
                    if (b == null) continue;
                    String id = b.optString("id", "").trim();
                    String title = b.optString("title", "").trim();
                    String thumbnailUrl = b.has("thumbnailUrl") ? b.optString("thumbnailUrl", null) : null;
                    if (thumbnailUrl == null || thumbnailUrl.isEmpty()) {
                        thumbnailUrl = getServerThumbnailUrlForId(ctx, id);
                    }
                    if (thumbnailUrl == null || thumbnailUrl.isEmpty()) {
                        thumbnailUrl = findThumbnailUrlForAudioBook(ctx, id, title, serverBooks);
                    }
                    JSONArray partsArr = b.optJSONArray("parts");
                    List<ServerAudioPart> parts = new ArrayList<>();
                    if (partsArr != null) {
                        for (int j = 0; j < partsArr.length(); j++) {
                            JSONObject p = partsArr.optJSONObject(j);
                            if (p == null) continue;
                            parts.add(new ServerAudioPart(
                                    p.optString("id", ""),
                                    p.optString("title", ""),
                                    p.optString("url", "")
                            ));
                        }
                    }
                    if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                        Log.d(TAG, "audio book thumbnail: id=" + id + " url=" + thumbnailUrl.substring(0, Math.min(80, thumbnailUrl.length())) + "...");
                    }
                    Log.d(TAG, "parsed book id=" + id + " title=" + title + " parts=" + parts.size());
                    if (id.isEmpty() || title.isEmpty()) {
                        Log.w(TAG, "Skipping book with empty id or title at index " + i);
                        continue;
                    }
                    list.add(new ServerAudioBook(id, title, sortPartsByNumber(parts), thumbnailUrl));
                    if ("geetaji_chintan".equals(id)) {
                        Log.d(TAG, "✅ geetaji_chintan added to list: parts=" + parts.size() + " thumbnail=" + thumbnailUrl);
                    }
                } catch (Exception e) {
                    String bookId = (b != null) ? b.optString("id", "unknown") : "null";
                    Log.e(TAG, "Parse error for book index " + i + " (id might be: " + bookId + ")", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Parse error", e);
        }
        Log.d(TAG, "parseBooks: returning " + list.size() + " books");
        boolean hasGeetaji = false;
        for (ServerAudioBook b : list) {
            if ("geetaji_chintan".equals(b.getId())) {
                hasGeetaji = true;
                break;
            }
        }
        Log.d(TAG, "parseBooks: geetaji_chintan in list: " + hasGeetaji);
        return list;
    }

    /** Same book = same thumbnail: 56 books list me se audio book ke title se match karo, usi book ka thumbnailUrl lo. */
    private static String findThumbnailUrlForAudioBook(android.content.Context ctx, String id, String audioTitle,
                                                       List<Book> serverBooks) {
        if (audioTitle == null) audioTitle = "";
        String t = audioTitle.trim();
        if (serverBooks != null) {
            for (Book book : serverBooks) {
                String name = book != null ? book.getName() : null;
                if (name == null) continue;
                name = name.trim();
                if (name.equals(t)) return book.getThumbnailUrl();
                if (normalizeForMatch(name).equals(normalizeForMatch(t))) return book.getThumbnailUrl();
            }
        }
        return getServerThumbnailUrlForId(ctx, id);
    }

    private static String normalizeForMatch(String s) {
        return s == null ? "" : s.trim();
    }

    /** Fallback: known audio id -> same thumbnail as Books page. */
    private static String getServerThumbnailUrlForId(android.content.Context ctx, String id) {
        if (ctx == null || id == null || id.isEmpty()) return null;
        // Chintan books: same as Books page – raw GitHub URL (ASCII name)
        if ("mahabharat_chintan".equals(id)) {
            return "https://raw.githubusercontent.com/daveashish12356-dotcom/swamisachidanand-audio/gh-pages/thumbnails/mahabharat_chintan.jpg";
        }
        if ("ramayan_chintan".equals(id)) {
            return "https://raw.githubusercontent.com/daveashish12356-dotcom/swamisachidanand-audio/gh-pages/thumbnails/ramayan_chintan.jpg";
        }
        if ("geetaji_chintan".equals(id)) {
            return "https://raw.githubusercontent.com/daveashish12356-dotcom/swamisachidanand-audio/gh-pages/thumbnails/geetaji_chintan.jpg";
        }
        if ("kandadeprabandh_sar".equals(id)) {
            return "https://raw.githubusercontent.com/daveashish12356-dotcom/swamisachidanand-audio/gh-pages/thumbnails/kandadeprabandh_sar.jpg";
        }
        if ("krantikathao".equals(id)) {
            return "https://raw.githubusercontent.com/daveashish12356-dotcom/swamisachidanand-audio/main/thumbnails/krantikathao.jpg";
        }
        String thumbName = null;
        switch (id) {
            case "amarakantak_madhyapradesh": thumbName = "અમરકંટક અને મધ્યપ્રદેશનો મહિમા.jpg"; break;
            case "aapni_durbalatao": thumbName = "આપણી દુર્બળતાઓ.jpg"; break;
            case "mara_anubhavo": thumbName = "મારા અનુભવો.jpg"; break;
            case "africa_sansmaran": thumbName = "આફ્રિકા-પ્રવાસનાં સંસ્મરણો.jpg"; break;
            case "aavego_laganiyo": thumbName = "આવેગો અને લાગણીઓ.jpg"; break;
            case "mahabharat_jeevankathao": thumbName = "મહાભારતની જીવનકથાઓ.jpg"; break;
            case "mara_upkarkao": thumbName = "મારા ઉપકારકો.jpg"; break;
            case "upsanhar": thumbName = "ઉપસંહાર.jpg"; break;
            case "valmiki_ramayan_sar": thumbName = "વાલ્મીકિ-રામાયણ-સાર.jpg"; break;
            case "upanishad_kathao_chintan": thumbName = "ઉપનિષદોની કથાઓ અને ચિંતન.jpg"; break;
            default: break;
        }
        try {
            String base = ctx.getString(R.string.server_books_base_url);
            if (base == null) base = "";
            base = base.trim();
            if (!base.isEmpty() && !base.endsWith("/")) base += "/";
            if (thumbName != null) {
                String encoded = URLEncoder.encode(thumbName, StandardCharsets.UTF_8.name()).replace("+", "%20");
                return base + "thumbnails/" + encoded;
            }
            return base + "thumbnails/" + id + ".jpg";
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void onAudioBookClick(ServerAudioBook book) {
        if (book == null || getActivity() == null) return;
        Fragment detail = AudioBookDetailFragment.newInstance(book);
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, detail)
                .addToBackStack("audio_books")
                .commit();
    }
}
