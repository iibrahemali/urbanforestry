// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports Intent, which is used to navigate between Activities (screens)
import android.content.Intent;
// Imports Bundle, a key-value container Android uses to pass saved state into onCreate
import android.os.Bundle;
// Imports Button, a UI widget the user can tap to trigger an action
import android.widget.Button;
// Imports EditText, a UI widget that lets the user type input
import android.widget.EditText;
// Imports TextView, a UI widget that displays non-editable text (used here as a clickable link)
import android.widget.TextView;
// Imports Toast, a lightweight popup message that appears briefly on screen
import android.widget.Toast;

// Imports AppCompatActivity, the base class for activities that support modern Android features and backwards compatibility
import androidx.appcompat.app.AppCompatActivity;

// Imports FirebaseAuth, the Firebase SDK class that handles authentication (sign in, sign up, sign out)
import com.google.firebase.auth.FirebaseAuth;

// Declares LoginActivity as a public class that extends AppCompatActivity, making it a screen in the app
public class LoginActivity extends AppCompatActivity {

    // Declares two EditText fields for user input — email and password — at the class level so all methods can access them
    EditText emailEditText, passwordEditText;
    // Declares the login button that the user taps to submit their credentials
    Button loginButton;
    // Declares a TextView that acts as a navigation link to the SignUp screen
    TextView goToSignUp;
    // Declares a FirebaseAuth instance used to call Firebase authentication methods
    FirebaseAuth mAuth;

    // Overrides onCreate, the lifecycle method Android calls when this Activity is first created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Applies the seasonal theme before the layout is inflated — must be called before super.onCreate() or it won't take effect
        setTheme(SeasonManager.getSeasonTheme(SeasonManager.getSeasonPref(this)));

        // Calls the parent class onCreate to complete Android's standard Activity setup
        super.onCreate(savedInstanceState);
        // Inflates the XML layout file activity_login.xml and sets it as the visible UI for this screen
        setContentView(R.layout.activity_login);

        // Gets the singleton FirebaseAuth instance — reusing it avoids creating unnecessary connections
        mAuth = FirebaseAuth.getInstance();

        // Links the emailEditText variable to the corresponding View in the XML layout using its resource ID
        emailEditText = findViewById(R.id.emailEditText);
        // Links the passwordEditText variable to its XML View so we can read what the user typed
        passwordEditText = findViewById(R.id.passwordEditText);
        // Links the loginButton variable to its XML View so we can attach a click listener
        loginButton = findViewById(R.id.loginButton);
        // Links the goToSignUp variable to its XML View so we can handle navigation to the sign-up screen
        goToSignUp = findViewById(R.id.goToSignUp);

        // Attaches a click listener to loginButton using a lambda — runs the block every time the button is tapped
        loginButton.setOnClickListener(v -> {
            // Reads the email field, converts the Editable to a String, and trims whitespace to avoid accidental spaces breaking validation
            String email = emailEditText.getText().toString().trim();
            // Reads the password field the same way — trimming ensures a space-only entry is treated as empty
            String password = passwordEditText.getText().toString().trim();

            // Checks if either field is blank before calling Firebase — Firebase would throw an error on empty credentials, so we catch it early
            if (email.isEmpty() || password.isEmpty()) {
                // Shows a short popup telling the user to fill in both fields, since silently failing would confuse them
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                // Exits the click listener early so the Firebase call below is never reached with invalid input
                return;
            }

            // Calls Firebase to authenticate the user with their email and password asynchronously
            mAuth.signInWithEmailAndPassword(email, password)
                    // Registers a callback that fires when the sign-in attempt completes, whether successful or not
                    .addOnCompleteListener(task -> {
                        // Checks if Firebase authentication succeeded
                        if (task.isSuccessful()) {
                            // Navigates to MainActivity now that the user is verified — Intent specifies the destination screen
                            startActivity(new Intent(this, MainActivity.class));
                            // Removes LoginActivity from the back stack so the user can't press Back to return to the login screen
                            finish();
                        } else {
                            // Displays the Firebase error message (e.g. wrong password, no account) so the user knows what went wrong
                            Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // Attaches a click listener to the "Go to Sign Up" link using a lambda
        goToSignUp.setOnClickListener(v -> {
            // Navigates to SignUpActivity — no finish() here so the user can press Back to return to login
            startActivity(new Intent(this, SignUpActivity.class));
        });
    }
}