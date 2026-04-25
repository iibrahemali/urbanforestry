// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports Manifest (unused after removing location logic but kept for future permissions)
import android.Manifest;
// Imports Intent for navigating back to FeedActivity after posting
import android.content.Intent;
// Imports PackageManager (unused after removing location logic)
import android.content.pm.PackageManager;
// Imports Bundle, the key-value container Android passes to onCreate with any saved state
import android.os.Bundle;
// Imports Button for the Post button
import android.widget.Button;
// Imports EditText for the text input field where the user types their post
import android.widget.EditText;
// Imports Toast to show brief feedback messages to the user
import android.widget.Toast;

// Imports AppCompatActivity, the base class that provides backwards-compatible Activity features
import androidx.appcompat.app.AppCompatActivity;

// Declares PostTextActivity as the screen where a user types and submits a text-only post
public class PostTextActivity extends AppCompatActivity {

    // Declares the text input field where the user writes their post content
    private EditText editText;
    // Declares the submit button that triggers the post creation
    private Button postBtn;
    // Declares the repository that handles all Firestore post creation operations
    private PostRepository postRepository;

    // Overrides onCreate, called when this Activity is first created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Applies the seasonal theme before layout inflation
        setTheme(SeasonManager.getSeasonTheme(SeasonManager.getSeasonPref(this)));

        // Calls the parent class onCreate to complete Android's standard Activity setup
        super.onCreate(savedInstanceState);
        // Inflates activity_post_text.xml and sets it as this screen's UI
        setContentView(R.layout.activity_post_text);

        // Instantiates the repository that will handle writing the post to Firestore
        postRepository = new PostRepository();

        // Links the EditText and Button variables to their corresponding Views in the XML layout
        editText = findViewById(R.id.editText);
        postBtn = findViewById(R.id.btn_post);

        // Attaches a click listener to the post button using a lambda
        postBtn.setOnClickListener(v -> {
            // Reads and trims the post text to prevent empty or whitespace-only posts from being submitted
            String caption = editText.getText().toString().trim();
            if (caption.isEmpty()) {
                Toast.makeText(this, "Please write something", Toast.LENGTH_SHORT).show();
                return;
            }

            // Disables the button and updates its label to prevent the user from tapping it multiple times while the upload runs
            postBtn.setEnabled(false);
            postBtn.setText("Posting...");

            // Logic change: Text-only posts should NEVER share location per user requirements.
            // We call submitPost directly with null/0 coordinates.
            submitPost(caption, null, 0);
        });
    }

    // Submits the post to Firestore with the provided caption and explicitly no location data
    private void submitPost(String caption, Double lat, double lng) {
        // Calls the repository to create a text post — the repository handles username lookup and Firestore writes
        postRepository.createPost(caption, lat, lng)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Post created!", Toast.LENGTH_SHORT).show();
                    // Navigates to FeedActivity using FLAG_ACTIVITY_CLEAR_TOP so the existing Feed instance is brought forward rather than a new one created
                    Intent i = new Intent(PostTextActivity.this, FeedActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                    // Closes PostTextActivity so the user can't navigate back to the composer after posting
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Re-enables the button so the user can try again if the post fails
                    postBtn.setEnabled(true);
                    postBtn.setText("Post");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
