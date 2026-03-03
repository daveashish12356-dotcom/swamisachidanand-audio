package com.swamisachidanand;

/** One item in the unified home history row (book, audio, or video). */
public final class HomeHistoryItem {
    public static final int TYPE_BOOK = 0;
    public static final int TYPE_AUDIO = 1;
    public static final int TYPE_VIDEO = 2;

    public final int type;
    public final Object data;

    public HomeHistoryItem(int type, Object data) {
        this.type = type;
        this.data = data;
    }
}
