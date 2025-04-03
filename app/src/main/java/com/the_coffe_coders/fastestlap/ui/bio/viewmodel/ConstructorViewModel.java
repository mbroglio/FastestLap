package com.the_coffe_coders.fastestlap.ui.bio.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.repository.constructor.ConstructorRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConstructorViewModel extends ViewModel {
    ConstructorRepository constructorRepository;
    private final MutableLiveData<Constructor> selectedConstructorLiveData = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public ConstructorViewModel(ConstructorRepository constructorRepository) {
        this.constructorRepository = constructorRepository;
    }

    public MutableLiveData<Result> getSelectedConstructor(String constructorId) {
        return constructorRepository.getConstructor(constructorId);
    }

}
