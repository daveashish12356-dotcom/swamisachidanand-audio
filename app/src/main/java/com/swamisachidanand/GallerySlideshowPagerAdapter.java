package com.swamisachidanand;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import java.util.ArrayList;
import java.util.List;

/** ViewPager2 adapter for top-of-gallery slideshow – video-like auto-playing photos. */
public class GallerySlideshowPagerAdapter extends RecyclerView.Adapter<GallerySlideshowPagerAdapter.SlideViewHolder> {

    public interface Listener {
        void onSlideClick(int position);

        /** Called when image fails to load so caller can skip it from slideshow. */
        void onSlideLoadFailed(int position);
    }

    private final List<String> urls = new ArrayList<>();
    // Track which positions have fully loaded at least once
    private final List<Boolean> loadedFlags = new ArrayList<>();
    private Listener listener;

    public void setListener(Listener l) {
        listener = l;
    }

    public void setUrls(List<String> list) {
        urls.clear();
        if (list != null) urls.addAll(list);
        loadedFlags.clear();
        for (int i = 0; i < urls.size(); i++) {
            loadedFlags.add(Boolean.FALSE);
        }
        notifyDataSetChanged();
    }

    /** Return true only when given position image has finished loading at least once. */
    public boolean isLoaded(int position) {
        if (position < 0 || position >= loadedFlags.size()) return false;
        Boolean b = loadedFlags.get(position);
        return b != null && b;
    }

    @NonNull
    @Override
    public SlideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo_slideshow, parent, false);
        return new SlideViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SlideViewHolder holder, int position) {
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSlideClick(position);
        });
        String url = position < urls.size() ? urls.get(position) : null;
        if (holder.image != null && url != null) {
            Glide.with(holder.image.getContext())
                .load(url)
                // Thumbnail pehle chhota version laayega → tez first paint
                .thumbnail(0.2f)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .transition(DrawableTransitionOptions.withCrossFade(200))
                // Network + decode हलका रहे – roughly 1024px तक ही
                .override(1024, 1024)
                .centerCrop()
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        if (listener != null) {
                            int pos = holder.getBindingAdapterPosition();
                            if (pos != RecyclerView.NO_POSITION) {
                                // Treat failed load as \"done\" so caller can skip it
                                if (pos < loadedFlags.size()) {
                                    loadedFlags.set(pos, Boolean.TRUE);
                                }
                                listener.onSlideLoadFailed(pos);
                            }
                        }
                        return false; // default error placeholder દેખાવા દો
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        int pos = holder.getBindingAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION && pos < loadedFlags.size()) {
                            loadedFlags.set(pos, Boolean.TRUE);
                        }
                        return false;
                    }
                })
                .into(holder.image);

            // अगला slide background में preload – ताकि swipe / auto-advance पर instant दिखे
            int nextIndex = position + 1;
            if (nextIndex < urls.size()) {
                String nextUrl = urls.get(nextIndex);
                if (nextUrl != null && !nextUrl.isEmpty()) {
                    Glide.with(holder.image.getContext())
                        .load(nextUrl)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .preload();
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return urls.isEmpty() ? 0 : urls.size();
    }

    static class SlideViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;

        SlideViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.slideshow_image);
        }
    }
}
