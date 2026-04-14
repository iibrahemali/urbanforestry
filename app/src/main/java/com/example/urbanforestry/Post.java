package com.example.urbanforestry;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Post {
    public String postId;
    public String uid;
    public String username;
    public String caption;
    public int likeCount;
    public int commentCount;
    public Timestamp createdAt;

    // UI state
    public boolean isLikedByMe;
    public String userEmoji;

    // Location fields (for the logo post / library directions)
    public boolean hasLocation = false;
    public double latitude;
    public double longitude;

    // Legacy fields
    public String imagePath;
    public int resourceId = -1;
    public String text; 
    public int heartCount = 0;
    public boolean isHeartedByMe = false;
    public boolean isCommentsVisible = false;
    public List<Comment> comments = new ArrayList<>();

    public Post() {
        // Required for Firestore
    }

    public Post(String username, String imagePath, String text) {
        this.username = username;
        this.imagePath = imagePath;
        this.text = text;
        this.caption = text;
    }

    public Post(String username, int resourceId, String text) {
        this.username = username;
        this.resourceId = resourceId;
        this.text = text;
        this.caption = text;
    }
}
