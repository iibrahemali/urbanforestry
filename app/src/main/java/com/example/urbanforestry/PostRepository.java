package com.example.urbanforestry;

import android.net.Uri;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    public Task<String> getUsername(String uid) {
        return mDatabase.child("users").child(uid).child("username").get().continueWith(task -> {
            DataSnapshot snapshot = task.getResult();
            return snapshot.getValue(String.class);
        });
    }

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

    public Task<Void> deletePost(String postId) {
        String uid = mAuth.getCurrentUser().getUid();
        DocumentReference postRef = mFirestore.collection("posts").document(postId);
        DocumentReference userRef = mFirestore.collection("users").document(uid);

        return mFirestore.runTransaction(transaction -> {
            transaction.delete(postRef);
            transaction.update(userRef, "postCount", FieldValue.increment(-1));
            return null;
        });
    }

    public Task<Void> toggleLike(String postId, String emoji) {
        String uid = mAuth.getCurrentUser().getUid();
        DocumentReference postRef = mFirestore.collection("posts").document(postId);
        DocumentReference likeRef = postRef.collection("likes").document(uid);

        return mFirestore.runTransaction(transaction -> {
            DocumentSnapshot likeSnapshot = transaction.get(likeRef);
            
            if (likeSnapshot.exists()) {
                transaction.delete(likeRef);
                transaction.update(postRef, "likeCount", FieldValue.increment(-1));
            } else {
                Map<String, Object> likeMap = new HashMap<>();
                likeMap.put("uid", uid);
                likeMap.put("emoji", emoji);
                likeMap.put("createdAt", FieldValue.serverTimestamp());
                
                transaction.set(likeRef, likeMap);
                transaction.update(postRef, "likeCount", FieldValue.increment(1));
            }
            return null;
        });
    }

    public Task<Void> addComment(String postId, String text) {
        String uid = mAuth.getCurrentUser().getUid();
        DocumentReference postRef = mFirestore.collection("posts").document(postId);
        DocumentReference commentRef = postRef.collection("comments").document();
        
        return getUsername(uid).continueWithTask(task -> {
            String username = task.getResult();
            
            Map<String, Object> commentMap = new HashMap<>();
            commentMap.put("commentId", commentRef.getId());
            commentMap.put("uid", uid);
            commentMap.put("username", username);
            commentMap.put("text", text);
            commentMap.put("createdAt", FieldValue.serverTimestamp());

            return mFirestore.runTransaction(transaction -> {
                transaction.set(commentRef, commentMap);
                transaction.update(postRef, "commentCount", FieldValue.increment(1));
                return null;
            });
        });
    }

    public Task<QuerySnapshot> getComments(String postId) {
        return mFirestore.collection("posts").document(postId)
                .collection("comments")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get();
    }

    public Task<String> getUserLikeEmoji(String postId) {
        String uid = mAuth.getCurrentUser().getUid();
        if (uid == null) return Tasks.forResult(null);
        return mFirestore.collection("posts").document(postId)
                .collection("likes").document(uid).get()
                .continueWith(task -> {
                    DocumentSnapshot doc = task.getResult();
                    if (doc.exists()) {
                        return doc.getString("emoji");
                    }
                    return null;
                });
    }

    public Task<QuerySnapshot> getPosts(DocumentSnapshot lastVisible, int limit) {
        Query query = mFirestore.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit);

        if (lastVisible != null) {
            query = query.startAfter(lastVisible);
        }

        return query.get();
    }
}