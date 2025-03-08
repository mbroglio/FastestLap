package com.the_coffe_coders.fastestlap.data.repository.track;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Track;

public class TrackRepository implements ITrackRepository {

    FirebaseTrackRepository firebaseTrackRepository;

    MutableLiveData<Result> trackLiveData;

    public TrackRepository() {
        trackLiveData = new MutableLiveData<>();
        firebaseTrackRepository = new FirebaseTrackRepository();
    }

    @Override
    public MutableLiveData<Result> getTrack(String trackId) {
        firebaseTrackRepository.getTrack(trackId, new FirebaseTrackRepository.TrackCallback() {
            @Override
            public void onSuccess(Track track) {
                trackLiveData.postValue(new Result.TrackSuccess(track));
            }

            @Override
            public void onFailure(Exception exception) {
                trackLiveData.postValue(new Result.Error("Error getting track data"));
            }
        });

        return trackLiveData;
    }
}
