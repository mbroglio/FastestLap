package com.the_coffe_coders.fastestlap.ui.home.handler;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;

import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.f1.standing.ConstructorStandings;
import com.the_coffe_coders.fastestlap.domain.f1.standing.ConstructorStandingsElement;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.ConstructorViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.NationViewModel;
import com.the_coffe_coders.fastestlap.ui.home.viewmodel.HomeViewModel;
import com.the_coffe_coders.fastestlap.ui.standing.ConstructorsStandingActivity;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModel;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.service.NetworkUtils;
import com.the_coffe_coders.fastestlap.util.service.SharedPreferencesUtils;
import com.the_coffe_coders.fastestlap.util.ui.NavigationUtils;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import lombok.Setter;

public class FavoriteConstructorHandler {
    private static final String TAG = FavoriteConstructorHandler.class.getSimpleName();

    private final Fragment fragment;
    private final Context context;
    private final HomeViewModel homeViewModel;
    private final ConstructorViewModel constructorViewModel;
    private final NationViewModel nationViewModel;
    private final UserViewModel userViewModel;
    private final NetworkUtils networkLiveData;
    private final SharedPreferencesUtils sharedPreferencesUtils;
    private final CardLoadedCallback cardLoadedCallback;
    private LifecycleOwner lifecycleOwner;
    private View view;
    @Setter
    private ConstructorStandings cachedConstructorStandings = null;
    private boolean constructorCardLoaded = false;
    // Cached last-built card data so back-stack returns can skip the entire ViewModel chain.
    private ConstructorStandingsElement cachedStandingsElement = null;
    private Nation cachedNation = null;

    public FavoriteConstructorHandler(Fragment fragment, View view,
                                      HomeViewModel homeViewModel,
                                      ConstructorViewModel constructorViewModel,
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
        this.constructorViewModel = constructorViewModel;
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
        this.constructorCardLoaded = false;
        this.cachedStandingsElement = null;
        this.cachedNation = null;
    }

    public void setupFavoriteConstructorCard() {
        String favoriteTeamId = getFavoriteTeamId();
        if (favoriteTeamId == null || favoriteTeamId.isEmpty() || favoriteTeamId.equals("null")) {
            Log.i(TAG, "Showing select favourite constructor card");
            showSelectFavouriteConstructor();
            return;
        }

        // Fast-path for back-stack returns: the card was already built and all data is cached
        // in memory. Skip the entire ViewModel/observer chain and go straight to buildConstructorCard.
        // This avoids re-downloading the car image on every back-stack return.
        if (constructorCardLoaded && cachedStandingsElement != null) {
            Log.i(TAG, "Constructor card already built — rebuilding from in-memory cache (fast path)");
            buildConstructorCard(cachedStandingsElement, cachedNation);
            return;
        }

        // Use cached standings data if available, otherwise fetch
        if (cachedConstructorStandings != null) {
            processConstructorStandings(favoriteTeamId, cachedConstructorStandings);
            return;
        }

        // Fetch standings using a one-shot observer that removes itself after the first
        // non-Loading result. Without this, the background Firebase refresh re-emits the
        // same LiveData and triggers a full card rebuild for every emission.
        MutableLiveData<Result> constructorStandingsData = homeViewModel.getConstructorStandingsLiveData(fragment.requireActivity().getApplication());

        // Track if observer was called with final result
        final boolean[] observerCalled = {false};

        // Timeout: if no final result within 2 seconds, proceed without standings
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!observerCalled[0] && !constructorCardLoaded) {
                Log.w(TAG, "Constructor standings timeout - proceeding with constructor data only");
                processConstructorStandings(favoriteTeamId, null);
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
                constructorStandingsData.removeObserver(observerHolder[0]);
                observerCalled[0] = true;

                if (result instanceof Result.ConstructorStandingsSuccess) {
                    ConstructorStandings standings = ((Result.ConstructorStandingsSuccess) result).getData();
                    cachedConstructorStandings = standings;
                    processConstructorStandings(favoriteTeamId, standings);
                } else {
                    // Standings fetch failed - still try to create card with constructor data only
                    Log.w(TAG, "Failed to fetch constructor standings: " + result.getError());
                    processConstructorStandings(favoriteTeamId, null);
                }
            } catch (ClassCastException e) {
                Log.e(TAG, "Type mismatch in setFavouriteConstructorCard - wrong result type received: " + e.getMessage());
                constructorStandingsData.removeObserver(observerHolder[0]);
                observerCalled[0] = true;
                processConstructorStandings(favoriteTeamId, null);
            } catch (Exception e) {
                Log.e(TAG, "Error in setFavouriteConstructorCard: " + e.getMessage());
                constructorStandingsData.removeObserver(observerHolder[0]);
                observerCalled[0] = true;
                processConstructorStandings(favoriteTeamId, null);
            }
        };
        constructorStandingsData.observe(lifecycleOwner, observerHolder[0]);
    }

    private void processConstructorStandings(String favoriteTeamId, ConstructorStandings standings) {
        try {
            ConstructorStandingsElement favouriteConstructor = null;
            if (standings != null && standings.getConstructorStandings() != null) {
                favouriteConstructor = homeViewModel.getConstructorStandingsElement(standings.getConstructorStandings(), favoriteTeamId);
            }

            if (favouriteConstructor == null) {
                // No standing found - create a card with constructor data only (no position/points)
                Log.i(TAG, "No standing found for constructor, fetching constructor data only");
                favouriteConstructor = new ConstructorStandingsElement();
                fetchConstructorDataForCard(favoriteTeamId, favouriteConstructor);
            } else {
                Log.i(TAG, "Fetching constructor data card");
                fetchConstructorDataForCard(favoriteTeamId, favouriteConstructor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing constructor standings: " + e.getMessage());
            showConstructorNotFound(0);
        }
    }

    private void fetchConstructorDataForCard(String teamId, ConstructorStandingsElement favouriteConstructor) {
        MutableLiveData<Result> constructorData = constructorViewModel.getSelectedConstructor(teamId);
        // One-shot observer: removes itself after the first non-Loading result so that
        // a subsequent background Firebase re-emission does not rebuild the card again.
        @SuppressWarnings("unchecked")
        androidx.lifecycle.Observer<Result>[] observerHolder = new androidx.lifecycle.Observer[1];
        observerHolder[0] = constructorResult -> {
            try {
                if (constructorResult instanceof Result.Loading) {
                    return;
                }
                constructorData.removeObserver(observerHolder[0]);
                if (constructorResult.isSuccess()) {
                    Constructor constructor = ((Result.ConstructorSuccess) constructorResult).getData();
                    favouriteConstructor.setConstructor(constructor);

                    // Preload the car image immediately so Glide's disk cache is warm
                    // by the time buildConstructorCard calls loadImagesInParallel.
                    // This download runs in parallel with the nation fetch below (~200ms ahead).
                    UIUtils.preloadImage(context, constructor.getCar_pic_url());

                    Log.i(TAG, "Fetching nation data for constructor card");
                    fetchNationForConstructor(favouriteConstructor);
                } else {
                    throw new Exception("Failed to fetch constructor data: " + constructorResult.getError());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching constructor data: " + e.getMessage());
                showConstructorNotFound(0);
            }
        };
        constructorData.observe(lifecycleOwner, observerHolder[0]);
    }

    private void fetchNationForConstructor(ConstructorStandingsElement favouriteConstructor) {
        try {
            MutableLiveData<Result> nationData = nationViewModel.getNation(favouriteConstructor.getConstructor().getNationality());
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
                        Log.i(TAG, "Building constructor card");
                        buildConstructorCard(favouriteConstructor, nation);
                    } else {
                        throw new Exception("Failed to fetch nation data: " + nationResult.getError());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching nation for constructor: " + e.getMessage());
                    showConstructorNotFound(0);
                }
            };
            nationData.observe(lifecycleOwner, observerHolder[0]);
        } catch (RuntimeException e) {
            Log.e(TAG, "Error fetching nation for constructor: " + e.getMessage());
            buildConstructorCard(favouriteConstructor, null);
        }
    }

    private void buildConstructorCard(ConstructorStandingsElement standingElement, Nation nation) {
        try {
            Constructor constructor = standingElement.getConstructor();

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
                    new String[]{constructor.getName(), nationAbbreviation},
                    new TextView[]{view.findViewById(R.id.favourite_constructor_name), view.findViewById(R.id.favourite_constructor_nationality)}
            );

            ImageView constructorCar = view.findViewById(R.id.favourite_constructor_car);
            ImageView constructorFlag = view.findViewById(R.id.favourite_constructor_flag);

            FrameLayout constructorCard = view.findViewById(R.id.favourite_constructor_layout);
            constructorCard.setOnClickListener(v -> NavigationUtils.navigateToBioPage(context, constructor.getConstructorId(), 0));

            buildConstructorCardFinalStep(standingElement, constructor);

            UIUtils.loadImageAsync(context, nationFlagUrl, constructorFlag);
            UIUtils.loadImageAsync(context, constructor.getCar_pic_url(), constructorCar);
        } catch (Exception e) {
            Log.e(TAG, "Error building constructor card: " + e.getMessage());
            showConstructorNotFound(0);
        }
    }

    private void buildConstructorCardFinalStep(ConstructorStandingsElement standingElement, Constructor constructor) {
        if (standingElement.getPosition() != null && standingElement.getPoints() != null) {
            UIUtils.multipleSetTextViewText(
                    new String[]{standingElement.getPosition(), standingElement.getPoints()},
                    new TextView[]{view.findViewById(R.id.favourite_constructor_position), view.findViewById(R.id.favourite_constructor_points)}
            );

            MaterialCardView teamRank = view.findViewById(R.id.favourite_constructor_rank);
            teamRank.setOnClickListener(v -> NavigationUtils.navigateToStandingsPage(context, constructor.getConstructorId(), 0));
        } else {
            MaterialCardView teamRank = view.findViewById(R.id.favourite_constructor_rank);
            teamRank.setClickable(false);
        }

        showFavouriteConstructorCard();
        Log.i(TAG, "Constructor card built successfully");
        constructorCardLoaded = true;
        cardLoadedCallback.onCardLoaded("constructor");
    }

    private void showFavouriteConstructorCard() {
        updateVisibility(R.id.favorite_constructor, R.id.pending_favorite_constructor, R.id.missing_favorite_constructor);
    }

    private void showSelectFavouriteConstructor() {
        cardLoadedCallback.onCardLoaded("constructor");
        updateVisibility(R.id.pending_favorite_constructor, R.id.favorite_constructor, R.id.missing_favorite_constructor);
        view.findViewById(R.id.pending_favorite_constructor).setOnClickListener(v ->
                context.startActivity(new Intent(context, ConstructorsStandingActivity.class)));
    }

    private void showConstructorNotFound(int problem) {
        cardLoadedCallback.onCardLoaded("constructor");
        updateVisibility(R.id.missing_favorite_constructor, R.id.favorite_constructor, R.id.pending_favorite_constructor);

        switch (problem) {
            case 0: //general error
                view.findViewById(R.id.missing_favorite_constructor).setOnClickListener(v ->
                        context.startActivity(new Intent(context, ConstructorsStandingActivity.class)));
                break;
            case 1: //no internet connection
                Log.e(TAG, "Constructor: No internet connection");
                view.findViewById(R.id.missing_favorite_constructor).setOnClickListener(v ->
                        Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show());
                break;
            default:
                Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateVisibility(int visibleId, int... goneIds) {
        view.findViewById(visibleId).setVisibility(View.VISIBLE);
        for (int id : goneIds) {
            view.findViewById(id).setVisibility(View.GONE);
        }
    }

    private String getFavoriteTeamId() {
        String teamId = sharedPreferencesUtils.readStringData(Constants.SHARED_PREFERENCES_FILENAME, Constants.SHARED_PREFERENCES_FAVORITE_TEAM);
        Log.i(TAG, "Favorite Team ID: " + teamId);
        return teamId;
    }

    @FunctionalInterface
    public interface CardLoadedCallback {
        void onCardLoaded(String cardName);
    }
}

