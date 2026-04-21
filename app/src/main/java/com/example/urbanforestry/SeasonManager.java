package com.example.urbanforestry;

import android.content.Context;
import java.util.Calendar;

public class SeasonManager {

    public enum Season {
        SPRING, SUMMER, AUTUMN, WINTER
    }

    public static Season getCurrentSeason() {
        int month = Calendar.getInstance().get(Calendar.MONTH);
        // Standard Northern Hemisphere seasons
        if (month >= Calendar.MARCH && month <= Calendar.MAY) return Season.SPRING;
        if (month >= Calendar.JUNE && month <= Calendar.AUGUST) return Season.SUMMER;
        if (month >= Calendar.SEPTEMBER && month <= Calendar.NOVEMBER) return Season.AUTUMN;
        return Season.WINTER;
    }

    public static int getSeasonTheme(Season season) {
        switch (season) {
            case SPRING: return R.style.Theme_UrbanForestry_Spring;
            case AUTUMN: return R.style.Theme_UrbanForestry_Autumn;
            case WINTER: return R.style.Theme_UrbanForestry_Winter;
            case SUMMER:
            default: return R.style.Theme_UrbanForestry_Summer;
        }
    }

    public static int getSeasonDialogTheme(Season season) {
        switch (season) {
            case SPRING: return R.style.Theme_UrbanForestry_Dialog_Spring;
            case AUTUMN: return R.style.Theme_UrbanForestry_Dialog_Autumn;
            case WINTER: return R.style.Theme_UrbanForestry_Dialog_Winter;
            case SUMMER:
            default: return R.style.Theme_UrbanForestry_Dialog_Summer;
        }
    }
    
    public static int getSeasonLogo(Season season) {
        switch (season) {
            case SPRING: return R.drawable.spring_logo_title;
            case AUTUMN: return R.drawable.autumn_logo_title;
            case WINTER: return R.drawable.winter_logo_title;
            case SUMMER:
            default: return R.drawable.logo_title;
        }
    }
}
