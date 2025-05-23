package com.the_coffe_coders.fastestlap.ui.event.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.repository.result.ResultRepository;

public class RaceResultViewModel extends ViewModel {
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final ResultRepository resultRepository;

    public RaceResultViewModel(ResultRepository resultRepository) {
        this.resultRepository = resultRepository;
    }

    public MutableLiveData<Result> getAllRaceResults(int numberOfRaces) {
        return resultRepository.fetchAllRaceResults(numberOfRaces);
    }

    public MutableLiveData<Result> getRaceResults(String raceId) {
        return resultRepository.fetchResults(raceId);
    }
}