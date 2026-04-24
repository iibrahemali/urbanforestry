// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports Intent for navigating to other Activities after successful sign-up
import android.content.Intent;
// Imports Bundle, the key-value container Android passes to onCreate with any saved state
import android.os.Bundle;
// Imports Editable, required by the TextWatcher interface even when not used in all callbacks
import android.text.Editable;
// Imports TextWatcher to listen for text changes in the email field so we can show domain suggestions
import android.text.TextWatcher;
// Imports ArrayAdapter to supply the autocomplete dropdown with the list of email domain suggestions
import android.widget.ArrayAdapter;
// Imports AutoCompleteTextView, a special EditText that shows a dropdown of suggestions as the user types
import android.widget.AutoCompleteTextView;
// Imports Button for the sign-up and navigation buttons
import android.widget.Button;
// Imports EditText for the name, username, and password input fields
import android.widget.EditText;
// Imports TextView used as a clickable "already have an account?" link
import android.widget.TextView;
// Imports Toast to show brief feedback messages to the user
import android.widget.Toast;

// Imports AppCompatActivity, the base class that provides backwards-compatible Activity features
import androidx.appcompat.app.AppCompatActivity;

// Imports FirebaseAuth to create a new account with email and password
import com.google.firebase.auth.FirebaseAuth;
// Imports FirebaseUser to get the newly created user's UID after successful registration
import com.google.firebase.auth.FirebaseUser;

// Imports ArrayList and List for building the list of autocomplete email suggestions
import java.util.ArrayList;
import java.util.List;

// Declares SignUpActivity as the screen where new users create an account
public class SignUpActivity extends AppCompatActivity {

    // Declares the input fields for the user's personal and account details
    EditText nameEditText, usernameEditText, passwordEditText;
    // Declares the email field as AutoCompleteTextView so it can show domain suggestions while typing
    AutoCompleteTextView emailEditText;
    // Declares the button that submits the sign-up form
    Button signUpButton;
    // Declares the link that takes the user back to the login screen
    TextView goToLogin;
    // Declares the FirebaseAuth instance used to create the account
    FirebaseAuth mAuth;
    // Declares the repository that handles saving user profile data to the database
    UserRepository userRepository;

    // Defines a set of common email domains shown as autocomplete suggestions to speed up email entry
    private static final String[] EMAIL_DOMAINS = {
            "gmail.com", "outlook.com", "yahoo.com", "hotmail.com", "icloud.com", "fandm.edu", "cityoflancasterpa.gov"
    };

    // Overrides onCreate, called when this Activity is first created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Applies the seasonal theme before layout inflation so colors and styles load correctly
        setTheme(SeasonManager.getSeasonTheme(SeasonManager.getSeasonPref(this)));

        // Calls the parent class onCreate to complete Android's standard Activity setup
        super.onCreate(savedInstanceState);
        // Inflates activity_sign_up.xml and sets it as this screen's UI
        setContentView(R.layout.activity_sign_up);

        // Gets the singleton FirebaseAuth instance to handle account creation
        mAuth = FirebaseAuth.getInstance();
        // Instantiates the repository responsible for writing profile data to Firebase
        userRepository = new UserRepository();

        // Links each UI field variable to its corresponding View in the XML layout
        nameEditText = findViewById(R.id.nameEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        signUpButton = findViewById(R.id.signUpButton);
        goToLogin = findViewById(R.id.goToLogin);

        // Attaches the email autocomplete behavior before the user starts typing
        setupEmailAutocomplete();

        // Attaches a click listener to the sign-up button using a lambda
        signUpButton.setOnClickListener(v -> {
            // Reads and trims all input fields to prevent whitespace-only entries from passing validation
            String name = nameEditText.getText().toString().trim();
            String username = usernameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            // Validates that all fields are filled — Firebase would fail with unclear errors on empty inputs
            if (name.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            // Validates minimum password length — Firebase requires at least 6 characters, so we catch this early with a clearer message
            if (password.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            // Calls Firebase to create a new account with the provided email and password asynchronously
            mAuth.createUserWithEmailAndPassword(email, password)
                    // Registers a callback that fires when the account creation attempt completes
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Gets the newly created FirebaseUser so we can access their UID
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Saves the user's profile details to the database using the UID as the key
                                saveUserToDatabase(user.getUid(), name, username, email);
                            }
                        } else {
                            // Shows the Firebase error (e.g. email already in use) so the user knows why it failed
                            Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // Navigates back to LoginActivity when the user taps the "Already have an account?" link
        goToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            // Finishes SignUpActivity so the user can't swipe back to it from LoginActivity
            finish();
        });
    }

    // Saves the new user's name, username, and email to Firebase after account creation succeeds
    private void saveUserToDatabase(String userId, String name, String username, String email) {
        // Calls the repository to write the profile data — separating this from the auth call keeps concerns clean
        userRepository.createUserProfile(userId, name, username, email)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                    // Navigates to the main map screen now that the account is fully set up
                    startActivity(new Intent(this, MainActivity.class));
                    // Finishes all previous screens so the user can't navigate back to sign-up
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Shows the database error — the auth account was created but profile data couldn't be saved
                    Toast.makeText(this, "Database Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Sets up the email field to suggest complete email addresses as the user types after the "@" symbol
    private void setupEmailAutocomplete() {
        // Attaches a TextWatcher to detect every character change in the email field
        emailEditText.addTextChangedListener(new TextWatcher() {
            // Not used — no action needed before the text changes
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            // Called on every keystroke — rebuilds the suggestion list based on what the user has typed so far
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String filter = s.toString();
                // Only show suggestions once the user has typed "@", since that's when the domain part begins
                if (filter.contains("@")) {
                    // Extracts everything up to and including "@" to use as the prefix for suggestions
                    String prefix = filter.substring(0, filter.indexOf("@") + 1);
                    // Extracts what the user has typed after "@" to filter the domain list
                    String suffix = filter.substring(filter.indexOf("@") + 1);

                    // Builds a filtered list of domains that start with what the user has typed after "@"
                    List<String> suggestions = new ArrayList<>();
                    for (String domain : EMAIL_DOMAINS) {
                        if (domain.startsWith(suffix)) {
                            // Prepends the user's prefix so the suggestion shows the full email address
                            suggestions.add(prefix + domain);
                        }
                    }

                    // Creates a new adapter with the filtered suggestions and attaches it to the dropdown
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            SignUpActivity.this,
                            R.layout.dropdown_item,
                            suggestions
                    );
                    emailEditText.setAdapter(adapter);
                    // Shows the dropdown only if there are suggestions and the user hasn't already completed the domain
                    if (!suggestions.isEmpty() && !suffix.equals(suggestions.get(0).substring(prefix.length()))) {
                        emailEditText.showDropDown();
                    }
                } else {
                    // Clears the adapter when there's no "@" since domain suggestions don't apply yet
                    emailEditText.setAdapter(null);
                }
            }

            // Not used — no action needed after the text changes
            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Sets the minimum number of characters typed before suggestions appear — 1 means suggestions start immediately
        emailEditText.setThreshold(1);
    }
}
