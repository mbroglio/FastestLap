package com.the_coffe_coders.fastestlap.ui.home.handler;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.material.card.MaterialCardView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.domain.f1.result.RaceResult;
import com.the_coffe_coders.fastestlap.domain.f1.track.Track;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModel;
import com.the_coffe_coders.fastestlap.ui.event.EventActivity;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.RaceResultViewModel;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.WeeklyRaceViewModel;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.NetworkUtils;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import org.threeten.bp.LocalDateTime;

import java.util.List;

import lombok.Setter;

public class LastRaceHandler {
    private static final String TAG = LastRaceHandler.class.getSimpleName();

    private final Context context;
    private final WeeklyRaceViewModel weeklyRaceViewModel;
    private final TrackViewModel trackViewModel;
    private final RaceResultViewModel raceResultViewModel;
    private final CardLoadedCallback cardLoadedCallback;
    private LifecycleOwner lifecycleOwner;
    private View view;
    @Setter
    private String nextRaceRound;

    public LastRaceHandler(Fragment fragment, View view,
                           WeeklyRaceViewModel weeklyRaceViewModel,
                           TrackViewModel trackViewModel,
                           RaceResultViewModel raceResultViewModel,
                           NetworkUtils networkLiveData,
                           CardLoadedCallback cardLoadedCallback) {
        this.context = fragment.requireContext();
        this.lifecycleOwner = fragment.getViewLifecycleOwner();
        this.view = view;
        this.weeklyRaceViewModel = weeklyRaceViewModel;
        this.trackViewModel = trackViewModel;
        this.raceResultViewModel = raceResultViewModel;
        this.cardLoadedCallback = cardLoadedCallback;
    }

    public void updateView(View view, LifecycleOwner lifecycleOwner) {
        this.view = view;
        this.lifecycleOwner = lifecycleOwner;
    }

    public void setupLastRaceCard() {
        LiveData<Result> lastRace = weeklyRaceViewModel.getLastRace();
        // One-shot observer: removes itself after the first non-Loading result to prevent
        // observer accumulation on repeated setupLastRaceCard() calls.
        @SuppressWarnings("unchecked")
        androidx.lifecycle.Observer<Result>[] observerHolder = new androidx.lifecycle.Observer[1];
        observerHolder[0] = result -> {
            try {
                if (result instanceof Result.Loading) {
                    return;
                }
                lastRace.removeObserver(observerHolder[0]);
                if (result.isSuccess()) {
                    WeeklyRace raceResult = ((Result.NextRaceSuccess) result).getData();
                    Log.i(TAG, "Last Race: " + raceResult);

                    if (raceResult.getRound().equals(nextRaceRound)) {
                        // Next race and last race are the same - we're at the beginning of the season
                        setSeasonBeginning();
                        return;
                    }

                    showPodium(raceResult);
                } else {
                    throw new Exception("Failed to fetch last race: " + result.getError());
                }
            } catch (Exception e) {
                lastRace.removeObserver(observerHolder[0]);
                Log.e(TAG, "Error in setLastRaceCard: " + e.getMessage());
                loadPendingResultsLayout();
            }
        };
        lastRace.observe(lifecycleOwner, observerHolder[0]);
    }

    private void setSeasonBeginning() {
        Log.i(TAG, "Season beginning detected - next race equals last race");
        showLastRaceNotFound();
        cardLoadedCallback.onCardLoaded("lastRace");
    }

    private void showPodium(WeeklyRace race) {
        try {
            String circuitId = race.getTrack().getTrackId();
            MutableLiveData<Result> trackData = trackViewModel.getTrack(circuitId);
            trackData.observe(lifecycleOwner, trackResult -> {
                try {
                    if (trackResult instanceof Result.Loading) {
                        return;
                    }
                    if (trackResult.isSuccess()) {
                        Track track = ((Result.TrackSuccess) trackResult).getData();
                        updateLastRaceUI(race, track);
                    } else {
                        throw new Exception("Failed to fetch track data: " + trackResult.getError());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error loading track: " + e.getMessage());
                    loadPendingResultsLayout();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in showPodium: " + e.getMessage());
            loadPendingResultsLayout();
        }
    }

    private void updateLastRaceUI(WeeklyRace race, Track track) {
        try {
            UIUtils.singleSetTextViewText(race.getRaceName(), view.findViewById(R.id.last_race_name));
            UIUtils.loadImageWithGlide(context, track.getTrack_minimal_layout_url(),
                    view.findViewById(R.id.last_race_track_outline),
                    () -> updateLastRaceUIFinalStep(race));
        } catch (Exception e) {
            Log.e(TAG, "Error updating last race UI: " + e.getMessage());
            loadPendingResultsLayout();
        }
    }

    private void updateLastRaceUIFinalStep(WeeklyRace race) {
        LocalDateTime dateTime = race.getDateTime();

        UIUtils.multipleSetTextViewText(
                new String[]{
                        String.valueOf(dateTime.getDayOfMonth()),
                        context.getString(R.string.round_plus_value, race.getRound())
                },
                new TextView[]{
                        view.findViewById(R.id.last_race_date),
                        view.findViewById(R.id.last_race_round)
                }
        );

        UIUtils.translateMonth(dateTime.getMonth().toString().substring(0, 3).toUpperCase(java.util.Locale.ROOT),
                view.findViewById(R.id.last_race_month), true);

        MutableLiveData<Result> raceResultData = raceResultViewModel.getRaceResults(race.getRound());
        raceResultData.observe(lifecycleOwner, result -> {
            try {
                if (result instanceof Result.Loading) {
                    return;
                }
                if (result.isSuccess()) {
                    List<RaceResult> raceResults = ((Result.RaceResultsSuccess) result).getData().getResults();
                    setDriverNames(raceResults);
                } else {
                    throw new Exception("Failed to fetch race results: " + result.getError());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting driver names: " + e.getMessage());
            }
        });

        MaterialCardView resultCard = view.findViewById(R.id.past_event_result);
        resultCard.setOnClickListener(v -> {
            Intent intent = new Intent(context, EventActivity.class);
            intent.putExtra("CIRCUIT_ID", race.getTrack().getTrackId());
            context.startActivity(intent);
        });
    }

    private void setDriverNames(List<RaceResult> raceResults) {
        try {
            for (int i = 0; i < Math.min(3, raceResults.size()); i++) {
                UIUtils.singleSetTextViewText(
                        raceResults.get(i).getDriver().getFullName(),
                        view.findViewById(Constants.LAST_RACE_DRIVER_NAME.get(i))
                );
            }
            cardLoadedCallback.onCardLoaded("lastRace");
        } catch (Exception e) {
            Log.e(TAG, "Error setting driver names: " + e.getMessage());
            cardLoadedCallback.onCardLoaded("lastRace");
        }
    }

    private void loadPendingResultsLayout() {
        view.findViewById(R.id.pending_last_race_results).setVisibility(View.VISIBLE);
        view.findViewById(R.id.last_race_results).setVisibility(View.GONE);
        cardLoadedCallback.onCardLoaded("lastRace");
    }

    private void showLastRaceNotFound() {
        view.findViewById(R.id.last_race_results).setVisibility(View.GONE);
        view.findViewById(R.id.season_results).setVisibility(View.GONE);
        view.findViewById(R.id.pending_last_race_results).setVisibility(View.GONE);
        view.findViewById(R.id.last_race_not_found_layout).setVisibility(View.VISIBLE);
    }

    @FunctionalInterface
    public interface CardLoadedCallback {
        void onCardLoaded(String cardName);
    }
}

