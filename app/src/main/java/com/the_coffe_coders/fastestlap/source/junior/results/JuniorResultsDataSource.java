package com.the_coffe_coders.fastestlap.source.junior.results;

import com.the_coffe_coders.fastestlap.repository.junior.result.JuniorResultCallback;

public interface JuniorResultsDataSource {

    void getJuniorResults(String series, JuniorResultCallback callback);

}
