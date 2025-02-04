package com.the_coffe_coders.fastestlap.repository.constructor;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.domain.Result;

public interface IConstructorRepository {
    MutableLiveData<Result> fetchConstructorStandings(long lastUpdate);
}
