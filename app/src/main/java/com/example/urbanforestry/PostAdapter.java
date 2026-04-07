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

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {

    private List<Post> posts;

    public PostAdapter(List<Post> posts) {
        this.posts = posts;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView username;
        TextView textView;
        Button btnHeart;
        Button btnComment;
        
        // Comment section views
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

        // ALWAYS reset first
        holder.imageView.setVisibility(View.GONE);
        holder.textView.setVisibility(View.GONE);
        holder.commentsList.removeAllViews();
        holder.commentsSection.setVisibility(View.GONE);
        holder.noCommentsTv.setVisibility(View.GONE);

        boolean hasImage = false;

        // CASE 1: Resource ID (Dummy Data)
        if (post.resourceId != -1) {
            holder.imageView.setVisibility(View.VISIBLE);
            holder.imageView.setImageResource(post.resourceId);
            hasImage = true;
        }
        // CASE 2: Image Path (Disk File)
        else if (post.imagePath != null && !post.imagePath.isEmpty()) {
            holder.imageView.setVisibility(View.VISIBLE);
            Bitmap bitmap = BitmapFactory.decodeFile(post.imagePath);
            holder.imageView.setImageBitmap(bitmap);
            hasImage = true;
        }

        // Handle Text / Caption
        if (post.text != null && !post.text.isEmpty()) {
            holder.textView.setVisibility(View.VISIBLE);
            holder.textView.setText(post.text);
        }
        else if (!hasImage) {
            holder.textView.setVisibility(View.VISIBLE);
            holder.textView.setText("Empty post");
        }

        // Handle Heart Button
        holder.btnHeart.setText("💚 " + post.heartCount);
        holder.btnHeart.setAlpha(post.isHeartedByMe ? 1.0f : 0.5f);

        // Handle Comment Button (Counter)
        holder.btnComment.setText("💬 " + post.comments.size());

        // Handle Comments Visibility
        if (post.isCommentsVisible) {
            holder.commentsSection.setVisibility(View.VISIBLE);
            
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

        // Click Listeners
        holder.btnHeart.setOnClickListener(v -> {
            if (post.isHeartedByMe) {
                post.heartCount--;
                post.isHeartedByMe = false;
            } else {
                post.heartCount++;
                post.isHeartedByMe = true;
            }
            notifyItemChanged(position);
        });

        holder.btnComment.setOnClickListener(v -> {
            post.isCommentsVisible = !post.isCommentsVisible;
            notifyItemChanged(position);
        });

        holder.btnSendComment.setOnClickListener(v -> {
            String commentText = holder.etComment.getText().toString().trim();
            if (!commentText.isEmpty()) {
                post.comments.add(new Comment("You", commentText));
                holder.etComment.setText(""); // Clear input
                notifyItemChanged(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }
}
