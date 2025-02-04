package com.the_coffe_coders.fastestlap.repository.driver;

import androidx.lifecycle.MediatorLiveData;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;

public class CommonDriverRepository {
    private final FirebaseDriverRepository firebaseDriverRepository;
    private final JolpicaDriverRepositoy jolpicaDriverRepository;
    private MediatorLiveData<Result> allDriverMediatorLiveData;

    public CommonDriverRepository() {
        //this.allDriverMediatorLiveData = new MediatorLiveData<>();;
        jolpicaDriverRepository = new JolpicaDriverRepositoy();
        firebaseDriverRepository = new FirebaseDriverRepository();
    }

    public MediatorLiveData<Result> getDriver(String driverId) {
        allDriverMediatorLiveData = new MediatorLiveData<>();
        allDriverMediatorLiveData.addSource(jolpicaDriverRepository.getDriver(driverId), jolpicaResult -> {
            if (jolpicaResult instanceof Result.DriverSuccess) {
                Driver jolpicaDriver = ((Result.DriverSuccess) jolpicaResult).getData();

                firebaseDriverRepository.getDriver(driverId, new FirebaseDriverRepository.DriverCallback() {
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
}
