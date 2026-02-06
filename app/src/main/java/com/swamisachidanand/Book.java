package com.swamisachidanand;

public class Book {
    private String name;
    private String fileName;
    private long size;
    private String publishYear;
    private String category; // Bhakti, Yatra, Updesh, Jeevan
    private String searchableText; // Original + English transliteration for search

    public Book(String name, String fileName, long size) {
        this.name = name != null ? name : "";
        this.fileName = fileName != null ? fileName : "";
        this.size = size;
        this.publishYear = null;
        this.category = null;
        try {
            this.searchableText = BookTransliterator.getSearchableText(this.name) + " " +
                    BookTransliterator.getSearchableText(this.fileName);
        } catch (Throwable t) {
            this.searchableText = (this.name + " " + this.fileName).toLowerCase();
        }
    }

    public String getName() {
        return name;
    }

    public String getFileName() {
        return fileName;
    }

    public long getSize() {
        return size;
    }

    public String getPublishYear() {
        return publishYear;
    }

    public void setPublishYear(String year) {
        this.publishYear = year;
    }
    
    public String getSearchableText() {
        return searchableText != null ? searchableText : name.toLowerCase();
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getFormattedSize() {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        }
    }
}

