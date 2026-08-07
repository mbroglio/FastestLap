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
     * Returns description text for WMO weather code.
     *
     * @param code      WMO weather code from Open-Meteo API.
     * @param isNight   True if it is currently nighttime at the circuit location.
     */
    public static String getWeatherText(int code, boolean isNight) {
        switch (code) {
            case 0:
                return isNight ? "Clear Night" : "Sunny";
            case 1:
            case 2:
                return "Partly Cloudy";
            case 3:
                return "Overcast";
            case 45:
            case 48:
                return "Foggy";
            case 51:
            case 53:
            case 55:
            case 61:
            case 63:
            case 80:
            case 81:
                return "Rainy";
            case 65:
            case 82:
                return "Heavy Rain";
            case 95:
            case 96:
            case 99:
                return "Thunderstorm";
            default:
                return isNight ? "Clear Night" : "Clear";
        }
    }

    /**
     * Returns drawable icon resource ID for weather code.
     *
     * @param code      WMO weather code from Open-Meteo API.
     * @param isNight   True if it is currently nighttime at the circuit location.
     */
    public static int getWeatherIconResId(int code, boolean isNight) {
        switch (code) {
            case 0:
                // Clear sky
                return isNight ? R.drawable.night_weather_icon : R.drawable.sun_weather_icon;
            case 1:
            case 2:
                // Mainly clear / partly cloudy
                return isNight ? R.drawable.night_cloud_weather_icon : R.drawable.partially_cloud_weather_icon;
            case 3:
            case 45:
            case 48:
                // Overcast / foggy
                return R.drawable.cloud_weather_icon;
            case 51:
            case 53:
            case 55:
            case 61:
            case 63:
            case 80:
            case 81:
                // Light/moderate rain
                return isNight ? R.drawable.night_rain_weather_icon : R.drawable.rain_weather_icon;
            case 65:
            case 82:
                // Heavy rain
                return R.drawable.heavy_rain_weather_icon;
            case 95:
            case 96:
            case 99:
                // Thunderstorm
                return R.drawable.storm_weather_icon;
            default:
                return isNight ? R.drawable.night_weather_icon : R.drawable.sun_weather_icon;
        }
    }

    /**
     * Returns raw video resource ID for weather background.
     *
     * @param code      WMO weather code from Open-Meteo API.
     * @param isNight   True if it is currently nighttime at the circuit location.
     */
    public static int getWeatherVideoResId(int code, boolean isNight) {
        switch (code) {
            case 0:
                // Clear sky
                return isNight ? R.raw.night_weather_video : R.raw.sun_weather_video;
            case 1:
            case 2:
                // Mainly clear / partly cloudy
                return isNight ? R.raw.night_cloud_weather_video : R.raw.partially_cloud_weather_video;
            case 3:
            case 45:
            case 48:
                // Overcast / foggy — no dedicated night video, use cloud
                return R.raw.cloud_weather_video;
            case 51:
            case 53:
            case 55:
            case 61:
            case 63:
            case 80:
            case 81:
                return R.raw.light_rain_weather_video;
            case 65:
            case 82:
            case 95:
            case 96:
            case 99:
                return R.raw.heavy_rain_weather_video;
            default:
                return isNight ? R.raw.night_weather_video : R.raw.sun_weather_video;
        }
    }

    /**
     * Converts m/s to km/h.
     */
    public static double msToKmh(double ms) {
        return ms * 3.6;
    }
}
