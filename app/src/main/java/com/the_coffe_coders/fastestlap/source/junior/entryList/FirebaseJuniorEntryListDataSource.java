package com.the_coffe_coders.fastestlap.source.junior.entryList;

import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_JUNIOR_COLLECTION;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_REALTIME_DATABASE;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.the_coffe_coders.fastestlap.domain.junior.JuniorDriver;
import com.the_coffe_coders.fastestlap.domain.junior.JuniorEntryList;
import com.the_coffe_coders.fastestlap.domain.junior.JuniorTeam;
import com.the_coffe_coders.fastestlap.repository.junior.entrylist.JuniorEntryListCallback;

import java.util.ArrayList;
import java.util.List;

public class FirebaseJuniorEntryListDataSource implements JuniorEntryListDataSource {
    private static final String TAG = "FirebaseJuniorEntryListDataSource";
    private static FirebaseJuniorEntryListDataSource instance;
    private final FirebaseDatabase database;

    private FirebaseJuniorEntryListDataSource() {
        this.database = FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE);
    }

    public static synchronized FirebaseJuniorEntryListDataSource getInstance() {
        if (instance == null) {
            instance = new FirebaseJuniorEntryListDataSource();
        }
        return instance;
    }

    @Override
    public void getJuniorEntryList(String series, JuniorEntryListCallback callback) {
        Log.i(TAG, "Fetching entry list from Firebase with series: " + series);

        DatabaseReference ref = database.getReference(FIREBASE_JUNIOR_COLLECTION).child(series).child("entrylist");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    JuniorEntryList entryList = new JuniorEntryList();
                    entryList.setSeries(series);

                    List<JuniorTeam> teams = new ArrayList<>();

                    for (DataSnapshot child : snapshot.getChildren()) {
                        if (child.exists()) {
                            JuniorTeam team = new JuniorTeam();
                            team.setName(child.getKey());
                            List<JuniorDriver> drivers = new ArrayList<>();

                            for (DataSnapshot driverSnapshot : child.getChildren()) {
                                if (driverSnapshot.exists()) {
                                    JuniorDriver driver = driverSnapshot.getValue(JuniorDriver.class);
                                    if (driver != null) {
                                        drivers.add(driver);
                                    }
                                }
                            }

                            team.setDrivers(drivers);
                            teams.add(team);

                        }
                    }

                    entryList.setTeams(teams);
                    callback.onEntryListLoaded(entryList);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase request cancelled: " + error.getMessage());
                callback.onError(new Exception("Firebase error: " + error.getMessage()));
            }

        });
    }
}
