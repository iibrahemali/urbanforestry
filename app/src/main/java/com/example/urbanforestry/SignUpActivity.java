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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    EditText nameEditText, usernameEditText, passwordEditText;
    AutoCompleteTextView emailEditText;
    Button signUpButton;
    TextView goToLogin;
    FirebaseAuth mAuth;
    DatabaseReference mDatabase;

    private static final String[] EMAIL_DOMAINS = {
            "gmail.com", "outlook.com", "yahoo.com", "hotmail.com", "icloud.com", "fandm.edu", "cityoflancasterpa.gov"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        nameEditText = findViewById(R.id.nameEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText    = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        signUpButton     = findViewById(R.id.signUpButton);
        goToLogin        = findViewById(R.id.goToLogin);

        setupEmailAutocomplete();

        signUpButton.setOnClickListener(v -> {
            String name = nameEditText.getText().toString().trim();
            String username = usernameEditText.getText().toString().trim();
            String email    = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (name.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
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
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToDatabase(user.getUid(), name, username, email);
                        }
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

    private void saveUserToDatabase(String userId, String name, String username, String email) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("username", username);
        userMap.put("email", email);

        mDatabase.child("users").child(userId).setValue(userMap)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, HomePage.class));
                    finish();
                } else {
                    Toast.makeText(this, "Database Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
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
                            R.layout.dropdown_item,
                            suggestions
                    );
                    emailEditText.setAdapter(adapter);
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