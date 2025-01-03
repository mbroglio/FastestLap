package com.the_coffe_coders.fastestlap.ui.standing.viewmodel;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandingsElement;
import com.the_coffe_coders.fastestlap.repository.constructor.ConstructorRepository;

import java.util.List;

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
        Log.i(TAG, "fetchConstructorStandings");
        constructorStandingsLiveData = constructorRepository.fetchConstructorStandings(lastUpdate);
    }

    public MutableLiveData<Result> getConstructorStandingsLiveData(long lastUpdate) {
        if (constructorStandingsLiveData != null) {
            Log.i(TAG, "getConstructorStandingsLiveData");
            fetchConstructorStandings(lastUpdate);
        }
        return constructorStandingsLiveData;
    }

    public ConstructorStandingsElement getConstructorStandingsElement(List<ConstructorStandingsElement> constructorsList, String constructorID) {
        for (ConstructorStandingsElement constructor : constructorsList) {
            if (constructor.getConstructor().getConstructorId().equals(constructorID)) {
                return constructor;
            }
        }

        return null;
    }
}
