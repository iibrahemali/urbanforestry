package com.example.urbanforestry;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.preference.PreferenceManager;

import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
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

import com.google.firebase.auth.FirebaseAuth;

import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;
import org.osmdroid.config.Configuration;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Random;


public class HomePage extends AppCompatActivity {

    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map = null;
    private MyLocationNewOverlay locationOverlay;

    final String[] gameList = {"N/A", "Find non-native tree species", "Find Oaks"};
    final int[] scoreList = {0, 6, 8};
    public static int[] currentGoals = {0, 0};
    public static int[] goalsProgress = {0, 0};

    private Polyline roadOverlay;

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
            
            if (locationOverlay != null) {
                locationOverlay.runOnFirstFix(() -> {
                    GeoPoint myLocation = locationOverlay.getMyLocation();
                    if (myLocation != null) {
                        runOnUiThread(() -> getDirections(myLocation, new GeoPoint(destLat, destLng)));
                    }
                });
            }
        }
    }

    private void getDirections(GeoPoint startPoint, GeoPoint endPoint) {
        new UpdateRoadTask().execute(startPoint, endPoint);
    }

    private class UpdateRoadTask extends AsyncTask<GeoPoint, Void, Road> {
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
            if (roadOverlay != null) {
                map.getOverlays().remove(roadOverlay);
            }
            
            if (road != null && road.mStatus == Road.STATUS_OK) {
                roadOverlay = RoadManager.buildRoadOverlay(road);
                roadOverlay.setColor(Color.BLUE);
                roadOverlay.setWidth(10);
                map.getOverlays().add(roadOverlay);
                map.invalidate();
                
                // Zoom to fit the road
                map.zoomToBoundingBox(road.mBoundingBox, true);
            } else {
                Toast.makeText(HomePage.this, "Error getting directions", Toast.LENGTH_SHORT).show();
            }
        }
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
        this.locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);

        this.locationOverlay.enableMyLocation();
        this.locationOverlay.enableFollowLocation();

        map.getOverlays().add(this.locationOverlay);
        map.getController().setZoom(18.0);
        
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

                final int finalLat = latIndex;
                final int finalLng = lngIndex;
                marker.setOnMarkerClickListener((m, mapView) -> {

                    String[] treeData = (String[]) m.getRelatedObject();

                    String status = treeData[23].toLowerCase();
                    String commonName = treeData[1].toLowerCase();

                    // Goal 1: Find invasive tree species (index 1 in gameList)
                    if (currentGoals[0] == 1 && status.contains("Non-native")) {
                        goalsProgress[0]++;
                        Toast.makeText(this, "Progress: Non-native tree found!", Toast.LENGTH_SHORT).show();
                    }

                    // Goal 2: Find Oaks (index 2 in gameList)
                    if (currentGoals[1] == 2 && commonName.contains("oak")) {
                        goalsProgress[1]++;
                        Toast.makeText(this, "Progress: Oak tree found!", Toast.LENGTH_SHORT).show();
                    }

                    // Call your update method to check for completion
                    updateGoals();

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

    @NonNull
    private Intent getIntent(String[] treeData) {
        Intent i = new Intent(getApplicationContext(), TreeInfo.class);

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

        return i;
    }

    private String getDescription(String[] treeData) {
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

    public void updateGoals() {

        if (currentGoals[0] == 0) currentGoals[0] = 1; // will be made random once list is expanded
        if (currentGoals[1] == 0) currentGoals[1] = 2;

        if (goalsProgress[0] >= scoreList[currentGoals[0]]) {
            Toast.makeText(this, "Goal complete!", Toast.LENGTH_SHORT).show();
            goalsProgress[0] = 0;
            currentGoals[0] = 1; // new random int
        }

        if (goalsProgress[1] >= scoreList[currentGoals[1]]) {
            Toast.makeText(this, "Goal complete!", Toast.LENGTH_SHORT).show();
            goalsProgress[1] = 0;
            currentGoals[1] = 2; // new random int not equal to the first one
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
