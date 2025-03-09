package com.the_coffe_coders.fastestlap.ui.bio.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.repository.track.TrackRepository;

public class TrackViewModel extends ViewModel {
    private final TrackRepository trackRepository;

    public TrackViewModel(TrackRepository trackRepository) {
        this.trackRepository = trackRepository;
    }

    public MutableLiveData<Result> getTrack(String trackId) {
        return trackRepository.getTrack(trackId);
    }

}
