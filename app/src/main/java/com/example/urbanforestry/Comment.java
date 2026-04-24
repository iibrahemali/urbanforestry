// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports Timestamp from Firebase so each comment can store the exact time it was submitted
import com.google.firebase.Timestamp;

// Declares Comment as a public class — it's a plain data model representing a single comment on a post
public class Comment {
    // Stores the unique Firestore document ID for this comment — used to reference it for deletes or updates
    public String commentId;
    // Stores the Firebase user ID of the person who wrote this comment — used to verify ownership
    public String uid;
    // Stores the display name of the user who wrote the comment — shown inline next to the comment text
    public String username;
    // Stores the actual text content of the comment
    public String text;
    // Stores the server-side timestamp of when the comment was created — used to sort comments oldest-first
    public Timestamp createdAt;

    // No-argument constructor required by Firestore — without it, Firestore cannot automatically deserialize comment subcollection documents into Comment objects
    public Comment() {
    }

    // Constructor used when creating a comment for a static/demo post that doesn't go to Firestore
    public Comment(String username, String text) {
        // Sets the author's name to display alongside the comment
        this.username = username;
        // Sets the comment body text
        this.text = text;
    }

    // Constructor used when creating a real comment submitted by a logged-in user — includes the UID for ownership tracking
    public Comment(String uid, String username, String text) {
        // Sets the user's Firebase UID so we can verify who wrote this comment
        this.uid = uid;
        // Sets the display name to show in the comment list
        this.username = username;
        // Sets the comment content
        this.text = text;
    }
}