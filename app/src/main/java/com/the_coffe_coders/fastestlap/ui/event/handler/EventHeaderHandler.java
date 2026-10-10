package com.the_coffe_coders.fastestlap.ui.event.handler;

import android.graphics.Outline;
import android.util.Log;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.domain.f1.track.Track;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.NationViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModel;
import com.the_coffe_coders.fastestlap.util.calendar.CalendarUtils;
import com.the_coffe_coders.fastestlap.util.service.NetworkUtils;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ui.NavigationUtils;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

public class EventHeaderHandler {
    private static final String TAG = "EventHeaderHandler";

    private final AppCompatActivity activity;
    private final View rootView;
    private final LoadingScreen loadingScreen;
    private final TrackViewModel trackViewModel;
    private final NationViewModel nationViewModel;

    public interface OnHeaderDataLoadedListener {
        void onHeaderDataLoaded(Track track, Nation nation);
    }

    public EventHeaderHandler(AppCompatActivity activity,
                              View rootView,
                              LoadingScreen loadingScreen,
                              TrackViewModel trackViewModel,
                              NationViewModel nationViewModel) {
        this.activity = activity;
        this.rootView = rootView;
        this.loadingScreen = loadingScreen;
        this.trackViewModel = trackViewModel;
        this.nationViewModel = nationViewModel;
    }

    public void setupOuterCard() {
        View eventCardOuter = rootView.findViewById(R.id.event_card_outer);
        if (eventCardOuter != null) {
            eventCardOuter.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    int width = view.getWidth();
                    int height = view.getHeight();
                    if (width <= 0 || height <= 0) {
                        return;
                    }
                    int radius = (int) (20 * activity.getResources().getDisplayMetrics().density);
                    outline.setRoundRect(0, -radius, width, height, radius);
                }
            });
            eventCardOuter.setClipToOutline(true);
        }
    }

    public void loadHeaderData(WeeklyRace weeklyRace, String trackId, OnHeaderDataLoadedListener listener) {
        UIUtils.singleSetTextViewText(weeklyRace.getRaceName().toUpperCase(), rootView.findViewById(R.id.topAppBarTitle));

        MutableLiveData<Result> trackData = trackViewModel.getTrack(trackId);
        @SuppressWarnings("unchecked")
        Observer<Result>[] observerTrack = new Observer[1];
        observerTrack[0] = result -> {
            if (result instanceof Result.Loading) {
                return;
            }
            trackData.removeObserver(observerTrack[0]);
            if (result.isSuccess()) {
                Track track = ((Result.TrackSuccess) result).getData();
                Log.i(TAG, "Track: " + track);

                if (track != null && track.getCountry() != null) {
                    try {
                        MutableLiveData<Result> nationData = nationViewModel.getNation(track.getCountry());
                        @SuppressWarnings("unchecked")
                        Observer<Result>[] observerNation = new Observer[1];
                        observerNation[0] = result1 -> {
                            if (result1 instanceof Result.Loading) {
                                return;
                            }
                            nationData.removeObserver(observerNation[0]);
                            if (result1.isSuccess()) {
                                Nation nation = ((Result.NationSuccess) result1).getData();
                                setEventImage(weeklyRace, track, nation, trackId, listener);
                            } else {
                                setEventImage(weeklyRace, track, null, trackId, listener);
                            }
                        };
                        nationData.observe(activity, observerNation[0]);
                    } catch (RuntimeException e) {
                        Log.e(TAG, "Error getting nation data: " + e.getMessage());
                        setEventImage(weeklyRace, track, null, trackId, listener);
                    }
                } else {
                    setEventImage(weeklyRace, track, null, trackId, listener);
                }
            } else {
                Log.e(TAG, "Error getting track data: " + result.getError() + ", falling back to weeklyRace.getTrack()");
                Track fallbackTrack = weeklyRace.getTrack();
                if (fallbackTrack != null) {
                    Track track = fallbackTrack;
                    if (track.getCountry() != null) {
                        fetchNationAndSetImage(weeklyRace, track, trackId, listener);
                    } else {
                        setEventImage(weeklyRace, track, null, trackId, listener);
                    }
                } else {
                    loadingScreen.hideLoadingScreen();
                }
            }
        };
        trackData.observe(activity, observerTrack[0]);
    }

    private void fetchNationAndSetImage(WeeklyRace weeklyRace, Track targetTrack, String trackId, OnHeaderDataLoadedListener listener) {
        try {
            MutableLiveData<Result> nationData = nationViewModel.getNation(targetTrack.getCountry());
            @SuppressWarnings("unchecked")
            Observer<Result>[] observerNation = new Observer[1];
            observerNation[0] = result1 -> {
                if (result1 instanceof Result.Loading) {
                    return;
                }
                nationData.removeObserver(observerNation[0]);
                if (result1.isSuccess()) {
                    Nation nation = ((Result.NationSuccess) result1).getData();
                    setEventImage(weeklyRace, targetTrack, nation, trackId, listener);
                } else {
                    setEventImage(weeklyRace, targetTrack, null, trackId, listener);
                }
            };
            nationData.observe(activity, observerNation[0]);
        } catch (RuntimeException e) {
            Log.e(TAG, "Error getting nation data: " + e.getMessage());
            setEventImage(weeklyRace, targetTrack, null, trackId, listener);
        }
    }

    private void setEventImage(WeeklyRace weeklyRace, Track track, Nation nation, String trackId, OnHeaderDataLoadedListener listener) {
        loadingScreen.updateProgress();

        String imageUrl = track != null ? track.getTrack_pic_url() : null;
        ImageView bgImageView = rootView.findViewById(R.id.event_background_image);

        if (bgImageView != null && imageUrl != null && !imageUrl.isEmpty()) {
            UIUtils.loadImageAsync(activity, imageUrl, bgImageView);
        }
        buildEventCardDetails(weeklyRace, track, nation, trackId);

        if (listener != null) {
            listener.onHeaderDataLoaded(track, nation);
        }
    }

    private void buildEventCardDetails(WeeklyRace weeklyRace, Track track, Nation nation, String trackId) {
        String gpName = (track != null && track.getGp_long_name() != null)
                ? track.getGp_long_name()
                : (weeklyRace != null ? weeklyRace.getRaceName() : "");

        UIUtils.multipleSetTextViewText(
                new String[]{
                        "Round " + weeklyRace.getRound(),
                        weeklyRace.getSeason(),
                        gpName},
                new TextView[]{
                        rootView.findViewById(R.id.round_number),
                        rootView.findViewById(R.id.event_year),
                        rootView.findViewById(R.id.gp_name)});

        UIUtils.translateEventDateInterval(weeklyRace.getDateInterval(), rootView.findViewById(R.id.event_date));

        LinearLayout trackLayout = rootView.findViewById(R.id.track_outline_layout);
        if (trackLayout != null) {
            trackLayout.setOnClickListener(v ->
                    NavigationUtils.navigateToBioPage(activity, trackId + "&" + weeklyRace.getRaceName().toUpperCase(), 2));
        }

        View scheduleWeatherBadge = rootView.findViewById(R.id.schedule_weather_badge);
        if (scheduleWeatherBadge != null) {
            scheduleWeatherBadge.setOnClickListener(v -> {
                if (!NetworkUtils.isNetworkAvailable(activity)) {
                    Toast.makeText(activity, "No internet connection", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (track == null) {
                    Toast.makeText(activity, "Cannot access weather", Toast.LENGTH_SHORT).show();
                }
                String locality = (track != null && track.getLocation() != null) ? track.getLocation().getLocality() : null;
                String lat = (track != null && track.getLocation() != null) ? track.getLocation().getLatitude() : null;
                String lon = (track != null && track.getLocation() != null) ? track.getLocation().getLongitude() : null;
                String zoneId = (track != null && track.getLocation() != null) ? track.getLocation().getZoneId() : null;

                String startDateStr = null;
                String endDateStr = null;
                if (weeklyRace.getFirstPractice() != null && weeklyRace.getFirstPractice().getStartDateTime() != null) {
                    startDateStr = weeklyRace.getFirstPractice().getStartDateTime().toLocalDate().toString();
                }
                if (weeklyRace.getFinalRace() != null && weeklyRace.getFinalRace().getStartDateTime() != null) {
                    endDateStr = weeklyRace.getFinalRace().getStartDateTime().toLocalDate().toString();
                }

                boolean isUnderway = weeklyRace.isUnderway(false);
                NavigationUtils.navigateToWeatherPage(activity, locality, lat, lon, "latest", startDateStr, endDateStr, zoneId, isUnderway);
            });
        }

        String nationFlagUrl = (nation != null) ? nation.getNation_flag_url() : null;
        ImageView flagView = rootView.findViewById(R.id.country_flag);
        ImageView trackOutlineView = rootView.findViewById(R.id.track_outline_image);

        if (flagView != null && nationFlagUrl != null) {
            UIUtils.loadImageAsync(activity, nationFlagUrl, flagView);
        }
        String outlineUrl = track != null ? track.getTrack_minimal_layout_url() : null;
        if (trackOutlineView != null && outlineUrl != null && !outlineUrl.isEmpty()) {
            UIUtils.loadImageAsync(activity, outlineUrl, trackOutlineView);
        }

        View scheduleCalendarBadge = rootView.findViewById(R.id.schedule_calendar_badge);
        if (scheduleCalendarBadge != null) {
            if (weeklyRace.isWeekFinished()) {
                scheduleCalendarBadge.setVisibility(View.GONE);
            } else {
                scheduleCalendarBadge.setVisibility(View.VISIBLE);
                scheduleCalendarBadge.setOnClickListener(v -> {
                    try {
                        CalendarUtils.addWeekendToCalendar(activity, weeklyRace);
                        Toast.makeText(activity, R.string.add_to_calendar_success, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error opening calendar: " + e.getMessage());
                        Toast.makeText(activity, R.string.calendar_not_found, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }
}
