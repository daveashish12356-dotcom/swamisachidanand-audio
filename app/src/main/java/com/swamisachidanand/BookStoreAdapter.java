package com.swamisachidanand;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Adapter for book store cards (cover + title + price). */
public class BookStoreAdapter extends RecyclerView.Adapter<BookStoreAdapter.ViewHolder> {

    private final List<BookStoreItem> items = new ArrayList<>();
    private OnBookStoreClickListener listener;
    private boolean useGridLayout;

    public void setUseGridLayout(boolean grid) {
        useGridLayout = grid;
    }

    public interface OnBookStoreClickListener {
        void onBookStoreClick(BookStoreItem item);
    }

    public void setItems(List<BookStoreItem> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    public void setOnBookStoreClickListener(OnBookStoreClickListener l) {
        listener = l;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = useGridLayout ? R.layout.item_book_store_grid : R.layout.item_book_store_card;
        View v = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookStoreItem item = items.get(position);
        holder.title.setText(item.name != null ? item.name : "");
        holder.price.setText("₹" + item.price);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onBookStoreClick(item);
        });

        holder.cover.setImageDrawable(null);
        holder.cover.setBackgroundResource(R.drawable.book_placeholder);
        if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                .load(item.imageUrl)
                .apply(RequestOptions.placeholderOf(R.drawable.book_placeholder).centerCrop())
                .into(holder.cover);
        } else if (item.img != null && !item.img.isEmpty()) {
            try {
                InputStream is = holder.itemView.getContext().getAssets().open("book_covers/" + item.img);
                Bitmap bmp = BitmapFactory.decodeStream(is);
                if (bmp != null) {
                    holder.cover.setImageBitmap(bmp);
                    holder.cover.setBackground(null);
                }
                is.close();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView title;
        TextView price;

        ViewHolder(View v) {
            super(v);
            cover = v.findViewById(R.id.book_store_cover);
            title = v.findViewById(R.id.book_store_title);
            price = v.findViewById(R.id.book_store_price);
        }
    }
}
