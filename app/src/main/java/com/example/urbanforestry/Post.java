package com.example.urbanforestry;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Post {
    public String postId;
    public String uid;
    public String username;
    public String caption;
    public String imageUrl; 
    public int likeCount;
    public int commentCount;
    public int reportCount; 
    public Timestamp createdAt;
    
    // UI state
    public boolean isLikedByMe;
    public boolean isReportedByMe; // Added this field
    public String userEmoji; // The emoji the user reacted with if any
    
    // Location fields
    public boolean hasLocation;
    public double latitude;
    public double longitude;

    // Legacy fields (keeping for compatibility if needed, but Firestore uses the above)
    public String imagePath; 
    public int resourceId = -1;
    public String text; // We'll map caption to this
    public int heartCount = 0; // We'll map likeCount to this
    public boolean isHeartedByMe = false; // We'll map isLikedByMe to this
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

    // Constructor for drawable resources (dummy data)
    public Post(String username, int resourceId, String text) {
        this.username = username;
        this.resourceId = resourceId;
        this.text = text;
        this.caption = text;
    }
}
