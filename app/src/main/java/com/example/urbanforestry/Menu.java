package com.example.urbanforestry;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Random;

public class Menu extends AppCompatActivity {
    String[] gameList;
    int[] scoreList;
    Random rand;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_menu);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent i = getIntent();
        gameList = i.getStringArrayExtra("gameList");
        scoreList = i.getIntArrayExtra("scoreList");

        Button signOutButton = findViewById(R.id.signOutButton);
        signOutButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Sign Out")
                    .setMessage("Are you sure you want to sign out?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(this, WelcomePage.class));
                        finish();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        Button gameButton = findViewById(R.id.gameButton);
        gameButton.setOnClickListener(v -> {
            rand = new Random(System.nanoTime());
            TextView goal = findViewById(R.id.gameGoal);
            TextView goalProgress = findViewById(R.id.goalProgress);

            goal.setText(gameList[rand.nextInt(gameList.length)]);
            String goalP = ": 0/" + scoreList[rand.nextInt(gameList.length)];
            goalProgress.setText(goalP);
        });

        Button cameraButton = findViewById(R.id.cameraButton);
        cameraButton.setOnClickListener(v -> {
            Intent i2 = new Intent(getApplicationContext(), CameraActivity.class);
            startActivity(i2);
        });

        Button aboutButton = findViewById(R.id.aboutButton);
        aboutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i3 = new Intent(getApplicationContext(), AboutActivity.class);
                startActivity(i3);
            }
        });
    }
}