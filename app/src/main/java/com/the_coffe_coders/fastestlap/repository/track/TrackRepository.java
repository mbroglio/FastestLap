package com.the_coffe_coders.fastestlap.repository.track;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Track;
import com.the_coffe_coders.fastestlap.source.track.FirebaseTrackDataSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TrackRepository {
    private final FirebaseTrackDataSource firebaseTrackDataSource;
    private static TrackRepository instance;
    Map<String, MutableLiveData<Result>> trackCache;
    Map<String, Long> lastFetchTime;
    
    private static final String TAG = "TrackRepository";
    
    public static synchronized TrackRepository getInstance() {
        if (instance == null) {
            instance = new TrackRepository();
        }
        return instance;
    }
    
    public TrackRepository() {
        this.firebaseTrackDataSource = FirebaseTrackDataSource.getInstance();
        this.trackCache = new HashMap<>();
        this.lastFetchTime = new HashMap<>();
    }
    
    public synchronized MutableLiveData<Result> getTrack(String trackId) {
        Log.d(TAG, "Fetching track with ID: " + trackId);
        if(!trackCache.containsKey(trackId) || !lastFetchTime.containsKey(trackId) || lastFetchTime.get(trackId) == null) {
            trackCache.put(trackId, new MutableLiveData<>());
            loadTrack(trackId);
        }else if(System.currentTimeMillis() - lastFetchTime.get(trackId) > 60000) {
            loadTrack(trackId);
        } else {
            Log.d(TAG, "Track found in cache: " + trackId);
        }
        return trackCache.get(trackId);
    }
    
    public void loadTrack(String trackId) {
        trackCache.get(trackId).postValue(new Result.Loading("Fetching track from remote"));
        firebaseTrackDataSource.getTrack(trackId, new TrackCallback() {
            @Override
            public void onSuccess(Track track) {
                Log.d(TAG, "Track loaded: " + track);
                if(track!=null){
                    track.setTrackId(trackId);
                    Objects.requireNonNull(trackCache.get(trackId)).postValue(new Result.TrackSuccess(track));
                    lastFetchTime.put(trackId, System.currentTimeMillis());
                }
            }

            @Override
            public void onFailure(Exception exception) {
                Log.e(TAG, "Error loading track: " + exception.getMessage());
            }
        });
    }
}
