// Declares the package this class belongs to, grouping all app classes under the same namespace
package com.example.urbanforestry;

// Imports LayoutInflater to inflate the list item XML layout for each achievement row
import android.view.LayoutInflater;
// Imports View as the base class for all UI widgets
import android.view.View;
// Imports ViewGroup, the parent container that each list item View is attached to
import android.view.ViewGroup;
// Imports ProgressBar to display a visual fill bar showing progress toward the next level
import android.widget.ProgressBar;
// Imports TextView to display the achievement name and level/progress stats text
import android.widget.TextView;

// Imports NonNull annotation to enforce null-safety on overridden methods
import androidx.annotation.NonNull;
// Imports RecyclerView to subclass the Adapter and ViewHolder it provides
import androidx.recyclerview.widget.RecyclerView;

// Imports List as the type for the data source passed into the adapter
import java.util.List;

// Declares AchievementAdapter as a RecyclerView adapter that binds Achievement data to individual list item views
public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.AchievementViewHolder> {

    // Stores the list of Achievement objects that this adapter will display — one per row in the RecyclerView
    private List<Achievement> achievementList;

    // Constructor that receives the data list from AchievementsActivity and stores it for use in onBindViewHolder
    public AchievementAdapter(List<Achievement> achievementList) {
        this.achievementList = achievementList;
    }

    // Called by the RecyclerView when it needs a new ViewHolder — inflates the item layout and wraps it in a ViewHolder
    @NonNull
    @Override
    public AchievementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflates the list item layout from XML — false means we let RecyclerView handle attaching it to the parent
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_trophy_list, parent, false);
        // Wraps the inflated View in an AchievementViewHolder that holds references to the child Views
        return new AchievementViewHolder(view);
    }

    // Called by the RecyclerView to populate a ViewHolder with data for a specific list position
    @Override
    public void onBindViewHolder(@NonNull AchievementViewHolder holder, int position) {
        // Gets the Achievement object for this position from the data list
        Achievement achievement = achievementList.get(position);

        // Sets the achievement's display name (e.g. "Oak Specialist") in the name TextView
        holder.tvName.setText(achievement.getName());

        // Builds the stats string combining level and progress (e.g. "Level 2: 3/5") for the stats TextView
        String stats = "Level " + achievement.getLevel() + ": " +
                achievement.getCurrentProgress() + "/" + achievement.getGoalTarget();
        holder.tvStats.setText(stats);

        // Calculates the progress as a percentage (0–100) so the ProgressBar knows how full to be
        int progressPercent = (int) (((double) achievement.getCurrentProgress() / achievement.getGoalTarget()) * 100);
        // Sets the ProgressBar fill to the calculated percentage
        holder.progressBar.setProgress(progressPercent);
    }

    // Returns the total number of items so the RecyclerView knows how many rows to create
    @Override
    public int getItemCount() {
        return achievementList.size();
    }

    // Declares the ViewHolder class — caches references to the child Views so we don't call findViewById on every bind
    public static class AchievementViewHolder extends RecyclerView.ViewHolder {
        // Holds a reference to the achievement name TextView
        TextView tvName, tvStats;
        // Holds a reference to the progress bar showing how close the user is to the next level
        ProgressBar progressBar;

        // Constructor that finds and stores the child View references from the inflated item layout
        public AchievementViewHolder(@NonNull View itemView) {
            // Calls super() so RecyclerView can track item position and recycle correctly
            super(itemView);
            // Finds and stores the name TextView by its resource ID
            tvName = itemView.findViewById(R.id.achievementName);
            // Finds and stores the stats TextView that shows level and progress numbers
            tvStats = itemView.findViewById(R.id.achievementLevelText);
            // Finds and stores the ProgressBar that visually represents progress toward the next level
            progressBar = itemView.findViewById(R.id.achievementProgress);
        }
    }
}
