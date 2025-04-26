package com.the_coffe_coders.fastestlap.source.constructor;

import com.the_coffe_coders.fastestlap.repository.constructor.ConstructorCallback;

public class JolpicaConstructorDataSource implements ConstructorDataSource {

    private static final String TAG = "JolpicaConstructorDataSource";
    private static JolpicaConstructorDataSource instance;

    private JolpicaConstructorDataSource() {
    }

    public static JolpicaConstructorDataSource getInstance() {
        if (instance == null) {
            instance = new JolpicaConstructorDataSource();
        }
        return instance;
    }

    @Override
    public void getConstructor(String constructorId, ConstructorCallback callback) {

    }
}
