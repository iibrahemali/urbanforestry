// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Declares Achievement as a public class — it's a plain data model representing a single achievement entry displayed in the Achievements screen
public class Achievement {
    // Stores the achievement's display name (e.g. "Oak Specialist") — shown as the title in the list item
    private String name;
    // Stores how many units of progress the user has made toward the next level — used to fill the progress bar
    private int currentProgress;
    // Stores the total number of units needed to complete one level — used as the denominator for the progress bar
    private int goalTarget;
    // Stores which level the user is currently at — calculated from how many times they've completed the goal
    private int level;

    // Constructor that initialises all fields — called by AchievementsActivity when building the achievement list
    public Achievement(String name, int currentProgress, int goalTarget, int level) {
        // Assigns the achievement name so the adapter can display it
        this.name = name;
        // Assigns the current progress value so the adapter can calculate the bar fill percentage
        this.currentProgress = currentProgress;
        // Assigns the goal target so the adapter knows what 100% looks like
        this.goalTarget = goalTarget;
        // Assigns the level so the adapter can display it alongside the progress text
        this.level = level;
    }

    // Getter for name — used by AchievementAdapter to populate the name TextView in each list item
    public String getName() {
        return name;
    }

    // Getter for currentProgress — used by AchievementAdapter to calculate and display the progress bar fill
    public int getCurrentProgress() {
        return currentProgress;
    }

    // Getter for goalTarget — used by AchievementAdapter to know the denominator of the progress fraction
    public int getGoalTarget() {
        return goalTarget;
    }

    // Getter for level — used by AchievementAdapter to display the current level label next to the progress bar
    public int getLevel() {
        return level;
    }
}