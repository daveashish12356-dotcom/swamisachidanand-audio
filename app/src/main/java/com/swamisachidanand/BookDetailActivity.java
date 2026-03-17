package com.swamisachidanand;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

public class BookDetailActivity extends AppCompatActivity {

    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_PRICE = "price";
    public static final String EXTRA_IMG = "img";
    public static final String EXTRA_IMAGE_URL = "imageUrl";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> finish());

        String name = getIntent().getStringExtra(EXTRA_NAME);
        int price = getIntent().getIntExtra(EXTRA_PRICE, 0);
        String img = getIntent().getStringExtra(EXTRA_IMG);
        String imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);

        TextView titleTv = findViewById(R.id.book_detail_title);
        TextView priceTv = findViewById(R.id.book_detail_price);
        ImageView cover = findViewById(R.id.book_detail_cover);

        if (titleTv != null) titleTv.setText(name != null ? name : "");
        if (priceTv != null) priceTv.setText("₹" + price);

        if (cover != null) {
            cover.setImageDrawable(null);
            cover.setBackgroundResource(R.drawable.book_placeholder);
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(this)
                    .load(imageUrl)
                    .apply(RequestOptions.placeholderOf(R.drawable.book_placeholder).fitCenter())
                    .into(cover);
            } else if (img != null && !img.isEmpty()) {
                try {
                    String url = "https://daveashish12356-dotcom.github.io/swamisachidanand-audio/public/book_covers/" + img;
                    Glide.with(this).load(url).apply(RequestOptions.placeholderOf(R.drawable.book_placeholder).fitCenter()).into(cover);
                } catch (Exception ignored) {
                }
            }
        }

        BookStoreItem item = new BookStoreItem();
        item.name = name;
        item.price = price;
        item.img = img;
        item.imageUrl = imageUrl;

        BookStoreItem finalItem = item;
        findViewById(R.id.book_detail_order_btn).setOnClickListener(v -> openWhatsApp(finalItem));
    }

    private void openWhatsApp(BookStoreItem item) {
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
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/919824112625?text=પુસ્તક ઓર્ડર")));
        }
    }
}
