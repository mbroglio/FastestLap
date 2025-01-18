package com.the_coffe_coders.fastestlap.source.user;

import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_REALTIME_DATABASE;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_USERS_COLLECTION;
import static com.the_coffe_coders.fastestlap.util.Constants.SHARED_PREFERENCES_FAVORITE_DRIVER;
import static com.the_coffe_coders.fastestlap.util.Constants.SHARED_PREFERENCES_FAVORITE_TEAM;
import static com.the_coffe_coders.fastestlap.util.Constants.SHARED_PREFERENCES_FILENAME;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.the_coffe_coders.fastestlap.domain.user.User;
import com.the_coffe_coders.fastestlap.util.SharedPreferencesUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that gets the user information using Firebase Realtime Database.
 */
public class UserFirebaseDataSource extends BaseUserDataRemoteDataSource {

    private static final String TAG = UserFirebaseDataSource.class.getSimpleName();

    private final DatabaseReference databaseReference;
    private final SharedPreferencesUtils sharedPreferencesUtil;

    public UserFirebaseDataSource(SharedPreferencesUtils sharedPreferencesUtil) {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance(FIREBASE_REALTIME_DATABASE);
        databaseReference = firebaseDatabase.getReference().getRef();
        this.sharedPreferencesUtil = sharedPreferencesUtil;
    }

    @Override
    public void saveUserData(User user) {
        databaseReference.child(FIREBASE_USERS_COLLECTION).child(user.getIdToken()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Log.d(TAG, "User already present in Firebase Realtime Database");
                    userResponseCallback.onSuccessFromRemoteDatabase(user);
                } else {
                    Log.d(TAG, "User not present in Firebase Realtime Database");
                    databaseReference.child(FIREBASE_USERS_COLLECTION).child(user.getIdToken()).setValue(user)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    userResponseCallback.onSuccessFromRemoteDatabase(user);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    userResponseCallback.onFailureFromRemoteDatabase(e.getLocalizedMessage());
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                userResponseCallback.onFailureFromRemoteDatabase(error.getMessage());
            }
        });
    }

    @Override
    public void getUserPreferences(String idToken) {
        databaseReference.child(FIREBASE_USERS_COLLECTION).child(idToken).
                child(SHARED_PREFERENCES_FAVORITE_DRIVER).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String favoriteDriver = task.getResult().getValue(String.class);
                        sharedPreferencesUtil.writeStringData(
                                SHARED_PREFERENCES_FILENAME,
                                SHARED_PREFERENCES_FAVORITE_DRIVER,
                                favoriteDriver);

                        databaseReference.child(FIREBASE_USERS_COLLECTION).child(idToken).
                                child(SHARED_PREFERENCES_FAVORITE_TEAM).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DataSnapshot> task) {
                                        if (task.isSuccessful()) {
                                            String favoriteTeam = task.getResult().getValue(String.class);
                                            sharedPreferencesUtil.writeStringData(
                                                    SHARED_PREFERENCES_FILENAME,
                                                    SHARED_PREFERENCES_FAVORITE_TEAM,
                                                    favoriteTeam);

                                            userResponseCallback.onSuccessFromGettingUserPreferences();
                                        }
                                    }
                                });
                    }
                });
    }

    @Override
    public void saveUserPreferences(String favoriteDriver, String favoriteTeam, String idToken) {

        databaseReference.child(FIREBASE_USERS_COLLECTION).child(idToken).
                child(SHARED_PREFERENCES_FAVORITE_DRIVER).setValue(favoriteDriver);

        databaseReference.child(FIREBASE_USERS_COLLECTION).child(idToken).
                child(SHARED_PREFERENCES_FAVORITE_TEAM).setValue(favoriteTeam).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.i(TAG, "fattoooo");
                    }
                });
    }
}
