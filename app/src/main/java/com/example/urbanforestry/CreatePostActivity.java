// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports Manifest to reference the CAMERA and location permission constants
import android.Manifest;
// Imports Intent for launching the camera app and navigating to PostImageActivity and PostTextActivity
import android.content.Intent;
// Imports PackageManager to check whether permissions have been granted
import android.content.pm.PackageManager;
// Imports Uri to pass the output file location to the camera via MediaStore
import android.net.Uri;
// Imports Bundle, the key-value container Android passes to onCreate with any saved state
import android.os.Bundle;
// Imports Environment to get the standard Pictures directory on external storage
import android.os.Environment;
// Imports MediaStore to use the standard camera capture action
import android.provider.MediaStore;
// Imports Log for debugging file creation errors
import android.util.Log;
// Imports Toast for brief feedback messages
import android.widget.Toast;

// Imports ActivityResultLauncher to handle the result from the camera or sub-Activity in a modern, type-safe way
import androidx.activity.result.ActivityResultLauncher;
// Imports StartActivityForResult contract to launch Activities and receive their results
import androidx.activity.result.contract.ActivityResultContracts;
// Imports AlertDialog to ask the user whether to share their location after taking a photo
import androidx.appcompat.app.AlertDialog;
// Imports AppCompatActivity, the base class that provides backwards-compatible Activity features
import androidx.appcompat.app.AppCompatActivity;
// Imports ActivityCompat to check and request runtime permissions
import androidx.core.app.ActivityCompat;
// Imports ContextCompat to safely check self permissions
import androidx.core.content.ContextCompat;
// Imports FileProvider to create a content URI for the photo file, required by the camera on Android 7+
import androidx.core.content.FileProvider;

// Imports File to create a temporary image file on disk before handing it to the camera
import java.io.File;

/**
 * CreatePostActivity serves as the entry point for creating new community content.
 * It manages the camera workflow, runtime permissions, and user consent for location sharing.
 */
public class CreatePostActivity extends AppCompatActivity {
    // Stores the absolute path of the temporary photo file created for the camera
    private String photoPath;

    // Launcher for finalizing the post (Image or Text) - returns result to FeedActivity
    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    setResult(RESULT_OK, result.getData());
                    finish();
                }
            });

    // Launcher for the system camera app
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // After photo capture, request location consent via dialog
                    showLocationConsentDialog();
                }
            });

    // Launcher for requesting the CAMERA permission at runtime
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    // If user grants permission, proceed with the camera launch
                    launchCamera();
                } else {
                    // If user denies, inform them why the feature won't work
                    Toast.makeText(this, "Camera permission is required to take photos.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply seasonal theme before layout inflation
        setTheme(SeasonManager.getSeasonTheme(SeasonManager.getSeasonPref(this)));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        // Restore photoPath state if activity was recreated (e.g., rotation)
        if (savedInstanceState != null)
            photoPath = savedInstanceState.getString("photoPath");

        // UI Listener: "Take a picture"
        findViewById(R.id.btn_camera).setOnClickListener(v -> {
            // Check if app already has CAMERA permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                // Request permission if not yet granted (prevents the SecurityException crash)
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        // UI Listener: "Write a text post"
        findViewById(R.id.btn_text).setOnClickListener(v -> {
            launcher.launch(new Intent(this, PostTextActivity.class));
        });
    }

    /**
     * launchCamera: Prepares the output file and triggers the system camera app.
     */
    private void launchCamera() {
        try {
            // Create a unique temporary file to store the high-res camera output
            String fileName = "pic_" + System.currentTimeMillis();
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File photoFile = File.createTempFile(fileName, ".jpg", storageDir);

            photoPath = photoFile.getAbsolutePath();
            
            // Create a content URI via FileProvider for secure inter-app file sharing
            Uri photoUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".fileprovider", photoFile);
            
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            cameraLauncher.launch(cameraIntent);
        } catch (Exception e) {
            Log.e("URBAN FORESTRY", "Error preparing camera file", e);
            Toast.makeText(this, "Could not initialize camera storage.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * showLocationConsentDialog: Prompts the user to decide if their current GPS
     * coordinates should be attached to the new post marker on the map.
     */
    private void showLocationConsentDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Share Location?")
                .setMessage("Do you want to pin this photo to your current location on the map?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    startPostImageActivity(true);
                })
                .setNegativeButton("No", (dialog, which) -> {
                    startPostImageActivity(false);
                })
                .setCancelable(false)
                .show();
    }

    private void startPostImageActivity(boolean shareLocation) {
        Intent intent = new Intent(this, PostImageActivity.class);
        intent.putExtra("imagePath", photoPath);
        intent.putExtra("shareLocation", shareLocation);
        launcher.launch(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("photoPath", photoPath);
    }
}
