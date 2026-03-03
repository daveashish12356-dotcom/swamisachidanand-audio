package com.swamisachidanand;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.widget.VideoView;

/** VideoView jo video ko zoom/crop nahi karta – scene me fit karta hai. */
public class FitVideoView extends VideoView {

    private int videoWidth;
    private int videoHeight;

    public FitVideoView(Context context) {
        super(context);
    }

    public FitVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FitVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /** Call when video dimensions are known (e.g. from OnPreparedListener). */
    public void setVideoDimensions(int w, int h) {
        if (w > 0 && h > 0 && (videoWidth != w || videoHeight != h)) {
            videoWidth = w;
            videoHeight = h;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        if (videoWidth > 0 && videoHeight > 0) {
            float videoAspect = (float) videoWidth / videoHeight;
            float viewAspect = (float) w / h;
            if (viewAspect > videoAspect) {
                h = (int) (w / videoAspect);
            } else {
                w = (int) (h * videoAspect);
            }
        }
        setMeasuredDimension(w, h);
    }
}
