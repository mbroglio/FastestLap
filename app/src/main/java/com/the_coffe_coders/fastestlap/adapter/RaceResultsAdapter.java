package com.the_coffe_coders.fastestlap.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResult;
import com.the_coffe_coders.fastestlap.util.Constants;

import java.util.List;
import java.util.Objects;

public class RaceResultsAdapter extends RecyclerView.Adapter<RaceResultsAdapter.ResultViewHolder> {

    private final List<RaceResult> raceResults;
    private final Context context;

    public RaceResultsAdapter(Context context, List<RaceResult> raceResults) {
        this.context = context;
        this.raceResults = raceResults;
    }

    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.race_result_item, parent, false);
        return new ResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        RaceResult result = raceResults.get(position);

        holder.positionText.setText(String.valueOf(position + 1));
        holder.driverName.setText(result.getDriver().getFullName());
        holder.teamName.setText(result.getConstructor().getName());

        // Set team color
        String teamId = result.getConstructor().getConstructorId();
        Integer teamColorObj = Constants.TEAM_COLOR.get(teamId);
        int teamColor = ContextCompat.getColor(context,
                Objects.requireNonNullElseGet(teamColorObj, () -> R.color.mercedes_f1));
        holder.teamColorIndicator.setBackgroundColor(teamColor);
    }

    @Override
    public int getItemCount() {
        return raceResults.size();
    }

    static class ResultViewHolder extends RecyclerView.ViewHolder {
        TextView positionText;
        View teamColorIndicator;
        TextView driverName;
        TextView teamName;

        ResultViewHolder(View itemView) {
            super(itemView);
            positionText = itemView.findViewById(R.id.position_text);
            teamColorIndicator = itemView.findViewById(R.id.team_color_indicator);
            driverName = itemView.findViewById(R.id.driver_name);
            teamName = itemView.findViewById(R.id.team_name);
        }
    }
}