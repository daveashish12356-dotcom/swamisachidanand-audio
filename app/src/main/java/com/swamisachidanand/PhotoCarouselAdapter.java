package com.swamisachidanand;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Carousel: pehla item VIDEO (thumbnail + play), baaki photos with different animations. */
public class PhotoCarouselAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_VIDEO = 0;
    private static final int VIEW_TYPE_PHOTO = 1;

    private final List<String> photoFiles = new ArrayList<>();
    private final String videoAssetPath;
    private final String videoThumbAssetPath;
    private final android.content.res.AssetManager assetManager;
    private final android.content.Context context;
    private OnVideoPlayListener videoPlayListener;

    public interface OnVideoPlayListener {
        void onPlayVideo(String assetPath, View videoItemView);
    }

    public PhotoCarouselAdapter(android.content.res.AssetManager assetManager, android.content.Context context) {
        this.assetManager = assetManager;
        this.context = context;
        this.videoAssetPath = "padma_bhushan_video.mp4";
        this.videoThumbAssetPath = "swamiji.jpg";

        // Order: video is position 0 (added in getItemCount). Photos below.
        photoFiles.add("F0GoDmQacAAaPKX.jpg");
        photoFiles.add("6f93aa5c-c068-4363-825f-b07b253ac9bf-md.jpg");
        photoFiles.add("e18b218e-6ec2-4f2d-ac3c-1c9b894cc42b-md.jpg");
        photoFiles.add("home_photo1.jpg");
        photoFiles.add("home_photo2.jpg");
        photoFiles.add("home_photo3.jpg");
        photoFiles.add("home_photo4.webp");
        photoFiles.add("swamiji.jpg");
    }

    public void setOnVideoPlayListener(OnVideoPlayListener listener) {
        this.videoPlayListener = listener;
    }

    public String getVideoAssetPath() {
        return videoAssetPath;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_VIDEO : VIEW_TYPE_PHOTO;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo_carousel, parent, false);
        if (viewType == VIEW_TYPE_VIDEO) {
            return new VideoViewHolder(view);
        }
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        try {
            if (holder instanceof VideoViewHolder) {
                bindVideo((VideoViewHolder) holder);
                return;
            }
            if (holder instanceof PhotoViewHolder) {
                int photoIndex = position - 1; // first item is video
                if (photoIndex >= 0 && photoIndex < photoFiles.size()) {
                    bindPhoto((PhotoViewHolder) holder, photoFiles.get(photoIndex), photoIndex);
                }
            }
        } catch (Throwable t) {
            android.util.Log.e("PhotoCarousel", "onBindViewHolder error", t);
        }
    }

    private void bindVideo(VideoViewHolder holder) {
        View item = holder.itemView;
        if (item == null) return;
        setItemWidth(item);
        ImageView imageView = holder.imageView;
        ImageView playOverlay = holder.playOverlay;
        if (imageView != null) {
            imageView.setVisibility(View.VISIBLE);
            loadBitmapInto(videoThumbAssetPath, imageView);
        }
        if (holder.carouselVideoView != null) {
            holder.carouselVideoView.setVisibility(View.GONE);
            holder.carouselVideoView.stopPlayback();
            holder.carouselVideoView.setOnClickListener(null);
        }
        if (playOverlay != null) {
            playOverlay.setVisibility(View.VISIBLE);
            playOverlay.setOnClickListener(v -> {
                android.util.Log.d("PhotoCarousel", "Video play overlay tapped");
                if (videoPlayListener != null) videoPlayListener.onPlayVideo(videoAssetPath, item);
            });
        }
        if (item != null) {
            item.setOnClickListener(v -> {
                android.util.Log.d("PhotoCarousel", "Video card tapped");
                if (videoPlayListener != null) videoPlayListener.onPlayVideo(videoAssetPath, item);
            });
        }
    }

    private void bindPhoto(PhotoViewHolder holder, String photoFile, int photoIndex) {
        View item = holder.itemView;
        if (item == null) return;
        setItemWidth(item);
        ImageView imageView = holder.imageView;
        ImageView playOverlay = holder.playOverlay;
        if (playOverlay != null) {
            playOverlay.setVisibility(View.GONE);
            playOverlay.setOnClickListener(null);
        }
        if (item != null) item.setOnClickListener(null);

        if (imageView != null) {
            imageView.setVisibility(View.VISIBLE);
            loadBitmapInto(photoFile, imageView);
            applyPhotoAnimation(imageView, photoIndex);
        }
    }

    private void setItemWidth(View item) {
        try {
            if (item.getParent() instanceof RecyclerView) {
                RecyclerView rv = (RecyclerView) item.getParent();
                int w = rv != null ? rv.getWidth() : 0;
                if (w > 0) {
                    ViewGroup.LayoutParams lp = item.getLayoutParams();
                    if (lp instanceof RecyclerView.LayoutParams && ((RecyclerView.LayoutParams) lp).width != w) {
                        ((RecyclerView.LayoutParams) lp).width = w;
                        item.setLayoutParams(lp);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void loadBitmapInto(String assetPath, ImageView imageView) {
        InputStream is = null;
        try {
            is = assetManager.open(assetPath);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } catch (Throwable e) {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        } finally {
            if (is != null) try { is.close(); } catch (IOException ignored) {}
        }
    }

    private void applyPhotoAnimation(ImageView imageView, int photoIndex) {
        if (imageView == null) return;
        imageView.clearAnimation();
        int kind = photoIndex % 6;
        long duration = 650;
        OvershootInterpolator overshoot = new OvershootInterpolator(1.1f);
        AccelerateDecelerateInterpolator smooth = new AccelerateDecelerateInterpolator();
        try {
            switch (kind) {
                case 0:
                    imageView.setAlpha(0f);
                    imageView.setScaleX(0.88f);
                    imageView.setScaleY(0.88f);
                    imageView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(duration)
                            .setInterpolator(overshoot).start();
                    break;
                case 1:
                    imageView.setAlpha(0f);
                    imageView.setTranslationY(60f);
                    imageView.animate().alpha(1f).translationY(0f).setDuration(duration)
                            .setInterpolator(smooth).start();
                    break;
                case 2:
                    imageView.setAlpha(0f);
                    imageView.setTranslationX(60f);
                    imageView.animate().alpha(1f).translationX(0f).setDuration(duration)
                            .setInterpolator(smooth).start();
                    break;
                case 3:
                    imageView.setAlpha(0f);
                    imageView.setRotation(-12f);
                    imageView.animate().alpha(1f).rotation(0f).setDuration(duration)
                            .setInterpolator(overshoot).start();
                    break;
                case 4:
                    ScaleAnimation scale = new ScaleAnimation(0.75f, 1f, 0.75f, 1f,
                            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                    scale.setDuration(duration);
                    scale.setInterpolator(overshoot);
                    imageView.startAnimation(scale);
                    imageView.animate().alpha(1f).setDuration(duration).start();
                    break;
                default:
                    imageView.setAlpha(0f);
                    imageView.animate().alpha(1f).setDuration(duration)
                            .setInterpolator(smooth).start();
                    break;
            }
        } catch (Exception e) {
            imageView.setAlpha(1f);
        }
    }

    @Override
    public int getItemCount() {
        return 1 + photoFiles.size(); // 1 video + photos
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof VideoViewHolder) {
            VideoViewHolder vh = (VideoViewHolder) holder;
            if (vh.carouselVideoView != null) {
                vh.carouselVideoView.stopPlayback();
                vh.carouselVideoView.setVisibility(View.GONE);
            }
            if (vh.imageView != null) vh.imageView.setVisibility(View.VISIBLE);
            if (vh.playOverlay != null) vh.playOverlay.setVisibility(View.VISIBLE);
        }
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView playOverlay;
        VideoView carouselVideoView;

        VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            try {
                imageView = itemView.findViewById(R.id.photo_image);
                playOverlay = itemView.findViewById(R.id.play_overlay);
                carouselVideoView = itemView.findViewById(R.id.carousel_video_view);
            } catch (Exception e) {
                android.util.Log.e("PhotoCarousel", "Error init VideoViewHolder", e);
            }
        }
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView playOverlay;

        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            try {
                imageView = itemView.findViewById(R.id.photo_image);
                playOverlay = itemView.findViewById(R.id.play_overlay);
            } catch (Exception e) {
                android.util.Log.e("PhotoCarousel", "Error init PhotoViewHolder", e);
            }
        }
    }
}
