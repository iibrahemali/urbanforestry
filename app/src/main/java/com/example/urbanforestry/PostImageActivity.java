package com.example.urbanforestry;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class PostImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_image);

        String imagePath = getIntent().getStringExtra("imagePath");
        ImageView imagePreview = findViewById(R.id.imagePreview);
        EditText captionEditText = findViewById(R.id.captionEditText);
        Button postBtn = findViewById(R.id.btn_post);

        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            imagePreview.setImageBitmap(bitmap);
        }

        postBtn.setOnClickListener(v -> {
            String caption = captionEditText.getText().toString();

            Intent resultIntent = new Intent();
            resultIntent.putExtra("imagePath", imagePath);
            resultIntent.putExtra("text", caption); // Pass caption as "text"

            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
}