package com.example.urbanforestry;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

public class MyCelia extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Tells the system that the app uses "dark mode"
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }
}
