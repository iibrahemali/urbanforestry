package com.example.urbanforestry;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class CreatePostActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> launcher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            setResult(RESULT_OK, result.getData());
                            finish(); // pass back to FeedActivity
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        findViewById(R.id.btn_camera).setOnClickListener(v -> {
            launcher.launch(new Intent(this, CameraActivity.class));
        });

        findViewById(R.id.btn_text).setOnClickListener(v -> {
            launcher.launch(new Intent(this, PostTextActivity.class));
        });
    }
}