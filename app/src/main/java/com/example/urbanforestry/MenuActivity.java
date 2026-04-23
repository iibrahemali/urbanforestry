package com.example.urbanforestry;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MenuActivity extends AppCompatActivity {

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

        // Use lighter background for spring to match logo background
        if (SeasonManager.getSeasonPref(this) == SeasonManager.Season.SPRING)
            findViewById(R.id.main).setBackgroundColor(Color.parseColor("#DA8E92"));

        ImageView logo = findViewById(R.id.logo);
        logo.setImageResource(SeasonManager.getSeasonLogoHorizontal(SeasonManager.getSeasonPref(this)));

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

        int currentGoal1 = Missions.currentGoals[0];
        goal1.setText(Missions.gameList[currentGoal1] + ":");
        String goalP1 = Missions.goalsProgress[0] + "/" + Missions.scoreList[currentGoal1];
        goalProgress1.setText(goalP1);

        int currentGoal2 = Missions.currentGoals[1];
        goal2.setText(Missions.gameList[currentGoal2] + ":");
        String goalP2 = Missions.goalsProgress[1] + "/" + Missions.scoreList[currentGoal2];
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
