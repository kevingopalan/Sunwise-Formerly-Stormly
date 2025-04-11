package com.venomdevelopment.sunwise;

public class NewsItem {

    private String title;
    private String content;
    private String imageUrl;

    // Constructor
    public NewsItem(String title, String content, String imageUrl) {
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
