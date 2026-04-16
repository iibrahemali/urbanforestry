package com.example.urbanforestry;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.AchievementViewHolder> {

    private List<achievement> achievementList;

    public AchievementAdapter(List<achievement> achievementList) {
        this.achievementList = achievementList;
    }

    @NonNull
    @Override
    public AchievementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_trophy_list, parent, false);
        return new AchievementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AchievementViewHolder holder, int position) {
        achievement achievement = achievementList.get(position);

        holder.tvName.setText(achievement.getName());

        // Calculate Level and Progress Text
        String stats = "Level " + achievement.getLevel() + ": " +
                achievement.getCurrentProgress() + "/" + achievement.getGoalTarget();
        holder.tvStats.setText(stats);

        // Calculate progress percentage for the bar
        int progressPercent = (int) (((double) achievement.getCurrentProgress() / achievement.getGoalTarget()) * 100);
        holder.progressBar.setProgress(progressPercent);
    }

    @Override
    public int getItemCount() {
        return achievementList.size();
    }

    public static class AchievementViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvStats;
        ProgressBar progressBar;

        public AchievementViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.achievementName);
            tvStats = itemView.findViewById(R.id.achievementLevelText);
            progressBar = itemView.findViewById(R.id.achievementProgress);
        }
    }
}