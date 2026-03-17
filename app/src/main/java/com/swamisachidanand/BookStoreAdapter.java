package com.swamisachidanand;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

/** Adapter for book store cards (cover + title + price). Covers સર્વરથી જ – assets નો ઉપયોગ નહીં. */
public class BookStoreAdapter extends RecyclerView.Adapter<BookStoreAdapter.ViewHolder> {

    private static final String SERVER_COVERS_BASE = "https://daveashish12356-dotcom.github.io/swamisachidanand-audio/public/book_covers/";

    private final List<BookStoreItem> items = new ArrayList<>();
    private OnBookStoreClickListener listener;
    private OnBookStoreClickListener orderClickListener;
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

    public void setOnOrderClickListener(OnBookStoreClickListener l) {
        orderClickListener = l;
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
        if (holder.orderBtn != null) {
            holder.orderBtn.setText("ઓર્ડર કરો");
            holder.orderBtn.setOnClickListener(v -> {
                if (orderClickListener != null) orderClickListener.onBookStoreClick(item);
                else if (listener != null) listener.onBookStoreClick(item);
            });
        }

        holder.cover.setImageDrawable(null);
        holder.cover.setBackgroundResource(R.drawable.book_placeholder);
        String coverUrl = (item.imageUrl != null && !item.imageUrl.isEmpty())
            ? item.imageUrl
            : (item.img != null && !item.img.isEmpty() ? SERVER_COVERS_BASE + item.img : null);
        if (coverUrl != null) {
            Glide.with(holder.itemView.getContext())
                .load(coverUrl)
                .apply(RequestOptions.placeholderOf(R.drawable.book_placeholder).fitCenter())
                .into(holder.cover);
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
        TextView orderBtn;

        ViewHolder(View v) {
            super(v);
            cover = v.findViewById(R.id.book_store_cover);
            title = v.findViewById(R.id.book_store_title);
            price = v.findViewById(R.id.book_store_price);
            orderBtn = v.findViewById(R.id.book_store_order_btn);
        }
    }
}
