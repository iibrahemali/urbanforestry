package com.example.urbanforestry;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ProfileActivity extends AppCompatActivity {

    private TextView accountName, usernameText, profileBio, postsCount, likesCount;
    private Button backButton, editProfileButton;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        backButton = findViewById(R.id.backButton);
        editProfileButton = findViewById(R.id.editProfileButton);

        backButton.setOnClickListener(v -> finish());

        editProfileButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditProfileActivity.class);
            // Pass current data to the edit screen
            intent.putExtra("currentName", accountName.getText().toString());
            String rawUsername = usernameText.getText().toString();
            if (rawUsername.startsWith("@")) rawUsername = rawUsername.substring(1);
            intent.putExtra("currentUsername", rawUsername);
            startActivity(intent);
        });

        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            
            // 1. Load basic info from Realtime Database (Name/Username)
            mDatabase.child("users").child(userId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("name").getValue(String.class);
                        String username = snapshot.child("username").getValue(String.class);

                        if (name != null) accountName.setText(name);
                        if (username != null) usernameText.setText("@" + username);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });

            // 2. Load Firestore Profile Data (Bio, PostCount)
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

            // 3. Calculate Total Likes from all user's posts
            userRepository.getUserPosts(userId).addOnSuccessListener(queryDocumentSnapshots -> {
                long totalLikes = 0;
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Long likes = document.getLong("likeCount");
                    if (likes != null) {
                        totalLikes += likes;
                    }
                }
                likesCount.setText(String.valueOf(totalLikes));
                
                // Sync postCount based on actual posts found
                postsCount.setText(String.valueOf(queryDocumentSnapshots.size()));
            });

        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }
}
