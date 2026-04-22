package com.example.urbanforestry;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FeedActivity extends AppCompatActivity {

    private FloatingActionButton fab;
    private List<Post> postList;
    private PostAdapter adapter;
    private FirebaseFirestore db;
    private List<Post> staticPosts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set seasonal theme before onCreate
        setTheme(SeasonManager.getSeasonTheme(SeasonManager.getSeasonPref(this)));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        db = FirebaseFirestore.getInstance();
        fab = findViewById(R.id.fab_add);
        ImageView profileButton = findViewById(R.id.profile_button);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        // Initialize static posts
        staticPosts = new ArrayList<>();

        // 1. Seasonal Logo Post
        int seasonLogo = SeasonManager.getSeasonLogo(SeasonManager.getSeasonPref(this));
        Post logoPost = new Post("Urban Forestry", seasonLogo, "Our logo (Click for directions to Lancaster Library!)");
        logoPost.hasLocation = true;
        logoPost.latitude = 40.04005;
        logoPost.longitude = -76.30612;
        staticPosts.add(logoPost);

        // 2. Grant Knoll Sycamore
        // Location: 265 Plane Tree Dr, Lancaster, PA 17603 -> Approx: 40.0381, -76.3477
        Post sycamorePost = new Post("History Hunter", R.drawable.grant_noll_sycamore, "Grant Knoll Sycamore, The Oldest Tree in Lancaster County");
        sycamorePost.hasLocation = true;
        sycamorePost.latitude = 40.03814;
        sycamorePost.longitude = -76.34772;
        staticPosts.add(sycamorePost);

        postList = new ArrayList<>();
        postList.addAll(staticPosts);

        adapter = new PostAdapter(postList, this);

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

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();

        // Load the user's profile picture
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            mDatabase.child("users").child(userId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && !isDestroyed() && !isFinishing()) {
                        String picUrl = snapshot.child("profilePicUrl").getValue(String.class);
                        if (picUrl != null)
                            Glide.with(FeedActivity.this).load(picUrl).into(profileButton);
                        else
                            Glide.with(FeedActivity.this).load(R.mipmap.default_pfp).circleCrop().into(profileButton);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }

        // Make the "+" button transparent when scrolled to the bottom
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (!recyclerView.canScrollVertically(1))
                    fab.setAlpha(0.25f);
                else
                    fab.setAlpha(1f);
            }
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

                        if (post.text == null && doc.contains("caption")) {
                            post.text = doc.getString("caption");
                        }

                        // Preserve local state (INCLUDING reported status)
                        for (Post existingPost : postList) {
                            if (existingPost.postId != null && existingPost.postId.equals(post.postId)) {
                                post.isLikedByMe = existingPost.isLikedByMe;
                                post.isReportedByMe = existingPost.isReportedByMe;
                                post.isCommentsVisible = existingPost.isCommentsVisible;
                                break;
                            }
                        }

                        fetchedPosts.add(post);

                        if (currentUid != null) {
                            if (!post.isLikedByMe) checkIfLiked(post);
                            if (!post.isReportedByMe) checkIfReported(post);
                        }
                    }

                    // Refresh list: Static posts first, then newest Firestore posts
                    postList.clear();
                    postList.addAll(staticPosts);
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

    private void checkIfReported(Post post) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || post.postId == null) return;

        db.collection("posts").document(post.postId)
                .collection("reports").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        post.isReportedByMe = true;
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
