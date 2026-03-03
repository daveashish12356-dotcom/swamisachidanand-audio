package com.swamisachidanand;

/** Ashram contact data for About page. */
public class AshramItem {
    public final String title;
    public final String desc;
    public final String address;
    public final String phone;
    public final String website;
    public final String mapUrl;
    public final int thumbnailResId;

    public AshramItem(String title, String desc, String address, String phone,
                      String website, String mapUrl, int thumbnailResId) {
        this.title = title;
        this.desc = desc;
        this.address = address;
        this.phone = phone;
        this.website = website;
        this.mapUrl = mapUrl;
        this.thumbnailResId = thumbnailResId;
    }
}
