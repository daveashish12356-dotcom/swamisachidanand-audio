package com.swamisachidanand;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * One book from server audio list (has many parts).
 * thumbnailUrl: optional cover image URL from server – when set, app loads it automatically (no app update needed).
 */
public class ServerAudioBook implements Parcelable {
    private final String id;
    private final String title;
    private final List<ServerAudioPart> parts;
    private final String thumbnailUrl;
    private final boolean isNew;
    /** સર્વર "category": "adhyatm" – 13 ફિલ્ટર માં આપમેળે આ કેટેગરીમાં દેખાશે. */
    private final String category;
    private boolean expanded = true;

    public ServerAudioBook(String id, String title, List<ServerAudioPart> parts) {
        this(id, title, parts, null, false, null);
    }

    public ServerAudioBook(String id, String title, List<ServerAudioPart> parts, String thumbnailUrl) {
        this(id, title, parts, thumbnailUrl, false, null);
    }

    public ServerAudioBook(String id, String title, List<ServerAudioPart> parts, String thumbnailUrl, boolean isNew) {
        this(id, title, parts, thumbnailUrl, isNew, null);
    }

    public ServerAudioBook(String id, String title, List<ServerAudioPart> parts, String thumbnailUrl, boolean isNew, String category) {
        this.id = id != null ? id : "";
        this.title = title != null ? title : "";
        this.parts = parts != null ? new ArrayList<>(parts) : new ArrayList<>();
        this.thumbnailUrl = (thumbnailUrl != null && !thumbnailUrl.isEmpty()) ? thumbnailUrl : null;
        this.isNew = isNew;
        this.category = (category != null && !category.isEmpty()) ? category : null;
    }

    protected ServerAudioBook(Parcel in) {
        id = in.readString();
        title = in.readString();
        parts = in.createTypedArrayList(ServerAudioPart.CREATOR);
        thumbnailUrl = in.readString();
        isNew = in.readByte() != 0;
        category = in.readString();
        expanded = in.readByte() != 0;
    }

    public static final Creator<ServerAudioBook> CREATOR = new Creator<ServerAudioBook>() {
        @Override
        public ServerAudioBook createFromParcel(Parcel in) { return new ServerAudioBook(in); }
        @Override
        public ServerAudioBook[] newArray(int size) { return new ServerAudioBook[size]; }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeTypedList(parts);
        dest.writeString(thumbnailUrl);
        dest.writeByte((byte) (isNew ? 1 : 0));
        dest.writeString(category);
        dest.writeByte((byte) (expanded ? 1 : 0));
    }

    @Override
    public int describeContents() { return 0; }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public List<ServerAudioPart> getParts() { return parts; }
    /** Optional cover image URL from server – when set, app loads it automatically. */
    public String getThumbnailUrl() { return thumbnailUrl; }
    /** Server JSON "new": true – નવા ઓડિયો ફિલ્ટર અને હોમ પર પહેલાં. */
    public boolean isNew() { return isNew; }
    /** Server JSON "category": "adhyatm" – 13 ફિલ્ટર મુજબ આપમેળે આ કેટેગરીમાં. */
    public String getCategory() { return category; }

    /** Total duration in seconds (sum of all parts). 0 if unknown. */
    public int getTotalDurationSeconds() {
        if (parts == null) return 0;
        int total = 0;
        for (ServerAudioPart p : parts) {
            if (p != null) total += p.getDurationSeconds();
        }
        return total;
    }
    public boolean isExpanded() { return expanded; }
    public void setExpanded(boolean expanded) { this.expanded = expanded; }
}
