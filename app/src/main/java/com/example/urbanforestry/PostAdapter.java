package com.example.urbanforestry;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {

    private List<Post> posts;
    private PostRepository postRepository;

    public PostAdapter(List<Post> posts) {
        this.posts = posts;
        this.postRepository = new PostRepository();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView username;
        TextView textView;
        MaterialButton btnGetRoute;
        MaterialButton btnHeart;
        MaterialButton btnComment;
        ImageButton btnDelete;
        ImageButton btnEdit;
        ImageButton btnReport;

        View commentsSection;
        LinearLayout commentsList;
        TextView noCommentsTv;
        EditText etComment;
        MaterialButton btnSendComment;

        public ViewHolder(View view) {
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
            btnSendComment = view.findViewById(R.id.btn_send_comment);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Post post = posts.get(position);

        holder.username.setText(post.username);

        holder.imageView.setVisibility(View.GONE);
        holder.textView.setVisibility(View.GONE);
        holder.commentsList.removeAllViews();
        holder.commentsSection.setVisibility(View.GONE);
        holder.noCommentsTv.setVisibility(View.GONE);

        // Show edit and delete buttons only if current user is the owner
        String currentUid = FirebaseAuth.getInstance().getUid();
        boolean isOwner = currentUid != null && post.uid != null && currentUid.equals(post.uid);

        if (isOwner) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnReport.setVisibility(View.GONE);
        } else {
            holder.btnDelete.setVisibility(View.GONE);
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnReport.setVisibility(View.VISIBLE);
        }

        // REPORT BUTTON LOGIC
        holder.btnReport.setOnClickListener(v -> {
            if (post.postId == null) return;

            new AlertDialog.Builder(holder.itemView.getContext())
                    .setMessage("Are you sure you want to report this post for inappropriate content?")
                    .setPositiveButton("Report", (dialog, which) -> {
                        postRepository.reportPost(post.postId)
                                .addOnSuccessListener(deleted -> {
                                    if (deleted) {
                                        Toast.makeText(holder.itemView.getContext(), "Post removed due to multiple reports.", Toast.LENGTH_LONG).show();
                                        posts.remove(position);
                                        notifyItemRemoved(position);
                                        notifyItemRangeChanged(position, posts.size());
                                    } else {
                                        Toast.makeText(holder.itemView.getContext(), "Post reported.", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(holder.itemView.getContext(), "Report failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // EDIT BUTTON LOGIC
        holder.btnEdit.setOnClickListener(v -> {
            if (post.postId == null) return;

            AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
            builder.setTitle("Edit Post");

            final EditText input = new EditText(holder.itemView.getContext());
            input.setText(post.caption != null ? post.caption : post.text);
            builder.setView(input);

            builder.setPositiveButton("Save", (dialog, which) -> {
                String newCaption = input.getText().toString().trim();
                postRepository.updatePost(post.postId, newCaption)
                        .addOnSuccessListener(aVoid -> {
                            post.caption = newCaption;
                            post.text = newCaption;
                            notifyItemChanged(position);
                            Toast.makeText(holder.itemView.getContext(), "Post updated", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(holder.itemView.getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

            builder.show();
        });

        // GET DIRECTIONS TO PHOTO LOCATION
        holder.btnGetRoute.setOnClickListener(v -> {
            if (post.hasLocation) {
                Intent intent = new Intent(v.getContext(), HomePage.class);
                intent.putExtra("destLat", post.latitude);
                intent.putExtra("destLng", post.longitude);
                intent.putExtra("getDirections", true);

                // Add info for the toggle button dialog
                intent.putExtra("postUser", post.username);
                String infoText = (post.caption != null && !post.caption.isEmpty()) ? post.caption : post.text;
                intent.putExtra("postText", infoText);

                v.getContext().startActivity(intent);
            } else {
                Toast.makeText(v.getContext(), "No location data for this photo", Toast.LENGTH_SHORT).show();
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (post.postId == null) return;

            BottomSheetDialog dialog = new BottomSheetDialog(holder.itemView.getContext(), R.style.DeleteBottomSheetStyle);
            View dialogView = LayoutInflater.from(holder.itemView.getContext())
                    .inflate(R.layout.dialog_delete_confirm, null);
            dialog.setContentView(dialogView);

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            dialogView.findViewById(R.id.btn_confirm_delete).setOnClickListener(confirmView -> {
                dialog.dismiss();
                postRepository.deletePost(post.postId)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(holder.itemView.getContext(), "Post deleted", Toast.LENGTH_SHORT).show();
                            posts.remove(position);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, posts.size());
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(holder.itemView.getContext(), "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });

            dialogView.findViewById(R.id.btn_cancel_delete).setOnClickListener(cancelView -> dialog.dismiss());

            dialog.show();
        });

        // Handle Image
        if (post.resourceId != -1) {
            holder.imageView.setVisibility(View.VISIBLE);
            holder.btnGetRoute.setVisibility(View.VISIBLE);
            holder.imageView.setImageResource(post.resourceId);
        } else if (post.imageUrl != null && !post.imageUrl.isEmpty()) {
            // New logic: Load from Firebase Storage URL
            holder.imageView.setVisibility(View.VISIBLE);
            holder.btnGetRoute.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(post.imageUrl)
                    .into(holder.imageView);
        } else if (post.imagePath != null && !post.imagePath.isEmpty()) {
            // Local path (for legacy/development)
            holder.imageView.setVisibility(View.VISIBLE);
            holder.btnGetRoute.setVisibility(View.VISIBLE);
            Bitmap bitmap = BitmapFactory.decodeFile(post.imagePath);
            holder.imageView.setImageBitmap(bitmap);
        }

        // Handle Caption/Text
        String displayCaption = post.caption != null ? post.caption : post.text;
        if (displayCaption != null && !displayCaption.isEmpty()) {
            holder.textView.setVisibility(View.VISIBLE);
            holder.textView.setText(displayCaption);
        }

        // Handle Like/Heart Button UI
        int currentLikes = (post.postId != null) ? post.likeCount : post.heartCount;
        boolean isLiked = (post.postId != null) ? post.isLikedByMe : post.isHeartedByMe;

        holder.btnHeart.setText(String.valueOf(currentLikes));
        if (isLiked) {
            holder.btnHeart.setIconResource(R.drawable.ic_heart_filled);
            holder.btnHeart.setAlpha(1.0f);
        } else {
            holder.btnHeart.setIconResource(R.drawable.ic_heart_outline);
            holder.btnHeart.setAlpha(0.6f);
        }

        // Handle Comment Button UI
        int currentComments = (post.postId != null) ? post.commentCount : (post.comments != null ? post.comments.size() : 0);
        holder.btnComment.setText(String.valueOf(currentComments));
        holder.btnComment.setIconResource(R.drawable.ic_comment_outline);

        // Like Click Listener
        holder.btnHeart.setOnClickListener(v -> {
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

            String reactionEmoji = "❤️";
            postRepository.toggleLike(post.postId, reactionEmoji)
                    .addOnSuccessListener(aVoid -> {
                        if (post.isLikedByMe) {
                            post.likeCount--;
                            post.isLikedByMe = false;
                            post.userEmoji = null;
                        } else {
                            post.likeCount++;
                            post.isLikedByMe = true;
                            post.userEmoji = reactionEmoji;
                        }
                        notifyItemChanged(position);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(holder.itemView.getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        // Toggle Comments & Load them
        holder.btnComment.setOnClickListener(v -> {
            post.isCommentsVisible = !post.isCommentsVisible;
            if (post.isCommentsVisible && post.postId != null) {
                loadComments(holder, post);
            } else {
                holder.commentsSection.setVisibility(View.GONE);
            }
            notifyItemChanged(position);
        });

        if (post.isCommentsVisible) {
            holder.commentsSection.setVisibility(View.VISIBLE);
            if (post.postId != null) {
                loadComments(holder, post);
            }
        }

        // Add Comment Listener
        holder.btnSendComment.setOnClickListener(v -> {
            String commentText = holder.etComment.getText().toString().trim();
            if (commentText.isEmpty()) return;

            if (post.postId == null) {
                // Dummy data
                post.comments.add(new Comment("You", commentText));
                holder.etComment.setText("");
                notifyItemChanged(position);
                return;
            }

            holder.btnSendComment.setEnabled(false);
            postRepository.addComment(post.postId, commentText)
                    .addOnSuccessListener(aVoid -> {
                        holder.etComment.setText("");
                        holder.btnSendComment.setEnabled(true);
                        post.commentCount++;
                        loadComments(holder, post); // Refresh
                        notifyItemChanged(position);
                    })
                    .addOnFailureListener(e -> {
                        holder.btnSendComment.setEnabled(true);
                        Toast.makeText(holder.itemView.getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void loadComments(ViewHolder holder, Post post) {
        if (post.postId == null) return;

        postRepository.getComments(post.postId)
            .addOnSuccessListener(queryDocumentSnapshots -> {
                holder.commentsList.removeAllViews();
                List<Comment> comments = queryDocumentSnapshots.toObjects(Comment.class);
                if (comments.isEmpty()) {
                    holder.noCommentsTv.setVisibility(View.VISIBLE);
                } else {
                    holder.noCommentsTv.setVisibility(View.GONE);

                    // Resolve the seasonal logo_color attribute programmatically
                    TypedValue typedValue = new TypedValue();
                    holder.itemView.getContext().getTheme().resolveAttribute(R.attr.logo_color, typedValue, true);
                    int seasonalLogoColor = typedValue.data;

                    for (Comment comment : comments) {
                        TextView ct = new TextView(holder.itemView.getContext());
                        ct.setText(comment.username + ": " + comment.text);
                        ct.setPadding(8, 4, 8, 4);
                        ct.setTextColor(seasonalLogoColor);
                        holder.commentsList.addView(ct);
                    }
                }
            });
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }
}
