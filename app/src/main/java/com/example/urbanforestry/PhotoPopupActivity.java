package com.example.urbanforestry;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;

public class PhotoPopupActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set seasonal dialog theme before onCreate to maintain dialog behavior
        setTheme(SeasonManager.getSeasonDialogTheme(SeasonManager.getSeasonPref(this)));

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_photo_popup);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent i = getIntent();

        TextView username = findViewById(R.id.username);
        username.append(i.getStringExtra("username"));

        TextView caption = findViewById(R.id.caption);
        caption.setText(i.getStringExtra("caption"));

        ImageView image = findViewById(R.id.image);
        Glide.with(this).load(i.getStringExtra("imageUrl")).into(image);

        Button getDirections = findViewById(R.id.getDirections);
        getDirections.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), MainActivity.class);
            intent.putExtra("destLat", i.getDoubleExtra("latitude", 0));
            intent.putExtra("destLng", i.getDoubleExtra("longitude", 0));
            intent.putExtra("getDirections", true);

            // Add info for the toggle button dialog
            intent.putExtra("postUser", i.getStringExtra("username"));
            intent.putExtra("postText", i.getStringExtra("caption"));

            v.getContext().startActivity(intent);
        });
    }
}