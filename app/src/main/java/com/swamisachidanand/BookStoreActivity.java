package com.swamisachidanand;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BookStoreActivity extends AppCompatActivity {

    private RecyclerView sectionsRecycler;
    private BookStoreSectionAdapter sectionAdapter;
    private final List<BookStoreItem> allBooks = new ArrayList<>();
    private String coversBaseUrl = "";
    private static final String BOOKS_STORE_URL = "https://daveashish12356-dotcom.github.io/swamisachidanand-audio/public/books_store.json?v=2";

    private static String[] filterLabels() { return BookStoreCategoryHelper.getFilterLabels(); }
    private static String[] filterIds() { return BookStoreCategoryHelper.getFilterIds(); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_store);

        SwipeRefreshLayout swipeRefresh = findViewById(R.id.book_store_swipe_refresh);
        swipeRefresh.setOnRefreshListener(this::loadBooks);

        sectionsRecycler = findViewById(R.id.book_store_sections);
        sectionsRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        sectionsRecycler.setItemViewCacheSize(20);
        sectionAdapter = new BookStoreSectionAdapter(this);
        sectionAdapter.setCoversBaseUrl("https://daveashish12356-dotcom.github.io/swamisachidanand-audio/public/book_covers/");
        sectionAdapter.setOnBookStoreClickListener(this::openBookDetail);
        sectionAdapter.setOnOrderClickListener(this::openWhatsAppOrder);
        sectionAdapter.setOnSectionTitleClickListener(filterId -> openBooksTabWithFilter(filterId));
        sectionsRecycler.setAdapter(sectionAdapter);

        loadBooks();
    }

    /** Build section list on background thread to avoid UI hang. */
    private List<BookStoreSectionAdapter.Section> buildSections() {
        List<BookStoreSectionAdapter.Section> sections = new ArrayList<>();
        BookStoreSectionAdapter.Section headerSec = new BookStoreSectionAdapter.Section();
        headerSec.filterId = "header";
        headerSec.title = "";
        sections.add(headerSec);

        // નવાં પુસ્તકો = સર્વર "new": true અથવા અગાઉનાં નામ (મહાન રામાનુજાચાર્ય, દેવાલય થી દેહાલય) – સર્વરમાં new ઉમેરો તો આપમેળે ઉમેરાશે
        BookStoreSectionAdapter.Section newBooks = new BookStoreSectionAdapter.Section();
        newBooks.title = "📖 નવાં પુસ્તકો";
        newBooks.filterId = "new";
        newBooks.books = new ArrayList<>();
        for (BookStoreItem b : allBooks) {
            if (b != null && (b.isNew || isNewBookByName(b.name))) newBooks.books.add(b);
        }
        sections.add(newBooks);

        String[] fIds = filterIds();
        String[] fLabels = filterLabels();
        for (int i = 0; i < fIds.length; i++) {
            String id = fIds[i];
            if ("all".equals(id) || "new".equals(id)) continue; // new already added above
            String label = i < fLabels.length ? fLabels[i] : id;
            List<BookStoreItem> books = filterByCategory(allBooks, id);
            BookStoreSectionAdapter.Section sec = new BookStoreSectionAdapter.Section();
            sec.title = label;
            sec.filterId = id;
            sec.books = books;
            sections.add(sec);
        }

        BookStoreSectionAdapter.Section allSec = new BookStoreSectionAdapter.Section();
        allSec.title = "All";
        allSec.filterId = "all";
        allSec.books = new ArrayList<>(allBooks);
        sections.add(allSec);
        return sections;
    }

    private void buildSectionsAndRefresh() {
        sectionAdapter.setCoversBaseUrl(coversBaseUrl);
        sectionAdapter.setSections(buildSections());
    }

    private void openBookDetail(BookStoreItem item) {
        if (item == null) return;
        Intent i = new Intent(this, BookDetailActivity.class);
        i.putExtra(BookDetailActivity.EXTRA_NAME, item.name);
        i.putExtra(BookDetailActivity.EXTRA_PRICE, item.price);
        i.putExtra(BookDetailActivity.EXTRA_IMG, item.img);
        i.putExtra(BookDetailActivity.EXTRA_IMAGE_URL, item.imageUrl);
        startActivity(i);
    }

    /** Price page section (e.g. નવાં પુસ્તકો) tap → open Books tab with that filter so server books dikhe. */
    private void openBooksTabWithFilter(String filterId) {
        if (filterId == null) return;
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra(MainActivity.EXTRA_TARGET_TAB, R.id.nav_books);
        i.putExtra(MainActivity.EXTRA_BOOKS_FILTER_ID, filterId);
        startActivity(i);
        finish();
    }

    private void openWhatsAppOrder(BookStoreItem item) {
        try {
            String msg = "સચ્ચિદાનંદજી પુસ્તકાલય – ઓર્ડર\n\n"
                + "પુસ્તક: " + (item.name != null ? item.name : "") + "\n"
                + "કિંમત: ₹" + item.price + "\n\n"
                + "મારું નામ: \n"
                + "મોબાઈલ નંબર: \n"
                + "સરનામું (કુરિયર): ";
            String url = "https://wa.me/919824112625?text=" + Uri.encode(msg);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/919824112625?text=પુસ્તક: " + (item.name != null ? item.name : "") + " - ₹" + item.price)));
        }
    }

    private static boolean isNewBookByName(String name) {
        if (name == null) return false;
        String n = name.trim();
        return n.contains("મહાન રામાનુજાચાર્ય") || n.contains("દેવાલય થી દેહાલય");
    }

    private List<BookStoreItem> filterByCategory(List<BookStoreItem> list, String categoryId) {
        if (list == null) return new ArrayList<>();
        if ("all".equals(categoryId)) return new ArrayList<>(list);
        List<BookStoreItem> out = new ArrayList<>();
        for (BookStoreItem b : list) {
            if (b != null && b.name != null && BookStoreCategoryHelper.belongsToCategory(b.name, categoryId)) out.add(b);
        }
        return out;
    }

    private void loadBooks() {
        allBooks.clear();
        new Thread(() -> {
            String baseUrl = "https://daveashish12356-dotcom.github.io/swamisachidanand-audio/public/";
            String coversBase = baseUrl + "book_covers/";
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();
                Request req = new Request.Builder()
                    .url(BOOKS_STORE_URL)
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    if (resp.isSuccessful() && resp.body() != null) {
                        String json = resp.body().string();
                        JSONObject root = new JSONObject(json);
                        JSONArray arr = root.optJSONArray("books");
                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject o = arr.getJSONObject(i);
                                BookStoreItem item = new BookStoreItem();
                                item.id = o.optString("id", "");
                                item.name = o.optString("name", "");
                                item.price = o.optInt("price", 0);
                                item.img = o.optString("img", "");
                                item.imageUrl = (item.img != null && !item.img.isEmpty()) ? (coversBase + item.img) : null;
                                item.isNew = o.optBoolean("new", false);
                                allBooks.add(item);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (allBooks.isEmpty()) {
                loadFromAssets();
            }
            final String base = coversBase;
            final List<BookStoreSectionAdapter.Section> sections = buildSections();
            runOnUiThread(() -> {
                coversBaseUrl = base;
                sectionAdapter.setCoversBaseUrl(base);
                SwipeRefreshLayout s = findViewById(R.id.book_store_swipe_refresh);
                if (s != null) s.setRefreshing(false);
                sectionsRecycler.post(() -> sectionAdapter.setSections(sections));
            });
        }).start();
    }

    private void loadFromAssets() {
        try {
            InputStream is = getAssets().open("books_store_list.json");
            byte[] buf = new byte[is.available()];
            is.read(buf);
            is.close();
            String json = new String(buf, StandardCharsets.UTF_8);
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject o = arr.getJSONObject(i);
                BookStoreItem item = new BookStoreItem();
                item.id = o.optString("id", "");
                item.name = o.optString("name", "");
                item.price = o.optInt("price", 0);
                item.img = o.optString("img", "");
                item.isNew = o.optBoolean("new", false);
                allBooks.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
