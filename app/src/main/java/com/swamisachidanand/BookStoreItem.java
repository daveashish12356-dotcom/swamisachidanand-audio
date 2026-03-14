package com.swamisachidanand;

/** Item for Surat Book Center – cover, name, price. */
public class BookStoreItem {
    public String id;
    public String name;
    public int price;
    public String img;      // filename e.g. 1.jpg
    public String imageUrl; // full URL when from server, null = load from assets

    public BookStoreItem() {}

    public BookStoreItem(String id, String name, int price, String img) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.img = img;
    }
}
