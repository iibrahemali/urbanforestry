package com.example.urbanforestry;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class FeedActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FloatingActionButton fab;
    private List<Post> postList;
    private PostAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        recyclerView = findViewById(R.id.recyclerView);
        fab = findViewById(R.id.fab_add);

        postList = new ArrayList<>();
        adapter = new PostAdapter(postList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // TEMP: fake data (replace with Firebase later)
        loadDummyPosts();

        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreatePostActivity.class);
            postLauncher.launch(intent);
        });
    }

    private void loadDummyPosts() {
        // Use R.drawable.logo_title instead of a file path
        postList.add(new Post("User1", R.drawable.logo_title, "This is our logo"));
        postList.add(new Post("User2", null, "This is a text post example"));
        adapter.notifyDataSetChanged();
    }

    private ActivityResultLauncher<Intent> postLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {

                            Intent data = result.getData();

                            // TEXT POST
                            if (data.getStringExtra("text") != null) {
                                String text = data.getStringExtra("text");
                                postList.add(0, new Post("You", null, text));
                            }

                            // IMAGE POST
                            else if (data.getStringExtra("imagePath") != null) {
                                String path = data.getStringExtra("imagePath");
                                postList.add(0, new Post("You", path, null));
                            }

                            adapter.notifyDataSetChanged();
                        }
                    }
            );
}
