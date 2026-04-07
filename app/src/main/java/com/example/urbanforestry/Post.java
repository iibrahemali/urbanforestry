package com.example.urbanforestry;

public class Post {
    public String username;
    public String imagePath; // null if text post (for disk files)
    public int resourceId = -1; // -1 if not a drawable resource
    public String text;      // null if image post

    public Post(String username, String imagePath, String text) {
        this.username = username;
        this.imagePath = imagePath;
        this.text = text;
    }

    // Constructor for drawable resources (dummy data)
    public Post(String username, int resourceId, String text) {
        this.username = username;
        this.resourceId = resourceId;
        this.text = text;
    }
}
