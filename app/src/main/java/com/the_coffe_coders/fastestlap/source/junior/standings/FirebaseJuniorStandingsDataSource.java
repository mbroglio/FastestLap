package com.the_coffe_coders.fastestlap.source.junior.standings;

import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_JUNIOR_COLLECTION;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_REALTIME_DATABASE;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorConstructorStandings;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorConstructorStandingsElement;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorDriverStandings;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorDriverStandingsElement;
import com.the_coffe_coders.fastestlap.repository.junior.standings.JuniorStandingsCallback;

import java.util.ArrayList;
import java.util.List;

public class FirebaseJuniorStandingsDataSource implements JuniorStandingsDataSource {
    private static final String TAG = "FirebaseJuniorStandingsDataSource";
    private static FirebaseJuniorStandingsDataSource instance;
    private final FirebaseDatabase database;

    private FirebaseJuniorStandingsDataSource() {
        this.database = FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE);
    }

    public static synchronized FirebaseJuniorStandingsDataSource getInstance() {
        if (instance == null) {
            instance = new FirebaseJuniorStandingsDataSource();
        }
        return instance;
    }

    @Override
    public void getJuniorDriverStandings(String series, JuniorStandingsCallback callback) {
        Log.i(TAG, "Fetching junior driver standings from Firebase with series: " + series);

        DatabaseReference ref = database.getReference(FIREBASE_JUNIOR_COLLECTION).child(series)
                .child("standings").child("drivers");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    JuniorDriverStandings standings = new JuniorDriverStandings();
                    standings.setSeries(series);

                    List<JuniorDriverStandingsElement> elements = new ArrayList<>();

                    for (DataSnapshot child : snapshot.getChildren()) {
                        if (child.exists()) {
                            JuniorDriverStandingsElement element = child.getValue(JuniorDriverStandingsElement.class);
                            if (element != null) {
                                Log.i(TAG, "element: " + element);
                                elements.add(element);
                            }
                        }
                    }

                    standings.setDriverStandings(elements);
                    Log.i(TAG, "Successfully loaded " + elements.size() + " driver standings for series: " + series);
                    callback.onDriverStandingsLoaded(standings);
                } else {
                    Log.e(TAG, "No driver standings found for series: " + series);
                    callback.onError(new Exception("No driver standings found for series: " + series));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching junior driver standings from Firebase: " + error.getMessage());
                callback.onError(new Exception(error.getMessage()));
            }
        });
    }

    @Override
    public void getJuniorConstructorStandings(String series, JuniorStandingsCallback callback) {
        Log.i(TAG, "Fetching junior constructor standings from Firebase with series: " + series);

        DatabaseReference ref = database.getReference(FIREBASE_JUNIOR_COLLECTION).child(series)
                .child("standings").child("constructors");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    JuniorConstructorStandings standings = new JuniorConstructorStandings();
                    standings.setSeries(series);

                    List<JuniorConstructorStandingsElement> elements = new ArrayList<>();

                    for (DataSnapshot child : snapshot.getChildren()) {
                        if (child.exists()) {
                            JuniorConstructorStandingsElement element = child.getValue(JuniorConstructorStandingsElement.class);
                            if (element != null) {
                                elements.add(element);
                            }
                        }
                    }

                    standings.setConstructorStandings(elements);
                    Log.i(TAG, "Successfully loaded " + elements.size() + " constructor standings for series: " + series);
                    callback.onConstructorStandingsLoaded(standings);
                } else {
                    Log.e(TAG, "No constructor standings found for series: " + series);
                    callback.onError(new Exception("No constructor standings found for series: " + series));
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching junior constructor standings from Firebase: " + error.getMessage());
                callback.onError(new Exception(error.getMessage()));
            }

        });
    }
}
