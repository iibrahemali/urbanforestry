// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports Intent for navigating to CreatePostActivity and ProfileActivity

import android.content.Intent;
// Imports Bundle, the key-value container Android passes to onCreate with any saved state
import android.os.Bundle;
// Imports Log for recording non-fatal errors from the Firestore snapshot listener
import android.util.Log;
// Imports ImageView for the profile picture button in the toolbar
import android.widget.ImageView;
// Imports Toast for brief feedback messages
import android.widget.Toast;

// Imports ActivityResultLauncher to handle the result from CreatePostActivity
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
// Imports NonNull for null-safety annotations on overridden callback methods
import androidx.annotation.NonNull;
// Imports AppCompatActivity, the base class that provides backwards-compatible Activity features
import androidx.appcompat.app.AppCompatActivity;
// Imports LinearLayoutManager to arrange posts vertically in the RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager;
// Imports RecyclerView to display the scrollable list of posts
import androidx.recyclerview.widget.RecyclerView;

// Imports Glide to load the user's profile picture into the toolbar button
import com.bumptech.glide.Glide;
// Imports FloatingActionButton for the "+" button that opens the post creation screen
import com.google.android.material.floatingactionbutton.FloatingActionButton;
// Imports FirebaseAuth to get the current user's UID for like/report state checks
import com.google.firebase.auth.FirebaseAuth;
// Imports FirebaseUser to safely access the current user
import com.google.firebase.auth.FirebaseUser;
// Imports Firebase Realtime Database types to read the user's profile picture URL
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
// Imports Firestore types for fetching and listening to the posts collection
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

// Imports ArrayList and List for managing the post data list
import java.util.ArrayList;
import java.util.List;

// Declares FeedActivity as the main social feed screen showing all user posts in reverse-chronological order
public class FeedActivity extends AppCompatActivity {

    // Declares the floating action button that opens the post creation chooser
    private FloatingActionButton fab;
    // Stores the combined list of static + Firestore posts displayed in the RecyclerView
    private List<Post> postList;
    // Declares the adapter that binds Post objects to RecyclerView item views
    private PostAdapter adapter;
    // Declares the Firestore instance used to listen for real-time post updates
    private FirebaseFirestore db;
    // Stores the pinned static demo posts that always appear at the top of the feed
    private List<Post> staticPosts;

    // Overrides onCreate, called when this Activity is first created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Applies the seasonal theme before layout inflation
        setTheme(SeasonManager.getSeasonTheme(SeasonManager.getSeasonPref(this)));

        // Calls the parent class onCreate to complete Android's standard Activity setup
        super.onCreate(savedInstanceState);
        // Inflates activity_feed.xml and sets it as this screen's UI
        setContentView(R.layout.activity_feed);

        // Gets the Firestore instance for real-time post queries
        db = FirebaseFirestore.getInstance();
        // Links the FloatingActionButton, profile image button, and RecyclerView to their Views in the layout
        fab = findViewById(R.id.fab_add);
        ImageView profileButton = findViewById(R.id.profile_button);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        // Initializes the static posts list before populating it
        staticPosts = new ArrayList<>();

        // Creates a pinned post showing the seasonal logo that links to Lancaster Library's location on the map
        int seasonLogo = SeasonManager.getSeasonLogo(SeasonManager.getSeasonPref(this));
        Post logoPost = new Post("Urban Forestry", seasonLogo, "Our logo (Click for directions to Lancaster Library!)");
        // Attaches the library's GPS coordinates so the "Get Directions" button works on this static post
        logoPost.hasLocation = true;
        logoPost.latitude = 40.04005;
        logoPost.longitude = -76.30612;
        staticPosts.add(logoPost);

        // Creates a pinned post for the historic Grant Knoll Sycamore tree
        Post sycamorePost = new Post("History Hunter", R.drawable.grant_noll_sycamore, "Grant Knoll Sycamore, The Oldest Tree in Lancaster County");
        // Attaches the sycamore's GPS coordinates so users can get directions to it
        sycamorePost.hasLocation = true;
        sycamorePost.latitude = 40.03814;
        sycamorePost.longitude = -76.34772;
        staticPosts.add(sycamorePost);

        // Initializes the combined post list and pre-fills it with the static posts
        postList = new ArrayList<>();
        postList.addAll(staticPosts);

        // Creates the adapter with the combined list and the current Context
        adapter = new PostAdapter(postList, this);

        // Attaches a LinearLayoutManager so posts scroll vertically, and sets the adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Starts the real-time Firestore listener that keeps the post list up to date
        fetchPostsFromFirestore();

        // Opens CreatePostActivity when the user taps the "+" button
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreatePostActivity.class);
            postLauncher.launch(intent);
        });

        // Opens ProfileActivity when the user taps their profile picture in the toolbar
        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        });

        // Gets Firebase references to load the current user's profile picture into the toolbar
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();

        // Loads the profile picture only if a user is currently logged in
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            // Attaches a real-time listener to the user's database record so the profile picture updates live
            mDatabase.child("users").child(userId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // Confirm that the data exists and the activity is still open
                    // Then, update the profile pic in the top-right
                    if (snapshot.exists() && !isDestroyed() && !isFinishing()) {
                        String picUrl = snapshot.child("profilePicUrl").getValue(String.class);
                        if (picUrl != null)
                            // Loads the user's custom profile picture into the toolbar button
                            Glide.with(FeedActivity.this).load(picUrl).into(profileButton);
                        else
                            // Falls back to the default avatar with a circular crop if no picture is set
                            Glide.with(FeedActivity.this).load(R.mipmap.default_pfp).circleCrop().into(profileButton);
                    }
                }

                // No action needed when the listener is disconnected
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }

        // Fades the "+" button to 25% opacity when the user scrolls to the bottom — prevents it from covering the last post
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // canScrollVertically(1) returns false when the list can't scroll down any further (i.e., at the bottom)
                if (!recyclerView.canScrollVertically(1))
                    fab.setAlpha(0.25f);
                else
                    fab.setAlpha(1f);
            }
        });
    }

    // Sets up a real-time Firestore listener that rebuilds the post list whenever any post is added, changed, or removed
    private void fetchPostsFromFirestore() {
        // Gets the current user's UID to check like and report states per post
        String currentUid = FirebaseAuth.getInstance().getUid();

        db.collection("posts")
                // Orders posts newest-first so the feed shows the most recent content at the top
                .orderBy("createdAt", Query.Direction.DESCENDING)
                // Registers a persistent listener — fires immediately on first load and again on every subsequent change
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w("FeedActivity", "Listen failed.", error);
                        return;
                    }

                    // Null value means the listener was detached — nothing to display
                    if (value == null) return;

                    // Builds the fresh list of fetched posts from the Firestore snapshot
                    List<Post> fetchedPosts = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        Post post = doc.toObject(Post.class);

                        // Backfills the text field from caption for legacy compatibility — some older UI paths use "text"
                        if (post.text == null && doc.contains("caption")) {
                            post.text = doc.getString("caption");
                        }

                        // Copies local UI state (liked, reported, comments expanded) from the existing list to preserve it across re-fetches
                        for (Post existingPost : postList) {
                            if (existingPost.postId != null && existingPost.postId.equals(post.postId)) {
                                post.isLikedByMe = existingPost.isLikedByMe;
                                post.isReportedByMe = existingPost.isReportedByMe;
                                post.isCommentsVisible = existingPost.isCommentsVisible;
                                break;
                            }
                        }

                        fetchedPosts.add(post);

                        // Checks Firestore subcollections for like/report state if they haven't been loaded yet for this post
                        if (currentUid != null) {
                            if (!post.isLikedByMe) checkIfLiked(post);
                            if (!post.isReportedByMe) checkIfReported(post);
                        }
                    }

                    // Rebuilds the combined list: static pinned posts first, then Firestore posts newest-first
                    postList.clear();
                    postList.addAll(staticPosts);
                    postList.addAll(fetchedPosts);
                    // Tells the adapter the entire dataset changed so it redraws all visible items
                    adapter.notifyDataSetChanged();
                });
    }

    // Checks Firestore to see if the current user has liked a specific post — used to restore like button state after re-fetching
    private void checkIfLiked(Post post) {
        String uid = FirebaseAuth.getInstance().getUid();
        // Skips the check if there's no user or if the post doesn't have a real Firestore ID
        if (uid == null || post.postId == null) return;

        // Looks for a document in the "likes" subcollection keyed by the current user's UID
        db.collection("posts").document(post.postId)
                .collection("likes").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // If a like document exists, marks the post as liked and refreshes the UI
                        post.isLikedByMe = true;
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    // Checks Firestore to see if the current user has reported a specific post — used to restore the report button state
    private void checkIfReported(Post post) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || post.postId == null) return;

        // Looks for a document in the "reports" subcollection keyed by the current user's UID
        db.collection("posts").document(post.postId)
                .collection("reports").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // If a report document exists, marks the post as reported and refreshes the UI
                        post.isReportedByMe = true;
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    // Registers a launcher for CreatePostActivity — shows a success toast when a new post is created
    private ActivityResultLauncher<Intent> postLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            // The Firestore listener will automatically pick up the new post — no manual refresh needed
                            Toast.makeText(this, "Post shared!", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
}
