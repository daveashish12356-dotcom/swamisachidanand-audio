package com.swamisachidanand;

public class Book {
    private String name;
    private String fileName;
    private long size;
    private String publishYear;
    private String category;
    private String searchableText;
    /** When set, PDF loads from this URL (server). */
    private String pdfUrl;
    /** When set, thumbnail loads from this URL (server). */
    private String thumbnailUrl;

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

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
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

