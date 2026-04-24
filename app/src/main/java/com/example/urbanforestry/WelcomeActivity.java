// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports Intent for navigating between Activities (e.g., moving from Welcome to MainActivity)
import android.content.Intent;
// Imports Color so we can set a specific hex background color for the Spring season
import android.graphics.Color;
// Imports Bundle, the key-value container Android passes to onCreate with any saved state
import android.os.Bundle;
// Imports Handler and Looper to schedule a delayed UI action (revealing the buttons after the splash screen)
import android.os.Handler;
import android.os.Looper;
// Imports View for working with the layout containers
import android.view.View;
// Imports Button for the Login and Sign Up buttons
import android.widget.Button;
// Imports ImageView to update the seasonal logo displayed on the welcome screen
import android.widget.ImageView;

// Imports EdgeToEdge to make the app draw behind system bars for a full-screen immersive look
import androidx.activity.EdgeToEdge;
// Imports AppCompatActivity, the base class that provides backwards-compatible Activity features
import androidx.appcompat.app.AppCompatActivity;
// Imports AppCompatDelegate (unused directly here, but inherited from Application class)
import androidx.appcompat.app.AppCompatDelegate;
// Imports SplashScreen from the Jetpack library to control the splash screen duration
import androidx.core.splashscreen.SplashScreen;

// Imports FirebaseAuth to check if a user is already logged in — avoids showing the welcome screen to returning users
import com.google.firebase.auth.FirebaseAuth;

// Declares WelcomeActivity as the app's entry point screen, shown on first launch
public class WelcomeActivity extends AppCompatActivity {

    // Declares the FirebaseAuth instance used to check login state
    private FirebaseAuth mAuth;
    // Declares the two navigation buttons visible on this screen
    private Button btnLogin, btnSignUp;

    // Overrides onCreate, called when this Activity is first created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Determines the current season preference to apply the correct splash screen theme
        SeasonManager.Season currentSeason = SeasonManager.getSeasonPref(this);
        // Declares the starting theme variable — will be set by the switch below
        int startingTheme;
        // Picks the seasonal splash screen theme — must be done before SplashScreen.installSplashScreen() to take effect
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
                // Summer is the fallback to ensure a valid theme is always applied
                startingTheme = R.style.Theme_App_Starting_Summer;
                break;
        }
        // Applies the chosen theme before inflating any layout
        setTheme(startingTheme);

        // Installs the Android 12+ splash screen — must be called before super.onCreate()
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        // Defines the minimum splash screen duration in milliseconds to give users time to see the logo
        final long splashDuration = 2000;
        // Records the timestamp when the splash screen was first shown, so we can measure elapsed time
        final long startTime = System.currentTimeMillis();

        // Keeps the splash screen visible until at least 2 seconds have passed — prevents an instant flash
        splashScreen.setKeepOnScreenCondition(() ->
                System.currentTimeMillis() - startTime < splashDuration
        );

        // Calls the parent class onCreate to complete Android's standard Activity setup
        super.onCreate(savedInstanceState);
        // Enables edge-to-edge rendering so the app draws behind status and navigation bars
        EdgeToEdge.enable(this);
        // Inflates activity_welcome.xml and sets it as this screen's UI
        setContentView(R.layout.activity_welcome);

        // Hides the default action bar since this screen uses a custom full-screen design
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Gets the root layout and the container holding the login/sign-up buttons
        View mainLayout = findViewById(R.id.mainLayout);
        View buttonContainer = findViewById(R.id.buttonContainer);

        // Hides the buttons initially so they don't appear before the splash screen finishes
        if (buttonContainer != null) {
            buttonContainer.setVisibility(View.INVISIBLE);
        }

        // Sets a specific pink background for Spring because its logo has a transparent background that needs a matching color
        if (currentSeason == SeasonManager.Season.SPRING && mainLayout != null) {
            mainLayout.setBackgroundColor(Color.parseColor("#DA8E92"));
        }

        // Updates the main logo to match the current season so the welcome screen feels cohesive
        ImageView mainLogo = findViewById(R.id.logo);
        if (mainLogo != null) {
            mainLogo.setImageResource(SeasonManager.getSeasonLogo(currentSeason));
        }

        // Initialises FirebaseAuth to check if there is already a signed-in user
        mAuth = FirebaseAuth.getInstance();

        // If a user is already signed in, skip the welcome screen entirely and go straight to the map
        if (mAuth.getCurrentUser() != null) {
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            startActivity(intent);
            // Finishes WelcomeActivity so pressing Back doesn't return here after login
            finish();
        } else {
            // Waits until the splash screen duration has elapsed before revealing the login/sign-up buttons
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (buttonContainer != null) {
                    // Makes the buttons visible once the splash timer expires
                    buttonContainer.setVisibility(View.VISIBLE);
                }
            }, splashDuration);
        }

        // Finds the login and sign-up buttons in the layout
        btnLogin = findViewById(R.id.goToLoginButton);
        btnSignUp = findViewById(R.id.goToSignUpButton);

        // Navigates to LoginActivity when the user taps the Login button
        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });

        // Navigates to SignUpActivity when the user taps the Sign Up button
        btnSignUp.setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
        });
    }
}
