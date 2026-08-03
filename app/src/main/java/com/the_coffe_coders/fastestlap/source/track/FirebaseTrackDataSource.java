package com.the_coffe_coders.fastestlap.source.track;

import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_CIRCUITS_COLLECTION;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_CIRCUIT_MAP_COLLECTION;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_REALTIME_DATABASE;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.the_coffe_coders.fastestlap.domain.f1.track.Track;
import com.the_coffe_coders.fastestlap.repository.track.TrackCallback;

import java.util.HashMap;
import java.util.Map;

public class FirebaseTrackDataSource implements TrackDataSource {
    private static final String TAG = "FirebaseTrackDataSource";
    private static FirebaseTrackDataSource instance;
    private final FirebaseDatabase database;

    // Common fallback aliases for circuit IDs between Ergast/Jolpica and Firebase
    private static final Map<String, String> TRACK_ID_ALIASES = new HashMap<>() {{
        put("sepang", "sepang_international_circuit");
        put("sepang_international_circuit", "sepang");
    }};

    public FirebaseTrackDataSource() {
        this.database = FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE);
    }

    public static synchronized FirebaseTrackDataSource getInstance() {
        if (instance == null) {
            instance = new FirebaseTrackDataSource();
        }
        return instance;
    }

    @Override
    public void getTrack(String trackId, TrackCallback callback) {
        if (trackId == null || trackId.isEmpty()) {
            callback.onError(new IllegalArgumentException("trackId is null or empty"));
            return;
        }

        fetchTrackDirect(trackId, new TrackCallback() {
            @Override
            public void onTrackLoaded(Track track) {
                callback.onTrackLoaded(track);
            }

            @Override
            public void onError(Exception e) {
                // If direct lookup under circuits/trackId failed, check app_config/circuit_name_id_map
                checkCircuitMapCollection(trackId, callback, e);
            }
        });
    }

    private void fetchTrackDirect(String key, TrackCallback callback) {
        DatabaseReference databaseReference = database.getReference(FIREBASE_CIRCUITS_COLLECTION).child(key);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Track track = snapshot.getValue(Track.class);
                    if (track != null) {
                        if (track.getTrackId() == null || track.getTrackId().isEmpty()) {
                            track.setTrackId(key);
                        }
                        callback.onTrackLoaded(track);
                    } else {
                        callback.onError(new NullPointerException("Track data is null for key: " + key));
                    }
                } else {
                    callback.onError(new NullPointerException("No track found for key: " + key));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.toException());
            }
        });
    }

    private void checkCircuitMapCollection(String originalTrackId, TrackCallback callback, Exception originalError) {
        DatabaseReference mapRef = database.getReference(FIREBASE_CIRCUIT_MAP_COLLECTION).child(originalTrackId);
        mapRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String mappedId = snapshot.getValue(String.class);
                    if (mappedId != null && !mappedId.isEmpty()) {
                        Log.d(TAG, "Found mapped circuit ID '" + mappedId + "' for '" + originalTrackId + "'");
                        fetchTrackDirect(mappedId, callback);
                        return;
                    }
                }

                // Check alias map as fallback (e.g. sepang -> sepang_international_circuit)
                String aliasId = TRACK_ID_ALIASES.get(originalTrackId.toLowerCase());
                if (aliasId != null) {
                    Log.d(TAG, "Trying alias circuit ID '" + aliasId + "' for '" + originalTrackId + "'");
                    fetchTrackDirect(aliasId, callback);
                } else {
                    searchAllCircuits(originalTrackId, callback, originalError);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                String aliasId = TRACK_ID_ALIASES.get(originalTrackId.toLowerCase());
                if (aliasId != null) {
                    fetchTrackDirect(aliasId, callback);
                } else {
                    searchAllCircuits(originalTrackId, callback, originalError);
                }
            }
        });
    }

    private void searchAllCircuits(String originalTrackId, TrackCallback callback, Exception originalError) {
        DatabaseReference circuitsRef = database.getReference(FIREBASE_CIRCUITS_COLLECTION);
        circuitsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String cleanSearchId = originalTrackId.toLowerCase().replaceAll("[^a-z0-9]", "");
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String key = child.getKey();
                        Track t = child.getValue(Track.class);
                        if (key != null) {
                            String cleanKey = key.toLowerCase().replaceAll("[^a-z0-9]", "");
                            if (cleanKey.equals(cleanSearchId) || cleanKey.contains(cleanSearchId)) {
                                if (t != null) {
                                    if (t.getTrackId() == null || t.getTrackId().isEmpty()) {
                                        t.setTrackId(key);
                                    }
                                    Log.d(TAG, "Found matching circuit by key search: " + key);
                                    callback.onTrackLoaded(t);
                                    return;
                                }
                            }
                        }
                        if (t != null && t.getTrackName() != null) {
                            String cleanName = t.getTrackName().toLowerCase().replaceAll("[^a-z0-9]", "");
                            if (cleanName.equals(cleanSearchId) || cleanName.contains(cleanSearchId)) {
                                if (t.getTrackId() == null || t.getTrackId().isEmpty()) {
                                    t.setTrackId(child.getKey());
                                }
                                Log.d(TAG, "Found matching circuit by trackName search: " + t.getTrackName());
                                callback.onTrackLoaded(t);
                                return;
                            }
                        }
                    }
                }
                callback.onError(originalError);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(originalError);
            }
        });
    }
}


