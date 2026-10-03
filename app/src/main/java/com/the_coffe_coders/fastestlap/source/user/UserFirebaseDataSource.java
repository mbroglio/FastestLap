package com.the_coffe_coders.fastestlap.source.user;

import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_REALTIME_DATABASE;
import static com.the_coffe_coders.fastestlap.util.Constants.FIREBASE_USERS_COLLECTION;
import static com.the_coffe_coders.fastestlap.util.Constants.SHARED_PREFERENCES_AUTO_LOGIN;
import static com.the_coffe_coders.fastestlap.util.Constants.SHARED_PREFERENCES_FAVORITE_DRIVER;
import static com.the_coffe_coders.fastestlap.util.Constants.SHARED_PREFERENCES_FAVORITE_TEAM;
import static com.the_coffe_coders.fastestlap.util.Constants.SHARED_PREFERENCES_FILENAME;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.the_coffe_coders.fastestlap.domain.user.User;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.notification.AppNotificationManager;
import com.the_coffe_coders.fastestlap.util.service.SharedPreferencesUtils;

import java.util.HashMap;
import java.util.Map;

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
        databaseReference.child(FIREBASE_USERS_COLLECTION).child(user.getIdToken()).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DataSnapshot snapshot = task.getResult();
                if (snapshot.exists()) {
                    Log.d(TAG, "User already present in Firebase Realtime Database");
                    userResponseCallback.onSuccessFromRemoteDatabase(user);
                } else {
                    Log.d(TAG, "User not present in Firebase Realtime Database");
                    databaseReference.child(FIREBASE_USERS_COLLECTION).child(user.getIdToken()).setValue(user)
                            .addOnSuccessListener(aVoid -> userResponseCallback.onSuccessFromRemoteDatabase(user))
                            .addOnFailureListener(e -> userResponseCallback.onFailureFromRemoteDatabase(e.getLocalizedMessage()));
                }
            } else {
                Exception error = task.getException() != null ? task.getException() : new Exception("Failed to check user");
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
                                child(SHARED_PREFERENCES_FAVORITE_TEAM).get().addOnCompleteListener(taskTeam -> {
                                    if (taskTeam.isSuccessful()) {
                                        String favoriteTeam = taskTeam.getResult().getValue(String.class);
                                        sharedPreferencesUtil.writeStringData(
                                                SHARED_PREFERENCES_FILENAME,
                                                SHARED_PREFERENCES_FAVORITE_TEAM,
                                                favoriteTeam);

                                        // Also retrieve remote news source preference if present
                                        databaseReference.child(FIREBASE_USERS_COLLECTION).child(idToken).
                                                child(Constants.SHARED_PREFERENCES_NEWS_SOURCE_ID).get().addOnCompleteListener(taskNewsId -> {
                                                    if (taskNewsId.isSuccessful() && taskNewsId.getResult().getValue(String.class) != null) {
                                                        String sourceId = taskNewsId.getResult().getValue(String.class);
                                                        sharedPreferencesUtil.writeStringData(
                                                                SHARED_PREFERENCES_FILENAME,
                                                                Constants.SHARED_PREFERENCES_NEWS_SOURCE_ID,
                                                                sourceId);
                                                        AppNotificationManager.getInstance().updateNewsTopicSubscription(sourceId);
                                                    }

                                                    // Also retrieve remote notification preferences (F1, F2, F3)
                                                    databaseReference.child(FIREBASE_USERS_COLLECTION).child(idToken).
                                                            child("notification_preferences").get().addOnCompleteListener(taskNotif -> {
                                                                if (taskNotif.isSuccessful() && taskNotif.getResult() != null && taskNotif.getResult().exists()) {
                                                                    Log.i(TAG, "Restoring notification preferences from Firebase for user " + idToken);
                                                                    for (DataSnapshot child : taskNotif.getResult().getChildren()) {
                                                                        String key = child.getKey();
                                                                        Boolean val = child.getValue(Boolean.class);
                                                                        if (key != null && val != null) {
                                                                            sharedPreferencesUtil.writeBooleanData(SHARED_PREFERENCES_FILENAME, key, val);
                                                                        }
                                                                    }
                                                                    if (sharedPreferencesUtil.getContext() != null) {
                                                                        AppNotificationManager.getInstance().syncSessionTopicSubscriptions(sharedPreferencesUtil.getContext(), false);
                                                                    }
                                                                }
                                                                userResponseCallback.onSuccessFromGettingUserPreferences();
                                                            });
                                                });
                                    }
                                });
                    }
                });
    }

    @Override
    public void saveUserPreferences(String favoriteDriver, String favoriteTeam, String autoLogin, String idToken) {

        databaseReference.child(FIREBASE_USERS_COLLECTION).child(idToken).
                child(SHARED_PREFERENCES_FAVORITE_DRIVER).setValue(favoriteDriver);

        databaseReference.child(FIREBASE_USERS_COLLECTION).child(idToken).
                child(SHARED_PREFERENCES_FAVORITE_TEAM).setValue(favoriteTeam).addOnSuccessListener(unused -> Log.i(TAG, "fattoooo team"));

        databaseReference.child(FIREBASE_USERS_COLLECTION).child(idToken).
                child(SHARED_PREFERENCES_AUTO_LOGIN).setValue(autoLogin).addOnSuccessListener(unused -> Log.i(TAG, "fattoooo auto login"));
    }

    @Override
    public void saveUserDriverPreferences(String favoriteDriver, String idToken) {
        databaseReference.child(FIREBASE_USERS_COLLECTION).child(idToken).
                child(SHARED_PREFERENCES_FAVORITE_DRIVER).setValue(favoriteDriver).addOnSuccessListener(unused -> Log.i(TAG, "fattoooo driver"));
    }

    @Override
    public void saveUserConstructorPreferences(String favoriteTeam, String idToken) {
        databaseReference.child(FIREBASE_USERS_COLLECTION).child(idToken).
                child(SHARED_PREFERENCES_FAVORITE_TEAM).setValue(favoriteTeam).addOnSuccessListener(unused -> Log.i(TAG, "fattoooo team"));
    }

    @Override
    public void saveUserAutoLoginPreferences(String autoLogin, String idToken) {
        databaseReference.child(FIREBASE_USERS_COLLECTION).child(idToken).
                child(SHARED_PREFERENCES_AUTO_LOGIN).setValue(autoLogin).addOnSuccessListener(unused -> Log.i(TAG, "fattoooo auto login"));
    }

    @Override
    public void saveUserNewsSourcePreferences(String newsSource, String newsSourceId, String idToken) {
        if (idToken != null) {
            databaseReference.child(FIREBASE_USERS_COLLECTION).child(idToken).
                    child(Constants.SHARED_PREFERENCES_NEWS_SOURCE).setValue(newsSource);
            databaseReference.child(FIREBASE_USERS_COLLECTION).child(idToken).
                    child(Constants.SHARED_PREFERENCES_NEWS_SOURCE_ID).setValue(newsSourceId)
                    .addOnSuccessListener(unused -> Log.i(TAG, "Saved news source preference to remote DB"));
        }
    }

    @Override
    public Task<Boolean> isAutoLoginEnabled(String idToken) {
        TaskCompletionSource<Boolean> taskCompletionSource = new TaskCompletionSource<>();
        databaseReference.child(FIREBASE_USERS_COLLECTION).child(idToken)
                .child(SHARED_PREFERENCES_AUTO_LOGIN).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String autoLogin = task.getResult().getValue(String.class);
                        taskCompletionSource.setResult(Boolean.parseBoolean(autoLogin));
                    } else {
                        Log.e(TAG, "Failed to get auto_login value", task.getException());
                        taskCompletionSource.setResult(false);
                    }
                });
        return taskCompletionSource.getTask();
    }

    @Override
    public Task<Map<String, Boolean>> getNotificationPreferences(String idToken) {
        TaskCompletionSource<Map<String, Boolean>> taskCompletionSource = new TaskCompletionSource<>();
        databaseReference.child(FIREBASE_USERS_COLLECTION).child(idToken)
                .child("notification_preferences").get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        Map<String, Boolean> prefs = new HashMap<>();
                        for (DataSnapshot child : task.getResult().getChildren()) {
                            String key = child.getKey();
                            Boolean val = child.getValue(Boolean.class);
                            if (key != null && val != null) {
                                prefs.put(key, val);
                            }
                        }
                        taskCompletionSource.setResult(prefs);
                    } else if (task.isSuccessful()) {
                        taskCompletionSource.setResult(new HashMap<>());
                    } else {
                        Log.e(TAG, "Failed to get notification_preferences", task.getException());
                        taskCompletionSource.setException(task.getException() != null ? task.getException() : new Exception("Failed to get notification preferences"));
                    }
                });
        return taskCompletionSource.getTask();
    }

    @Override
    public void saveUserNotificationPreference(String key, boolean value, String idToken) {
        if (idToken != null) {
            databaseReference.child(FIREBASE_USERS_COLLECTION).child(idToken)
                    .child("notification_preferences").child(key).setValue(value)
                    .addOnSuccessListener(unused -> Log.i(TAG, "Saved notification preference " + key + "=" + value + " to remote DB"));
        }
    }

}
