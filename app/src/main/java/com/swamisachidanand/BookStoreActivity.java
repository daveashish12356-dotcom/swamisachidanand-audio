package com.swamisachidanand;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BookStoreActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private BookStoreAdapter adapter;
    private final List<BookStoreItem> allBooks = new ArrayList<>();
    private static final String BOOKS_STORE_URL = "https://daveashish12356-dotcom.github.io/swamisachidanand-audio/public/books_store.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_store);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        recycler = findViewById(R.id.book_store_grid);
        recycler.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new BookStoreAdapter();
        adapter.setUseGridLayout(true);
        adapter.setOnBookStoreClickListener(item -> {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/919824112625?text=પુસ્તક: " + item.name + " - ₹" + item.price));
                startActivity(i);
            } catch (Exception ignored) {}
        });
        recycler.setAdapter(adapter);

        loadBooks();
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
                Request req = new Request.Builder().url(BOOKS_STORE_URL).build();
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
            runOnUiThread(() -> adapter.setItems(allBooks));
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
                allBooks.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
