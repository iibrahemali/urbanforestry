// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports Intent so we can read the tree data extras passed by MapMarkers
import android.content.Intent;
// Imports Bundle, the key-value container Android passes to onCreate with any saved state
import android.os.Bundle;
// Imports Html to render an HTML anchor tag as a clickable hyperlink in a TextView
import android.text.Html;
// Imports LinkMovementMethod to make the hyperlink in the Morton Arboretum TextView actually tappable
import android.text.method.LinkMovementMethod;
// Imports View to hide a TextView when its content is empty
import android.view.View;
// Imports TextView to display all the tree's botanical and ecological data
import android.widget.TextView;

// Imports EdgeToEdge to render the app behind system bars for a full-screen look
import androidx.activity.EdgeToEdge;
// Imports AppCompatActivity, the base class that provides backwards-compatible Activity features
import androidx.appcompat.app.AppCompatActivity;
// Imports Insets and related classes to apply correct padding around system bars
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Declares TreeInfoActivity as a dialog-style detail screen that displays data about a specific tree tapped on the map
public class TreeInfoActivity extends AppCompatActivity {

    // Overrides onCreate, called when this Activity is first created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Uses the dialog-style seasonal theme so this screen appears as an overlay rather than a full screen
        setTheme(SeasonManager.getSeasonDialogTheme(SeasonManager.getSeasonPref(this)));

        // Calls the parent class onCreate to complete Android's standard Activity setup
        super.onCreate(savedInstanceState);
        // Enables edge-to-edge rendering so the content draws behind system bars
        EdgeToEdge.enable(this);
        // Inflates activity_tree_info.xml and sets it as this screen's UI
        setContentView(R.layout.activity_tree_info);
        // Applies system bar insets as padding so content isn't hidden behind the status bar or nav bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Gets the Intent that launched this Activity — it contains all tree data as extras
        Intent i = getIntent();

        // Displays the tree's common name (e.g. "Red Maple") passed from the CSV via MapMarkers
        TextView commonName = findViewById(R.id.commonName);
        commonName.setText(i.getStringExtra("commonName"));

        // Displays the tree's scientific/botanical name (e.g. "Acer rubrum")
        TextView botanicalName = findViewById(R.id.botanicalName);
        botanicalName.setText(i.getStringExtra("botanicalName"));

        // Displays the tree's plant family — shows "Not listed" if the CSV column was "0" (a sentinel value for missing data)
        TextView family = findViewById(R.id.family);
        if (i.getStringExtra("familyCommon").equals("0"))
            family.append("Not listed");
        else
            // Combines the common family name and botanical family name in parentheses for completeness
            family.append(i.getStringExtra("familyCommon") + " (" + i.getStringExtra("familyBotanical") + ")");

        // Displays whether the tree is native or cultivated — shows "Not listed" if the CSV value was "0"
        TextView nativeOrCultivated = findViewById(R.id.nativeOrCultivated);
        if (i.getStringExtra("nativeOrCultivated").equals("0"))
            nativeOrCultivated.append("Not listed");
        else
            nativeOrCultivated.append(i.getStringExtra("nativeOrCultivated"));

        // Displays the wildlife value the tree provides — shows "N/A" when the CSV column was "0"
        TextView wildlife = findViewById(R.id.wildlife);
        if (i.getStringExtra("wildlife").equals("0"))
            wildlife.append("N/A");
        else
            wildlife.append(i.getStringExtra("wildlife"));

        // Displays the tree's fall foliage colors — shows "N/A" when the CSV column was "0"
        TextView fallColors = findViewById(R.id.fallColors);
        if (i.getStringExtra("fallColors").equals("0"))
            fallColors.append("N/A");
        else
            fallColors.append(i.getStringExtra("fallColors"));

        // Displays whether the tree produces flowers — shows "N/A" when the CSV column was "0"
        TextView flowers = findViewById(R.id.flowers);
        if (i.getStringExtra("flowers").equals("0"))
            flowers.append("N/A");
        else
            flowers.append(i.getStringExtra("flowers"));

        // Displays the tree's diameter at breast height (DBH) — a standard forestry measurement of trunk width
        TextView dbh = findViewById(R.id.dbh);
        dbh.append(i.getStringExtra("dbh"));

        // Displays the tree's height — shows "Not listed" when the CSV column was "0"
        TextView height = findViewById(R.id.height);
        if (i.getStringExtra("height").equals("0"))
            height.append("Not listed");
        else
            height.append(i.getStringExtra("height"));

        // Displays the tree's description text if one was found — hides the field entirely if there's nothing to show
        TextView description = findViewById(R.id.description);
        if (!i.getStringExtra("description").isEmpty())
            description.setText(i.getStringExtra("description"));
        else
            // Hides the description TextView rather than showing an empty field, keeping the layout clean
            description.setVisibility(View.GONE);

        // Sets up a clickable link to the Morton Arboretum's page for this tree — hides the link if no page exists
        TextView mortonLink = findViewById(R.id.mortonLink);
        if (!i.getStringExtra("mortonPage").isEmpty()) {
            // Builds the full Morton Arboretum URL using the page name slug passed from MapMarkers
            String url = "https://mortonarb.org/plant-and-protect/trees-and-plants/"
                    + i.getStringExtra("mortonPage") + "/";
            // Wraps the URL in an HTML anchor tag so it renders as a tappable hyperlink
            String linkHtml = "<a href=\"" + url + "\">More info from the Morton Arboretum</a>";
            // Parses the HTML string into a formatted Spanned object that TextView can render
            mortonLink.setText(Html.fromHtml(linkHtml, Html.FROM_HTML_MODE_COMPACT));
            // Enables link clicking — without this, the link would display but not open when tapped
            mortonLink.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            // Hides the link entirely when no Morton page exists for this tree species
            mortonLink.setVisibility(View.GONE);
        }

        // Reads the distance to the tree passed from MapMarkers (calculated using the user's GPS location)
        TextView distance = findViewById(R.id.distance);
        double distanceDouble = i.getDoubleExtra("distance", Double.POSITIVE_INFINITY);
        // Only shows the distance if the user's location was known — POSITIVE_INFINITY is used as a sentinel for "unknown"
        if (distanceDouble != Double.POSITIVE_INFINITY)
            distance.setText("Distance: " + (int) distanceDouble + " m");
    }
}
