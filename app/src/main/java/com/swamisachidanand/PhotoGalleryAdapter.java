package com.swamisachidanand;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import java.util.ArrayList;
import java.util.List;

/** Grid adapter – full photo visible (aspect ratio), tap opens full-screen slideshow. */
public class PhotoGalleryAdapter extends RecyclerView.Adapter<PhotoGalleryAdapter.PhotoViewHolder> {

    public interface OnPhotoClickListener {
        void onPhotoClick(int position);
    }

    private final List<String> imageUrls = new ArrayList<>();
    private int columnWidthPx = 0;
    private OnPhotoClickListener photoClickListener;

    public void setColumnWidthPx(int widthPx) {
        columnWidthPx = widthPx;
    }

    public void setOnPhotoClickListener(OnPhotoClickListener listener) {
        photoClickListener = listener;
    }

    public void setImageUrls(List<String> urls) {
        imageUrls.clear();
        if (urls != null) imageUrls.addAll(urls);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo_gallery, parent, false);
        return new PhotoViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        holder.itemView.setOnClickListener(v -> {
            if (photoClickListener != null) photoClickListener.onPhotoClick(position);
        });
        String url = position < imageUrls.size() ? imageUrls.get(position) : null;
        if (url != null && holder.image != null) {
            int w = columnWidthPx > 0 ? columnWidthPx : holder.image.getContext().getResources().getDisplayMetrics().widthPixels / 3;
            Glide.with(holder.image.getContext())
                .load(url)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .transition(DrawableTransitionOptions.withCrossFade(200))
                .fitCenter()
                .addListener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) { return false; }
                    @Override
                    public boolean onResourceReady(Drawable r, Object model, Target<Drawable> t, DataSource ds, boolean isFirstResource) {
                        if (r != null && r.getIntrinsicWidth() > 0 && r.getIntrinsicHeight() > 0 && holder.image != null) {
                            int h = (int) (w * (float) r.getIntrinsicHeight() / r.getIntrinsicWidth());
                            ViewGroup.LayoutParams lp = holder.image.getLayoutParams();
                            if (lp != null && lp.height != h) {
                                lp.height = h;
                                holder.image.setLayoutParams(lp);
                            }
                        }
                        return false;
                    }
                })
                .into(holder.image);
        } else if (holder.image != null) {
            holder.image.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView image;

        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.gallery_photo_image);
        }
    }
}
