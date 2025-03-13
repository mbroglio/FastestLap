package com.the_coffe_coders.fastestlap.ui.bio.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.repository.constructor.CommonConstructorRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConstructorViewModel extends ViewModel {
    private final CommonConstructorRepository constructorRepository;
    private final MutableLiveData<Constructor> selectedConstructorLiveData = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public ConstructorViewModel(CommonConstructorRepository constructorRepository) {
        this.constructorRepository = constructorRepository;
    }

    /**
     * Gets constructor data as LiveData, converting the CompletableFuture result to LiveData
     *
     * @param teamId The ID of the constructor/team to fetch
     * @return MutableLiveData that will be updated with the constructor result
     */
    public MutableLiveData<Result> getSelectedConstructorLiveData(String teamId) {
        MutableLiveData<Result> constructorLiveData = new MutableLiveData<>();

        // Set initial loading state
        //constructorLiveData.setValue(new Result.Loading());

        // Call the repository to get the CompletableFuture
        constructorRepository.getConstructor(teamId)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        // Handle any exceptions from the future
                        constructorLiveData.postValue(new Result.Error("Error fetching constructor: " +
                                throwable.getMessage()));
                    } else {
                        // Post the result to the LiveData
                        constructorLiveData.postValue(result);
                    }
                });

        return constructorLiveData;
    }


}
