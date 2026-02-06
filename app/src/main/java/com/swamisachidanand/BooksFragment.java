package com.swamisachidanand;

import android.content.Intent;
import android.content.res.AssetManager;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class BooksFragment extends Fragment implements BookAdapter.OnBookClickListener {

    private static final String TAG = "BooksFragment";
    private static final int REQUEST_CODE_VOICE_SEARCH = 1001;
    
    private RecyclerView booksRecyclerView;
    private LinearLayout emptyText;
    private TextView emptyTextMessage;
    private BookAdapter bookAdapter;
    private TextInputEditText searchInput;
    private ImageView clearSearch;
    private ImageView micButton;
    private List<Book> books = new ArrayList<>();
    private List<Book> allBooks = new ArrayList<>();

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
            booksRecyclerView = view.findViewById(R.id.books_recycler_view);
            emptyText = view.findViewById(R.id.empty_text);
            emptyTextMessage = view.findViewById(R.id.empty_text_message);
            searchInput = view.findViewById(R.id.search_input);
            clearSearch = view.findViewById(R.id.clear_search);
            micButton = view.findViewById(R.id.mic_button);
            setupRecyclerView();
            setupSearch();
            if (view != null) view.postDelayed(() -> {
                if (isAdded() && getContext() != null) loadBooks();
            }, 400);
        } catch (Throwable t) {
            Log.e(TAG, "onCreateView error", t);
            if (view == null && container != null) view = new View(container.getContext());
        }
        return view != null ? view : (container != null ? new View(container.getContext()) : null);
    }

    private void setupRecyclerView() {
        if (booksRecyclerView == null) return;
        android.content.Context ctx = getContext();
        if (ctx == null) return;
        int spanCount = getSpanCountForScreen();
        GridLayoutManager layoutManager = new GridLayoutManager(ctx, spanCount);
        booksRecyclerView.setLayoutManager(layoutManager);
        bookAdapter = new BookAdapter(books, this);
        booksRecyclerView.setAdapter(bookAdapter);
    }

    private void setupSearch() {
        if (searchInput == null || clearSearch == null || micButton == null) return;
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                
                // Show/hide clear button
                if (query.isEmpty()) {
                    clearSearch.setVisibility(View.GONE);
                } else {
                    clearSearch.setVisibility(View.VISIBLE);
                }
                
                // Filter books
                filterBooks(query);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Clear search button
        clearSearch.setOnClickListener(v -> {
            searchInput.setText("");
            clearSearch.setVisibility(View.GONE);
            filterBooks("");
        });
        
        // Mic button - Voice search
        micButton.setOnClickListener(v -> {
            startVoiceSearch();
        });
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
                filterBooks(spokenText);
            }
        }
    }

    private void filterBooks(String query) {
        List<Book> filtered = new ArrayList<>();
        
        if (query.isEmpty()) {
            filtered.addAll(allBooks);
        } else {
            // Normalize query - trim and lowercase only
            String queryLower = query.trim().toLowerCase();
            if (queryLower.isEmpty()) {
                filtered.addAll(allBooks);
                bookAdapter.updateBooks(filtered);
                return;
            }
            
            Log.d(TAG, "Searching for: " + queryLower);
            
            for (Book book : allBooks) {
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
        if (bookAdapter != null) bookAdapter.updateBooks(filtered);
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
        
        // Match if at least 60% of query characters are found in sequence
        if (matchedChars >= (query.length() * 0.6)) {
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

    private void loadBooks() {
        new Thread(() -> {
            try {
                android.app.Activity act = getActivity();
                if (act == null) return;
                AssetManager assetManager = act.getAssets();
                String[] assetFiles = assetManager.list("");
                
                if (assetFiles != null && assetFiles.length > 0) {
                    for (String fileName : assetFiles) {
                        String lowerFileName = fileName.toLowerCase();
                        if (lowerFileName.endsWith(".pdf")) {
                            try {
                                long size = 0;
                                try {
                                    size = act.getAssets().openFd(fileName).getLength();
                                } catch (IOException e) {
                                    try {
                                        java.io.InputStream is = act.getAssets().open(fileName);
                                        size = is.available();
                                        is.close();
                                    } catch (IOException e2) {
                                        Log.e(TAG, "Could not get size for: " + fileName);
                                    }
                                }
                                
                                String displayName = fileName.replace(".pdf", "").replace(".PDF", "");
                                Book book = new Book(displayName, fileName, size);
                                
                                String year = PdfYearExtractor.extractYearSimple(fileName);
                                if (year != null) {
                                    book.setPublishYear(year);
                                }
                                
                                String category = detectCategory(displayName);
                                book.setCategory(category);
                                
                                books.add(book);
                                allBooks.add(book);
                                Log.d(TAG, "Added book: " + displayName);
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing file: " + fileName, e);
                            }
                        }
                    }
                }
                
                Collections.sort(books, (b1, b2) -> (b1.getName() != null ? b1.getName() : "").compareToIgnoreCase(b2.getName() != null ? b2.getName() : ""));
                Collections.sort(allBooks, (b1, b2) -> (b1.getName() != null ? b1.getName() : "").compareToIgnoreCase(b2.getName() != null ? b2.getName() : ""));

                if (act != null) {
                    act.runOnUiThread(() -> {
                        if (!isAdded() || getContext() == null) return;
                        if (bookAdapter != null) bookAdapter.updateBooks(books);
                        if (emptyText != null && booksRecyclerView != null) {
                            if (books.isEmpty()) {
                                emptyText.setVisibility(View.VISIBLE);
                                booksRecyclerView.setVisibility(View.GONE);
                                if (emptyTextMessage != null) emptyTextMessage.setText("No books available");
                            } else {
                                emptyText.setVisibility(View.GONE);
                                booksRecyclerView.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                }
            } catch (IOException e) {
                Log.e(TAG, "Error loading books", e);
                android.app.Activity actErr = getActivity();
                if (actErr != null) {
                    actErr.runOnUiThread(() -> {
                        if (emptyText != null) emptyText.setVisibility(View.VISIBLE);
                        if (booksRecyclerView != null) booksRecyclerView.setVisibility(View.GONE);
                        if (emptyTextMessage != null) emptyTextMessage.setText("Error loading books");
                    });
                }
            }
        }).start();
    }

    private String detectCategory(String bookName) {
        String lowerName = bookName.toLowerCase();
        
        if (lowerName.contains("ભક્તિ") || lowerName.contains("bhakti") || 
            lowerName.contains("ભજન") || lowerName.contains("bhajan") ||
            lowerName.contains("ભાગવત") || lowerName.contains("bhagwat") ||
            lowerName.contains("વિષ્ણુ") || lowerName.contains("vishnu") ||
            lowerName.contains("રામાયણ") || lowerName.contains("ramayan") ||
            lowerName.contains("રામ") || lowerName.contains("ram") ||
            lowerName.contains("કૃષ્ણ") || lowerName.contains("krishna") ||
            lowerName.contains("ભર્તૃહરિ") || lowerName.contains("bhartrihari") ||
            lowerName.contains("શતક") || lowerName.contains("shata") ||
            lowerName.contains("સહસ્રનામ") || lowerName.contains("sahasranam")) {
            return "Bhakti";
        } else if (lowerName.contains("યાત્રા") || lowerName.contains("yatra") ||
                   lowerName.contains("પ્રવાસ") || lowerName.contains("travel") ||
                   lowerName.contains("પ્રવાસનાં") || lowerName.contains("પ્રવાસની") ||
                   lowerName.contains("તીર્થ") || lowerName.contains("tirth") ||
                   lowerName.contains("મુલાકાત") || lowerName.contains("mulakat") ||
                   lowerName.contains("આફ્રિકા") || lowerName.contains("africa") ||
                   lowerName.contains("યુરોપ") || lowerName.contains("europe") ||
                   lowerName.contains("ટર્કી") || lowerName.contains("turkey") ||
                   lowerName.contains("ઈજિપ્ત") || lowerName.contains("egypt") ||
                   lowerName.contains("આંદામાન") || lowerName.contains("andaman")) {
            return "Yatra";
        } else if (lowerName.contains("જીવન") || lowerName.contains("jeevan") ||
                   lowerName.contains("ચરિત્ર") || lowerName.contains("charitra") ||
                   lowerName.contains("જીવનકથા") || lowerName.contains("jeevankatha") ||
                   lowerName.contains("અનુભવ") || lowerName.contains("anubhav") ||
                   lowerName.contains("બાયપાસ") || lowerName.contains("bypass")) {
            return "Jeevan";
        }
        
        return "Updesh";
    }

    @Override
    public void onBookClick(Book book) {
        try {
            if (book != null && getActivity() instanceof MainActivity) {
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
        emptyText = null;
        emptyTextMessage = null;
        searchInput = null;
        clearSearch = null;
        micButton = null;
        bookAdapter = null;
    }
}
