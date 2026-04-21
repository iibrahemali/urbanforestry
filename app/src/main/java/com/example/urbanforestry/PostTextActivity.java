package com.example.urbanforestry;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class PostTextActivity extends AppCompatActivity {

    private EditText editText;
    private Button postBtn;
    private PostRepository postRepository;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set seasonal theme before onCreate
        setTheme(SeasonManager.getSeasonTheme(SeasonManager.getCurrentSeason()));
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_text);

        postRepository = new PostRepository();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

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

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // If no permission, post without location
                submitPost(caption, null, 0);
                return;
            }

            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    submitPost(caption, location.getLatitude(), location.getLongitude());
                } else {
                    submitPost(caption, null, 0);
                }
            }).addOnFailureListener(e -> {
                submitPost(caption, null, 0);
            });
        });
    }

    private void submitPost(String caption, Double lat, double lng) {
        postRepository.createPost(caption, lat, lng)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Post created!", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                postBtn.setEnabled(true);
                postBtn.setText("Post");
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }
}
