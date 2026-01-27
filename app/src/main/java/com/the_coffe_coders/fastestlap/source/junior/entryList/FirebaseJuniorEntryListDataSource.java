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
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorEntryList;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorTeam;
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
                        try {
                            if (child.exists()) {
                                JuniorTeam team = child.getValue(JuniorTeam.class);
                                if (team != null) {
                                    team.setName(child.getKey());
                                }
                                teams.add(team);

                            }
                        } catch (Exception e) {
                            callback.onError(new Exception("Firebase error: " + e.getMessage()));
                        }

                    }

                    entryList.setTeams(teams);
                    callback.onEntryListLoaded(entryList);

                } else {
                    callback.onError(new Exception("No entry list found in Firebase"));
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
