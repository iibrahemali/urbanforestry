package com.example.urbanforestry;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class TreeInfo extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set seasonal dialog theme before onCreate to maintain dialog behavior
        setTheme(SeasonManager.getSeasonDialogTheme(SeasonManager.getCurrentSeason()));

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tree_info);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent i = getIntent();

        TextView commonName = findViewById(R.id.commonName);
        commonName.setText(i.getStringExtra("commonName"));

        TextView botanicalName = findViewById(R.id.botanicalName);
        botanicalName.setText(i.getStringExtra("botanicalName"));

        TextView family = findViewById(R.id.family);
        if (i.getStringExtra("familyCommon").equals("0"))
            family.append("Not listed");
        else
            family.append(i.getStringExtra("familyCommon") + " (" + i.getStringExtra("familyBotanical") + ")");

        TextView nativeOrCultivated = findViewById(R.id.nativeOrCultivated);
        if (i.getStringExtra("nativeOrCultivated").equals("0"))
            nativeOrCultivated.append("Not listed");
        else
            nativeOrCultivated.append(i.getStringExtra("nativeOrCultivated"));

        TextView wildlife = findViewById(R.id.wildlife);
        if (i.getStringExtra("wildlife").equals("0"))
            wildlife.append("N/A");
        else
            wildlife.append(i.getStringExtra("wildlife"));

        TextView fallColors = findViewById(R.id.fallColors);
        if (i.getStringExtra("fallColors").equals("0"))
            fallColors.append("N/A");
        else
            fallColors.append(i.getStringExtra("fallColors"));

        TextView flowers = findViewById(R.id.flowers);
        if (i.getStringExtra("flowers").equals("0"))
            flowers.append("N/A");
        else
            flowers.append(i.getStringExtra("flowers"));

        TextView dbh = findViewById(R.id.dbh);
        dbh.append(i.getStringExtra("dbh"));

        TextView height = findViewById(R.id.height);
        if (i.getStringExtra("height").equals("0"))
            height.append("Not listed");
        else
            height.append(i.getStringExtra("height"));

        TextView description = findViewById(R.id.description);
        if (!i.getStringExtra("description").isEmpty())
            description.setText(i.getStringExtra("description"));
        else
            description.setVisibility(View.GONE);

        TextView mortonLink = findViewById(R.id.mortonLink);
        if (!i.getStringExtra("mortonPage").isEmpty()) {
            String url = "https://mortonarb.org/plant-and-protect/trees-and-plants/"
                    + i.getStringExtra("mortonPage") + "/";
            String linkHtml = "<a href=\"" + url + "\">More info from the Morton Arboretum</a>";
            mortonLink.setText(Html.fromHtml(linkHtml, Html.FROM_HTML_MODE_COMPACT));
            mortonLink.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            mortonLink.setVisibility(View.GONE);
        }

        TextView distance = findViewById(R.id.distance);
        double distanceDouble = i.getDoubleExtra("distance", Double.POSITIVE_INFINITY);
        if (distanceDouble != Double.POSITIVE_INFINITY)
            distance.setText("Distance: " + (int) distanceDouble + " m");
    }
}
