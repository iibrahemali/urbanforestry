// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports Context so Missions can show Toasts and access SharedPreferences without needing an Activity reference
import android.content.Context;
// Imports Toast to display brief in-app notifications when the user makes progress or completes a goal
import android.widget.Toast;

// Imports ArrayList to initialise the visitedTreesBySlot list
import java.util.ArrayList;
// Imports HashSet to track which trees the user has already visited per goal slot — HashSet prevents duplicates automatically
import java.util.HashSet;
// Imports List as the interface type for visitedTreesBySlot, keeping it flexible
import java.util.List;
// Imports Random for picking a new goal randomly when a slot is emptied
import java.util.Random;

// Declares Missions as a utility class with only static state and methods — no instance needed since mission state is global for the session
public class Missions {
    // Defines the list of available goal descriptions; index 0 is "N/A" as a placeholder for an inactive slot
    public final static String[] gameList = {"N/A", "Find non-native tree species", "Find Oak Trees", "Find Maple Trees", "Find Spruce Trees",
            "Find Trees that are Red in the Fall"};
    // Defines the number of unique trees required to complete each corresponding goal — aligned by index with gameList
    public final static int[] scoreList = {0, 6, 8, 4, 4, 4};
    // Tracks the currently active goal for each slot — index 0 and 1 represent the two simultaneous goal slots
    public final static int[] currentGoals = {0, 0};
    // Tracks how many qualifying trees the user has found for each active goal slot
    public final static int[] goalsProgress = {0, 0};
    // Stores the set of tree IDs already counted for each goal slot — prevents the same tree from counting twice toward the same goal
    public final static List<HashSet<String>> visitedTreesBySlot = new ArrayList<>();

    // Called when the user taps a tree marker on the map — updates progress for any active goal the tree satisfies
    public static void updateGoalProgress(String treeId, String[] treeData, Context ctx) {
        // Tracks whether any goal slot made progress, so we know whether to call updateGoals at the end
        boolean progressMade = false;

        // Iterates through all active goal slots so this works even if more slots are added later
        for (int i = 0; i < currentGoals.length; i++) {
            // Gets the set of already-visited tree IDs for this specific goal slot
            HashSet<String> visitedForThisSlot = visitedTreesBySlot.get(i);
            // Gets the goal index for this slot so we can check whether the tree satisfies it
            int goalId = currentGoals[i];

            // Only counts the tree if it hasn't been counted for this goal slot before, to prevent farming the same tree
            if (!visitedForThisSlot.contains(treeId)) {
                // Checks whether this tree's data satisfies the active goal for this slot
                if (isGoalSatisfied(goalId, treeData)) {
                    // Increments the progress counter for this slot
                    goalsProgress[i]++;
                    // Marks this tree as visited for this slot so it won't count again
                    visitedForThisSlot.add(treeId);
                    // Records that at least one slot made progress so we trigger a goal update check
                    progressMade = true;
                    // Notifies the user that they advanced toward their goal
                    Toast.makeText(ctx, "Made progress on goal: " + gameList[goalId], Toast.LENGTH_SHORT).show();
                }
            }
        }

        // Only calls updateGoals if progress was actually made, to avoid unnecessary work
        if (progressMade)
            updateGoals(ctx);
    }

    // Returns true if the given tree satisfies the conditions for the specified goal
    private static boolean isGoalSatisfied(int goalId, String[] treeData) {
        // Converts to lowercase once here so all comparisons below are case-insensitive
        String commonName = treeData[1].toLowerCase();
        // Column 23 in the CSV holds the native/non-native status of the tree
        String status = treeData[23].toLowerCase();

        switch (goalId) {
            case 1:
                // Goal: find non-native species — checks the status column for the "non-native" keyword
                return status.contains("non-native");
            case 2:
                // Goal: find oak trees — checks the common name for "oak"
                return commonName.contains("oak");
            case 3:
                // Goal: find maple trees — checks the common name for "maple"
                return commonName.contains("maple");
            case 4:
                // Goal: find spruce trees — checks the common name for "spruce"
                return commonName.contains("spruce");
            case 5:
                // Goal: find trees with red fall color — checks column 33 (fall color) for "red"
                return treeData[33].toLowerCase().contains("red");
            default:
                // Any unknown goal ID returns false to avoid accidental credit
                return false;
        }
    }

    // Checks all goal slots and handles completion — assigns a new goal when one is finished
    public static void updateGoals(Context ctx) {
        for (int ii = 0; ii < currentGoals.length; ii++) {
            // If a slot has no active goal (index 0 = "N/A"), assign one immediately
            if (currentGoals[ii] == 0) assignNewGoal(ii);

            // Checks if the user has met the required count for the current goal in this slot
            if (goalsProgress[ii] >= scoreList[currentGoals[ii]]) {
                // Congratulates the user with a toast message
                Toast.makeText(ctx, "Goal \"" + gameList[currentGoals[ii]] + "\" Complete!", Toast.LENGTH_SHORT).show();

                // Adds to the lifetime completion count for this goal type, which feeds the Achievements screen
                incrementLifetimeAchievement(currentGoals[ii], ctx);
                // Clears the visited tree set for this slot so the new goal starts with a clean slate
                visitedTreesBySlot.get(ii).clear();

                // Resets the progress counter for this slot before assigning the next goal
                goalsProgress[ii] = 0;
                // Assigns a new random goal for this slot
                assignNewGoal(ii);
            }
        }
    }

    // Picks a new random goal for a given slot, ensuring it doesn't duplicate a goal already active in another slot
    private static void assignNewGoal(int slotIndex) {
        Random random = new Random();
        int newGoal;

        do {
            // Picks a random index from 1 to gameList.length-1, skipping index 0 ("N/A") since that's not a real goal
            newGoal = random.nextInt(gameList.length - 1) + 1;
        // Keeps picking until the chosen goal isn't already running in a different slot — prevents both slots showing the same goal
        } while (isGoalAlreadyActive(newGoal));

        // Assigns the new goal to the specified slot
        currentGoals[slotIndex] = newGoal;
        // Clears the visited history for this slot since it's starting a fresh goal
        visitedTreesBySlot.get(slotIndex).clear();
    }

    // Returns true if the given goal ID is already active in any slot — used to prevent duplicate simultaneous goals
    private static boolean isGoalAlreadyActive(int goalId) {
        for (int activeGoal : currentGoals) {
            // Compares against each active slot's current goal
            if (activeGoal == goalId) return true;
        }
        return false;
    }

    // Increments the lifetime completion count for the given goal in SharedPreferences — persists across app sessions
    private static void incrementLifetimeAchievement(int goalId, Context ctx) {
        // Opens the "UserStats" SharedPreferences file where all persistent user statistics are stored
        android.content.SharedPreferences prefs = ctx.getSharedPreferences("UserStats", Context.MODE_PRIVATE);
        // Gets an editor to modify the file
        android.content.SharedPreferences.Editor editor = prefs.edit();

        // Builds the key using the goal index so each goal has its own counter (e.g., "goal_count_2" for oak trees)
        String key = "goal_count_" + goalId;

        // Reads the existing total, defaulting to 0 if this goal has never been completed before
        int currentTotal = prefs.getInt(key, 0);
        // Writes the incremented value back to the file
        editor.putInt(key, currentTotal + 1);

        // Uses apply() instead of commit() to save asynchronously and avoid blocking the UI thread
        editor.apply();
    }
}
