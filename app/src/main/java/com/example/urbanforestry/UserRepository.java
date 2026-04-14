package com.example.urbanforestry;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class UserRepository {
    private final FirebaseAuth mAuth;
    private final DatabaseReference mDatabase;
    private final FirebaseFirestore mFirestore;

    public UserRepository() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mFirestore = FirebaseFirestore.getInstance();
    }

    public Task<Void> createUserProfile(String userId, String name, String username, String email) {
        // Realtime Database: basic info
        Map<String, Object> rtdbMap = new HashMap<>();
        rtdbMap.put("name", name);
        rtdbMap.put("username", username);
        rtdbMap.put("email", email);

        Task<Void> rtdbTask = mDatabase.child("users").child(userId).setValue(rtdbMap);

        // Firestore: Extended profile document (cleaned up)
        Map<String, Object> firestoreMap = new HashMap<>();
        firestoreMap.put("uid", userId);
        firestoreMap.put("bio", "");
        firestoreMap.put("postCount", 0);
        firestoreMap.put("totalLikes", 0);
        firestoreMap.put("createdAt", FieldValue.serverTimestamp());

        Task<Void> firestoreTask = mFirestore.collection("users").document(userId).set(firestoreMap);

        return Tasks.whenAll(rtdbTask, firestoreTask);
    }

    public Task<DocumentSnapshot> getUserFirestoreData(String userId) {
        return mFirestore.collection("users").document(userId).get();
    }

    public Task<QuerySnapshot> getUserPosts(String userId) {
        return mFirestore.collection("posts")
                .whereEqualTo("uid", userId)
                .get();
    }
}