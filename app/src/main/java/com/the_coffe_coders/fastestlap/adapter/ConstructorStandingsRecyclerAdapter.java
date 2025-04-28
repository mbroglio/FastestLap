package com.the_coffe_coders.fastestlap.adapter;

import android.content.Context;
import android.content.Intent;
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
import com.the_coffe_coders.fastestlap.ui.bio.ConstructorBioActivity;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModel;
import com.the_coffe_coders.fastestlap.ui.standing.ConstructorsStandingActivity;
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
    private View constructorCard;
    private int counter;

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
        constructorCard = LayoutInflater.from(parent.getContext()).inflate(R.layout.team_card, parent, false);
        return new ConstructorViewHolder(constructorCard);
    }

    @Override
    public void onBindViewHolder(@NonNull ConstructorViewHolder holder, int position) {
        constructorStandingsElement = constructorStandingsList.get(position);
        String currentConstructorId = constructorStandingsElement.getConstructor().getConstructorId();

        constructorViewModel.getSelectedConstructor(currentConstructorId).observe(lifecycleOwner, result -> {
            if(result instanceof Result.Loading){
                return;
            }
            if(result.isSuccess()){
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

                if (currentConstructorId.equals(constructorId)) {
                    UIUtils.animateCardBackgroundColor(context, holder.constructorCardView, R.color.yellow, Color.TRANSPARENT, 1000, 10);
                }

                constructorCard.setOnClickListener(v -> {
                    Intent intent = new Intent(context, ConstructorBioActivity.class);
                    intent.putExtra("TEAM_ID", currentConstructorId);
                    context.startActivity(intent);
                });

                UIUtils.loadSequenceOfImagesWithGlide(context,
                        new String[]{
                                constructor.getCar_pic_url(),
                                constructor.getTeam_logo_url()},
                        new ImageView[]{
                                holder.constructorCarImage,
                                holder.constructorLogo},

                        () -> processDriverOne(holder, constructor));
            }
        });

    }

    private void processDriverOne(ConstructorViewHolder holder, Constructor constructor) {
        driverViewModel.getDriver(constructor.getDriverOneId()).observe(lifecycleOwner, result -> {
            if(result instanceof Result.Loading){
                return;
            }
            if(result.isSuccess()){
                Driver driverOne = ((Result.DriverSuccess) result).getData();

                UIUtils.singleSetTextViewText(driverOne.getFullName(), holder.driverOneName);
                UIUtils.loadImageWithGlide(context, driverOne.getDriver_pic_url(), holder.driverOneImage,
                        () -> processDriverTwo(holder, constructor));
            }
        });
    }

    private void processDriverTwo(ConstructorViewHolder holder, Constructor constructor) {
        driverViewModel.getDriver(constructor.getDriverTwoId()).observe(lifecycleOwner, result -> {
            if(result instanceof Result.Loading){
                return;
            }
            if(result.isSuccess()){
                Driver driverTwo = ((Result.DriverSuccess) result).getData();

                UIUtils.singleSetTextViewText(driverTwo.getFullName(), holder.driverTwoName);
                UIUtils.loadImageWithGlide(context, driverTwo.getDriver_pic_url(), holder.driverTwoImage, () -> {

                    counter++;
                    Log.i("ConstructorsStanding", "onBindViewHolder " + counter + "/" + getItemCount());
                    loadingScreen.hideLoadingScreenWithCondition(counter == getItemCount() - 1);
                });
            }
        });
    }

    @Override
    public int getItemCount() {
        return constructorStandingsList.size();
    }

    public static class ConstructorViewHolder extends RecyclerView.ViewHolder {

        TextView constructorName, constructorPoints, constructorPosition, driverOneName, driverTwoName;
        ImageView constructorLogo, constructorCarImage, driverOneImage, driverTwoImage;
        LinearLayout constructorCardInnerLayout;
        MaterialCardView constructorCardView;

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
            constructorCardView = itemView.findViewById(R.id.team_card_view);
        }
    }
}
