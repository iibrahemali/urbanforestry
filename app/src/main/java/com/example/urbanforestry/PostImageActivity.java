// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports Manifest to reference the ACCESS_FINE_LOCATION permission constant
import android.Manifest;
// Imports Intent for navigating back to FeedActivity after a successful post upload
import android.content.Intent;
// Imports PackageManager to check whether the location permission has been granted
import android.content.pm.PackageManager;
// Imports Bitmap and BitmapFactory (unused here but kept for consistency with the broader codebase)
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
// Imports Uri to create a file URI for the image so it can be passed to Firebase Storage
import android.net.Uri;
// Imports Bundle, the key-value container Android passes to onCreate with any saved state
import android.os.Bundle;
// Imports Button for the Post button
import android.widget.Button;
// Imports EditText for the caption input field
import android.widget.EditText;
// Imports ImageView to preview the photo before it is posted
import android.widget.ImageView;
// Imports Toast to show brief status or error messages
import android.widget.Toast;

// Imports AppCompatActivity, the base class that provides backwards-compatible Activity features
import androidx.appcompat.app.AppCompatActivity;
// Imports ActivityCompat to check runtime permissions in a backwards-compatible way
import androidx.core.app.ActivityCompat;

// Imports Glide to efficiently load and display the local image file in the preview ImageView
import com.bumptech.glide.Glide;
// Imports FusedLocationProviderClient to get the device's last known GPS coordinates
import com.google.android.gms.location.FusedLocationProviderClient;
// Imports LocationServices to obtain the FusedLocationProviderClient instance
import com.google.android.gms.location.LocationServices;

// Imports File to wrap the local file path into a File object for URI conversion
import java.io.File;

// Declares PostImageActivity as the screen where the user previews a captured photo, adds a caption, and submits it as a post
public class PostImageActivity extends AppCompatActivity {

    // Stores the absolute file path of the captured photo — passed in via the Intent from CreatePostActivity
    private String imagePath;
    // Declares the caption input field so the user can describe their photo
    private EditText captionEditText;
    // Declares the Post button that submits the image and caption
    private Button postBtn;
    // Declares the repository that handles uploading the image to Firebase Storage and creating the post in Firestore
    private PostRepository postRepository;
    // Declares the location client used to attach GPS coordinates to the post if the user consented
    private FusedLocationProviderClient fusedLocationClient;

    // Overrides onCreate, called when this Activity is first created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Applies the seasonal theme before layout inflation
        setTheme(SeasonManager.getSeasonTheme(SeasonManager.getSeasonPref(this)));

        // Calls the parent class onCreate to complete Android's standard Activity setup
        super.onCreate(savedInstanceState);
        // Inflates activity_post_image.xml and sets it as this screen's UI
        setContentView(R.layout.activity_post_image);

        // Instantiates the repository responsible for the upload and post creation logic
        postRepository = new PostRepository();
        // Gets the FusedLocationProviderClient which uses GPS, WiFi, and cell towers to determine location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Reads the image file path passed in from CreatePostActivity via the Intent
        imagePath = getIntent().getStringExtra("imagePath");
        // Reads whether the user consented to share their location from the Intent
        boolean shareLocation = getIntent().getBooleanExtra("shareLocation", false);

        // If no image path was provided, there's nothing to post — show an error and close this screen
        if (imagePath == null) {
            Toast.makeText(this, "Error loading image. Please try again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Links the UI widgets to their corresponding Views in the XML layout
        ImageView imagePreview = findViewById(R.id.imagePreview);
        captionEditText = findViewById(R.id.captionEditText);
        postBtn = findViewById(R.id.btn_post);

        // Loads and displays the local image file into the preview ImageView using Glide
        if (imagePath != null)
            Glide.with(this).load(imagePath).into(imagePreview);

        // Attaches a click listener to the Post button
        postBtn.setOnClickListener(v -> {
            // Reads and trims the caption — an empty caption is allowed for image posts
            String caption = captionEditText.getText().toString().trim();
            if (imagePath == null) {
                Toast.makeText(this, "No image to post", Toast.LENGTH_SHORT).show();
                return;
            }

            // Disables the button and updates the label to prevent duplicate submissions while uploading
            postBtn.setEnabled(false);
            postBtn.setText("Posting...");

            // If the user explicitly chose NOT to share location, post without it immediately
            if (!shareLocation) {
                submitPost(caption, null, null);
                return;
            }

            // Checks location permission before attempting to read GPS coordinates
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Posts without location if permission was denied — location is optional, not required
                submitPost(caption, null, null);
                return;
            }

            // Requests the last known GPS location asynchronously — uses cached location to avoid battery drain
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    // Attaches GPS coordinates so the post can appear as a blue dot on the map
                    submitPost(caption, location.getLatitude(), location.getLongitude());
                } else {
                    // Falls back to posting without location if no cached GPS fix is available
                    submitPost(caption, null, null);
                }
            }).addOnFailureListener(e -> {
                // Falls back to posting without location if the location request itself fails
                submitPost(caption, null, null);
            });
        });
    }

    // Uploads the image to Firebase Storage and creates the post document in Firestore
    private void submitPost(String caption, Double lat, Double lon) {
        // Converts the local file path string into a Uri, which Firebase Storage's putFile() requires
        Uri imageUri = Uri.fromFile(new File(imagePath));
        // Calls the repository to handle the full upload-then-post creation pipeline
        postRepository.createImagePost(caption, imageUri, lat, lon)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Post uploaded!", Toast.LENGTH_SHORT).show();
                    // Signals FeedActivity that a new post was created so it can refresh
                    setResult(RESULT_OK);
                    // Navigates back to the Feed using FLAG_ACTIVITY_CLEAR_TOP to avoid stacking duplicate Feed instances
                    Intent i = new Intent(PostImageActivity.this, FeedActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                    // Closes this Activity so the user lands directly on the updated feed
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Re-enables the Post button so the user can retry after an upload failure
                    postBtn.setEnabled(true);
                    postBtn.setText("Post");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
