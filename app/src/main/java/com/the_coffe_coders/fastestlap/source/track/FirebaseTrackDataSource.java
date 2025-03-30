package com.the_coffe_coders.fastestlap.source.track;

import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_CIRCUITS_COLLECTION;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_REALTIME_DATABASE;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Track;
import com.the_coffe_coders.fastestlap.repository.track.TrackCallback;

public class FirebaseTrackDataSource implements TrackDataSource {
    private final FirebaseDatabase database;
    private static FirebaseTrackDataSource instance;

    public static synchronized FirebaseTrackDataSource getInstance() {
        if (instance == null) {
            instance = new FirebaseTrackDataSource();
        }
        return instance;
    }

    public FirebaseTrackDataSource() {
        this.database = FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE);
    }

    @Override
    public void getTrack(String trackId, TrackCallback callback) {
        DatabaseReference databaseReference = database.getReference(FIREBASE_CIRCUITS_COLLECTION).child(trackId);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Track track = snapshot.getValue(Track.class);
                    if (track != null) {
                        callback.onSuccess(track);
                    } else {
                        callback.onFailure(new NullPointerException("Track data is null"));
                    }
                } else {
                    callback.onFailure(new NullPointerException("No track found"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailure(error.toException());
            }
        });
    }
}
