package com.the_coffe_coders.fastestlap.adapter.junior;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.junior.JuniorEntryList;
import com.the_coffe_coders.fastestlap.domain.junior.JuniorTeam;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.Objects;

public class JuniorEntryListRecyclerAdapter extends RecyclerView.Adapter<JuniorEntryListRecyclerAdapter.JuniorEntryListViewHolder> {

    private final Context context;
    private final JuniorEntryList juniorEntryList;
    private int series;

    public JuniorEntryListRecyclerAdapter(Context context, JuniorEntryList juniorEntryList, int series) {
        this.context = context;
        this.juniorEntryList = juniorEntryList;
        this.series = series;
    }

    @NonNull
    @Override
    public JuniorEntryListRecyclerAdapter.JuniorEntryListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.junior_entry_list_card, parent, false);
        return new JuniorEntryListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JuniorEntryListRecyclerAdapter.JuniorEntryListViewHolder holder, int position) {

        JuniorTeam juniorTeam = juniorEntryList.getTeams().get(position);
        holder.teamName.setText(juniorTeam.getName());
        holder.driverOneName.setText(juniorTeam.getDrivers().get(0).getDriver());
        holder.driverTwoName.setText(juniorTeam.getDrivers().get(1).getDriver());

        switch (series) {
            case 0:
                holder.driverThreeName.setVisibility(View.GONE);
                break;
            case 1:
                holder.driverThreeName.setText(juniorTeam.getDrivers().get(2).getDriver());
                break;
        }

        String matchedKey = UIUtils.findMatchingKeyInMap(juniorTeam.getName(), Constants.JUNIOR_TEAM_GRADIENT_COLOR);

        holder.juniorTeamCard.setBackground(AppCompatResources
                .getDrawable(context, Objects.requireNonNull(Constants.JUNIOR_TEAM_GRADIENT_COLOR.get(matchedKey))));

        UIUtils.loadImageWithGlide(context, juniorTeam.getTeamLogoUrl(), holder.teamLogo, null);

    }

    @Override
    public int getItemCount() {
        return juniorEntryList.getTeams().size();
    }

    public static class JuniorEntryListViewHolder extends RecyclerView.ViewHolder {
        TextView teamName, driverOneName, driverTwoName, driverThreeName;
        ImageView teamLogo;
        LinearLayout juniorTeamCard;


        public JuniorEntryListViewHolder(@NonNull View itemView) {
            super(itemView);

            teamName = itemView.findViewById(R.id.team_name);
            driverOneName = itemView.findViewById(R.id.driver_1_name);
            driverTwoName = itemView.findViewById(R.id.driver_2_name);
            driverThreeName = itemView.findViewById(R.id.driver_3_name);
            teamLogo = itemView.findViewById(R.id.team_logo);
            juniorTeamCard = itemView.findViewById(R.id.junior_team_card_inner_layout);
        }
    }
}
