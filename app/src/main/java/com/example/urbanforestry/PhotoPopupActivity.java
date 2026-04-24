// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports Intent for navigating to MainActivity when the user taps "Get Directions"
import android.content.Intent;
// Imports Bundle, the key-value container Android passes to onCreate with any saved state
import android.os.Bundle;
// Imports Button for the "Get Directions" button
import android.widget.Button;
// Imports ImageView to display the photo from the post
import android.widget.ImageView;
// Imports TextView to display the username and caption
import android.widget.TextView;
// Imports Toast for brief error or status messages
import android.widget.Toast;

// Imports EdgeToEdge to render the app behind system bars for a full-screen look
import androidx.activity.EdgeToEdge;
// Imports AppCompatActivity, the base class that provides backwards-compatible Activity features
import androidx.appcompat.app.AppCompatActivity;
// Imports Insets and related classes to apply correct padding around system bars
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Imports Glide to load the post's image from a Firebase Storage URL into the ImageView
import com.bumptech.glide.Glide;

// Declares PhotoPopupActivity as a dialog-style screen that shows a single post's photo and details when tapped from the map
public class PhotoPopupActivity extends AppCompatActivity {
    // Overrides onCreate, called when this Activity is first created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Applies the dialog-style seasonal theme so this screen appears as an overlay rather than a full-screen Activity
        setTheme(SeasonManager.getSeasonDialogTheme(SeasonManager.getSeasonPref(this)));

        // Calls the parent class onCreate to complete Android's standard Activity setup
        super.onCreate(savedInstanceState);
        // Enables edge-to-edge rendering so the content draws behind system bars
        EdgeToEdge.enable(this);
        // Inflates activity_photo_popup.xml and sets it as this screen's UI
        setContentView(R.layout.activity_photo_popup);
        // Applies system bar insets as padding so content isn't hidden behind the status bar or nav bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Gets the Intent that launched this Activity — it contains the post data passed from MapMarkers
        Intent i = getIntent();

        // Finds the username TextView and appends the poster's name to any existing prefix text in the layout
        TextView username = findViewById(R.id.username);
        username.append(i.getStringExtra("username"));

        // Finds the caption TextView and sets the post's text description
        TextView caption = findViewById(R.id.caption);
        caption.setText(i.getStringExtra("caption"));

        // Finds the ImageView and loads the post image from its Firebase Storage URL using Glide
        ImageView image = findViewById(R.id.image);
        Glide.with(this).load(i.getStringExtra("imageUrl")).into(image);

        // Finds the "Get Directions" button and attaches a listener to navigate back to the map with routing data
        Button getDirections = findViewById(R.id.getDirections);
        getDirections.setOnClickListener(v -> {
            // Creates an Intent targeting MainActivity, which handles the routing logic
            Intent intent = new Intent(v.getContext(), MainActivity.class);
            // Passes the post's GPS coordinates so MainActivity knows where to route to
            intent.putExtra("destLat", i.getDoubleExtra("latitude", 0));
            intent.putExtra("destLng", i.getDoubleExtra("longitude", 0));
            // Signals MainActivity to start a route calculation as soon as it receives this intent
            intent.putExtra("getDirections", true);

            // Passes the username and caption so the route toggle button can show post info in its dialog
            intent.putExtra("postUser", i.getStringExtra("username"));
            intent.putExtra("postText", i.getStringExtra("caption"));

            // Starts MainActivity with the routing data — it will handle building and displaying the route
            v.getContext().startActivity(intent);
        });
    }
}
