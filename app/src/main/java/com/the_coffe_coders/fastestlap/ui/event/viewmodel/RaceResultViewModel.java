package com.the_coffe_coders.fastestlap.ui.event.viewmodel;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.repository.f1.result.ResultRepository;

public class RaceResultViewModel extends ViewModel {
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final ResultRepository resultRepository;

    public RaceResultViewModel(ResultRepository resultRepository) {
        this.resultRepository = resultRepository;
    }

    public MutableLiveData<Result> getRaceResults(String raceId) {
        return resultRepository.fetchResults(raceId);
    }

    public MutableLiveData<Result> getQualifyingResults(String raceId) {
        return resultRepository.fetchQualifyingResults(raceId);
    }

    public MutableLiveData<Result> getSprintResults(String round) {
        return resultRepository.fetchSprintResults(round);
    }

    /**
     * Recupera la lista degli stint degli pneumatici per un evento e una sessione specifici.
     *
     * @param eventName nome dell'evento (es. visualizzato nella topBar di EventActivity)
     * @param sessionName nome della sessione (es. "Race", "Qualifying", "Sprint")
     * @return LiveData che emette Result.Loading -> Result.StintsSuccess o Result.Error
     */
    public MutableLiveData<Result> getStints(String eventName, String sessionName) {
        Log.i("RaceResultViewModel", "Fetching stints for event: " + eventName + ", session: " + sessionName);
        return resultRepository.fetchStints(eventName, sessionName);
    }
}