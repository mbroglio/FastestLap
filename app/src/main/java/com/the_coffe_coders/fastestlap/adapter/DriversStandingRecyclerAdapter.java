package com.the_coffe_coders.fastestlap.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.grand_prix.DriverStandingsElement;
import com.the_coffe_coders.fastestlap.ui.bio.DriverBioActivity;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModel;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.UIUtils;

import java.util.List;

public class DriversStandingRecyclerAdapter extends RecyclerView.Adapter<DriversStandingRecyclerAdapter.DriverViewHolder>{

    private final Context context;
    private final List<DriverStandingsElement> driversStandingList;
    private final List<Driver> driversList;
    private final String driverId;
    private final LoadingScreen loadingScreen;
    private final DriverViewModel driverViewModel;
    private final ConstructorViewModel constructorViewModel;
    private final LifecycleOwner lifecycleOwner;
    private DriverStandingsElement driverStandingsElement;
    //private int counter;

    public DriversStandingRecyclerAdapter(Context context, List<DriverStandingsElement> driversStandingList,
                                          List<Driver> driversList, String driverId, DriverViewModel driverViewModel,
                                          ConstructorViewModel constructorViewModel, LifecycleOwner lifecycleOwner,
                                          LoadingScreen loadingScreen) {
        this.context = context;
        this.driversStandingList = driversStandingList;
        this.driversList = driversList;
        this.driverId = driverId;
        this.driverViewModel = driverViewModel;
        this.constructorViewModel = constructorViewModel;
        this.lifecycleOwner = lifecycleOwner;
        this.loadingScreen = loadingScreen;
        //this.counter = 1;
    }

    @NonNull
    @Override
    public DriversStandingRecyclerAdapter.DriverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.driver_card, parent, false);
        return new DriverViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DriverViewHolder holder, int position) {
        driverStandingsElement = new DriverStandingsElement();
        if(driversStandingList == null){
            driverStandingsElement.setDriver(driversList.get(position));
            driverStandingsElement.setPoints("0");
        }else{
            driverStandingsElement = driversStandingList.get(position);
        }

        driverViewModel.getDriver(driverStandingsElement.getDriver().getDriverId()).observe(lifecycleOwner, result -> {
            if(result instanceof Result.Loading){
                return;
            }
            if(result.isSuccess()){
                Driver driver = ((Result.DriverSuccess) result).getData();

                UIUtils.multipleSetTextViewText(
                        new String[]{
                                driver.getFullName(),
                                driverStandingsElement.getPoints(),
                        },
                        new TextView[]{
                                holder.driverName,
                                holder.driverPoints,

                        });

                UIUtils.setTextViewTextWithCondition(driverStandingsElement.getPosition() == null || driverStandingsElement.getPosition().equals("-"),
                        ContextCompat.getString(context, R.string.last_driver_position), //if true
                        driverStandingsElement.getPosition(), //if false
                       holder.driverPosition);

                if (driverStandingsElement.getDriver().getDriverId().equals(driverId)) {
                    UIUtils.animateCardBackgroundColor(context, holder.driverCard.findViewById(R.id.driver_card_view), R.color.yellow, Color.TRANSPARENT, 1000, 10);
                }

                holder.driverCard.setOnClickListener(v -> goToBioPage(position));

                if (driver.getTeam_id() != null) {
                    holder.driverCardInnerLayout.setBackground(AppCompatResources.getDrawable(context, Constants.TEAM_GRADIENT_COLOR.get(driver.getTeam_id())));
                } else {
                    holder.driverCardInnerLayout.setBackground(AppCompatResources.getDrawable(context, R.color.timer_gray));
                    holder.driverTeamImage.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.f1_car_icon_filled));
                }

                UIUtils.loadImageWithGlide(context, driver.getDriver_pic_url(), holder.driverImage, () ->
                        generateForConstructor(holder, driver, position));

            }
        });
    }

    private void goToBioPage(int position) {
        //TEMPORARY FIX
        String driverIdToShow;
        if(driversStandingList == null){
            driverIdToShow = driversList.get(position).getDriverId();
        }else{
            driverIdToShow = driversStandingList.get(position).getDriver().getDriverId();
        }
        //

        Intent intent = new Intent(context, DriverBioActivity.class);
        intent.putExtra("DRIVER_ID", driverIdToShow);
        context.startActivity(intent);
    }

    private void generateForConstructor(DriverViewHolder holder, Driver driver, int position) {
        constructorViewModel.getSelectedConstructor(driver.getTeam_id()).observe(lifecycleOwner, result -> {
            if(result instanceof Result.Loading){
                return;
            }
            if(result.isSuccess()){
                Constructor constructor = ((Result.ConstructorSuccess) result).getData();

                UIUtils.loadImageWithGlide(context, constructor.getTeam_logo_minimal_url(), holder.driverTeamImage, () -> {

                    loadingScreen.updateProgress();

                    Log.i("DriversStanding", "onBindViewHolder " + position + "/" + getItemCount());
                    loadingScreen.hideLoadingScreenWithCondition(position == getItemCount() - 1);
                    //counter++;
                });
            }
        });
    }

    @Override
    public int getItemCount() {
        if(driversList != null){
            return driversList.size();
        }else if(driversStandingList != null){
            return driversStandingList.size();
        }else{
            return 0;
        }
    }

    public static class DriverViewHolder extends RecyclerView.ViewHolder {

        MaterialCardView driverCard;
        TextView driverName, driverPoints, driverPosition;
        ImageView driverImage, driverTeamImage;
        RelativeLayout driverCardInnerLayout;

        public DriverViewHolder(@NonNull View itemView) {
            super(itemView);

            driverCard = itemView.findViewById(R.id.driver_card_view);
            driverName = itemView.findViewById(R.id.driver_name);
            driverPoints = itemView.findViewById(R.id.driver_points);
            driverPosition = itemView.findViewById(R.id.driver_position);
            driverImage = itemView.findViewById(R.id.driver_image);
            driverTeamImage = itemView.findViewById(R.id.team_logo);
            driverCardInnerLayout = itemView.findViewById(R.id.small_driver_card);
        }
    }
}
