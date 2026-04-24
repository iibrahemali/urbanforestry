// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports Timestamp from Firebase, used to store and retrieve the exact date/time a post was created
import com.google.firebase.Timestamp;
// Imports ArrayList, a resizable list used to hold the post's loaded comments in memory
import java.util.ArrayList;
// Imports the List interface, which ArrayList implements — used so the field type stays flexible
import java.util.List;

// Declares Post as a public class — it's a plain data model that represents a single post in the app
public class Post {
    // Stores the unique Firestore document ID for this post — used to reference it in all database operations
    public String postId;
    // Stores the Firebase user ID of the person who created this post — used to verify ownership
    public String uid;
    // Stores the display name of the user who created the post — shown directly in the feed
    public String username;
    // Stores the text caption the user wrote when submitting the post
    public String caption;
    // Stores the Firebase Storage download URL for the post's image, if one was uploaded
    public String imageUrl;
    // Stores the total number of likes this post has received — displayed on the like button
    public int likeCount;
    // Stores the total number of comments — displayed on the comment button
    public int commentCount;
    // Stores the number of times this post has been reported — used to auto-delete posts above a threshold
    public int reportCount;
    // Stores the server-side timestamp of when the post was created — used to sort the feed newest-first
    public Timestamp createdAt;

    // UI state — these fields are not stored in Firestore; they track local state per session
    // Tracks whether the currently logged-in user has liked this post — prevents double-liking
    public boolean isLikedByMe;
    // Tracks whether the currently logged-in user has reported this post — prevents double-reporting
    public boolean isReportedByMe;
    // Stores the emoji the current user reacted with, if any — used to display a personalised reaction icon
    public String userEmoji;

    // Location fields — only populated if the user chose to share their location when posting
    // Flag indicating whether this post has an attached location — controls whether the "Get Directions" button is shown
    public boolean hasLocation;
    // Stores the latitude coordinate of the post's location
    public double latitude;
    // Stores the longitude coordinate of the post's location
    public double longitude;

    // Legacy fields kept for backwards compatibility with locally-generated dummy/test posts
    // Stores a local file path to an image — used for posts created from the camera before uploading
    public String imagePath;
    // Stores an Android drawable resource ID for static demo posts — -1 means no drawable is set
    public int resourceId = -1;
    // Maps to the caption field for legacy compatibility — some code paths use text instead of caption
    public String text;
    // Maps to likeCount for legacy compatibility — used by static demo posts that don't use Firestore
    public int heartCount = 0;
    // Maps to isLikedByMe for legacy compatibility — used by static demo posts
    public boolean isHeartedByMe = false;
    // Tracks whether the comment section for this post is currently expanded in the UI
    public boolean isCommentsVisible = false;
    // Holds comments loaded into memory for this post — populated when the user taps the comment button
    public List<Comment> comments = new ArrayList<>();

    // No-argument constructor required by Firestore — without it, Firestore cannot automatically deserialize documents into Post objects
    public Post() {
    }

    // Constructor for creating a post from a local file path — used when posting a camera image before upload
    public Post(String username, String imagePath, String text) {
        // Sets the username field so the post can be displayed with an author name immediately
        this.username = username;
        // Sets the local image path used to preview the image before it is uploaded to Firebase Storage
        this.imagePath = imagePath;
        // Sets both text and caption so that both legacy and current code paths can read the post content
        this.text = text;
        this.caption = text;
    }

    // Constructor for creating a static demo post using an Android drawable resource ID instead of a real image URL
    public Post(String username, int resourceId, String text) {
        // Sets the username to display on the demo post card
        this.username = username;
        // Sets the drawable resource ID so the adapter can load the image without a network call
        this.resourceId = resourceId;
        // Sets both text and caption for the same compatibility reason as the other constructor
        this.text = text;
        this.caption = text;
    }
}