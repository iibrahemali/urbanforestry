// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports Context to access SharedPreferences for reading and writing the theme preference
import android.content.Context;
// Imports Intent for navigating to other Activities from the menu
import android.content.Intent;
// Imports SharedPreferences to persist the user's chosen season theme across sessions
import android.content.SharedPreferences;
// Imports Color to set a specific background hex color for the Spring season
import android.graphics.Color;
// Imports Bundle, the key-value container Android passes to onCreate with any saved state
import android.os.Bundle;
// Imports View for working with layout containers in the window insets listener
import android.view.View;
// Imports AdapterView to handle item selection events from the Spinner dropdown
import android.widget.AdapterView;
// Imports ArrayAdapter to supply the theme Spinner with its list of options
import android.widget.ArrayAdapter;
// Imports Button for the Sign Out, About, and Achievements buttons
import android.widget.Button;
// Imports ImageView to display the seasonal horizontal logo
import android.widget.ImageView;
// Imports Spinner, the dropdown widget used to pick the seasonal theme
import android.widget.Spinner;
// Imports TextView to display the current goal names and their progress
import android.widget.TextView;

// Imports EdgeToEdge to render the app behind system bars for a full-screen look
import androidx.activity.EdgeToEdge;
// Imports AppCompatActivity, the base class that provides backwards-compatible Activity features
import androidx.appcompat.app.AppCompatActivity;
// Imports Insets and related classes to apply correct padding around system bars
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Declares MenuActivity as the app's side/overlay menu screen, launched from the main map
public class MenuActivity extends AppCompatActivity {

    // Overrides onCreate, called when this Activity is first created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Uses the dialog-style seasonal theme so the menu appears as a floating overlay rather than a full screen
        setTheme(SeasonManager.getSeasonDialogTheme(SeasonManager.getSeasonPref(this)));

        // Calls the parent class onCreate to complete Android's standard Activity setup
        super.onCreate(savedInstanceState);
        // Enables edge-to-edge rendering so the content draws behind system bars
        EdgeToEdge.enable(this);
        // Inflates activity_menu.xml and sets it as this screen's UI
        setContentView(R.layout.activity_menu);
        // Applies system bar insets as padding so content isn't hidden behind the status bar or nav bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Sets a specific pink background for Spring because the Spring logo has a transparent background that needs a matching color
        if (SeasonManager.getSeasonPref(this) == SeasonManager.Season.SPRING)
            findViewById(R.id.main).setBackgroundColor(Color.parseColor("#DA8E92"));

        // Finds the logo ImageView and sets the horizontal seasonal logo so it fits the wide menu layout
        ImageView logo = findViewById(R.id.logo);
        logo.setImageResource(SeasonManager.getSeasonLogoHorizontal(SeasonManager.getSeasonPref(this)));

        // Finds the Sign Out button and attaches a click listener
        Button signOutButton = findViewById(R.id.signOutButton);
        signOutButton.setOnClickListener(v -> {
            // Returns result code -1 to MainActivity, which intercepts it and shows a confirmation dialog before actually signing out
            setResult(-1);
            finish();
        });

        // Finds the four TextViews that display the two active goals and their progress fractions
        TextView goal1 = findViewById(R.id.gameGoal1);
        TextView goalProgress1 = findViewById(R.id.goalProgress1);
        TextView goal2 = findViewById(R.id.gameGoal2);
        TextView goalProgress2 = findViewById(R.id.goalProgress2);

        // Reads the first active goal index from the global Missions state
        int currentGoal1 = Missions.currentGoals[0];
        // Sets the goal 1 label using the goal's description string from the gameList array
        goal1.setText(Missions.gameList[currentGoal1] + ":");
        // Builds the progress fraction string (e.g. "3/8") for goal 1
        String goalP1 = Missions.goalsProgress[0] + "/" + Missions.scoreList[currentGoal1];
        goalProgress1.setText(goalP1);

        // Reads the second active goal index
        int currentGoal2 = Missions.currentGoals[1];
        // Sets the goal 2 label
        goal2.setText(Missions.gameList[currentGoal2] + ":");
        // Builds the progress fraction string for goal 2
        String goalP2 = Missions.goalsProgress[1] + "/" + Missions.scoreList[currentGoal2];
        goalProgress2.setText(goalP2);

        // Finds the About button and launches AboutActivity when tapped
        Button aboutButton = findViewById(R.id.aboutButton);
        aboutButton.setOnClickListener(v -> {
            Intent i3 = new Intent(getApplicationContext(), AboutActivity.class);
            startActivity(i3);
        });

        // Finds the Achievements button and launches AchievementsActivity when tapped
        Button achievementButton = findViewById(R.id.achievementsButton);
        achievementButton.setOnClickListener(v -> {
            Intent trophyIntent = new Intent(getApplicationContext(), AchievementsActivity.class);
            startActivity(trophyIntent);
        });

        // Opens the SharedPreferences file to read and write the user's chosen theme
        SharedPreferences sp = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        // Creates an ArrayAdapter supplying the Spinner with the list of available season options
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Default", "Summer", "Autumn", "Winter", "Spring"}
        );
        // Sets the standard dropdown layout for the expanded Spinner options
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Finds the theme Spinner and attaches the adapter
        Spinner themeSpinner = findViewById(R.id.themeSpinner);
        themeSpinner.setAdapter(adapter);
        // Reads the currently saved theme preference so the Spinner starts on the right option
        String currentTheme = sp.getString("theme", "Default");
        // Sets the Spinner's selected position to match the saved theme — avoids showing a wrong default
        themeSpinner.setSelection(adapter.getPosition(currentTheme));
        // Attaches a listener so any new selection is saved and the menu is recreated with the new theme
        themeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            // Called whenever the user picks a new item in the Spinner dropdown
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Gets the name of the newly selected theme
                String newTheme = parent.getItemAtPosition(position).toString();
                // Only saves and recreates if the theme actually changed — prevents unnecessary recreates on initial load
                if (!newTheme.equals(currentTheme)) {
                    SharedPreferences.Editor editor = sp.edit();
                    // Writes the new theme to SharedPreferences so it persists across sessions
                    editor.putString("theme", newTheme);
                    editor.apply();
                    // Recreates the Activity so the new theme is applied immediately
                    recreate();
                }
            }

            // Not used — no action needed when nothing is selected
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
}
