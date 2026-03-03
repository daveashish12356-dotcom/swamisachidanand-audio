package com.swamisachidanand;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/** Adds horizontal spacing between items in a horizontal RecyclerView. */
public class HorizontalSpacingItemDecoration extends RecyclerView.ItemDecoration {

    private final int spacingPx;

    public HorizontalSpacingItemDecoration(int spacingPx) {
        this.spacingPx = spacingPx;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        outRect.top = 0;
        outRect.bottom = 0;
        outRect.left = spacingPx / 2;
        outRect.right = spacingPx / 2;
    }
}
