package com.the_coffe_coders.fastestlap.ui.home.handler;

import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Practice;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Session;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.domain.f1.standing.ConstructorStandings;
import com.the_coffe_coders.fastestlap.domain.f1.standing.ConstructorStandingsElement;
import com.the_coffe_coders.fastestlap.domain.f1.standing.DriverStandings;
import com.the_coffe_coders.fastestlap.domain.f1.standing.DriverStandingsElement;
import com.the_coffe_coders.fastestlap.domain.f1.track.Track;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.DriverViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.NationViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModel;
import com.the_coffe_coders.fastestlap.ui.event.EventActivity;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.WeeklyRaceViewModel;
import com.the_coffe_coders.fastestlap.ui.home.viewmodel.HomeViewModel;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.NetworkUtils;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import java.util.List;
import java.util.Objects;

public class NextRaceHandler {
    private static final String TAG = NextRaceHandler.class.getSimpleName();

    private final Fragment fragment;
    private final Context context;
    private LifecycleOwner lifecycleOwner;
    private View view;
    private final WeeklyRaceViewModel weeklyRaceViewModel;
    private final TrackViewModel trackViewModel;
    private final NationViewModel nationViewModel;
    private final HomeViewModel homeViewModel;
    private final DriverViewModel driverViewModel;
    private final NetworkUtils networkLiveData;
    private final CardLoadedCallback cardLoadedCallback;

    private String nextRaceRound;

    @FunctionalInterface
    public interface CardLoadedCallback {
        void onCardLoaded(String cardName);
    }

    @FunctionalInterface
    public interface NextRaceRoundCallback {
        void onNextRaceRoundRetrieved(String round);
    }

    public NextRaceHandler(Fragment fragment, View view,
                          WeeklyRaceViewModel weeklyRaceViewModel,
                          TrackViewModel trackViewModel,
                          NationViewModel nationViewModel,
                          HomeViewModel homeViewModel,
                          DriverViewModel driverViewModel,
                          NetworkUtils networkLiveData,
                          CardLoadedCallback cardLoadedCallback) {
        this.fragment = fragment;
        this.context = fragment.requireContext();
        this.lifecycleOwner = fragment.getViewLifecycleOwner();
        this.view = view;
        this.weeklyRaceViewModel = weeklyRaceViewModel;
        this.trackViewModel = trackViewModel;
        this.nationViewModel = nationViewModel;
        this.homeViewModel = homeViewModel;
        this.driverViewModel = driverViewModel;
        this.networkLiveData = networkLiveData;
        this.cardLoadedCallback = cardLoadedCallback;
    }

    public void updateView(View view, LifecycleOwner lifecycleOwner) {
        this.view = view;
        this.lifecycleOwner = lifecycleOwner;
    }

    public void setupNextSessionCard(NextRaceRoundCallback roundCallback) {
        LiveData<Result> nextRaceLiveData = weeklyRaceViewModel.getNextRaceLiveData();
        try {
            // One-shot observer: removes itself after the first non-Loading result.
            // Without this, repeated setupNextSessionCard calls (e.g. swipe-refresh, network
            // restore) pile up observers and fire the card logic multiple times per emission.
            final boolean[] observerFired = {false};

            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (!observerFired[0]) {
                    Log.w(TAG, "Next race timeout — no result within 5 s");
                    setUpdating();
                    cardLoadedCallback.onCardLoaded("nextSession");
                }
            }, 5000);

            androidx.lifecycle.Observer<Result>[] observerHolder = new androidx.lifecycle.Observer[1];
            observerHolder[0] = result -> {
                try {
                    if (result instanceof Result.Loading) {
                        return;
                    }
                    // One-shot: remove before processing to avoid double-fire
                    nextRaceLiveData.removeObserver(observerHolder[0]);
                    observerFired[0] = true;

                    if (result.isSuccess()) {
                        WeeklyRace nextRace = ((Result.NextRaceSuccess) result).getData();
                        Log.i(TAG, "Next race: " + nextRace.getRound());
                        nextRaceRound = nextRace.getRound();
                        if (roundCallback != null) {
                            roundCallback.onNextRaceRoundRetrieved(nextRaceRound);
                        }
                        processNextRace(nextRace);
                    } else {
                        throw new Exception("Failed to fetch next race: " + result.getError());
                    }
                } catch (Exception e) {
                    nextRaceLiveData.removeObserver(observerHolder[0]);
                    observerFired[0] = true;
                    if (networkLiveData.isConnected()) {
                        Log.e(TAG, "Error in setNextSessionCard: " + e.getMessage());
                        setSeasonEnded();
                    } else {
                        Log.e(TAG, "Error in setNextSessionCard: No internet connection");
                        setUpdating();
                    }
                }
            };
            nextRaceLiveData.observe(lifecycleOwner, observerHolder[0]);
        } catch (Exception e) {
            if (networkLiveData.isConnected()) {
                Log.e(TAG, "Error in setNextSessionCard: " + e.getMessage());
                setSeasonEnded();
            } else {
                Log.e(TAG, "Error in setNextSessionCard: No internet connection");
                setUpdating();
            }
        }

        ImageView iconImageView = view.findViewById(R.id.live_icon);
        Animation pulseAnimation = AnimationUtils.loadAnimation(context, R.anim.pulse_static);
        iconImageView.startAnimation(pulseAnimation);
    }

    private void processNextRace(WeeklyRace nextRace) {
        try {
            if (nextRace == null) throw new Exception("Next race is null");
            if (!nextRace.getSeason().equals(ServiceLocator.currentYear))
                throw new Exception("Season mismatch");
            MutableLiveData<Result> trackData = trackViewModel.getTrack(nextRace.getTrack().getTrackId());
            trackData.observe(lifecycleOwner, trackResult -> {
                try {
                    if (trackResult instanceof Result.Loading) {
                        return;
                    }
                    if (trackResult.isSuccess()) {
                        Track track = ((Result.TrackSuccess) trackResult).getData();
                        nextRace.setTrack(track);
                        fetchNationForNextRace(nextRace, track);
                    } else {
                        throw new Exception("Failed to fetch track data: " + trackResult.getError());
                    }
                } catch (Exception e) {
                    if (networkLiveData.isConnected()) {
                        Log.e(TAG, "Error in processNextRace: " + e.getMessage());
                        setSeasonEnded();
                    } else {
                        Log.e(TAG, "Error in processNextRace: No internet connection");
                        setUpdating();
                    }
                }
            });
        } catch (Exception e) {
            if (networkLiveData.isConnected()) {
                Log.e(TAG, "Error in processNextRace: " + e.getMessage());
                setSeasonEnded();
            } else {
                if (Objects.equals(e.getMessage(), "Season mismatch")) setSeasonEnded();
                Log.e(TAG, "Error in processNextRace: No internet connection");
                setUpdating();
            }
        }
    }

    private void fetchNationForNextRace(WeeklyRace nextRace, Track track) {
        try {
            MutableLiveData<Result> nationData = nationViewModel.getNation(track.getCountry());
            nationData.observe(lifecycleOwner, nationResult -> {
                try {
                    if (nationResult instanceof Result.Loading) {
                        return;
                    }
                    if (nationResult.isSuccess()) {
                        Nation nation = ((Result.NationSuccess) nationResult).getData();
                        setNextRaceCard(nextRace, nation);
                    } else {
                        throw new Exception("Failed to fetch nation data: " + nationResult.getError());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching nation: " + e.getMessage());
                    setSeasonEnded();
                }
            });
        } catch (RuntimeException e) {
            Log.e(TAG, "Error fetching nation: " + e.getMessage());
            setNextRaceCard(nextRace, null);
        }
    }

    private void setNextRaceCard(WeeklyRace nextRace, Nation nation) {
        try {
            UIUtils.singleSetTextViewText(nextRace.getRaceName(), view.findViewById(R.id.home_next_gp_name));

            String nationFlagUrl = null;
            if (nation != null) {
                nationFlagUrl = nation.getNation_flag_url();
            }

            UIUtils.loadImageWithGlide(context, nationFlagUrl, view.findViewById(R.id.home_next_gp_flag), () -> {
                try {
                    setNextRaceCardFinalStep(nextRace);
                } catch (Exception e) {
                    setSeasonEnded();
                }
            });

        } catch (Exception e) {
            Log.i(TAG, "connected: " + networkLiveData.isConnected());
            if (networkLiveData.isConnected()) {
                setSeasonEnded();
                Log.e(TAG, "Error in setNextRaceCard: " + e.getMessage());
            } else {
                Log.e(TAG, "Error in setNextRaceCard: No internet connection");
                setUpdating();
            }
        }
    }

    private void setNextRaceCardFinalStep(WeeklyRace nextRace) throws Exception {
        if (!nextRace.getSeason().equals(ServiceLocator.currentYear)) {
            throw new Exception("Season mismatch");
        }

        List<Session> sessions = nextRace.getSessions();
        Session nextEvent = nextRace.findNextEvent(sessions);
        if (nextEvent != null) {
            startCountdown(nextEvent.getStartDateTime());
            updateSessionType(nextEvent);
        } else {
            setUpdating();
        }

        FrameLayout nextSessionCard = view.findViewById(R.id.timer_card_countdown);
        nextSessionCard.setOnClickListener(v -> {
            Intent intent = new Intent(context, EventActivity.class);
            intent.putExtra("CIRCUIT_ID", nextRace.getTrack().getTrackId());
            context.startActivity(intent);
        });
    }

    private void updateSessionType(Session nextEvent) {
        String sessionId = nextEvent.getClass().getSimpleName().equals("Practice")
            ? "Practice" + ((Practice) nextEvent).getNumber()
            : nextEvent.getClass().getSimpleName();
        TextView sessionTypeView = view.findViewById(R.id.next_session_type);

        UIUtils.translateSessionType(context, sessionTypeView, sessionId);
    }

    private void startCountdown(LocalDateTime eventDate) {
        LinearLayout liveIconLayout = view.findViewById(R.id.timer_live_layout);
        liveIconLayout.setVisibility(View.GONE);
        long millisUntilStart = ZonedDateTime.of(eventDate, ZoneId.systemDefault()).toInstant().toEpochMilli() - System.currentTimeMillis();
        if (millisUntilStart <= 0) {
            liveIconLayout.setVisibility(View.VISIBLE);
            cardLoadedCallback.onCardLoaded("nextSession");
            return;
        }

        new CountDownTimer(millisUntilStart, 1000) {
            final TextView days = view.findViewById(R.id.next_days_counter);
            final TextView hours = view.findViewById(R.id.next_hours_counter);
            final TextView minutes = view.findViewById(R.id.next_minutes_counter);
            final TextView seconds = view.findViewById(R.id.next_seconds_counter);

            @Override
            public void onTick(long millisUntilFinished) {
                UIUtils.multipleSetTextViewText(
                    new String[]{
                        String.valueOf(millisUntilFinished / 86400000),
                        String.valueOf((millisUntilFinished % 86400000) / 3600000),
                        String.valueOf(((millisUntilFinished % 86400000) % 3600000) / 60000),
                        String.valueOf((((millisUntilFinished % 86400000) % 3600000) % 60000) / 1000)
                    },
                    new TextView[]{days, hours, minutes, seconds}
                );
            }

            @Override
            public void onFinish() {
                UIUtils.multipleSetTextViewText(
                    new String[]{"0", "0", "0", "0"},
                    new TextView[]{days, hours, minutes, seconds}
                );
                liveIconLayout.setVisibility(View.VISIBLE);
            }
        }.start();
        cardLoadedCallback.onCardLoaded("nextSession");
    }

    private void setSeasonEnded() {
        view.findViewById(R.id.last_race_results).setVisibility(View.GONE);
        view.findViewById(R.id.timer).setVisibility(View.GONE);
        view.findViewById(R.id.season_ended).setVisibility(View.VISIBLE);
        view.findViewById(R.id.season_results).setVisibility(View.VISIBLE);
        view.findViewById(R.id.pending_last_race_results).setVisibility(View.GONE);

        buildFinalDriversStanding(view.findViewById(R.id.season_results));
        buildFinalTeamsStanding(view.findViewById(R.id.season_results));
        cardLoadedCallback.onCardLoaded("nextSession");
    }

    private void setUpdating() {
        view.findViewById(R.id.timer_card_countdown).setVisibility(View.GONE);
        view.findViewById(R.id.timer_updating).setVisibility(View.VISIBLE);
        cardLoadedCallback.onCardLoaded("nextSession");
    }

    private void buildFinalDriversStanding(View seasonEndedCard) {
        MutableLiveData<Result> driverStandingsLiveData = homeViewModel.getDriverStandingsLiveData(fragment.requireActivity().getApplication());

        driverStandingsLiveData.observe(lifecycleOwner, result -> {
            try {
                if (result instanceof Result.Loading) {
                    return;
                }
                if (result.isSuccess()) {
                    DriverStandings driverStandings = ((Result.DriverStandingsSuccess) result).getData();
                    List<DriverStandingsElement> driversList = driverStandings.getDriverStandingsElements();
                    for (int i = 0; i < Math.min(3, driversList.size()); i++) {
                        setStandingFields(seasonEndedCard, driversList.get(i).getDriver().getDriverId(), i);
                    }
                } else {
                    throw new Exception("Failed to fetch driver standings: " + result.getError());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in buildFinalDriversStanding: " + e.getMessage());
            }
        });
    }

    private void setStandingFields(View seasonEndedCard, String driverId, int position) {
        MutableLiveData<Result> driverData = driverViewModel.getDriver(driverId);
        driverData.observe(lifecycleOwner, result -> {
            try {
                if (result instanceof Result.Loading) {
                    return;
                }
                if (result.isSuccess()) {
                    Driver driver = ((Result.DriverSuccess) result).getData();

                    UIUtils.singleSetTextViewText(driver.getFullName(),
                        seasonEndedCard.findViewById(Constants.HOME_SEASON_DRIVER_STANDINGS_NAME_FIELD.get(position)));

                    View driverColor = seasonEndedCard.findViewById(Constants.HOME_SEASON_DRIVER_STANDINGS_COLOR_FIELD.get(position));
                    driverColor.setBackgroundResource(Constants.TEAM_COLOR.getOrDefault(driver.getTeam_id(), R.color.timer_gray));
                } else {
                    throw new Exception("Failed to fetch driver data: " + result.getError());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in setStandingFields: " + e.getMessage());
            }
        });
    }

    private void buildFinalTeamsStanding(View seasonEndedCard) {
        MutableLiveData<Result> constructorStandingsData = homeViewModel.getConstructorStandingsLiveData(fragment.requireActivity().getApplication());
        constructorStandingsData.observe(lifecycleOwner, result -> {
            try {
                if (result instanceof Result.Loading) {
                    return;
                }
                if (result.isSuccess()) {
                    ConstructorStandings standings = ((Result.ConstructorStandingsSuccess) result).getData();
                    List<ConstructorStandingsElement> constructorsList = standings.getConstructorStandings();
                    for (int i = 0; i < Math.min(3, constructorsList.size()); i++) {
                        ConstructorStandingsElement constructor = constructorsList.get(i);

                        UIUtils.singleSetTextViewText(constructor.getConstructor().getName(),
                            seasonEndedCard.findViewById(Constants.HOME_SEASON_TEAM_STANDINGS_NAME_FIELD.get(i)));

                        View constructorColor = seasonEndedCard.findViewById(Constants.HOME_SEASON_TEAM_STANDINGS_COLOR_FIELD.get(i));
                        constructorColor.setBackgroundResource(Constants.TEAM_COLOR.getOrDefault(
                            constructor.getConstructor().getConstructorId(), R.color.timer_gray));
                    }
                } else {
                    throw new Exception("Failed to fetch constructor standings: " + result.getError());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in buildFinalTeamsStanding: " + e.getMessage());
            }
        });
    }
}

