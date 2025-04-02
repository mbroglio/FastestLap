package com.the_coffe_coders.fastestlap.repository.weeklyrace;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.WeeklyRace;
import com.the_coffe_coders.fastestlap.source.weeklyrace.BaseWeeklyRaceRemoteDataSource;
import com.the_coffe_coders.fastestlap.source.weeklyrace.WeeklyRaceRemoteDataSource;

import java.util.List;

public class WeeklyRaceRepository {
    private static final String TAG = "WeeklyRaceRepository";
    private static WeeklyRaceRepository instance;
    private final MutableLiveData<Result> nextRaceMutableLiveData;
    private Long nextRaceTime;
    private final MutableLiveData<Result> lastRaceMutableLiveData;
    private Long lastRaceTime;
    private final MutableLiveData<Result> weeklyRacesMutableLiveData;
    private Long weeklyRacesTime;

    private final BaseWeeklyRaceRemoteDataSource weeklyRaceRemoteDataSource;

    private final long FRESH_TIMEOUT = 60000;

    private WeeklyRaceRepository() {
        this.nextRaceMutableLiveData = new MutableLiveData<>();
        this.nextRaceTime = FRESH_TIMEOUT;
        this.lastRaceMutableLiveData = new MutableLiveData<>();
        this.lastRaceTime = FRESH_TIMEOUT;
        this.weeklyRacesMutableLiveData = new MutableLiveData<>();
        this.weeklyRacesTime = FRESH_TIMEOUT;
        this.weeklyRaceRemoteDataSource = new WeeklyRaceRemoteDataSource();
    }

    public static WeeklyRaceRepository getInstance() {
        if (instance == null) {
            instance = new WeeklyRaceRepository();
        }
        return instance;
    }

    public synchronized MutableLiveData<Result> fetchNextWeeklyRace() {
        if(System.currentTimeMillis() - FRESH_TIMEOUT > nextRaceTime) {
            nextRaceMutableLiveData.postValue(new Result.Loading("Loading next race"));
            loadNextRace();
        }
        return nextRaceMutableLiveData;
    }

    private void loadNextRace() {
        nextRaceMutableLiveData.postValue(new Result.Loading("Loading next race"));
        try {
            weeklyRaceRemoteDataSource.getNextRace(new SingleWeeklyRaceCallback() {

                @Override
                public void onSuccess(WeeklyRace weeklyRace) {
                    nextRaceTime = System.currentTimeMillis();
                    nextRaceMutableLiveData.postValue(new Result.NextRaceSuccess(weeklyRace));
                }

                @Override
                public void onFailure(Exception exception) {
                    nextRaceMutableLiveData.postValue(new Result.Error(exception.getMessage()));
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized MutableLiveData<Result> fetchLastWeeklyRace() {

        if(System.currentTimeMillis() - FRESH_TIMEOUT > lastRaceTime) {
            lastRaceMutableLiveData.postValue(new Result.Loading("Loading last race"));
            loadLastRace();
        }
        return lastRaceMutableLiveData;
    }

    private void loadLastRace() {
        lastRaceMutableLiveData.postValue(new Result.Loading("Loading last race"));
        weeklyRaceRemoteDataSource.getLastRace(new SingleWeeklyRaceCallback() {
            @Override
            public void onSuccess(WeeklyRace weeklyRace) {
                lastRaceTime = System.currentTimeMillis();
                lastRaceMutableLiveData.postValue(new Result.NextRaceSuccess(weeklyRace));
            }

            @Override
            public void onFailure(Exception exception) {
                lastRaceMutableLiveData.postValue(new Result.Error(exception.getMessage()));
            }
        });
    }

    public synchronized MutableLiveData<Result> fetchWeeklyRaces() {

        if(System.currentTimeMillis() - FRESH_TIMEOUT > weeklyRacesTime) {
            weeklyRacesMutableLiveData.postValue(new Result.Loading("Loading weekly races"));
            loadWeeklyRaces();
        }
        return weeklyRacesMutableLiveData;
    }

    private void loadWeeklyRaces() {
        weeklyRacesMutableLiveData.postValue(new Result.Loading("Loading weekly races"));
        weeklyRaceRemoteDataSource.getWeeklyRaces(new WeeklyRacesCallback() {

            @Override
            public void onSuccess(List<WeeklyRace> weeklyRaces) {
                weeklyRacesTime = System.currentTimeMillis();
                weeklyRacesMutableLiveData.postValue(new Result.WeeklyRaceSuccess(weeklyRaces));
            }

            @Override
            public void onFailure(Exception exception) {
                weeklyRacesMutableLiveData.postValue(new Result.Error(exception.getMessage()));
            }
        });
    }
}
