package com.swamisachidanand;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    private List<CategoryItem> categories;
    private OnCategoryClickListener listener;
    private int selectedPosition = 0; // "All" is selected by default

    public interface OnCategoryClickListener {
        void onCategoryClick(String category);
    }

    public CategoryAdapter(List<CategoryItem> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_chip, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryItem category = categories.get(position);
        holder.chip.setText(category.getName());
        
        boolean isSelected = position == selectedPosition;
        holder.chip.setChecked(isSelected);
        
        // Update chip appearance based on selection with beautiful styling
        if (isSelected) {
            holder.chip.setChipBackgroundColorResource(R.color.bhagva_dark);
            holder.chip.setChipStrokeColorResource(R.color.bhagva_darker);
            holder.chip.setChipStrokeWidth(3f);
            holder.chip.setElevation(6f);
        } else {
            holder.chip.setChipBackgroundColorResource(R.color.bhagva);
            holder.chip.setChipStrokeColorResource(R.color.bhagva_dark);
            holder.chip.setChipStrokeWidth(2f);
            holder.chip.setElevation(2f);
        }
        
        holder.chip.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return;
            }
            int previousSelected = selectedPosition;
            selectedPosition = adapterPosition;
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
            
            if (listener != null) {
                CategoryItem clickedCategory = categories.get(adapterPosition);
                listener.onCategoryClick(clickedCategory.getCategory());
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        Chip chip;

        CategoryViewHolder(View itemView) {
            super(itemView);
            chip = (Chip) itemView;
        }
    }

    public static class CategoryItem {
        private String name;
        private String category; // "All", "Bhakti", "Yatra", "Updesh", "Jeevan"

        public CategoryItem(String name, String category) {
            this.name = name;
            this.category = category;
        }

        public String getName() {
            return name;
        }

        public String getCategory() {
            return category;
        }
    }
}

