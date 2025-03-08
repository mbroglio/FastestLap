package com.the_coffe_coders.fastestlap.data.repository.track;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.domain.Result;

public interface ITrackRepository {

    MutableLiveData<Result> getTrack(String trackId);
}
