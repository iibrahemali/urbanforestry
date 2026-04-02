package com.example.urbanforestry;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
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

        TextView tv = findViewById(R.id.phone_number_tv);
        tv.setOnClickListener(v -> {
            //get the number
            TextView tv2 = (TextView) v;
            String digits = tv2.getText().toString();

            digits.replace("-", "");
            String number = "tel:" + digits;
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(number));
            startActivity(intent);
        });

        ImageButton ib = findViewById(R.id.share_button);
        ib.setOnClickListener(v -> {
            EditText numET = findViewById(R.id.sharable_text_et);
            String luckyNum = numET.getText().toString();

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, "My lucky number is: " + luckyNum);
            intent.setType("text/plain");
            startActivity(Intent.createChooser(intent, "Share via"));
        });

    }
}