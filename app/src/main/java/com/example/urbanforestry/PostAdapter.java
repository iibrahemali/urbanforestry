package com.example.urbanforestry;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {

    private List<Post> posts;
    private PostRepository postRepository;
    private String currentUid;

    public PostAdapter(List<Post> posts) {
        this.posts = posts;
        this.postRepository = new PostRepository();
        this.currentUid = FirebaseAuth.getInstance().getUid();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView username;
        TextView textView;
        MaterialButton btnHeart;
        MaterialButton btnComment;
        ImageButton btnDelete;
        
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
            btnHeart = (MaterialButton) view.findViewById(R.id.btn_heart);
            btnComment = (MaterialButton) view.findViewById(R.id.btn_comment);
            btnDelete = view.findViewById(R.id.btn_delete);
            
            commentsSection = view.findViewById(R.id.comments_section);
            commentsList = view.findViewById(R.id.comments_list);
            noCommentsTv = view.findViewById(R.id.no_comments_tv);
            etComment = view.findViewById(R.id.et_comment);
            btnSendComment = (MaterialButton) view.findViewById(R.id.btn_send_comment);
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

        // Show delete button only if current user is the owner
        if (post.uid != null && post.uid.equals(currentUid)) {
            holder.btnDelete.setVisibility(View.VISIBLE);
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }

        // Delete Click Listener
        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(holder.itemView.getContext())
                .setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    postRepository.deletePost(post.postId)
                        .addOnSuccessListener(aVoid -> {
                            posts.remove(position);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, posts.size());
                            Toast.makeText(holder.itemView.getContext(), "Post deleted", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(holder.itemView.getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        // Handle Image
        if (post.resourceId != -1) {
            holder.imageView.setVisibility(View.VISIBLE);
            holder.imageView.setImageResource(post.resourceId);
        } else if (post.imagePath != null && !post.imagePath.isEmpty()) {
            holder.imageView.setVisibility(View.VISIBLE);
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
            } else {
                String reactionEmoji = "❤️"; 
                postRepository.toggleLike(post.postId, reactionEmoji)
                    .addOnSuccessListener(aVoid -> {
                        if (post.isLikedByMe) {
                            post.likeCount--;
                            post.isLikedByMe = false;
                        } else {
                            post.likeCount++;
                            post.isLikedByMe = true;
                        }
                        notifyItemChanged(position);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(holder.itemView.getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            }
        });

        // Toggle Comments & Load them
        holder.btnComment.setOnClickListener(v -> {
            post.isCommentsVisible = !post.isCommentsVisible;
            notifyItemChanged(position);
        });

        if (post.isCommentsVisible) {
            holder.commentsSection.setVisibility(View.VISIBLE);
            if (post.postId != null) {
                loadComments(holder, post);
            } else {
                if (post.comments.isEmpty()) {
                    holder.noCommentsTv.setVisibility(View.VISIBLE);
                } else {
                    holder.noCommentsTv.setVisibility(View.GONE);
                    for (Comment comment : post.comments) {
                        TextView ct = new TextView(holder.itemView.getContext());
                        ct.setText(comment.username + ": " + comment.text);
                        ct.setPadding(8, 4, 8, 4);
                        ct.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.logo_color));
                        holder.commentsList.addView(ct);
                    }
                }
            }
        }

        // Add Comment Listener
        holder.btnSendComment.setOnClickListener(v -> {
            String commentText = holder.etComment.getText().toString().trim();
            if (commentText.isEmpty()) return;

            if (post.postId == null) {
                post.comments.add(new Comment("You", commentText));
                holder.etComment.setText("");
                notifyItemChanged(position);
            } else {
                holder.btnSendComment.setEnabled(false);
                postRepository.addComment(post.postId, commentText)
                    .addOnSuccessListener(aVoid -> {
                        holder.etComment.setText("");
                        holder.btnSendComment.setEnabled(true);
                        post.commentCount++;
                        notifyItemChanged(position);
                    })
                    .addOnFailureListener(e -> {
                        holder.btnSendComment.setEnabled(true);
                        Toast.makeText(holder.itemView.getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            }
        });
    }

    private void loadComments(ViewHolder holder, Post post) {
        if (post.postId == null) return;
        
        postRepository.getComments(post.postId)
            .addOnSuccessListener(queryDocumentSnapshots -> {
                holder.commentsList.removeAllViews();
                List<Comment> commentsList = queryDocumentSnapshots.toObjects(Comment.class);
                if (commentsList.isEmpty()) {
                    holder.noCommentsTv.setVisibility(View.VISIBLE);
                } else {
                    holder.noCommentsTv.setVisibility(View.GONE);
                    for (Comment comment : commentsList) {
                        TextView ct = new TextView(holder.itemView.getContext());
                        ct.setText(comment.username + ": " + comment.text);
                        ct.setPadding(8, 4, 8, 4);
                        ct.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.logo_color));
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
