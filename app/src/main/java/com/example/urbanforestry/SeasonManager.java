// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports Context so SeasonManager can access SharedPreferences, which are tied to the app context
import android.content.Context;
// Imports SharedPreferences to read the user's saved theme preference from persistent local storage
import android.content.SharedPreferences;

// Imports Calendar to get the current month for determining the real-world season
import java.util.Calendar;

// Declares SeasonManager as a utility class with only static methods — no instances needed since it just maps seasons to resources
public class SeasonManager {

    // Defines an enum for the four seasons — using an enum rather than raw strings prevents typos and makes switch statements exhaustive
    public enum Season {
        SPRING, SUMMER, AUTUMN, WINTER
    }

    // Returns the current real-world season based on the device's calendar month
    public static Season getCurrentSeason() {
        // Gets the current month as an integer (Calendar.JANUARY = 0, Calendar.DECEMBER = 11)
        int month = Calendar.getInstance().get(Calendar.MONTH);
        // Returns SPRING for March–May based on standard Northern Hemisphere meteorological seasons
        if (month >= Calendar.MARCH && month <= Calendar.MAY) return Season.SPRING;
        // Returns SUMMER for June–August
        if (month >= Calendar.JUNE && month <= Calendar.AUGUST) return Season.SUMMER;
        // Returns AUTUMN for September–November
        if (month >= Calendar.SEPTEMBER && month <= Calendar.NOVEMBER) return Season.AUTUMN;
        // Returns WINTER as the default for December–February (the only remaining case)
        return Season.WINTER;
    }

    // Returns the user's preferred season for the theme — falls back to the real calendar season if the user hasn't changed it
    public static Season getSeasonPref(Context ctx) {
        // Opens the "MyPrefs" SharedPreferences file where the user's theme choice is stored
        SharedPreferences sp = ctx.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        // Reads the "theme" key, defaulting to "Default" if it hasn't been set yet
        switch (sp.getString("theme", "Default")) {
            // Returns the corresponding Season enum value for each explicit user choice
            case "Spring":
                return Season.SPRING;
            case "Autumn":
                return Season.AUTUMN;
            case "Winter":
                return Season.WINTER;
            case "Summer":
                return Season.SUMMER;
            default:
                // Falls back to the real current season if the user hasn't picked a preference
                return getCurrentSeason();
        }
    }

    // Maps a Season enum value to the corresponding full-screen Activity theme resource ID
    public static int getSeasonTheme(Season season) {
        switch (season) {
            case SPRING:
                // Returns the Spring-specific style defined in res/values/themes.xml
                return R.style.Theme_UrbanForestry_Spring;
            case AUTUMN:
                return R.style.Theme_UrbanForestry_Autumn;
            case WINTER:
                return R.style.Theme_UrbanForestry_Winter;
            case SUMMER:
            default:
                // Summer is also the fallback to ensure we always return a valid theme
                return R.style.Theme_UrbanForestry_Summer;
        }
    }

    // Maps a Season to a Dialog-style theme — used by Activities that appear as overlays so they don't cover the full screen
    public static int getSeasonDialogTheme(Season season) {
        switch (season) {
            case SPRING:
                return R.style.Theme_UrbanForestry_Dialog_Spring;
            case AUTUMN:
                return R.style.Theme_UrbanForestry_Dialog_Autumn;
            case WINTER:
                return R.style.Theme_UrbanForestry_Dialog_Winter;
            case SUMMER:
            default:
                return R.style.Theme_UrbanForestry_Dialog_Summer;
        }
    }

    // Maps a Season to the vertical/square logo drawable resource ID — used on the Welcome and Feed screens
    public static int getSeasonLogo(Season season) {
        switch (season) {
            case SPRING:
                return R.drawable.spring_logo_title;
            case AUTUMN:
                return R.drawable.autumn_logo_title;
            case WINTER:
                return R.drawable.winter_logo_title;
            case SUMMER:
            default:
                // The default logo doubles as the Summer logo
                return R.drawable.logo_title;
        }
    }

    // Maps a Season to the horizontal logo drawable resource ID — used in the Menu where the layout is landscape-oriented
    public static int getSeasonLogoHorizontal(Season season) {
        switch (season) {
            case SPRING:
                return R.drawable.hori_spring_logo;
            case AUTUMN:
                return R.drawable.hori_autumn_logo;
            case WINTER:
                return R.drawable.hori_winter_logo;
            case SUMMER:
            default:
                return R.drawable.hori_summer_logo;
        }
    }
}
