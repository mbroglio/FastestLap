package com.the_coffe_coders.fastestlap.ui.standing.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.repository.standing.constructor.ConstructorStandingRepository;

public class ConstructorStandingsViewModel extends ViewModel {

    private static final String TAG = ConstructorStandingsViewModel.class.getSimpleName();

    public ConstructorStandingsViewModel() {
    }

    public LiveData<Result> getConstructorStandings() {
        return ConstructorStandingRepository.getInstance().fetchConstructorStanding();
    }
}