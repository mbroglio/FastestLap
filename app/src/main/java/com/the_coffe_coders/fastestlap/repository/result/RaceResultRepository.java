package com.the_coffe_coders.fastestlap.repository.result;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.api.RaceResultsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.source.result.BaseRaceResultLocalDataSource;
import com.the_coffe_coders.fastestlap.source.result.BaseRaceResultRemoteDataSource;

public class RaceResultRepository implements IRaceResultRepository, RaceResultResponseCallback {

    private static final String TAG = "RaceResultRepository";

    private final MutableLiveData<Result> raceResultMutableLiveData;

    private final BaseRaceResultRemoteDataSource raceResultRemoteDataSource;
    private final BaseRaceResultLocalDataSource raceResultLocalDataSource;

    public static boolean isOutdate = true;


    public RaceResultRepository(BaseRaceResultRemoteDataSource raceResultRemoteDataSource, BaseRaceResultLocalDataSource raceResultLocalDataSource) {
        this.raceResultMutableLiveData = new MutableLiveData<>();
        this.raceResultRemoteDataSource = raceResultRemoteDataSource;
        this.raceResultLocalDataSource = raceResultLocalDataSource;
        this.raceResultRemoteDataSource.setRaceResultCallback(this);
    }

    @Override
    public MutableLiveData<Result> fetchRaceResults(int round, long lastUpdate) {
        if(isOutdate) { //TODO change in currentTime - lastUpdate > FRESH_TIMEOUT)
            //TODO fetch from remote

            raceResultRemoteDataSource.getRaceResults(round);
            isOutdate = false;
        }else {
            Log.i(TAG, "fetchRaceResults from local");
            raceResultLocalDataSource.getRaceResults();
        }
        return raceResultMutableLiveData;
    }

    @Override
    public MutableLiveData<Result> fetchRaceResult(String resultId, long lastUpdate) {
        return null;
    }


    @Override
    public void onSuccessFromRemote(RaceResultsAPIResponse raceResultsAPIResponse) {
    }

    @Override
    public void onFailureFromRemote(Exception exception) {

    }

    @Override
    public void onFailureFromLocal(Exception exception) {

    }
}
