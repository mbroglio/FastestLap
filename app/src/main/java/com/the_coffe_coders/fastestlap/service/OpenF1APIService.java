package com.the_coffe_coders.fastestlap.service;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Retrofit service interface for the OpenF1 REST API.
 * Base URL: https://api.openf1.org/v1/
 */
public interface OpenF1APIService {

    /**
     * Returns the list of Race Control messages for the latest session.
     */
    @GET("race_control")
    Call<ResponseBody> getRaceControlMessages(@Query("session_key") String sessionKey);

    /**
     * Returns the list of Team Radio recordings for the latest session.
     */
    @GET("team_radio")
    Call<ResponseBody> getTeamRadioMessages(@Query("session_key") String sessionKey);

    /**
     * Returns the list of meetings for a given year (e.g. 2026).
     */
    @GET("meetings")
    Call<ResponseBody> getMeetings(@Query("year") String year);

    /**
     * Returns the list of sessions for a given meeting_key and optional session_name.
     */
    @GET("sessions")
    Call<ResponseBody> getSessions(@Query("meeting_key") int meetingKey, @Query("session_name") String sessionName);

    /**
     * Returns the list of sessions for a given meeting_key.
     */
    @GET("sessions")
    Call<ResponseBody> getSessionsByMeetingKey(@Query("meeting_key") int meetingKey);

    /**
     * Returns the list of tyre stints for a given session_key.
     */
    @GET("stints")
    Call<ResponseBody> getStints(@Query("session_key") int sessionKey);

    /**
     * Returns the list of tyre stints for a string session_key (e.g. "latest").
     */
    @GET("stints")
    Call<ResponseBody> getStintsBySessionKey(@Query("session_key") String sessionKey);

    /**
     * Returns pit stop data for a given session_key.
     */
    @GET("pit")
    Call<ResponseBody> getPitStops(@Query("session_key") int sessionKey);

    /**
     * Returns pit stop data for a string session_key (e.g. "latest").
     */
    @GET("pit")
    Call<ResponseBody> getPitStopsBySessionKey(@Query("session_key") String sessionKey);

    /**
     * Returns weather telemetry data for a given session_key.
     */
    @GET("weather")
    Call<ResponseBody> getWeather(@Query("session_key") int sessionKey);

    /**
     * Returns weather telemetry data for a string session_key.
     */
    @GET("weather")
    Call<ResponseBody> getWeatherBySessionKey(@Query("session_key") String sessionKey);

    /**
     * Returns weather telemetry data for a given meeting_key.
     */
    @GET("weather")
    Call<ResponseBody> getWeatherByMeetingKey(@Query("meeting_key") int meetingKey);
}
