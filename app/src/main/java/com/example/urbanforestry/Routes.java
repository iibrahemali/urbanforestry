// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports Activity so Routes can access the layout and run UI operations without being an Activity itself
import android.app.Activity;
// Imports Intent to read direction data passed from other screens
import android.content.Intent;
// Imports Color to generate vivid random colors for each new route line
import android.graphics.Color;
// Imports Address and Geocoder to convert GPS coordinates into a human-readable street address
import android.location.Address;
import android.location.Geocoder;
// Imports AsyncTask to perform the network-based route calculation off the main (UI) thread
import android.os.AsyncTask;
// Imports Log for error tracking
import android.util.Log;
// Imports LayoutInflater to create toggle button Views dynamically at runtime
import android.view.LayoutInflater;
// Imports LinearLayout as the container that holds the route toggle buttons
import android.widget.LinearLayout;
// Imports Toast for brief user notifications
import android.widget.Toast;

// Imports AlertDialog for the route info dialog shown when a toggle button is tapped
import androidx.appcompat.app.AlertDialog;

// Imports MaterialButton to create and style the per-route toggle buttons
import com.google.android.material.button.MaterialButton;

// Imports osmdroid routing classes for calculating routes using the OSRM API
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
// Imports GeoPoint to represent GPS coordinates
import org.osmdroid.util.GeoPoint;
// Imports Marker and Polyline to draw the route endpoint dot and the route line on the map
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

// Imports I/O and collection classes
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

// Declares Routes as a helper class that manages multiple simultaneous navigation routes on the map
public class Routes {
    // Stores the Activity reference for accessing layout, UI thread, and context
    private final Activity a;
    // Stores the container View that holds all route toggle buttons — found from the Activity's layout
    private final LinearLayout routeControlsContainer;

    // Stores all active routes as a static list so they persist across Activity recreations (e.g., screen rotation)
    private static List<ActiveRoute> activeRoutes = new ArrayList<>();

    // Inner class that bundles all data and UI references for a single active navigation route
    private static class ActiveRoute {
        // Unique identifier for this route, built from "lat,lng" of the destination
        String id;
        // Stores the full Road object returned by OSRM — needed to rebuild overlays after Activity recreation
        Road road;
        // The line color assigned to this route — random per route so multiple routes are visually distinct
        int color;
        // The destination GeoPoint — used to place the endpoint marker and check for arrival
        GeoPoint destination;
        // The human-readable address of the destination — shown in the route info dialog
        String destName;
        // The username of the post author — shown in the route info dialog so the user knows which post they're navigating to
        String sourceUser;
        // The caption text from the source post — shown in the route info dialog for context
        String sourceText;

        // UI references for the current Activity instance — recreated on each rebuild since they're bound to a specific Activity
        Polyline overlay;
        Marker marker;
        MaterialButton toggleButton;
        // Tracks whether the route line and marker are currently visible — persists across rebuilds
        boolean isVisible = true;
        // Tracks whether the arrival prompt has already been shown — prevents showing it multiple times
        boolean arrivalPromptShown = false;

        // Constructor that initialises all persistent data fields for a new route
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

    // Constructor that stores the Activity reference and finds the toggle button container from the layout
    public Routes(Activity a) {
        this.a = a;
        // Finds the LinearLayout that will hold the route toggle buttons — must exist in activity_main.xml
        this.routeControlsContainer = a.findViewById(R.id.routeControlsContainer);
    }

    // Rebuilds all route overlays and toggle buttons for the current Activity instance — called after recreation or when a new route is added
    public void rebuildRouteUI() {
        // Skips if the map or container isn't ready yet — prevents NullPointerException during early initialization
        if (MainActivity.map == null || routeControlsContainer == null) return;

        // Removes any existing overlays from the old Activity instance to prevent duplicates that can't be toggled off
        for (ActiveRoute ar : activeRoutes) {
            if (ar.overlay != null) MainActivity.map.getOverlays().remove(ar.overlay);
            if (ar.marker != null) MainActivity.map.getOverlays().remove(ar.marker);
        }

        // Clears the button container so old buttons are removed before new ones are added
        routeControlsContainer.removeAllViews();
        // Iterates over a copy to prevent ConcurrentModificationException if a route is removed during the loop
        for (ActiveRoute ar : new ArrayList<>(activeRoutes)) {
            // Re-creates the Polyline overlay from the stored Road data for this Activity's map instance
            ar.overlay = RoadManager.buildRoadOverlay(ar.road);
            ar.overlay.setColor(ar.color);
            // Sets a thick line width so routes are clearly visible on the map
            ar.overlay.setWidth(12.0f);

            // Re-creates the destination marker for this Activity's map instance
            ar.marker = new Marker(MainActivity.map);
            ar.marker.setPosition(ar.destination);
            ar.marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            // Makes the marker non-interactive — the toggle button handles interactions instead
            ar.marker.setOnMarkerClickListener((marker, mapView) -> false);

            // Adds overlays to the map only if the route is currently set to visible
            if (ar.isVisible) {
                MainActivity.map.getOverlays().add(ar.overlay);
                MainActivity.map.getOverlays().add(ar.marker);
            }

            // Creates a new toggle button inflated from the item_route_toggle layout
            MaterialButton btn = (MaterialButton) LayoutInflater.from(a)
                    .inflate(R.layout.item_route_toggle, routeControlsContainer, false);
            // Colors the button to match its route line so the user can visually identify which button controls which route
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ar.color));
            // Dims the button if the route is hidden — gives visual feedback that the route is off
            btn.setAlpha(ar.isVisible ? 1.0f : 0.4f);
            routeControlsContainer.addView(btn);
            // Stores the button reference so it can be updated when visibility is toggled
            ar.toggleButton = btn;

            // Shows the route info dialog when the toggle button is tapped
            btn.setOnClickListener(v -> showRouteInfoDialog(ar, btn));
        }
        // Tells the map to redraw all overlays — necessary after adding or removing overlays programmatically
        MainActivity.map.invalidate();
    }

    // Reads direction data from an Intent and starts a route calculation if this is a new destination
    public void handleDirectionsIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("getDirections", false)) {
            // Extracts the destination coordinates and post metadata from the Intent extras
            double destLat = intent.getDoubleExtra("destLat", 0.0);
            double destLng = intent.getDoubleExtra("destLng", 0.0);
            String sourceUser = intent.getStringExtra("postUser");
            String sourceText = intent.getStringExtra("postText");
            GeoPoint destination = new GeoPoint(destLat, destLng);
            // Uses "lat,lng" as the unique route ID so the same destination is never tracked twice
            String routeId = destLat + "," + destLng;

            // Checks if this destination is already being tracked to avoid duplicate routes
            for (ActiveRoute ar : activeRoutes) {
                if (ar.id.equals(routeId)) {
                    Toast.makeText(a, "Already tracking this location!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Waits for the first GPS fix before calculating the route — can't route without a starting point
            if (MainActivity.locationOverlay != null) {
                MainActivity.locationOverlay.runOnFirstFix(() -> {
                    GeoPoint myLocation = MainActivity.locationOverlay.getMyLocation();
                    if (myLocation != null) {
                        // Reverses the destination coordinates to a human-readable address for the route dialog
                        String destName = getAddressName(destination);
                        // Runs the route calculation on the UI thread via AsyncTask to avoid blocking the GPS callback thread
                        a.runOnUiThread(() -> new UpdateRoadTask(destination, routeId, destName, sourceUser, sourceText).execute(myLocation, destination));
                    }
                });
            }
        }
    }

    // Converts a GeoPoint to a human-readable street address using the device's Geocoder
    private String getAddressName(GeoPoint point) {
        // Geocoder uses the device's locale so the address format matches the user's region
        Geocoder geocoder = new Geocoder(a, Locale.getDefault());
        try {
            // Requests only the first (most likely) result — maxResults=1 is enough for a display label
            List<Address> addresses = geocoder.getFromLocation(point.getLatitude(), point.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                // Returns the full formatted address line (e.g., "265 Plane Tree Dr, Lancaster, PA 17603")
                return addresses.get(0).getAddressLine(0);
            }
        } catch (IOException e) {
            Log.e("MainActivity", "Geocoding failed", e);
        }
        // Falls back to showing raw coordinates if geocoding fails or returns no results
        return String.format(Locale.US, "%.4f, %.4f", point.getLatitude(), point.getLongitude());
    }

    // AsyncTask that calls the OSRM routing API off the main thread to avoid blocking the UI while fetching the route
    private class UpdateRoadTask extends AsyncTask<GeoPoint, Void, Road> {
        // Stores the parameters passed to the task so they're accessible in doInBackground and onPostExecute
        private GeoPoint destination;
        private String routeId;
        private String destName;
        private String sourceUser;
        private String sourceText;

        // Constructor that receives all data needed to calculate and store the route
        UpdateRoadTask(GeoPoint destination, String routeId, String destName, String sourceUser, String sourceText) {
            this.destination = destination;
            this.routeId = routeId;
            this.destName = destName;
            this.sourceUser = sourceUser;
            this.sourceText = sourceText;
        }

        // Runs on a background thread — calls the OSRM API to calculate the walking/driving route
        @Override
        protected Road doInBackground(GeoPoint... params) {
            // Creates an OSRM road manager with a user agent string identifying this app (required by OSRM's usage policy)
            RoadManager roadManager = new OSRMRoadManager(a, "Urban Forestry App");
            // Builds the waypoints list: params[0] is the user's location, params[1] is the destination
            ArrayList<GeoPoint> waypoints = new ArrayList<>();
            waypoints.add(params[0]);
            waypoints.add(params[1]);
            // Returns the Road object containing the route geometry, distance, and status
            return roadManager.getRoad(waypoints);
        }

        // Runs on the UI thread after doInBackground completes — adds the route to the map
        @Override
        protected void onPostExecute(Road road) {
            // Only proceeds if the OSRM API returned a valid route
            if (road != null && road.mStatus == Road.STATUS_OK) {
                // Generates a random vivid color for this route's line and toggle button
                Random rnd = new Random();
                float[] hsv = new float[3];
                do {
                    // Picks a random hue from 0–360 degrees
                    hsv[0] = rnd.nextInt(360);
                // Avoids the yellow/orange range (hue 35–85) since it's hard to see on a map background
                } while (hsv[0] > 35 && hsv[0] < 85);

                // High saturation (0.8–1.0) ensures the color is vivid rather than washed out
                hsv[1] = 0.8f + rnd.nextFloat() * 0.2f;
                // Medium brightness (0.5–0.8) keeps the color visible without being blindingly bright
                hsv[2] = 0.5f + rnd.nextFloat() * 0.3f;
                int color = Color.HSVToColor(hsv);

                // Creates and stores the new ActiveRoute with all its data
                ActiveRoute newRoute = new ActiveRoute(routeId, road, color, destination, destName, sourceUser, sourceText);
                activeRoutes.add(newRoute);

                // Rebuilds the UI to add the new route's button and overlays
                rebuildRouteUI();

                // Decides how to zoom the camera based on the route's length
                if (road.mLength > 0.05) {
                    // If the route is longer than 50 metres, zoom to fit the full route bounding box
                    MainActivity.map.zoomToBoundingBox(road.mBoundingBox, true);
                } else {
                    // For very short routes, just center on the destination and set a close zoom level
                    MainActivity.map.getController().animateTo(destination);
                    MainActivity.map.getController().setZoom(18.0);
                }
            } else {
                Toast.makeText(a, "Error getting directions", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Shows a dialog with route info (source post, author) and options to delete or toggle visibility
    private void showRouteInfoDialog(ActiveRoute route, MaterialButton btn) {
        // Builds the info text from the route's source post data — handles nulls gracefully
        String info = "From post by: " + (route.sourceUser != null ? route.sourceUser : "Unknown") +
                "\n\nText: " + (route.sourceText != null ? route.sourceText : "No description");
        // Changes the negative button label based on the current visibility state
        String showHideText = route.isVisible ? "Hide route" : "Show route";

        new AlertDialog.Builder(a)
                .setTitle("Route information")
                .setMessage(info)
                // Deletes the route and its overlays permanently from the map
                .setPositiveButton("Delete", (dialog, which) -> {
                    removeRoute(route);
                    Toast.makeText(a, "Directions deleted", Toast.LENGTH_SHORT).show();
                })
                // Toggles the route's visibility without deleting it — useful for temporarily decluttering the map
                .setNegativeButton(showHideText, (dialog, which) ->
                        toggleRouteVisibility(route, btn))
                .setNeutralButton("Close", null)
                .show();
    }

    // Flips the visibility state of a route and updates the map overlays and button alpha accordingly
    private void toggleRouteVisibility(ActiveRoute route, MaterialButton btn) {
        // Flips the boolean visibility flag
        route.isVisible = !route.isVisible;
        if (route.isVisible) {
            // Adds the route line and destination marker back to the map
            MainActivity.map.getOverlays().add(route.overlay);
            MainActivity.map.getOverlays().add(route.marker);
            // Restores full opacity on the toggle button to indicate the route is active
            btn.setAlpha(1.0f);
        } else {
            // Removes the overlays from the map so the route is hidden
            MainActivity.map.getOverlays().remove(route.overlay);
            MainActivity.map.getOverlays().remove(route.marker);
            // Dims the button to indicate the route is currently hidden
            btn.setAlpha(0.4f);
        }
        // Redraws the map to reflect the visibility change immediately
        MainActivity.map.invalidate();
    }

    // Checks whether the user has arrived at any active route's destination — called on every GPS location update
    public void checkArrival(GeoPoint currentLoc) {
        // Skips entirely if there's no GPS fix to avoid NullPointerException
        if (currentLoc == null) return;

        for (int i = 0; i < activeRoutes.size(); i++) {
            ActiveRoute ar = activeRoutes.get(i);
            // Skips routes where the arrival prompt has already been shown to avoid repeated dialogs
            if (ar.arrivalPromptShown) continue;

            // Calculates the distance from the user's current position to the route's destination
            double distance = currentLoc.distanceToAsDouble(ar.destination);
            // Triggers the arrival dialog if the user is within 20 metres of the destination
            if (distance < 20.0) {
                // Marks the prompt as shown immediately to prevent it from firing again on the next GPS update
                ar.arrivalPromptShown = true;
                showArrivalDialog(ar);
            }
        }
    }

    // Shows a dialog asking the user whether they want to keep or remove the route after arriving at the destination
    private void showArrivalDialog(ActiveRoute route) {
        new AlertDialog.Builder(a)
                .setTitle("Destination Reached!")
                .setMessage("You have made it to your destination. Would you like to turn off the directions for this location?")
                // Removes the route if the user confirms — keeps the map clean after navigation
                .setPositiveButton("Yes, turn off", (dialog, which) -> {
                    removeRoute(route);
                })
                // Keeps the route active if the user wants to navigate back or reference it later
                .setNegativeButton("Keep them on", null)
                // Prevents accidental dismissal by tapping outside — user must make a deliberate choice
                .setCancelable(false)
                .show();
    }

    // Removes a route's overlays, toggle button, and data from the active routes list
    private void removeRoute(ActiveRoute route) {
        // Removes the route line from the map if it exists
        if (route.overlay != null) MainActivity.map.getOverlays().remove(route.overlay);
        // Removes the destination marker from the map if it exists
        if (route.marker != null) MainActivity.map.getOverlays().remove(route.marker);
        // Removes the toggle button from the UI container if it exists
        if (route.toggleButton != null) routeControlsContainer.removeView(route.toggleButton);
        // Removes the route from the persistent static list so it doesn't reappear after Activity recreation
        activeRoutes.remove(route);
        // Redraws the map to reflect the removal immediately
        MainActivity.map.invalidate();
    }
}
