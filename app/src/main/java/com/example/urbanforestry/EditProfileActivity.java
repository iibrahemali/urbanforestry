package com.example.urbanforestry;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EditProfileActivity extends AppCompatActivity {

    private EditText editNameEditText, editUsernameEditText, editBioEditText;
    private Button saveProfileButton, cancelEditButton;
    private UserRepository userRepository;
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set seasonal theme before onCreate
        setTheme(SeasonManager.getSeasonTheme(SeasonManager.getSeasonPref(this)));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        userRepository = new UserRepository();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        currentUid = user.getUid();

        editNameEditText = findViewById(R.id.editNameEditText);
        editUsernameEditText = findViewById(R.id.editUsernameEditText);
        editBioEditText = findViewById(R.id.editBioEditText);
        saveProfileButton = findViewById(R.id.saveProfileButton);
        cancelEditButton = findViewById(R.id.cancelEditButton);

        loadCurrentData();

        cancelEditButton.setOnClickListener(v -> finish());

        saveProfileButton.setOnClickListener(v -> {
            String name = editNameEditText.getText().toString().trim();
            String username = editUsernameEditText.getText().toString().trim();
            String bio = editBioEditText.getText().toString().trim();

            if (name.isEmpty() || username.isEmpty()) {
                Toast.makeText(this, "Name and Username cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            saveProfileButton.setEnabled(false);
            saveProfileButton.setText("Saving...");

            userRepository.updateProfile(currentUid, name, username, bio)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        saveProfileButton.setEnabled(true);
                        saveProfileButton.setText("Save Changes");
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });
    }

    private void loadCurrentData() {
        // Pre-fill fields with current data
        userRepository.getUserFirestoreData(currentUid).addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                editBioEditText.setText(documentSnapshot.getString("bio"));
            }
        });

        // We could also pull name/username from Realtime DB here if needed, 
        // but typically you'd pass them via Intent to make it instant.
        String existingName = getIntent().getStringExtra("currentName");
        String existingUsername = getIntent().getStringExtra("currentUsername");

        if (existingName != null) editNameEditText.setText(existingName);
        if (existingUsername != null) editUsernameEditText.setText(existingUsername);
    }
}
