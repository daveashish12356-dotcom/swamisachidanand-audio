package com.swamisachidanand;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class HomeVideoAdapter extends RecyclerView.Adapter<HomeVideoAdapter.VH> {

    private final List<HomeVideoLoader.HomeVideoItem> items = new ArrayList<>();
    private String openVideoVideoId;

    void setItems(List<HomeVideoLoader.HomeVideoItem> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_horizontal, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        HomeVideoLoader.HomeVideoItem item = items.get(pos);
        h.title.setText(item.title != null ? item.title : "");
        String meta = HomeVideoLoader.toRelativeTime(item.publishedAt) + (item.viewCount >= 0 ? " • " + HomeVideoLoader.formatViewCount(item.viewCount) : "");
        h.meta.setText(meta);
        if (item.durationSeconds >= 0) {
            h.duration.setText(HomeVideoLoader.formatDuration(item.durationSeconds));
            h.duration.setVisibility(View.VISIBLE);
        } else {
            h.duration.setVisibility(View.GONE);
        }
        if (item.thumbnailUrl != null && !item.thumbnailUrl.isEmpty()) {
            Glide.with(h.thumb.getContext()).load(item.thumbnailUrl)
                    .placeholder(R.drawable.book_placeholder).error(R.drawable.book_placeholder)
                    .centerCrop().into(h.thumb);
        } else {
            h.thumb.setImageResource(R.drawable.book_placeholder);
        }
        h.itemView.setOnClickListener(v -> {
            if (item.videoId != null) {
                RecentVideoHelper.saveRecentVideoId(v.getContext(), item.videoId);
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + item.videoId));
                v.getContext().startActivity(i);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView thumb;
        final TextView title, meta, duration;

        VH(View v) {
            super(v);
            thumb = v.findViewById(R.id.video_thumb);
            title = v.findViewById(R.id.video_title);
            meta = v.findViewById(R.id.video_meta);
            duration = v.findViewById(R.id.video_duration);
        }
    }
}
