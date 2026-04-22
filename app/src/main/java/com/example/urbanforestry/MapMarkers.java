package com.example.urbanforestry;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MapMarkers {
    private final Context ctx;

    public MapMarkers(Context ctx) {
        this.ctx = ctx;
    }

    public void showTreeMarkers() {
        try {
            InputStream is = ctx.getAssets().open("trees.csv");
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

                Marker marker = new Marker(MainActivity.map);
                marker.setPosition(point);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);

                marker.setRelatedObject(cols.clone());

                // Make it a small green dot
                marker.setIcon(ContextCompat.getDrawable(ctx, R.drawable.green_dot));


                marker.setOnMarkerClickListener((m, mapView) -> {
                    String[] treeData = (String[]) m.getRelatedObject();
                    String treeId = m.getPosition().getLatitude() + "," + m.getPosition().getLongitude();

                    // Update goal progress
                    if (MainActivity.locationOverlay.getMyLocation() != null) {
                        double distance = MainActivity.locationOverlay.getMyLocation().distanceToAsDouble(m.getPosition());
                        // Trees can only be "discovered" if the user is within 10m
                        if (distance <= 10.0)
                            // Check if discovering the tree makes progress toward any goals
                            Missions.updateGoalProgress(treeId, treeData, ctx);
                    }

                    // Show the tree info
                    Intent i = getTreeInfoIntent(treeData, m);
                    ctx.startActivity(i);

                    return true;
                });

                MainActivity.map.getOverlays().add(marker);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @NonNull
    private Intent getTreeInfoIntent(String[] treeData, Marker m) {
        Intent i = new Intent(ctx.getApplicationContext(), TreeInfoActivity.class);

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
        i.putExtra("description", getTreeDescription(treeData));
        i.putExtra("mortonPage", getMortonPageName(treeData));
        i.putExtra("distance", getDistanceToTree(m));

        return i;
    }

    private String getTreeDescription(String[] treeData) {
        // Get the tree's botanical name, and return the corresponding description in strings.xml

        String strName = treeData[2].replaceAll(" ", "_");
        int strId = ctx.getResources().getIdentifier(strName, "string", ctx.getPackageName());

        if (strId != 0)
            return ctx.getString(strId);
        else
            // Handle cases without botanical species names
            switch (treeData[1]) {
                case "White swamp birch":
                    return ctx.getString(R.string.white_swamp_birch);
                case "Serviceberry":
                    return ctx.getString(R.string.serviceberry);
                case "Okame cherry":
                    return ctx.getString(R.string.okame_cherry);
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

    private double getDistanceToTree(Marker m) {
        if (MainActivity.locationOverlay.getMyLocation() == null)
            return Double.POSITIVE_INFINITY;
        else
            return MainActivity.locationOverlay.getMyLocation().distanceToAsDouble(m.getPosition());
    }

    public void showCompostBinMarkers() {
        try {
            InputStream is = ctx.getAssets().open("compostBins.csv");
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
                Marker marker = new Marker(MainActivity.map);
                marker.setPosition(point);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                marker.setIcon(ContextCompat.getDrawable(ctx, R.drawable.brown_dot));
                marker.setTitle(binName);

                marker.setOnMarkerClickListener((m, mapView) -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                    builder.setTitle(binName);
                    builder.setMessage("Composting is the process of recycling organic waste, like food scraps and leaves, into nutrient-rich soil.");
                    builder.setPositiveButton("Visit link", (dialog, which) -> {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                        ctx.startActivity(browserIntent);
                    });
                    builder.setNegativeButton("Close", null);
                    builder.show();
                    return true;
                });

                MainActivity.map.getOverlays().add(marker);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
