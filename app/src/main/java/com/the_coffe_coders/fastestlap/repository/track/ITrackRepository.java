package com.the_coffe_coders.fastestlap.repository.track;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Track;

public interface ITrackRepository {

    public MutableLiveData<Result> getTrack(String trackId);
}
