package com.example.urbanforestry;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PostTextActivity extends AppCompatActivity {

    private EditText editText;
    private Button postBtn;
    private PostRepository postRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_text);

        postRepository = new PostRepository();
        editText = findViewById(R.id.editText);
        postBtn = findViewById(R.id.btn_post);

        postBtn.setOnClickListener(v -> {
            String caption = editText.getText().toString().trim();
            if (caption.isEmpty()) {
                Toast.makeText(this, "Please write something", Toast.LENGTH_SHORT).show();
                return;
            }

            postBtn.setEnabled(false);
            postBtn.setText("Posting...");

            postRepository.createPost(caption)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Post created!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    postBtn.setEnabled(true);
                    postBtn.setText("Post");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
        });
    }
}