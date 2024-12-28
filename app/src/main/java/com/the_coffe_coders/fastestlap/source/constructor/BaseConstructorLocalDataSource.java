package com.the_coffe_coders.fastestlap.source.constructor;

import com.the_coffe_coders.fastestlap.domain.constructor.Constructor;
import com.the_coffe_coders.fastestlap.domain.driver.Driver;
import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;
import com.the_coffe_coders.fastestlap.repository.constructor.ConstructorResponseCallback;

import java.util.List;

public abstract class BaseConstructorLocalDataSource {
    protected ConstructorResponseCallback constructorCallback;

    public void setConstructorCallback(ConstructorResponseCallback constructorCallback) {
        this.constructorCallback = constructorCallback;
    }

    public abstract void insertConstructors(List<Constructor> constructorsList);
    public abstract void getConstructor();

    public abstract void insertConstructorsStandings(ConstructorStandings constructorStandings);
    public abstract void getConstructorStandings();
}