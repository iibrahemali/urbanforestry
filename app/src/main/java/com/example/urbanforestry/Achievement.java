package com.example.urbanforestry;

public class Achievement {
    private String name;
    private int currentProgress;
    private int goalTarget;
    private int level;

    public Achievement(String name, int currentProgress, int goalTarget, int level) {
        this.name = name;
        this.currentProgress = currentProgress;
        this.goalTarget = goalTarget;
        this.level = level;
    }

    // Getters for the Adapter to use
    public String getName() {
        return name;
    }

    public int getCurrentProgress() {
        return currentProgress;
    }

    public int getGoalTarget() {
        return goalTarget;
    }

    public int getLevel() {
        return level;
    }
}