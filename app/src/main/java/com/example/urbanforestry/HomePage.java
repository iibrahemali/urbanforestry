package com.example.urbanforestry;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.PreferenceManager;

import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;
import org.osmdroid.config.Configuration;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.HashSet;


public class HomePage extends AppCompatActivity {
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;

    public static MapView map = null;
    public static MyLocationNewOverlay locationOverlay;

    private SeasonManager.Season themeSeason;

    private Routes routes;

    private Location lastLocation = null;
    private float sessionDistanceWalked = 0f;

    // If the user taps the "Sign out" button in the menu, return to this activity and ask for confirmation
    private final ActivityResultLauncher<Intent> arl = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == -1)
                    new AlertDialog.Builder(this)
                            .setMessage("Are you sure you want to sign out?")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                FirebaseAuth.getInstance().signOut();
                                startActivity(new Intent(this, WelcomePage.class));
                                finish();
                            })
                            .setNegativeButton("No", null)
                            .show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set seasonal theme before onCreate
        themeSeason = SeasonManager.getSeasonPref(this);
        setTheme(SeasonManager.getSeasonTheme(themeSeason));

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_home_page);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        routes = new Routes(this);
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        MapMarkers mapMarkers = new MapMarkers(this);
        mapMarkers.showTreeMarkers();
        mapMarkers.showCompostBinMarkers();
        map.invalidate();

        Missions.visitedTreesBySlot.add(new HashSet<>());
        Missions.visitedTreesBySlot.add(new HashSet<>());

        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE});

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageButton menuButton = findViewById(R.id.menuButton);
        menuButton.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), Menu.class);
            i.putExtra("gameList", Missions.gameList);
            i.putExtra("scoreList", Missions.scoreList);
            i.putExtra("currentGoals", Missions.currentGoals);
            i.putExtra("goalsProgress", Missions.goalsProgress);

            arl.launch(i);
        });

        ImageButton feedButton = findViewById(R.id.feedButton);
        feedButton.setOnClickListener(v -> {
            Intent i2 = new Intent(getApplicationContext(), FeedActivity.class);
            startActivity(i2);
        });

        // Initialize missions if they haven't been started yet
        if (Missions.currentGoals[0] == 0) Missions.updateGoals(ctx);

        // RE-POPULATE THE BUTTONS and OVERLAYS from the persistent list
        routes.rebuildRouteUI();

        routes.handleDirectionsIntent(getIntent());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Don't return to log in screen on back swipe
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Important: Update the intent so handleDirectionsIntent uses the new one
        setIntent(intent);
        routes.handleDirectionsIntent(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Reload if the theme was updated from the menu
        if (SeasonManager.getSeasonPref(this) != themeSeason)
            recreate();

        map.onResume(); // an overridden method so the map doesn't have to be destroyed and recreated
        if (locationOverlay != null)
            locationOverlay.enableMyLocation();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
        if (locationOverlay != null)
            locationOverlay.disableMyLocation();
    }

    private void setupLocationTracking() {
        GpsMyLocationProvider provider = new GpsMyLocationProvider(this);
        this.locationOverlay = new MyLocationNewOverlay(provider, map);

        this.locationOverlay.enableMyLocation();
        this.locationOverlay.enableFollowLocation();

        map.getOverlays().add(this.locationOverlay);
        map.getController().setZoom(18.0);

        // Listen for location changes to check for arrival
        provider.startLocationProvider((location, source) -> {
            if (location == null) return;

            // --- DISTANCE TRACKING LOGIC ---
            if (lastLocation != null) {
                // Calculate distance from previous point to current point
                float distanceIncrement = lastLocation.distanceTo(location);

                // Filter out GPS "jitter" (e.g., ignore movements < 1 meter or > 50 meters between updates)
                if (distanceIncrement > 1.0 && distanceIncrement < 50.0) {
                    updateTotalDistance(distanceIncrement);
                }
            }
            lastLocation = location;
            // -------------------------------
            GeoPoint currentLoc = new GeoPoint(location);
            runOnUiThread(() -> routes.checkArrival(currentLoc));
        });
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
            setupLocationTracking();
        } else {
            setupLocationTracking(); // Permissions already granted
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupLocationTracking();
            }
        }
    }

    private void updateTotalDistance(float meters) {
        android.content.SharedPreferences prefs = getSharedPreferences("UserStats", MODE_PRIVATE);
        float totalMeters = prefs.getFloat("total_meters_walked", 0f);

        float newTotal = totalMeters + meters;

        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat("total_meters_walked", newTotal);
        editor.apply();

        // Achievement Notification: Check if we just crossed a whole kilometer mark
        if ((int) (newTotal / 1000) > (int) (totalMeters / 1000)) {
            int km = (int) (newTotal / 1000);
            runOnUiThread(() ->
                    Toast.makeText(this, "Achievement: Walked " + km + "km!", Toast.LENGTH_LONG).show()
            );
        }
    }
}
