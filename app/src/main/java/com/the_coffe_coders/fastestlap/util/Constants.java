package com.the_coffe_coders.fastestlap.util;

import com.the_coffe_coders.fastestlap.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO:
 * - Extract maps to database, use url to fetch data from firebase
 */

public class Constants {

    public static final int SPACER_HEIGHT = 20;

    public static final String SHARED_PREFERENCES_FILENAME = "shared";
    public static final String SHARED_PREFERENCES_FAVORITE_DRIVER = "driver_favorite";
    public static final String SHARED_PREFERENCES_FAVORITE_TEAM = "team_favorite";
    public static final String SHARED_PREFERENCES_AUTO_LOGIN = "auto_login";
    public static final String SHARED_PREFERENCES_LAST_UPDATE = "last_update";
    public static final int DATABASE_VERSION = 1;
    public static final String SAVED_DRIVERS_STANDINGS_DATABASE = "saved_drivers_standings";
    public static final int FRESH_TIMEOUT = 1000 * 60; // 1 minute in milliseconds
    public static final String RETROFIT_ERROR = "retrofit_error";
    public static final String API_KEY_ERROR = "api_key_error";

    public static final int MAX_RETRY_COUNT = 4;

    public static final Map<String, Integer> TEAM_COLOR = new HashMap<String, Integer>() {{
        put("mercedes", R.color.mercedes_f1);
        put("red_bull", R.color.redbull_f1);
        put("mclaren", R.color.mclaren_f1);
        put("ferrari", R.color.ferrari_f1);
        put("aston_martin", R.color.aston_martin_f1);
        put("alpine", R.color.alpine_f1);
        put("haas", R.color.haas_f1);
        put("sauber", R.color.kick_f1);
        put("williams", R.color.williams_f1);
        put("rb", R.color.racing_bulls_f1); // Racing Bulls
    }};

    public static final Map<String, Integer> TEAM_GRADIENT_COLOR = new HashMap<String, Integer>() {{
        put("mercedes", R.drawable.gradient_color_mercedes);
        put("red_bull", R.drawable.gradient_color_red_bull);
        put("mclaren", R.drawable.gradient_color_mclaren);
        put("ferrari", R.drawable.gradient_color_ferrari);
        put("aston_martin", R.drawable.gradient_color_aston_martin);
        put("alpine", R.drawable.gradient_color_alpine);
        put("haas", R.drawable.gradient_color_haas);
        put("sauber", R.drawable.gradient_color_sauber);
        put("williams", R.drawable.gradient_color_williams);
        put("rb", R.drawable.gradient_color_rb); // Racing Bulls
    }};

    public static final String[] SESSIONS = {
            "Practice1",
            "Practice2",
            "Practice3",
            "SprintQualifying",
            "Sprint",
            "Qualifying",
            "Race"
    };

    public static final Map<String, String> SESSION_NAMES = Map.of(
            "Practice1", "Practice 1",
            "Practice2", "Practice 2",
            "Practice3", "Practice 3",
            "Qualifying", "Qualifying",
            "SprintQualifying", "Sprint Qualifying",
            "Sprint", "Sprint",
            "Race", "Race"
    );

    public static final Map<String, Integer> SESSION_DURATION = Map.of(
            "Practice", 60,
            "Qualifying", 60,
            "SprintQualifying", 45,
            "Sprint", 60,
            "Race", 120
    );

    public static final Map<String, Integer> SESSION_FLAG_FIELD = Map.of(
            "Practice1", R.id.session_1_flag,
            "Practice2", R.id.session_2_flag,
            "Practice3", R.id.session_3_flag,
            "SprintQualifying", R.id.session_2_flag,
            "Sprint", R.id.session_3_flag,
            "Qualifying", R.id.session_4_flag,
            "Race", R.id.session_5_flag
    );

    public static final Map<String, Integer> SESSION_NAME_FIELD = Map.of(
            "Practice1", R.id.session_1_name,
            "Practice2", R.id.session_2_name,
            "Practice3", R.id.session_3_name,
            "SprintQualifying", R.id.session_2_name,
            "Sprint", R.id.session_3_name,
            "Qualifying", R.id.session_4_name,
            "Race", R.id.session_5_name
    );

    public static final Map<String, Integer> SESSION_ROW = Map.of(
            "Practice1", R.id.session_1,
            "Practice2", R.id.session_2,
            "Practice3", R.id.session_3,
            "SprintQualifying", R.id.session_2,
            "Sprint", R.id.session_3,
            "Qualifying", R.id.session_4,
            "Race", R.id.session_5
    );


    public static final Map<String, String> SESSION_DAY = Map.of(
            "Practice1", "Friday",
            "Practice2", "Friday",
            "Practice3", "Saturday",
            "SprintQualifying", "Friday",
            "Sprint", "Saturday",
            "Qualifying", "Saturday",
            "Race", "Sunday"
    );

    public static final Map<String, Integer> SESSION_DAY_FIELD = Map.of(
            "Practice1", R.id.session_1_day,
            "Practice2", R.id.session_2_day,
            "Practice3", R.id.session_3_day,
            "SprintQualifying", R.id.session_2_day,
            "Sprint", R.id.session_3_day,
            "Qualifying", R.id.session_4_day,
            "Race", R.id.session_5_day
    );
    public static final Map<String, Integer> SESSION_TIME_FIELD = Map.of(
            "Practice1", R.id.session_1_time,
            "Practice2", R.id.session_2_time,
            "Practice3", R.id.session_3_time,
            "SprintQualifying", R.id.session_2_time,
            "Sprint", R.id.session_3_time,
            "Qualifying", R.id.session_4_time,
            "Race", R.id.session_5_time
    );

    public static final List<Integer> PODIUM_DRIVER_NAME = List.of(
            R.id.first_name,
            R.id.second_name,
            R.id.third_name
    );

    public static final List<Integer> PODIUM_TEAM_COLOR = List.of(
            R.id.first_color,
            R.id.second_color,
            R.id.third_color
    );

    public static final List<Integer> LAST_RACE_DRIVER_NAME = List.of(
            R.id.last_race_first,
            R.id.last_race_second,
            R.id.last_race_third
    );

    public static final List<Integer> PAST_RACE_DRIVER_NAME = List.of(
            R.id.past_first_driver,
            R.id.past_second_driver,
            R.id.past_third_driver
    );

    public static final List<Integer> HOME_SEASON_DRIVER_STANDINGS_NAME_FIELD = List.of(
            R.id.first_driver,
            R.id.second_driver,
            R.id.third_driver
    );

    public static final List<Integer> HOME_SEASON_DRIVER_STANDINGS_COLOR_FIELD = List.of(
            R.id.first_driver_color,
            R.id.second_driver_color,
            R.id.third_driver_color
    );

    public static final List<Integer> HOME_SEASON_TEAM_STANDINGS_NAME_FIELD = List.of(
            R.id.first_team,
            R.id.second_team,
            R.id.third_team
    );

    public static final List<Integer> HOME_SEASON_TEAM_STANDINGS_COLOR_FIELD = List.of(
            R.id.first_team_color,
            R.id.second_team_color,
            R.id.third_team_color
    );

    public static final String INVALID_USER_ERROR = "invalidUserError";
    public static final String INVALID_CREDENTIALS_ERROR = "invalidCredentials";
    public static final String USER_COLLISION_ERROR = "userCollisionError";
    public static final String WEAK_PASSWORD_ERROR = "passwordIsWeak";
    public static final String UNEXPECTED_ERROR = "unexpected_error";
    public static final String FIREBASE_REALTIME_DATABASE = "https://fastest-lap-ac540-default-rtdb.europe-west1.firebasedatabase.app/";
    public static final String FIREBASE_USERS_COLLECTION = "users";
    public static final int MINIMUM_LENGTH_PASSWORD = 8;
    public static final String FIREBASE_DRIVERS_COLLECTION = "drivers";
    public static final String FIREBASE_TEAMS_COLLECTION = "teams";
    public static final String FIREBASE_CIRCUITS_COLLECTION = "circuits";
    public static final String FIREBASE_NATIONS_COLLECTION = "nations";

}
