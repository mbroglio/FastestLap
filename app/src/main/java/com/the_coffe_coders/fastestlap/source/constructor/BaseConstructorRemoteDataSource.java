package com.the_coffe_coders.fastestlap.source.constructor;

import com.the_coffe_coders.fastestlap.repository.constructor.ConstructorResponseCallback;

public abstract class BaseConstructorRemoteDataSource {
    protected ConstructorResponseCallback constructorCallback;

    public void setConstructorCallback(ConstructorResponseCallback constructorCallback) {
        this.constructorCallback = constructorCallback;
    }

    public abstract void getConstructorStandings();
}
