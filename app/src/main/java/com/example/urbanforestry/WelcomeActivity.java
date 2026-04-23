package com.example.urbanforestry;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.splashscreen.SplashScreen;

import com.google.firebase.auth.FirebaseAuth;

public class WelcomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private Button btnLogin, btnSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Determine season and set the appropriate starting theme for the splash screen
        SeasonManager.Season currentSeason = SeasonManager.getSeasonPref(this);
        int startingTheme;
        switch (currentSeason) {
            case SPRING:
                startingTheme = R.style.Theme_App_Starting_Spring;
                break;
            case AUTUMN:
                startingTheme = R.style.Theme_App_Starting_Autumn;
                break;
            case WINTER:
                startingTheme = R.style.Theme_App_Starting_Winter;
                break;
            case SUMMER:
            default:
                startingTheme = R.style.Theme_App_Starting_Summer;
                break;
        }
        setTheme(startingTheme);

        // Add a splash screen with the logo
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        final long splashDuration = 2000;
        final long startTime = System.currentTimeMillis();

        // Keep the splash screen visible for at least 2 seconds
        splashScreen.setKeepOnScreenCondition(() ->
                System.currentTimeMillis() - startTime < splashDuration
        );

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcome);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        View mainLayout = findViewById(R.id.mainLayout);
        View buttonContainer = findViewById(R.id.buttonContainer);

        // Initially hide the login/signup buttons
        if (buttonContainer != null) {
            buttonContainer.setVisibility(View.INVISIBLE);
        }

        // Apply specific Spring background color if it's currently Spring
        if (currentSeason == SeasonManager.Season.SPRING && mainLayout != null) {
            mainLayout.setBackgroundColor(Color.parseColor("#DA8E92"));
        }

        // Update the main logo on the welcome page itself to match the season
        ImageView mainLogo = findViewById(R.id.logo);
        if (mainLogo != null) {
            mainLogo.setImageResource(SeasonManager.getSeasonLogo(currentSeason));
        }

        mAuth = FirebaseAuth.getInstance();

        // Determine navigation path
        if (mAuth.getCurrentUser() != null) {
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            // If not logged in, wait for the splash duration then reveal the login buttons
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (buttonContainer != null) {
                    buttonContainer.setVisibility(View.VISIBLE);
                }
            }, splashDuration);
        }

        btnLogin = findViewById(R.id.goToLoginButton);
        btnSignUp = findViewById(R.id.goToSignUpButton);

        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });

        btnSignUp.setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
        });
    }
}
