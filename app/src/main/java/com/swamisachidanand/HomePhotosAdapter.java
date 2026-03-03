package com.swamisachidanand;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import java.util.ArrayList;
import java.util.List;

/** Simple horizontal photo strip – Swamiji / Ashram images. */
public class HomePhotosAdapter extends RecyclerView.Adapter<HomePhotosAdapter.Holder> {

    private final List<PhotoItem> items = new ArrayList<>();

    public void setItems(List<PhotoItem> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_photo, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        PhotoItem item = items.get(position);
        if (item.drawableRes != 0) {
            h.img.setImageResource(item.drawableRes);
        } else if (item.url != null && !item.url.isEmpty()) {
            Glide.with(h.itemView.getContext())
                    .load(item.url)
                    .transform(new RoundedCorners(24))
                    .centerCrop()
                    .placeholder(R.drawable.book_placeholder)
                    .error(R.drawable.book_placeholder)
                    .into(h.img);
        } else {
            h.img.setImageResource(R.drawable.book_placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView img;
        Holder(View v) {
            super(v);
            img = v.findViewById(R.id.home_photo_image);
        }
    }

    public static class PhotoItem {
        public int drawableRes;
        public String url;

        public PhotoItem(int drawableRes) {
            this.drawableRes = drawableRes;
            this.url = null;
        }

        public PhotoItem(String url) {
            this.drawableRes = 0;
            this.url = url;
        }
    }
}
