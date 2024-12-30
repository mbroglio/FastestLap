package com.the_coffe_coders.fastestlap.ui.standing.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.repository.constructor.ConstructorRepository;

public class ConstructorStandingsViewModel extends ViewModel {

    private static final String TAG = ConstructorStandingsViewModel.class.getSimpleName();

    private final ConstructorRepository constructorRepository;
    private final int page;
    private MutableLiveData<Result> constructorStandingsLiveData;

    public ConstructorStandingsViewModel(ConstructorRepository constructorRepository) {
        this.constructorRepository = constructorRepository;
        this.page = 3;//TODO Verify
    }

    private void fetchConstructorStandings(long lastUpdate) {
        constructorStandingsLiveData = constructorRepository.fetchConstructorStandings(lastUpdate);
    }

    public MutableLiveData<Result> getConstructorStandingsLiveData(long lastUpdate) {
        if (constructorStandingsLiveData != null) {
            fetchConstructorStandings(lastUpdate);
        }
        return constructorStandingsLiveData;
    }


}
