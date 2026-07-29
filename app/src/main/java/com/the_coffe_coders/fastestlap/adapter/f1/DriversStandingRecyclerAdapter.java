package com.the_coffe_coders.fastestlap.adapter.f1;

import android.content.Context;
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
import com.the_coffe_coders.fastestlap.domain.f1.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.f1.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.f1.standing.DriverStandingsElement;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModel;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ui.NavigationUtils;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.List;

public class DriversStandingRecyclerAdapter extends RecyclerView.Adapter<DriversStandingRecyclerAdapter.DriverViewHolder> {

    private final Context context;
    private final List<DriverStandingsElement> driversStandingList;
    private final List<Driver> driversList;
    private final String driverId;
    private final LoadingScreen loadingScreen;
    private final DriverViewModel driverViewModel;
    private final ConstructorViewModel constructorViewModel;
    private final LifecycleOwner lifecycleOwner;
    private final boolean[] loadedPositions;
    private final int targetLoadCount;
    private int currentLoadedCount = 0;

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
        this.targetLoadCount = Math.min(getItemCount(), 3);
        this.loadedPositions = new boolean[getItemCount()];
        preloadAllItems();
    }

    @NonNull
    @Override
    public DriversStandingRecyclerAdapter.DriverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.driver_card, parent, false);
        return new DriverViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DriverViewHolder holder, int position) {
        final DriverStandingsElement element;
        if (driversStandingList == null) { //use driversList
            element = new DriverStandingsElement();
            element.setDriver(driversList.get(position));
            element.setPoints("0");
            element.setPosition(String.valueOf(position + 1));
        } else { //use driversStandingList
            element = driversStandingList.get(position);
        }

        final String currentDriverId = element.getDriver().getDriverId();

        try {
            androidx.lifecycle.LiveData<Result> driverLd = driverViewModel.getDriver(currentDriverId);
            @SuppressWarnings("unchecked")
            androidx.lifecycle.Observer<Result>[] selfRef = new androidx.lifecycle.Observer[1];
            selfRef[0] = result -> {
                if (result instanceof Result.Loading) {
                    return;
                }
                driverLd.removeObserver(selfRef[0]);
                loadingScreen.updateProgress();
                if (result.isSuccess()) {
                    showDriverFound(holder);
                    Driver driver = ((Result.DriverSuccess) result).getData();

                    UIUtils.multipleSetTextViewText(
                            new String[]{
                                    driver.getFullName(),
                                    element.getPoints(),
                            },
                            new TextView[]{
                                    holder.driverName,
                                    holder.driverPoints,

                            });

                    UIUtils.setTextViewTextWithCondition(element.getPosition() == null || element.getPosition().equals("-"),
                            ContextCompat.getString(context, R.string.last_driver_position), //if true
                            element.getPosition(), //if false
                            holder.driverPosition);

                    if (driverId != null) {
                        if (currentDriverId.equals(driverId)) {
                            UIUtils.animateCardBackgroundColor(context, holder.driverCard.findViewById(R.id.driver_card_view), R.color.yellow, Color.TRANSPARENT, 1000, 10);
                        }
                    }

                    holder.driverCard.setOnClickListener(v -> goToBioPage(position));
                    Log.i("DriversStanding", driver.getDriverId() + " driver.getTeam_id()");

                    if (driver.getTeam_id() != null) {
                        try {
                            holder.driverCardInnerLayout.setBackground(AppCompatResources.getDrawable(context, Constants.TEAM_GRADIENT_COLOR.get(driver.getTeam_id())));
                        } catch (Exception e) {
                            holder.driverCardInnerLayout.setBackground(AppCompatResources.getDrawable(context, R.color.timer_gray));
                            holder.driverTeamImage.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.f1_car_icon_filled));
                        }
                    } else {
                        holder.driverCardInnerLayout.setBackground(AppCompatResources.getDrawable(context, R.color.timer_gray));
                        holder.driverTeamImage.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.f1_car_icon_filled));
                    }

                    UIUtils.loadImageWithGlide(context, driver.getDriver_half_pic_url(), holder.driverImage, () -> {
                        loadingScreen.updateProgress();
                        generateForConstructor(holder, driver, position);
                    });

                } else {
                    showDriverNotFound(holder, currentDriverId);
                    endLoading(position);
                }
            };
            driverLd.observe(lifecycleOwner, selfRef[0]);
        } catch (RuntimeException e) {
            Log.e("DriversStandingAdapter", "driver error: " + e.getMessage());
            showDriverNotFound(holder, currentDriverId);
            endLoading(position);
        }

    }

    private void showDriverFound(DriverViewHolder holder) {
        holder.driverCardInnerLayout.setVisibility(View.VISIBLE);
        holder.driverNotFound.setVisibility(View.GONE);
    }

    private void showDriverNotFound(DriverViewHolder holder, String driverId) {
        holder.driverCardInnerLayout.setVisibility(View.INVISIBLE);
        holder.driverNotFound.setVisibility(View.VISIBLE);
        Log.i("DriversStandingAdapter", "Driver not found id test: " + driverId + " -> " + driverId.contains("_"));

        if (driverId != null) {
            if (driverId.contains("_")) {
                driverId = driverId.split("_")[1];
            }
            UIUtils.singleSetTextViewText(driverId.toUpperCase(java.util.Locale.ROOT) + " " + context.getString(R.string.driver_info_not_found), holder.driverNotFound);
        } else {
            UIUtils.singleSetTextViewText(context.getString(R.string.driver_info_not_found), holder.driverNotFound);
        }

    }

    private void goToBioPage(int position) {
        String driverIdToShow;
        if (driversStandingList == null) {
            driverIdToShow = driversList.get(position).getDriverId();
        } else {
            driverIdToShow = driversStandingList.get(position).getDriver().getDriverId();
        }

        NavigationUtils.navigateToBioPage(context, driverIdToShow, 1);
    }

    private void generateForConstructor(DriverViewHolder holder, Driver driver, int position) {
        if (driver.getTeam_id() == null) {
            endLoading(position);
            return;
        }

        try {
            androidx.lifecycle.LiveData<Result> constLd = constructorViewModel.getSelectedConstructor(driver.getTeam_id());
            @SuppressWarnings("unchecked")
            androidx.lifecycle.Observer<Result>[] selfRef = new androidx.lifecycle.Observer[1];
            selfRef[0] = result -> {
                if (result instanceof Result.Loading) {
                    return;
                }
                constLd.removeObserver(selfRef[0]);
                loadingScreen.updateProgress();
                if (result.isSuccess()) {
                    Constructor constructor = ((Result.ConstructorSuccess) result).getData();

                    UIUtils.loadImageWithGlide(context, constructor.getTeam_logo_minimal_url(), holder.driverTeamImage, () -> {
                        loadingScreen.updateProgress();
                        endLoading(position);
                    });
                } else {
                    endLoading(position);
                }
            };
            constLd.observe(lifecycleOwner, selfRef[0]);
        } catch (Exception e) {
            endLoading(position);
        }
    }

    private void preloadAllItems() {
        for (int i = 0; i < getItemCount(); i++) {
            final int pos = i;
            String dId;
            if (driversStandingList == null) {
                dId = driversList.get(pos).getDriverId();
            } else {
                dId = driversStandingList.get(pos).getDriver().getDriverId();
            }

            androidx.lifecycle.LiveData<Result> dLd = driverViewModel.getDriver(dId);
            @SuppressWarnings("unchecked")
            androidx.lifecycle.Observer<Result>[] selfRef = new androidx.lifecycle.Observer[1];
            selfRef[0] = result -> {
                if (result instanceof Result.Loading) return;
                dLd.removeObserver(selfRef[0]);
                if (result.isSuccess()) {
                    Driver driver = ((Result.DriverSuccess) result).getData();
                    String driverImgUrl = driver.getDriver_half_pic_url();
                    String teamId = driver.getTeam_id();

                    if (teamId != null) {
                        androidx.lifecycle.LiveData<Result> cLd = constructorViewModel.getSelectedConstructor(teamId);
                        @SuppressWarnings("unchecked")
                        androidx.lifecycle.Observer<Result>[] cRef = new androidx.lifecycle.Observer[1];
                        cRef[0] = cRes -> {
                            if (cRes instanceof Result.Loading) return;
                            cLd.removeObserver(cRef[0]);
                            if (cRes.isSuccess()) {
                                Constructor c = ((Result.ConstructorSuccess) cRes).getData();
                                UIUtils.preloadImagesInParallel(context,
                                        new String[]{driverImgUrl, c.getTeam_logo_minimal_url()},
                                        () -> endLoading(pos));
                            } else {
                                UIUtils.preloadImage(context, driverImgUrl, () -> endLoading(pos));
                            }
                        };
                        cLd.observe(lifecycleOwner, cRef[0]);
                    } else {
                        UIUtils.preloadImage(context, driverImgUrl, () -> endLoading(pos));
                    }
                } else {
                    endLoading(pos);
                }
            };
            dLd.observe(lifecycleOwner, selfRef[0]);
        }
    }

    private void endLoading(int position) {
        boolean shouldHide = false;
        synchronized (this) {
            if (position >= 0 && position < loadedPositions.length && !loadedPositions[position]) {
                loadedPositions[position] = true;
                currentLoadedCount++;
                Log.i("DriversStanding", "onBindViewHolder " + position + "/" + getItemCount() + " - loaded: " + currentLoadedCount + "/" + targetLoadCount);
                boolean allTopItemsLoaded = true;
                for (int i = 0; i < targetLoadCount; i++) {
                    if (i < loadedPositions.length && !loadedPositions[i]) {
                        allTopItemsLoaded = false;
                        break;
                    }
                }
                if (allTopItemsLoaded) {
                    shouldHide = true;
                }
            }
        }
        if (shouldHide) {
            loadingScreen.hideLoadingScreen();
        }
    }

    @Override
    public int getItemCount() {
        if (driversList != null) {
            return driversList.size();
        } else if (driversStandingList != null) {
            return driversStandingList.size();
        } else {
            return 0;
        }
    }

    public static class DriverViewHolder extends RecyclerView.ViewHolder {

        final MaterialCardView driverCard;
        final TextView driverName;
        final TextView driverPoints;
        final TextView driverPosition;
        final TextView driverNotFound;
        final ImageView driverImage;
        final ImageView driverTeamImage;
        final RelativeLayout driverCardInnerLayout;

        public DriverViewHolder(@NonNull View itemView) {
            super(itemView);

            driverCard = itemView.findViewById(R.id.driver_card_view);
            driverName = itemView.findViewById(R.id.driver_name);
            driverPoints = itemView.findViewById(R.id.driver_points);
            driverPosition = itemView.findViewById(R.id.driver_position);
            driverImage = itemView.findViewById(R.id.driver_image);
            driverTeamImage = itemView.findViewById(R.id.team_logo);
            driverCardInnerLayout = itemView.findViewById(R.id.small_driver_card);
            driverNotFound = itemView.findViewById(R.id.driver_not_found);
        }
    }
}
