package com.the_coffe_coders.fastestlap.repository.constructor;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;

import java.util.List;

public interface IConstructorRepository {
    List<Constructor> findConstructors();

    Constructor find(String id);

    MutableLiveData<Result> fetchConstructorStandings(long lastUpdate);
    MutableLiveData<Result> fetchConstructors(long lastUpdate);

    MutableLiveData<Result> fetchConstructor(String constructorId, long lastUpdate);


}
