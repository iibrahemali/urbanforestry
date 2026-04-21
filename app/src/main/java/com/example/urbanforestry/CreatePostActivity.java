package com.example.urbanforestry;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;

public class CreatePostActivity extends AppCompatActivity {
    private String photoPath;

    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    setResult(RESULT_OK, result.getData());
                    finish(); // pass back to FeedActivity
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Photo taken, ask if they want to share location
                    new AlertDialog.Builder(this)
                            .setTitle("Share Location")
                            .setMessage("Do you want to share the location of this image?")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                startPostImageActivity(true);
                            })
                            .setNegativeButton("No", (dialog, which) -> {
                                startPostImageActivity(false);
                            })
                            .setCancelable(false)
                            .show();
                }
            });

    private void startPostImageActivity(boolean shareLocation) {
        Intent intent = new Intent(this, PostImageActivity.class);
        intent.putExtra("imagePath", photoPath);
        intent.putExtra("shareLocation", shareLocation);
        launcher.launch(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set seasonal theme before onCreate
        setTheme(SeasonManager.getSeasonTheme(SeasonManager.getCurrentSeason()));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        findViewById(R.id.btn_camera).setOnClickListener(v -> {
            try {
                String fileName = "pic_" + System.currentTimeMillis();
                File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                File photoFile = File.createTempFile(fileName, ".jpg", storageDir);

                photoPath = photoFile.getAbsolutePath();
                Uri photoUri = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider", photoFile);
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                cameraLauncher.launch(cameraIntent);
            } catch (Exception e) {
                Log.d("URBAN FORESTRY", "Error creating file");
            }
        });

        findViewById(R.id.btn_text).setOnClickListener(v -> {
            launcher.launch(new Intent(this, PostTextActivity.class));
        });
    }
}
