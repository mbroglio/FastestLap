package com.the_coffe_coders.fastestlap.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandingsElement;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModel;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.UIUtils;

import java.util.List;
import java.util.Objects;

public class ConstructorStandingsRecyclerAdapter extends RecyclerView.Adapter<ConstructorStandingsRecyclerAdapter.ConstructorViewHolder> {
    private final Context context;
    private final String constructorId;
    private final DriverViewModel driverViewModel;
    private final ConstructorViewModel constructorViewModel;
    private final List<ConstructorStandingsElement> constructorStandingsList;
    private final LifecycleOwner lifecycleOwner;
    private final LoadingScreen loadingScreen;
    private ConstructorStandingsElement constructorStandingsElement;

    public ConstructorStandingsRecyclerAdapter(Context context, String constructorId, List<ConstructorStandingsElement> constructorStandingsList,
                                               DriverViewModel driverViewModel, ConstructorViewModel constructorViewModel,
                                               LifecycleOwner lifecycleOwner, LoadingScreen loadingScreen) {
        this.context = context;
        this.constructorId = constructorId;
        this.constructorStandingsList = constructorStandingsList;
        this.driverViewModel = driverViewModel;
        this.constructorViewModel = constructorViewModel;
        this.lifecycleOwner = lifecycleOwner;
        this.loadingScreen = loadingScreen;
    }

    @NonNull
    @Override
    public ConstructorStandingsRecyclerAdapter.ConstructorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.team_card, parent, false);
        return new ConstructorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConstructorViewHolder holder, int position) {
        constructorStandingsElement = constructorStandingsList.get(position);
        String currentConstructorId = constructorStandingsElement.getConstructor().getConstructorId();


            constructorViewModel.getSelectedConstructor(currentConstructorId).observe(lifecycleOwner, result -> {
                if (result instanceof Result.Loading) {
                    return;
                }
                if (result.isSuccess()) {
                    showConstructorFound(holder);
                    Constructor constructor = ((Result.ConstructorSuccess) result).getData();
                    constructorStandingsElement.setConstructor(constructor);

                    holder.constructorCardInnerLayout.setBackground(AppCompatResources.getDrawable(context,
                            Objects.requireNonNull(Constants.TEAM_GRADIENT_COLOR.get(currentConstructorId))));

                    UIUtils.setTextViewTextWithCondition(constructorStandingsElement.getPosition() == null,
                            ContextCompat.getString(context, R.string.last_constructor_position), //if true
                            constructorStandingsElement.getPosition(), //if false
                            holder.constructorPosition);

                    UIUtils.multipleSetTextViewText(
                            new String[]{
                                    constructor.getName(),
                                    constructorStandingsElement.getPoints()},
                            new TextView[]{
                                    holder.constructorName,
                                    holder.constructorPoints});

                    if (constructorId != null) {
                        if (currentConstructorId.equals(constructorId)) {
                            UIUtils.animateCardBackgroundColor(context, holder.constructorCard, R.color.yellow, Color.TRANSPARENT, 1000, 10);
                        }
                    }

                    holder.constructorCard.setOnClickListener(v -> goToBioPage(position));

                    UIUtils.loadSequenceOfImagesWithGlide(context,
                            new String[]{
                                    constructor.getCar_pic_url(),
                                    constructor.getTeam_logo_url()},
                            new ImageView[]{
                                    holder.constructorCarImage,
                                    holder.constructorLogo},

                            () -> processDriverOne(holder, constructor, position));
                }else{
                    showConstructorNotFound(holder, currentConstructorId);
                }
            });



    }

    private void goToBioPage(int position) {
        //TEMPORARY FIX
        String constructorIdToShow = constructorStandingsList.get(position).getConstructor().getConstructorId();
        //

        UIUtils.navigateToBioPage(context, constructorIdToShow, 0);
    }

    private void processDriverOne(ConstructorViewHolder holder, Constructor constructor, int position) {
        try{
            driverViewModel.getDriver(constructor.getDriverOneId()).observe(lifecycleOwner, result -> {
                if (result instanceof Result.Loading) {
                    return;
                }
                if (result.isSuccess()) {
                    Driver driverOne = ((Result.DriverSuccess) result).getData();

                    UIUtils.singleSetTextViewText(driverOne.getFullName(), holder.driverOneName);
                    UIUtils.loadImageWithGlide(context, driverOne.getDriver_pic_url(), holder.driverOneImage,
                            () -> processDriverTwo(holder, constructor, position));
                }else{
                    setMissingDriver(holder, constructor.getDriverOneId(), constructor, position, 1);
                }
            });
        } catch (RuntimeException e) {
            setMissingDriver(holder, constructor.getDriverOneId(), constructor, position, 1);
        }

    }

    private void processDriverTwo(ConstructorViewHolder holder, Constructor constructor, int position) {
        try{
            driverViewModel.getDriver(constructor.getDriverTwoId()).observe(lifecycleOwner, result -> {
                if (result instanceof Result.Loading) {
                    return;
                }
                if (result.isSuccess()) {
                    Driver driverTwo = ((Result.DriverSuccess) result).getData();

                    UIUtils.singleSetTextViewText(driverTwo.getFullName(), holder.driverTwoName);
                    UIUtils.loadImageWithGlide(context, driverTwo.getDriver_pic_url(), holder.driverTwoImage, () -> endLoading(position));
                }else{
                    setMissingDriver(holder, constructor.getDriverTwoId(), constructor, position, 2);
                }
            });
        } catch (RuntimeException e) {
            setMissingDriver(holder, constructor.getDriverTwoId(), constructor, position, 2);
        }

    }

    private void endLoading(int position) {
        loadingScreen.updateProgress();

        Log.i("ConstructorsStanding", "onBindViewHolder " + position + "/" + getItemCount());
        loadingScreen.hideLoadingScreenWithCondition(position == getItemCount() - 1);
    }

    private void setMissingDriver(ConstructorViewHolder holder, String driverId, Constructor constructor, int position, int driverType) {
        if(driverId.contains("_")) {
            driverId = driverId.split("_")[1];
        }

        switch (driverType){
            case 1 :
                UIUtils.singleSetTextViewText(driverId.toUpperCase(), holder.driverOneName);
                UIUtils.loadImageWithGlide(context, null, holder.driverOneImage,
                        () -> processDriverTwo(holder, constructor, position));
                break;
            case 2 :
                UIUtils.singleSetTextViewText(driverId.toUpperCase(), holder.driverTwoName);
                UIUtils.loadImageWithGlide(context, null, holder.driverTwoImage, () -> endLoading(position));
        }

    }

    private void showConstructorFound(ConstructorStandingsRecyclerAdapter.ConstructorViewHolder holder) {
        holder.constructorCardInnerLayout.setVisibility(View.VISIBLE);
        holder.constructorNotFound.setVisibility(View.GONE);
    }

    private void showConstructorNotFound(ConstructorViewHolder holder, String constructorId) {
        holder.constructorCardInnerLayout.setVisibility(View.INVISIBLE);
        holder.constructorNotFound.setVisibility(View.VISIBLE);
        Log.i("ConstructorsStandingAdapter", "Constructor not found id test: " + constructorId + " -> " + constructorId.contains("_"));

        if(constructorId.contains("_")) {
            constructorId = constructorId.split("_")[0] + " " + constructorId.split("_")[1];
        }

        UIUtils.singleSetTextViewText(context.getString(R.string.constructor_info_not_found_upper_case, constructorId.toUpperCase()), holder.constructorNotFound);


    }

    @Override
    public int getItemCount() {
        return constructorStandingsList.size();
    }

    public static class ConstructorViewHolder extends RecyclerView.ViewHolder {

        final TextView constructorName;
        final TextView constructorPoints;
        final TextView constructorPosition;
        final TextView driverOneName;
        final TextView driverTwoName;
        final TextView constructorNotFound;
        final ImageView constructorLogo;
        final ImageView constructorCarImage;
        final ImageView driverOneImage;
        final ImageView driverTwoImage;
        final LinearLayout constructorCardInnerLayout;
        final MaterialCardView constructorCard;

        public ConstructorViewHolder(@NonNull View itemView) {
            super(itemView);

            constructorName = itemView.findViewById(R.id.team_name);
            constructorPoints = itemView.findViewById(R.id.team_points);
            constructorPosition = itemView.findViewById(R.id.team_position);
            constructorLogo = itemView.findViewById(R.id.team_logo);
            constructorCarImage = itemView.findViewById(R.id.car_image);
            driverOneName = itemView.findViewById(R.id.driver_1_name);
            driverOneImage = itemView.findViewById(R.id.driver_1_pic);
            driverTwoName = itemView.findViewById(R.id.driver_2_name);
            driverTwoImage = itemView.findViewById(R.id.driver_2_pic);
            constructorCardInnerLayout = itemView.findViewById(R.id.team_card);
            constructorCard = itemView.findViewById(R.id.team_card_view);
            constructorNotFound = itemView.findViewById(R.id.constructor_not_found);
        }
    }
}
