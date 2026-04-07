package com.example.urbanforestry;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

        public ViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.post_image);
            username = view.findViewById(R.id.post_username);
            textView = view.findViewById(R.id.post_text);
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

        // CASE 1: Resource ID (Dummy Data)
        if (post.resourceId != -1) {
            holder.imageView.setVisibility(View.VISIBLE);
            holder.imageView.setImageResource(post.resourceId);
        }
        // CASE 2: Image Path (Disk File)
        else if (post.imagePath != null && !post.imagePath.isEmpty()) {
            holder.imageView.setVisibility(View.VISIBLE);
            Bitmap bitmap = BitmapFactory.decodeFile(post.imagePath);
            holder.imageView.setImageBitmap(bitmap);
        }
        // CASE 3: Text Post
        else if (post.text != null && !post.text.isEmpty()) {
            holder.textView.setVisibility(View.VISIBLE);
            holder.textView.setText(post.text);
        }
        else {
            holder.textView.setVisibility(View.VISIBLE);
            holder.textView.setText("Empty post");
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }
}
