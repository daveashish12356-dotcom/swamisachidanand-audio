package com.swamisachidanand;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/** Grid cards for “દૈનિક પ્રવચન” — title + accent color (MP3 list later). */
public class DainikPravachanCategoryAdapter extends RecyclerView.Adapter<DainikPravachanCategoryAdapter.Holder> {

    public static final class Row {
        public final String title;
        public final int accentColor;

        public Row(@NonNull String title, int accentColor) {
            this.title = title;
            this.accentColor = accentColor;
        }
    }

    public interface Listener {
        void onCategoryClick(@NonNull String title);
    }

    private final List<Row> rows = new ArrayList<>();
    @Nullable
    private Listener listener;

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public void setRows(@Nullable List<Row> list) {
        rows.clear();
        if (list != null) rows.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dainik_pravachan_category, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Row r = rows.get(position);
        h.title.setText(r.title);
        h.colorBlock.setBackgroundColor(0xFF000000 | r.accentColor);
        h.card.setStrokeColor(0xFF000000 | r.accentColor);
        h.card.setOnClickListener(v -> {
            if (listener != null) listener.onCategoryClick(r.title);
        });
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final View colorBlock;
        final TextView title;

        Holder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.dainik_category_card);
            colorBlock = itemView.findViewById(R.id.dainik_category_color_block);
            title = itemView.findViewById(R.id.dainik_category_title);
        }
    }
}
