// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports Uri to represent the image file to be uploaded to Firebase Storage
import android.net.Uri;
// Imports Task and Tasks from Google's GMS library for chaining and combining asynchronous Firebase operations
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
// Imports FirebaseAuth to identify the currently logged-in user for ownership and attribution
import com.google.firebase.auth.FirebaseAuth;
// Imports Realtime Database types — used only to fetch the username, which is stored there
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
// Imports Firestore types for all post, comment, like, and report operations
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
// Imports Firebase Storage types for uploading images to Cloud Storage
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

// Imports HashMap and Map for building the key-value data structures written to Firestore
import java.util.HashMap;
import java.util.Map;
// Imports UUID to generate a unique storage path for each uploaded image, preventing filename collisions
import java.util.UUID;

// Declares PostRepository as a data layer class that encapsulates all Firebase operations related to posts
public class PostRepository {
    // Declares Firestore for storing and querying post documents
    private final FirebaseFirestore mFirestore;
    // Declares the Realtime Database reference used only to look up the current user's username
    private final DatabaseReference mDatabase;
    // Declares FirebaseAuth to get the UID of the currently logged-in user
    private final FirebaseAuth mAuth;
    // Declares Firebase Storage for uploading post images
    private final FirebaseStorage mStorage;

    // Constructor that initialises all four Firebase service instances — called once per screen that interacts with posts
    public PostRepository() {
        mFirestore = FirebaseFirestore.getInstance();
        // Gets the root reference of the Realtime Database to navigate to any child node
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        mStorage = FirebaseStorage.getInstance();
    }

    // Fetches the username for a given UID from the Realtime Database — used before creating a post so the author's name is saved with it
    public Task<String> getUsername(String uid) {
        return mDatabase.child("users").child(uid).child("username").get().continueWith(task -> {
            DataSnapshot snapshot = task.getResult();
            // Returns the username string, or null if the field doesn't exist
            return snapshot.getValue(String.class);
        });
    }

    // Creates a text-only post in Firestore, optionally with GPS coordinates
    public Task<Void> createPost(String caption, Double latitude, Double longitude) {
        // Gets the current user's UID to set as the post owner
        String uid = mAuth.getCurrentUser().getUid();

        // Fetches the username first because Firestore documents don't join across collections — we embed the name directly
        return getUsername(uid).continueWithTask(task -> {
            String username = task.getResult();

            // Creates a new auto-generated Firestore document reference — this gives us the ID before writing
            DocumentReference postRef = mFirestore.collection("posts").document();
            // Reads the generated ID so we can store it as a field inside the document itself
            String postId = postRef.getId();

            // Builds the post data map with all required fields
            Map<String, Object> postMap = new HashMap<>();
            postMap.put("postId", postId);
            postMap.put("uid", uid);
            postMap.put("username", username);
            postMap.put("caption", caption);
            // Initialises counters to 0 — incremented by transactions later to avoid race conditions
            postMap.put("likeCount", 0);
            postMap.put("commentCount", 0);
            postMap.put("reportCount", 0);
            // Uses a server-side timestamp so the feed ordering is consistent regardless of the device's clock
            postMap.put("createdAt", FieldValue.serverTimestamp());

            // Includes location data only if coordinates were provided — avoids storing null values in Firestore
            if (latitude != null && longitude != null) {
                postMap.put("hasLocation", true);
                postMap.put("latitude", latitude);
                postMap.put("longitude", longitude);
            } else {
                postMap.put("hasLocation", false);
            }

            // Uses a transaction to atomically create the post AND increment the user's post count — prevents the count from drifting out of sync
            return mFirestore.runTransaction(transaction -> {
                DocumentReference userRef = mFirestore.collection("users").document(uid);
                // Increments the post count by 1 using FieldValue.increment to avoid read-modify-write race conditions
                transaction.update(userRef, "postCount", FieldValue.increment(1));
                // Writes the post document in the same atomic transaction
                transaction.set(postRef, postMap);
                return null;
            });
        });
    }

    // Creates an image post by uploading the photo to Firebase Storage first, then creating the Firestore document
    public Task<Void> createImagePost(String caption, Uri imageUri, Double latitude, Double longitude) {
        String uid = mAuth.getCurrentUser().getUid();
        // Generates a unique storage path using a UUID so multiple uploads never overwrite each other
        String storagePath = "posts/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference storageRef = mStorage.getReference().child(storagePath);

        // Uploads the image to Cloud Storage and chains a download URL fetch after it completes
        return storageRef.putFile(imageUri).continueWithTask(task -> {
            // Re-throws the upload error so the caller's failure listener receives it
            if (!task.isSuccessful()) throw task.getException();
            // Requests the publicly accessible download URL for the uploaded image
            return storageRef.getDownloadUrl();
        }).continueWithTask(task -> {
            // Converts the download URI to a String to store as a field in Firestore
            String imageUrl = task.getResult().toString();
            // Fetches the username before creating the post, for the same reason as in createPost()
            return getUsername(uid).continueWithTask(nameTask -> {
                String username = nameTask.getResult();

                // Creates an auto-generated Firestore document reference for the new post
                DocumentReference postRef = mFirestore.collection("posts").document();
                String postId = postRef.getId();

                // Builds the post data map, including the Firebase Storage download URL
                Map<String, Object> postMap = new HashMap<>();
                postMap.put("postId", postId);
                postMap.put("uid", uid);
                postMap.put("username", username);
                postMap.put("caption", caption);
                // Stores the Firebase Storage URL so the feed can load the image without re-uploading
                postMap.put("imageUrl", imageUrl);
                postMap.put("likeCount", 0);
                postMap.put("commentCount", 0);
                postMap.put("reportCount", 0);
                postMap.put("createdAt", FieldValue.serverTimestamp());

                // Conditionally includes location coordinates, same as in createPost()
                if (latitude != null && longitude != null) {
                    postMap.put("hasLocation", true);
                    postMap.put("latitude", latitude);
                    postMap.put("longitude", longitude);
                } else {
                    postMap.put("hasLocation", false);
                }

                // Atomically creates the post and increments the user's post count in one transaction
                return mFirestore.runTransaction(transaction -> {
                    DocumentReference userRef = mFirestore.collection("users").document(uid);
                    transaction.update(userRef, "postCount", FieldValue.increment(1));
                    transaction.set(postRef, postMap);
                    return null;
                });
            });
        });
    }

    // Updates a post's caption in Firestore — used by the edit button in PostAdapter
    public Task<Void> updatePost(String postId, String newCaption) {
        // Gets a reference to the specific post document and updates only the caption field
        DocumentReference postRef = mFirestore.collection("posts").document(postId);
        return postRef.update("caption", newCaption);
    }

    // Deletes a post from Firestore and atomically decrements the post owner's post count
    public Task<Void> deletePost(String postId) {
        DocumentReference postRef = mFirestore.collection("posts").document(postId);
        // Reads the post first to get the owner's UID — needed to decrement their post count
        return postRef.get().continueWithTask(task -> {
            DocumentSnapshot snapshot = task.getResult();
            // Gets the owner's UID stored inside the document — more reliable than using the current auth UID since admins could delete others' posts
            String uid = snapshot.getString("uid");

            // Uses a transaction to atomically delete the post and decrement the post count
            return mFirestore.runTransaction(transaction -> {
                transaction.delete(postRef);
                if (uid != null) {
                    DocumentReference userRef = mFirestore.collection("users").document(uid);
                    // Decrements by 1 — using FieldValue.increment(-1) is race-safe
                    transaction.update(userRef, "postCount", FieldValue.increment(-1));
                }
                return null;
            });
        });
    }

    // Removes the current user's report from a post — called when the user taps "Withdraw" in the already-reported dialog
    public Task<Void> unreportPost(String postId) {
        String uid = mAuth.getCurrentUser().getUid();
        DocumentReference postRef = mFirestore.collection("posts").document(postId);
        // The report is stored as a subcollection document keyed by the user's UID
        DocumentReference reportRef = postRef.collection("reports").document(uid);

        // Uses a transaction to atomically delete the report document and decrement the report count
        return mFirestore.runTransaction(transaction -> {
            DocumentSnapshot reportSnapshot = transaction.get(reportRef);
            // If no report exists, does nothing — prevents double-decrementing if the state is out of sync
            if (!reportSnapshot.exists()) return null;

            // Deletes the report document and decrements the counter in one atomic operation
            transaction.delete(reportRef);
            transaction.update(postRef, "reportCount", FieldValue.increment(-1));
            return null;
        });
    }

    // Reports a post — auto-deletes it if it accumulates more than 5 reports to prevent abuse
    public Task<Boolean> reportPost(String postId) {
        String uid = mAuth.getCurrentUser().getUid();
        DocumentReference postRef = mFirestore.collection("posts").document(postId);
        // The report is stored as a subcollection document keyed by the user's UID to prevent duplicate reports
        DocumentReference reportRef = postRef.collection("reports").document(uid);

        return mFirestore.runTransaction(transaction -> {
            DocumentSnapshot reportSnapshot = transaction.get(reportRef);

            // If the user has already reported this post, throws an exception so the adapter can show the already-reported dialog
            if (reportSnapshot.exists()) {
                throw new RuntimeException("ALREADY_REPORTED");
            }

            // Reads the current report count to decide whether to delete the post
            DocumentSnapshot postSnapshot = transaction.get(postRef);
            long currentReports = postSnapshot.getLong("reportCount") != null ? postSnapshot.getLong("reportCount") : 0;
            long newReportCount = currentReports + 1;

            // Records this user's report as a subcollection document with a timestamp for audit purposes
            Map<String, Object> reportData = new HashMap<>();
            reportData.put("timestamp", FieldValue.serverTimestamp());
            transaction.set(reportRef, reportData);

            // Auto-deletes the post if it has been reported more than 5 times — acts as a community moderation mechanism
            if (newReportCount > 5) {
                String postOwnerUid = postSnapshot.getString("uid");
                transaction.delete(postRef);
                if (postOwnerUid != null) {
                    // Decrements the owner's post count since their post was removed
                    DocumentReference userRef = mFirestore.collection("users").document(postOwnerUid);
                    transaction.update(userRef, "postCount", FieldValue.increment(-1));
                }
                // Returns true to signal that the post was deleted — the adapter removes it from the list
                return true;
            } else {
                // Updates the report count without deleting the post
                transaction.update(postRef, "reportCount", newReportCount);
                // Returns false to signal that the post was only flagged, not deleted
                return false;
            }
        });
    }

    // Toggles a like on a post — adds a like if not yet liked, removes it if already liked (atomic toggle)
    public Task<Void> toggleLike(String postId, String emoji) {
        String uid = mAuth.getCurrentUser().getUid();
        DocumentReference postRef = mFirestore.collection("posts").document(postId);
        // The like is stored as a subcollection document keyed by the user's UID so each user can only like once
        DocumentReference likeRef = postRef.collection("likes").document(uid);

        // Uses a transaction to atomically check the like state and update both the like document and the count
        return mFirestore.runTransaction(transaction -> {
            DocumentSnapshot likeSnapshot = transaction.get(likeRef);

            if (likeSnapshot.exists()) {
                // If a like already exists, remove it (unlike) and decrement the count
                transaction.delete(likeRef);
                transaction.update(postRef, "likeCount", FieldValue.increment(-1));
            } else {
                // If no like exists, create it and increment the count
                Map<String, Object> likeMap = new HashMap<>();
                likeMap.put("uid", uid);
                // Stores the emoji reaction type for potential future "reactions" feature expansion
                likeMap.put("emoji", emoji);
                likeMap.put("createdAt", FieldValue.serverTimestamp());

                transaction.set(likeRef, likeMap);
                transaction.update(postRef, "likeCount", FieldValue.increment(1));
            }
            return null;
        });
    }

    // Adds a comment to a post in Firestore and atomically increments the post's comment count
    public Task<Void> addComment(String postId, String text) {
        String uid = mAuth.getCurrentUser().getUid();
        DocumentReference postRef = mFirestore.collection("posts").document(postId);
        // Creates a new auto-generated document in the comments subcollection
        DocumentReference commentRef = postRef.collection("comments").document();

        // Fetches the username before creating the comment so it's embedded in the document
        return getUsername(uid).continueWithTask(task -> {
            String username = task.getResult();

            // Builds the comment data map
            Map<String, Object> commentMap = new HashMap<>();
            // Stores the generated ID as a field so the comment can be referenced for future deletion
            commentMap.put("commentId", commentRef.getId());
            commentMap.put("uid", uid);
            commentMap.put("username", username);
            commentMap.put("text", text);
            commentMap.put("createdAt", FieldValue.serverTimestamp());

            // Atomically creates the comment and increments the post's comment count
            return mFirestore.runTransaction(transaction -> {
                transaction.set(commentRef, commentMap);
                transaction.update(postRef, "commentCount", FieldValue.increment(1));
                return null;
            });
        });
    }

    // Fetches all comments for a post ordered by creation time — used by PostAdapter when the comment section is expanded
    public Task<QuerySnapshot> getComments(String postId) {
        return mFirestore.collection("posts").document(postId)
                .collection("comments")
                // Orders oldest-first so comments read chronologically, like a conversation
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get();
    }

    // Fetches the emoji the current user used to like a specific post — used to restore the reaction icon state
    public Task<String> getUserLikeEmoji(String postId) {
        String uid = mAuth.getCurrentUser().getUid();
        // Returns null immediately if no user is logged in to avoid a null pointer on getCurrentUser()
        if (uid == null) return Tasks.forResult(null);
        return mFirestore.collection("posts").document(postId)
                .collection("likes").document(uid).get()
                .continueWith(task -> {
                    DocumentSnapshot doc = task.getResult();
                    if (doc.exists()) {
                        // Returns the stored emoji string (e.g., "❤️")
                        return doc.getString("emoji");
                    }
                    return null;
                });
    }

    // Fetches a paginated page of posts — supports pagination for large feeds using Firestore cursor-based queries
    public Task<QuerySnapshot> getPosts(DocumentSnapshot lastVisible, int limit) {
        Query query = mFirestore.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                // Limits results per page to avoid loading the entire collection at once
                .limit(limit);

        // If a cursor was provided, starts after the last document from the previous page
        if (lastVisible != null) {
            query = query.startAfter(lastVisible);
        }

        return query.get();
    }
}
