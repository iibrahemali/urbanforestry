package com.example.urbanforestry;

import static com.example.urbanforestry.HomePage.goalsProgress;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class Menu extends AppCompatActivity {
    String[] gameList;
    int[] scoreList;
    int[] currentGoals;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Use seasonal DIALOG theme instead of full-screen theme
        setTheme(SeasonManager.getSeasonDialogTheme(SeasonManager.getSeasonPref(this)));

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
            // Go back to the main activity to confirm
            setResult(-1);
            finish();
        });

        TextView goal1 = findViewById(R.id.gameGoal1);
        TextView goalProgress1 = findViewById(R.id.goalProgress1);
        TextView goal2 = findViewById(R.id.gameGoal2);
        TextView goalProgress2 = findViewById(R.id.goalProgress2);

        int currentGoal1 = currentGoals[0];
        goal1.setText(gameList[currentGoal1]);
        String goalP1 = ": " + goalsProgress[0] + "/" + scoreList[currentGoal1];
        goalProgress1.setText(goalP1);

        int currentGoal2 = currentGoals[1];
        goal2.setText(gameList[currentGoal2]);
        String goalP2 = ": " + goalsProgress[1] + "/" + scoreList[currentGoal2];
        goalProgress2.setText(goalP2);

        Button aboutButton = findViewById(R.id.aboutButton);
        aboutButton.setOnClickListener(v -> {
            Intent i3 = new Intent(getApplicationContext(), AboutActivity.class);
            startActivity(i3);
        });

        Button achievementButton = findViewById(R.id.achievementsButton);
        achievementButton.setOnClickListener(v -> {
            Intent trophyIntent = new Intent(getApplicationContext(), AchievementsActivity.class);
            startActivity(trophyIntent);
        });

        // Dropdown to choose seasonal theme
        SharedPreferences sp = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Default", "Summer", "Autumn", "Winter", "Spring"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner themeSpinner = findViewById(R.id.themeSpinner);
        themeSpinner.setAdapter(adapter);
        // Set the dropdown's value to the current theme preference
        String currentTheme = sp.getString("theme", "Default");
        themeSpinner.setSelection(adapter.getPosition(currentTheme));
        // When a new theme is selected, store it in SharedPreferences
        themeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String newTheme = parent.getItemAtPosition(position).toString();
                // If the theme is changed, save the new theme in SharedPreferences
                if (!newTheme.equals(currentTheme)) {
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("theme", newTheme);
                    editor.apply();
                    // Reload the menu with the new theme
                    recreate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
}
