// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports Bundle, the key-value container Android passes to onCreate with any saved state
import android.os.Bundle;
// Imports Button for the Save and Cancel buttons
import android.widget.Button;
// Imports EditText for the name, username, and bio input fields
import android.widget.EditText;
// Imports Toast to show brief status or error messages
import android.widget.Toast;

// Imports AppCompatActivity, the base class that provides backwards-compatible Activity features
import androidx.appcompat.app.AppCompatActivity;

// Imports FirebaseAuth to get the currently logged-in user's UID
import com.google.firebase.auth.FirebaseAuth;
// Imports FirebaseUser to safely access the current user without null pointer risks
import com.google.firebase.auth.FirebaseUser;

// Declares EditProfileActivity as the screen where users can update their name, username, and bio
public class EditProfileActivity extends AppCompatActivity {

    // Declares the three editable fields shown on this screen
    private EditText editNameEditText, editUsernameEditText, editBioEditText;
    // Declares the Save and Cancel action buttons
    private Button saveProfileButton, cancelEditButton;
    // Declares the repository that handles all profile update database operations
    private UserRepository userRepository;
    // Stores the UID of the currently logged-in user — used as the key for all database writes
    private String currentUid;

    // Overrides onCreate, called when this Activity is first created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Applies the seasonal theme before layout inflation
        setTheme(SeasonManager.getSeasonTheme(SeasonManager.getSeasonPref(this)));

        // Calls the parent class onCreate to complete Android's standard Activity setup
        super.onCreate(savedInstanceState);
        // Inflates activity_edit_profile.xml and sets it as this screen's UI
        setContentView(R.layout.activity_edit_profile);

        // Instantiates the repository responsible for writing updated profile data to Firebase
        userRepository = new UserRepository();
        // Gets the currently logged-in FirebaseUser — if null, the session has expired
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // Closes the screen if no user is logged in, since there's nothing to edit without a valid session
        if (user == null) {
            finish();
            return;
        }
        // Stores the UID so every subsequent database call can identify whose data to update
        currentUid = user.getUid();

        // Links each UI field to its corresponding View in the XML layout
        editNameEditText = findViewById(R.id.editNameEditText);
        editUsernameEditText = findViewById(R.id.editUsernameEditText);
        editBioEditText = findViewById(R.id.editBioEditText);
        saveProfileButton = findViewById(R.id.saveProfileButton);
        cancelEditButton = findViewById(R.id.cancelEditButton);

        // Pre-fills the fields with the user's current data so they don't have to retype everything
        loadCurrentData();

        // Closes the screen without saving when the user taps Cancel
        cancelEditButton.setOnClickListener(v -> finish());

        // Handles saving the updated profile when the user taps Save
        saveProfileButton.setOnClickListener(v -> {
            // Reads and trims all input fields to prevent whitespace-only values being saved
            String name = editNameEditText.getText().toString().trim();
            String username = editUsernameEditText.getText().toString().trim();
            String bio = editBioEditText.getText().toString().trim();

            // Validates that the required fields are not empty — bio is optional so it's not checked here
            if (name.isEmpty() || username.isEmpty()) {
                Toast.makeText(this, "Name and Username cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            // Disables the button and changes its label to prevent duplicate save requests while the network call is in flight
            saveProfileButton.setEnabled(false);
            saveProfileButton.setText("Saving...");

            // Calls the repository to write the updated profile to both Firebase Realtime Database and Firestore
            userRepository.updateProfile(currentUid, name, username, bio)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                        // Closes the screen and returns to ProfileActivity after a successful save
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        // Re-enables the button so the user can try again after an error
                        saveProfileButton.setEnabled(true);
                        saveProfileButton.setText("Save Changes");
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });
    }

    // Pre-fills the edit fields with the user's current profile data to avoid forcing them to retype everything
    private void loadCurrentData() {
        // Loads the bio from Firestore since it's stored there rather than in the Realtime Database
        userRepository.getUserFirestoreData(currentUid).addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Sets the bio field if one was found in Firestore
                editBioEditText.setText(documentSnapshot.getString("bio"));
            }
        });

        // Reads the name and username from the Intent extras passed by ProfileActivity — this avoids an extra network call since those values are already on screen
        String existingName = getIntent().getStringExtra("currentName");
        String existingUsername = getIntent().getStringExtra("currentUsername");

        // Pre-fills name only if it was successfully passed via the Intent
        if (existingName != null) editNameEditText.setText(existingName);
        // Pre-fills username only if it was successfully passed via the Intent
        if (existingUsername != null) editUsernameEditText.setText(existingUsername);
    }
}
