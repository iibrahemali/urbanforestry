// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports AlertDialog to show a bottom-sheet chooser when the user taps their profile picture
import android.app.AlertDialog;
// Imports Intent for navigating to EditProfileActivity and for launching the camera and gallery
import android.content.Intent;
// Imports Uri to represent the selected or captured image as a content URI
import android.net.Uri;
// Imports Bundle, the key-value container Android passes to onCreate with any saved state
import android.os.Bundle;
// Imports Environment to get the standard Pictures directory for the camera output file
import android.os.Environment;
// Imports MediaStore (unused here but part of camera-related imports)
import android.provider.MediaStore;
// Imports View for generic View interactions
import android.view.View;
// Imports Button for the Back and Edit Profile buttons
import android.widget.Button;
// Imports ImageView to display the profile picture
import android.widget.ImageView;
// Imports TextView to display the name, username, bio, posts count, and likes count
import android.widget.TextView;
// Imports Toast to show brief feedback messages
import android.widget.Toast;

// Imports ActivityResultLauncher and contracts to handle gallery and camera results in a type-safe way
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
// Imports NonNull for null-safety annotations on overridden callback methods
import androidx.annotation.NonNull;
// Imports AppCompatActivity, the base class that provides backwards-compatible Activity features
import androidx.appcompat.app.AppCompatActivity;
// Imports Toolbar for the custom navigation bar at the top of the screen
import androidx.appcompat.widget.Toolbar;
// Imports FileProvider to create secure content URIs for camera output files on Android 7+
import androidx.core.content.FileProvider;

// Imports Glide to load and display profile pictures from URLs or resource IDs efficiently
import com.bumptech.glide.Glide;
// Imports FirebaseAuth to identify the currently logged-in user
import com.google.firebase.auth.FirebaseAuth;
// Imports FirebaseUser to access the current user's UID
import com.google.firebase.auth.FirebaseUser;
// Imports Firebase Realtime Database classes to read name and username (stored there, not Firestore)
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
// Imports Firestore snapshot types to iterate over the user's posts when counting likes
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

// Imports File to create a temporary file for the camera output
import java.io.File;
// Imports IOException for handling file creation errors
import java.io.IOException;
// Imports SimpleDateFormat and Date to generate unique timestamp-based filenames for camera photos
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// Declares ProfileActivity as the screen showing the logged-in user's profile data, stats, and photo controls
public class ProfileActivity extends AppCompatActivity {

    // Declares TextViews for the user's display name, username handle, bio, post count, and total likes received
    private TextView accountName, usernameText, profileBio, postsCount, likesCount;
    // Declares the ImageView that shows the profile picture and opens the image source picker when tapped
    private ImageView profileImage;
    // Declares the Back and Edit Profile buttons
    private Button backButton, editProfileButton;
    // Declares the Firebase Realtime Database reference for reading name and username
    private DatabaseReference mDatabase;
    // Declares FirebaseAuth for getting the current user's UID
    private FirebaseAuth mAuth;
    // Declares the repository that handles profile data reads and profile picture uploads
    private UserRepository userRepository;
    // Stores the URI of a photo taken by the camera — kept as a field so it's accessible in the camera result callback
    private Uri photoUri;
    // Stores the current profile picture URL — used to decide whether to show a "Remove" option in the picker dialog
    private String profilePicUrl;

    // Overrides onCreate, called when this Activity is first created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Applies the seasonal theme before layout inflation
        setTheme(SeasonManager.getSeasonTheme(SeasonManager.getSeasonPref(this)));

        // Calls the parent class onCreate to complete Android's standard Activity setup
        super.onCreate(savedInstanceState);
        // Inflates activity_profile.xml and sets it as this screen's UI
        setContentView(R.layout.activity_profile);

        // Gets the Firebase auth, database, and repository instances used throughout this screen
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        userRepository = new UserRepository();

        // Sets up the Toolbar as the ActionBar so the built-in back arrow works correctly
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Hides the default title since the layout uses a custom title TextView
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        // Closes the Activity when the user taps the back arrow in the Toolbar
        toolbar.setNavigationOnClickListener(v -> finish());

        // Links all UI field variables to their corresponding Views in the XML layout
        accountName = findViewById(R.id.accountName);
        usernameText = findViewById(R.id.username);
        profileBio = findViewById(R.id.profileBio);
        postsCount = findViewById(R.id.postsCount);
        likesCount = findViewById(R.id.likesCount);
        profileImage = findViewById(R.id.profileImage);
        backButton = findViewById(R.id.backButton);
        editProfileButton = findViewById(R.id.editProfileButton);

        // Closes the Activity when the user taps the back button
        backButton.setOnClickListener(v -> finish());

        // Opens the profile picture source picker (Gallery / Camera / Remove) when the user taps their photo
        profileImage.setOnClickListener(v -> showImageSourceDialog());

        // Navigates to EditProfileActivity, passing the current name and username as Intent extras so fields are pre-filled
        editProfileButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditProfileActivity.class);
            intent.putExtra("currentName", accountName.getText().toString());
            // Strips the leading "@" from the username display before passing it, since EditProfileActivity doesn't use "@"
            String rawUsername = usernameText.getText().toString();
            if (rawUsername.startsWith("@")) rawUsername = rawUsername.substring(1);
            intent.putExtra("currentUsername", rawUsername);
            startActivity(intent);
        });

        // Fetches and displays the user's profile data from Firebase
        loadUserProfile();
    }

    // Shows a dialog letting the user choose between Gallery, Camera, or Remove for their profile picture
    private void showImageSourceDialog() {
        // Only offers the "Remove" option if there is currently a profile picture set — prevents removing a non-existent photo
        String[] options;
        if (profilePicUrl == null)
            options = new String[]{"Gallery", "Camera"};
        else
            options = new String[]{"Gallery", "Camera", "Remove"};
        new AlertDialog.Builder(this)
                .setTitle("Select Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Launches the system gallery picker to select an existing photo
                        galleryLauncher.launch("image/*");
                    } else if (which == 1) {
                        // Opens the camera to take a new profile photo
                        openCamera();
                    } else {
                        // Removes the current profile picture and reverts to the default avatar
                        removeProfilePic();
                    }
                })
                .show();
    }

    // Creates a temporary file and opens the camera to capture a new profile photo
    private void openCamera() {
        File photoFile = null;
        try {
            // Creates a new unique temp file in the Pictures directory for the camera to write into
            photoFile = createImageFile();
        } catch (IOException ex) {
            Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show();
        }
        if (photoFile != null) {
            // Converts the file path to a content URI using FileProvider — required for camera access on Android 7+
            photoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            // Launches the camera, instructing it to save the captured photo to our URI
            cameraLauncher.launch(photoUri);
        }
    }

    // Creates a uniquely named temporary image file to serve as the camera output destination
    private File createImageFile() throws IOException {
        // Generates a timestamp string to make the filename unique and prevent overwriting previous captures
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        // Writes the file to the app's private external Pictures directory so it doesn't appear in the gallery
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    // Registers a launcher that opens the system gallery and handles the selected image URI
    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    // Uploads the selected image to Firebase Storage and updates the profile picture
                    uploadImage(uri);
                }
            }
    );

    // Registers a launcher that captures a photo with the camera and handles the success flag
    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success && photoUri != null) {
                    // Uploads the captured photo to Firebase Storage using the URI we created before launching the camera
                    uploadImage(photoUri);
                }
            }
    );

    // Uploads the selected or captured image to Firebase Storage and updates the profile picture displayed on screen
    private void uploadImage(Uri uri) {
        // Gets the current user's UID to use as the storage path key
        String userId = mAuth.getCurrentUser().getUid();
        userRepository.uploadProfilePicture(userId, uri)
                .addOnSuccessListener(downloadUrl -> {
                    // Checks that the Activity is still visible before touching the UI — avoids crashes from background callbacks
                    if (!isDestroyed() && !isFinishing()) {
                        // Loads the newly uploaded photo into the ImageView with a circular crop for the profile picture style
                        Glide.with(this).load(downloadUrl).circleCrop().into(profileImage);
                        Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Clears the profile picture URL from the database and reverts the displayed image to the default avatar
    private void removeProfilePic() {
        String userId = mAuth.getCurrentUser().getUid();
        // Sets the profilePicUrl field to null in the Realtime Database so future loads use the default avatar
        mDatabase.child("users").child(userId).child("profilePicUrl").setValue(null)
                .addOnSuccessListener(downloadUrl -> {
                    if (!isDestroyed() && !isFinishing()) {
                        // Loads the default profile picture resource as a circular crop
                        Glide.with(this).load(R.mipmap.default_pfp).circleCrop().into(profileImage);
                        Toast.makeText(this, "Profile picture removed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Fetches the user's profile data from Firebase and populates all TextViews on the screen
    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            // Attaches a real-time listener to the user's Realtime Database record so the UI updates live
            mDatabase.child("users").child(userId).addValueEventListener(new ValueEventListener() {
                // Called whenever the user's data changes in the Realtime Database
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Reads the name, username, and profile picture URL fields from the database snapshot
                        String name = snapshot.child("name").getValue(String.class);
                        String username = snapshot.child("username").getValue(String.class);
                        String picUrl = snapshot.child("profilePicUrl").getValue(String.class);
                        // Stores the URL so the image picker dialog knows whether to offer a "Remove" option
                        profilePicUrl = picUrl;

                        // Updates each TextView only if the value is non-null to avoid blanking out existing text
                        if (name != null) accountName.setText(name);
                        // Prepends "@" to the username to match the social media convention
                        if (username != null) usernameText.setText("@" + username);
                        if (picUrl != null && !isDestroyed() && !isFinishing()) {
                            // Loads the profile picture URL into the ImageView with a circular crop
                            Glide.with(ProfileActivity.this).load(picUrl).circleCrop().into(profileImage);
                        }
                    }
                }

                // Called if the database listener is disconnected — no UI action needed here
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });

            // Fetches the user's Firestore document to read the bio and post count — these are stored in Firestore, not the Realtime DB
            userRepository.getUserFirestoreData(userId).addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String bio = documentSnapshot.getString("bio");
                    Long pCount = documentSnapshot.getLong("postCount");

                    // Shows the bio if one was saved, otherwise displays a placeholder so the field isn't empty
                    if (bio != null && !bio.isEmpty()) {
                        profileBio.setText(bio);
                    } else {
                        profileBio.setText("No bio yet.");
                    }

                    // Sets the post count if the Firestore document has one
                    if (pCount != null) {
                        postsCount.setText(String.valueOf(pCount));
                    }
                }
            });

            // Fetches all of the user's posts to calculate their total received likes — Firestore doesn't store this as a field
            userRepository.getUserPosts(userId).addOnSuccessListener(queryDocumentSnapshots -> {
                long totalLikes = 0;
                // Iterates through every post and sums up the likeCount field
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Long likes = document.getLong("likeCount");
                    if (likes != null) {
                        totalLikes += likes;
                    }
                }
                // Displays the total likes count
                likesCount.setText(String.valueOf(totalLikes));
                // Uses the actual Firestore post count here too since it's more accurate than the cached Firestore field
                postsCount.setText(String.valueOf(queryDocumentSnapshots.size()));
            });

        } else {
            // This shouldn't happen since ProfileActivity is only reachable when logged in, but handled defensively
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }
}
