package com.example.urbanforestry;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class WelcomePage extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private Button btnLogin, btnSignUp, btnMap, btnSignOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcome_page);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        mAuth = FirebaseAuth.getInstance();

        btnLogin = findViewById(R.id.goToLoginButton);
        btnSignUp = findViewById(R.id.goToSignUpButton);
        btnMap = findViewById(R.id.gotoMapButton);
        // We'll reuse the signup button for signout when logged in
        btnSignOut = findViewById(R.id.goToSignUpButton); 

        updateUI(mAuth.getCurrentUser());

        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });

        btnSignUp.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() != null) {
                // Currently logged in, so this button acts as Sign Out
                mAuth.signOut();
                updateUI(null);
            } else {
                startActivity(new Intent(this, SignUpActivity.class));
            }
        });

        btnMap.setOnClickListener(v -> {
            startActivity(new Intent(this, HomePage.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check session again when returning to this page
        updateUI(mAuth.getCurrentUser());
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            // User is logged in
            btnLogin.setVisibility(View.GONE);
            btnMap.setVisibility(View.VISIBLE);
            
            btnSignUp.setText("Sign Out");
            btnSignUp.setVisibility(View.VISIBLE);
        } else {
            // User is NOT logged in
            btnLogin.setVisibility(View.VISIBLE);
            btnSignUp.setText("Sign Up");
            btnSignUp.setVisibility(View.VISIBLE);
            
            btnMap.setVisibility(View.GONE);
        }
    }
}