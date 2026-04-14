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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recyclerView);
        fab = findViewById(R.id.fab_add);
        ImageView profileButton = findViewById(R.id.profile_button);

        postList = new ArrayList<>();
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
        db.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w("FeedActivity", "Listen failed.", error);
                        return;
                    }

                    postList.clear();
                    // Keep the local dummy logo post at the top for testing directions
                    Post logoPost = new Post("Urban Forestry", R.drawable.logo_title, "Our logo (Click for directions to Lancaster Library!)");
                    logoPost.hasLocation = true;
                    logoPost.latitude = 40.04005;
                    logoPost.longitude = -76.30612;
                    postList.add(logoPost);

                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Post post = doc.toObject(Post.class);
                            // Map Firestore "caption" to Post "text" if needed
                            if (post.text == null && doc.contains("caption")) {
                                post.text = doc.getString("caption");
                            }
                            postList.add(post);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private ActivityResultLauncher<Intent> postLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            // After adding a post, the Firestore SnapshotListener will 
                            // automatically update the list.
                            Toast.makeText(this, "Post shared!", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
}
