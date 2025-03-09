package com.the_coffe_coders.fastestlap.source.user;

import com.google.android.gms.tasks.Task;
import com.the_coffe_coders.fastestlap.domain.user.User;
import com.the_coffe_coders.fastestlap.repository.user.UserResponseCallback;

public abstract class BaseUserDataRemoteDataSource {
    protected UserResponseCallback userResponseCallback;

    public void setUserResponseCallback(UserResponseCallback userResponseCallback) {
        this.userResponseCallback = userResponseCallback;
    }

    public abstract void saveUserData(User user);

    public abstract void getUserPreferences(String idToken);

    public abstract void saveUserPreferences(String favoriteDriver, String favoriteTeam, String autoLogin, String idToken);

    public abstract Task<Boolean> isAutoLoginEnabled(String idToken);
}

