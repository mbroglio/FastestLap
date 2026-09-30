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

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorEntryList;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorTeam;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;

import java.util.Objects;

public class JuniorEntryListRecyclerAdapter extends RecyclerView.Adapter<JuniorEntryListRecyclerAdapter.JuniorEntryListViewHolder> {

    private final Context context;
    private final JuniorEntryList juniorEntryList;
    private final int series;
    private final LoadingScreen loadingScreen;
    private final boolean[] loadedPositions;
    private final int targetLoadCount;
    private int currentLoadedCount = 0;

    public JuniorEntryListRecyclerAdapter(Context context, JuniorEntryList juniorEntryList, int series, LoadingScreen loadingScreen) {
        this.context = context;
        this.juniorEntryList = juniorEntryList;
        this.series = series;
        this.loadingScreen = loadingScreen;
        this.targetLoadCount = (juniorEntryList != null && juniorEntryList.getTeams() != null) ? juniorEntryList.getTeams().size() : 0;
        this.loadedPositions = new boolean[targetLoadCount];
        preloadAllItems();
    }

    public JuniorEntryListRecyclerAdapter(Context context, JuniorEntryList juniorEntryList, int series) {
        this(context, juniorEntryList, series, null);
    }

    private void preloadAllItems() {
        if (targetLoadCount == 0) {
            if (loadingScreen != null) {
                loadingScreen.hideLoadingScreen();
            }
            return;
        }

        for (int i = 0; i < targetLoadCount; i++) {
            final int pos = i;
            JuniorTeam team = juniorEntryList.getTeams().get(pos);
            if (team == null) {
                endLoading(pos);
                continue;
            }

            String logoUrl = team.getTeam_logo_url();
            String carImageUrl = team.getCar_image_url();

            boolean hasLogoUrl = logoUrl != null && !logoUrl.trim().isEmpty() && !"-".equals(logoUrl.trim()) && logoUrl.startsWith("http");
            boolean hasCarUrl = carImageUrl != null && !carImageUrl.trim().isEmpty() && !"-".equals(carImageUrl.trim()) && carImageUrl.startsWith("http");

            if (hasLogoUrl && hasCarUrl) {
                UIUtils.preloadImagesInParallel(context, new String[]{logoUrl, carImageUrl}, () -> endLoading(pos));
            } else if (hasLogoUrl) {
                UIUtils.preloadImage(context, logoUrl, () -> endLoading(pos));
            } else if (hasCarUrl) {
                UIUtils.preloadImage(context, carImageUrl, () -> endLoading(pos));
            } else {
                endLoading(pos);
            }
        }
    }

    private void endLoading(int position) {
        boolean shouldHide = false;
        synchronized (this) {
            if (targetLoadCount > 0 && position >= 0 && position < loadedPositions.length && !loadedPositions[position]) {
                loadedPositions[position] = true;
                currentLoadedCount++;
                android.util.Log.i("JuniorEntryList", "Item " + position + " loaded (" + currentLoadedCount + "/" + targetLoadCount + ")");
                if (currentLoadedCount >= targetLoadCount) {
                    shouldHide = true;
                }
            }
        }
        if (shouldHide && loadingScreen != null) {
            loadingScreen.hideLoadingScreen();
        }
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

        if (juniorTeam.getDrivers() != null && !juniorTeam.getDrivers().isEmpty()) {
            holder.driverOneName.setText(juniorTeam.getDrivers().get(0).getDriver());
            if (juniorTeam.getDrivers().size() > 1) {
                holder.driverTwoName.setText(juniorTeam.getDrivers().get(1).getDriver());
            }
        }

        switch (series) {
            case 0:
                if (holder.driverThreeContainer != null) {
                    holder.driverThreeContainer.setVisibility(View.GONE);
                }
                holder.driverThreeName.setVisibility(View.GONE);
                break;
            case 1:
                if (holder.driverThreeContainer != null) {
                    holder.driverThreeContainer.setVisibility(View.VISIBLE);
                }
                holder.driverThreeName.setVisibility(View.VISIBLE);
                if (juniorTeam.getDrivers() != null && juniorTeam.getDrivers().size() > 2) {
                    holder.driverThreeName.setText(juniorTeam.getDrivers().get(2).getDriver());
                }
                break;
        }

        String matchedKey = UIUtils.findMatchingKeyInMap(juniorTeam.getName(), Constants.JUNIOR_TEAM_GRADIENT_COLOR);

        if (matchedKey != null && Constants.JUNIOR_TEAM_GRADIENT_COLOR.containsKey(matchedKey)) {
            holder.juniorTeamCard.setBackground(AppCompatResources
                    .getDrawable(context, Objects.requireNonNull(Constants.JUNIOR_TEAM_GRADIENT_COLOR.get(matchedKey))));
        }

        UIUtils.loadImagesInParallel(context,
                new String[]{juniorTeam.getTeam_logo_url(), juniorTeam.getCar_image_url()},
                new ImageView[]{holder.teamLogo, holder.carImage},
                null);

    }

    @Override
    public int getItemCount() {
        return juniorEntryList.getTeams().size();
    }

    public static class JuniorEntryListViewHolder extends RecyclerView.ViewHolder {
        TextView teamName, driverOneName, driverTwoName, driverThreeName;
        ImageView teamLogo, carImage;
        LinearLayout juniorTeamCard;
        View driverThreeContainer;


        public JuniorEntryListViewHolder(@NonNull View itemView) {
            super(itemView);

            teamName = itemView.findViewById(R.id.team_name);
            driverOneName = itemView.findViewById(R.id.driver_1_name);
            driverTwoName = itemView.findViewById(R.id.driver_2_name);
            driverThreeName = itemView.findViewById(R.id.driver_3_name);
            driverThreeContainer = itemView.findViewById(R.id.driver_3_container);
            teamLogo = itemView.findViewById(R.id.team_logo);
            carImage = itemView.findViewById(R.id.car_image);
            juniorTeamCard = itemView.findViewById(R.id.junior_team_card_inner_layout);
        }
    }
}
