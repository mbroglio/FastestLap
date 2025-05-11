package com.the_coffe_coders.fastestlap.ui.standing.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandingsElement;
import com.the_coffe_coders.fastestlap.repository.standing.constructor.ConstructorStandingRepository;

public class ConstructorStandingsViewModel extends ViewModel {
    private final MutableLiveData<ConstructorStandingsElement> selectedConstructorLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final ConstructorStandingRepository constructorStandingRepository;

    public ConstructorStandingsViewModel(ConstructorStandingRepository constructorStandingRepository) {
        this.constructorStandingRepository = constructorStandingRepository;
    }

    public MutableLiveData<Result> getConstructorStandings() {
        return constructorStandingRepository.getConstructorStandings();
    }
}