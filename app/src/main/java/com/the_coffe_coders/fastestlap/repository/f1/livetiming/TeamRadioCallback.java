package com.the_coffe_coders.fastestlap.repository.f1.livetiming;

import com.the_coffe_coders.fastestlap.domain.f1.livetiming.TeamRadioMessage;

import java.util.List;

/**
 * Callback interface used by the Live Timing data layer to return
 * a list of {@link TeamRadioMessage} objects (or an error) to its callers.
 */
public interface TeamRadioCallback {

    /**
     * Called when the Team Radio messages have been successfully fetched and parsed.
     *
     * @param messages the non-null, possibly empty list of team-radio recordings
     *                 sorted chronologically (oldest first, as returned by the API)
     */
    void onSuccess(List<TeamRadioMessage> messages);

    /**
     * Called when the fetch or parsing operation failed.
     *
     * @param exception the exception that caused the failure
     */
    void onFailure(Exception exception);
}
