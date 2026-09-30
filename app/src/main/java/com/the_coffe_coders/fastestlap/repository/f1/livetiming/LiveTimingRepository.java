package com.the_coffe_coders.fastestlap.repository.f1.livetiming;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.livetiming.RaceControlMessage;
import com.the_coffe_coders.fastestlap.domain.f1.livetiming.TeamRadioMessage;
import com.the_coffe_coders.fastestlap.source.f1.livetiming.OpenF1LiveTimingDataSource;
import com.the_coffe_coders.fastestlap.util.service.NetworkUtils;

import java.util.List;

/**
 * Repository for live-timing data fetched from the OpenF1 API.
 *
 * <p>This class follows the same structural pattern used by
 * {@code WeeklyRaceRepository} for the Ergast/Jolpica data:
 * <ul>
 *   <li>It exposes {@link LiveData}&lt;{@link Result}&gt; streams that the UI layer
 *       observes via ViewModels.</li>
 *   <li>It delegates network calls to {@link OpenF1LiveTimingDataSource} and posts
 *       the appropriate {@link Result} subtype ({@code Loading}, success, or
 *       {@code Error}) back on the LiveData.</li>
 *   <li>It is a singleton, created once and reused across the app lifecycle.</li>
 *   <li>It respects network availability via {@link NetworkUtils}: if the device is
 *       offline an {@code Error} result is posted immediately instead of
 *       attempting a doomed network call.</li>
 * </ul>
 *
 * <p><b>No local caching</b> is implemented here because live-timing data is
 * inherently ephemeral and must always be fetched fresh from the remote API.
 * The caller is responsible for refreshing on a suitable polling interval.
 * </p>
 */
public class LiveTimingRepository {

    private static final String TAG = "LiveTimingRepository";

    private static LiveTimingRepository instance;

    private final MutableLiveData<Result> raceControlLiveData;
    private final MutableLiveData<Result> teamRadioLiveData;

    private final OpenF1LiveTimingDataSource remoteDataSource;
    private final NetworkUtils networkUtils;

    // ─────────────────────────────────────────────────────────────
    // Singleton
    // ─────────────────────────────────────────────────────────────

    private LiveTimingRepository(Context context) {
        this.raceControlLiveData = new MutableLiveData<>();
        this.teamRadioLiveData   = new MutableLiveData<>();
        this.remoteDataSource    = new OpenF1LiveTimingDataSource();
        this.networkUtils        = new NetworkUtils(context);
    }

    public static synchronized LiveTimingRepository getInstance(Context context) {
        if (instance == null) {
            instance = new LiveTimingRepository(context.getApplicationContext());
        }
        return instance;
    }

    // ─────────────────────────────────────────────────────────────
    // Public API – observed by ViewModels
    // ─────────────────────────────────────────────────────────────

    /**
     * Returns a {@link LiveData} stream that emits the full list of
     * {@link RaceControlMessage} objects for the latest session.
     *
     * <p>Immediately posts a {@link Result.Loading} value and then kicks off the
     * remote fetch. The caller should call this method every time it wants a
     * fresh snapshot (e.g. on a polling timer).</p>
     *
     * @return a LiveData delivering {@link Result.RaceControlSuccess} on success or
     *         {@link Result.Error} on failure
     */
    public synchronized LiveData<Result> fetchRaceControlMessages() {
        Log.d(TAG, "Fetching race control messages");
        if (networkUtils.isConnected()) {
            loadRaceControlFromRemote();
        } else {
            Log.w(TAG, "No network connection – cannot fetch race control messages");
            raceControlLiveData.postValue(new Result.Error("No network connection"));
        }
        return raceControlLiveData;
    }

    /**
     * Returns a {@link LiveData} stream that emits the full list of
     * {@link TeamRadioMessage} objects for the latest session.
     *
     * <p>Immediately posts a {@link Result.Loading} value and then kicks off the
     * remote fetch. The caller should call this method every time it wants a
     * fresh snapshot (e.g. on a polling timer).</p>
     *
     * @return a LiveData delivering {@link Result.TeamRadioSuccess} on success or
     *         {@link Result.Error} on failure
     */
    public synchronized LiveData<Result> fetchTeamRadioMessages() {
        Log.d(TAG, "Fetching team radio messages");
        if (networkUtils.isConnected()) {
            loadTeamRadioFromRemote();
        } else {
            Log.w(TAG, "No network connection – cannot fetch team radio messages");
            teamRadioLiveData.postValue(new Result.Error("No network connection"));
        }
        return teamRadioLiveData;
    }

    // ─────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────

    private void loadRaceControlFromRemote() {
        raceControlLiveData.postValue(new Result.Loading("Loading race control messages"));

        remoteDataSource.getRaceControlMessages(new RaceControlCallback() {
            @Override
            public void onSuccess(List<RaceControlMessage> messages) {
                Log.d(TAG, "Race control messages loaded: " + messages.size());
                raceControlLiveData.postValue(new Result.RaceControlSuccess(messages));
            }

            @Override
            public void onFailure(Exception exception) {
                Log.e(TAG, "Error fetching race control messages: " + exception.getMessage());
                raceControlLiveData.postValue(new Result.Error(exception.getMessage()));
            }
        });
    }

    private void loadTeamRadioFromRemote() {
        teamRadioLiveData.postValue(new Result.Loading("Loading team radio messages"));

        remoteDataSource.getTeamRadioMessages(new TeamRadioCallback() {
            @Override
            public void onSuccess(List<TeamRadioMessage> messages) {
                Log.d(TAG, "Team radio messages loaded: " + messages.size());
                teamRadioLiveData.postValue(new Result.TeamRadioSuccess(messages));
            }

            @Override
            public void onFailure(Exception exception) {
                Log.e(TAG, "Error fetching team radio messages: " + exception.getMessage());
                teamRadioLiveData.postValue(new Result.Error(exception.getMessage()));
            }
        });
    }
}
