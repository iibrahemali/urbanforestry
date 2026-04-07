package com.example.urbanforestry;

import android.app.AlertDialog;
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
        TextView emojiDisplay;
        Button btnEmoji;
        Button btnComment;
        LinearLayout commentsContainer;

        public ViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.post_image);
            username = view.findViewById(R.id.post_username);
            textView = view.findViewById(R.id.post_text);
            emojiDisplay = view.findViewById(R.id.emoji_display);
            btnEmoji = view.findViewById(R.id.btn_emoji);
            btnComment = view.findViewById(R.id.btn_comment);
            commentsContainer = view.findViewById(R.id.comments_container);
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
        holder.emojiDisplay.setVisibility(View.GONE);
        holder.commentsContainer.removeAllViews();
        holder.commentsContainer.setVisibility(View.GONE);

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

        // Handle Emojis
        if (!post.emojis.isEmpty()) {
            holder.emojiDisplay.setVisibility(View.VISIBLE);
            StringBuilder sb = new StringBuilder();
            for (String emoji : post.emojis) {
                sb.append(emoji).append(" ");
            }
            holder.emojiDisplay.setText(sb.toString().trim());
        }

        // Handle Comments
        if (!post.comments.isEmpty()) {
            holder.commentsContainer.setVisibility(View.VISIBLE);
            for (Comment comment : post.comments) {
                TextView ct = new TextView(holder.itemView.getContext());
                ct.setText(comment.username + ": " + comment.text);
                ct.setPadding(0, 4, 0, 4);
                holder.commentsContainer.addView(ct);
            }
        }

        // Click Listeners
        holder.btnEmoji.setOnClickListener(v -> {
            String[] items = {"🌳", "❤️", "👍", "🌱", "🦋"};
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Pick a reaction")
                    .setItems(items, (dialog, which) -> {
                        post.emojis.add(items[which]);
                        notifyItemChanged(position);
                    })
                    .show();
        });

        holder.btnComment.setOnClickListener(v -> {
            EditText input = new EditText(v.getContext());
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Add Comment")
                    .setView(input)
                    .setPositiveButton("Post", (dialog, which) -> {
                        String commentText = input.getText().toString();
                        if (!commentText.isEmpty()) {
                            post.comments.add(new Comment("You", commentText));
                            notifyItemChanged(position);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }
}
