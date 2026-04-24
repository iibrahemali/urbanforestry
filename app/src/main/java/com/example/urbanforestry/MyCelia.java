// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports Application, the base class for maintaining global application state — subclassing it lets us run code before any Activity starts
import android.app.Application;

// Imports AppCompatDelegate, which controls the night/dark mode setting for the entire app
import androidx.appcompat.app.AppCompatDelegate;

// Declares MyCelia as the custom Application class — registered in AndroidManifest so Android instantiates it first on launch
public class MyCelia extends Application {
    // Overrides onCreate, which is called once when the app process starts — used to apply global settings before any screen loads
    @Override
    public void onCreate() {
        // Calls the parent Application.onCreate() to complete Android's standard application setup
        super.onCreate();
        // Forces dark mode across the entire app — done here rather than in each Activity so it applies universally and consistently
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }
}
