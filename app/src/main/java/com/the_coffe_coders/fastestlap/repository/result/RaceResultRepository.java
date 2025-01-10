package com.the_coffe_coders.fastestlap.repository.result;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.api.RaceResultsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.RaceResult;
import com.the_coffe_coders.fastestlap.dto.ResultDTO;
import com.the_coffe_coders.fastestlap.mapper.SessionMapper;
import com.the_coffe_coders.fastestlap.source.result.BaseRaceResultLocalDataSource;
import com.the_coffe_coders.fastestlap.source.result.BaseRaceResultRemoteDataSource;

import java.util.ArrayList;
import java.util.List;

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
        this.raceResultLocalDataSource.setRaceResultCallback(this);
    }

    @Override
    public MutableLiveData<Result> fetchRaceResults(int round, long lastUpdate) {
        Log.i(TAG, "fetchRaceResults");
        if(isOutdate) { //TODO change in currentTime - lastUpdate > FRESH_TIMEOUT)
            //TODO fetch from remote
            Log.i(TAG, "Remote fetchRaceResults");
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
        Log.i(TAG, "onSuccessFromRemote");
        Log.i(TAG, raceResultsAPIResponse.toString());
        List<RaceResult> raceResult = new ArrayList<>();
        for(ResultDTO resultDTO: raceResultsAPIResponse.getRaceResults()) {
            raceResult.add(SessionMapper.toResult(resultDTO));
        }
        raceResultLocalDataSource.insertRaceResultList(raceResult);
    }

    @Override
    public void onFailureFromRemote(Exception exception) {

    }

    @Override
    public void onSuccessFromLocal(RaceResult raceResult) {

    }

    @Override
    public void onSuccessFromLocal(List<RaceResult> raceResultList) {
        Log.i(TAG, "onSuccessFromLocal");
        new Handler(Looper.getMainLooper()).post(() -> {
            raceResultMutableLiveData.setValue(new Result.RaceResultSuccess(raceResultList));
        });
        Log.i(TAG, raceResultMutableLiveData.toString());
    }

    @Override
    public void onFailureFromLocal(Exception exception) {

    }
}
