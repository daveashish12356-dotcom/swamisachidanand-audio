package com.swamisachidanand;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * Global search results – Books | Audio | Videos in tabs.
 * Opens when user submits search from any page.
 */
public class SearchResultActivity extends AppCompatActivity {

    public static final String EXTRA_QUERY = "query";

    private TextInputEditText searchInput;
    private ImageView clearSearch;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private SearchPagerAdapter pagerAdapter;

    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.search_toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(R.string.search_global_hint);
            }
        }

        searchInput = findViewById(R.id.global_search_input);
        clearSearch = findViewById(R.id.global_clear_search);

        tabLayout = findViewById(R.id.search_tabs);
        viewPager = findViewById(R.id.search_view_pager);

        currentQuery = getIntent() != null ? getIntent().getStringExtra(EXTRA_QUERY) : "";
        if (currentQuery == null) currentQuery = "";

        if (searchInput != null) {
            searchInput.setText(currentQuery);
            searchInput.requestFocus();
        }

        pagerAdapter = new SearchPagerAdapter(this, currentQuery);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(getTabTitle(position, 0));
        }).attach();

        setupSearchBar();
        setupResultCountUpdates();
    }

    private String getTabTitle(int position, int count) {
        String base;
        switch (position) {
            case 0: base = getString(R.string.search_tab_books); break;
            case 1: base = getString(R.string.search_tab_audio); break;
            case 2: base = getString(R.string.search_tab_videos); break;
            default: base = "";
        }
        return count >= 0 ? base + " (" + count + ")" : base;
    }

    private void setupSearchBar() {
        if (searchInput == null) return;
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (clearSearch != null) {
                    clearSearch.setVisibility(s != null && s.length() > 0 ? android.view.View.VISIBLE : android.view.View.GONE);
                }
            }
        });
        if (clearSearch != null) {
            clearSearch.setOnClickListener(v -> {
                if (searchInput != null) searchInput.setText("");
            });
        }
        View micBtn = findViewById(R.id.global_mic_button);
        if (micBtn != null) {
            micBtn.setOnClickListener(v -> {
                try {
                    Intent i = new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    i.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    i.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "gu-IN");
                    i.putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Search...");
                    startActivityForResult(i, 1003);
                } catch (Exception e) {
                    Toast.makeText(this, "Voice search not available", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setupResultCountUpdates() {
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateTabTitles();
            }
        });
    }

    void reportCount(int tabType, int count) {
        if (pagerAdapter != null) pagerAdapter.setResultCount(tabType, count);
        updateTabTitles();
    }

    void updateTabTitles() {
        if (tabLayout == null || pagerAdapter == null) return;
        for (int i = 0; i < 3; i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab != null) {
                int count = pagerAdapter.getResultCount(i);
                tab.setText(getTabTitle(i, count));
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1003 && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty() && searchInput != null) {
                searchInput.setText(results.get(0));
                performSearch();
            }
        }
    }

    private void performSearch() {
        if (searchInput == null) return;
        String q = searchInput.getText() != null ? searchInput.getText().toString().trim() : "";
        if (q.isEmpty()) {
            Toast.makeText(this, getString(R.string.search_global_hint), Toast.LENGTH_SHORT).show();
            return;
        }
        currentQuery = q;
        pagerAdapter.setQuery(q);
        pagerAdapter.notifyDataSetChanged();
        updateTabTitles();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    static class SearchPagerAdapter extends FragmentStateAdapter {

        private String query;
        private final int[] counts = new int[]{-1, -1, -1};

        SearchPagerAdapter(FragmentActivity activity, String query) {
            super(activity);
            this.query = query != null ? query : "";
        }

        void setQuery(String q) {
            this.query = q != null ? q : "";
        }

        int getResultCount(int position) {
            if (position >= 0 && position < counts.length) return counts[position];
            return -1;
        }

        void setResultCount(int position, int count) {
            if (position >= 0 && position < counts.length) counts[position] = count;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return SearchResultFragment.newInstance(position, query);
        }

        @Override
        public int getItemCount() {
            return 3;
        }

        @Override
        public long getItemId(int position) {
            return position * 10000L + (query != null ? query.hashCode() & 0xFFFFFFFFL : 0);
        }

        @Override
        public boolean containsItem(long itemId) {
            long h = query != null ? query.hashCode() & 0xFFFFFFFFL : 0;
            return itemId == h || itemId == 10000 + h || itemId == 20000 + h;
        }
    }
}
