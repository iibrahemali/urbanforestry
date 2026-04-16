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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserRepository {
    private final FirebaseAuth mAuth;
    private final DatabaseReference mDatabase;
    private final FirebaseFirestore mFirestore;
    private final FirebaseStorage mStorage;

    public UserRepository() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mFirestore = FirebaseFirestore.getInstance();
        mStorage = FirebaseStorage.getInstance();
    }

    public Task<Void> createUserProfile(String userId, String name, String username, String email) {
        Map<String, Object> rtdbMap = new HashMap<>();
        rtdbMap.put("name", name);
        rtdbMap.put("username", username);
        rtdbMap.put("email", email);

        Task<Void> rtdbTask = mDatabase.child("users").child(userId).setValue(rtdbMap);

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

    public Task<Void> updateProfile(String userId, String name, String username, String bio) {
        Map<String, Object> rtdbUpdates = new HashMap<>();
        rtdbUpdates.put("name", name);
        rtdbUpdates.put("username", username);
        Task<Void> rtdbTask = mDatabase.child("users").child(userId).updateChildren(rtdbUpdates);

        Map<String, Object> firestoreUpdates = new HashMap<>();
        firestoreUpdates.put("bio", bio);
        Task<Void> firestoreTask = mFirestore.collection("users").document(userId).update(firestoreUpdates);

        return Tasks.whenAll(rtdbTask, firestoreTask);
    }

    public Task<String> uploadProfilePicture(String userId, Uri imageUri) {
        StorageReference ref = mStorage.getReference().child("profile_pics/" + userId + ".jpg");
        return ref.putFile(imageUri).continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return ref.getDownloadUrl();
        }).continueWithTask(task -> {
            String downloadUrl = task.getResult().toString();
            // Store in Firestore
            Task<Void> firestoreTask = mFirestore.collection("users").document(userId)
                    .update("profilePicUrl", downloadUrl);
            // Also store in Realtime Database for easy access
            Task<Void> rtdbTask = mDatabase.child("users").child(userId)
                    .child("profilePicUrl").setValue(downloadUrl);
            
            return Tasks.whenAll(firestoreTask, rtdbTask).continueWith(t -> downloadUrl);
        });
    }
}