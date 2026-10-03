package com.the_coffe_coders.fastestlap.util.weather;

import com.the_coffe_coders.fastestlap.R;

public class WeatherUtils {

    /**
     * Convert wind direction in degrees (0-360) to cardinal direction (N, NE, E, SE, S, SW, W, NW).
     */
    public static String getCardinalDirection(Integer degrees) {
        if (degrees == null) return "N/A";
        int deg = ((degrees % 360) + 360) % 360;
        if (deg >= 337.5 || deg < 22.5) return "N";
        if (deg >= 22.5 && deg < 67.5) return "NE";
        if (deg >= 67.5 && deg < 112.5) return "E";
        if (deg >= 112.5 && deg < 157.5) return "SE";
        if (deg >= 157.5 && deg < 202.5) return "S";
        if (deg >= 202.5 && deg < 247.5) return "SW";
        if (deg >= 247.5 && deg < 292.5) return "W";
        return "NW";
    }

    /**
     * Returns description text for WMO 4677 weather code.
     * Reference: NOAA WMO Code Table 4677 (ww = 00-99).
     *
     * @param code    WMO weather code (0-99).
     * @param isNight True if it is currently nighttime at the circuit location.
     */
    public static String getWeatherText(int code, boolean isNight) {
        switch (code) {
            // 00-19: No precipitation at the station at the time of observation
            case 0:
                return isNight ? "Clear Night" : "Sunny";
            case 1:
                return "Mainly Clear";
            case 2:
                return "Partly Cloudy";
            case 3:
            case 22:
            case 26:
            case 15:
                return "Overcast";
            // 20-29: Precipitation, fog, ice fog or thunderstorm during preceding hour
            case 20:
            case 21:
            case 23:
            case 24:
            case 25:
            case 16:
            case 14:
                return "Scattered Rain";
            case 29:
            case 13:
                return "Scattered Thunderstorm";
            case 18:
            case 19:
                return "Windy";

            // 30-39: Duststorm, sandstorm, drifting or blowing snow
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
                return "Duststorm";
            case 36:
            case 37:
            case 38:
            case 39:
                return "Snow";

            // 40-49: Fog or ice fog at time of observation
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 28:
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47:
            case 48:
            case 49:
                return "Fog";

            case 50:
            case 51:
            case 52:
            case 53:
            case 54:
            case 55:
            case 56:
            case 57:
            case 58:
            case 59:
            case 60:
            case 61:
            case 66:
            case 80:
            case 91:
                return "Light Rain";
            case 62:
            case 63:
            case 81:
            case 92:
                return "Moderate Rain";
            case 64:
            case 65:
            case 67:
            case 82:
                return "Heavy Rain";
            case 68:
            case 69:
            case 83:
            case 84:
                return "Rain and Snow";

            // 70-79: Solid precipitation not in showers
            case 70:
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77:
            case 78:
            case 79:
            case 85:
            case 86:
            case 93:
            case 94:
                return "Snow";

            // 80-99: Showery precipitation, or with thunderstorm
            case 27:
            case 87:
            case 88:
            case 89:
            case 90:
                return "Hail";
            case 17:
            case 95:
            case 96:
            case 98:
                return "Thunderstorm";
            case 97:
            case 99:
                return "Heavy Thunderstorm";
            default:
                return isNight ? "Clear Night" : "Clear";
        }
    }

    /**
     * Returns drawable icon resource ID for WMO 4677 weather code.
     * Reference: NOAA WMO Code Table 4677 (ww = 00-99).
     *
     * @param code    WMO weather code (0-99).
     * @param isNight True if it is currently nighttime at the circuit location.
     */
    public static int getWeatherIconResId(int code, boolean isNight) {
        switch (code) {
            // 00-19: No precipitation at the station at the time of observation
            case 0:
                return isNight ? R.drawable.night_weather_icon : R.drawable.sun_weather_icon;
            case 1:
            case 2:
                return isNight ? R.drawable.night_cloud_weather_icon : R.drawable.partially_cloud_weather_icon;
            case 3:
            case 22:
            case 26:
            case 15:
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
                return R.drawable.cloud_weather_icon;
            // 20-29: Precipitation, fog, ice fog or thunderstorm during preceding hour
            case 20:
            case 21:
            case 23:
            case 24:
            case 25:
            case 16:
            case 14:
                return isNight ? R.drawable.scattered_rain_night_icon : R.drawable.scattered_rain_day_icon;
            case 29:
            case 13:
                return isNight ? R.drawable.scattered_storm_night_icon : R.drawable.scattered_storm_day_icon;
            case 18:
            case 19:
                return R.drawable.wind_icon;
            case 36:
            case 37:
            case 38:
            case 39:
                return R.drawable.snow_icon;
            // 40-49: Fog or ice fog at time of observation
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 28:
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47:
            case 48:
            case 49:
                return isNight ? R.drawable.fog_night : R.drawable.fog_day;
            case 50:
            case 51:
            case 52:
            case 53:
            case 54:
            case 55:
            case 56:
            case 57:
            case 58:
            case 59:
            case 60:
            case 61:
            case 66:
            case 80:
            case 91:
                return R.drawable.light_rain_icon;
            case 62:
            case 63:
            case 81:
            case 92:
                return R.drawable.moderate_rain_icon;
            case 64:
            case 65:
            case 67:
            case 82:
                return R.drawable.heavy_rain_icon;
            case 68:
            case 69:
            case 83:
            case 84:
                return R.drawable.rain_and_snow_icon;

            // 70-79: Solid precipitation not in showers
            case 70:
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77:
            case 78:
            case 79:
            case 85:
            case 86:
            case 93:
            case 94:
                return R.drawable.snow_icon;
            // 80-99: Showery precipitation, or with thunderstorm
            case 27:
            case 87:
            case 88:
            case 89:
            case 90:
                return R.drawable.hail_icon;
            case 17:
            case 95:
            case 96:
            case 98:
                return R.drawable.light_thunderstorm;
            case 97:
            case 99:
                return R.drawable.heavy_thunderstorm;
            default:
                return isNight ? R.drawable.night_weather_icon : R.drawable.sun_weather_icon;
        }
    }

    /**
     * Converts m/s to km/h.
     */
    public static double msToKmh(double ms) {
        return ms * 3.6;
    }
}
