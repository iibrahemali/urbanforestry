package com.example.urbanforestry;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
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

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;
import org.osmdroid.config.Configuration;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;


public class HomePage extends AppCompatActivity {

    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map = null;
    private MyLocationNewOverlay locationOverlay;

    final String[] gameList = {"N/A", "Find non-native tree species", "Find Oaks", "Find Maple Trees"};
    final int[] scoreList = {0, 6, 8, 4};
    public static int[] currentGoals = {0, 0};
    public static int[] goalsProgress = {0, 0};
    private List<HashSet<String>> visitedTreesBySlot = new ArrayList<>();

    private LinearLayout routeControlsContainer;
    
    // Store active routes: Key is target lat,lng string
    private List<ActiveRoute> activeRoutes = new ArrayList<>();

    private static class ActiveRoute {
        String id;
        Polyline overlay;
        Marker marker;
        MaterialButton toggleButton;
        int color;
        boolean isVisible = true;
        boolean arrivalPromptShown = false;
        GeoPoint destination;
        String sourceUser;
        String sourceText;

        ActiveRoute(String id, Polyline overlay, Marker marker, MaterialButton toggleButton, int color, GeoPoint destination, String sourceUser, String sourceText) {
            this.id = id;
            this.overlay = overlay;
            this.marker = marker;
            this.toggleButton = toggleButton;
            this.color = color;
            this.destination = destination;
            this.sourceUser = sourceUser;
            this.sourceText = sourceText;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_home_page);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        routeControlsContainer = findViewById(R.id.routeControlsContainer);

        visitedTreesBySlot.add(new HashSet<>()); // Slot 0
        visitedTreesBySlot.add(new HashSet<>());

        loadTreeData(); // This loads all of the trees
        loadCompostData(); // This loads all of the compost bins
        map.invalidate();

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
            i.putExtra("gameList", gameList);
            i.putExtra("scoreList", scoreList);
            i.putExtra("currentGoals", currentGoals);
            i.putExtra("goalsProgress", goalsProgress);
            startActivity(i);
        });

        ImageButton feedButton = findViewById(R.id.feedButton);
        feedButton.setOnClickListener(v -> {
            Intent i2 = new Intent(getApplicationContext(), FeedActivity.class);
            startActivity(i2);
        });
    }

    private void handleDirectionsIntent() {
        if (getIntent().getBooleanExtra("getDirections", false)) {
            double destLat = getIntent().getDoubleExtra("destLat", 0.0);
            double destLng = getIntent().getDoubleExtra("destLng", 0.0);
            String sourceUser = getIntent().getStringExtra("postUser");
            String sourceText = getIntent().getStringExtra("postText");
            GeoPoint destination = new GeoPoint(destLat, destLng);
            String routeId = destLat + "," + destLng;

            // Check if this route is already tracked
            for (ActiveRoute ar : activeRoutes) {
                if (ar.id.equals(routeId)) {
                    Toast.makeText(this, "Already tracking this location!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (locationOverlay != null) {
                locationOverlay.runOnFirstFix(() -> {
                    GeoPoint myLocation = locationOverlay.getMyLocation();
                    if (myLocation != null) {
                        String startName = getAddressName(myLocation);
                        String destName = getAddressName(destination);

                        runOnUiThread(() -> {
                            Toast.makeText(this, "Start: " + startName + "\nTarget: " + destName, Toast.LENGTH_LONG).show();
                            new UpdateRoadTask(destination, routeId, destName, sourceUser, sourceText).execute(myLocation, destination);
                        });
                    }
                });
            }
        }
    }

    private String getAddressName(GeoPoint point) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(point.getLatitude(), point.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }
        } catch (IOException e) {
            Log.e("HomePage", "Geocoding failed", e);
        }
        return String.format(Locale.US, "%.4f, %.4f", point.getLatitude(), point.getLongitude());
    }

    private class UpdateRoadTask extends AsyncTask<GeoPoint, Void, Road> {
        private GeoPoint destination;
        private String routeId;
        private String destName;
        private String sourceUser;
        private String sourceText;

        UpdateRoadTask(GeoPoint destination, String routeId, String destName, String sourceUser, String sourceText) {
            this.destination = destination;
            this.routeId = routeId;
            this.destName = destName;
            this.sourceUser = sourceUser;
            this.sourceText = sourceText;
        }

        @Override
        protected Road doInBackground(GeoPoint... params) {
            RoadManager roadManager = new OSRMRoadManager(HomePage.this, "Urban Forestry App");
            ArrayList<GeoPoint> waypoints = new ArrayList<>();
            waypoints.add(params[0]);
            waypoints.add(params[1]);
            return roadManager.getRoad(waypoints);
        }

        @Override
        protected void onPostExecute(Road road) {
            if (road != null && road.mStatus == Road.STATUS_OK) {
                // Generate a random distinct vivid color (avoiding light/yellow colors for visibility)
                Random rnd = new Random();
                float[] hsv = new float[3];
                do {
                    hsv[0] = rnd.nextInt(360); // Hue [0, 360]
                } while (hsv[0] > 35 && hsv[0] < 85); // Avoid yellow and orange range (approx 40-80)
                
                hsv[1] = 0.8f + rnd.nextFloat() * 0.2f; // High saturation (0.8 - 1.0)
                hsv[2] = 0.5f + rnd.nextFloat() * 0.3f; // Medium brightness (0.5 - 0.8) to keep it visible
                int color = Color.HSVToColor(hsv);

                Polyline overlay = RoadManager.buildRoadOverlay(road);
                overlay.setColor(color);
                overlay.setWidth(12.0f);
                map.getOverlays().add(overlay);

                Marker marker = new Marker(map);
                marker.setPosition(destination);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                marker.setTitle("Target Location");
                marker.setSnippet(destName);
                map.getOverlays().add(marker);

                // Create the toggle button
                MaterialButton btn = (MaterialButton) LayoutInflater.from(HomePage.this)
                        .inflate(R.layout.item_route_toggle, routeControlsContainer, false);
                btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
                routeControlsContainer.addView(btn);

                ActiveRoute newRoute = new ActiveRoute(routeId, overlay, marker, btn, color, destination, sourceUser, sourceText);
                activeRoutes.add(newRoute);

                btn.setOnClickListener(v -> {
                    newRoute.isVisible = !newRoute.isVisible;
                    if (newRoute.isVisible) {
                        map.getOverlays().add(newRoute.overlay);
                        map.getOverlays().add(newRoute.marker);
                        btn.setAlpha(1.0f);
                    } else {
                        map.getOverlays().remove(newRoute.overlay);
                        map.getOverlays().remove(newRoute.marker);
                        btn.setAlpha(0.4f);
                    }
                    map.invalidate();
                });

                btn.setOnLongClickListener(v -> {
                    showRouteInfoDialog(newRoute);
                    return true;
                });

                map.zoomToBoundingBox(road.mBoundingBox, true);
                map.invalidate();
            } else {
                Toast.makeText(HomePage.this, "Error getting directions", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showRouteInfoDialog(ActiveRoute route) {
        String info = "From post by: " + (route.sourceUser != null ? route.sourceUser : "Unknown") +
                      "\nText: " + (route.sourceText != null ? route.sourceText : "No description");

        new AlertDialog.Builder(this)
                .setTitle("Route Information")
                .setMessage(info + "\n\nDo you want to delete these directions?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    removeRoute(route);
                    Toast.makeText(this, "Directions deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void checkArrival(GeoPoint currentLoc) {
        if (currentLoc == null) return;
        
        List<ActiveRoute> toRemove = new ArrayList<>();
        for (ActiveRoute ar : activeRoutes) {
            if (ar.arrivalPromptShown) continue;

            double distance = currentLoc.distanceToAsDouble(ar.destination);
            if (distance < 20.0) { // Within 20 meters
                ar.arrivalPromptShown = true;
                showArrivalDialog(ar);
            }
        }
    }

    private void showArrivalDialog(ActiveRoute route) {
        new AlertDialog.Builder(this)
                .setTitle("Destination Reached!")
                .setMessage("You have made it to your destination. Would you like to turn off the directions for this location?")
                .setPositiveButton("Yes, turn off", (dialog, which) -> {
                    removeRoute(route);
                })
                .setNegativeButton("Keep them on", null)
                .setCancelable(false)
                .show();
    }

    private void removeRoute(ActiveRoute route) {
        map.getOverlays().remove(route.overlay);
        map.getOverlays().remove(route.marker);
        routeControlsContainer.removeView(route.toggleButton);
        activeRoutes.remove(route);
        map.invalidate();
    }

    @Override
    public void onResume() {
        super.onResume();
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
        provider.startLocationProvider(new IMyLocationConsumer() {
            @Override
            public void onLocationChanged(Location location, IMyLocationProvider source) {
                GeoPoint currentLoc = new GeoPoint(location);
                runOnUiThread(() -> checkArrival(currentLoc));
            }
        });

        handleDirectionsIntent();
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

    private void loadTreeData() {
        try {
            InputStream is = getAssets().open("trees.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line;
            boolean firstLine = true;

            int latIndex = -1;
            int lngIndex = -1;

            while ((line = reader.readLine()) != null) {

                String[] cols = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                // Remove quotes around strings
                for (int i = 0; i < cols.length; i++)
                    cols[i] = cols[i].replaceAll("^\"|\"$", "");

                // First row = headers
                if (firstLine) {
                    firstLine = false;

                    for (int i = 0; i < cols.length; i++) {
                        if (cols[i].equals("LATITUDE")) latIndex = i;
                        if (cols[i].equals("LONGITUDE")) lngIndex = i;
                    }
                    continue;
                }

                if (latIndex == -1 || lngIndex == -1) continue;

                double lat = Double.parseDouble(cols[latIndex]);
                double lng = Double.parseDouble(cols[lngIndex]);

                GeoPoint point = new GeoPoint(lat, lng);

                Marker marker = new Marker(map);
                marker.setPosition(point);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);

                marker.setRelatedObject(cols.clone());

                // Make it a small green dot
                marker.setIcon(ContextCompat.getDrawable(this, R.drawable.green_dot));


                marker.setOnMarkerClickListener((m, mapView) -> {
                    String[] treeData = (String[]) m.getRelatedObject();
                    String treeId = m.getPosition().getLatitude() + "," + m.getPosition().getLongitude();

                    // Update goal progress
                    if (locationOverlay.getMyLocation() != null) {
                        double distance = locationOverlay.getMyLocation().distanceToAsDouble(m.getPosition());
                        boolean progressMade = false;

                        // Trees can only be "discovered" if the user is within 10m
                        if (distance <= 10.0) {
                            // Loop through all active goal slots, to make it more flexible in case later more are deired
                            for (int ii = 0; ii < currentGoals.length; ii++) {
                                int activeGoalId = currentGoals[ii];
                                HashSet<String> visitedForThisSlot = visitedTreesBySlot.get(ii);

                                // has this tree been checked for this specific goal
                                if (!visitedForThisSlot.contains(treeId)) {
                                    if (isGoalSatisfied(activeGoalId, treeData)) {
                                        goalsProgress[ii]++;
                                        visitedForThisSlot.add(treeId);
                                        progressMade = true;
                                        Toast.makeText(this, "Made rogress on Goal: " + gameList[activeGoalId], Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        }

                        if (progressMade) updateGoals();
                    }

                    // Show the tree info
                    Intent i = getIntent(treeData);
                    startActivity(i);

                    return true;
                });


                map.getOverlays().add(marker);

            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isGoalSatisfied(int goalId, String[] treeData) {
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
                return treeData[33].toLowerCase().contains("red"); // Fall color check
            default:
                return false;
        }
    }

    @NonNull
    private Intent getIntent(String[] treeData) {
        Intent i = new Intent(getApplicationContext(), TreeInfo.class);

        // Send relevant tree data to the info page
        i.putExtra("commonName", treeData[1]);
        i.putExtra("botanicalName", treeData[2]);
        i.putExtra("familyCommon", treeData[55]);
        i.putExtra("familyBotanical", treeData[56]);
        i.putExtra("botanicalName", treeData[2]);
        i.putExtra("nativeOrCultivated", treeData[23]);
        i.putExtra("wildlife", treeData[31]);
        i.putExtra("fallColors", treeData[33]);
        i.putExtra("flowers", treeData[35]);
        i.putExtra("dbh", treeData[5]);
        i.putExtra("height", treeData[27]);
        i.putExtra("description", getDescription(treeData));
        i.putExtra("mortonPage", getMortonPageName(treeData));

        return i;
    }

    private String getDescription(String[] treeData) {
        // Get the tree's botanical name, and return the corresponding description in strings.xml

        String strName = treeData[2].replaceAll(" ", "_");
        int strId = getResources().getIdentifier(strName, "string", getPackageName());

        if (strId != 0)
            return getString(strId);
        else
            // Handle cases without botanical species names
            switch (treeData[1]) {
                case "White swamp birch":
                    return getString(R.string.white_swamp_birch);
                case "Serviceberry":
                    return getString(R.string.serviceberry);
                case "Okame cherry":
                    return getString(R.string.okame_cherry);
                default:
                    // No description exists
                    return "";
            }
    }

    // When new species are added to the database, this function must be updated to ensure no invalid links
    private String getMortonPageName(String[] treeData) {
        // Handle cases where the common name in our data doesn't match the site's
        switch (treeData[1]) {
            case "Baldcypress":
                return "bald-cypress";
            case "Bigleaf linden":
                return "big-leaved-linden";
            case "Black tupelo":
                return "tupelo";
            case "Eastern redbud":
                return "redbud";
            case "Freeman maple":
                return "freemans-maple";
            case "Goldenrain tree":
                return "golden-rain-tree";
            case "Honeylocust":
                return "honey-locust";
            case "Kentucky Coffee tree":
                return "kentucky-coffeetree";
            case "Northern white cedar":
                return "eastern-arborvitae";
            case "Pagoda-tree":
                return "japanese-scholar-tree";
            case "Paperbark maple":
                return "paper-barked-maple";
            case "Snowdrop tree":
                return "silverbell";
            case "Sweetgum":
                return "sweet-gum";
            case "Deodar cedar":
            case "Lily Magnolia":
            case "Punk tree":
            case "Sawtooth oak":
            case "Soulard crab":
            case "Southern red oak":
            case "Weeping willow":
            case "Yellow wood":
            case "yew spp":
            case "Tree, Unknown":
                // No page exists
                return "";
            default:
                // Default format
                return treeData[1].toLowerCase().replaceAll(" ", "-");
        }
    }

    public void updateGoals() {
        Random rand = new Random();
        if (currentGoals[0] == 0) currentGoals[0] = 1; // will be made random once list is expanded
        if (currentGoals[1] == 0) currentGoals[1] = 2;

        for (int ii = 0; ii < currentGoals.length; ii++) {
            if (goalsProgress[ii] >= scoreList[currentGoals[ii]]) {
                Toast.makeText(this, "Goal \"" + gameList[currentGoals[ii]] + "\" Complete!", Toast.LENGTH_SHORT).show();

                // RESET the specific tracking for this slot
                visitedTreesBySlot.get(ii).clear();

                goalsProgress[ii] = 0;
                do {
                    currentGoals[ii] = rand.nextInt(gameList.length);
                } while (currentGoals[ii] == 0 || (ii > 0 && currentGoals[ii] == currentGoals[ii - 1])); // may need to change to make more robust
            }
        }

    }

    private void loadCompostData() {
        try {
            InputStream is = getAssets().open("compostBins.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                // Bin Name,Coordinates,Link
                String[] cols = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                String binName = cols[0].replaceAll("^\"|\"$", "");
                String coordsStr = cols[1].replaceAll("^\"|\"$", "");
                String link = cols[2].replaceAll("^\"|\"$", "");

                String[] latLng = coordsStr.split(",");
                double lat = Double.parseDouble(latLng[0].trim());
                double lng = Double.parseDouble(latLng[1].trim());

                GeoPoint point = new GeoPoint(lat, lng);
                Marker marker = new Marker(map);
                marker.setPosition(point);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                marker.setIcon(ContextCompat.getDrawable(this, R.drawable.brown_dot));
                marker.setTitle(binName);

                marker.setOnMarkerClickListener((m, mapView) -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(HomePage.this);
                    builder.setTitle(binName);
                    builder.setMessage("Composting is the process of recycling organic waste, like food scraps and leaves, into nutrient-rich soil.");
                    builder.setPositiveButton("Visit Link", (dialog, which) -> {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                        startActivity(browserIntent);
                    });
                    builder.setNegativeButton("Close", null);
                    builder.show();
                    return true;
                });

                map.getOverlays().add(marker);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
