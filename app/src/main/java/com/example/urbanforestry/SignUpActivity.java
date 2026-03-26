package com.example.urbanforestry;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SignUpActivity extends AppCompatActivity {

    EditText emailEditText, passwordEditText;
    Button signUpButton;
    TextView goToLogin;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();

//        emailEditText    = findViewById(R.id.emailEditText);
//        passwordEditText = findViewById(R.id.passwordEditText);
//        signUpButton     = findViewById(R.id.signUpButton);
//        goToLogin        = findViewById(R.id.goToLogin);
//
//        signUpButton.setOnClickListener(v -> {
//            String email    = emailEditText.getText().toString().trim();
//            String password = passwordEditText.getText().toString().trim();
//
//            if (email.isEmpty() || password.isEmpty()) {
//                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
//                return;
//            }
//            if (password.length() < 6) {
//                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            mAuth.createUserWithEmailAndPassword(email, password)
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();
//                        startActivity(new Intent(this, HomePage.class));
//                        finish();
//                    } else {
//                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
//                    }
//                });
//        });
//
//        goToLogin.setOnClickListener(v -> {
//            startActivity(new Intent(this, LoginActivity.class));
//            finish();
//        });
    }
}