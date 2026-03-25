package com.swamisachidanand;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Dim full screen with a clear "hole" over a target (coach mark).
 * Software layer so CLEAR xfermode works on all devices.
 */
public class TourSpotlightView extends View {

    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    @Nullable
    private Rect holeRect;
    private float padDp = 8f;

    public TourSpotlightView(Context context) {
        super(context);
        init();
    }

    public TourSpotlightView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        dimPaint.setColor(0xE6000000);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(Math.max(3f, getResources().getDisplayMetrics().density * 3f));
        ringPaint.setColor(0xFFFF9800);
    }

    public void setHoleRect(@Nullable Rect rectInWindow, int paddingPx) {
        if (rectInWindow == null || rectInWindow.isEmpty()) {
            holeRect = null;
        } else {
            holeRect = new Rect(rectInWindow);
            holeRect.inset(-paddingPx, -paddingPx);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        int save = canvas.saveLayer(0, 0, w, h, null);
        canvas.drawRect(0, 0, w, h, dimPaint);

        if (holeRect != null && !holeRect.isEmpty()) {
            RectF rf = new RectF(holeRect);
            float r = 20f * getResources().getDisplayMetrics().density;
            canvas.drawRoundRect(rf, r, r, clearPaint);
            canvas.drawRoundRect(rf, r, r, ringPaint);
        }
        canvas.restoreToCount(save);
    }
}
