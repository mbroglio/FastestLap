package com.the_coffe_coders.fastestlap.ui;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.constructor.ConstructorStanding;
import com.the_coffe_coders.fastestlap.domain.driver.DriverStanding;
import com.the_coffe_coders.fastestlap.domain.driver.StandingsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.race_result.Race;
import com.the_coffe_coders.fastestlap.domain.race_result.ResultsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.race_week.FirstPractice;
import com.the_coffe_coders.fastestlap.domain.race_week.Qualifying;
import com.the_coffe_coders.fastestlap.domain.race_week.RaceWeekAPIresponse;
import com.the_coffe_coders.fastestlap.domain.race_week.Races;
import com.the_coffe_coders.fastestlap.domain.race_week.SecondPractice;
import com.the_coffe_coders.fastestlap.domain.race_week.Session;
import com.the_coffe_coders.fastestlap.domain.race_week.Sprint;
import com.the_coffe_coders.fastestlap.domain.race_week.SprintQualifying;
import com.the_coffe_coders.fastestlap.domain.race_week.ThirdPractice;
import com.the_coffe_coders.fastestlap.utils.Constants;
import com.the_coffe_coders.fastestlap.utils.JSONParserUtils;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HomePageActivity extends AppCompatActivity {
    private final String TAG = "HomePageActivity";
    private ErgastAPI ergastAPI;
    private final ZoneId localZone = ZoneId.systemDefault();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_page);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.homepage), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setLastRaceCard();
        setNextSessionCard();
        setFavouriteDriverCard();
        setFavouriteConstructorCard();
    }

    private void setLastRaceCard() {
        String BASE_URL = "https://ergast.com/api/f1/";
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ergastAPI = retrofit.create(ErgastAPI.class);

        ergastAPI.getLastRaceResults().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                Log.i(TAG, "Response: " + response);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        JsonObject jsonObject = new Gson().fromJson(responseBody, JsonObject.class);
                        JsonObject mrdata = jsonObject.getAsJsonObject("MRData");

                        JSONParserUtils parser = new JSONParserUtils(HomePageActivity.this);
                        ResultsAPIResponse raceResults = parser.parseRaceResults(mrdata);

                        showPodium(raceResults);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
            }
        });
    }

    private void showPodium(ResultsAPIResponse raceResults) {
        Race race = raceResults.getRaceTable().getRaces().get(0);
        String circuitId = race.getCircuit().getCircuitId();

        TextView raceName = findViewById(R.id.last_race_name);
        raceName.setText(race.getRaceName());

        ImageView trackOutline = findViewById(R.id.last_race_track_outline);
        trackOutline.setImageResource(Constants.EVENT_CIRCUIT.get(circuitId));

        TextView raceDate = findViewById(R.id.last_race_date);
        LocalDate date = LocalDate.parse(race.getDate());
        raceDate.setText(String.valueOf(date.getDayOfMonth()));

        // Get the first three characters of the month in capital letters
        TextView raceMonth = findViewById(R.id.last_race_month);
        String month = date.getMonth().toString().substring(0, 3).toUpperCase();
        raceMonth.setText(month);

        TextView roundNumber = findViewById(R.id.last_race_round);
        String round = "Round " + race.getRound();
        roundNumber.setText(round);

        setDriverNames(raceResults);
    }

    private void setDriverNames(ResultsAPIResponse raceResults) {
        for (int i = 0; i < 3; i++) {
            String driverId = raceResults.getRaceTable().getRaces().get(0).getResults().get(i).getDriver().getDriverId();
            Integer driverFullName = Constants.DRIVER_FULLNAME.get(driverId);
            TextView driver = findViewById(Constants.LAST_RACE_DRIVER_NAME.get(i));
            driver.setText(driverFullName);
        }
    }

    private void setNextSessionCard() {
        String BASE_URL = "https://api.jolpi.ca/ergast/f1/";
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ergastAPI = retrofit.create(ErgastAPI.class);

        ergastAPI.getNextRace().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                Log.i(TAG, "Response: " + response);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        JsonObject jsonObject = new Gson().fromJson(responseBody, JsonObject.class);
                        JsonObject mrdata = jsonObject.getAsJsonObject("MRData");

                        JSONParserUtils parser = new JSONParserUtils(HomePageActivity.this);
                        RaceWeekAPIresponse raceSchedule = parser.parseRaceWeek(mrdata);

                        processNextRace(raceSchedule);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
            }
        });
    }

    private void processNextRace(RaceWeekAPIresponse raceSchedule) {
        Races race = raceSchedule.getRaceTable().getRaces().get(0);

        TextView nextRaceName = findViewById(R.id.home_next_gp_name);
        nextRaceName.setText(race.getRaceName());

        ImageView nextRaceFlag = findViewById(R.id.home_next_gp_flag);
        String nation = race.getCircuit().getLocation().getCountry();
        nextRaceFlag.setImageResource(Constants.NATION_COUNTRY_FLAG.get(nation));

        List<Session> sessions = populateSessions(raceSchedule);
        Session nextEvent = findNextEvent(sessions);
        if (nextEvent != null) {
            ZonedDateTime eventDateTime = nextEvent.getStartDateTime();
            startCountdown(eventDateTime);
        }

        TextView sessionType = findViewById(R.id.next_session_type);
        sessionType.setText(Constants.SESSION_NAMES.get(nextEvent.getSessionId()));
    }

    private List<Session> populateSessions(RaceWeekAPIresponse raceSchedule) {
        List<Session> sessions = new ArrayList<>();
        Races race = raceSchedule.getRaceTable().getRaces().get(0);

        addSession(sessions, race.getFirstPractice());
        addSession(sessions, race.getSecondPractice());
        addSession(sessions, race.getThirdPractice());
        addSession(sessions, race.getSprintQualifying());
        addSession(sessions, race.getSprint());
        addSession(sessions, race.getQualifying());

        // Add Race session
        ZonedDateTime startDateTime = ZonedDateTime.parse(
                race.getDate() + "T" + race.getTime() + "[UTC]"
        );
        sessions.add(new Session("Race", false, false, startDateTime, null));

        return sessions;
    }

    private void addSession(List<Session> sessions, Object tmpSession) {
        if (tmpSession != null) {
            String sessionId = tmpSession.getClass().getSimpleName();
            Log.i(TAG, "Session ID: " + sessionId);

            String date = null;
            String time = null;

            if (tmpSession instanceof FirstPractice) {
                date = ((FirstPractice) tmpSession).getDate();
                time = ((FirstPractice) tmpSession).getTime();
            } else if (tmpSession instanceof SecondPractice) {
                date = ((SecondPractice) tmpSession).getDate();
                time = ((SecondPractice) tmpSession).getTime();
            } else if (tmpSession instanceof ThirdPractice) {
                date = ((ThirdPractice) tmpSession).getDate();
                time = ((ThirdPractice) tmpSession).getTime();
            } else if (tmpSession instanceof SprintQualifying) {
                date = ((SprintQualifying) tmpSession).getDate();
                time = ((SprintQualifying) tmpSession).getTime();
            } else if (tmpSession instanceof Sprint) {
                date = ((Sprint) tmpSession).getDate();
                time = ((Sprint) tmpSession).getTime();
            } else if (tmpSession instanceof Qualifying) {
                date = ((Qualifying) tmpSession).getDate();
                time = ((Qualifying) tmpSession).getTime();
            }

            if (date != null && time != null) {
                ZonedDateTime startDateTime = ZonedDateTime.parse(date + "T" + time + "[UTC]");
                sessions.add(new Session(sessionId, false, false, startDateTime, null));
            }
        }
    }

    private Session findNextEvent(List<Session> sessions) {
        ZonedDateTime currentDateTime = ZonedDateTime.now(localZone);
        Session nextSession = null;

        for (Session session : sessions) {
            Log.i(TAG, "Session: " + session.getStartDateTime());
            if (currentDateTime.isAfter(session.getStartDateTime()) && currentDateTime.isBefore(session.getEndDateTime())) {
                session.setUnderway(true);
                Log.i(TAG, "Session underway: " + session.getSessionId());
            } else if (currentDateTime.isAfter(session.getEndDateTime())) {
                session.setUnderway(false);
                session.setFinished(true);
            }
        }

        for (Session session : sessions) {
            if (!session.isFinished()) {
                nextSession = session;
                break;
            }
        }

        return nextSession;
    }

    private void startCountdown(ZonedDateTime eventDate) {
        long millisUntilStart = eventDate.toInstant().toEpochMilli() - ZonedDateTime.now(ZoneId.of("UTC")).toInstant().toEpochMilli();
        new CountDownTimer(millisUntilStart, 1000) {
            TextView days_counter = findViewById(R.id.next_days_counter);
            TextView hours_counter = findViewById(R.id.next_hours_counter);
            TextView minutes_counter = findViewById(R.id.next_minutes_counter);
            TextView seconds_counter = findViewById(R.id.next_seconds_counter);

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

    private void setFavouriteDriverCard() {
        String BASE_URL = "https://api.jolpi.ca/ergast/f1/current/";
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ergastAPI = retrofit.create(ErgastAPI.class);

        ergastAPI.getDriverStandings().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                Log.i(TAG, "Response: " + response);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseString = response.body().string();
                        JsonObject jsonResponse = new Gson().fromJson(responseString, JsonObject.class);
                        JsonObject mrdata = jsonResponse.getAsJsonObject("MRData");

                        JSONParserUtils jsonParserUtils = new JSONParserUtils(HomePageActivity.this);
                        StandingsAPIResponse standing = jsonParserUtils.parseDriverStandings(mrdata);

                        processStanding(standing);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
            }
        });
    }

    private void processStanding(StandingsAPIResponse standing) {
        List<DriverStanding> standings = standing.getStandingsTable().getStandingsLists().get(0).getDriverStandings();

        for (DriverStanding standingElement : standings){
            if(standingElement.getDriver().getDriverId().equals(Constants.FAVOURITE_DRIVER)){
                buildDriverCard(standingElement);
            }
        }
    }

    private void buildDriverCard(DriverStanding standingElement) {
        TextView driverName = findViewById(R.id.favourite_driver_name);
        driverName.setText(Constants.DRIVER_FULLNAME.get(Constants.FAVOURITE_DRIVER));

        String driverNationality = standingElement.getDriver().getNationality();
        ImageView driverFlag = findViewById(R.id.favourite_driver_flag);
        driverFlag.setImageResource(Constants.NATION_COUNTRY_FLAG.get(Constants.NATIONALITY_NATION.get(driverNationality)));

        TextView nationality = findViewById(R.id.favourite_driver_nationality);
        nationality.setText(driverNationality.toUpperCase());

        ImageView driverImage = findViewById(R.id.favourite_driver_pic);
        driverImage.setImageResource(Constants.DRIVER_IMAGE.get(Constants.FAVOURITE_DRIVER));

        TextView driverPosition = findViewById(R.id.favourite_driver_position);
        driverPosition.setText(standingElement.getPosition());

        TextView driverPoints = findViewById(R.id.favourite_driver_points);
        driverPoints.setText(standingElement.getPoints());
    }

    private void setFavouriteConstructorCard() {
        String BASE_URL = "https://api.jolpi.ca/ergast/f1/current/";
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ergastAPI = retrofit.create(ErgastAPI.class);

        ergastAPI.getConstructorStandings().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                Log.i(TAG, "Response: " + response);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseString = response.body().string();
                        JsonObject jsonResponse = new Gson().fromJson(responseString, JsonObject.class);
                        JsonObject mrdata = jsonResponse.getAsJsonObject("MRData");

                        JSONParserUtils jsonParserUtils = new JSONParserUtils(HomePageActivity.this);
                        com.the_coffe_coders.fastestlap.domain.constructor.StandingsAPIResponse standing = jsonParserUtils.parseConstructorStandings(mrdata);

                        processConstructorStanding(standing);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
            }
        });
    }

    private void processConstructorStanding(com.the_coffe_coders.fastestlap.domain.constructor.StandingsAPIResponse standing) {
        List<com.the_coffe_coders.fastestlap.domain.constructor.ConstructorStanding> standings = standing.getStandingsTable().getStandingsLists().get(0).getConstructorStandings();

        for (com.the_coffe_coders.fastestlap.domain.constructor.ConstructorStanding standingElement : standings){
            if(standingElement.getConstructor().getConstructorId().equals(Constants.FAVOURITE_TEAM)){
                buildConstructorCard(standingElement);
            }
        }
    }

    private void buildConstructorCard(ConstructorStanding standingElement) {
        TextView constructorName = findViewById(R.id.favourite_constructor_name);
        constructorName.setText(standingElement.getConstructor().getName());

        ImageView constructorCar = findViewById(R.id.favourite_constructor_car);
        constructorCar.setImageResource(Constants.TEAM_CAR.get(Constants.FAVOURITE_TEAM));

        TextView constructorPosition = findViewById(R.id.favourite_constructor_position);
        constructorPosition.setText(standingElement.getPosition());

        TextView constructorPoints = findViewById(R.id.favourite_constructor_points);
        constructorPoints.setText(standingElement.getPoints());

        ImageView constructorFlag = findViewById(R.id.favourite_constructor_flag);
        String nationality = standingElement.getConstructor().getNationality();
        Log.i(TAG, "Nationality: " + nationality);
        constructorFlag.setImageResource(Constants.NATION_COUNTRY_FLAG.get(Constants.NATIONALITY_NATION.get(nationality)));

        TextView constructorNationality = findViewById(R.id.favourite_constructor_nationality);
        constructorNationality.setText(nationality.toUpperCase());
    }
}