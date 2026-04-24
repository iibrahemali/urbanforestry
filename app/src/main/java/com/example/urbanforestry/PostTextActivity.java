// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports Manifest to reference the ACCESS_FINE_LOCATION permission constant
import android.Manifest;
// Imports Intent for navigating back to FeedActivity after posting
import android.content.Intent;
// Imports PackageManager to check whether the location permission has been granted
import android.content.pm.PackageManager;
// Imports Location (used implicitly via the FusedLocationProvider callback)
import android.location.Location;
// Imports Bundle, the key-value container Android passes to onCreate with any saved state
import android.os.Bundle;
// Imports Button for the Post button
import android.widget.Button;
// Imports EditText for the text input field where the user types their post
import android.widget.EditText;
// Imports Toast to show brief feedback messages to the user
import android.widget.Toast;

// Imports AppCompatActivity, the base class that provides backwards-compatible Activity features
import androidx.appcompat.app.AppCompatActivity;
// Imports ActivityCompat to check runtime permissions in a backwards-compatible way
import androidx.core.app.ActivityCompat;

// Imports FusedLocationProviderClient to get the device's last known GPS location
import com.google.android.gms.location.FusedLocationProviderClient;
// Imports LocationServices to obtain the FusedLocationProviderClient instance
import com.google.android.gms.location.LocationServices;

// Declares PostTextActivity as the screen where a user types and submits a text-only post
public class PostTextActivity extends AppCompatActivity {

    // Declares the text input field where the user writes their post content
    private EditText editText;
    // Declares the submit button that triggers the post creation
    private Button postBtn;
    // Declares the repository that handles all Firestore post creation operations
    private PostRepository postRepository;
    // Declares the location client used to attach the user's GPS coordinates to the post if permitted
    private FusedLocationProviderClient fusedLocationClient;

    // Overrides onCreate, called when this Activity is first created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Applies the seasonal theme before layout inflation
        setTheme(SeasonManager.getSeasonTheme(SeasonManager.getSeasonPref(this)));

        // Calls the parent class onCreate to complete Android's standard Activity setup
        super.onCreate(savedInstanceState);
        // Inflates activity_post_text.xml and sets it as this screen's UI
        setContentView(R.layout.activity_post_text);

        // Instantiates the repository that will handle writing the post to Firestore
        postRepository = new PostRepository();
        // Gets the FusedLocationProviderClient which uses GPS, WiFi, and cell towers to determine location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Links the EditText and Button variables to their corresponding Views in the XML layout
        editText = findViewById(R.id.editText);
        postBtn = findViewById(R.id.btn_post);

        // Attaches a click listener to the post button using a lambda
        postBtn.setOnClickListener(v -> {
            // Reads and trims the post text to prevent empty or whitespace-only posts from being submitted
            String caption = editText.getText().toString().trim();
            if (caption.isEmpty()) {
                Toast.makeText(this, "Please write something", Toast.LENGTH_SHORT).show();
                return;
            }

            // Disables the button and updates its label to prevent the user from tapping it multiple times while the upload runs
            postBtn.setEnabled(false);
            postBtn.setText("Posting...");

            // Checks if the app has been granted the fine location permission before trying to read GPS coordinates
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Posts without a location if permission was denied — we don't want to block posting just because location isn't available
                submitPost(caption, null, 0);
                return;
            }

            // Requests the last known device location asynchronously — uses cached location so it's fast and battery-friendly
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    // Attaches the GPS coordinates to the post so it can appear as a blue dot on the map
                    submitPost(caption, location.getLatitude(), location.getLongitude());
                } else {
                    // Falls back to posting without location if no cached location is available
                    submitPost(caption, null, 0);
                }
            }).addOnFailureListener(e -> {
                // Falls back to posting without location if the location request itself fails
                submitPost(caption, null, 0);
            });
        });
    }

    // Submits the post to Firestore with the provided caption and optional location coordinates
    private void submitPost(String caption, Double lat, double lng) {
        // Calls the repository to create a text post — the repository handles username lookup and Firestore writes
        postRepository.createPost(caption, lat, lng)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Post created!", Toast.LENGTH_SHORT).show();
                    // Navigates to FeedActivity using FLAG_ACTIVITY_CLEAR_TOP so the existing Feed instance is brought forward rather than a new one created
                    Intent i = new Intent(PostTextActivity.this, FeedActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                    // Closes PostTextActivity so the user can't navigate back to the composer after posting
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Re-enables the button so the user can try again if the post fails
                    postBtn.setEnabled(true);
                    postBtn.setText("Post");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
