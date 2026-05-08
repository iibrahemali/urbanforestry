// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports Uri to represent the image file to be uploaded to Firebase Storage
import android.net.Uri;
// Imports Log for recording cleanup errors
import android.util.Log;
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
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
// Imports Firebase Storage types for uploading and deleting images
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

// Imports Collections for cleanup logic and UID generation
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Declares PostRepository as a data layer class that encapsulates all Firebase operations related to posts
public class PostRepository {
    private final FirebaseFirestore mFirestore;
    private final DatabaseReference mDatabase;
    private final FirebaseAuth mAuth;
    private final FirebaseStorage mStorage;

    public PostRepository() {
        mFirestore = FirebaseFirestore.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        mStorage = FirebaseStorage.getInstance();
    }

    // Fetches the username for a given UID from the Realtime Database
    public Task<String> getUsername(String uid) {
        return mDatabase.child("users").child(uid).child("username").get().continueWith(task -> {
            DataSnapshot snapshot = task.getResult();
            return snapshot.getValue(String.class);
        });
    }

    // Creates a text-only post in Firestore, optionally with GPS coordinates
    public Task<Void> createPost(String caption, Double latitude, Double longitude) {
        String uid = mAuth.getCurrentUser().getUid();
        return getUsername(uid).continueWithTask(task -> {
            String username = task.getResult();
            DocumentReference postRef = mFirestore.collection("posts").document();
            String postId = postRef.getId();

            Map<String, Object> postMap = new HashMap<>();
            postMap.put("postId", postId);
            postMap.put("uid", uid);
            postMap.put("username", username);
            postMap.put("caption", caption);
            postMap.put("likeCount", 0);
            postMap.put("commentCount", 0);
            postMap.put("reportCount", 0);
            postMap.put("createdAt", FieldValue.serverTimestamp());

            if (latitude != null && longitude != null) {
                postMap.put("hasLocation", true);
                postMap.put("latitude", latitude);
                postMap.put("longitude", longitude);
            } else {
                postMap.put("hasLocation", false);
            }

            return mFirestore.runTransaction(transaction -> {
                DocumentReference userRef = mFirestore.collection("users").document(uid);
                transaction.update(userRef, "postCount", FieldValue.increment(1));
                transaction.set(postRef, postMap);
                return null;
            });
        });
    }

    // Creates an image post by uploading the photo to Firebase Storage first
    public Task<Void> createImagePost(String caption, Uri imageUri, Double latitude, Double longitude) {
        String uid = mAuth.getCurrentUser().getUid();
        String storagePath = "posts/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference storageRef = mStorage.getReference().child(storagePath);

        return storageRef.putFile(imageUri).continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return storageRef.getDownloadUrl();
        }).continueWithTask(task -> {
            String imageUrl = task.getResult().toString();
            return getUsername(uid).continueWithTask(nameTask -> {
                String username = nameTask.getResult();
                DocumentReference postRef = mFirestore.collection("posts").document();
                String postId = postRef.getId();

                Map<String, Object> postMap = new HashMap<>();
                postMap.put("postId", postId);
                postMap.put("uid", uid);
                postMap.put("username", username);
                postMap.put("caption", caption);
                postMap.put("imageUrl", imageUrl);
                postMap.put("likeCount", 0);
                postMap.put("commentCount", 0);
                postMap.put("reportCount", 0);
                postMap.put("createdAt", FieldValue.serverTimestamp());

                if (latitude != null && longitude != null) {
                    postMap.put("hasLocation", true);
                    postMap.put("latitude", latitude);
                    postMap.put("longitude", longitude);
                } else {
                    postMap.put("hasLocation", false);
                }

                return mFirestore.runTransaction(transaction -> {
                    DocumentReference userRef = mFirestore.collection("users").document(uid);
                    transaction.update(userRef, "postCount", FieldValue.increment(1));
                    transaction.set(postRef, postMap);
                    return null;
                });
            });
        });
    }

    // Updates a post's caption in Firestore
    public Task<Void> updatePost(String postId, String newCaption) {
        DocumentReference postRef = mFirestore.collection("posts").document(postId);
        return postRef.update("caption", newCaption);
    }

    // Deletes a post AND triggers a deep cleanup of orphaned sub-data
    public Task<Void> deletePost(String postId) {
        DocumentReference postRef = mFirestore.collection("posts").document(postId);
        return postRef.get().continueWithTask(task -> {
            DocumentSnapshot snapshot = task.getResult();
            if (!snapshot.exists()) return Tasks.forResult(null);

            String uid = snapshot.getString("uid");
            String imageUrl = snapshot.getString("imageUrl");

            // Transactional delete + user post count update
            return mFirestore.runTransaction(transaction -> {
                transaction.delete(postRef);
                if (uid != null) {
                    DocumentReference userRef = mFirestore.collection("users").document(uid);
                    transaction.update(userRef, "postCount", FieldValue.increment(-1));
                }
                return null;
            }).continueWithTask(t -> performDeepCleanup(postId, imageUrl));
        });
    }

    // Helper method to scrub image from Storage and scrub orphaned subcollections
    private Task<Void> performDeepCleanup(String postId, String imageUrl) {
        List<Task<?>> tasks = new ArrayList<>();

        // 1. Delete the image file from Storage
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                tasks.add(mStorage.getReferenceFromUrl(imageUrl).delete());
            } catch (Exception e) {
                Log.e("PostRepository", "Cleanup error: image already gone or invalid.");
            }
        }

        // 2. Delete all documents in subcollections (comments, likes, reports)
        String[] subs = {"comments", "likes", "reports"};
        for (String sub : subs) {
            tasks.add(mFirestore.collection("posts").document(postId).collection(sub).get()
                    .continueWithTask(task -> {
                        List<Task<Void>> delTasks = new ArrayList<>();
                        if (task.isSuccessful() && task.getResult() != null) {
                            for (DocumentSnapshot doc : task.getResult()) {
                                delTasks.add(doc.getReference().delete());
                            }
                        }
                        return Tasks.whenAll(delTasks);
                    }));
        }

        return Tasks.whenAll(tasks);
    }

    // Utility: Cleans up ANY existing orphaned data currently in your Firebase database
    public void flushOrphanedData() {
        String[] subs = {"comments", "likes", "reports"};
        for (String sub : subs) {
            mFirestore.collectionGroup(sub).get().addOnSuccessListener(snapshot -> {
                for (DocumentSnapshot doc : snapshot) {
                    DocumentReference postRef = doc.getReference().getParent().getParent();
                    if (postRef != null) {
                        postRef.get().addOnSuccessListener(pDoc -> {
                            if (!pDoc.exists()) doc.getReference().delete();
                        });
                    }
                }
            });
        }
    }

    // Reports a post — auto-deletes it with deep cleanup if threshold (5) is reached
    public Task<Boolean> reportPost(String postId) {
        String uid = mAuth.getCurrentUser().getUid();
        DocumentReference postRef = mFirestore.collection("posts").document(postId);
        DocumentReference reportRef = postRef.collection("reports").document(uid);
        final String[] imageUrlHolder = new String[1];

        return mFirestore.runTransaction(transaction -> {
            DocumentSnapshot reportSnapshot = transaction.get(reportRef);
            if (reportSnapshot.exists()) throw new RuntimeException("ALREADY_REPORTED");

            DocumentSnapshot postSnapshot = transaction.get(postRef);
            imageUrlHolder[0] = postSnapshot.getString("imageUrl");
            long reports = (postSnapshot.getLong("reportCount") != null ? postSnapshot.getLong("reportCount") : 0) + 1;

            transaction.set(reportRef, new HashMap<String, Object>() {{ put("timestamp", FieldValue.serverTimestamp()); }});

            if (reports > 5) {
                String ownerUid = postSnapshot.getString("uid");
                transaction.delete(postRef);
                if (ownerUid != null) {
                    transaction.update(mFirestore.collection("users").document(ownerUid), "postCount", FieldValue.increment(-1));
                }
                return true;
            } else {
                transaction.update(postRef, "reportCount", reports);
                return false;
            }
        }).continueWithTask(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult()) {
                return performDeepCleanup(postId, imageUrlHolder[0]).continueWith(t -> true);
            }
            return task;
        });
    }

    // Toggles a like: creates a like document and increments, or deletes and decrements
    public Task<Void> toggleLike(String postId, String emoji) {
        String uid = mAuth.getCurrentUser().getUid();
        DocumentReference postRef = mFirestore.collection("posts").document(postId);
        DocumentReference likeRef = postRef.collection("likes").document(uid);
        return mFirestore.runTransaction(transaction -> {
            if (transaction.get(likeRef).exists()) {
                transaction.delete(likeRef);
                transaction.update(postRef, "likeCount", FieldValue.increment(-1));
            } else {
                Map<String, Object> map = new HashMap<>();
                map.put("uid", uid);
                map.put("emoji", emoji);
                map.put("createdAt", FieldValue.serverTimestamp());
                transaction.set(likeRef, map);
                transaction.update(postRef, "likeCount", FieldValue.increment(1));
            }
            return null;
        });
    }

    // Adds a comment to a post and increments commentCount
    public Task<Void> addComment(String postId, String text) {
        String uid = mAuth.getCurrentUser().getUid();
        DocumentReference postRef = mFirestore.collection("posts").document(postId);
        return getUsername(uid).continueWithTask(task -> {
            DocumentReference cRef = postRef.collection("comments").document();
            Map<String, Object> map = new HashMap<>();
            map.put("commentId", cRef.getId());
            map.put("uid", uid);
            map.put("username", task.getResult());
            map.put("text", text);
            map.put("createdAt", FieldValue.serverTimestamp());
            return mFirestore.runTransaction(transaction -> {
                transaction.set(cRef, map);
                transaction.update(postRef, "commentCount", FieldValue.increment(1));
                return null;
            });
        });
    }

    // Fetches all comments for a post
    public Task<QuerySnapshot> getComments(String postId) {
        return mFirestore.collection("posts").document(postId).collection("comments").orderBy("createdAt", Query.Direction.ASCENDING).get();
    }

    // Removes a user's report and decrements reportCount
    public Task<Void> unreportPost(String postId) {
        String uid = mAuth.getCurrentUser().getUid();
        DocumentReference postRef = mFirestore.collection("posts").document(postId);
        DocumentReference reportRef = postRef.collection("reports").document(uid);
        return mFirestore.runTransaction(transaction -> {
            if (transaction.get(reportRef).exists()) {
                transaction.delete(reportRef);
                transaction.update(postRef, "reportCount", FieldValue.increment(-1));
            }
            return null;
        });
    }

    // Fetches the emoji the current user used to like a specific post
    public Task<String> getUserLikeEmoji(String postId) {
        String uid = mAuth.getCurrentUser().getUid();
        if (uid == null) return Tasks.forResult(null);
        return mFirestore.collection("posts").document(postId).collection("likes").document(uid).get().continueWith(task -> task.getResult().exists() ? task.getResult().getString("emoji") : null);
    }

    // Fetches a paginated list of posts for the feed
    public Task<QuerySnapshot> getPosts(DocumentSnapshot last, int limit) {
        Query q = mFirestore.collection("posts").orderBy("createdAt", Query.Direction.DESCENDING).limit(limit);
        if (last != null) q = q.startAfter(last);
        return q.get();
    }
}
