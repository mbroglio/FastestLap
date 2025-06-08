package com.the_coffe_coders.fastestlap.repository.user;

import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Task;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.user.User;
import com.the_coffe_coders.fastestlap.source.user.BaseUserAuthenticationRemoteDataSource;
import com.the_coffe_coders.fastestlap.source.user.BaseUserDataRemoteDataSource;

public class UserRepository implements IUserRepository, UserResponseCallback {
    private static final String TAG = UserRepository.class.getSimpleName();
    private final MutableLiveData<Result> userMutableLiveData;
    private final MutableLiveData<Result> userPreferencesMutableLiveData;
    private final BaseUserAuthenticationRemoteDataSource userRemoteDataSource;
    private final BaseUserDataRemoteDataSource userDataRemoteDataSource;

    public UserRepository(BaseUserAuthenticationRemoteDataSource userRemoteDataSource,
                          BaseUserDataRemoteDataSource userDataRemoteDataSource) {
        this.userRemoteDataSource = userRemoteDataSource;
        this.userDataRemoteDataSource = userDataRemoteDataSource;
        this.userMutableLiveData = new MutableLiveData<>();
        this.userPreferencesMutableLiveData = new MutableLiveData<>();
        this.userRemoteDataSource.setUserResponseCallback(this);
        this.userDataRemoteDataSource.setUserResponseCallback(this);
    }

    @Override
    public MutableLiveData<Result> getUser(String email, String password, boolean isUserRegistered) {
        if (isUserRegistered) {
            signIn(email, password);
        } else {
            signUp(email, password);
        }
        return userMutableLiveData;
    }

    @Override
    public MutableLiveData<Result> getGoogleUser(String idToken) {
        signInWithGoogle(idToken);
        return userMutableLiveData;
    }

    @Override
    public MutableLiveData<Result> getUserPreferences(String idToken) {
        userDataRemoteDataSource.getUserPreferences(idToken);
        return userPreferencesMutableLiveData;
    }

    @Override
    public com.the_coffe_coders.fastestlap.domain.user.User getLoggedUser() {
        return userRemoteDataSource.getLoggedUser();
    }

    @Override
    public MutableLiveData<Result> logout() {
        userRemoteDataSource.logout();
        return userMutableLiveData;
    }

    @Override
    public void signUp(String email, String password) {
        userRemoteDataSource.signUp(email, password);
    }

    @Override
    public void signIn(String email, String password) {
        userRemoteDataSource.signIn(email, password);
    }

    @Override
    public void signInWithGoogle(String token) {
        userRemoteDataSource.signInWithGoogle(token);
    }

    @Override
    public void saveUserPreferences(String favoriteDriver, String favoriteTeam, String autoLogin, String idToken) {
        userDataRemoteDataSource.saveUserPreferences(favoriteDriver, favoriteTeam, autoLogin, idToken);
    }

    @Override
    public void saveUserDriverPreferences(String favoriteDriver, String idToken) {
        userDataRemoteDataSource.saveUserDriverPreferences(favoriteDriver, idToken);
    }

    @Override
    public void saveUserConstructorPreferences(String favoriteTeam, String idToken) {
        userDataRemoteDataSource.saveUserConstructorPreferences(favoriteTeam, idToken);
    }


    @Override
    public void saveUserAutoLoginPreferences(String autoLogin, String idToken) {
        userDataRemoteDataSource.saveUserAutoLoginPreferences(autoLogin, idToken);
    }

    @Override
    public Task<Boolean> isAutoLoginEnabled(String idToken) {
        return userDataRemoteDataSource.isAutoLoginEnabled(idToken);
    }

    @Override
    public void onSuccessFromAuthentication(User user) {
        if (user != null) {
            userDataRemoteDataSource.saveUserData(user);
        }
    }

    @Override
    public void onFailureFromAuthentication(String message) {
        Result.Error result = new Result.Error(message);
        userMutableLiveData.postValue(result);
    }

    @Override
    public void onSuccessFromRemoteDatabase(com.the_coffe_coders.fastestlap.domain.user.User user) {
        Result.UserSuccess result = new Result.UserSuccess(user);
        userMutableLiveData.postValue(result);
    }

    @Override
    public void onSuccessFromGettingUserPreferences() {
        userPreferencesMutableLiveData.postValue(new Result.UserSuccess(null));
    }

    @Override
    public void onFailureFromRemoteDatabase(String message) {
        Result.Error result = new Result.Error(message);
        userMutableLiveData.postValue(result);
    }

    @Override
    public void onSuccessLogout() {

    }
}
