package com.example.urbanforestry;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

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
        Button btnHeart;
        Button btnComment;
        
        View commentsSection;
        LinearLayout commentsList;
        TextView noCommentsTv;
        EditText etComment;
        Button btnSendComment;

        public ViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.post_image);
            username = view.findViewById(R.id.post_username);
            textView = view.findViewById(R.id.post_text);
            btnHeart = view.findViewById(R.id.btn_heart);
            btnComment = view.findViewById(R.id.btn_comment);
            
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

        // Handle Like/Heart Button
        int currentLikes = post.postId != null ? post.likeCount : post.heartCount;
        boolean isLiked = post.postId != null ? post.isLikedByMe : post.isHeartedByMe;
        String emoji = post.userEmoji != null ? post.userEmoji : "❤️";
        
        holder.btnHeart.setText(emoji + " " + currentLikes);
        holder.btnHeart.setAlpha(isLiked ? 1.0f : 0.5f);

        // Handle Comment Button
        int currentComments = post.postId != null ? post.commentCount : (post.comments != null ? post.comments.size() : 0);
        holder.btnComment.setText("💬 " + currentComments);

        // Like Click Listener
        holder.btnHeart.setOnClickListener(v -> {
            if (post.postId == null) {
                if (post.isHeartedByMe) { post.heartCount--; post.isHeartedByMe = false; }
                else { post.heartCount++; post.isHeartedByMe = true; }
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
                    for (Comment comment : comments) {
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
