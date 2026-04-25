// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports Manifest to reference location permission constants
import android.Manifest;
// Imports Context for passing to osmdroid's configuration loader
import android.content.Context;
// Imports Intent for launching other Activities and handling sign-out results
import android.content.Intent;
// Imports PackageManager to check whether location permissions have been granted
import android.content.pm.PackageManager;
// Imports Location to hold GPS location data for distance tracking
import android.location.Location;
// Imports Bundle, the key-value container Android passes to onCreate with any saved state
import android.os.Bundle;

// Imports OnBackPressedCallback to override the back gesture — prevents returning to the login screen after login
import androidx.activity.OnBackPressedCallback;
// Imports ActivityResultLauncher and contract to handle the result from MenuActivity (sign-out confirmation)
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
// Imports PreferenceManager to load osmdroid's tile cache configuration
import androidx.preference.PreferenceManager;

// Imports ImageButton for the Menu and Feed navigation buttons on the map screen
import android.widget.ImageButton;
// Imports Toast for brief feedback notifications
import android.widget.Toast;

// Imports EdgeToEdge to render the app behind system bars for a full-screen map experience
import androidx.activity.EdgeToEdge;
// Imports NonNull for null-safety annotations
import androidx.annotation.NonNull;
// Imports AlertDialog for the sign-out confirmation prompt
import androidx.appcompat.app.AlertDialog;
// Imports AppCompatActivity, the base class that provides backwards-compatible Activity features
import androidx.appcompat.app.AppCompatActivity;
// Imports ActivityCompat to check and request runtime permissions in a backwards-compatible way
import androidx.core.app.ActivityCompat;
// Imports ContextCompat to check permission state without throwing exceptions
import androidx.core.content.ContextCompat;
// Imports Insets classes to apply system bar padding to the layout
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Imports FirebaseAuth to sign the user out when they confirm from the menu
import com.google.firebase.auth.FirebaseAuth;

// Imports osmdroid tile and map classes for displaying the OpenStreetMap map
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;
import org.osmdroid.config.Configuration;
// Imports the location overlay that shows the user's position as a blue dot on the map
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

// Imports GeoPoint to represent GPS coordinates on the map
import org.osmdroid.util.GeoPoint;

// Imports ArrayList and HashSet for permission lists and visited tree tracking
import java.util.ArrayList;
import java.util.HashSet;

// Declares MainActivity as the app's main screen, showing the osmdroid map with tree markers, routes, and navigation buttons
public class MainActivity extends AppCompatActivity {
    // Stores the request code used to identify this specific permission request in onRequestPermissionsResult
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;

    // Declares the MapView as a public static field so other classes (MapMarkers, Routes) can access the map directly
    public static MapView map = null;
    // Declares the location overlay as public static so Routes can check the user's GPS position for arrival detection
    public static MyLocationNewOverlay locationOverlay;

    // Stores the currently applied season theme to detect when the user changes the theme in the menu
    private SeasonManager.Season themeSeason;

    // Declares the Routes helper that manages navigation route overlays on the map
    private Routes routes;

    // Stores the user's last known GPS location for calculating distance walked between updates
    private Location lastLocation = null;
    // Accumulates the total distance walked in this session — used for the km achievement toast
    private float sessionDistanceWalked = 0f;

    // Registers a launcher for MenuActivity — intercepts result code -1 to show a sign-out confirmation dialog
    private final ActivityResultLauncher<Intent> arl = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Result code -1 is a custom signal from MenuActivity meaning the user tapped "Sign Out"
                if (result.getResultCode() == -1)
                    new AlertDialog.Builder(this)
                            .setMessage("Are you sure you want to sign out?")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                // Signs the user out from Firebase and navigates back to the welcome screen
                                FirebaseAuth.getInstance().signOut();
                                startActivity(new Intent(this, WelcomeActivity.class));
                                // Closes MainActivity so the user can't press Back to return to the map while signed out
                                finish();
                            })
                            .setNegativeButton("No", null)
                            .show();
            });

    // Overrides onCreate, called when this Activity is first created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Reads the season preference and applies the theme before layout inflation
        themeSeason = SeasonManager.getSeasonPref(this);
        setTheme(SeasonManager.getSeasonTheme(themeSeason));

        // Calls the parent class onCreate to complete Android's standard Activity setup
        super.onCreate(savedInstanceState);
        // Enables edge-to-edge rendering so the map fills the entire screen
        EdgeToEdge.enable(this);

        // Inflates activity_main.xml and sets it as this screen's UI
        setContentView(R.layout.activity_main);

        // Loads osmdroid's tile configuration from SharedPreferences — required before using MapView
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        // Instantiates the Routes helper — it finds the route controls container from the layout
        routes = new Routes(this);
        // Finds the MapView from the layout and stores it as a static field for access by other classes
        map = findViewById(R.id.map);
        // Sets the tile source to MAPNIK (standard OpenStreetMap tiles)
        map.setTileSource(TileSourceFactory.MAPNIK);
        // Enables pinch-to-zoom and multi-touch gestures on the map
        map.setMultiTouchControls(true);

        // Creates a MapMarkers helper and loads all three marker types onto the map
        MapMarkers mapMarkers = new MapMarkers(this);
        mapMarkers.showTreeMarkers();       // Green dots for trees from the CSV
        mapMarkers.showCompostBinMarkers(); // Brown dots for compost bins from the CSV
        mapMarkers.showPhotoMarkers();      // Blue dots for posts that have GPS coordinates
        // Tells the map to redraw itself after all markers have been added
        map.invalidate();

        // Initialises the two mission goal slots with empty visited-tree sets before goals are assigned
        Missions.visitedTreesBySlot.add(new HashSet<>());
        Missions.visitedTreesBySlot.add(new HashSet<>());

        // Requests location and storage permissions — location is needed for the GPS dot, storage for tile caching
        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE});

        // Applies system bar insets as padding so buttons aren't hidden behind the status bar or nav bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Finds the menu button and attaches a click listener that launches MenuActivity with the current goal state
        ImageButton menuButton = findViewById(R.id.menuButton);
        menuButton.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), MenuActivity.class);
            // Passes mission arrays so the menu can display the current goals and progress without needing Firestore
            i.putExtra("gameList", Missions.gameList);
            i.putExtra("scoreList", Missions.scoreList);
            i.putExtra("currentGoals", Missions.currentGoals);
            i.putExtra("goalsProgress", Missions.goalsProgress);

            // Launches via the result launcher so we can intercept the sign-out signal
            arl.launch(i);
        });

        // Finds the feed button and launches FeedActivity when tapped
        ImageButton feedButton = findViewById(R.id.feedButton);
        feedButton.setOnClickListener(v -> {
            Intent i2 = new Intent(getApplicationContext(), FeedActivity.class);
            startActivity(i2);
        });

        // Assigns initial goals if none are active yet — called once on first launch per session
        if (Missions.currentGoals[0] == 0) Missions.updateGoals(ctx);

        // Rebuilds any active route overlays and toggle buttons from the persistent static list — survives Activity recreation
        routes.rebuildRouteUI();

        // Checks the incoming Intent for directions data (e.g., from PostAdapter's "Get Directions" button)
        Intent intent = getIntent();
        routes.handleDirectionsIntent(intent);
        // FIX: Clear the intent extras after processing so they don't trigger again on Activity recreation (e.g. theme change)
        if (intent != null) {
            intent.removeExtra("getDirections");
            intent.removeExtra("destLat");
            intent.removeExtra("destLng");
        }

        // Overrides the back gesture to do nothing — prevents the user from accidentally returning to the login screen
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Intentionally empty — back navigation is disabled on the main map screen
            }
        });
    }

    // Called when the Activity receives a new Intent while already running (e.g., tapped "Get Directions" from the feed)
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Updates the stored Intent so handleDirectionsIntent uses the freshly received data, not the original launch intent
        setIntent(intent);
        routes.handleDirectionsIntent(intent);
        
        // FIX: Clear the intent extras here as well for consistency
        if (intent != null) {
            intent.removeExtra("getDirections");
            intent.removeExtra("destLat");
            intent.removeExtra("destLng");
        }
    }

    // Resumes the map and location tracking when the user returns to this screen
    @Override
    public void onResume() {
        super.onResume();

        // Recreates the Activity if the theme was changed in the menu — ensures the new colors apply immediately
        if (SeasonManager.getSeasonPref(this) != themeSeason)
            recreate();

        // Resumes the osmdroid map tile loading and rendering
        map.onResume();
        if (locationOverlay != null)
            // Re-enables the GPS location listener so the blue dot stays accurate
            locationOverlay.enableMyLocation();
    }

    // Pauses the map and stops GPS updates when the user leaves this screen — saves battery
    @Override
    public void onPause() {
        super.onPause();
        // Pauses osmdroid's tile loading to release resources
        map.onPause();
        if (locationOverlay != null)
            // Disables the GPS listener since updates aren't needed when the map isn't visible
            locationOverlay.disableMyLocation();
    }

    // Sets up GPS tracking, the location overlay, and the distance tracking callback
    private void setupLocationTracking() {
        // Creates a GPS location provider that feeds updates to the location overlay
        GpsMyLocationProvider provider = new GpsMyLocationProvider(this);
        // Creates the overlay that draws the blue "my location" dot on the map
        this.locationOverlay = new MyLocationNewOverlay(provider, map);

        // Starts receiving GPS updates so the blue dot appears on the map
        this.locationOverlay.enableMyLocation();
        // Makes the map camera follow the user's position as they walk
        this.locationOverlay.enableFollowLocation();

        // Adds the location overlay to the map so it renders above all other overlays
        map.getOverlays().add(this.locationOverlay);
        // Sets an initial zoom level that shows a useful area around the user
        map.getController().setZoom(18.0);

        // Registers a callback that fires on every GPS location update — used for distance tracking and arrival detection
        provider.startLocationProvider((location, source) -> {
            if (location == null) return;

            // Calculates the distance walked since the last GPS update
            if (lastLocation != null) {
                // distanceTo returns the distance in meters between the two points
                float distanceIncrement = lastLocation.distanceTo(location);

                // Filters out GPS jitter: ignores movements under 1 meter (noise) and over 50 meters (GPS teleport)
                if (distanceIncrement > 1.0 && distanceIncrement < 50.0) {
                    updateTotalDistance(distanceIncrement);
                }
            }
            // Updates the last known location for the next distance calculation
            lastLocation = location;

            // Converts to a GeoPoint for use with osmdroid's arrival check
            GeoPoint currentLoc = new GeoPoint(location);
            // Runs arrival check on the UI thread since it may show a dialog
            runOnUiThread(() -> routes.checkArrival(currentLoc));
        });
    }

    // Checks which of the requested permissions are missing and requests them if necessary
    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            // Checks each permission — only adds it to the request list if not already granted
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permissionsToRequest.size(), permission);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            // Shows the system permission dialog for any missing permissions
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
            // Sets up location tracking even before the user responds — it will start showing once permission is granted
        } else {
            // All permissions are already granted, so set up location tracking immediately
            setupLocationTracking();
        }
    }

    // Called by the system with the user's response to the permission dialog
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Checks that this result is for our specific request and that at least one permission was granted
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Re-runs location setup now that we have permission — the overlay can now actually acquire a fix
                setupLocationTracking();
            }
        }
    }

    // Adds the given distance increment to the running total stored in SharedPreferences and fires an achievement toast at km milestones
    private void updateTotalDistance(float meters) {
        // Opens the UserStats SharedPreferences file to read the current total
        android.content.SharedPreferences prefs = getSharedPreferences("UserStats", MODE_PRIVATE);
        float totalMeters = prefs.getFloat("total_meters_walked", 0f);

        // Adds the new increment to the running total
        float newTotal = totalMeters + meters;

        // Writes the updated total back to SharedPreferences asynchronously
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat("total_meters_walked", newTotal);
        editor.apply();

        // Checks if the user just crossed a whole kilometer mark — integer division detects boundary crossings
        if ((int) (newTotal / 1000) > (int) (totalMeters / 1000)) {
            int km = (int) (newTotal / 1000);
            // Shows the achievement toast on the UI thread since this callback runs on a background GPS thread
            runOnUiThread(() ->
                    Toast.makeText(this, "Achievement: Walked " + km + "km!", Toast.LENGTH_LONG).show()
            );
        }
    }
}
