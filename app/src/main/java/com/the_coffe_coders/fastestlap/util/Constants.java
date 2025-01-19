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

    public static final String SHARED_PREFERENCES_FILENAME = "";
    public static final String SHARED_PREFERENCES_FAVORITE_DRIVER = "driver_favorite";
    public static final String SHARED_PREFERENCES_FAVORITE_TEAM = "team_favorite";

    public static final String SHARED_PREFERENCES_LAST_UPDATE = "last_update";

    public static final String SAMPLE_JSON_FILENAME = "sample_api_response.json";

    public static final int DATABASE_VERSION = 1;
    public static final String SAVED_DRIVERS_DATABASE = "saved_db";
    public static final String SAVED_DRIVERS_STANDINGS_DATABASE = "saved_drivers_standings";

    public static final int FRESH_TIMEOUT = 1000 * 60; // 1 minute in milliseconds

    public static final String RETROFIT_ERROR = "retrofit_error";
    public static final String API_KEY_ERROR = "api_key_error";
    public static final Map<String, Integer> DRIVER_IMAGE = new HashMap<String, Integer>() {{
        put("hamilton", R.drawable.hamilton_pic);
        put("russell", R.drawable.russell_pic);
        put("max_verstappen", R.drawable.verstappen_pic);
        put("perez", R.drawable.perez_pic);
        put("norris", R.drawable.norris_pic);
        put("piastri", R.drawable.piastri_pic);
        put("leclerc", R.drawable.leclerc_pic);
        put("sainz", R.drawable.sainz_pic);
        put("alonso", R.drawable.alonso_pic);
        put("stroll", R.drawable.stroll_pic);
        put("ocon", R.drawable.ocon_pic);
        put("gasly", R.drawable.gasly_pic);
        put("kevin_magnussen", R.drawable.magnussen_pic);
        put("hulkenberg", R.drawable.hulkenberg_pic);
        put("bottas", R.drawable.bottas_pic);
        put("zhou", R.drawable.zhou_pic);
        put("albon", R.drawable.albon_pic);
        put("sargeant", R.drawable.sargeant_pic);
        put("ricciardo", R.drawable.ricciardo_pic);
        put("tsunoda", R.drawable.tsunoda_pic);
        put("bearman", R.drawable.bearman_pic);
        put("colapinto", R.drawable.colapinto_pic);
        put("lawson", R.drawable.lawson_pic);
        put("doohan", R.drawable.doohan_pic);
    }};

    public static final Map<String, Integer> DRIVER_FULLNAME = new HashMap<String, Integer>() {
        {
            put("hamilton", R.string.lewis_hamilton);
            put("russell", R.string.george_russell);
            put("max_verstappen", R.string.max_verstappen);
            put("perez", R.string.sergio_perez);
            put("norris", R.string.lando_norris);
            put("piastri", R.string.oscar_piastri);
            put("leclerc", R.string.charles_leclerc);
            put("sainz", R.string.carlos_sainz);
            put("alonso", R.string.fernando_alonso);
            put("stroll", R.string.lance_stroll);
            put("ocon", R.string.esteban_ocon);
            put("gasly", R.string.pierre_gasly);
            put("kevin_magnussen", R.string.kevin_magnussen);
            put("hulkenberg", R.string.nico_hulkenberg);
            put("bottas", R.string.valtteri_bottas);
            put("zhou", R.string.zhou_guanyu);
            put("albon", R.string.alexander_albon);
            put("sargeant", R.string.logan_sargeant);
            put("ricciardo", R.string.daniel_ricciardo);
            put("tsunoda", R.string.yuki_tsunoda);
            put("bearman", R.string.oliver_bearman);
            put("colapinto", R.string.franco_colapinto);
            put("lawson", R.string.liam_lawson);
            put("doohan", R.string.jack_doohan);
        }
    };

    public static final Map<String, String> DRIVER_TEAM = new HashMap<String, String>() {{
        put("hamilton", "mercedes");
        put("russell", "mercedes");
        put("max_verstappen", "red_bull");
        put("perez", "red_bull");
        put("norris", "mclaren");
        put("piastri", "mclaren");
        put("leclerc", "ferrari");
        put("sainz", "ferrari");
        put("alonso", "aston_martin");
        put("stroll", "aston_martin");
        put("ocon", "alpine");
        put("gasly", "alpine");
        put("kevin_magnussen", "haas");
        put("hulkenberg", "haas");
        put("bottas", "sauber");
        put("zhou", "sauber");
        put("albon", "williams");
        put("sargeant", "williams");
        put("ricciardo", "rb");
        put("tsunoda", "rb");
        put("bearman", "haas");
        put("colapinto", "williams");
        put("lawson", "rb");
        put("doohan", "alpine");
    }};

    public static final Map<String, Integer> TEAM_LOGO = new HashMap<String, Integer>() {{
        put("mercedes", R.drawable.mercedeslogo);
        put("red_bull", R.drawable.redbulllogo);
        put("mclaren", R.drawable.mclarenlogo);
        put("ferrari", R.drawable.ferrarilogo);
        put("aston_martin", R.drawable.astonmartinlogo);
        put("alpine", R.drawable.alpinelogo);
        put("haas", R.drawable.haaslogo);
        put("sauber", R.drawable.stakelogo);
        put("williams", R.drawable.williamslogo);
        put("rb", R.drawable.racingbullslogo); // Racing Bulls
    }};

    public static final Map<String, Integer> TEAM_LOGO_DRIVER_CARD = new HashMap<String, Integer>() {{
        put("mercedes", R.drawable.mercedeslogo);
        put("red_bull", R.drawable.redbulllogo_short);
        put("mclaren", R.drawable.mclarenlogo_short);
        put("ferrari", R.drawable.ferrarilogo);
        put("aston_martin", R.drawable.astonmartinlogo_short);
        put("alpine", R.drawable.alpinelogo_short);
        put("haas", R.drawable.haaslogo_short);
        put("sauber", R.drawable.stakelogo);
        put("williams", R.drawable.williamslogo);
        put("rb", R.drawable.racingbullslogo_short); // Racing Bulls
    }};


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

    public static final Map<String, Integer> TEAM_FULLNAME = new HashMap<String, Integer>() {{
        put("mercedes", R.string.mercedes);
        put("red_bull", R.string.red_bull);
        put("mclaren", R.string.mclaren);
        put("ferrari", R.string.ferrari);
        put("aston_martin", R.string.aston_martin);
        put("alpine", R.string.alpine);
        put("haas", R.string.haas);
        put("sauber", R.string.sauber);
        put("williams", R.string.williams);
        put("rb", R.string.racing_bulls); // Racing Bulls
    }};

    public static final Map<String, Integer> TEAM_CAR = new HashMap<String, Integer>() {{
        put("mercedes", R.drawable.mercedes);
        put("red_bull", R.drawable.red_bull_racing);
        put("mclaren", R.drawable.mclaren);
        put("ferrari", R.drawable.ferrari);
        put("aston_martin", R.drawable.aston_martin);
        put("alpine", R.drawable.alpine);
        put("haas", R.drawable.haas);
        put("sauber", R.drawable.kick_sauber);
        put("williams", R.drawable.williams);
        put("rb", R.drawable.rb);
    }};

    public static final Map<String, String> TEAM_DRIVER1 = new HashMap<String, String>() {{
        put("mercedes", "hamilton");
        put("red_bull", "max_verstappen");
        put("mclaren", "norris");
        put("ferrari", "leclerc");
        put("aston_martin", "alonso");
        put("alpine", "gasly");
        put("haas", "hulkenberg");
        put("sauber", "bottas");
        put("williams", "albon");
        put("rb", "tsunoda");
    }};

    public static final Map<String, String> TEAM_DRIVER2 = new HashMap<String, String>() {{
        put("mercedes", "russell");
        put("red_bull", "perez");
        put("mclaren", "piastri");
        put("ferrari", "sainz");
        put("aston_martin", "stroll");
        put("alpine", "ocon");
        put("haas", "kevin_magnussen");
        put("sauber", "zhou");
        put("williams", "colapinto");
        put("rb", "lawson");
    }};

    // Event Constants
    public static final Map<String, String> TRACK_LONG_GP_NAME = new HashMap<>() {{
        put("bahrain", "FORMULA 1 GULF AIR\nBAHRAIN GRAND PRIX");
        put("jeddah", "FORMULA 1 STC\nSAUDI ARABIAN GRAND PRIX");
        put("albert_park", "FORMULA 1 ROLEX\nAUSTRALIAN GRAND PRIX");
        put("suzuka", "FORMULA 1 MSC CRUISES\nJAPANESE GRAND PRIX");
        put("shanghai", "FORMULA 1 LENOVO\nCHINESE GRAND PRIX");
        put("miami", "FORMULA 1 CRYPTO.COM\nMIAMI GRAND PRIX");
        put("imola", "FORMULA 1 MSC CRUISES\nGRAN PREMIO DEL MADE IN ITALY E DELL'EMILIA-ROMAGNA");
        put("monaco", "FORMULA 1\nGRAND PRIX DE MONACO");
        put("villeneuve", "FORMULA 1 AWS\nGRAND PRIX DU CANADA");
        put("catalunya", "FORMULA 1 ARAMCO\nGRAN PREMIO DE ESPAÑA");
        put("red_bull_ring", "FORMULA 1 QATAR AIRWAYS\nAUSTRIAN GRAND PRIX");
        put("silverstone", "FORMULA 1 QATAR AIRWAYS\nBRITISH GRAND PRIX");
        put("hungaroring", "FORMULA 1\nHUNGARIAN GRAND PRIX");
        put("spa", "FORMULA 1 ROLEX\nBELGIAN GRAND PRIX");
        put("zandvoort", "FORMULA 1 HEINEKEN\nDUTCH GRAND PRIX");
        put("monza", "FORMULA 1 PIRELLI\nGRAN PREMIO D’ITALIA");
        put("baku", "FORMULA 1 QATAR AIRWAYS\nAZERBAIJAN GRAND PRIX");
        put("marina_bay", "FORMULA 1 SINGAPORE AIRLINES\nSINGAPORE GRAND PRIX");
        put("americas", "FORMULA 1 PIRELLI\nUNITED STATES GRAND PRIX");
        put("rodriguez", "FORMULA 1\nGRAN PREMIO DE LA CIUDAD DE MÉXICO");
        put("interlagos", "FORMULA 1 LENOVO\nGRANDE PRÊMIO DE SÃO PAULO");
        put("vegas", "FORMULA 1 HEINEKEN SILVER\nLAS VEGAS GRAND PRIX");
        put("losail", "FORMULA 1 QATAR AIRWAYS\nQATAR GRAND PRIX");
        put("yas_marina", "FORMULA 1 ETIHAD AIRWAYS\nABU DHABI GRAND PRIX");
    }};

    public static final Map<String, Integer> EVENT_PICTURE = new HashMap<>() {{
        put("bahrain", R.drawable.bahrain);
        put("jeddah", R.drawable.saudi_arabia);
        put("albert_park", R.drawable.australia_image);
        put("suzuka", R.drawable.japan_image);
        put("shanghai", R.drawable.china_image);
        put("miami", R.drawable.miami);
        put("imola", R.drawable.emilia_romagna);
        put("monaco", R.drawable.monaco_image);
        put("villeneuve", R.drawable.canada_image);
        put("catalunya", R.drawable.spain_image);
        put("red_bull_ring", R.drawable.austria);
        put("silverstone", R.drawable.great_britain);
        put("hungaroring", R.drawable.hungary);
        put("spa", R.drawable.belgium);
        put("zandvoort", R.drawable.netherlands_image);
        put("monza", R.drawable.italy);
        put("baku", R.drawable.azerbaijan);
        put("marina_bay", R.drawable.singapore);
        put("americas", R.drawable.usa);
        put("rodriguez", R.drawable.mexico_image);
        put("interlagos", R.drawable.brazil);
        put("vegas", R.drawable.las_vegas);
        put("losail", R.drawable.qatar_image);
        put("yas_marina", R.drawable.abu_dhabi);
    }};
    public static final Map<String, Integer> NATION_COUNTRY_FLAG = new HashMap<>() {{
        put("Bahrain", R.drawable.bahrain_flag);
        put("Saudi Arabia", R.drawable.saudi_arabia_flag);
        put("Australia", R.drawable.australia_flag);
        put("Japan", R.drawable.japan_flag);
        put("China", R.drawable.china_flag);
        put("USA", R.drawable.united_states_flag);
        put("Monaco", R.drawable.monaco_flag);
        put("Canada", R.drawable.canada_flag);
        put("Spain", R.drawable.spain_flag);
        put("Austria", R.drawable.austria_flag);
        put("UK", R.drawable.united_kingdom_flag);
        put("Hungary", R.drawable.hungary_flag);
        put("Belgium", R.drawable.belgium_flag);
        put("Netherlands", R.drawable.netherlands_flag);
        put("Italy", R.drawable.italy_flag);
        put("Azerbaijan", R.drawable.azerbaijan_flag);
        put("Singapore", R.drawable.singapore_flag);
        put("Qatar", R.drawable.qatar_flag);
        put("UAE", R.drawable.uae_flag);
        put("Brazil", R.drawable.brazil_flag);
        put("Mexico", R.drawable.mexico_flag);
        put("United States", R.drawable.united_states_flag);
        put("Germany", R.drawable.germany_flag);
        put("France", R.drawable.france_flag);
    }};

    public static final Map<String, String> NATIONALITY_NATION = new HashMap<>() {{
        put("British", "UK");
        put("Dutch", "Netherlands");
        put("Australian", "Australia");
        put("Japanese", "Japan");
        put("Chinese", "China");
        put("American", "USA");
        put("Monégasque", "Monaco");
        put("Canadian", "Canada");
        put("Spanish", "Spain");
        put("French", "France");
        put("Mexican", "Mexico");
        put("Brazilian", "Brazil");
        put("Italian", "Italy");
        put("Azerbaijani", "Azerbaijan");
        put("Singaporean", "Singapore");
        put("Qatari", "Qatar");
        put("Emirati", "UAE");
        put("German", "Germany");
    }};

    public static final Map<String, String> NATIONALITY_ABBREVIATION = new HashMap<>() {{
        put("British", "GBR");
        put("Dutch", "NED");
        put("Australian", "AUS");
        put("Japanese", "JAP");
        put("Chinese", "CHI");
        put("American", "USA");
        put("Monégasque", "MON");
        put("Canadian", "CAM");
        put("Spanish", "SPA");
        put("French", "FRA");
        put("Mexican", "MEX");
        put("Brazilian", "BRA");
        put("Italian", "ITA");
        put("Azerbaijani", "AZE");
        put("Singaporean", "SGP");
        put("Qatari", "QAT");
        put("Emirati", "UAE");
        put("German", "GER");
    }};

    public static final Map<String, Integer> EVENT_CIRCUIT = new HashMap<>() {{
        put("bahrain", R.drawable.f1_2024_bhr_outline);
        put("jeddah", R.drawable.f1_2024_sau_outline);
        put("albert_park", R.drawable.f1_2024_aus_outline);
        put("suzuka", R.drawable.f1_2024_jap_outline);
        put("shanghai", R.drawable.f1_2024_chn_outline);
        put("miami", R.drawable.f1_2024_mia_outline);
        put("imola", R.drawable.f1_2024_ero_outline);
        put("monaco", R.drawable.f1_2024_mco_outline);
        put("villeneuve", R.drawable.f1_2024_can_outline);
        put("catalunya", R.drawable.f1_2024_spn_outline);
        put("red_bull_ring", R.drawable.f1_2024_aut_outline);
        put("silverstone", R.drawable.f1_2024_gbr_outline);
        put("hungaroring", R.drawable.f1_2024_hun_outline);
        put("spa", R.drawable.f1_2024_bel_outline);
        put("zandvoort", R.drawable.f1_2024_nld_outline);
        put("monza", R.drawable.f1_2024_ita_outline);
        put("baku", R.drawable.f1_2024_aze_outline);
        put("marina_bay", R.drawable.f1_2024_sgp_outline);
        put("americas", R.drawable.f1_2024_usa_outline);
        put("rodriguez", R.drawable.f1_2024_mex_outline);
        put("interlagos", R.drawable.f1_2024_bra_outline);
        put("vegas", R.drawable.f1_2024_lve_outline);
        put("losail", R.drawable.f1_2024_qat_outline);
        put("yas_marina", R.drawable.f1_2024_abu_outline);
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
}
