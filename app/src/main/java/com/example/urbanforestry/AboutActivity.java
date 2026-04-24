// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports Intent so we can launch the phone dialer and email app from this screen
import android.content.Intent;
// Imports Uri to parse phone numbers and email addresses into the format that Intent actions expect
import android.net.Uri;
// Imports Bundle, the key-value container Android passes to onCreate with any saved state
import android.os.Bundle;
// Imports EditText (unused here but part of standard imports kept for potential future use)
import android.widget.EditText;
// Imports ImageButton for the social media icon buttons
import android.widget.ImageButton;
// Imports TextView (unused here but part of standard imports)
import android.widget.TextView;

// Imports EdgeToEdge to render the app behind system bars for a full-screen look
import androidx.activity.EdgeToEdge;
// Imports AppCompatActivity, the base class that provides backwards-compatible Activity features
import androidx.appcompat.app.AppCompatActivity;
// Imports Insets and related classes to apply correct padding around system bars
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Declares AboutActivity as the screen showing contact information and social media links for the Urban Forestry department
public class AboutActivity extends AppCompatActivity {

    // Overrides onCreate, called when this Activity is first created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Applies the seasonal theme before layout inflation so the screen matches the rest of the app
        setTheme(SeasonManager.getSeasonTheme(SeasonManager.getSeasonPref(this)));

        // Calls the parent class onCreate to complete Android's standard Activity setup
        super.onCreate(savedInstanceState);
        // Enables edge-to-edge rendering so the content draws behind system bars
        EdgeToEdge.enable(this);
        // Inflates activity_about.xml and sets it as this screen's UI
        setContentView(R.layout.activity_about);
        // Applies padding equal to the system bar insets so content isn't hidden behind the status bar or nav bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Tapping Michael's phone button opens the device's dialer pre-filled with his number
        findViewById(R.id.phone_michael).setOnClickListener(v -> dialNumber("(717) 517-0864"));
        // Tapping Michael's email button opens the default email app pre-filled with his address
        findViewById(R.id.email_michael).setOnClickListener(v -> sendEmail("mmeyer@cityoflancasterpa.gov"));

        // Tapping Rick's phone button opens the dialer pre-filled with his number
        findViewById(R.id.phone_rick).setOnClickListener(v -> dialNumber("(717) 291-4846"));
        // Tapping Rick's email button opens the email app pre-filled with his address
        findViewById(R.id.email_rick).setOnClickListener(v -> sendEmail("randerer@cityoflancasterpa.gov"));

        // Social media buttons — each opens the corresponding platform's page in the browser
        findViewById(R.id.btn_linkedin).setOnClickListener(v -> openUrl("https://www.linkedin.com/company/cityoflancasterpa"));
        findViewById(R.id.btn_youtube).setOnClickListener(v -> openUrl("https://www.youtube.com/c/cityoflancasterpagov"));
        findViewById(R.id.btn_instagram).setOnClickListener(v -> openUrl("https://www.instagram.com/cityoflancpa"));
        findViewById(R.id.btn_facebook).setOnClickListener(v -> openUrl("https://www.facebook.com/CityOfLancasterPA/"));

    }

    // Launches the phone dialer with the given number pre-filled — uses ACTION_DIAL rather than ACTION_CALL so the user confirms before dialing
    private void dialNumber(String digits) {
        // Strips all non-numeric characters from the display string (parentheses, dashes, spaces) to build a clean "tel:" URI
        String number = "tel:" + digits.replaceAll("[^0-9]", "");
        // Creates an intent that opens the dialer app with the number pre-filled
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(number));
        startActivity(intent);
    }

    // Launches the default email client with the "To" field pre-filled — uses ACTION_SENDTO with a mailto: URI
    private void sendEmail(String email) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        // Parses the email address into a mailto: URI so Android routes it to the correct email app
        intent.setData(Uri.parse("mailto:" + email));
        startActivity(intent);
    }

    // Opens the given URL in the device's default browser
    private void openUrl(String url) {
        // ACTION_VIEW with a parsed http/https URI tells Android to open it in a browser
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }
}
