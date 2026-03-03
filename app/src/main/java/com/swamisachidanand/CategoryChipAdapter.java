package com.swamisachidanand;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CategoryChipAdapter extends RecyclerView.Adapter<CategoryChipAdapter.ChipViewHolder> {
    private final List<String> categories;
    private final List<String> categoryIds;
    private int selectedIndex = 0;
    private OnCategorySelectedListener listener;

    public interface OnCategorySelectedListener {
        void onCategorySelected(String categoryId);
    }

    public CategoryChipAdapter(List<String> categories, List<String> categoryIds) {
        this.categories = categories;
        this.categoryIds = categoryIds;
    }

    public void setListener(OnCategorySelectedListener listener) {
        this.listener = listener;
    }

    public void setSelectedIndex(int index) {
        int old = selectedIndex;
        selectedIndex = index;
        if (old >= 0 && old < getItemCount()) notifyItemChanged(old);
        if (selectedIndex >= 0 && selectedIndex < getItemCount()) notifyItemChanged(selectedIndex);
    }

    @NonNull
    @Override
    public ChipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_chip, parent, false);
        return new ChipViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ChipViewHolder holder, int position) {
        String label = position < categories.size() ? categories.get(position) : "";
        holder.text.setText(label);
        boolean selected = (position == selectedIndex);
        holder.text.setSelected(selected);
        holder.text.setBackgroundResource(selected ? R.drawable.chip_selected : R.drawable.chip_unselected);
        holder.text.setTextColor(androidx.core.content.ContextCompat.getColor(holder.text.getContext(),
                selected ? android.R.color.white : com.swamisachidanand.R.color.text_primary));
        holder.itemView.setOnClickListener(v -> {
            setSelectedIndex(position);
            if (listener != null && position < categoryIds.size()) {
                listener.onCategorySelected(categoryIds.get(position));
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class ChipViewHolder extends RecyclerView.ViewHolder {
        TextView text;

        ChipViewHolder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.chip_text);
        }
    }
}
