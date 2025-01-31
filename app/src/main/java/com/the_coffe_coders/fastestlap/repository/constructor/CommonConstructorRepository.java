package com.the_coffe_coders.fastestlap.repository.constructor;


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.api.ConstructorStandingsAPIResponse;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;
import com.the_coffe_coders.fastestlap.mapper.ConstructorStandingsMapper;
import com.the_coffe_coders.fastestlap.repository.driver.FirebaseDriverRepository;
import com.the_coffe_coders.fastestlap.repository.driver.JolpicaDriverRepositoy;
import com.the_coffe_coders.fastestlap.source.constructor.BaseConstructorLocalDataSource;
import com.the_coffe_coders.fastestlap.source.constructor.BaseConstructorRemoteDataSource;

import java.util.Collections;
import java.util.List;

public class CommonConstructorRepository implements IConstructorRepository, ConstructorResponseCallback {

    private final MediatorLiveData<Result> allConstructorMediatorLiveData;

    private final FirebaseConstructorRepository firebaseConstructorRepository;
    private final JolpicaConstructorRepository jolpicaConstructorRepository;
    public CommonConstructorRepository() {
        this.allConstructorMediatorLiveData = new MediatorLiveData<>();
        jolpicaConstructorRepository = new JolpicaConstructorRepository();
        firebaseConstructorRepository = new FirebaseConstructorRepository();
    }

    /*public MediatorLiveData<Result> getDriver(String driverId) {
        allDriverMediatorLiveData.addSource(jolpicaDriverRepository.getDriver(driverId), allDriverMediatorLiveData::setValue);
        firebaseDriverRepository.getDriverData(driverId,
                new FirebaseDriverRepository.DriverCallback() {

                    @Override
                    public void onSuccess(Driver driver) {
                        allDriverMediatorLiveData.setValue(new Result.DriverSuccess(driver));

                    }

                    @Override
                    public void onFailure(Exception exception) {
                        allDriverMediatorLiveData.setValue(new Result.Error(exception.getMessage()));
                    }
                });

        return allDriverMediatorLiveData;
    }*/

    public MediatorLiveData<Result> getDriver(String driverId) {
        allDriverMediatorLiveData.addSource(jolpicaDriverRepository.getDriver(driverId), jolpicaResult -> {
            if (jolpicaResult instanceof Result.DriverSuccess) {
                Driver jolpicaDriver = ((Result.DriverSuccess) jolpicaResult).getData();

                firebaseDriverRepository.getDriverData(driverId, new FirebaseDriverRepository.DriverCallback() {
                    @Override
                    public void onSuccess(Driver firebaseDriver) {
                        firebaseDriver.setDriverId(jolpicaDriver.getDriverId());
                        firebaseDriver.setPermanentNumber(jolpicaDriver.getPermanentNumber());
                        firebaseDriver.setCode(jolpicaDriver.getCode());
                        firebaseDriver.setUrl(jolpicaDriver.getUrl());
                        allDriverMediatorLiveData.setValue(new Result.DriverSuccess(firebaseDriver));
                    }

                    @Override
                    public void onFailure(Exception exception) {
                        // Se Firebase fallisce, usa i dati di Jolpica
                        allDriverMediatorLiveData.setValue(jolpicaResult);
                    }
                });
            }
        });

        return allDriverMediatorLiveData;
    }

    @Override
    public void onSuccessFromRemote(ConstructorStandingsAPIResponse constructorStandingsAPIResponse, long lastUpdate) {

    }

    @Override
    public void onFailureFromRemote(Exception e) {

    }

    @Override
    public void onSuccessFromLocal(ConstructorStandings constructorStandings) {

    }

    @Override
    public void onFailureFromLocal(Exception e) {

    }

    @Override
    public List<Constructor> findConstructors() {
        return Collections.emptyList();
    }

    @Override
    public Constructor find(String id) {
        return null;
    }

    @Override
    public MutableLiveData<Result> fetchConstructorStandings(long lastUpdate) {
        return null;
    }

    @Override
    public MutableLiveData<Result> fetchConstructors(long lastUpdate) {
        return null;
    }

    @Override
    public MutableLiveData<Result> fetchConstructor(String driverId, long lastUpdate) {
        return null;
    }
}