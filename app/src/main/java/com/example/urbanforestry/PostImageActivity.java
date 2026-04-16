package com.example.urbanforestry;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class PostImageActivity extends AppCompatActivity {

    private String imagePath;
    private EditText captionEditText;
    private Button postBtn;
    private PostRepository postRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_image);

        postRepository = new PostRepository();
        imagePath = getIntent().getStringExtra("imagePath");
        ImageView imagePreview = findViewById(R.id.imagePreview);
        captionEditText = findViewById(R.id.captionEditText);
        postBtn = findViewById(R.id.btn_post);

        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            imagePreview.setImageBitmap(bitmap);
        }

        postBtn.setOnClickListener(v -> {
            String caption = captionEditText.getText().toString().trim();
            if (imagePath == null) {
                Toast.makeText(this, "No image to post", Toast.LENGTH_SHORT).show();
                return;
            }

            postBtn.setEnabled(false);
            postBtn.setText("Posting...");

            // In a real app, you'd get actual lat/lon from a Location provider.
            // Using 0.0 for now as a placeholder.
            Uri imageUri = Uri.fromFile(new File(imagePath));
            postRepository.createImagePost(caption, imageUri, 0.0, 0.0)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Post uploaded!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
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