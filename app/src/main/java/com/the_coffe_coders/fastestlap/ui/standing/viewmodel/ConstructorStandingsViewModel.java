package com.the_coffe_coders.fastestlap.ui.standing.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandingsElement;
import com.the_coffe_coders.fastestlap.repository.standings.ConstructorStandingsStandingsRepository;

import java.util.List;

public class ConstructorStandingsViewModel extends ViewModel {

    private static final String TAG = ConstructorStandingsViewModel.class.getSimpleName();

    private final ConstructorStandingsStandingsRepository constructorRepository;
    private final MutableLiveData<Result> constructorStandingsLiveData;
    private final MutableLiveData<String> errorMessageLiveData;
    private final MutableLiveData<Boolean> isLoadingLiveData;
    private final int page;
    private long lastUpdateTimestamp = 0;

    public ConstructorStandingsViewModel(ConstructorStandingsStandingsRepository constructorRepository) {
        this.constructorRepository = constructorRepository;
        this.page = 3; // TODO Verify

        // Initialize LiveData objects
        constructorStandingsLiveData = new MutableLiveData<>();
        errorMessageLiveData = new MutableLiveData<>();
        isLoadingLiveData = new MutableLiveData<>();
    }

    public LiveData<Result> getConstructorStandings() {
        return constructorStandingsLiveData;
    }

    public LiveData<Boolean> isLoading() {
        return isLoadingLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessageLiveData;
    }

    public void fetchConstructorStandings(long lastUpdate) {
        Log.i(TAG, "fetchConstructorStandings");
        isLoadingLiveData.setValue(true);
        errorMessageLiveData.setValue(null);

        constructorRepository.fetchConstructorStandings(lastUpdate)
                .thenAccept(result -> {
                    isLoadingLiveData.postValue(false);
                    if (result instanceof Result.Error) {
                        Log.e(TAG, "Error fetching constructor standings: " + result.getError());
                        errorMessageLiveData.postValue(result.getError());
                    } else if (result instanceof Result.ConstructorStandingsSuccess) {
                        Log.i(TAG, "Successfully fetched constructor standings");
                        lastUpdateTimestamp = System.currentTimeMillis();
                    }
                    constructorStandingsLiveData.postValue(result);
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Exception while fetching constructor standings", throwable);
                    isLoadingLiveData.postValue(false);
                    errorMessageLiveData.postValue("An error occurred: " + throwable.getMessage());
                    return null;
                });
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