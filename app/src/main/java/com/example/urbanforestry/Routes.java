package com.example.urbanforestry;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;

import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class Routes {
    private final Activity a;
    private final LinearLayout routeControlsContainer;

    // PERSISTENT STATIC LIST - prevents buttons from disappearing when switching activities
    private static List<ActiveRoute> activeRoutes = new ArrayList<>();

    private static class ActiveRoute {
        String id;
        Road road; // Store the road data to rebuild overlays
        int color;
        GeoPoint destination;
        String destName;
        String sourceUser;
        String sourceText;

        // UI objects for the current Activity instance
        Polyline overlay;
        Marker marker;
        MaterialButton toggleButton;
        boolean isVisible = true;
        boolean arrivalPromptShown = false;

        ActiveRoute(String id, Road road, int color, GeoPoint destination, String destName, String sourceUser, String sourceText) {
            this.id = id;
            this.road = road;
            this.color = color;
            this.destination = destination;
            this.destName = destName;
            this.sourceUser = sourceUser;
            this.sourceText = sourceText;
        }
    }

    public Routes(Activity a) {
        this.a = a;
        this.routeControlsContainer = a.findViewById(R.id.routeControlsContainer);
    }

    public void rebuildRouteUI() {
        if (HomePage.map == null || routeControlsContainer == null) return;

        // REMOVE existing overlays to prevent duplicates that can't be toggled off
        for (ActiveRoute ar : activeRoutes) {
            if (ar.overlay != null) HomePage.map.getOverlays().remove(ar.overlay);
            if (ar.marker != null) HomePage.map.getOverlays().remove(ar.marker);
        }

        routeControlsContainer.removeAllViews();
        // Use a copy to prevent concurrent modification if we remove while rebuilding
        for (ActiveRoute ar : new ArrayList<>(activeRoutes)) {
            // Re-create overlays for this specific map instance
            ar.overlay = RoadManager.buildRoadOverlay(ar.road);
            ar.overlay.setColor(ar.color);
            ar.overlay.setWidth(12.0f);

            ar.marker = new Marker(HomePage.map);
            ar.marker.setPosition(ar.destination);
            ar.marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            ar.marker.setTitle("Target Location");
            ar.marker.setSnippet(ar.destName);

            if (ar.isVisible) {
                HomePage.map.getOverlays().add(ar.overlay);
                HomePage.map.getOverlays().add(ar.marker);
            }

            // Create new button for the current activity instance
            MaterialButton btn = (MaterialButton) LayoutInflater.from(a)
                    .inflate(R.layout.item_route_toggle, routeControlsContainer, false);
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ar.color));
            btn.setAlpha(ar.isVisible ? 1.0f : 0.4f);
            routeControlsContainer.addView(btn);
            ar.toggleButton = btn;

            btn.setOnClickListener(v -> showRouteInfoDialog(ar, btn));
        }
        HomePage.map.invalidate();
    }

    public void handleDirectionsIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("getDirections", false)) {
            double destLat = intent.getDoubleExtra("destLat", 0.0);
            double destLng = intent.getDoubleExtra("destLng", 0.0);
            String sourceUser = intent.getStringExtra("postUser");
            String sourceText = intent.getStringExtra("postText");
            GeoPoint destination = new GeoPoint(destLat, destLng);
            String routeId = destLat + "," + destLng;

            // Check if this route is already tracked
            for (ActiveRoute ar : activeRoutes) {
                if (ar.id.equals(routeId)) {
                    Toast.makeText(a, "Already tracking this location!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (HomePage.locationOverlay != null) {
                HomePage.locationOverlay.runOnFirstFix(() -> {
                    GeoPoint myLocation = HomePage.locationOverlay.getMyLocation();
                    if (myLocation != null) {
                        String destName = getAddressName(destination);
                        a.runOnUiThread(() -> new UpdateRoadTask(destination, routeId, destName, sourceUser, sourceText).execute(myLocation, destination));
                    }
                });
            }
        }
    }

    private String getAddressName(GeoPoint point) {
        Geocoder geocoder = new Geocoder(a, Locale.getDefault());
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
            RoadManager roadManager = new OSRMRoadManager(a, "Urban Forestry App");
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

                ActiveRoute newRoute = new ActiveRoute(routeId, road, color, destination, destName, sourceUser, sourceText);
                activeRoutes.add(newRoute);

                // Rebuild the UI to include the new route and refresh overlays
                rebuildRouteUI();

                // FIX: Check if road distance is very short before zooming
                if (road.mLength > 0.05) { // If distance > 50 meters, zoom to bounding box
                    HomePage.map.zoomToBoundingBox(road.mBoundingBox, true);
                } else {
                    // Otherwise, just center on destination and keep a reasonable zoom
                    HomePage.map.getController().animateTo(destination);
                    HomePage.map.getController().setZoom(18.0);
                }
            } else {
                Toast.makeText(a, "Error getting directions", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showRouteInfoDialog(ActiveRoute route, MaterialButton btn) {
        String info = "From post by: " + (route.sourceUser != null ? route.sourceUser : "Unknown") +
                "\n\nText: " + (route.sourceText != null ? route.sourceText : "No description");
        String showHideText = route.isVisible ? "Hide route" : "Show route";

        new AlertDialog.Builder(a)
                .setMessage(info)
                .setPositiveButton("Delete", (dialog, which) -> {
                    removeRoute(route);
                    Toast.makeText(a, "Directions deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(showHideText, (dialog, which) ->
                        toggleRouteVisibility(route, btn))
                .setNeutralButton("Close", null)
                .show();
    }

    private void toggleRouteVisibility(ActiveRoute route, MaterialButton btn) {
        route.isVisible = !route.isVisible;
        if (route.isVisible) {
            HomePage.map.getOverlays().add(route.overlay);
            HomePage.map.getOverlays().add(route.marker);
            btn.setAlpha(1.0f);
        } else {
            HomePage.map.getOverlays().remove(route.overlay);
            HomePage.map.getOverlays().remove(route.marker);
            btn.setAlpha(0.4f);
        }
        HomePage.map.invalidate();
    }

    public void checkArrival(GeoPoint currentLoc) {
        if (currentLoc == null) return;

        for (int i = 0; i < activeRoutes.size(); i++) {
            ActiveRoute ar = activeRoutes.get(i);
            if (ar.arrivalPromptShown) continue;

            double distance = currentLoc.distanceToAsDouble(ar.destination);
            if (distance < 20.0) { // Within 20 meters
                ar.arrivalPromptShown = true;
                showArrivalDialog(ar);
            }
        }
    }

    private void showArrivalDialog(ActiveRoute route) {
        new AlertDialog.Builder(a)
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
        if (route.overlay != null) HomePage.map.getOverlays().remove(route.overlay);
        if (route.marker != null) HomePage.map.getOverlays().remove(route.marker);
        if (route.toggleButton != null) routeControlsContainer.removeView(route.toggleButton);
        activeRoutes.remove(route);
        HomePage.map.invalidate();
    }
}
