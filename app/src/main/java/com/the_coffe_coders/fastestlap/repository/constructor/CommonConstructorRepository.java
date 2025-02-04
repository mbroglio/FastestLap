package com.the_coffe_coders.fastestlap.repository.constructor;


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.api.ConstructorStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;

import java.util.Collections;
import java.util.List;

public class CommonConstructorRepository {
    private final String TAG = "CommonConstructorRepository";
    private final MediatorLiveData<Result> allConstructorMediatorLiveData;

    private final FirebaseConstructorRepository firebaseConstructorRepository;
    private final JolpicaConstructorRepository jolpicaConstructorRepository;
    public CommonConstructorRepository() {
        this.allConstructorMediatorLiveData = new MediatorLiveData<>();
        jolpicaConstructorRepository = new JolpicaConstructorRepository();
        firebaseConstructorRepository = new FirebaseConstructorRepository();;
    }

    public MediatorLiveData<Result> getConstructor(String constructorId) {
        allConstructorMediatorLiveData.addSource(jolpicaConstructorRepository.getConstructor(constructorId), jolpicaResult -> {
            if (jolpicaResult instanceof Result.ConstructorSuccess) {
                Constructor jolpicaConstructor = ((Result.ConstructorSuccess) jolpicaResult).getData();

                firebaseConstructorRepository.getConstructorData(constructorId, new FirebaseConstructorRepository.ConstructorCallback() {
                    @Override
                    public void onSuccess(Constructor firebaseConstructor) {
                        firebaseConstructor.setConstructorId(jolpicaConstructor.getConstructorId());
                        firebaseConstructor.setUrl(jolpicaConstructor.getUrl());
                        allConstructorMediatorLiveData.setValue(new Result.ConstructorSuccess(firebaseConstructor));
                    }

                    @Override
                    public void onFailure(Exception exception) {
                        // Se Firebase fallisce, usa i dati di Jolpica
                        Log.i(TAG, "FIREBASE FAILURE => " + exception.getMessage());
                        allConstructorMediatorLiveData.setValue(jolpicaResult);
                    }
                });
            }
        });

        return allConstructorMediatorLiveData;
    }
}