package com.example.urbanforestry;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class SignUpActivity extends AppCompatActivity {

    AutoCompleteTextView emailEditText;
    EditText passwordEditText;
    Button signUpButton;
    TextView goToLogin;
    FirebaseAuth mAuth;

    private static final String[] EMAIL_DOMAINS = {
            "gmail.com", "outlook.com", "yahoo.com", "hotmail.com", "icloud.com"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();

        emailEditText    = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        signUpButton     = findViewById(R.id.signUpButton);
        goToLogin        = findViewById(R.id.goToLogin);

        setupEmailAutocomplete();

        signUpButton.setOnClickListener(v -> {
            String email    = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, HomePage.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
        });

        goToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void setupEmailAutocomplete() {
        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String filter = s.toString();
                if (filter.contains("@")) {
                    String prefix = filter.substring(0, filter.indexOf("@") + 1);
                    String suffix = filter.substring(filter.indexOf("@") + 1);
                    
                    List<String> suggestions = new ArrayList<>();
                    for (String domain : EMAIL_DOMAINS) {
                        if (domain.startsWith(suffix)) {
                            suggestions.add(prefix + domain);
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            SignUpActivity.this,
                            android.R.layout.simple_dropdown_item_1line,
                            suggestions
                    );
                    emailEditText.setAdapter(adapter);
                    // Force the dropdown to show if there are matches
                    if (!suggestions.isEmpty() && !suffix.equals(suggestions.get(0).substring(prefix.length()))) {
                         emailEditText.showDropDown();
                    }
                } else {
                    emailEditText.setAdapter(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        emailEditText.setThreshold(1);
    }
}