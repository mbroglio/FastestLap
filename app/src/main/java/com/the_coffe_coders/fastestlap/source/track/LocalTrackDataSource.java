package com.the_coffe_coders.fastestlap.source.track;

import android.util.Log;

import com.the_coffe_coders.fastestlap.database.AppRoomDatabase;
import com.the_coffe_coders.fastestlap.database.TrackDAO;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Track;
import com.the_coffe_coders.fastestlap.repository.track.TrackCallback;

public class LocalTrackDataSource implements TrackDataSource {
    private static final String TAG = "TrackLocalDataSource";
    private static LocalTrackDataSource instance;
    private final TrackDAO trackDAO;

    private LocalTrackDataSource(AppRoomDatabase appRoomDatabase) {
        this.trackDAO = appRoomDatabase.trackDAO();
    }

    public static synchronized LocalTrackDataSource getInstance(AppRoomDatabase appRoomDatabase) {
        if (instance == null) {
            instance = new LocalTrackDataSource(appRoomDatabase);
        }
        return instance;
    }

    @Override
    public void getTrack(String trackId, TrackCallback callback) {
        Log.d(TAG, "Fetching track with ID: " + trackId);
        callback.onTrackLoaded(trackDAO.getById(trackId));
    }

    public void insertTrack(Track track) {
        trackDAO.insertTrack(track);
    }
}
