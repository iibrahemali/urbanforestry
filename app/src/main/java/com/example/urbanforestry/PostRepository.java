package com.example.urbanforestry;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class PostRepository {
    private final FirebaseFirestore mFirestore;
    private final DatabaseReference mDatabase;
    private final FirebaseAuth mAuth;

    public PostRepository() {
        mFirestore = FirebaseFirestore.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
    }

    public Task<String> getUsername(String uid) {
        return mDatabase.child("users").child(uid).child("username").get().continueWith(task -> {
            DataSnapshot snapshot = task.getResult();
            return snapshot.getValue(String.class);
        });
    }

    public Task<Void> createPost(String caption) {
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

            // Use a transaction to create the post and increment postCount
            return mFirestore.runTransaction(transaction -> {
                DocumentReference userRef = mFirestore.collection("users").document(uid);
                
                // Increment postCount on user document
                transaction.update(userRef, "postCount", FieldValue.increment(1));
                
                // Create the post document
                transaction.set(postRef, postMap);
                
                return null;
            });
        });
    }
}