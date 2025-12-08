package com.the_coffe_coders.fastestlap.source.junior.calendar;

import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_JUNIOR_COLLECTION;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_REALTIME_DATABASE;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.the_coffe_coders.fastestlap.domain.junior.calendar.JuniorCalendar;
import com.the_coffe_coders.fastestlap.domain.junior.calendar.JuniorCalendarElement;
import com.the_coffe_coders.fastestlap.repository.junior.calendar.JuniorCalendarCallback;

import java.util.ArrayList;
import java.util.List;


public class FirebaseJuniorCalendarDataSource implements JuniorCalendarDataSource {
    private static final String TAG = "FirebaseJuniorCalendarDataSource";
    private static FirebaseJuniorCalendarDataSource instance;
    private final FirebaseDatabase database;


    private FirebaseJuniorCalendarDataSource() {
        this.database = FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE);
    }

    public static synchronized FirebaseJuniorCalendarDataSource getInstance() {
        if (instance == null) {
            instance = new FirebaseJuniorCalendarDataSource();
        }
        return instance;
    }

    /*
    * ----------------------------------------------------------------------------------------------
    * Recupero calendario in base alla categoria selezionata
    * ----------------------------------------------------------------------------------------------
    * */

    @Override
    public void getJuniorCalendar(String series, JuniorCalendarCallback callback){
        Log.i(TAG, "Fetching calendar from Firebase with series: " + series);

        DatabaseReference ref = database.getReference(FIREBASE_JUNIOR_COLLECTION).child(series).child("calendar");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    JuniorCalendar calendar = new JuniorCalendar();
                    calendar.setSeries(series);

                    List<JuniorCalendarElement> events = new ArrayList<>();

                    // The snapshot itself is the list, so iterate over its children directly
                    for(DataSnapshot child : snapshot.getChildren()){
                        if(child.exists()){
                            // Each child is a calendar element (an item in the array)
                            JuniorCalendarElement event = child.getValue(JuniorCalendarElement.class);
                            if(event != null){
                                events.add(event);
                            }
                        }
                    }

                    // Check if we found any events
                    if (events.isEmpty()) {
                        Log.w(TAG, "No events found for series: " + series);
                    }

                    calendar.setEvents(events);
                    callback.onCalendarLoaded(calendar);

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
