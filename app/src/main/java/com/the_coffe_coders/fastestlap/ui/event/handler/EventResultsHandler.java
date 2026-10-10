package com.the_coffe_coders.fastestlap.ui.event.handler;

import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Session;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.domain.f1.result.QualifyingResult;
import com.the_coffe_coders.fastestlap.domain.f1.result.RaceResult;
import com.the_coffe_coders.fastestlap.domain.f1.result.RaceResultFastestLap;
import com.the_coffe_coders.fastestlap.domain.f1.result.Stint;
import com.the_coffe_coders.fastestlap.domain.f1.track.Track;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.EventViewModel;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.RaceResultViewModel;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ui.NavigationUtils;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.List;
import java.util.Objects;

public class EventResultsHandler {
    private static final String TAG = "EventResultsHandler";

    private final AppCompatActivity activity;
    private final View rootView;
    private final LoadingScreen loadingScreen;
    private final RaceResultViewModel raceResultViewModel;
    private final EventViewModel eventViewModel;

    private Track track;
    private Race currentRace;

    public EventResultsHandler(AppCompatActivity activity,
                               View rootView,
                               LoadingScreen loadingScreen,
                               RaceResultViewModel raceResultViewModel,
                               EventViewModel eventViewModel) {
        this.activity = activity;
        this.rootView = rootView;
        this.loadingScreen = loadingScreen;
        this.raceResultViewModel = raceResultViewModel;
        this.eventViewModel = eventViewModel;
    }

    public void setTrack(Track track) {
        this.track = track;
    }

    public void showResults(WeeklyRace weeklyRace, Track track) {
        this.track = track;
        loadingScreen.updateProgress();

        View countdownView = rootView.findViewById(R.id.timer_card_countdown);
        View resultsView = rootView.findViewById(R.id.timer_card_results);
        View pendingResultsView = rootView.findViewById(R.id.timer_card_pending_results);

        if (countdownView != null) countdownView.setVisibility(View.GONE);
        if (resultsView != null) resultsView.setVisibility(View.VISIBLE);
        if (pendingResultsView != null) pendingResultsView.setVisibility(View.GONE);

        processRaceResults(weeklyRace);
    }

    public void processRaceResults(WeeklyRace weeklyRace) {
        Log.i(TAG, "Processing race results for round: " + weeklyRace.getRound());

        MutableLiveData<Result> resultMutableLiveData = raceResultViewModel.getRaceResults(weeklyRace.getRound());
        @SuppressWarnings("unchecked")
        Observer<Result>[] observerHolder = new Observer[1];
        observerHolder[0] = result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            resultMutableLiveData.removeObserver(observerHolder[0]);

            try {
                if (result instanceof Result.RaceResultsSuccess) {
                    Race race = ((Result.RaceResultsSuccess) result).getData();
                    List<RaceResult> podium = race != null ? race.getRaceResults() : null;

                    if (podium == null || podium.isEmpty()) {
                        showPendingResults();
                    } else {
                        this.currentRace = race;

                        Log.i(TAG, "Podium found, size: " + podium.size());
                        for (int i = 0; i < 3 && i < podium.size(); i++) {
                            String teamId = podium.get(i).getConstructor().getConstructorId();

                            UIUtils.singleSetTextViewText(podium.get(i).getDriver().getFullName(),
                                    rootView.findViewById(Constants.PODIUM_DRIVER_NAME.get(i)));

                            LinearLayout teamColor = rootView.findViewById(Constants.PODIUM_TEAM_COLOR.get(i));
                            Integer teamColorObj = Constants.TEAM_COLOR.get(teamId);
                            if (teamColor != null) {
                                teamColor.setBackgroundColor(ContextCompat.getColor(activity, Objects.requireNonNullElseGet(teamColorObj, () -> R.color.mercedes_f1)));
                            }
                        }

                        View resultsView = rootView.findViewById(R.id.timer_card_results);
                        if (resultsView != null) {
                            resultsView.setOnClickListener(v -> showRaceResultsDialog(currentRace, "Race"));
                        }
                    }
                } else {
                    showPendingResults();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing race results: " + e.getMessage());
                showPendingResults();
            } finally {
                Log.i("ActivityDataLog", "DATA_AND_IMAGES_FULLY_LOADED: EventActivity at " + System.currentTimeMillis());
                loadingScreen.hideLoadingScreen();
            }
        };
        resultMutableLiveData.observe(activity, observerHolder[0]);
    }

    public void showPendingResults() {
        Log.i(TAG, "No results found");
        View pendingResultsView = rootView.findViewById(R.id.timer_card_pending_results);
        View countdownView = rootView.findViewById(R.id.timer_card_countdown);
        View resultsView = rootView.findViewById(R.id.timer_card_results);

        if (pendingResultsView != null) pendingResultsView.setVisibility(View.VISIBLE);
        if (countdownView != null) countdownView.setVisibility(View.GONE);
        if (resultsView != null) resultsView.setVisibility(View.GONE);
    }

    public void manageSessionScheduleClick(Session session, String round) {
        Log.i(TAG, "session id clicked: " + session.getClass().getSimpleName());
        if (!session.isPractice()) {
            if (session.isRace()) {
                if (currentRace != null && currentRace.getRaceResults() != null && !currentRace.getRaceResults().isEmpty()) {
                    showRaceResultsDialog(currentRace, "Race");
                } else {
                    fetchRaceResultsAndShow(round);
                }
            } else if (session.isQualifying()) {
                processQualifyingData(round);
            } else if (session.isSprint()) {
                processSprintData(round);
            } else {
                Toast.makeText(activity, R.string.results_not_available, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void showRaceResultsDialog(Race race) {
        showRaceResultsDialog(race, null);
    }

    public void showRaceResultsDialog(Race race, String preferredSession) {
        if (race != null) {
            // Assicura che l'oggetto Race abbia i dati completi del Track da Firebase (incluso gp_long_name e layout)
            if (this.track != null) {
                if (race.getTrack() == null) {
                    race.setTrack(this.track);
                } else {
                    if (race.getTrack().getGp_long_name() == null && this.track.getGp_long_name() != null) {
                        race.getTrack().setGp_long_name(this.track.getGp_long_name());
                    }
                    if (race.getTrack().getLocation() == null && this.track.getLocation() != null) {
                        race.getTrack().setLocation(this.track.getLocation());
                    }
                    if (race.getTrack().getTrack_minimal_layout_url() == null && this.track.getTrack_minimal_layout_url() != null) {
                        race.getTrack().setTrack_minimal_layout_url(this.track.getTrack_minimal_layout_url());
                    }
                }
            }

            String sessionName = null;
            RaceResultFastestLap raceFastestLap = null;

            if ("Sprint".equalsIgnoreCase(preferredSession) && race.getSprintResults() != null && !race.getSprintResults().isEmpty()) {
                Log.i(TAG, "Showing sprint results");
                sessionName = "Sprint";
                raceFastestLap = eventViewModel.extractFastestLap(race.getSprintResults());
            } else if ("Race".equalsIgnoreCase(preferredSession) && race.getRaceResults() != null && !race.getRaceResults().isEmpty()) {
                Log.i(TAG, "Showing race results");
                sessionName = "Race";
                raceFastestLap = eventViewModel.extractFastestLap(race.getRaceResults());
            } else {
                Log.e(TAG, "No results found");
                Toast.makeText(activity, R.string.results_not_available, Toast.LENGTH_SHORT).show();
                return;
            }

            List<Stint> cachedStints = sessionName.equals("Sprint") ? race.getSprintStints() : race.getRaceStints();
            if (cachedStints == null || cachedStints.isEmpty()) {
                cachedStints = race.getStints();
            }

            NavigationUtils.showRaceResults(activity, race, 0, cachedStints, raceFastestLap);
        } else {
            Log.e(TAG, "race is null, cannot show results");
        }
    }

    public void showQualifyingResultsDialog(Race race) {
        if (race != null) {
            if (race.getQualifyingResults() != null && !race.getQualifyingResults().isEmpty()) {
                Log.i(TAG, "Showing qualifying results");
                NavigationUtils.showRaceResults(activity, race, 1, null, null);
            } else {
                Log.e(TAG, "qualifying results not found");
            }
        } else {
            Log.e(TAG, "race is null, cannot show qualifying results");
        }
    }

    public void fetchRaceResultsAndShow(String round) {
        if (loadingScreen != null) {
            loadingScreen.showLoadingScreen(true);
        }
        MutableLiveData<Result> resultMutableLiveData = raceResultViewModel.getRaceResults(round);
        Observer<Result> observer = new Observer<>() {
            @Override
            public void onChanged(Result result) {
                if (result instanceof Result.Loading) {
                    return;
                }
                resultMutableLiveData.removeObserver(this);
                if (loadingScreen != null) {
                    loadingScreen.hideLoadingScreenImmediately();
                }
                if (result instanceof Result.RaceResultsSuccess) {
                    Race race = ((Result.RaceResultsSuccess) result).getData();
                    if (race != null && race.getRaceResults() != null && !race.getRaceResults().isEmpty()) {
                        currentRace = race;
                        showRaceResultsDialog(race, "Race");
                    } else {
                        Toast.makeText(activity, R.string.results_not_available, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(activity, R.string.results_not_available, Toast.LENGTH_SHORT).show();
                }
            }
        };
        resultMutableLiveData.observe(activity, observer);
    }

    public void processQualifyingData(String round) {
        Log.d(TAG, "Processing qualifying data for round: " + round);
        if (loadingScreen != null) {
            loadingScreen.showLoadingScreen(true);
        }

        MutableLiveData<Result> qualifyingLiveData = raceResultViewModel.getQualifyingResults(round);
        Observer<Result> observer = new Observer<>() {
            @Override
            public void onChanged(Result result) {
                if (result instanceof Result.Loading) {
                    return;
                }

                qualifyingLiveData.removeObserver(this);

                if (loadingScreen != null) {
                    loadingScreen.hideLoadingScreenImmediately();
                }

                try {
                    if (result instanceof Result.RaceResultsSuccess) {
                        Race race = ((Result.RaceResultsSuccess) result).getData();
                        List<QualifyingResult> qualifyingResults = race != null ? race.getQualifyingResults() : null;

                        if (qualifyingResults == null || qualifyingResults.isEmpty()) {
                            Log.i(TAG, "No qualifying results found");
                            Toast.makeText(activity, R.string.results_not_available, Toast.LENGTH_SHORT).show();
                        } else {
                            Log.i(TAG, "Qualifying results found: " + qualifyingResults.size());
                            showQualifyingResultsDialog(race);
                        }
                    } else if (result instanceof Result.Error) {
                        Toast.makeText(activity, R.string.results_not_available, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing qualifying data: " + e.getMessage());
                }
            }
        };
        qualifyingLiveData.observe(activity, observer);
    }

    public void processSprintData(String round) {
        Log.d(TAG, "Processing sprint data for round: " + round);
        if (loadingScreen != null) {
            loadingScreen.showLoadingScreen(true);
        }

        MutableLiveData<Result> sprintLiveData = raceResultViewModel.getSprintResults(round);
        Observer<Result> observer = new Observer<>() {
            @Override
            public void onChanged(Result result) {
                if (result instanceof Result.Loading) {
                    return;
                }

                sprintLiveData.removeObserver(this);

                if (loadingScreen != null) {
                    loadingScreen.hideLoadingScreenImmediately();
                }

                try {
                    if (result instanceof Result.RaceResultsSuccess) {
                        Race race = ((Result.RaceResultsSuccess) result).getData();
                        List<RaceResult> sprintResults = race != null ? race.getSprintResults() : null;

                        if (sprintResults == null || sprintResults.isEmpty()) {
                            Log.i(TAG, "No sprint results found");
                            Toast.makeText(activity, R.string.results_not_available, Toast.LENGTH_SHORT).show();
                        } else {
                            Log.i(TAG, "Sprint results found: " + sprintResults.size());
                            showRaceResultsDialog(race, "Sprint");
                        }
                    } else if (result instanceof Result.Error) {
                        Toast.makeText(activity, R.string.results_not_available, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing sprint data: " + e.getMessage());
                }
            }
        };
        sprintLiveData.observe(activity, observer);
    }
}
