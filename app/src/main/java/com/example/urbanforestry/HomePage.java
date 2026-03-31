package com.example.urbanforestry;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.widget.Button;

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
import org.osmdroid.views.overlay.Marker;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;


public class HomePage extends AppCompatActivity {

    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map = null;
    private MyLocationNewOverlay locationOverlay;

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
        map.invalidate();

        Button signOutButton = findViewById(R.id.signOutButton);
        signOutButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Sign Out")
                    .setMessage("Are you sure you want to sign out?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(this, WelcomePage.class));
                        finish();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE});

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public void onResume(){
        super.onResume();
        map.onResume(); // an overridden method so the map doesn't have to be destroyed and recreated
        if(locationOverlay != null)
            locationOverlay.enableMyLocation();
    }

    @Override
    public void onPause(){
        super.onPause();
        map.onPause();
        if(locationOverlay != null)
            locationOverlay.disableMyLocation();
    }

    private void setupLocationTracking(){
        this.locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);

        this.locationOverlay.enableMyLocation();
        this.locationOverlay.enableFollowLocation();

        map.getOverlays().add(this.locationOverlay);
        map.getController().setZoom(18.0);
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

                // Make it a small green dot
                marker.setIcon(ContextCompat.getDrawable(this, R.drawable.green_dot));

                String commonName = cols[1];     // iTree Common Name
                String botanicalName = cols[2];  // iTree Botanical Name
                String dbh = cols[5];            // DBH

                marker.setTitle(commonName);
                marker.setSnippet("Species: " + botanicalName + "\nDBH: " + dbh);

                marker.setSubDescription("");

                map.getOverlays().add(marker);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}