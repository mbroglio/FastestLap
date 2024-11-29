package com.the_coffe_coders.fastestlap;

import java.util.HashMap;
import java.util.Map;

public class Constants {
    public static final Map<String, String> TRACK_LONG_GP_NAME = new HashMap<>() {{
        put("bahrain", "FORMULA 1 GULF AIR BAHRAIN GRAND PRIX");
        put("jeddah", "FORMULA 1 STC SAUDI ARABIAN GRAND PRIX");
        put("albert_park", "FORMULA 1 ROLEX AUSTRALIAN GRAND PRIX");
        put("suzuka", "FORMULA 1 MSC CRUISES JAPANESE GRAND PRIX");
        put("shanghai", "FORMULA 1 LENOVO CHINESE GRAND PRIX");
        put("miami", "FORMULA 1 CRYPTO.COM MIAMI GRAND PRIX");
        put("imola", "FORMULA 1 MSC CRUISES GRAN PREMIO DEL MADE IN ITALY E DELL'EMILIA-ROMAGNA");
        put("monaco", "FORMULA 1 GRAND PRIX DE MONACO");
        put("villeneuve", "FORMULA 1 AWS GRAND PRIX DU CANADA");
        put("catalunya", "FORMULA 1 ARAMCO GRAN PREMIO DE ESPAÑA");
        put("red_bull_ring", "FORMULA 1 QATAR AIRWAYS AUSTRIAN GRAND PRIX");
        put("silverstone", "FORMULA 1 QATAR AIRWAYS BRITISH GRAND PRIX");
        put("hungaroring", "FORMULA 1 HUNGARIAN GRAND PRIX");
        put("spa", "FORMULA 1 ROLEX BELGIAN GRAND PRIX");
        put("zandvoort", "FORMULA 1 HEINEKEN DUTCH GRAND PRIX");
        put("monza", "FORMULA 1 PIRELLI GRAN PREMIO D’ITALIA");
        put("baku", "FORMULA 1 QATAR AIRWAYS AZERBAIJAN GRAND PRIX");
        put("marina_bay", "FORMULA 1 SINGAPORE AIRLINES SINGAPORE GRAND PRIX");
        put("americas", "FORMULA 1 PIRELLI UNITED STATES GRAND PRIX");
        put("rodriguez", "FORMULA 1 GRAN PREMIO DE LA CIUDAD DE MÉXICO");
        put("interlagos", "FORMULA 1 LENOVO GRANDE PRÊMIO DE SÃO PAULO");
        put("vegas", "FORMULA 1 HEINEKEN SILVER LAS VEGAS GRAND PRIX");
        put("losail", "FORMULA 1 QATAR AIRWAYS QATAR GRAND PRIX");
        put("yas_marina", "FORMULA 1 ETIHAD AIRWAYS ABU DHABI GRAND PRIX");
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

    public static final Map<String, Integer> EVENT_COUNTRY_FLAG = new HashMap<>() {{
        //put("bahrain", R.drawable.bahrain_flag);
        //put("jeddah", R.drawable.saudi_arabia_flag);
        put("albert_park", R.drawable.australia);
        put("suzuka", R.drawable.japan);
        put("shanghai", R.drawable.china);
        put("miami", R.drawable.united_states);
        //put("imola", R.drawable.italy_flag);
        put("monaco", R.drawable.monaco);
        put("villeneuve", R.drawable.canada);
        put("catalunya", R.drawable.spain);
        //put("red_bull_ring", R.drawable.austria_flag);
        put("silverstone", R.drawable.united_kingdom);
        //put("hungaroring", R.drawable.hungary_flag);
        //put("spa", R.drawable.belgium_flag);
        put("zandvoort", R.drawable.netherlands);
        //put("monza", R.drawable.italy_flag);
        //put("baku", R.drawable.azerbaijan_flag);
        //put("marina_bay", R.drawable.singapore_flag);
        put("americas", R.drawable.united_states);
        put("rodriguez", R.drawable.mexico);
        //put("interlagos", R.drawable.brazil_flag);
        put("vegas", R.drawable.united_states);
        put("losail", R.drawable.qatar_flag);
        //put("yas_marina", R.drawable.united_arab_emirates_flag);
    }};

    public static final Map<String, Integer> EVENT_CIRCUIT = new HashMap<>() {{
        put("bahrain", R.drawable.f1_2024_bhr_outline);
        put("jeddah", R.drawable.f1_2024_sau_outline);
        put("albert_park", R.drawable.f1_2024_aus_outline);
        put("suzuka", R.drawable.f1_2024_jap_outline);
        put("shangai", R.drawable.f1_2024_chn_outline);
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
            "FirstPractice",
            "SecondPractice",
            "ThirdPractice",
            "SprintQualifying",
            "Sprint",
            "Qualifying",
            "Race"
    };

    public static final Map<String, String> SESSION_NAMES = Map.of(
            "FirstPractice", "Practice 1",
            "SecondPractice", "Practice 2",
            "ThirdPractice", "Practice 3",
            "Qualifying", "Qualifying",
            "SprintQualifying", "Sprint Qualifying",
            "Sprint", "Sprint",
            "Race", "Race"
    );

    public static final Map<String, Integer> SESSION_DURATION = Map.of(
            "FirstPractice", 60,
            "SecondPractice", 60,
            "ThirdPractice", 60,
            "Qualifying", 60,
            "SprintQualifying", 45,
            "Sprint", 60,
            "Race", 120
    );

    public static final Map<String, Integer> SESSION_FLAG_FIELD = Map.of(
            "FirstPractice", R.id.session_1_flag,
            "SecondPractice", R.id.session_2_flag,
            "ThirdPractice", R.id.session_3_flag,
            "SprintQualifying", R.id.session_2_flag,
            "Sprint", R.id.session_3_flag,
            "Qualifying", R.id.session_4_flag,
            "Race", R.id.session_5_flag
    );

    public static final Map<String, Integer> SESSION_NAME_FIELD = Map.of(
            "FirstPractice", R.id.session_1_name,
            "SecondPractice", R.id.session_2_name,
            "ThirdPractice", R.id.session_3_name,
            "SprintQualifying", R.id.session_2_name,
            "Sprint", R.id.session_3_name,
            "Qualifying", R.id.session_4_name,
            "Race", R.id.session_5_name
    );

    public static final Map<String, String> SESSION_DAY = Map.of(
            "FirstPractice", "Friday",
            "SecondPractice", "Friday",
            "ThirdPractice", "Saturday",
            "SprintQualifying", "Friday",
            "Sprint", "Saturday",
            "Qualifying", "Saturday",
            "Race", "Sunday"
    );

    public static final Map<String, Integer> SESSION_DAY_FIELD = Map.of(
            "FirstPractice", R.id.session_1_day,
            "SecondPractice", R.id.session_2_day,
            "ThirdPractice", R.id.session_3_day,
            "SprintQualifying", R.id.session_2_day,
            "Sprint", R.id.session_3_day,
            "Qualifying", R.id.session_4_day,
            "Race", R.id.session_5_day
    );
    public static final Map<String, Integer> SESSION_TIME_FIELD = Map.of(
            "FirstPractice", R.id.session_1_time,
            "SecondPractice", R.id.session_2_time,
            "ThirdPractice", R.id.session_3_time,
            "SprintQualifying", R.id.session_2_time,
            "Sprint", R.id.session_3_time,
            "Qualifying", R.id.session_4_time,
            "Race", R.id.session_5_time
    );
}
