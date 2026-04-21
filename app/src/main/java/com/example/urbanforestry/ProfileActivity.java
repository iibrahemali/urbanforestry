package com.example.urbanforestry;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private TextView accountName, usernameText, profileBio, postsCount, likesCount;
    private ImageView profileImage;
    private Button backButton, editProfileButton;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private UserRepository userRepository;
    private Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set seasonal theme before onCreate
        setTheme(SeasonManager.getSeasonTheme(SeasonManager.getCurrentSeason()));
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        userRepository = new UserRepository();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        accountName = findViewById(R.id.accountName);
        usernameText = findViewById(R.id.username);
        profileBio = findViewById(R.id.profileBio);
        postsCount = findViewById(R.id.postsCount);
        likesCount = findViewById(R.id.likesCount);
        profileImage = findViewById(R.id.profileImage);
        backButton = findViewById(R.id.backButton);
        editProfileButton = findViewById(R.id.editProfileButton);

        backButton.setOnClickListener(v -> finish());

        profileImage.setOnClickListener(v -> showImageSourceDialog());

        editProfileButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditProfileActivity.class);
            intent.putExtra("currentName", accountName.getText().toString());
            String rawUsername = usernameText.getText().toString();
            if (rawUsername.startsWith("@")) rawUsername = rawUsername.substring(1);
            intent.putExtra("currentUsername", rawUsername);
            startActivity(intent);
        });

        loadUserProfile();
    }

    private void showImageSourceDialog() {
        String[] options = {"Gallery", "Camera"};
        new AlertDialog.Builder(this)
                .setTitle("Select Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        galleryLauncher.launch("image/*");
                    } else {
                        openCamera();
                    }
                })
                .show();
    }

    private void openCamera() {
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show();
        }
        if (photoFile != null) {
            photoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            cameraLauncher.launch(photoUri);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadImage(uri);
                }
            }
    );

    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success && photoUri != null) {
                    uploadImage(photoUri);
                }
            }
    );

    private void uploadImage(Uri uri) {
        String userId = mAuth.getCurrentUser().getUid();
        userRepository.uploadProfilePicture(userId, uri)
                .addOnSuccessListener(downloadUrl -> {
                    Glide.with(this).load(downloadUrl).circleCrop().into(profileImage);
                    Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            
            mDatabase.child("users").child(userId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("name").getValue(String.class);
                        String username = snapshot.child("username").getValue(String.class);
                        String picUrl = snapshot.child("profilePicUrl").getValue(String.class);

                        if (name != null) accountName.setText(name);
                        if (username != null) usernameText.setText("@" + username);
                        if (picUrl != null) {
                            Glide.with(ProfileActivity.this).load(picUrl).circleCrop().into(profileImage);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });

            userRepository.getUserFirestoreData(userId).addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String bio = documentSnapshot.getString("bio");
                    Long pCount = documentSnapshot.getLong("postCount");
                    
                    if (bio != null && !bio.isEmpty()) {
                        profileBio.setText(bio);
                    } else {
                        profileBio.setText("No bio yet.");
                    }
                    
                    if (pCount != null) {
                        postsCount.setText(String.valueOf(pCount));
                    }
                }
            });

            userRepository.getUserPosts(userId).addOnSuccessListener(queryDocumentSnapshots -> {
                long totalLikes = 0;
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Long likes = document.getLong("likeCount");
                    if (likes != null) {
                        totalLikes += likes;
                    }
                }
                likesCount.setText(String.valueOf(totalLikes));
                postsCount.setText(String.valueOf(queryDocumentSnapshots.size()));
            });

        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }
}
