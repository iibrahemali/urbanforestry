// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports Uri to represent the image file selected from the gallery or taken by the camera
import android.net.Uri;
// Imports Task and Tasks from Google's GMS library to chain asynchronous operations and combine multiple tasks
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
// Imports FirebaseAuth to get the currently signed-in user's UID for data ownership
import com.google.firebase.auth.FirebaseAuth;
// Imports Firebase Realtime Database types for reading and writing user data stored there
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
// Imports Firestore types for the user's bio, post count, and profile picture URL stored there
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
// Imports Firebase Storage types for uploading profile picture images
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

// Imports HashMap and Map for building the key-value data structures written to Firebase
import java.util.HashMap;
import java.util.Map;
// Imports UUID to generate a unique filename for uploaded profile pictures, preventing overwrites
import java.util.UUID;

// Declares UserRepository as a data layer class that encapsulates all Firebase operations related to user accounts
public class UserRepository {
    // Declares FirebaseAuth for accessing the current user's authentication state
    private final FirebaseAuth mAuth;
    // Declares the Realtime Database reference used for name, username, and profile picture URL (fast, simple reads)
    private final DatabaseReference mDatabase;
    // Declares Firestore for bio, post count, and structured user documents
    private final FirebaseFirestore mFirestore;
    // Declares Firebase Storage for uploading and downloading profile picture image files
    private final FirebaseStorage mStorage;

    // Constructor that initialises all four Firebase service instances — called once per screen that needs user data
    public UserRepository() {
        mAuth = FirebaseAuth.getInstance();
        // Gets the root reference of the Realtime Database so we can navigate to any child node
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mFirestore = FirebaseFirestore.getInstance();
        mStorage = FirebaseStorage.getInstance();
    }

    // Creates a new user profile in both the Realtime Database and Firestore when a user signs up
    public Task<Void> createUserProfile(String userId, String name, String username, String email) {
        // Builds the map of fields to store in the Realtime Database — name/username/email are stored here for fast lookups
        Map<String, Object> rtdbMap = new HashMap<>();
        rtdbMap.put("name", name);
        rtdbMap.put("username", username);
        rtdbMap.put("email", email);

        // Writes the Realtime Database user record
        Task<Void> rtdbTask = mDatabase.child("users").child(userId).setValue(rtdbMap);

        // Builds the separate Firestore document for the user — stores social data like bio and post count
        Map<String, Object> firestoreMap = new HashMap<>();
        firestoreMap.put("uid", userId);
        firestoreMap.put("bio", "");
        // Initialises post count and total likes to 0 so the profile screen can display them immediately
        firestoreMap.put("postCount", 0);
        firestoreMap.put("totalLikes", 0);
        // Uses a server-side timestamp so the created date is authoritative and timezone-independent
        firestoreMap.put("createdAt", FieldValue.serverTimestamp());

        // Writes the Firestore user document
        Task<Void> firestoreTask = mFirestore.collection("users").document(userId).set(firestoreMap);

        // Waits for both writes to complete before reporting success — ensures neither database is partially written
        return Tasks.whenAll(rtdbTask, firestoreTask);
    }

    // Fetches the user's Firestore document — used on the Profile and Edit Profile screens to read bio and post count
    public Task<DocumentSnapshot> getUserFirestoreData(String userId) {
        return mFirestore.collection("users").document(userId).get();
    }

    // Fetches all posts owned by the given user — used by ProfileActivity to calculate total likes
    public Task<QuerySnapshot> getUserPosts(String userId) {
        return mFirestore.collection("posts")
                // Filters to only return documents where the "uid" field matches this user
                .whereEqualTo("uid", userId)
                .get();
    }

    // Updates the user's name, username (in Realtime Database) and bio (in Firestore) simultaneously
    public Task<Void> updateProfile(String userId, String name, String username, String bio) {
        // Builds the Realtime Database update map — only includes the fields we want to change
        Map<String, Object> rtdbUpdates = new HashMap<>();
        rtdbUpdates.put("name", name);
        rtdbUpdates.put("username", username);
        // Uses updateChildren() rather than setValue() to avoid overwriting other fields like email
        Task<Void> rtdbTask = mDatabase.child("users").child(userId).updateChildren(rtdbUpdates);

        // Builds the Firestore update map for the bio — stored separately because Firestore is used for richer user data
        Map<String, Object> firestoreUpdates = new HashMap<>();
        firestoreUpdates.put("bio", bio);
        Task<Void> firestoreTask = mFirestore.collection("users").document(userId).update(firestoreUpdates);

        // Waits for both updates to complete so the caller knows the full profile was saved before proceeding
        return Tasks.whenAll(rtdbTask, firestoreTask);
    }

    // Uploads a new profile picture to Firebase Storage and saves the download URL to both databases
    public Task<String> uploadProfilePicture(String userId, Uri imageUri) {
        // Stores all profile pictures under "profile_pics/{userId}.jpg" — using the UID as the filename ensures only one image per user
        StorageReference ref = mStorage.getReference().child("profile_pics/" + userId + ".jpg");
        // Uploads the image file and then chains the download URL fetch
        return ref.putFile(imageUri).continueWithTask(task -> {
            // Re-throws the upload error so the caller's failure listener receives it
            if (!task.isSuccessful()) throw task.getException();
            // Fetches the public download URL for the uploaded file
            return ref.getDownloadUrl();
        }).continueWithTask(task -> {
            // Converts the URL URI to a String since that's what both databases store
            String downloadUrl = task.getResult().toString();
            // Saves the download URL to Firestore so it can be read alongside other profile data
            Task<Void> firestoreTask = mFirestore.collection("users").document(userId)
                    .update("profilePicUrl", downloadUrl);
            // Also saves to Realtime Database so FeedActivity can load the profile picture quickly without a Firestore read
            Task<Void> rtdbTask = mDatabase.child("users").child(userId)
                    .child("profilePicUrl").setValue(downloadUrl);

            // Waits for both saves, then passes the download URL through to the success listener
            return Tasks.whenAll(firestoreTask, rtdbTask).continueWith(t -> downloadUrl);
        });
    }
}
