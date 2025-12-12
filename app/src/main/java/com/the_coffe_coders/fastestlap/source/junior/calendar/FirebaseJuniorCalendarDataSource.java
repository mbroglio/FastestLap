package com.the_coffe_coders.fastestlap.source.junior.calendar;


import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_CIRCUIT_MAP_COLLECTION;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_JUNIOR_COLLECTION;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_REALTIME_DATABASE;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Track;
import com.the_coffe_coders.fastestlap.domain.junior.calendar.JuniorCalendar;
import com.the_coffe_coders.fastestlap.domain.junior.calendar.JuniorCalendarElement;
import com.the_coffe_coders.fastestlap.domain.nation.Nation;
import com.the_coffe_coders.fastestlap.repository.junior.calendar.JuniorCalendarCallback;
import com.the_coffe_coders.fastestlap.repository.nation.NationCallback;
import com.the_coffe_coders.fastestlap.repository.track.TrackCallback;
import com.the_coffe_coders.fastestlap.source.nation.FirebaseNationDataSource;
import com.the_coffe_coders.fastestlap.source.track.FirebaseTrackDataSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class FirebaseJuniorCalendarDataSource implements JuniorCalendarDataSource {
    private static final String TAG = "FirebaseJuniorCalendarDataSource";
    private static FirebaseJuniorCalendarDataSource instance;
    private final FirebaseDatabase database;
    //private final FirebaseTrackDataSource trackDataSource;
    //private final FirebaseNationDataSource nationDataSource;

    long childrenCount;

    private FirebaseJuniorCalendarDataSource() {
        this.database = FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE);
        //this.trackDataSource = FirebaseTrackDataSource.getInstance();
        //this.nationDataSource = FirebaseNationDataSource.getInstance();
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

    //This logic considers the db already enriched with supplementary information (circuit name, nation_flag_url)
    @Override
    public void getJuniorCalendar(String series, JuniorCalendarCallback callback) {
        Log.i(TAG, "Fetching calendar from Firebase with series: " + series);

        DatabaseReference ref = database.getReference(FIREBASE_JUNIOR_COLLECTION).child(series).child("calendar");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    JuniorCalendar calendar = new JuniorCalendar();
                    calendar.setSeries(series);

                    List<JuniorCalendarElement> events= new ArrayList<>();
                    for(DataSnapshot child : snapshot.getChildren()){
                        if(child.exists()) {
                            JuniorCalendarElement event = child.getValue(JuniorCalendarElement.class);
                            if(event != null){
                                events.add(event);
                            }
                        }
                    }

                    if(events.isEmpty()){
                        Log.w(TAG, "No events found for series: " + series);
                        calendar.setEvents(null);
                        callback.onCalendarLoaded(calendar);
                    }else{
                        calendar.setEvents(events);
                        callback.onCalendarLoaded(calendar);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase request cancelled: " + error.getMessage());
                callback.onError(new Exception("Firebase error: " + error.getMessage()));
            }
        });
    }


    /* LOGIC BEFORE JS FUNCTION UPDATE
    @Override
    public void getJuniorCalendar(String series, JuniorCalendarCallback callback){
        Log.i(TAG, "Fetching calendar from Firebase with series: " + series);

        DatabaseReference ref = database.getReference(FIREBASE_JUNIOR_COLLECTION).child(series).child("calendar");

        final AtomicInteger counter = new AtomicInteger(0);
        final AtomicBoolean completed = new AtomicBoolean(false);
        final List<JuniorCalendarElement> events = Collections.synchronizedList(new ArrayList<>());

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    childrenCount = snapshot.getChildrenCount();
                    if(childrenCount == 0){
                        Log.w(TAG, "No events found for series: " + series);
                        callback.onCalendarLoaded(null);
                    }

                    JuniorCalendar calendar = new JuniorCalendar();
                    calendar.setSeries(series);

                    // The snapshot itself is the list, so iterate over its children directly
                    for(DataSnapshot child : snapshot.getChildren()){
                        if(child.exists()){
                            // Each child is a calendar element (an item in the array)
                            JuniorCalendarElement event = child.getValue(JuniorCalendarElement.class);
                            if(event != null){
                                database.getReference(FIREBASE_CIRCUIT_MAP_COLLECTION).child(event.getCircuit())
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                if(snapshot.exists()){
                                                    String circuitId = snapshot.getValue(String.class);
                                                    if(circuitId != null) {
                                                        trackDataSource.getTrack(circuitId, new TrackCallback() {
                                                            @Override
                                                            public void onTrackLoaded(Track track) {
                                                                if (track != null) {
                                                                    event.setCircuit(track.getTrackName());

                                                                    nationDataSource.getNation(track.getCountry(), new NationCallback() {
                                                                        @Override
                                                                        public void onNationLoaded(Nation nation) {
                                                                            if(nation != null){
                                                                                event.setNationFlagUrl(nation.getNation_flag_url());
                                                                                addEventAndCheckCompletion(events, event, calendar, childrenCount, counter, completed, callback);
                                                                            }
                                                                        }

                                                                        @Override
                                                                        public void onError(Exception e) {
                                                                            Log.e(TAG, "Error loading Nation: " + e.getMessage());
                                                                            addEventAndCheckCompletion(events, event, calendar, childrenCount, counter, completed, callback);
                                                                        }
                                                                    });
                                                                }
                                                            }

                                                            @Override
                                                            public void onError(Exception e) {
                                                                Log.e(TAG, "Error loading Track: " + e.getMessage());
                                                                addEventAndCheckCompletion(events, event, calendar, childrenCount, counter,completed, callback);
                                                            }
                                                        });
                                                    }
                                                }else{
                                                    Log.e(TAG, "No circuit found for ID: " + event.getCircuit());
                                                    addEventAndCheckCompletion(events, event, calendar, childrenCount, counter, completed, callback);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                Log.e(TAG, "Map Lookup Error: " + error.getMessage());
                                                addEventAndCheckCompletion(events, event, calendar, childrenCount, counter, completed, callback);
                                            }
                                        });
                            }
                        }else{
                            Log.w(TAG, "Child is null for series: " + series);
                            callback.onCalendarLoaded(null);
                        }
                    }
                }else{
                    Log.w(TAG, "No events found for series: " + series);
                    callback.onCalendarLoaded(null);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase request cancelled: " + error.getMessage());
                callback.onError(new Exception("Firebase error: " + error.getMessage()));
            }
        });
    }

    private void addEventAndCheckCompletion(List<JuniorCalendarElement> events, JuniorCalendarElement event, JuniorCalendar calendar, long childrenCount, AtomicInteger counter, AtomicBoolean completed, JuniorCalendarCallback callback) {
        events.add(event);
        checkCompletion(childrenCount, counter, completed, calendar, events, callback);
    }

    private void checkCompletion(long childrenCount, AtomicInteger counter, AtomicBoolean completed, JuniorCalendar calendar, List<JuniorCalendarElement> events,JuniorCalendarCallback callback) {
        if (counter.incrementAndGet() == childrenCount && completed.compareAndSet(false, true)) {
            Log.i(TAG, "All events fetched for series: " + calendar.getSeries());

            List<JuniorCalendarElement> snapshot;
            synchronized (events) {
                snapshot = new ArrayList<>(events);
            }

            try {
                snapshot.sort(Comparator.comparingInt(o -> Integer.parseInt(o.getRound())));
            } catch (Throwable t) {
                Log.w(TAG, "Sorting calendar failed: " + t.getMessage());
            }
            
            calendar.setEvents(snapshot);
            callback.onCalendarLoaded(calendar);
        }
    }
    */
}






