package com.swamisachidanand;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PravachanAdapter extends RecyclerView.Adapter<PravachanAdapter.ViewHolder> {

    public interface Listener {
        void onPravachanClick(@NonNull PravachanItem item);
    }

    private final List<PravachanItem> items = new ArrayList<>();
    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<PravachanItem> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pravachan, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PravachanItem item = items.get(position);
        holder.title.setText(item.title);
        StringBuilder meta = new StringBuilder();
        if (item.speaker != null && !item.speaker.isEmpty()) {
            meta.append(item.speaker);
        }
        if (item.createdAtMillis > 0) {
            if (meta.length() > 0) meta.append(" • ");
            DateFormat df = android.text.format.DateFormat.getDateFormat(holder.itemView.getContext());
            meta.append(df.format(new Date(item.createdAtMillis)));
        }
        holder.meta.setText(meta.toString());
        View.OnClickListener click = v -> {
            if (listener != null) listener.onPravachanClick(item);
        };
        holder.itemView.setOnClickListener(click);
        if (holder.playButton != null) {
            holder.playButton.setOnClickListener(click);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView meta;
        final View playButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.pravachan_item_title);
            meta = itemView.findViewById(R.id.pravachan_item_meta);
            playButton = itemView.findViewById(R.id.pravachan_item_play);
        }
    }
}

