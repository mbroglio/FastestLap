package com.the_coffe_coders.fastestlap.repository.user;

import androidx.lifecycle.MutableLiveData;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.user.User;

import java.util.Set;

public interface IUserRepository {
    MutableLiveData<Result> getUser(String email, String password, boolean isUserRegistered);
    MutableLiveData<Result> getGoogleUser(String idToken);
    MutableLiveData<Result> getUserFavoriteNews(String idToken);
    MutableLiveData<Result> getUserPreferences(String idToken);
    MutableLiveData<Result> logout();
    User getLoggedUser();
    void signUp(String email, String password);
    void signIn(String email, String password);
    void signInWithGoogle(String token);
    void saveUserPreferences(String favoriteCountry, Set<String> favoriteTopics, String idToken);
}
