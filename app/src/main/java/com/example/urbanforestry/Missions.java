package com.example.urbanforestry;

import android.content.Context;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class Missions {
    public final static String[] gameList = {"N/A", "Find non-native tree species", "Find Oak Trees", "Find Maple Trees", "Find Spruce Trees",
            "Find Trees that are Red in the Fall"};
    public final static int[] scoreList = {0, 6, 8, 4, 4, 4};
    public final static int[] currentGoals = {0, 0};
    public final static int[] goalsProgress = {0, 0};
    public final static List<HashSet<String>> visitedTreesBySlot = new ArrayList<>();

    public static void updateGoalProgress(String treeId, String[] treeData, Context ctx) {
        boolean progressMade = false;

        // Loop through all active goal slots, to make it more flexible in case later more are desired
        for (int i = 0; i < currentGoals.length; i++) {
            HashSet<String> visitedForThisSlot = visitedTreesBySlot.get(i);
            int goalId = currentGoals[i];

            // has this tree been checked for this specific goal
            if (!visitedForThisSlot.contains(treeId)) {
                if (isGoalSatisfied(goalId, treeData)) {
                    goalsProgress[i]++;
                    visitedForThisSlot.add(treeId);
                    progressMade = true;
                    Toast.makeText(ctx, "Made progress on goal: " + gameList[goalId], Toast.LENGTH_SHORT).show();
                }
            }
        }

        if (progressMade)
            updateGoals(ctx);
    }

    private static boolean isGoalSatisfied(int goalId, String[] treeData) {
        String commonName = treeData[1].toLowerCase();
        String status = treeData[23].toLowerCase();

        switch (goalId) {
            case 1:
                return status.contains("non-native");
            case 2:
                return commonName.contains("oak");
            case 3:
                return commonName.contains("maple");
            case 4:
                return commonName.contains("spruce");
            case 5:
                return treeData[33].toLowerCase().contains("red"); // Fall color check
            default:
                return false;
        }
    }

    public static void updateGoals(Context ctx) {
        for (int ii = 0; ii < currentGoals.length; ii++) {
            if (currentGoals[ii] == 0) assignNewGoal(ii);

            if (goalsProgress[ii] >= scoreList[currentGoals[ii]]) {
                Toast.makeText(ctx, "Goal \"" + gameList[currentGoals[ii]] + "\" Complete!", Toast.LENGTH_SHORT).show();

                incrementLifetimeAchievement(currentGoals[ii], ctx);
                // RESET the specific tracking for this slot
                visitedTreesBySlot.get(ii).clear();

                goalsProgress[ii] = 0;
                assignNewGoal(ii);
            }
        }
    }

    private static void assignNewGoal(int slotIndex) {
        Random random = new Random();
        int newGoal;

        do {
            // Pick a random index from gameList (skipping 0 which is "N/A")
            newGoal = random.nextInt(gameList.length - 1) + 1;
        } while (isGoalAlreadyActive(newGoal)); // Keep picking if it's already in another slot

        currentGoals[slotIndex] = newGoal;
        visitedTreesBySlot.get(slotIndex).clear(); // Always clear the history for the new goal
    }

    private static boolean isGoalAlreadyActive(int goalId) {
        for (int activeGoal : currentGoals) {
            if (activeGoal == goalId) return true;
        }
        return false;
    }

    private static void incrementLifetimeAchievement(int goalId, Context ctx) {
        // Open the storage file named "UserStats"
        android.content.SharedPreferences prefs = ctx.getSharedPreferences("UserStats", Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();

        // Create a key based on the goal index (e.g., "goal_count_2")
        String key = "goal_count_" + goalId;

        // Get the current total, add 1, and save it
        int currentTotal = prefs.getInt(key, 0);
        editor.putInt(key, currentTotal + 1);

        // Use apply() to save in the background
        editor.apply();
    }
}
