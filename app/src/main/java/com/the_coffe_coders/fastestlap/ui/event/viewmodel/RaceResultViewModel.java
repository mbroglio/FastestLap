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
     * @param location località dell'evento (es. "Melbourne", "Monza")
     * @param eventName nome dell'evento (es. "Australian Grand Prix")
     * @param sessionName nome della sessione (es. "Race", "Sprint")
     * @return LiveData che emette Result.Loading -> Result.StintsSuccess o Result.Error
     */
    public MutableLiveData<Result> getStints(String location, String eventName, String sessionName) {
        return getStints(location, eventName, sessionName, null);
    }

    /**
     * Recupera la lista degli stint degli pneumatici per un evento e una sessione specifici,
     * includendo anche il nome ufficiale lungo del GP per massimizzare la precisione di matching con OpenF1.
     *
     * @param location località dell'evento (es. "Melbourne", "Monza")
     * @param eventName nome dell'evento (es. "Australian Grand Prix")
     * @param sessionName nome della sessione (es. "Race", "Sprint")
     * @param gpLongName nome ufficiale lungo del GP (es. "FORMULA 1 ROLEX AUSTRALIAN GRAND PRIX 2026")
     * @return LiveData che emette Result.Loading -> Result.StintsSuccess o Result.Error
     */
    public MutableLiveData<Result> getStints(String location, String eventName, String sessionName, String gpLongName) {
        Log.i("RaceResultViewModel", "Fetching stints for event: " + eventName + ", location: " + location + ", session: " + sessionName + ", gpLongName: " + gpLongName);
        return resultRepository.fetchStints(location, eventName, sessionName, gpLongName);
    }
}