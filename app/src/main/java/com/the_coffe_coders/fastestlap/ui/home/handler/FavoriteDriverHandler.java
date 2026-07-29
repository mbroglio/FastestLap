package com.the_coffe_coders.fastestlap.ui.home.handler;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;

import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.f1.standing.DriverStandings;
import com.the_coffe_coders.fastestlap.domain.f1.standing.DriverStandingsElement;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.NationViewModel;
import com.the_coffe_coders.fastestlap.ui.home.viewmodel.HomeViewModel;
import com.the_coffe_coders.fastestlap.ui.standing.DriversStandingActivity;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModel;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.NetworkUtils;
import com.the_coffe_coders.fastestlap.util.SharedPreferencesUtils;
import com.the_coffe_coders.fastestlap.util.ui.NavigationUtils;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import lombok.Setter;

public class FavoriteDriverHandler {
    private static final String TAG = FavoriteDriverHandler.class.getSimpleName();

    private final Fragment fragment;
    private final Context context;
    private final HomeViewModel homeViewModel;
    private final DriverViewModel driverViewModel;
    private final NationViewModel nationViewModel;
    private final UserViewModel userViewModel;
    private final NetworkUtils networkLiveData;
    private final SharedPreferencesUtils sharedPreferencesUtils;
    private final CardLoadedCallback cardLoadedCallback;
    private LifecycleOwner lifecycleOwner;
    private View view;
    @Setter
    private DriverStandings cachedDriverStandings = null;
    private boolean driverCardLoaded = false;
    // Cached last-built card data so back-stack returns can skip the entire ViewModel chain.
    private DriverStandingsElement cachedStandingsElement = null;
    private Nation cachedNation = null;

    public FavoriteDriverHandler(Fragment fragment, View view,
                                 HomeViewModel homeViewModel,
                                 DriverViewModel driverViewModel,
                                 NationViewModel nationViewModel,
                                 UserViewModel userViewModel,
                                 NetworkUtils networkLiveData,
                                 SharedPreferencesUtils sharedPreferencesUtils,
                                 CardLoadedCallback cardLoadedCallback) {
        this.fragment = fragment;
        this.context = fragment.requireContext();
        this.lifecycleOwner = fragment.getViewLifecycleOwner();
        this.view = view;
        this.homeViewModel = homeViewModel;
        this.driverViewModel = driverViewModel;
        this.nationViewModel = nationViewModel;
        this.userViewModel = userViewModel;
        this.networkLiveData = networkLiveData;
        this.sharedPreferencesUtils = sharedPreferencesUtils;
        this.cardLoadedCallback = cardLoadedCallback;
    }

    public void updateView(View view, LifecycleOwner lifecycleOwner) {
        this.view = view;
        this.lifecycleOwner = lifecycleOwner;
    }

    public void resetCardLoaded() {
        this.driverCardLoaded = false;
        this.cachedStandingsElement = null;
        this.cachedNation = null;
    }

    public void setupFavoriteDriverCard() {
        String favoriteDriverId = getFavoriteDriverId();
        if (favoriteDriverId == null || favoriteDriverId.isEmpty() || favoriteDriverId.equals("null")) {
            showSelectFavouriteDriver();
            return;
        }

        // Fast-path for back-stack returns: the card was already built and all data is cached
        // in memory. Skip the entire ViewModel/observer chain and go straight to buildDriverCard.
        // This avoids re-downloading the driver image on every back-stack return.
        if (driverCardLoaded && cachedStandingsElement != null) {
            Log.i(TAG, "Driver card already built — rebuilding from in-memory cache (fast path)");
            buildDriverCard(cachedStandingsElement, cachedNation);
            return;
        }

        // Use cached standings data if available, otherwise fetch
        if (cachedDriverStandings != null) {
            processDriverStandings(favoriteDriverId, cachedDriverStandings);
            return;
        }

        // Fetch standings using a one-shot observer that removes itself after the first
        // non-Loading result. Without this, the background Firebase refresh re-emits the
        // same LiveData and triggers a full card rebuild for every emission.
        MutableLiveData<Result> driverStandingsLiveData = homeViewModel.getDriverStandingsLiveData(fragment.requireActivity().getApplication());

        // Track if observer was called with final result
        final boolean[] observerCalled = {false};

        // Timeout: if no final result within 2 seconds, proceed without standings
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!observerCalled[0] && !driverCardLoaded) {
                Log.w(TAG, "Driver standings timeout - proceeding with driver data only");
                processDriverStandings(favoriteDriverId, null);
            }
        }, 2000);

        @SuppressWarnings("unchecked")
        androidx.lifecycle.Observer<Result>[] observerHolder = new androidx.lifecycle.Observer[1];
        observerHolder[0] = result -> {
            try {
                if (result instanceof Result.Loading) {
                    return;
                }

                // One-shot: remove this observer so future re-emissions (e.g. background
                // Firebase refresh) do not trigger another full card rebuild.
                driverStandingsLiveData.removeObserver(observerHolder[0]);
                observerCalled[0] = true;

                if (result instanceof Result.DriverStandingsSuccess) {
                    DriverStandings driverStandings = ((Result.DriverStandingsSuccess) result).getData();
                    cachedDriverStandings = driverStandings;
                    processDriverStandings(favoriteDriverId, driverStandings);
                } else {
                    // Standings fetch failed - still try to create card with driver data only
                    Log.w(TAG, "Failed to fetch driver standings: " + result.getError());
                    processDriverStandings(favoriteDriverId, null);
                }
            } catch (ClassCastException e) {
                Log.e(TAG, "Type mismatch in setFavouriteDriverCard - wrong result type received: " + e.getMessage());
                driverStandingsLiveData.removeObserver(observerHolder[0]);
                observerCalled[0] = true;
                processDriverStandings(favoriteDriverId, null);
            } catch (Exception e) {
                Log.e(TAG, "Error in setFavouriteDriverCard: " + e.getMessage());
                driverStandingsLiveData.removeObserver(observerHolder[0]);
                observerCalled[0] = true;
                processDriverStandings(favoriteDriverId, null);
            }
        };
        driverStandingsLiveData.observe(lifecycleOwner, observerHolder[0]);
    }

    private void processDriverStandings(String favoriteDriverId, DriverStandings driverStandings) {
        try {
            DriverStandingsElement favouriteDriver = null;
            if (driverStandings != null && driverStandings.getDriverStandingsElements() != null) {
                favouriteDriver = homeViewModel.getDriverStandingsElement(driverStandings.getDriverStandingsElements(), favoriteDriverId);
            }

            if (favouriteDriver == null) {
                // No standing found - create a card with driver data only (no position/points)
                Log.i(TAG, "No standing found for driver, fetching driver data only");
                favouriteDriver = new DriverStandingsElement();
                fetchDriverDataForCard(favoriteDriverId, favouriteDriver);
            } else {
                Log.i(TAG, "Fetching driver data card");
                fetchDriverDataForCard(favoriteDriverId, favouriteDriver);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing driver standings: " + e.getMessage());
            showDriverNotFound(0);
        }
    }

    private void fetchDriverDataForCard(String driverId, DriverStandingsElement favouriteDriver) {
        MutableLiveData<Result> driverData = driverViewModel.getDriver(driverId);
        // One-shot observer: removes itself after the first non-Loading result so that
        // a subsequent background Firebase re-emission does not rebuild the card again.
        @SuppressWarnings("unchecked")
        androidx.lifecycle.Observer<Result>[] observerHolder = new androidx.lifecycle.Observer[1];
        observerHolder[0] = driverResult -> {
            try {
                if (driverResult instanceof Result.Loading) {
                    return;
                }
                driverData.removeObserver(observerHolder[0]);
                if (driverResult.isSuccess()) {
                    Driver driver = ((Result.DriverSuccess) driverResult).getData();
                    favouriteDriver.setDriver(driver);

                    // Preload the driver image immediately so Glide's disk cache is warm
                    // by the time buildDriverCard calls loadImagesInParallel.
                    UIUtils.preloadImage(context, driver.getDriver_half_pic_url());

                    Log.i(TAG, "Fetching nation data for driver card");
                    fetchNationForDriver(favouriteDriver);
                } else {
                    throw new Exception("Failed to fetch driver data: " + driverResult.getError());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching driver data: " + e.getMessage());
                showDriverNotFound(0);
            }
        };
        driverData.observe(lifecycleOwner, observerHolder[0]);
    }

    private void fetchNationForDriver(DriverStandingsElement favouriteDriver) {
        try {
            MutableLiveData<Result> nationData = nationViewModel.getNation(favouriteDriver.getDriver().getNationality());
            // One-shot observer: removes itself after the first non-Loading result so that
            // a subsequent background Firebase re-emission does not rebuild the card again.
            @SuppressWarnings("unchecked")
            androidx.lifecycle.Observer<Result>[] observerHolder = new androidx.lifecycle.Observer[1];
            observerHolder[0] = nationResult -> {
                try {
                    if (nationResult instanceof Result.Loading) {
                        return;
                    }
                    nationData.removeObserver(observerHolder[0]);
                    if (nationResult.isSuccess()) {
                        Nation nation = ((Result.NationSuccess) nationResult).getData();
                        Log.i(TAG, "Building driver card");
                        buildDriverCard(favouriteDriver, nation);
                    } else {
                        throw new Exception("Failed to fetch nation data: " + nationResult.getError());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching nation for driver: " + e.getMessage());
                    showDriverNotFound(0);
                }
            };
            nationData.observe(lifecycleOwner, observerHolder[0]);
        } catch (RuntimeException e) {
            Log.e(TAG, "Error fetching nation for driver: " + e.getMessage());
            buildDriverCard(favouriteDriver, null);
        }
    }

    private void buildDriverCard(DriverStandingsElement standingElement, Nation nation) {
        try {
            Driver driver = standingElement.getDriver();

            // Cache the card data so back-stack returns can take the fast path.
            cachedStandingsElement = standingElement;
            cachedNation = nation;

            String nationFlagUrl = null;
            String nationAbbreviation = null;
            if (nation != null) {
                nationFlagUrl = nation.getNation_flag_url();
                nationAbbreviation = nation.getAbbreviation();
            }

            UIUtils.multipleSetTextViewText(
                    new String[]{driver.getGivenName() + " " + driver.getFamilyName(), nationAbbreviation},
                    new TextView[]{view.findViewById(R.id.favourite_driver_name), view.findViewById(R.id.favourite_driver_nationality)}
            );

            ImageView driverFlag = view.findViewById(R.id.favourite_driver_flag);
            ImageView driverImage = view.findViewById(R.id.favourite_driver_pic);
            driverImage.setOnClickListener(v -> NavigationUtils.navigateToBioPage(context, driver.getDriverId(), 1));

            buildDriverCardFinalStep(standingElement, driver);

            UIUtils.loadImageAsync(context, nationFlagUrl, driverFlag);
            UIUtils.loadImageAsync(context, driver.getDriver_half_pic_url(), driverImage);
        } catch (Exception e) {
            Log.e(TAG, "Error building driver card: " + e.getMessage());
            showDriverNotFound(0);
        }
    }

    private void buildDriverCardFinalStep(DriverStandingsElement standingElement, Driver driver) {
        if (standingElement.getPosition() != null && standingElement.getPoints() != null) {
            UIUtils.multipleSetTextViewText(
                    new String[]{standingElement.getPosition(), standingElement.getPoints()},
                    new TextView[]{view.findViewById(R.id.favourite_driver_position), view.findViewById(R.id.favourite_driver_points)}
            );

            MaterialCardView driverRank = view.findViewById(R.id.favourite_driver_rank);
            driverRank.setOnClickListener(v -> NavigationUtils.navigateToStandingsPage(context, driver.getDriverId(), 1));
        } else {
            MaterialCardView driverRank = view.findViewById(R.id.favourite_driver_rank);
            driverRank.setClickable(false);
        }

        Log.i(TAG, "Driver card built successfully");
        showFavouriteDriverCard();
        driverCardLoaded = true;
        cardLoadedCallback.onCardLoaded("driver");
    }

    private void showSelectFavouriteDriver() {
        cardLoadedCallback.onCardLoaded("driver");
        updateVisibility(R.id.pending_favorite_driver, R.id.favorite_driver, R.id.missing_favorite_driver);
        view.findViewById(R.id.pending_favorite_driver).setOnClickListener(v ->
                context.startActivity(new Intent(context, DriversStandingActivity.class)));
        Log.e(TAG, "Showing select favourite driver card");
    }

    private void showDriverNotFound(int problem) {
        cardLoadedCallback.onCardLoaded("driver");
        updateVisibility(R.id.missing_favorite_driver, R.id.favorite_driver, R.id.pending_favorite_driver);

        switch (problem) {
            case 0: //general error
                view.findViewById(R.id.missing_favorite_driver).setOnClickListener(v ->
                        context.startActivity(new Intent(context, DriversStandingActivity.class)));
                break;
            case 1: //no internet connection
                Log.e(TAG, "Driver: No internet connection");
                view.findViewById(R.id.missing_favorite_driver).setOnClickListener(v ->
                        Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show());
                break;
            default:
                Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show();
        }
    }

    private void showFavouriteDriverCard() {
        updateVisibility(R.id.favorite_driver, R.id.missing_favorite_driver, R.id.pending_favorite_driver);
    }

    private void updateVisibility(int visibleId, int... goneIds) {
        view.findViewById(visibleId).setVisibility(View.VISIBLE);
        for (int id : goneIds) {
            view.findViewById(id).setVisibility(View.GONE);
        }
    }

    private String getFavoriteDriverId() {
        String driverId = sharedPreferencesUtils.readStringData(Constants.SHARED_PREFERENCES_FILENAME, Constants.SHARED_PREFERENCES_FAVORITE_DRIVER);
        Log.i(TAG, "Favorite Driver ID: " + driverId);
        return driverId;
    }

    @FunctionalInterface
    public interface CardLoadedCallback {
        void onCardLoaded(String cardName);
    }
}

