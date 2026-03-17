package com.swamisachidanand;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Vertical list of sections; each section = title + horizontal list of PDF books (like Book Store photo).
 */
public class BooksSectionAdapter extends RecyclerView.Adapter<BooksSectionAdapter.SectionViewHolder> {

    public static class Section {
        public String title;
        public String filterId;
        public List<Book> books = new ArrayList<>();
    }

    private final List<Section> sections = new ArrayList<>();
    private final Context context;
    private BookAdapter.OnBookClickListener clickListener;
    private Map<String, Integer> readingProgressMap;

    public BooksSectionAdapter(Context context) {
        this.context = context;
    }

    public void setOnBookClickListener(BookAdapter.OnBookClickListener l) {
        clickListener = l;
    }

    public void setReadingProgressMap(Map<String, Integer> map) {
        this.readingProgressMap = map;
    }

    public void setSections(List<Section> list) {
        sections.clear();
        if (list != null) sections.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_books_section, parent, false);
        return new SectionViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SectionViewHolder holder, int position) {
        Section s = position < sections.size() ? sections.get(position) : null;
        if (s == null) return;
        holder.title.setText(s.title != null ? s.title : "");
        List<Book> list = s.books != null ? s.books : new ArrayList<>();
        boolean isAllSection = "all".equals(s.filterId);
        if (holder.recycler.getAdapter() == null) {
            BookAdapter adapter = new BookAdapter(list, clickListener);
            adapter.setUseCompactLayout(!isAllSection);
            if (isAllSection) {
                holder.recycler.setLayoutManager(new GridLayoutManager(context, 2));
                holder.recycler.setItemViewCacheSize(30);
            } else {
                holder.recycler.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
            }
            holder.recycler.setAdapter(adapter);
        } else {
            ((BookAdapter) holder.recycler.getAdapter()).setUseCompactLayout(!isAllSection);
            if (holder.recycler.getLayoutManager() instanceof GridLayoutManager != isAllSection) {
                if (isAllSection) {
                    holder.recycler.setLayoutManager(new GridLayoutManager(context, 2));
                    holder.recycler.setItemViewCacheSize(30);
                } else {
                    holder.recycler.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
                }
            }
        }
        BookAdapter adapter = (BookAdapter) holder.recycler.getAdapter();
        if (adapter != null) {
            adapter.setReadingProgressMap(readingProgressMap);
            adapter.updateBooks(list);
        }
        float density = context.getResources().getDisplayMetrics().density;
        if (isAllSection) {
            int rows = list.isEmpty() ? 1 : (list.size() + 1) / 2;
            int rowHeightDp = 280;
            int heightDp = Math.min(rows * rowHeightDp, 25000);
            int px = (int) (heightDp * density);
            ViewGroup.LayoutParams lp = holder.recycler.getLayoutParams();
            lp.height = px;
            holder.recycler.setLayoutParams(lp);
            holder.recycler.setNestedScrollingEnabled(false);
        } else {
            int px = (int) (220 * density);
            ViewGroup.LayoutParams lp = holder.recycler.getLayoutParams();
            lp.height = px;
            holder.recycler.setLayoutParams(lp);
            holder.recycler.setNestedScrollingEnabled(false);
        }
    }

    @Override
    public int getItemCount() {
        return sections.size();
    }

    static class SectionViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        RecyclerView recycler;

        SectionViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.books_section_title);
            recycler = itemView.findViewById(R.id.books_section_recycler);
        }
    }
}
