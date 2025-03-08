package com.the_coffe_coders.fastestlap.data.repository.user;

import com.the_coffe_coders.fastestlap.domain.user.User;

public interface UserResponseCallback {
    void onSuccessFromAuthentication(User user);

    void onFailureFromAuthentication(String message);

    void onSuccessFromRemoteDatabase(User user);

    void onSuccessFromGettingUserPreferences();

    void onFailureFromRemoteDatabase(String message);

    void onSuccessLogout();
}
