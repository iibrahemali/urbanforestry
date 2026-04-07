package com.example.urbanforestry;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PostTextActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_text);

        EditText editText = findViewById(R.id.editText);
        Button postBtn = findViewById(R.id.btn_post);

        postBtn.setOnClickListener(v -> {
            String text = editText.getText().toString();

            Intent resultIntent = new Intent();
            resultIntent.putExtra("text", text);

            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
}