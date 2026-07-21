package com.the_coffe_coders.fastestlap.adapter.f1;

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
import com.the_coffe_coders.fastestlap.domain.f1.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.f1.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.f1.standing.ConstructorStandingsElement;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModel;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ui.NavigationUtils;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class ConstructorStandingsRecyclerAdapter extends RecyclerView.Adapter<ConstructorStandingsRecyclerAdapter.ConstructorViewHolder> {
    private final Context context;
    private final String constructorId;
    private final DriverViewModel driverViewModel;
    private final ConstructorViewModel constructorViewModel;
    private final List<ConstructorStandingsElement> constructorStandingsList;
    private final List<Constructor> constructorList;
    private final LifecycleOwner lifecycleOwner;
    private final LoadingScreen loadingScreen;
    // Tracks which items have fully loaded (constructor + drivers) so we only count them once.
    private final boolean[] loadedPositions;
    private final int targetLoadCount;
    private int currentLoadedCount = 0;

    public ConstructorStandingsRecyclerAdapter(Context context, String constructorId, List<ConstructorStandingsElement> constructorStandingsList,
                                               List<Constructor> constructorList, DriverViewModel driverViewModel, ConstructorViewModel constructorViewModel,
                                               LifecycleOwner lifecycleOwner, LoadingScreen loadingScreen) {
        this.context = context;
        this.constructorId = constructorId;
        this.constructorStandingsList = constructorStandingsList;
        this.constructorList = constructorList;
        this.driverViewModel = driverViewModel;
        this.constructorViewModel = constructorViewModel;
        this.lifecycleOwner = lifecycleOwner;
        this.loadingScreen = loadingScreen;
        this.targetLoadCount = Math.min(getItemCount(), 3);
        this.loadedPositions = new boolean[getItemCount()];
    }

    @NonNull
    @Override
    public ConstructorStandingsRecyclerAdapter.ConstructorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.team_card, parent, false);
        return new ConstructorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConstructorViewHolder holder, int position) {
        // Build a local element so parallel binds don't share mutable state.
        final ConstructorStandingsElement element = new ConstructorStandingsElement();
        if (constructorStandingsList == null) {
            element.setConstructor(constructorList.get(position));
            element.setPoints("0");
            element.setPosition(String.valueOf(position + 1));
        } else {
            element.setConstructor(constructorStandingsList.get(position).getConstructor());
            element.setPoints(constructorStandingsList.get(position).getPoints());
            element.setPosition(constructorStandingsList.get(position).getPosition());
        }

        final String currentConstructorId = element.getConstructor().getConstructorId();

        // Use an array so the lambda can reference its own observer and remove it after the
        // first non-Loading result. Without this, LiveData emitting twice (cache then network)
        // would call endLoading twice per position and decrement pendingCards prematurely.
        androidx.lifecycle.LiveData<Result> constructorLd =
                constructorViewModel.getSelectedConstructor(currentConstructorId);
        androidx.lifecycle.Observer<Result>[] selfRef = new androidx.lifecycle.Observer[1];
        selfRef[0] = result -> {
            if (result instanceof Result.Loading) return;
            // Self-remove: fires at most once per bind regardless of how many times
            // the LiveData emits (cache + network).
            constructorLd.removeObserver(selfRef[0]);
            loadingScreen.updateProgress();

            if (result.isSuccess()) {
                showConstructorFound(holder);
                Constructor constructor = ((Result.ConstructorSuccess) result).getData();
                element.setConstructor(constructor);

                try {
                    holder.constructorCardInnerLayout.setBackground(AppCompatResources.getDrawable(context,
                            Objects.requireNonNull(Constants.TEAM_GRADIENT_COLOR.get(currentConstructorId))));
                } catch (Exception e) {
                    holder.constructorCardInnerLayout.setBackground(
                            AppCompatResources.getDrawable(context, R.color.timer_gray));
                }

                UIUtils.setTextViewTextWithCondition(element.getPosition() == null,
                        ContextCompat.getString(context, R.string.last_constructor_position),
                        element.getPosition(),
                        holder.constructorPosition);

                UIUtils.multipleSetTextViewText(
                        new String[]{constructor.getName(), element.getPoints()},
                        new TextView[]{holder.constructorName, holder.constructorPoints});

                if (constructorId != null && currentConstructorId.equals(constructorId)) {
                    UIUtils.animateCardBackgroundColor(context, holder.constructorCard,
                            R.color.yellow, Color.TRANSPARENT, 1000, 10);
                }

                holder.constructorCard.setOnClickListener(v -> goToBioPage(position));

                UIUtils.loadImagesInParallel(context,
                        new String[]{constructor.getCar_pic_url(), constructor.getTeam_logo_url()},
                        new ImageView[]{holder.constructorCarImage, holder.constructorLogo},
                        () -> {
                            loadingScreen.updateProgress();
                            loadBothDriversInParallel(holder, constructor, position);
                        });
            } else {
                showConstructorNotFound(holder, currentConstructorId);
                // Ensure the latch is decremented even on error so the loading screen
                // is not blocked forever by a failed constructor fetch.
                endLoading(position);
            }
        };

        try {
            constructorLd.observe(lifecycleOwner, selfRef[0]);
        } catch (RuntimeException e) {
            Log.e("ConstructorsStandingAdapter", "constructor error: " + e.getMessage());
            showConstructorNotFound(holder, currentConstructorId);
            endLoading(position);
        }
    }

    private void goToBioPage(int position) {
        String constructorIdToShow;
        if (constructorStandingsList == null) {
            constructorIdToShow = constructorList.get(position).getConstructorId();
        } else {
            constructorIdToShow = constructorStandingsList.get(position).getConstructor().getConstructorId();
        }
        NavigationUtils.navigateToBioPage(context, constructorIdToShow, 0);
    }

    /**
     * Fetches and loads both driver images in parallel using self-removing observers.
     * An {@link AtomicInteger} countdown starts at 2; each driver decrements it when its
     * image is ready. When it reaches 0, both are done and {@link #endLoading} is called.
     * Self-removing observers prevent LiveData double-emissions (cache + network) from
     * triggering duplicate image loads or extra endLoading calls.
     */
    private void loadBothDriversInParallel(ConstructorViewHolder holder, Constructor constructor, int position) {
        AtomicInteger driversReady = new AtomicInteger(2);
        Runnable onOneDriverReady = () -> {
            loadingScreen.updateProgress();
            if (driversReady.decrementAndGet() == 0) {
                endLoading(position);
            }
        };

        // Driver 1 — self-removing observer
        androidx.lifecycle.LiveData<Result> d1Ld = driverViewModel.getDriver(constructor.getDriverOneId());
        androidx.lifecycle.Observer<Result>[] d1Ref = new androidx.lifecycle.Observer[1];
        d1Ref[0] = result -> {
            if (result instanceof Result.Loading) return;
            d1Ld.removeObserver(d1Ref[0]);
            loadingScreen.updateProgress();
            if (result.isSuccess()) {
                Driver d1 = ((Result.DriverSuccess) result).getData();
                UIUtils.singleSetTextViewText(d1.getFullName(), holder.driverOneName);
                UIUtils.loadImageWithGlide(context, d1.getDriver_half_pic_url(), holder.driverOneImage, onOneDriverReady);
            } else {
                String id = constructor.getDriverOneId();
                if (id != null) {
                    UIUtils.singleSetTextViewText(
                            id.contains("_") ? id.split("_")[1].toUpperCase(java.util.Locale.ROOT) : id.toUpperCase(java.util.Locale.ROOT),
                            holder.driverOneName);
                } else {
                    UIUtils.singleSetTextViewText("N/A", holder.driverOneName);
                }
                UIUtils.loadImageWithGlide(context, null, holder.driverOneImage, onOneDriverReady);
            }
        };
        try {
            d1Ld.observe(lifecycleOwner, d1Ref[0]);
        } catch (RuntimeException e) {
            UIUtils.loadImageWithGlide(context, null, holder.driverOneImage, onOneDriverReady);
        }

        // Driver 2 — self-removing observer, starts immediately (does NOT wait for driver 1)
        androidx.lifecycle.LiveData<Result> d2Ld = driverViewModel.getDriver(constructor.getDriverTwoId());
        androidx.lifecycle.Observer<Result>[] d2Ref = new androidx.lifecycle.Observer[1];
        d2Ref[0] = result -> {
            if (result instanceof Result.Loading) return;
            d2Ld.removeObserver(d2Ref[0]);
            loadingScreen.updateProgress();
            if (result.isSuccess()) {
                Driver d2 = ((Result.DriverSuccess) result).getData();
                UIUtils.singleSetTextViewText(d2.getFullName(), holder.driverTwoName);
                UIUtils.loadImageWithGlide(context, d2.getDriver_half_pic_url(), holder.driverTwoImage, onOneDriverReady);
            } else {
                String id = constructor.getDriverTwoId();
                if (id != null) {
                    UIUtils.singleSetTextViewText(
                            id.contains("_") ? id.split("_")[1].toUpperCase(java.util.Locale.ROOT) : id.toUpperCase(java.util.Locale.ROOT),
                            holder.driverTwoName);
                } else {
                    UIUtils.singleSetTextViewText("N/A", holder.driverTwoName);
                }
                UIUtils.loadImageWithGlide(context, null, holder.driverTwoImage, onOneDriverReady);
            }
        };
        try {
            d2Ld.observe(lifecycleOwner, d2Ref[0]);
        } catch (RuntimeException e) {
            UIUtils.loadImageWithGlide(context, null, holder.driverTwoImage, onOneDriverReady);
        }
    }

    private void endLoading(int position) {
        boolean shouldHide = false;
        synchronized (this) {
            if (position >= 0 && position < loadedPositions.length && !loadedPositions[position]) {
                loadedPositions[position] = true;
                currentLoadedCount++;
                Log.i("ConstructorsStanding", "onBindViewHolder " + position + "/" + getItemCount() + " — loaded: " + currentLoadedCount + "/" + targetLoadCount);
                if (currentLoadedCount >= targetLoadCount) {
                    shouldHide = true;
                }
            }
        }
        if (shouldHide) {
            loadingScreen.hideLoadingScreen();
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

        if (constructorId != null) {
            if (constructorId.contains("_")) {
                constructorId = constructorId.split("_")[0] + " " + constructorId.split("_")[1];
            }
            UIUtils.singleSetTextViewText(constructorId.toUpperCase(java.util.Locale.ROOT) + " " + context.getString(R.string.constructor_info_not_found), holder.constructorNotFound);
        } else {
            UIUtils.singleSetTextViewText(context.getString(R.string.constructor_info_not_found), holder.constructorNotFound);
        }


    }

    @Override
    public int getItemCount() {
        if (constructorList != null)
            return constructorList.size();
        else if (constructorStandingsList != null)
            return constructorStandingsList.size();
        else
            return 0;
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
