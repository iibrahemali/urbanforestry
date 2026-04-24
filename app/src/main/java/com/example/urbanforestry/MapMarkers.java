// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports Context to access assets, resources, and start Activities without needing an Activity reference directly
import android.content.Context;
// Imports Intent for launching TreeInfoActivity and PhotoPopupActivity when a marker is tapped
import android.content.Intent;
// Imports Uri to open a browser link for compost bin information
import android.net.Uri;
// Imports Log for recording Firestore listener errors
import android.util.Log;

// Imports NonNull for null-safety annotations
import androidx.annotation.NonNull;
// Imports AlertDialog to show a compost bin information popup when a brown marker is tapped
import androidx.appcompat.app.AlertDialog;
// Imports ContextCompat to safely load drawable resources without crashing on older Android versions
import androidx.core.content.ContextCompat;

// Imports Firebase authentication and Firestore types for loading user photo markers from the posts collection
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

// Imports osmdroid GeoPoint and Marker classes for placing markers on the map
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;

// Imports I/O classes for reading the CSV files bundled in the assets folder
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
// Imports ArrayList and List for building collections of addresses
import java.util.ArrayList;
import java.util.List;

// Declares MapMarkers as a helper class responsible for loading and placing all three marker types on the map
public class MapMarkers {
    // Stores the Context used to open assets, start Activities, and load drawables
    private final Context ctx;

    // Constructor that stores the Context — called once by MainActivity to create the helper
    public MapMarkers(Context ctx) {
        this.ctx = ctx;
    }

    // Reads the trees.csv asset and places a green dot marker on the map for every tree in the file
    public void showTreeMarkers() {
        try {
            // Opens the trees.csv file from the app's assets folder
            InputStream is = ctx.getAssets().open("trees.csv");
            // Wraps the stream in a BufferedReader for efficient line-by-line reading
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line;
            // Tracks whether we're on the header row so we can find the column indices before reading data
            boolean firstLine = true;

            // Stores the column index of the latitude and longitude fields — -1 means not yet found
            int latIndex = -1;
            int lngIndex = -1;

            // Reads the CSV one line at a time until the end of the file
            while ((line = reader.readLine()) != null) {

                // Splits on commas while respecting quoted strings that may contain embedded commas
                String[] cols = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                // Strips surrounding double-quotes from each field value
                for (int i = 0; i < cols.length; i++)
                    cols[i] = cols[i].replaceAll("^\"|\"$", "");

                // On the first line (the header row), finds which column indices hold LATITUDE and LONGITUDE
                if (firstLine) {
                    firstLine = false;

                    for (int i = 0; i < cols.length; i++) {
                        if (cols[i].equals("LATITUDE")) latIndex = i;
                        if (cols[i].equals("LONGITUDE")) lngIndex = i;
                    }
                    // Skips to the next line after storing the header column indices
                    continue;
                }

                // Skips the row if the lat/lng columns were never found — prevents crashing on malformed CSVs
                if (latIndex == -1 || lngIndex == -1) continue;

                // Parses the latitude and longitude strings into doubles for use with GeoPoint
                double lat = Double.parseDouble(cols[latIndex]);
                double lng = Double.parseDouble(cols[lngIndex]);

                // Creates an osmdroid GeoPoint from the coordinates
                GeoPoint point = new GeoPoint(lat, lng);

                // Creates a Marker and places it at the tree's position on the map
                Marker marker = new Marker(MainActivity.map);
                marker.setPosition(point);
                // Centers the marker icon both horizontally and vertically on the coordinate point
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);

                // Attaches the full row data to the marker so we can access all tree fields when it's tapped
                marker.setRelatedObject(cols.clone());

                // Uses a small green dot drawable to represent a tree — keeps the map uncluttered
                marker.setIcon(ContextCompat.getDrawable(ctx, R.drawable.green_dot));

                // Sets up the tap listener for this tree marker
                marker.setOnMarkerClickListener((m, mapView) -> {
                    // Retrieves the full CSV row data attached to this marker
                    String[] treeData = (String[]) m.getRelatedObject();
                    // Builds a unique ID for this tree using its coordinates — used as the key in the visited set
                    String treeId = m.getPosition().getLatitude() + "," + m.getPosition().getLongitude();

                    // Only awards goal progress if the user is close enough to the tree — prevents remote farming
                    if (MainActivity.locationOverlay.getMyLocation() != null) {
                        double distance = MainActivity.locationOverlay.getMyLocation().distanceToAsDouble(m.getPosition());
                        // Trees can only be "discovered" if the user is within 10 metres
                        if (distance <= 10.0)
                            // Checks if discovering this tree makes progress toward any active goals
                            Missions.updateGoalProgress(treeId, treeData, ctx);
                    }

                    // Launches TreeInfoActivity with the tree's data passed as Intent extras
                    Intent i = getTreeInfoIntent(treeData, m);
                    ctx.startActivity(i);

                    // Returns true to consume the tap event and prevent default marker behavior
                    return true;
                });

                // Adds the marker to the map's overlay list so it becomes visible
                MainActivity.map.getOverlays().add(marker);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Builds and returns an Intent for TreeInfoActivity, populated with all tree fields extracted from the CSV row
    @NonNull
    private Intent getTreeInfoIntent(String[] treeData, Marker m) {
        Intent i = new Intent(ctx.getApplicationContext(), TreeInfoActivity.class);

        // Passes the tree's common name (column 1) so the info screen can display it as the title
        i.putExtra("commonName", treeData[1]);
        // Passes the botanical name (column 2) for the scientific name label
        i.putExtra("botanicalName", treeData[2]);
        // Passes the common and botanical family names (columns 55 and 56)
        i.putExtra("familyCommon", treeData[55]);
        i.putExtra("familyBotanical", treeData[56]);
        i.putExtra("botanicalName", treeData[2]);
        // Passes the native/cultivated status (column 23) for the origin label
        i.putExtra("nativeOrCultivated", treeData[23]);
        // Passes the wildlife value (column 31) for ecological information
        i.putExtra("wildlife", treeData[31]);
        // Passes the fall foliage colors (column 33)
        i.putExtra("fallColors", treeData[33]);
        // Passes flower information (column 35)
        i.putExtra("flowers", treeData[35]);
        // Passes the diameter at breast height (column 5), a standard forestry trunk width measurement
        i.putExtra("dbh", treeData[5]);
        // Passes the tree's height (column 27)
        i.putExtra("height", treeData[27]);
        // Looks up a description string from strings.xml using the tree's botanical name
        i.putExtra("description", getTreeDescription(treeData));
        // Looks up the Morton Arboretum page URL slug for the "More Info" link
        i.putExtra("mortonPage", getMortonPageName(treeData));
        // Calculates and passes the distance from the user to this tree in metres
        i.putExtra("distance", getDistanceToTree(m));

        return i;
    }

    // Looks up the description string for a tree by converting its botanical name to a string resource identifier
    private String getTreeDescription(String[] treeData) {
        // Replaces spaces in the botanical name with underscores to match the string resource naming convention
        String strName = treeData[2].replaceAll(" ", "_");
        // Dynamically resolves the string resource ID — returns 0 if no matching resource exists
        int strId = ctx.getResources().getIdentifier(strName, "string", ctx.getPackageName());

        if (strId != 0)
            // Returns the description text from strings.xml if a matching resource was found
            return ctx.getString(strId);
        else
            // Handles edge cases where the common name doesn't map to a botanical name string resource
            switch (treeData[1]) {
                case "White swamp birch":
                    return ctx.getString(R.string.white_swamp_birch);
                case "Serviceberry":
                    return ctx.getString(R.string.serviceberry);
                case "Okame cherry":
                    return ctx.getString(R.string.okame_cherry);
                default:
                    // Returns an empty string for trees with no description — TreeInfoActivity hides the field in this case
                    return "";
            }
    }

    // Maps a tree's common name to the correct URL slug for its Morton Arboretum page — must be updated when new species are added
    private String getMortonPageName(String[] treeData) {
        // Handles cases where the common name in our CSV doesn't match the slug used on the Morton site
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
            // These species have no Morton Arboretum page — returning "" causes TreeInfoActivity to hide the link
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
                return "";
            default:
                // For most species, the Morton slug matches the lowercased common name with spaces replaced by hyphens
                return treeData[1].toLowerCase().replaceAll(" ", "-");
        }
    }

    // Calculates the distance in metres from the user's current location to the given marker's position
    private double getDistanceToTree(Marker m) {
        // Returns POSITIVE_INFINITY as a sentinel if no GPS fix is available — TreeInfoActivity won't show distance in this case
        if (MainActivity.locationOverlay.getMyLocation() == null)
            return Double.POSITIVE_INFINITY;
        else
            // Uses osmdroid's built-in method which calculates the great-circle distance between two GeoPoints
            return MainActivity.locationOverlay.getMyLocation().distanceToAsDouble(m.getPosition());
    }

    // Reads the compostBins.csv asset and places a brown dot marker for each compost bin location
    public void showCompostBinMarkers() {
        try {
            // Opens the compost bins CSV from the app's assets folder
            InputStream is = ctx.getAssets().open("compostBins.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                // Splits each line respecting quoted fields that may contain embedded commas
                String[] cols = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                // Skips the header row — this CSV doesn't need dynamic column discovery since it has a fixed structure
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                // Extracts the bin name, coordinate string, and info link from the three columns
                String binName = cols[0].replaceAll("^\"|\"$", "");
                String coordsStr = cols[1].replaceAll("^\"|\"$", "");
                String link = cols[2].replaceAll("^\"|\"$", "");

                // Splits the coordinate string (stored as "lat,lng") into separate values
                String[] latLng = coordsStr.split(",");
                double lat = Double.parseDouble(latLng[0].trim());
                double lng = Double.parseDouble(latLng[1].trim());

                // Creates a GeoPoint and a Marker at the bin's position
                GeoPoint point = new GeoPoint(lat, lng);
                Marker marker = new Marker(MainActivity.map);
                marker.setPosition(point);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                // Uses a brown dot to visually distinguish compost bins from tree markers
                marker.setIcon(ContextCompat.getDrawable(ctx, R.drawable.brown_dot));
                marker.setTitle(binName);

                // Shows a dialog with the bin's name, a composting description, and a link to the bin's page
                marker.setOnMarkerClickListener((m, mapView) -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                    builder.setTitle(binName);
                    builder.setMessage("Composting is the process of recycling organic waste, like food scraps and leaves, into nutrient-rich soil.");
                    // Opens the bin's website in the browser so the user can learn more or find hours
                    builder.setPositiveButton("Visit link", (dialog, which) -> {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                        ctx.startActivity(browserIntent);
                    });
                    builder.setNegativeButton("Close", null);
                    builder.show();
                    return true;
                });

                // Adds the marker to the map's overlay list
                MainActivity.map.getOverlays().add(marker);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Listens to Firestore for posts that have GPS coordinates and places a blue dot marker for each one
    public void showPhotoMarkers() {
        try {
            // Attaches a real-time snapshot listener to the posts collection so new photo posts appear on the map without restarting
            FirebaseFirestore.getInstance().collection("posts")
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            Log.w("MapMarkers", "Listen failed.", error);
                            return;
                        }

                        // Null value means the listener was detached — nothing to display
                        if (value == null) return;

                        // Iterates through every post document in the snapshot
                        for (QueryDocumentSnapshot doc : value) {
                            Post post = doc.toObject(Post.class);
                            // Only places a marker if the post has an associated GPS location
                            if (post.hasLocation) {
                                GeoPoint point = new GeoPoint(post.latitude, post.longitude);
                                Marker marker = new Marker(MainActivity.map);
                                marker.setPosition(point);
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                                // Uses a blue dot to distinguish user photo posts from tree and compost markers
                                marker.setIcon(ContextCompat.getDrawable(ctx, R.drawable.blue_dot));

                                // Opens PhotoPopupActivity with the post's data when the blue dot is tapped
                                marker.setOnMarkerClickListener((m, mapView) -> {
                                    Intent i = new Intent(ctx.getApplicationContext(), PhotoPopupActivity.class);
                                    // Passes all fields the popup screen needs to display the post's details
                                    i.putExtra("username", post.username);
                                    i.putExtra("caption", post.caption);
                                    i.putExtra("imageUrl", post.imageUrl);
                                    i.putExtra("latitude", post.latitude);
                                    i.putExtra("longitude", post.longitude);
                                    ctx.startActivity(i);

                                    return true;
                                });

                                // Adds the blue dot marker to the map
                                MainActivity.map.getOverlays().add(marker);
                            }
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
