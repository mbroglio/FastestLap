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
import com.the_coffe_coders.fastestlap.util.NetworkUtils;
import com.the_coffe_coders.fastestlap.util.SharedPreferencesUtils;
import com.the_coffe_coders.fastestlap.util.ui.NavigationUtils;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import lombok.Setter;

public class FavoriteConstructorHandler {
    private static final String TAG = FavoriteConstructorHandler.class.getSimpleName();

    private final Fragment fragment;
    private final Context context;
    private final LifecycleOwner lifecycleOwner;
    private final View view;
    private final HomeViewModel homeViewModel;
    private final ConstructorViewModel constructorViewModel;
    private final NationViewModel nationViewModel;
    private final UserViewModel userViewModel;
    private final NetworkUtils networkLiveData;
    private final SharedPreferencesUtils sharedPreferencesUtils;
    private final CardLoadedCallback cardLoadedCallback;

    @Setter
    private ConstructorStandings cachedConstructorStandings = null;
    private boolean constructorCardLoaded = false;

    @FunctionalInterface
    public interface CardLoadedCallback {
        void onCardLoaded(String cardName);
    }

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
    public void setupFavoriteConstructorCard() {
        String favoriteTeamId = getFavoriteTeamId();
        if (favoriteTeamId == null || favoriteTeamId.isEmpty() || favoriteTeamId.equals("null")) {
            Log.i(TAG, "Showing select favourite constructor card");
            showSelectFavouriteConstructor();
            return;
        }

        // Use cached data if available, otherwise fetch
        if (cachedConstructorStandings != null) {
            processConstructorStandings(favoriteTeamId, cachedConstructorStandings);
            return;
        }

        // Fetch standings - LiveData will handle multiple observers gracefully
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

        constructorStandingsData.observe(lifecycleOwner, result -> {
            try {
                if (result instanceof Result.Loading) {
                    return;
                }

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
                observerCalled[0] = true;
                processConstructorStandings(favoriteTeamId, null);
            } catch (Exception e) {
                Log.e(TAG, "Error in setFavouriteConstructorCard: " + e.getMessage());
                observerCalled[0] = true;
                processConstructorStandings(favoriteTeamId, null);
            }
        });
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
        constructorData.observe(lifecycleOwner, constructorResult -> {
            try {
                if (constructorResult instanceof Result.Loading) {
                    return;
                }
                if (constructorResult.isSuccess()) {
                    Constructor constructor = ((Result.ConstructorSuccess) constructorResult).getData();
                    favouriteConstructor.setConstructor(constructor);
                    Log.i(TAG, "Fetching nation data for constructor card");
                    fetchNationForConstructor(favouriteConstructor);
                } else {
                    throw new Exception("Failed to fetch constructor data: " + constructorResult.getError());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching constructor data: " + e.getMessage());
                showConstructorNotFound(0);
            }
        });
    }

    private void fetchNationForConstructor(ConstructorStandingsElement favouriteConstructor) {
        try {
            MutableLiveData<Result> nationData = nationViewModel.getNation(favouriteConstructor.getConstructor().getNationality());
            nationData.observe(lifecycleOwner, nationResult -> {
                try {
                    if (nationResult instanceof Result.Loading) {
                        return;
                    }
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
            });
        } catch (RuntimeException e) {
            Log.e(TAG, "Error fetching nation for constructor: " + e.getMessage());
            buildConstructorCard(favouriteConstructor, null);
        }
    }

    private void buildConstructorCard(ConstructorStandingsElement standingElement, Nation nation) {
        if (networkLiveData.isConnected() && userViewModel.getLoggedUser() != null) {
            try {
                Constructor constructor = standingElement.getConstructor();

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

                UIUtils.loadImagesInParallel(context,
                    new String[]{nationFlagUrl, constructor.getCar_pic_url()},
                    new ImageView[]{constructorFlag, constructorCar},
                    () -> buildConstructorCardFinalStep(standingElement, constructor));

            } catch (Exception e) {
                Log.e(TAG, "Error building constructor card: " + e.getMessage());
                showConstructorNotFound(0);
            }
        } else {
            Log.e(TAG, "Error building constructor card: No internet connection");
            showConstructorNotFound(1);
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
}

