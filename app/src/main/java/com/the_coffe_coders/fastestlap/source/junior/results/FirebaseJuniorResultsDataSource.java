package com.the_coffe_coders.fastestlap.source.junior.results;

import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_JUNIOR_COLLECTION;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_REALTIME_DATABASE;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.the_coffe_coders.fastestlap.domain.junior.result.JuniorResult;
import com.the_coffe_coders.fastestlap.domain.junior.result.JuniorResultElement;
import com.the_coffe_coders.fastestlap.repository.junior.result.JuniorResultCallback;

import java.util.ArrayList;
import java.util.List;

public class FirebaseJuniorResultsDataSource implements JuniorResultsDataSource {
    private static final String TAG = "FirebaseJuniorResultsDataSource";
    private static FirebaseJuniorResultsDataSource instance;
    private final FirebaseDatabase database;

    private FirebaseJuniorResultsDataSource() {
        this.database = FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE);
    }

    public static synchronized FirebaseJuniorResultsDataSource getInstance() {
        if (instance == null) {
            instance = new FirebaseJuniorResultsDataSource();
        }
        return instance;
    }

    @Override
    public void getJuniorResults(String series, JuniorResultCallback callback) {
        Log.i(TAG, "Fetching junior results from Firebase with series: " + series);

        DatabaseReference ref = database.getReference(FIREBASE_JUNIOR_COLLECTION).child(series).child("results");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    JuniorResult result = new JuniorResult();
                    result.setSeries(series);

                    List<JuniorResultElement> results = new ArrayList<>();

                    for (DataSnapshot child : snapshot.getChildren()) {
                        if (child.exists()) {
                            JuniorResultElement element = child.getValue(JuniorResultElement.class);
                            if (element != null) {
                                results.add(element);
                                Log.d(TAG, "Loaded result element for round: " + element.getRound());
                            }
                        }
                    }

                    result.setResults(results);
                    Log.i(TAG, "Successfully loaded " + results.size() + " results for series: " + series);
                    callback.onResultLoaded(result);
                } else {
                    Log.w(TAG, "No results found for series: " + series);
                    callback.onError(new Exception("No results found for series: " + series));
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                Log.e(TAG, "Error fetching junior results: " + error.getMessage());
                callback.onError(new Exception(error.getMessage()));
            }
        });
    }


}
