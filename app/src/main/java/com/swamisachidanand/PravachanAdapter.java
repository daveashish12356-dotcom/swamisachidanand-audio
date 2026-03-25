package com.swamisachidanand;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PravachanAdapter extends RecyclerView.Adapter<PravachanAdapter.ViewHolder> {

    public interface Listener {
        void onPravachanClick(@NonNull PravachanItem item, int adapterPosition);
    }

    public interface InlinePlayerViewsListener {
        void onInlinePlayerViewsReady(
                @NonNull ImageButton playBtn,
                @NonNull ImageButton back10Btn,
                @NonNull ImageButton forward10Btn,
                @NonNull SeekBar seekBar,
                @NonNull TextView timeCurrent,
                @NonNull TextView timeTotal
        );
    }

    private final List<PravachanItem> items = new ArrayList<>();
    private Listener listener;
    private InlinePlayerViewsListener inlinePlayerViewsListener;
    private String expandedItemId;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setInlinePlayerViewsListener(InlinePlayerViewsListener listener) {
        this.inlinePlayerViewsListener = listener;
    }

    public void setItems(List<PravachanItem> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    public void setExpandedItemId(String expandedItemId) {
        this.expandedItemId = expandedItemId;
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

        boolean isExpanded = expandedItemId != null && expandedItemId.equals(item.id);
        holder.inlinePlayerContainer.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        // Use bindingAdapterPosition — captured `position` can be wrong after recycle / notifyDataSetChanged.
        View.OnClickListener click = v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION || listener == null) return;
            if (pos < 0 || pos >= items.size()) return;
            PravachanItem clicked = items.get(pos);
            listener.onPravachanClick(clicked, pos);
        };
        holder.itemView.setOnClickListener(click);
        if (holder.playButton != null) {
            holder.playButton.setOnClickListener(click);
        }

        if (holder.inlinePlayButton != null) {
            holder.inlinePlayButton.setOnClickListener(click);
        }

        if (isExpanded && inlinePlayerViewsListener != null
                && holder.inlinePlayButton != null && holder.inlineSeekBar != null
                && holder.inlineTimeCurrent != null && holder.inlineTimeTotal != null
                && holder.inlineBack10Button != null && holder.inlineForward10Button != null) {
            inlinePlayerViewsListener.onInlinePlayerViewsReady(
                    holder.inlinePlayButton,
                    holder.inlineBack10Button,
                    holder.inlineForward10Button,
                    holder.inlineSeekBar,
                    holder.inlineTimeCurrent,
                    holder.inlineTimeTotal
            );
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
        final LinearLayout inlinePlayerContainer;
        final ImageButton inlinePlayButton;
        final ImageButton inlineBack10Button;
        final ImageButton inlineForward10Button;
        final SeekBar inlineSeekBar;
        final TextView inlineTimeCurrent;
        final TextView inlineTimeTotal;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.pravachan_item_title);
            meta = itemView.findViewById(R.id.pravachan_item_meta);
            playButton = itemView.findViewById(R.id.pravachan_item_play);
            inlinePlayerContainer = itemView.findViewById(R.id.pravachan_inline_player_container);
            inlinePlayButton = itemView.findViewById(R.id.pravachan_inline_play);
            inlineBack10Button = itemView.findViewById(R.id.pravachan_inline_back_10);
            inlineForward10Button = itemView.findViewById(R.id.pravachan_inline_forward_10);
            inlineSeekBar = itemView.findViewById(R.id.pravachan_inline_seek);
            inlineTimeCurrent = itemView.findViewById(R.id.pravachan_inline_time_current);
            inlineTimeTotal = itemView.findViewById(R.id.pravachan_inline_time_total);
        }
    }
}

