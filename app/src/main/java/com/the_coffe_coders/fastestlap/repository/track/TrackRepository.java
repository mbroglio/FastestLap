package com.the_coffe_coders.fastestlap.repository.track;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.track.Track;
import com.the_coffe_coders.fastestlap.source.track.FirebaseTrackDataSource;
import com.the_coffe_coders.fastestlap.source.track.LocalTrackDataSource;
import com.the_coffe_coders.fastestlap.util.service.NetworkUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TrackRepository {
    private static final String TAG = "TrackRepository";
    private static TrackRepository instance;
    //Data sources
    final FirebaseTrackDataSource firebaseTrackDataSource;
    final LocalTrackDataSource localTrackDataSource;
    //Cache
    private final Map<String, MutableLiveData<Result>> trackCache;
    private final Map<String, Long> lastUpdateTimestamps;
    private final NetworkUtils networkLiveData;

    public TrackRepository(AppRoomDatabase appRoomDatabase, Context context) {
        trackCache = new HashMap<>();
        lastUpdateTimestamps = new HashMap<>();
        firebaseTrackDataSource = FirebaseTrackDataSource.getInstance();
        localTrackDataSource = LocalTrackDataSource.getInstance(appRoomDatabase);
        networkLiveData = new NetworkUtils(context);
    }

    public static synchronized TrackRepository getInstance(AppRoomDatabase appRoomDatabase, Context context) {
        if (instance == null) {
            instance = new TrackRepository(appRoomDatabase, context);
        }
        return instance;
    }

    private boolean isNetworkAvailable() {
        return networkLiveData.isConnected();
    }

    public synchronized MutableLiveData<Result> getTrack(String trackId) {
        Log.d(TAG, "Fetching track with ID: " + trackId);
        if (!trackCache.containsKey(trackId)) {
            trackCache.put(trackId, new MutableLiveData<>());
            loadTrackCacheFirst(trackId);
        } else {
            Long lastUpdate = lastUpdateTimestamps.get(trackId);
            if (lastUpdate == null || System.currentTimeMillis() - lastUpdate > 300000) {
                if (isNetworkAvailable()) {
                    loadTrackFromRemote(trackId, true);
                }
            } else {
                Log.d(TAG, "Track found in cache: " + trackId);
            }
        }
        return trackCache.get(trackId);
    }

    private void loadTrackCacheFirst(String trackId) {
        localTrackDataSource.getTrack(trackId, new TrackCallback() {
            @Override
            public void onTrackLoaded(Track track) {
                if (track != null) {
                    Log.d(TAG, "Track loaded from local database (cache hit): " + trackId);
                    track.setTrackId(trackId);

                    Long previousTs = lastUpdateTimestamps.get(trackId);
                    boolean isStale = previousTs == null || System.currentTimeMillis() - previousTs > 300_000L;
                    lastUpdateTimestamps.put(trackId, System.currentTimeMillis());
                    Objects.requireNonNull(trackCache.get(trackId)).postValue(new Result.TrackSuccess(track));

                    if (isNetworkAvailable() && isStale) {
                        loadTrackFromRemote(trackId, true);
                    } else {
                        Log.d(TAG, "Track cache still fresh, skipping remote refresh: " + trackId);
                    }
                } else {
                    Log.d(TAG, "Track cache miss in local database: " + trackId);
                    if (isNetworkAvailable()) {
                        loadTrackFromRemote(trackId, false);
                    } else {
                        Objects.requireNonNull(trackCache.get(trackId)).postValue(
                                new Result.Error("Track not found locally and no network connection available"));
                    }
                }
            }


            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error checking local database for track: " + e.getMessage());
                if (isNetworkAvailable()) {
                    loadTrackFromRemote(trackId, false);
                }
            }
        });
    }

    private void loadTrackFromRemote(String trackId, boolean isBackgroundRefresh) {
        if (!isBackgroundRefresh) {
            trackCache.get(trackId).postValue(new Result.Loading("Fetching track from remote"));
        }
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
                    } else if (!isBackgroundRefresh) {
                        Log.e(TAG, "Track not found in remote: " + trackId);
                        Objects.requireNonNull(trackCache.get(trackId)).postValue(
                                new Result.Error("Track not found in remote: " + trackId));
                    }
                }

                @Override
                public void onError(Exception exception) {
                    Log.e(TAG, "Error loading track: " + exception.getMessage());
                    if (!isBackgroundRefresh) {
                        Objects.requireNonNull(trackCache.get(trackId)).postValue(
                                new Result.Error("Error loading track: " + exception.getMessage()));
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading track: " + e.getMessage());
            if (!isBackgroundRefresh) {
                Objects.requireNonNull(trackCache.get(trackId)).postValue(
                        new Result.Error("Error loading track: " + e.getMessage()));
            }
        }
    }
}
