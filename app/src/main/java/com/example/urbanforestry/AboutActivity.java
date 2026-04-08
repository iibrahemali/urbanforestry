package com.example.urbanforestry;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_about);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Contact Michael Meyer
        findViewById(R.id.phone_michael).setOnClickListener(v -> dialNumber("(717) 517-0864"));
        findViewById(R.id.email_michael).setOnClickListener(v -> sendEmail("mmeyer@cityoflancasterpa.gov"));

        // Contact Rick Anderer
        findViewById(R.id.phone_rick).setOnClickListener(v -> dialNumber("(717) 291-4846"));
        findViewById(R.id.email_rick).setOnClickListener(v -> sendEmail("randerer@cityoflancasterpa.gov"));

        // Social Media Links
        findViewById(R.id.btn_linkedin).setOnClickListener(v -> openUrl("https://www.linkedin.com/company/cityoflancasterpa"));
        findViewById(R.id.btn_youtube).setOnClickListener(v -> openUrl("https://www.youtube.com/c/cityoflancasterpagov"));
        findViewById(R.id.btn_instagram).setOnClickListener(v -> openUrl("https://www.instagram.com/cityoflancpa"));
        findViewById(R.id.btn_facebook).setOnClickListener(v -> openUrl("https://www.facebook.com/CityOfLancasterPA/"));

    }

    private void dialNumber(String digits) {
        String number = "tel:" + digits.replaceAll("[^0-9]", "");
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(number));
        startActivity(intent);
    }

    private void sendEmail(String email) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + email));
        startActivity(intent);
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }
}
