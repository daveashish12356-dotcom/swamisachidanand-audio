package com.swamisachidanand;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PravachanItem implements Parcelable {
    public final String id;
    public final String title;
    public final String speaker;
    public final String audioUrl;
    public final long createdAtMillis;

    public PravachanItem(@NonNull String id,
                         @NonNull String title,
                         @NonNull String audioUrl,
                         @Nullable String speaker,
                         long createdAtMillis) {
        this.id = id;
        this.title = title;
        this.audioUrl = audioUrl;
        this.speaker = speaker != null ? speaker : "";
        this.createdAtMillis = createdAtMillis;
    }

    protected PravachanItem(Parcel in) {
        id = in.readString();
        title = in.readString();
        speaker = in.readString();
        audioUrl = in.readString();
        createdAtMillis = in.readLong();
    }

    public static final Creator<PravachanItem> CREATOR = new Creator<PravachanItem>() {
        @Override
        public PravachanItem createFromParcel(Parcel in) {
            return new PravachanItem(in);
        }

        @Override
        public PravachanItem[] newArray(int size) {
            return new PravachanItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(speaker);
        dest.writeString(audioUrl);
        dest.writeLong(createdAtMillis);
    }
}

