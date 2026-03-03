package com.swamisachidanand;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * One audio part from server (audio_list.json).
 * durationSeconds: from JSON "duration" or "durationSeconds", or 0 if unknown.
 */
public class ServerAudioPart implements Parcelable {
    private final String id;
    private final String title;
    private final String url;
    private final int durationSeconds;

    public ServerAudioPart(String id, String title, String url) {
        this(id, title, url, 0);
    }

    public ServerAudioPart(String id, String title, String url, int durationSeconds) {
        this.id = id;
        this.title = title != null ? title : "";
        this.url = url != null ? url : "";
        this.durationSeconds = durationSeconds >= 0 ? durationSeconds : 0;
    }

    protected ServerAudioPart(Parcel in) {
        id = in.readString();
        title = in.readString();
        url = in.readString();
        durationSeconds = in.readInt();
    }

    public static final Creator<ServerAudioPart> CREATOR = new Creator<ServerAudioPart>() {
        @Override
        public ServerAudioPart createFromParcel(Parcel in) { return new ServerAudioPart(in); }
        @Override
        public ServerAudioPart[] newArray(int size) { return new ServerAudioPart[size]; }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(url);
        dest.writeInt(durationSeconds);
    }

    @Override
    public int describeContents() { return 0; }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    /** Duration in seconds, 0 if unknown. */
    public int getDurationSeconds() { return durationSeconds; }
}
