// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports Intent for launching the camera app and navigating to PostImageActivity and PostTextActivity
import android.content.Intent;
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

// Imports ActivityResultLauncher to handle the result from the camera or sub-Activity in a modern, type-safe way
import androidx.activity.result.ActivityResultLauncher;
// Imports StartActivityForResult contract to launch Activities and receive their results
import androidx.activity.result.contract.ActivityResultContracts;
// Imports AlertDialog to ask the user whether to share their location after taking a photo
import androidx.appcompat.app.AlertDialog;
// Imports AppCompatActivity, the base class that provides backwards-compatible Activity features
import androidx.appcompat.app.AppCompatActivity;
// Imports FileProvider to create a content URI for the photo file, required by the camera on Android 7+
import androidx.core.content.FileProvider;

// Imports File to create a temporary image file on disk before handing it to the camera
import java.io.File;

// Declares CreatePostActivity as the chooser screen where users decide between a photo post or a text post
public class CreatePostActivity extends AppCompatActivity {
    // Stores the absolute path of the temporary photo file created for the camera — persisted so it survives rotation
    private String photoPath;

    // Registers a launcher for PostImageActivity or PostTextActivity results — passes the result back up to FeedActivity
    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Propagates the RESULT_OK back to FeedActivity so it knows a new post was created
                    setResult(RESULT_OK, result.getData());
                    // Closes CreatePostActivity since the post flow is complete
                    finish();
                }
            });

    // Registers a separate launcher for the camera intent so we can intercept the result and ask about location sharing
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // After a photo is taken, ask the user whether they want to attach their GPS location to the post
                    new AlertDialog.Builder(this)
                            .setMessage("Do you want to share the location of this image?")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                // Passes shareLocation=true to PostImageActivity so it will read GPS coordinates
                                startPostImageActivity(true);
                            })
                            .setNegativeButton("No", (dialog, which) -> {
                                // Passes shareLocation=false so PostImageActivity skips the GPS read
                                startPostImageActivity(false);
                            })
                            // Prevents dismissing the dialog by tapping outside — the user must make a deliberate choice
                            .setCancelable(false)
                            .show();
                }
            });

    // Launches PostImageActivity with the captured photo path and the user's location sharing preference
    private void startPostImageActivity(boolean shareLocation) {
        Intent intent = new Intent(this, PostImageActivity.class);
        // Passes the file path so PostImageActivity can load and upload the correct photo
        intent.putExtra("imagePath", photoPath);
        // Passes the location consent flag so PostImageActivity knows whether to request GPS coordinates
        intent.putExtra("shareLocation", shareLocation);
        // Uses the main launcher (not cameraLauncher) so the result propagates correctly back to FeedActivity
        launcher.launch(intent);
    }

    // Overrides onCreate, called when this Activity is first created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Applies the seasonal theme before layout inflation
        setTheme(SeasonManager.getSeasonTheme(SeasonManager.getSeasonPref(this)));

        // Calls the parent class onCreate to complete Android's standard Activity setup
        super.onCreate(savedInstanceState);
        // Inflates activity_create_post.xml and sets it as this screen's UI
        setContentView(R.layout.activity_create_post);

        // Restores the photoPath if the Activity was recreated (e.g., on screen rotation) so the captured photo isn't lost
        if (savedInstanceState != null)
            photoPath = savedInstanceState.getString("photoPath");

        // Attaches a click listener to the Camera button using a lambda
        findViewById(R.id.btn_camera).setOnClickListener(v -> {
            try {
                // Generates a unique filename using the current timestamp to avoid collisions
                String fileName = "pic_" + System.currentTimeMillis();
                // Gets the app's external Pictures directory as the save location for the photo
                File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                // Creates a temporary .jpg file that the camera will write into
                File photoFile = File.createTempFile(fileName, ".jpg", storageDir);

                // Stores the absolute path so we can access the file after the camera returns
                photoPath = photoFile.getAbsolutePath();
                // Converts the file path to a content URI using FileProvider — required on Android 7+ for security
                Uri photoUri = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider", photoFile);
                // Creates the camera intent and tells it exactly where to save the captured photo
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                // Launches the camera app and waits for the result via cameraLauncher
                cameraLauncher.launch(cameraIntent);
            } catch (Exception e) {
                Log.d("URBAN FORESTRY", "Error creating file");
            }
        });

        // Launches PostTextActivity directly when the user taps the Text post button — no file creation needed
        findViewById(R.id.btn_text).setOnClickListener(v -> {
            launcher.launch(new Intent(this, PostTextActivity.class));
        });
    }

    // Saves the photoPath to the outgoing Bundle so it survives configuration changes (e.g., screen rotation)
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Stores photoPath so onCreate can restore it if the Activity is recreated mid-flow
        outState.putString("photoPath", photoPath);
    }
}
