package com.example.urbanforestry;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AchievementsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);

        // 1. Initialize the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.achievementRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 2. Load the lifetime totals from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserStats", MODE_PRIVATE);

        int kilometersWalked = (int)prefs.getFloat("total_meters_walked", 0  );
        int nonNativeGoals = prefs.getInt("goal_count_1", 0);
        int oakGoals = prefs.getInt("goal_count_2", 0);
        int mapleGoals = prefs.getInt("goal_count_3", 0);
        int spruceGoals = prefs.getInt("goal_count_4", 0);
        int fallRedGoals = prefs.getInt("goal_count_5", 0);

        // 3. Create the list of Achievement objects
        List<Achievement> achievementList = new ArrayList<>();

        achievementList.add(calculateProgress("Urban Hiker", kilometersWalked, 1000));
        achievementList.add(calculateProgress("Invasive Hunter", nonNativeGoals, 6));
        achievementList.add(calculateProgress("Oak Specialist", oakGoals, 5));
        achievementList.add(calculateProgress("Maple Master", mapleGoals, 4));
        achievementList.add(calculateProgress("Spruce Spring(steen)", spruceGoals, 3));
        achievementList.add(calculateProgress("Red Rider", fallRedGoals, 4));

        // 4. Set the Adapter
        AchievementAdapter adapter = new AchievementAdapter(achievementList);
        recyclerView.setAdapter(adapter);

        // 5. Back Button Logic
        Button btnBack = findViewById(R.id.backButton);
        btnBack.setOnClickListener(v -> finish());
    }

    /**
     * Logic helper to turn a raw total into Level and Progress
     * Example: If total is 7 and threshold is 5:
     * Level = (7 / 5) + 1 = 2
     * CurrentProgress = 7 % 5 = 2
     * Result: Level 2, 2/5 progress
     */
    private Achievement calculateProgress(String name, int totalGoalsFinished, int threshold) {
        int level = (totalGoalsFinished / threshold) + 1;
        int progressTowardNextLevel = totalGoalsFinished % threshold;

        return new Achievement(name, progressTowardNextLevel, threshold, level);
    }
}