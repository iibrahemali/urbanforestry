// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports Context for loading theme attributes and creating dialogs without holding an Activity reference
import android.content.Context;
// Imports Intent for navigating to MainActivity to get directions
import android.content.Intent;
// Imports Bitmap and BitmapFactory to decode local image file paths into displayable bitmaps
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
// Imports TypedValue to resolve theme attributes (like accent color) at runtime
import android.util.TypedValue;
// Imports layout and view classes used to build the dynamic comment list and inflate item layouts
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
// Imports UI widget types used inside each post item view
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

// Imports AlertDialog for the edit post and report confirmation dialogs
import androidx.appcompat.app.AlertDialog;
// Imports ContextCompat for safely loading color resources
import androidx.core.content.ContextCompat;
// Imports RecyclerView for the adapter and ViewHolder base classes
import androidx.recyclerview.widget.RecyclerView;

// Imports Glide to load post images from Firebase Storage URLs efficiently
import com.bumptech.glide.Glide;
// Imports BottomSheetDialog to show the delete confirmation as a bottom sheet panel
import com.google.android.material.bottomsheet.BottomSheetDialog;
// Imports MaterialButton for the like, comment, get-route, and send-comment buttons
import com.google.android.material.button.MaterialButton;
// Imports FirebaseAuth to get the current user's UID for ownership checks
import com.google.firebase.auth.FirebaseAuth;

// Imports List as the type for the posts data source
import java.util.List;

// Declares PostAdapter as a RecyclerView adapter that binds Post objects to the post item view layout
public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {

    // Stores the list of Post objects that the RecyclerView will display
    private List<Post> posts;
    // Handles all Firebase post operations (like, comment, delete, report, edit) so the adapter stays focused on UI
    private PostRepository postRepository;
    // Stores the Context for resolving theme attributes and creating dialogs
    private Context ctx;

    // Constructor that receives the data list and context from FeedActivity
    public PostAdapter(List<Post> posts, Context ctx) {
        this.posts = posts;
        // Instantiates the repository — one instance shared across all items to avoid redundant Firebase connections
        this.postRepository = new PostRepository();
        this.ctx = ctx;
    }

    // Declares the ViewHolder class — caches references to the child Views inside each post item to avoid repeated findViewById calls
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // Displays the post's image (from a URL, drawable resource, or local file path)
        ImageView imageView;
        // Displays the post author's username
        TextView username;
        // Displays the post's caption text
        TextView textView;
        // Button for navigating to the post's GPS location on the map
        MaterialButton btnGetRoute;
        // Like/heart reaction button that shows the current count
        MaterialButton btnHeart;
        // Comment button that expands/collapses the comment section and shows the current count
        MaterialButton btnComment;
        // Delete button shown only to the post owner
        ImageButton btnDelete;
        // Edit button shown only to the post owner
        ImageButton btnEdit;
        // Report button shown only to non-owners
        ImageButton btnReport;

        // Container View that wraps the entire comments section — shown or hidden based on isCommentsVisible
        View commentsSection;
        // LinearLayout that holds the dynamically added comment TextViews
        LinearLayout commentsList;
        // Placeholder text shown when the post has no comments yet
        TextView noCommentsTv;
        // Input field for typing a new comment
        EditText etComment;
        // Submit button that sends the typed comment to Firebase
        MaterialButton btnSendComment;

        // Constructor that finds and stores all child View references from the inflated item layout
        public ViewHolder(View view) {
            // Calls super() so RecyclerView can track item position and handle recycling correctly
            super(view);
            imageView = view.findViewById(R.id.post_image);
            username = view.findViewById(R.id.post_username);
            textView = view.findViewById(R.id.post_text);
            btnGetRoute = view.findViewById(R.id.btn_get_route);
            btnHeart = view.findViewById(R.id.btn_heart);
            btnComment = view.findViewById(R.id.btn_comment);
            btnDelete = view.findViewById(R.id.btn_delete);
            btnEdit = view.findViewById(R.id.btn_edit);
            btnReport = view.findViewById(R.id.btn_report);

            commentsSection = view.findViewById(R.id.comments_section);
            commentsList = view.findViewById(R.id.comments_list);
            noCommentsTv = view.findViewById(R.id.no_comments_tv);
            etComment = view.findViewById(R.id.et_comment);
            btnSendComment = (MaterialButton) view.findViewById(R.id.btn_send_comment);
        }
    }

    // Called by RecyclerView when it needs a new ViewHolder — inflates the item layout and wraps it in a ViewHolder
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflates item_post.xml — false means RecyclerView handles attaching it to the parent
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
        return new ViewHolder(view);
    }

    // Called by RecyclerView to populate a ViewHolder with data for the given list position
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // Gets the Post object for this row
        Post post = posts.get(position);

        // Displays the author's username on the post card
        holder.username.setText(post.username);

        // Resets the visibility of content views before binding to prevent stale state from recycled views
        holder.imageView.setVisibility(View.GONE);
        holder.textView.setVisibility(View.GONE);
        holder.btnGetRoute.setVisibility(View.GONE);
        holder.commentsList.removeAllViews();
        holder.commentsSection.setVisibility(View.GONE);
        holder.noCommentsTv.setVisibility(View.GONE);

        // Gets the current user's UID to determine whether to show owner-only controls
        String currentUid = FirebaseAuth.getInstance().getUid();
        // True if the logged-in user is the post's author — controls button visibility
        boolean isOwner = currentUid != null && post.uid != null && currentUid.equals(post.uid);

        if (isOwner) {
            // Shows edit and delete buttons for the post owner — hides the report button since you can't report your own post
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnReport.setVisibility(View.GONE);
        } else {
            // Hides edit/delete for non-owners — shows the report button instead
            holder.btnDelete.setVisibility(View.GONE);
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnReport.setVisibility(View.VISIBLE);
        }

        // Sets the report icon to filled or outline depending on whether the current user has already reported this post
        if (post.isReportedByMe) {
            holder.btnReport.setImageResource(R.drawable.ic_report_filled);
        } else {
            holder.btnReport.setImageResource(R.drawable.ic_report_outline);
        }

        // Handles the report button tap — shows a confirmation before reporting, or the already-reported dialog if already done
        holder.btnReport.setOnClickListener(v -> {
            // Static demo posts have no postId and can't be reported
            if (post.postId == null) return;

            if (post.isReportedByMe) {
                // Shows a dialog offering the option to withdraw the report
                showAlreadyReportedDialog(holder, post, position);
            } else {
                // Shows a confirmation dialog before submitting the report — prevents accidental reports
                new AlertDialog.Builder(holder.itemView.getContext())
                        .setMessage("Are you sure you want to report this post for inappropriate content?")
                        .setPositiveButton("Report", (dialog, which) -> {
                            postRepository.reportPost(post.postId)
                                    .addOnSuccessListener(deleted -> {
                                        if (deleted) {
                                            // The post was auto-deleted after reaching 5 reports — removes it from the local list too
                                            posts.remove(position);
                                            notifyItemRemoved(position);
                                            notifyItemRangeChanged(position, posts.size());
                                        } else {
                                            // Post was flagged but not deleted — updates local state to reflect the report
                                            post.isReportedByMe = true;
                                            post.reportCount++;
                                            notifyItemChanged(position);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        // If Firebase says the user already reported this, shows the already-reported dialog instead
                                        if (e.getMessage() != null && e.getMessage().contains("ALREADY_REPORTED")) {
                                            showAlreadyReportedDialog(holder, post, position);
                                        }
                                    });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        // Handles the edit button tap — shows a dialog with the current caption pre-filled for editing
        holder.btnEdit.setOnClickListener(v -> {
            if (post.postId == null) return;

            AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
            builder.setTitle("Edit Post");

            // Creates an EditText input pre-populated with the current caption so the user doesn't have to retype everything
            final EditText input = new EditText(holder.itemView.getContext());
            input.setText(post.caption != null ? post.caption : post.text);
            builder.setView(input);

            builder.setPositiveButton("Save", (dialog, which) -> {
                String newCaption = input.getText().toString().trim();
                postRepository.updatePost(post.postId, newCaption)
                        .addOnSuccessListener(aVoid -> {
                            // Updates both caption and text fields locally so the UI reflects the change immediately without a re-fetch
                            post.caption = newCaption;
                            post.text = newCaption;
                            notifyItemChanged(position);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(holder.itemView.getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });
            // Dismisses the dialog without making changes if the user taps Cancel
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

            builder.show();
        });

        // Handles the "Get Directions" button — launches MainActivity with routing data if the post has a location
        holder.btnGetRoute.setOnClickListener(v -> {
            if (post.hasLocation) {
                // Navigates to MainActivity with the destination coordinates and post metadata
                Intent intent = new Intent(v.getContext(), MainActivity.class);
                intent.putExtra("destLat", post.latitude);
                intent.putExtra("destLng", post.longitude);
                // Signals MainActivity to start a route calculation immediately on launch
                intent.putExtra("getDirections", true);

                // Passes the author name and caption so the route toggle button dialog can show post context
                intent.putExtra("postUser", post.username);
                String infoText = (post.caption != null && !post.caption.isEmpty()) ? post.caption : post.text;
                intent.putExtra("postText", infoText);

                v.getContext().startActivity(intent);
            } else {
                // Informs the user if this post doesn't have location data attached
                Toast.makeText(v.getContext(), "No location data for this photo", Toast.LENGTH_SHORT).show();
            }
        });

        // Handles the delete button tap — shows a bottom sheet confirmation before permanently deleting
        holder.btnDelete.setOnClickListener(v -> {
            if (post.postId == null) return;

            // Creates a bottom sheet dialog styled with the DeleteBottomSheetStyle for a polished look
            BottomSheetDialog dialog = new BottomSheetDialog(holder.itemView.getContext(), R.style.DeleteBottomSheetStyle);
            View dialogView = LayoutInflater.from(holder.itemView.getContext())
                    .inflate(R.layout.dialog_delete_confirm, null);
            dialog.setContentView(dialogView);

            // Removes the default background so the custom rounded dialog background shows correctly
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            // Confirms deletion when the user taps the confirm button in the bottom sheet
            dialogView.findViewById(R.id.btn_confirm_delete).setOnClickListener(confirmView -> {
                dialog.dismiss();
                postRepository.deletePost(post.postId)
                        .addOnSuccessListener(aVoid -> {
                            // Removes the post from the local list and notifies RecyclerView to animate the removal
                            posts.remove(position);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, posts.size());
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(holder.itemView.getContext(), "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });

            // Cancels deletion and dismisses the sheet when the user taps Cancel
            dialogView.findViewById(R.id.btn_cancel_delete).setOnClickListener(cancelView -> dialog.dismiss());

            dialog.show();
        });

        // Resolves the theme's accent color at runtime to detect the Spring theme — Spring uses green buttons instead of white
        TypedValue tv = new TypedValue();
        ctx.getTheme().resolveAttribute(R.attr.accent_color, tv, true);
        if (tv.resourceId == R.color.spring_accent_color) {
            // Sets an alternative green color for the Get Directions button to match the Spring theme palette
            int altColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.spring_alt_color);
            holder.btnGetRoute.setBackgroundColor(altColor);
        }

        // Loads the post's image using the appropriate source depending on which field is set
        if (post.resourceId != -1) {
            // Static demo posts use a local drawable resource ID
            holder.imageView.setVisibility(View.VISIBLE);
            if (post.hasLocation) holder.btnGetRoute.setVisibility(View.VISIBLE);
            holder.imageView.setImageResource(post.resourceId);
        } else if (post.imageUrl != null && !post.imageUrl.isEmpty()) {
            // Real Firestore posts load their image from a Firebase Storage download URL using Glide
            holder.imageView.setVisibility(View.VISIBLE);
            if (post.hasLocation) holder.btnGetRoute.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(post.imageUrl)
                    .into(holder.imageView);
        } else if (post.imagePath != null && !post.imagePath.isEmpty()) {
            // Legacy fallback for posts created with local file paths before Firebase Storage was used
            holder.imageView.setVisibility(View.VISIBLE);
            if (post.hasLocation) holder.btnGetRoute.setVisibility(View.VISIBLE);
            Bitmap bitmap = BitmapFactory.decodeFile(post.imagePath);
            holder.imageView.setImageBitmap(bitmap);
        }

        // Displays the caption text if either field is populated — prefers caption over legacy text field
        String displayCaption = post.caption != null ? post.caption : post.text;
        if (displayCaption != null && !displayCaption.isEmpty()) {
            holder.textView.setVisibility(View.VISIBLE);
            holder.textView.setText(displayCaption);
        }

        // Determines the like count and liked state using the correct field depending on whether this is a real or demo post
        int currentLikes = (post.postId != null) ? post.likeCount : post.heartCount;
        boolean isLiked = (post.postId != null) ? post.isLikedByMe : post.isHeartedByMe;

        // Shows the like count on the button label
        holder.btnHeart.setText(String.valueOf(currentLikes));
        if (isLiked) {
            // Filled heart at full opacity indicates the user has liked this post
            holder.btnHeart.setIconResource(R.drawable.ic_heart_filled);
            holder.btnHeart.setAlpha(1.0f);
        } else {
            // Outline heart at reduced opacity indicates the post hasn't been liked yet
            holder.btnHeart.setIconResource(R.drawable.ic_heart_outline);
            holder.btnHeart.setAlpha(0.6f);
        }

        // Shows the comment count on the comment button label — uses real count for Firestore posts, in-memory count for demo posts
        int currentComments = (post.postId != null) ? post.commentCount : (post.comments != null ? post.comments.size() : 0);
        holder.btnComment.setText(String.valueOf(currentComments));
        holder.btnComment.setIconResource(R.drawable.ic_comment_outline);

        // Handles the like button tap — toggles like state locally and in Firestore
        holder.btnHeart.setOnClickListener(v -> {
            // Demo posts handle likes locally without hitting Firestore
            if (post.postId == null) {
                if (post.isHeartedByMe) {
                    post.heartCount--;
                    post.isHeartedByMe = false;
                } else {
                    post.heartCount++;
                    post.isHeartedByMe = true;
                }
                notifyItemChanged(position);
                return;
            }

            // Uses a heart emoji as the reaction type — stored in the like document for future emoji reaction features
            String reactionEmoji = "❤️";
            postRepository.toggleLike(post.postId, reactionEmoji)
                    .addOnSuccessListener(aVoid -> {
                        if (post.isLikedByMe) {
                            // Removes the like: decrements the count and clears the reaction emoji
                            post.likeCount--;
                            post.isLikedByMe = false;
                            post.userEmoji = null;
                        } else {
                            // Adds the like: increments the count and records the reaction emoji
                            post.likeCount++;
                            post.isLikedByMe = true;
                            post.userEmoji = reactionEmoji;
                        }
                        // Refreshes this item so the button icon and count update immediately
                        notifyItemChanged(position);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(holder.itemView.getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        // Handles the comment button tap — toggles the comments section open or closed
        holder.btnComment.setOnClickListener(v -> {
            // Flips the visibility flag so the section toggles each time the button is tapped
            post.isCommentsVisible = !post.isCommentsVisible;
            if (post.isCommentsVisible && post.postId != null) {
                // Loads fresh comments from Firestore when opening the section
                loadComments(holder, post);
            } else {
                // Hides the comments section when collapsing
                holder.commentsSection.setVisibility(View.GONE);
            }
            notifyItemChanged(position);
        });

        // Restores the comment section visibility and reloads comments after the view is recycled and rebound
        if (post.isCommentsVisible) {
            holder.commentsSection.setVisibility(View.VISIBLE);
            if (post.postId != null) {
                loadComments(holder, post);
            }
        }

        // Handles the send comment button tap — submits the typed comment to Firebase
        holder.btnSendComment.setOnClickListener(v -> {
            String commentText = holder.etComment.getText().toString().trim();
            // Does nothing if the comment field is empty — avoids blank comments being submitted
            if (commentText.isEmpty()) return;

            // Demo posts handle comments locally without Firestore
            if (post.postId == null) {
                post.comments.add(new Comment("You", commentText));
                holder.etComment.setText("");
                notifyItemChanged(position);
                return;
            }

            // Disables the button while submitting to prevent double submissions
            holder.btnSendComment.setEnabled(false);
            postRepository.addComment(post.postId, commentText)
                    .addOnSuccessListener(aVoid -> {
                        // Clears the input field and re-enables the button after successful submission
                        holder.etComment.setText("");
                        holder.btnSendComment.setEnabled(true);
                        // Increments the local comment count so the button label updates without a re-fetch
                        post.commentCount++;
                        // Reloads the comment list to include the newly added comment
                        loadComments(holder, post);
                        notifyItemChanged(position);
                    })
                    .addOnFailureListener(e -> {
                        // Re-enables the button so the user can retry after a failure
                        holder.btnSendComment.setEnabled(true);
                        Toast.makeText(holder.itemView.getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    // Shows a dialog informing the user they've already reported this post, with an option to withdraw the report
    private void showAlreadyReportedDialog(ViewHolder holder, Post post, int position) {
        new AlertDialog.Builder(holder.itemView.getContext())
                .setMessage("You have already reported this post.")
                .setPositiveButton("Continue", null)
                // Allows the user to undo their report if they changed their mind
                .setNegativeButton("Withdraw", (dialog, which) -> {
                    postRepository.unreportPost(post.postId)
                            .addOnSuccessListener(aVoid -> {
                                // Updates local state so the button reverts to the unreported icon
                                post.isReportedByMe = false;
                                post.reportCount--;
                                notifyItemChanged(position);
                            });
                })
                .show();
    }

    // Fetches and displays all comments for a post from Firestore into the comment list section
    private void loadComments(ViewHolder holder, Post post) {
        if (post.postId == null) return;

        postRepository.getComments(post.postId)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Clears the existing comment views before adding fresh ones to avoid duplicates
                    holder.commentsList.removeAllViews();
                    List<Comment> comments = queryDocumentSnapshots.toObjects(Comment.class);
                    if (comments.isEmpty()) {
                        // Shows the "No comments yet" placeholder when the list is empty
                        holder.noCommentsTv.setVisibility(View.VISIBLE);
                    } else {
                        holder.noCommentsTv.setVisibility(View.GONE);

                        // Resolves the seasonal background_color theme attribute to use as comment text color
                        TypedValue typedValue = new TypedValue();
                        holder.itemView.getContext().getTheme().resolveAttribute(R.attr.background_color, typedValue, true);
                        int seasonalColor = typedValue.data;

                        // Creates a TextView for each comment and adds it to the comments list
                        for (Comment comment : comments) {
                            TextView ct = new TextView(holder.itemView.getContext());
                            // Formats each comment as "username: text"
                            ct.setText(comment.username + ": " + comment.text);
                            ct.setPadding(8, 4, 8, 4);
                            // Uses the seasonal color so comment text matches the current theme
                            ct.setTextColor(seasonalColor);
                            holder.commentsList.addView(ct);
                        }
                    }
                    // Makes the comment section container visible now that it's been populated
                    holder.commentsSection.setVisibility(View.VISIBLE);
                });
    }

    // Returns the total number of posts in the list so RecyclerView knows how many rows to create
    @Override
    public int getItemCount() {
        return posts.size();
    }
}
