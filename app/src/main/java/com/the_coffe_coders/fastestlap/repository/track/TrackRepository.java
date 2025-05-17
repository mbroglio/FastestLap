package com.the_coffe_coders.fastestlap.repository.track;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Track;
import com.the_coffe_coders.fastestlap.source.track.FirebaseTrackDataSource;
import com.the_coffe_coders.fastestlap.source.track.LocalTrackDataSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TrackRepository {
    private static final String TAG = "TrackRepository";
    private static TrackRepository instance;

    //Cache
    private final Map<String, MutableLiveData<Result>> trackCache;
    private final Map<String, Long> lastUpdateTimestamps;

    //Data sources
    FirebaseTrackDataSource firebaseTrackDataSource;
    LocalTrackDataSource localTrackDataSource;
    AppRoomDatabase appRoomDatabase;

    public TrackRepository(AppRoomDatabase appRoomDatabase) {
        trackCache = new HashMap<>();
        lastUpdateTimestamps = new HashMap<>();
        firebaseTrackDataSource = FirebaseTrackDataSource.getInstance();
        localTrackDataSource = LocalTrackDataSource.getInstance(appRoomDatabase);
    }

    public static synchronized TrackRepository getInstance(AppRoomDatabase appRoomDatabase) {
        if (instance == null) {
            instance = new TrackRepository(appRoomDatabase);
        }
        return instance;
    }

    public synchronized MutableLiveData<Result> getTrack(String trackId) {
        Log.d(TAG, "Fetching track with ID: " + trackId);
        if (!trackCache.containsKey(trackId) || !lastUpdateTimestamps.containsKey(trackId) || lastUpdateTimestamps.get(trackId) == null) {
            trackCache.put(trackId, new MutableLiveData<>());
            loadTrack(trackId);
        } else if (System.currentTimeMillis() - lastUpdateTimestamps.get(trackId) > 60000) {
            loadTrack(trackId);
        } else {
            Log.d(TAG, "Track found in cache: " + trackId);
        }
        return trackCache.get(trackId);
    }

    public void loadTrackFromLocal(String trackId) {
        localTrackDataSource.getTrack(trackId, new TrackCallback() {
            @Override
            public void onTrackLoaded(Track track) {
                if (track != null) {
                    track.setTrackId(trackId);
                    trackCache.put(trackId, new MutableLiveData<>(new Result.TrackSuccess(track)));
                    lastUpdateTimestamps.put(trackId, System.currentTimeMillis());
                    Objects.requireNonNull(trackCache.get(trackId)).postValue(new Result.TrackSuccess(track));
                    Log.d(TAG, "Track loaded from local cache: " + track);
                } else {
                    Log.e(TAG, "Track not found in local cache: " + trackId);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading track from local cache: " + e.getMessage());
                loadTrackFromLocal(trackId);
            }
        });
    }

    public void loadTrack(String trackId) {
        trackCache.get(trackId).postValue(new Result.Loading("Fetching track from remote"));
        try {
            firebaseTrackDataSource.getTrack(trackId, new TrackCallback() {
                @Override
                public void onTrackLoaded(Track track) {
                    Log.d(TAG, "Track loaded: " + track);
                    if (track != null) {
                        track.setTrackId(trackId);
                        localTrackDataSource.insertTrack(track);
                        lastUpdateTimestamps.put(trackId, System.currentTimeMillis());
                        Objects.requireNonNull(trackCache.get(trackId)).postValue(new Result.TrackSuccess(track));
                    } else {
                        Log.e(TAG, "Track not found in cache: " + trackId);
                    }
                }

                @Override
                public void onError(Exception exception) {
                    Log.e(TAG, "Error loading track: " + exception.getMessage());

                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading track: " + e.getMessage());
        }
    }
}
