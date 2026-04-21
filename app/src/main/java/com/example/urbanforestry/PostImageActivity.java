package com.example.urbanforestry;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.File;

public class PostImageActivity extends AppCompatActivity {

    private String imagePath;
    private EditText captionEditText;
    private Button postBtn;
    private PostRepository postRepository;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_image);

        postRepository = new PostRepository();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

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

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // If no permission, post without location
                submitPost(caption, null, null);
                return;
            }

            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    submitPost(caption, location.getLatitude(), location.getLongitude());
                } else {
                    submitPost(caption, null, null);
                }
            }).addOnFailureListener(e -> {
                submitPost(caption, null, null);
            });
        });
    }

    private void submitPost(String caption, Double lat, Double lon) {
        Uri imageUri = Uri.fromFile(new File(imagePath));
        postRepository.createImagePost(caption, imageUri, lat, lon)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Post uploaded!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    // Return to the feed
                    Intent i = new Intent(PostImageActivity.this, FeedActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                    finish();
                })
                .addOnFailureListener(e -> {
                    postBtn.setEnabled(true);
                    postBtn.setText("Post");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
