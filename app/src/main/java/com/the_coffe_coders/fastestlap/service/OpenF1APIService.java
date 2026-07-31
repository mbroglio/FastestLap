package com.the_coffe_coders.fastestlap.service;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Retrofit service interface for the OpenF1 REST API.
 * Base URL: https://api.openf1.org/v1/
 *
 * The {@code session_key} parameter is always set to {@code "latest"} so that
 * the endpoints automatically return data for the session that is currently
 * in progress (or the most recent completed session when no session is live).
 */
public interface OpenF1APIService {

    /**
     * Returns the list of Race Control messages for the latest session.
     * Each item includes: meeting_key, session_key, date, driver_number,
     * lap_number, category, flag, scope, sector, qualifying_phase, message.
     */
    @GET("race_control")
    Call<ResponseBody> getRaceControlMessages(@Query("session_key") String sessionKey);

    /**
     * Returns the list of Team Radio recordings for the latest session.
     * Each item includes: meeting_key, session_key, driver_number, date, recording_url.
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
}
