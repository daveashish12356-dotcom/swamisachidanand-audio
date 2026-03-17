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

/**
 * One section = one filter label + horizontal list of books (or grid for "All").
 */
public class BookStoreSectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static class Section {
        public String title;
        public String filterId;
        public List<BookStoreItem> books = new ArrayList<>();
    }

    private final List<Section> sections = new ArrayList<>();
    private final Context context;
    private String coversBaseUrl;
    private BookStoreAdapter.OnBookStoreClickListener clickListener;
    private BookStoreAdapter.OnBookStoreClickListener orderClickListener;
    /** When section title is clicked (e.g. "નવાં પુસ્તકો"), open Books tab with this filter. */
    private OnSectionTitleClickListener sectionTitleClickListener;

    public interface OnSectionTitleClickListener {
        void onSectionTitleClick(String filterId);
    }

    public void setOnSectionTitleClickListener(OnSectionTitleClickListener l) {
        sectionTitleClickListener = l;
    }

    public BookStoreSectionAdapter(Context context) {
        this.context = context;
    }

    public void setCoversBaseUrl(String url) {
        coversBaseUrl = url;
    }

    public void setOnBookStoreClickListener(BookStoreAdapter.OnBookStoreClickListener l) {
        clickListener = l;
    }

    public void setOnOrderClickListener(BookStoreAdapter.OnBookStoreClickListener l) {
        orderClickListener = l;
    }

    public void setSections(List<Section> list) {
        sections.clear();
        if (list != null) sections.addAll(list);
        notifyDataSetChanged();
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_SECTION = 1;

    @Override
    public int getItemViewType(int position) {
        if (position < sections.size() && "header".equals(sections.get(position).filterId))
            return TYPE_HEADER;
        return TYPE_SECTION;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_book_store_info_card, parent, false);
            return new HeaderViewHolder(v);
        }
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_book_store_section, parent, false);
        return new SectionViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            holder.itemView.setAlpha(1f);
            return;
        }
        Section s = position < sections.size() ? sections.get(position) : null;
        if (s == null) return;
        SectionViewHolder sectionHolder = (SectionViewHolder) holder;
        sectionHolder.title.setText(s.title != null ? s.title : "");
        final String filterId = s.filterId;
        sectionHolder.title.setOnClickListener(v -> {
            if (sectionTitleClickListener != null && filterId != null && !"header".equals(filterId)) {
                sectionTitleClickListener.onSectionTitleClick(filterId);
            }
        });
        sectionHolder.title.setClickable(sectionTitleClickListener != null && filterId != null && !"header".equals(filterId));
        sectionHolder.title.setFocusable(sectionHolder.title.isClickable());
        List<BookStoreItem> list = s.books != null ? s.books : new ArrayList<>();
        for (BookStoreItem b : list) {
            if (coversBaseUrl != null && b.img != null && !b.img.isEmpty() && b.imageUrl == null)
                b.imageUrl = coversBaseUrl + b.img;
        }
        boolean isAll = "all".equals(s.filterId);
        if (sectionHolder.booksRecycler.getAdapter() == null) {
            BookStoreAdapter adapter = new BookStoreAdapter();
            adapter.setUseGridLayout(isAll);
            adapter.setOnBookStoreClickListener(clickListener);
            adapter.setOnOrderClickListener(orderClickListener);
            if (isAll) {
                sectionHolder.booksRecycler.setLayoutManager(new GridLayoutManager(context, 2));
                sectionHolder.booksRecycler.setItemViewCacheSize(25);
            } else {
                sectionHolder.booksRecycler.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
            }
            sectionHolder.booksRecycler.setAdapter(adapter);
        } else {
            ((BookStoreAdapter) sectionHolder.booksRecycler.getAdapter()).setUseGridLayout(isAll);
            if (sectionHolder.booksRecycler.getLayoutManager() instanceof GridLayoutManager != isAll) {
                if (isAll) {
                    sectionHolder.booksRecycler.setLayoutManager(new GridLayoutManager(context, 2));
                } else {
                    sectionHolder.booksRecycler.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
                }
            }
        }
        ((BookStoreAdapter) sectionHolder.booksRecycler.getAdapter()).setItems(list);
        sectionHolder.booksRecycler.setHasFixedSize(false);
        if (isAll) {
            int rows = list.isEmpty() ? 1 : (list.size() + 1) / 2;
            int rowHeightDp = 280;
            // 150+ books = 75+ rows – cap high so sab niche scroll me dikhe
            int heightDp = Math.min(rows * rowHeightDp, 25000);
            int px = (int) (heightDp * context.getResources().getDisplayMetrics().density);
            int titlePx = (int) (56 * context.getResources().getDisplayMetrics().density);
            int totalHeightPx = px + titlePx;
            // RecyclerView height
            ViewGroup.LayoutParams rvLp = sectionHolder.booksRecycler.getLayoutParams();
            rvLp.height = px;
            sectionHolder.booksRecycler.setLayoutParams(rvLp);
            sectionHolder.booksRecycler.setNestedScrollingEnabled(false);
            // Force section root to exact height so parent list scrolls & sab books dikhe
            ViewGroup.LayoutParams rootLp = sectionHolder.itemView.getLayoutParams();
            rootLp.height = totalHeightPx;
            sectionHolder.itemView.setLayoutParams(rootLp);
            sectionHolder.itemView.setMinimumHeight(totalHeightPx);
        } else {
            ViewGroup.LayoutParams rootLp = sectionHolder.itemView.getLayoutParams();
            rootLp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            sectionHolder.itemView.setLayoutParams(rootLp);
            sectionHolder.itemView.setMinimumHeight(0);
            ViewGroup.LayoutParams lp = sectionHolder.booksRecycler.getLayoutParams();
            lp.height = (int) (220 * context.getResources().getDisplayMetrics().density);
            sectionHolder.booksRecycler.setLayoutParams(lp);
            sectionHolder.booksRecycler.setNestedScrollingEnabled(false);
        }
    }

    @Override
    public int getItemCount() {
        return sections.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        HeaderViewHolder(View itemView) {
            super(itemView);
        }
    }

    static class SectionViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        RecyclerView booksRecycler;

        SectionViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.section_title);
            booksRecycler = itemView.findViewById(R.id.section_books);
        }
    }
}
