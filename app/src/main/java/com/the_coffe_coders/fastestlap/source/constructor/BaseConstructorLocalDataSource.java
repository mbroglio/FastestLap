package com.the_coffe_coders.fastestlap.source.constructor;

import com.the_coffe_coders.fastestlap.domain.grand_prix.ConstructorStandings;
import com.the_coffe_coders.fastestlap.repository.constructor.ConstructorResponseCallback;

public abstract class BaseConstructorLocalDataSource {
    protected ConstructorResponseCallback constructorCallback;

    public void setConstructorCallback(ConstructorResponseCallback constructorCallback) {
        this.constructorCallback = constructorCallback;
    }

    public abstract void insertConstructorsStandings(ConstructorStandings constructorStandings);

    public abstract void getConstructorStandings();
}