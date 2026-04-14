package com.example.urbanforestry;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FeedActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FloatingActionButton fab;
    private List<Post> postList;
    private PostAdapter adapter;
    private FirebaseFirestore db;
    private Post logoPost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recyclerView);
        fab = findViewById(R.id.fab_add);
        ImageView profileButton = findViewById(R.id.profile_button);

        // Initialize logo post and keep a persistent reference to it
        logoPost = new Post("Urban Forestry", R.drawable.logo_title, "Our logo (Click for directions to Lancaster Library!)");
        logoPost.hasLocation = true;
        logoPost.latitude = 40.04005;
        logoPost.longitude = -76.30612;

        postList = new ArrayList<>();
        postList.add(logoPost);
        
        adapter = new PostAdapter(postList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Fetch posts from Firebase Firestore
        fetchPostsFromFirestore();

        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreatePostActivity.class);
            postLauncher.launch(intent);
        });

        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        });
    }

    private void fetchPostsFromFirestore() {
        String currentUid = FirebaseAuth.getInstance().getUid();
        
        db.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w("FeedActivity", "Listen failed.", error);
                        return;
                    }

                    if (value == null) return;

                    List<Post> fetchedPosts = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        Post post = doc.toObject(Post.class);
                        
                        // Map Firestore "caption" to Post "text" if needed
                        if (post.text == null && doc.contains("caption")) {
                            post.text = doc.getString("caption");
                        }

                        // Preserve local state (like heart fill) if the post is already in our list
                        for (Post existingPost : postList) {
                            if (existingPost.postId != null && existingPost.postId.equals(post.postId)) {
                                post.isLikedByMe = existingPost.isLikedByMe;
                                post.isCommentsVisible = existingPost.isCommentsVisible;
                                break;
                            }
                        }
                        
                        fetchedPosts.add(post);
                        
                        // If this is the first time we see this post, check if user has liked it in DB
                        if (currentUid != null && !post.isLikedByMe) {
                            checkIfLiked(post);
                        }
                    }

                    // Update the main list while keeping the logo post at index 0
                    postList.clear();
                    postList.add(logoPost);
                    postList.addAll(fetchedPosts);
                    adapter.notifyDataSetChanged();
                });
    }

    private void checkIfLiked(Post post) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || post.postId == null) return;

        db.collection("posts").document(post.postId)
                .collection("likes").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        post.isLikedByMe = true;
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private ActivityResultLauncher<Intent> postLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Toast.makeText(this, "Post shared!", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
}
