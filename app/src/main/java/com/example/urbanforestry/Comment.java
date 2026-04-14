package com.example.urbanforestry;

import com.google.firebase.Timestamp;

public class Comment {
    public String commentId;
    public String uid;
    public String username;
    public String text;
    public Timestamp createdAt;

    public Comment() {
        // Required for Firestore
    }

    public Comment(String username, String text) {
        this.username = username;
        this.text = text;
    }
    
    public Comment(String uid, String username, String text) {
        this.uid = uid;
        this.username = username;
        this.text = text;
    }
}
