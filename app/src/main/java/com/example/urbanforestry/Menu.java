package com.example.urbanforestry;

import static com.example.urbanforestry.HomePage.goalsProgress;

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
    int[] currentGoals;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Use seasonal DIALOG theme instead of full-screen theme
        setTheme(SeasonManager.getSeasonDialogTheme(SeasonManager.getCurrentSeason()));

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
        currentGoals = i.getIntArrayExtra("currentGoals");

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

        TextView goal1 = findViewById(R.id.gameGoal1);
        TextView goalProgress1 = findViewById(R.id.goalProgress1);
        TextView goal2 = findViewById(R.id.gameGoal2);
        TextView goalProgress2 = findViewById(R.id.goalProgress2);

        int currentGoal1 = currentGoals[0];
        goal1.setText(gameList[currentGoal1]);
        String goalP1 = ": " + String.valueOf(goalsProgress[0]) + "/" + scoreList[currentGoal1];
        goalProgress1.setText(goalP1);

        int currentGoal2 = currentGoals[1];
        goal2.setText(gameList[currentGoal2]);
        String goalP2 = ": " + String.valueOf(goalsProgress[1]) + "/" + scoreList[currentGoal2];
        goalProgress2.setText(goalP2);

        Button aboutButton = findViewById(R.id.aboutButton);
        aboutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i3 = new Intent(getApplicationContext(), AboutActivity.class);
                startActivity(i3);
            }
        });

        Button achievementButton = findViewById(R.id.achievementsButton);
        achievementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent trophyIntent = new Intent(getApplicationContext(), achievements.class);
                startActivity(trophyIntent);
            }
        });
    }
}
