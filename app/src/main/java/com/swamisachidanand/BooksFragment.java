package com.swamisachidanand;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BooksFragment extends Fragment implements BookAdapter.OnBookClickListener {

    private static final String TAG = "BooksFragment";
    private static final int REQUEST_CODE_VOICE_SEARCH = 1001;
    private static final String PREFS_NAME = "reading_progress";
    private static final int POPULAR_COUNT = 8;

    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView booksRecyclerView;
    private RecyclerView categoryChipsRecycler;
    private RecyclerView popularRecycler;
    private LinearLayout popularSection;
    private LinearLayout searchResultsSection;
    private RecyclerView searchResultsRecycler;
    private TextView booksLoadingLine;
    private LinearLayout emptyText;
    private TextView emptyTextMessage;
    private BookAdapter bookAdapter;
    private BookAdapter popularAdapter;
    private BookAdapter searchResultsAdapter;
    private CategoryChipAdapter chipAdapter;
    private TextInputEditText searchInput;
    private ImageView clearSearch;
    private ImageView micButton;
    private ImageView filterButton;
    private List<Book> books = new ArrayList<>();
    private List<Book> allBooks = new ArrayList<>();
    private String selectedCategoryId = "all";
    /** One placeholder book shown while server list is loading – thumbnail visible, no click. */
    private static List<Book> placeholderBooks() {
        List<Book> list = new ArrayList<>();
        list.add(new Book("", "", 0));
        return list;
    }

    public BooksFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = null;
        try {
            view = inflater.inflate(R.layout.fragment_books, container, false);
            if (view == null) return container != null ? new View(container.getContext()) : null;
            swipeRefreshLayout = view.findViewById(R.id.books_swipe_refresh);
            booksRecyclerView = view.findViewById(R.id.books_recycler_view);
            categoryChipsRecycler = view.findViewById(R.id.category_chips_recycler);
            popularRecycler = view.findViewById(R.id.popular_recycler);
            popularSection = view.findViewById(R.id.popular_section);
            searchResultsSection = view.findViewById(R.id.books_search_results_section);
            searchResultsRecycler = view.findViewById(R.id.books_search_results_recycler);
            booksLoadingLine = view.findViewById(R.id.books_loading_line);
            emptyText = view.findViewById(R.id.empty_text);
            emptyTextMessage = view.findViewById(R.id.empty_text_message);
            searchInput = view.findViewById(R.id.global_search_input);
            if (searchInput != null) searchInput.setHint(R.string.search_books_hint);
            clearSearch = view.findViewById(R.id.global_clear_search);
            micButton = view.findViewById(R.id.global_mic_button);
            filterButton = view.findViewById(R.id.global_filter_button);
            if (filterButton != null) filterButton.setVisibility(View.VISIBLE);
            View avatar = view.findViewById(R.id.global_profile_avatar);
            if (avatar != null) {
                avatar.setOnClickListener(v -> {
                    android.app.Activity act = getActivity();
                    if (act instanceof MainActivity) ((MainActivity) act).openSwamiInfoPage();
                });
            }
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setOnRefreshListener(this::onRefreshRequested);
            }
            View nestedScroll = view.findViewById(R.id.books_nested_scroll);
            if (nestedScroll != null && getActivity() instanceof MainActivity) {
                nestedScroll.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).onScrolled(scrollY - oldScrollY);
                });
            }
            // Defer setup + load to next frame so Books tab appears first, no hang on open
            if (view != null) view.post(() -> {
                if (!isAdded() || getContext() == null) return;
                setupRecyclerView();
                setupCategoryChips();
                setupSearchResultsRecycler();
                setupSearch();
                if (bookAdapter != null) bookAdapter.updateBooks(placeholderBooks());
                loadBooks();
            });
        } catch (Throwable t) {
            Log.e(TAG, "onCreateView error", t);
            if (view == null && container != null) view = new View(container.getContext());
        }
        return view != null ? view : (container != null ? new View(container.getContext()) : null);
    }

    private void onRefreshRequested() {
        ServerBookLoader.clearCache();
        loadBooks();
    }

    private void setupRecyclerView() {
        android.content.Context ctx = getContext();
        if (ctx == null) return;
        if (booksRecyclerView != null) {
            int spanCount = getSpanCountForScreen();
            booksRecyclerView.setLayoutManager(new GridLayoutManager(ctx, spanCount));
            // setHasFixedSize not used: layout uses wrap_content in scroll direction
            booksRecyclerView.setItemViewCacheSize(20);
            bookAdapter = new BookAdapter(books, this);
            bookAdapter.setReadingProgressMap(loadReadingProgressMap());
            booksRecyclerView.setAdapter(bookAdapter);
        }
        if (popularRecycler != null) {
            popularRecycler.setLayoutManager(new LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false));
            // setHasFixedSize not used: horizontal list wrap_content
            popularRecycler.setItemAnimator(new DefaultItemAnimator());
            popularRecycler.setItemViewCacheSize(10);
            popularAdapter = new BookAdapter(new ArrayList<>(), this);
            popularAdapter.setUseCompactLayout(true);
            popularRecycler.setAdapter(popularAdapter);
        }
    }

    private void setupCategoryChips() {
        if (categoryChipsRecycler == null || getContext() == null) return;
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
        categoryChipsRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        categoryChipsRecycler.setAdapter(chipAdapter);
        if (filterButton != null) {
            filterButton.setOnClickListener(v -> {
                if (chipAdapter != null) chipAdapter.setSelectedIndex(0);
                selectedCategoryId = "all";
                applyFilters();
            });
        }
    }

    private Map<String, Integer> loadReadingProgressMap() {
        return loadReadingProgressMapInBackground(allBooks);
    }

    private Map<String, Integer> loadReadingProgressMapInBackground(List<Book> bookList) {
        Map<String, Integer> map = new HashMap<>();
        try {
            android.app.Activity act = getActivity();
            if (act == null) return map;
            SharedPreferences prefs = act.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            if (bookList == null) return map;
            for (Book b : bookList) {
                String name = b.getName();
                if (name == null) continue;
                int page = prefs.getInt(name + "_page", -1);
                int total = prefs.getInt(name + "_total_pages", 0);
                if (page >= 0 && total > 0) {
                    int pct = (int) ((page + 1) * 100.0 / total);
                    if (pct > 0 && pct < 100) {
                        map.put(name, pct);
                        String fname = b.getFileName();
                        if (fname != null) {
                            String base = fname.replace(".pdf", "").replace(".PDF", "");
                            map.put(base, pct);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "loadReadingProgressMap", e);
        }
        return map;
    }

    private void applyFilters() {
        // Category chips ke hisaab se main grid dikhana hai – search text yahan ignore
        filterBooks("");
    }

    /** લોકપ્રિય પુસ્તકો = નવાં પુસ્તકો (server list માં જે અંતે આવે તે – નવું add થયું તે પહેલા દેખાશે) */
    private void loadPopular(List<Book> serverBooks) {
        if (popularAdapter == null || popularSection == null) return;
        List<Book> popular = new ArrayList<>();
        if (serverBooks.size() <= POPULAR_COUNT) {
            popular.addAll(serverBooks);
            Collections.reverse(popular);
        } else {
            for (int i = serverBooks.size() - 1; i >= serverBooks.size() - POPULAR_COUNT; i--) {
                popular.add(serverBooks.get(i));
            }
        }
        popularAdapter.updateBooks(popular);
        popularSection.setVisibility(popular.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void setupSearchResultsRecycler() {
        android.content.Context ctx = getContext();
        if (searchResultsRecycler == null || ctx == null) return;
        searchResultsRecycler.setLayoutManager(new LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false));
        searchResultsRecycler.setItemAnimator(new DefaultItemAnimator());
        int spacing = (int) (10 * ctx.getResources().getDisplayMetrics().density);
        searchResultsRecycler.addItemDecoration(new HorizontalSpacingItemDecoration(spacing));
        searchResultsAdapter = new BookAdapter(new ArrayList<>(), this);
        searchResultsAdapter.setUseCompactLayout(true);
        searchResultsRecycler.setAdapter(searchResultsAdapter);
    }

    private void setupSearch() {
        if (searchInput == null || clearSearch == null || micButton == null) return;
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) clearSearch.setVisibility(View.GONE);
                else clearSearch.setVisibility(View.VISIBLE);
                updateSearchResults(query);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        // IME search button: sirf search results line update kare
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                String query = v.getText() != null ? v.getText().toString().trim() : "";
                updateSearchResults(query);
                return true;
            }
            return false;
        });
        clearSearch.setOnClickListener(v -> {
            searchInput.setText("");
            clearSearch.setVisibility(View.GONE);
            updateSearchResults("");
        });
        // Voice search bhi isi page par filter kare (alag search result activity nahi)
        micButton.setOnClickListener(v -> startVoiceSearch());
    }

    private void openGlobalSearch() {
        String q = searchInput != null && searchInput.getText() != null ? searchInput.getText().toString().trim() : "";
        if (q.isEmpty()) return;
        Intent i = new Intent(requireContext(), SearchResultActivity.class);
        i.putExtra(SearchResultActivity.EXTRA_QUERY, q);
        startActivity(i);
    }

    private void startVoiceSearch() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            // Set Gujarati language for voice search
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "gu-IN");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "gu-IN");
            intent.putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, new String[]{"gu-IN", "hi-IN", "en-IN"});
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "પુસ્તકો શોધવા માટે બોલો...");
            startActivityForResult(intent, REQUEST_CODE_VOICE_SEARCH);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Voice search not available", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Voice search error", e);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_VOICE_SEARCH && resultCode == android.app.Activity.RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty() && searchInput != null) {
                String spokenText = results.get(0);
                searchInput.setText(spokenText);
                // Text watcher + IME listener already search results line ko update kar dega
            }
        }
    }

    /** Sirf upar wali 'Search result' line ke liye – main grid (All Books) same rehta hai. */
    private void updateSearchResults(String query) {
        if (searchResultsSection == null || searchResultsRecycler == null || searchResultsAdapter == null) return;
        String q = query != null ? query.trim() : "";
        if (q.length() < 3) {
            searchResultsSection.setVisibility(View.GONE);
            searchResultsAdapter.updateBooks(new ArrayList<>());
            return;
        }
        String queryLower = q.toLowerCase();
        List<Book> source = allBooks != null ? allBooks : new ArrayList<>();
        List<Book> matches = new ArrayList<>();

        for (Book book : source) {
            String bookName = (book.getName() != null ? book.getName() : "").toLowerCase();
            String fileName = (book.getFileName() != null ? book.getFileName() : "").toLowerCase();
            String searchableText = book.getSearchableText();

            boolean found = false;
            if (searchableText != null && searchableText.contains(queryLower)) {
                found = true;
            } else if (bookName.contains(queryLower) || fileName.contains(queryLower)) {
                found = true;
            } else if (containsAllWords(queryLower, searchableText != null ? searchableText : bookName + " " + fileName)) {
                found = true;
            } else if ((searchableText != null && fuzzyMatch(queryLower, searchableText)) ||
                    fuzzyMatch(queryLower, bookName) || fuzzyMatch(queryLower, fileName)) {
                found = true;
            }

            if (found) matches.add(book);
        }

        Collections.sort(matches, (b1, b2) -> (b1.getName() != null ? b1.getName() : "")
                .compareToIgnoreCase(b2.getName() != null ? b2.getName() : ""));

        if (matches.isEmpty()) {
            searchResultsSection.setVisibility(View.GONE);
        } else {
            searchResultsSection.setVisibility(View.VISIBLE);
            searchResultsAdapter.updateBooks(matches);
        }
    }

    private void filterBooks(String query) {
        List<Book> filtered = new ArrayList<>();
        List<Book> source = filterByCategory(allBooks);
        if (query.isEmpty()) {
            filtered.addAll(source);
        } else {
            // Normalize query - trim and lowercase only
            String queryLower = query.trim().toLowerCase();
            if (queryLower.isEmpty()) {
                filtered.addAll(source);
                updateBooksDisplay(filtered);
                return;
            }
            // User ki request: कम से कम 3 character likhne par hi search filter lage
            if (queryLower.length() < 3) {
                filtered.addAll(source);
                updateBooksDisplay(filtered);
                return;
            }
            
            Log.d(TAG, "Searching for: " + queryLower);
            for (Book book : source) {
                String bookName = (book.getName() != null ? book.getName() : "").toLowerCase();
                String fileName = (book.getFileName() != null ? book.getFileName() : "").toLowerCase();
                String searchableText = book.getSearchableText(); // Includes English transliteration
                
                boolean matches = false;
                
                // 1. Search in searchable text (original + transliterated English) - BEST MATCH
                if (searchableText != null && searchableText.contains(queryLower)) {
                    matches = true;
                    Log.d(TAG, "Found via searchable text: " + book.getName());
                }
                // 2. Direct contains in original names
                else if (bookName.contains(queryLower) || fileName.contains(queryLower)) {
                    matches = true;
                    Log.d(TAG, "Found via direct contains: " + book.getName());
                }
                // 3. Word-by-word matching (for "purv amarkantak" type queries)
                else {
                    String[] queryWords = queryLower.split("\\s+");
                    boolean anyWordMatches = false;
                    
                    for (String word : queryWords) {
                        word = word.trim();
                        if (word.length() >= 2) {
                            if ((searchableText != null && searchableText.contains(word)) ||
                                bookName.contains(word) || fileName.contains(word)) {
                                anyWordMatches = true;
                                Log.d(TAG, "Found via word match '" + word + "': " + book.getName());
                                break;
                            }
                        }
                    }
                    
                    if (anyWordMatches) {
                        matches = true;
                    }
                }
                
                // 4. Fuzzy match as last resort
                if (!matches) {
                    if ((searchableText != null && fuzzyMatch(queryLower, searchableText)) ||
                        fuzzyMatch(queryLower, bookName) || fuzzyMatch(queryLower, fileName)) {
                        matches = true;
                        Log.d(TAG, "Found via fuzzy: " + book.getName());
                    }
                }
                
                if (matches) {
                    filtered.add(book);
                }
            }
            
            Log.d(TAG, "Total books found: " + filtered.size());
        }
        
        Collections.sort(filtered, (b1, b2) -> (b1.getName() != null ? b1.getName() : "").compareToIgnoreCase(b2.getName() != null ? b2.getName() : ""));
        updateBooksDisplay(filtered);
    }

    private List<Book> filterByCategory(List<Book> list) {
        if ("all".equals(selectedCategoryId)) return list;
        if ("popular".equals(selectedCategoryId)) {
            if (list.size() <= POPULAR_COUNT) {
                List<Book> out = new ArrayList<>(list);
                Collections.reverse(out);
                return out;
            }
            List<Book> out = new ArrayList<>();
            for (int i = list.size() - 1; i >= list.size() - POPULAR_COUNT; i--) {
                out.add(list.get(i));
            }
            return out;
        }
        List<Book> out = new ArrayList<>();
        for (Book b : list) {
            String cat = b.getCategory();
            if (selectedCategoryId.equals(cat)) out.add(b);
        }
        return out;
    }

    private void updateBooksDisplay(List<Book> filtered) {
        if (bookAdapter != null) {
            bookAdapter.setReadingProgressMap(loadReadingProgressMap());
            bookAdapter.updateBooks(filtered);
        }
        if (emptyText != null && booksRecyclerView != null) {
            if (filtered.isEmpty()) {
                emptyText.setVisibility(View.VISIBLE);
                booksRecyclerView.setVisibility(View.GONE);
                if (emptyTextMessage != null) emptyTextMessage.setText("કોઈ પુસ્તક મળ્યું નથી");
            } else {
                emptyText.setVisibility(View.GONE);
                booksRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    private boolean containsAllWords(String query, String text) {
        if (query == null || text == null || query.isEmpty() || text.isEmpty()) return false;
        
        // Split query into words
        String[] queryWords = query.split("\\s+");
        
        // Check if any query word appears in text
        for (String queryWord : queryWords) {
            queryWord = queryWord.trim().toLowerCase();
            if (queryWord.length() >= 2 && text.contains(queryWord)) {
                return true;
            }
        }
        
        return false;
    }

    private boolean startsWithAnyWord(String query, String text) {
        if (query == null || text == null || query.isEmpty()) return false;
        
        String[] textWords = text.split("[\\s,\\-–—\\.]+");
        for (String word : textWords) {
            word = word.trim();
            if (word.length() > 0 && word.startsWith(query)) {
                return true;
            }
            // Also check if query starts with word (for partial matches)
            if (word.length() > 0 && query.startsWith(word)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAnyWord(String query, String text) {
        if (query == null || text == null || query.isEmpty() || text.isEmpty()) return false;
        
        // Split query and text into words
        String[] queryWords = query.split("[\\s,\\-–—\\._]+");
        
        for (String queryWord : queryWords) {
            queryWord = queryWord.trim().toLowerCase();
            if (queryWord.isEmpty() || queryWord.length() < 2) continue;
            
            // Direct contains check
            if (text.contains(queryWord)) {
                return true;
            }
            
            // Check if query word is part of any word in text
            String[] textWords = text.split("[\\s,\\-–—\\._]+");
            for (String textWord : textWords) {
                textWord = textWord.trim().toLowerCase();
                if (textWord.contains(queryWord) || queryWord.contains(textWord)) {
                    return true;
                }
                // Fuzzy match for partial similarity
                if (textWord.length() >= 3 && fuzzyMatch(queryWord, textWord)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean fuzzyMatch(String query, String text) {
        if (query == null || text == null || query.isEmpty()) return false;
        
        // Remove special characters and normalize spaces
        query = query.replaceAll("[^\\w\\s]", " ").replaceAll("\\s+", " ").trim();
        text = text.replaceAll("[^\\w\\s]", " ").replaceAll("\\s+", " ").trim();
        
        if (query.isEmpty() || text.isEmpty()) return false;
        
        // Method 1: Check if query characters appear in sequence (allowing gaps)
        int queryIndex = 0;
        int matchedChars = 0;
        
        for (int i = 0; i < text.length() && queryIndex < query.length(); i++) {
            char textChar = Character.toLowerCase(text.charAt(i));
            char queryChar = Character.toLowerCase(query.charAt(queryIndex));
            
            if (textChar == queryChar) {
                matchedChars++;
                queryIndex++;
            }
        }
        
        // Match if at least 50% of query characters are found in sequence (thoda loose match)
        if (matchedChars >= (query.length() * 0.5)) {
            return true;
        }
        
        // Method 2: Check if major words from query appear in text (for multi-word queries)
        String[] queryWords = query.split("\\s+");
        if (queryWords.length > 1) {
            int matchedWords = 0;
            for (String word : queryWords) {
                if (word.length() >= 3 && text.contains(word)) {
                    matchedWords++;
                }
            }
            // If at least one major word matches, consider it a match
            return matchedWords > 0;
        }
        
        return false;
    }

    private int getSpanCountForScreen() {
        try {
            if (getResources() == null) return 2;
            android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
            if (dm == null) return 2;
            float density = dm.density;
            float dpWidth = dm.widthPixels / density;
        
            if (dpWidth >= 600) return 4;
            if (dpWidth >= 480) return 3;
            return 2;
        } catch (Exception e) {
            return 2;
        }
    }

    /** Load 56 books from server list (books_server_list.json). Thumbnail + PDF URLs point to server. Runs in background so UI thread is not blocked. */
    private void loadBooks() {
        android.app.Activity act = getActivity();
        if (act == null) return;
        if (booksLoadingLine != null) booksLoadingLine.setVisibility(View.VISIBLE);
        if (emptyText != null) emptyText.setVisibility(View.GONE);
        if (booksRecyclerView != null) booksRecyclerView.setVisibility(View.VISIBLE);
        if (bookAdapter != null) bookAdapter.updateBooks(placeholderBooks());
        new Thread(() -> {
            try {
                android.app.Activity activity = getActivity();
                if (activity == null) return;
                android.content.Context ctx = getContext();
                if (ctx == null) return;
                List<Book> loaded = ServerBookLoader.load(ctx);
                Log.d(TAG, "loadBooks: got " + loaded.size() + " books");

                List<Book> sortedBooks = new ArrayList<>(loaded);
                List<Book> sortedAll = new ArrayList<>(loaded);
                Collections.sort(sortedBooks, (b1, b2) -> (b1.getName() != null ? b1.getName() : "")
                        .compareToIgnoreCase(b2.getName() != null ? b2.getName() : ""));
                Collections.sort(sortedAll, (b1, b2) -> (b1.getName() != null ? b1.getName() : "")
                        .compareToIgnoreCase(b2.getName() != null ? b2.getName() : ""));

                Map<String, Integer> progressMap = loadReadingProgressMapInBackground(sortedAll);

                activity.runOnUiThread(() -> {
                    if (!isAdded()) return;
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    try {
                        books.clear();
                        allBooks.clear();
                        books.addAll(sortedBooks);
                        allBooks.addAll(sortedAll);
                        if (booksLoadingLine != null) booksLoadingLine.setVisibility(View.GONE);
                        loadPopular(sortedAll);
                        if (bookAdapter != null) {
                            bookAdapter.setReadingProgressMap(progressMap);
                        }
                        // Defer filter so list draws first, no jank
                        if (booksRecyclerView != null) {
                            booksRecyclerView.post(() -> {
                                if (isAdded()) applyFilters();
                            });
                        } else {
                            applyFilters();
                        }
                    } catch (Exception uiEx) {
                        Log.e(TAG, "Error updating books UI", uiEx);
                        if (emptyText != null) emptyText.setVisibility(View.VISIBLE);
                        if (booksRecyclerView != null) booksRecyclerView.setVisibility(View.GONE);
                        if (emptyTextMessage != null) emptyTextMessage.setText("પુસ્તકો લોડ થયા નહીં.");
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading books", e);
                android.app.Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        if (emptyText != null) emptyText.setVisibility(View.VISIBLE);
                        if (booksRecyclerView != null) booksRecyclerView.setVisibility(View.GONE);
                        if (emptyTextMessage != null) emptyTextMessage.setText("પુસ્તકો લોડ થયા નહીં.");
                    });
                }
            }
        }).start();
    }

    @Override
    public void onBookClick(Book book) {
        try {
            if (book == null || book.getPdfUrl() == null || book.getPdfUrl().isEmpty()) return;
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openBook(book);
            }
        } catch (Throwable t) {
            Log.e(TAG, "onBookClick", t);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        booksRecyclerView = null;
        categoryChipsRecycler = null;
        popularRecycler = null;
        popularSection = null;
        booksLoadingLine = null;
        emptyText = null;
        emptyTextMessage = null;
        searchInput = null;
        clearSearch = null;
        micButton = null;
        filterButton = null;
        bookAdapter = null;
        popularAdapter = null;
        chipAdapter = null;
    }
}
