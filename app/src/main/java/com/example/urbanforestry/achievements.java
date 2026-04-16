package com.example.urbanforestry;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class achievements extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);

        // 1. Initialize the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.achievementRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 2. Load the lifetime totals from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserStats", MODE_PRIVATE);

        // These keys ("goal_count_X") must match exactly what you wrote in HomePage
        int nonNativeGoals = prefs.getInt("goal_count_1", 0);
        int oakGoals = prefs.getInt("goal_count_2", 0);
        int mapleGoals = prefs.getInt("goal_count_3", 0);

        // 3. Create the list of Achievement objects
        List<achievement> achievementList = new ArrayList<>();

        // We use a threshold of 5 (5 goals to level up).
        // You can change this number to make leveling harder or easier.
        achievementList.add(calculateProgress("Invasive Hunter", nonNativeGoals, 5));
        achievementList.add(calculateProgress("Oak Specialist", oakGoals, 5));
        achievementList.add(calculateProgress("Maple Master", mapleGoals, 5));

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
    private achievement calculateProgress(String name, int totalGoalsFinished, int threshold) {
        int level = (totalGoalsFinished / threshold) + 1;
        int progressTowardNextLevel = totalGoalsFinished % threshold;

        return new achievement(name, progressTowardNextLevel, threshold, level);
    }
    }
}