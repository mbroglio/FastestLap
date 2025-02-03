package com.the_coffe_coders.fastestlap.repository.track;

import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_CIRCUITS_COLLECTION;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_REALTIME_DATABASE;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.grand_prix.Track;

public class FirebaseTrackRepository {
    private final FirebaseDatabase database;

    public FirebaseTrackRepository() {
        this.database = FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE);
    }

    public interface TrackCallback {
        void onSuccess(Track track);
        void onFailure(Exception exception);
    }

    public MutableLiveData<Result> getTrack(String trackId, TrackCallback callback) {
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
            public void onCancelled(DatabaseError error) {
                callback.onFailure(error.toException());
            }
        });
        return null;
    }
}
