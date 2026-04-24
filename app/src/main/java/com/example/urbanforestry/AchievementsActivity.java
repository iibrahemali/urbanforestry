// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports SharedPreferences to read the lifetime user statistics stored locally on the device
import android.content.SharedPreferences;
// Imports Bundle, the key-value container Android passes to onCreate with any saved state
import android.os.Bundle;
// Imports Button for the back button
import android.widget.Button;

// Imports AppCompatActivity, the base class that provides backwards-compatible Activity features
import androidx.appcompat.app.AppCompatActivity;
// Imports LinearLayoutManager to arrange the achievement list items vertically in the RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager;
// Imports RecyclerView to display the scrollable list of achievements
import androidx.recyclerview.widget.RecyclerView;

// Imports ArrayList and List to build the collection of Achievement objects before passing them to the adapter
import java.util.ArrayList;
import java.util.List;

// Declares AchievementsActivity as the screen showing the user's progress across all achievement categories
public class AchievementsActivity extends AppCompatActivity {

    // Overrides onCreate, called when this Activity is first created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Applies the seasonal theme before layout inflation
        setTheme(SeasonManager.getSeasonTheme(SeasonManager.getSeasonPref(this)));

        // Calls the parent class onCreate to complete Android's standard Activity setup
        super.onCreate(savedInstanceState);
        // Inflates activity_achievements.xml and sets it as this screen's UI
        setContentView(R.layout.activity_achievements);

        // Finds the RecyclerView and sets a vertical LinearLayoutManager so achievements stack top-to-bottom
        RecyclerView recyclerView = findViewById(R.id.achievementRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Opens the "UserStats" SharedPreferences file that stores all persistent lifetime statistics
        SharedPreferences prefs = getSharedPreferences("UserStats", MODE_PRIVATE);

        // Reads the total meters walked, cast to int for the achievement calculation — default is 0 if never set
        int kilometersWalked = (int) prefs.getFloat("total_meters_walked", 0);
        // Reads the number of times the user has completed each goal type — keys match those written in Missions.java
        int nonNativeGoals = prefs.getInt("goal_count_1", 0);
        int oakGoals = prefs.getInt("goal_count_2", 0);
        int mapleGoals = prefs.getInt("goal_count_3", 0);
        int spruceGoals = prefs.getInt("goal_count_4", 0);
        int fallRedGoals = prefs.getInt("goal_count_5", 0);

        // Builds the list of Achievement objects — each one is calculated from the raw lifetime total and a threshold
        List<Achievement> achievementList = new ArrayList<>();

        // Each calculateProgress call converts a raw count into a Level + progress-within-level representation
        achievementList.add(calculateProgress("Urban Hiker", kilometersWalked, 1000));
        achievementList.add(calculateProgress("Invasive Hunter", nonNativeGoals, 6));
        achievementList.add(calculateProgress("Oak Specialist", oakGoals, 5));
        achievementList.add(calculateProgress("Maple Master", mapleGoals, 4));
        achievementList.add(calculateProgress("Spruce Spring(steen)", spruceGoals, 3));
        achievementList.add(calculateProgress("Red Rider", fallRedGoals, 4));

        // Creates the adapter with the achievement list and sets it on the RecyclerView so items are rendered
        AchievementAdapter adapter = new AchievementAdapter(achievementList);
        recyclerView.setAdapter(adapter);

        // Finds the back button and closes this screen when tapped, returning to the previous Activity
        Button btnBack = findViewById(R.id.backButton);
        btnBack.setOnClickListener(v -> finish());
    }

    // Converts a raw lifetime total into a Level and progress-toward-next-level for display in the achievement card
    // Example: total=7, threshold=5 → Level 2, progress 2/5 (because the user completed one full cycle and is 2 into the next)
    private Achievement calculateProgress(String name, int totalGoalsFinished, int threshold) {
        // Integer division gives the number of complete cycles — add 1 so the first incomplete cycle is Level 1
        int level = (totalGoalsFinished / threshold) + 1;
        // Modulo gives the remainder progress within the current level cycle
        int progressTowardNextLevel = totalGoalsFinished % threshold;

        // Returns a new Achievement object with all calculated values ready for the adapter to display
        return new Achievement(name, progressTowardNextLevel, threshold, level);
    }
}
