package com.the_coffe_coders.fastestlap.utils;

import com.the_coffe_coders.fastestlap.R;

import java.util.HashMap;
import java.util.Map;

public class Constants {
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
            put("colapinto", R.string.oliver_bearman);
            put("lawson", R.string.liam_lawson);
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
        put("rb", R.drawable.racingbullslogo); // Red Bull Racing
    }};

    public static final Map<String, Integer> TEAM_COLOR = new HashMap<String, Integer>() {{
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
        put("rb", R.string.red_bull);
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
}
