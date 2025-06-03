package com.the_coffe_coders.fastestlap.ui.welcome.viewmodel;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.Task;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.user.User;
import com.the_coffe_coders.fastestlap.repository.user.IUserRepository;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.SharedPreferencesUtils;

import lombok.Getter;
import lombok.Setter;


public class UserViewModel extends ViewModel {
    private static final String TAG = UserViewModel.class.getSimpleName();

    private final IUserRepository userRepository;
    private MutableLiveData<Result> userMutableLiveData;
    private MutableLiveData<Result> userPreferencesMutableLiveData;
    @Setter
    @Getter
    private boolean authenticationError;

    public UserViewModel(IUserRepository userRepository) {
        this.userRepository = userRepository;
        authenticationError = false;
    }

    public MutableLiveData<Result> getUserMutableLiveData(
            String email, String password, boolean isUserRegistered) {
        if (userMutableLiveData == null) {
            getUserData(email, password, isUserRegistered);
        }
        return userMutableLiveData;
    }

    public void saveUserPreferences(String favoriteDriver, String favoriteTeam, String autoLogin, String idToken) {
        if (idToken != null) {
            userRepository.saveUserPreferences(favoriteDriver, favoriteTeam, autoLogin, idToken);
        }
    }

    public void saveUserDriverPreferences(String favoriteDriver, String idToken) {
        if (idToken != null) {
            userRepository.saveUserDriverPreferences(favoriteDriver, idToken);
        }
    }

    public void saveUserConstructorPreferences(String favoriteTeam, String idToken) {
        if (idToken != null) {
            userRepository.saveUserConstructorPreferences(favoriteTeam, idToken);
        }
    }

    public void saveUserAutoLoginPreferences(String autoLogin, String idToken) {
        if (idToken != null) {
            userRepository.saveUserAutoLoginPreferences(autoLogin, idToken);
        }
    }

    public Task<Boolean> isAutoLoginEnabled(String idToken) {
        return userRepository.isAutoLoginEnabled(idToken);
    }

    public MutableLiveData<Result> getUserPreferences(String idToken) {
        if (idToken != null) {
            userPreferencesMutableLiveData = userRepository.getUserPreferences(idToken);
        }
        return userPreferencesMutableLiveData;
    }

    public User getLoggedUser() {
        return userRepository.getLoggedUser();
    }

    public MutableLiveData<Result> logout() {
        if (userMutableLiveData == null) {
            userMutableLiveData = userRepository.logout();
        } else {
            userRepository.logout();
        }

        return userMutableLiveData;
    }

    public void getUser(String email, String password, boolean isUserRegistered) {
        userRepository.getUser(email, password, isUserRegistered);
    }

    private void getUserData(String email, String password, boolean isUserRegistered) {
        userMutableLiveData = userRepository.getUser(email, password, isUserRegistered);
    }

    public String getFavoriteDriverId(Context context) {
        SharedPreferencesUtils sharedPreferencesUtils = new SharedPreferencesUtils(context);
        // Check in SharedPreferences
        String driverId = sharedPreferencesUtils.readStringData(
                Constants.SHARED_PREFERENCES_FILENAME,
                Constants.SHARED_PREFERENCES_FAVORITE_DRIVER);

        if (driverId == null || driverId.isEmpty() || driverId.equals("null")) {
            // Not found in SharedPreferences, try to fetch from remote DB
            User loggedUser = getLoggedUser();
            if (loggedUser != null && loggedUser.getIdToken() != null) {
                Log.i(TAG, "Fetching favorite driver ID from remote database");
                getUserPreferences(loggedUser.getIdToken()).observeForever(result -> {
                    // Use observeForever temporarily and remove observer once we get the result
                    if (result.isSuccess()) {
                        String fetchedDriverId = sharedPreferencesUtils.readStringData(
                                Constants.SHARED_PREFERENCES_FILENAME,
                                Constants.SHARED_PREFERENCES_FAVORITE_DRIVER);
                        Log.i(TAG, "Favorite Driver ID from DB: " + fetchedDriverId);
                    } else {
                        Log.e(TAG, "Error fetching user preferences from DB");
                    }
                });
            } else {
                Log.i(TAG, "No logged user found, cannot fetch favorite driver ID from DB");
            }
        }

        Log.i(TAG, "Favorite Driver ID: " + driverId);
        return driverId;
    }

    public String getFavoriteTeamId(Context context) {
        SharedPreferencesUtils sharedPreferencesUtils = new SharedPreferencesUtils(context);
        // Check in SharedPreferences
        String teamId = sharedPreferencesUtils.readStringData(
                Constants.SHARED_PREFERENCES_FILENAME,
                Constants.SHARED_PREFERENCES_FAVORITE_TEAM);

        if (teamId == null || teamId.isEmpty() || teamId.equals("null")) {
            // Not found in SharedPreferences, try to fetch from remote DB
            User loggedUser = getLoggedUser();
            if (loggedUser != null && loggedUser.getIdToken() != null) {
                Log.i(TAG, "Fetching favorite team ID from remote database");
                getUserPreferences(loggedUser.getIdToken()).observeForever(result -> {
                    // Use observeForever temporarily and remove observer once we get the result
                    if (result.isSuccess()) {
                        String fetchedTeamId = sharedPreferencesUtils.readStringData(
                                Constants.SHARED_PREFERENCES_FILENAME,
                                Constants.SHARED_PREFERENCES_FAVORITE_TEAM);
                        Log.i(TAG, "Favorite Team ID from DB: " + fetchedTeamId);
                    } else {
                        Log.e(TAG, "Error fetching user preferences from DB");
                    }
                });
            } else {
                Log.i(TAG, "No logged user found, cannot fetch favorite team ID from DB");
            }
        }

        Log.i(TAG, "Favorite Team ID: " + teamId);
        return teamId;
    }
}
