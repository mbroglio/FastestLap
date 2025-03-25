package com.the_coffe_coders.fastestlap.ui.event;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.appbar.MaterialToolbar;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Practice;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResult;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Session;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Track;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.ui.bio.TrackBioActivity;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.NationViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.NationViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModel;
import com.the_coffe_coders.fastestlap.ui.bio.viewmodel.TrackViewModelFactory;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.EventViewModel;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.EventViewModelFactory;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;
import com.the_coffe_coders.fastestlap.util.UIUtils;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EventActivity extends AppCompatActivity {
    private static final String TAG = "EventActivity";
    private final boolean eventToProcess = true;
    LoadingScreen loadingScreen;
    EventViewModel eventViewModel;
    private String trackId;
    private Track track;
    private Nation nation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_event);


        eventViewModel = new ViewModelProvider(this, new EventViewModelFactory(ServiceLocator.getInstance().getRaceRepository(getApplication(), false), ServiceLocator.getInstance().getRaceResultRepository(getApplication(), false))).get(EventViewModel.class);

        // Show loading screen initially
        loadingScreen = new LoadingScreen(getWindow().getDecorView(), this);
        loadingScreen.showLoadingScreen();
        Log.i(TAG, "Loading screen shown");

        trackId = getIntent().getStringExtra("CIRCUIT_ID");
        Log.i(TAG, "Circuit ID: " + trackId);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);

        UIUtils.applyWindowInsets(toolbar);

        ScrollView eventScrollLayout = findViewById(R.id.event_scroll_view);
        UIUtils.applyWindowInsets(eventScrollLayout);


        processRaceData(toolbar);
    }

    private void processRaceData(MaterialToolbar toolbar) {
        List<WeeklyRace> races = new ArrayList<>();
        MutableLiveData<Result> data = ServiceLocator.getInstance().getRaceRepository(getApplication(), false).fetchWeeklyRaces(0);
        data.observe(this, result -> {
            if (result.isSuccess()) {
                Log.i("PastEvent", "SUCCESS");
                races.addAll(((Result.WeeklyRaceSuccess) result).getData());

                // Extract in another class
                WeeklyRace weeklyRace = null;
                for (WeeklyRace race : races) {
                    if (race.getTrack().getTrackId().equals(trackId)) {
                        weeklyRace = race;
                    }
                }

                buildEventCard(weeklyRace, toolbar);
            }
        });
    }

    private void buildEventCard(WeeklyRace weeklyRace, MaterialToolbar toolbar) {
        String grandPrixName = weeklyRace.getRaceName().toUpperCase();
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        TextView title = findViewById(R.id.topAppBarTitle);
        title.setText(grandPrixName);

        TrackViewModel trackViewModel = new ViewModelProvider(this, new TrackViewModelFactory(ServiceLocator.getInstance().getTrackRepository())).get(TrackViewModel.class);
        MutableLiveData<Result> trackData = trackViewModel.getTrack(trackId);

        trackData.observe(this, result -> {
            if (result.isSuccess()) {
                track = ((Result.TrackSuccess) result).getData();
                Log.i(TAG, "Track: " + track.toString());

                NationViewModel nationViewModel = new ViewModelProvider(this, new NationViewModelFactory(ServiceLocator.getInstance().getFirebaseNationRepository())).get(NationViewModel.class);
                MutableLiveData<Result> nationData = nationViewModel.getNation(track.getCountry());

                nationData.observe(this, result1 -> {
                    if (result1.isSuccess()) {
                        nation = ((Result.NationSuccess) result1).getData();
                        Log.i(TAG, "Nation: " + nation.toString());

                        setEventImage(weeklyRace, track, nation);
                    }
                });
            }
        });
    }

    private void buildEventCard(WeeklyRace weeklyRace, Track track, Nation nation) {
        ImageView countryFlag = findViewById(R.id.country_flag);
        Glide.with(this).load(nation.getNation_flag_url()).into(countryFlag);

        ImageView trackMap = findViewById(R.id.track_outline_image);
        Glide.with(this).load(track.getTrack_minimal_layout_url()).into(trackMap);

        TextView roundNumber = findViewById(R.id.round_number);
        String round = "Round " + weeklyRace.getRound();
        roundNumber.setText(round);

        TextView seasonYear = findViewById(R.id.event_year);
        String year = weeklyRace.getSeason();
        seasonYear.setText(year);

        TextView name = findViewById(R.id.gp_name);
        name.setText(track.getGp_long_name());

        //setEventImage(track.getTrack_pic_url());

        TextView eventDate = findViewById(R.id.event_date);
        eventDate.setText(weeklyRace.getDateInterval());

        LinearLayout trackLayout = findViewById(R.id.track_outline_layout);
        trackLayout.setOnClickListener(v -> {
            Intent intent = new Intent(EventActivity.this, TrackBioActivity.class);
            intent.putExtra("CIRCUIT_ID", trackId);
            intent.putExtra("GRAND_PRIX_NAME", weeklyRace.getRaceName().toUpperCase());
            startActivity(intent);
        });

        List<Session> sessions = weeklyRace.getSessions();
        Session nextEvent = weeklyRace.findNextEvent(sessions);
        boolean underway = weeklyRace.isUnderway() && !weeklyRace.isWeekFinished();
        //setLiveSession();
        if (nextEvent != null && !underway) {
            LocalDateTime eventDateTime = nextEvent.getStartDateTime();
            startCountdown(eventDateTime);
        } else if (!underway) {
            showResults(weeklyRace);
        }

        createWeekSchedule(sessions);
    }

    private void setEventImage(WeeklyRace weeklyRace, Track track, Nation nation) {

        String imageUrl = track.getTrack_pic_url();
        LinearLayout eventCard = findViewById(R.id.event_card);

        // Use Glide to load the image from URL
        Glide.with(this)
                .load(imageUrl)
                .transform(new BitmapTransformation() {
                    @Override
                    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {

                    }

                    @Override
                    protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
                        // Make the bitmap 30% transparent (76/255 ≈ 0.3)
                        return setAlpha(toTransform, 76);
                    }

                    public String getId() {
                        return "alpha";
                    }

                    // Helper method to set alpha on bitmap
                    private Bitmap setAlpha(Bitmap bitmap, int alpha) {
                        Bitmap mutableBitmap = bitmap.isMutable() ? bitmap : bitmap.copy(Bitmap.Config.ARGB_8888, true);
                        Canvas canvas = new Canvas(mutableBitmap);
                        Paint paint = new Paint();
                        paint.setAlpha(alpha);
                        canvas.drawRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), paint);
                        return mutableBitmap;
                    }
                })
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        eventCard.setBackground(resource);
                        buildEventCard(weeklyRace, track, nation);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // Use default image if loading fails
                        Drawable defaultImage = ContextCompat.getDrawable(getApplicationContext(), R.drawable.constructors_image);
                        if (defaultImage != null) {
                            defaultImage.setAlpha(76);
                        }
                        eventCard.setBackground(defaultImage);
                        buildEventCard(weeklyRace, track, nation);
                    }
                });
    }

    private void setLiveSession() {
        View liveSession = findViewById(R.id.event_live_card);
        View noLiveSession = findViewById(R.id.event_not_live_card);

        // Hide countdown view and show results view
        liveSession.setVisibility(View.VISIBLE);
        noLiveSession.setVisibility(View.GONE);

        ImageView liveIcon = findViewById(R.id.live_icon);
        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse_dynamic);
        liveIcon.startAnimation(pulse);
    }

    private void startCountdown(LocalDateTime eventDate) {
        long millisUntilStart = ZonedDateTime.of(eventDate, ZoneId.systemDefault()).toInstant().toEpochMilli() - System.currentTimeMillis();
        new CountDownTimer(millisUntilStart, 1000) {
            final TextView days_counter = findViewById(R.id.next_days_counter);
            final TextView hours_counter = findViewById(R.id.next_hours_counter);
            final TextView minutes_counter = findViewById(R.id.next_minutes_counter);
            final TextView seconds_counter = findViewById(R.id.next_seconds_counter);

            public void onTick(long millisUntilFinished) {
                long days = millisUntilFinished / 86400000;
                long hours = (millisUntilFinished % 86400000) / 3600000;
                long minutes = ((millisUntilFinished % 86400000) % 3600000) / 60000;
                long seconds = (((millisUntilFinished % 86400000) % 3600000) % 60000) / 1000;

                days_counter.setText(String.valueOf(days));
                hours_counter.setText(String.valueOf(hours));
                minutes_counter.setText(String.valueOf(minutes));
                seconds_counter.setText(String.valueOf(seconds));
            }

            public void onFinish() {
                days_counter.setText("0");
                hours_counter.setText("0");
                minutes_counter.setText("0");
                seconds_counter.setText("0");
            }
        }.start();
    }

    private void showResults(WeeklyRace weeklyRace) {
        View countdownView = findViewById(R.id.timer_card_countdown);
        View resultsView = findViewById(R.id.timer_card_results);
        View raceCancelledView = findViewById(R.id.timer_card_race_cancelled);

        // Hide countdown view and show results view
        countdownView.setVisibility(View.GONE);
        resultsView.setVisibility(View.VISIBLE);

        processRaceResults(weeklyRace);

        // Set podium circuit image
        ImageView trackOutline = findViewById(R.id.track_outline_image);
        Glide.with(this).load(track.getTrack_minimal_layout_url()).into(trackOutline);
    }

    private void processRaceResults(WeeklyRace weeklyRace) {
        Log.i(TAG, "Processing race results" + " " + weeklyRace.getRound());
        MutableLiveData<Result> resultMutableLiveData = eventViewModel.getRaceResults(0L, weeklyRace.getRound());

        resultMutableLiveData.observe(this, result -> {
            Race race = ((Result.LastRaceResultsSuccess) result).getData();
            List<RaceResult> podium = race.getRaceResults();
            Log.i(TAG, "Podium size" + podium.size());
            if (podium == null) {
                showPendingResults();
            } else {
                Log.i(TAG, "Podium found" + podium.size());
                for (int i = 0; i < 3; i++) {
                    String driverId = podium.get(i).getDriver().getDriverId();
                    String teamId = podium.get(i).getConstructor().getConstructorId();

                    TextView driverName = findViewById(Constants.PODIUM_DRIVER_NAME.get(i));
                    driverName.setText(podium.get(i).getDriver().getFullName());


                    LinearLayout teamColor = findViewById(Constants.PODIUM_TEAM_COLOR.get(i));
                    Integer teamColorObj = Constants.TEAM_COLOR.get(teamId);
                    teamColor.setBackgroundColor(ContextCompat.getColor(this, Objects.requireNonNullElseGet(teamColorObj, () -> R.color.mercedes_f1)));
                }
            }
        });
    }

    private void showPendingResults() {
        Log.i(TAG, "No results found");
        View pendingResultsView = findViewById(R.id.timer_card_pending_results);
        View countdownView = findViewById(R.id.timer_card_countdown);
        View resultsView = findViewById(R.id.timer_card_results);

        pendingResultsView.setVisibility(View.VISIBLE);
        countdownView.setVisibility(View.GONE);
        resultsView.setVisibility(View.GONE);
    }

    private void createWeekSchedule(List<Session> sessions) {
        String sessionId;

        for (Session session : sessions) {
            sessionId = session.getClass().getSimpleName();
            if (sessionId.equals("Practice")) {
                Practice practice = (Practice) session;
                sessionId = practice.getPractice();
            }

            TextView sessionName = findViewById(Constants.SESSION_NAME_FIELD.get(sessionId));
            sessionName.setText(Constants.SESSION_NAMES.get(sessionId));

            TextView sessionDay = findViewById(Constants.SESSION_DAY_FIELD.get(sessionId));
            sessionDay.setText(Constants.SESSION_DAY.get(sessionId));

            TextView sessionTime = findViewById(Constants.SESSION_TIME_FIELD.get(sessionId));
            if (sessionId.equals("Race")) sessionTime.setText(session.getStartingTime());
            else sessionTime.setText(session.getTime());

            setChequeredFlag(session);
        }
    }

    private void setChequeredFlag(Session session) {
        String sessionId = session.getClass().getSimpleName();
        if (sessionId.equals("Practice")) {
            Practice practice = (Practice) session;
            sessionId = practice.getPractice();
        }

        if (session.isFinished()) {
            ImageView flag = findViewById(Constants.SESSION_FLAG_FIELD.get(sessionId));
            flag.setVisibility(View.VISIBLE);

            LinearLayout currentSession = findViewById(Constants.SESSION_ROW.get(sessionId));
            currentSession.setClickable(true);
            currentSession.setFocusable(true);
            currentSession.setOnClickListener(view -> Log.i(TAG, "session clicked"));
        }

        loadingScreen.hideLoadingScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}