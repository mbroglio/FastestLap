package com.the_coffe_coders.fastestlap.ui.welcome.fragment;

import static com.the_coffe_coders.fastestlap.util.Constants.UNEXPECTED_ERROR;
import static com.the_coffe_coders.fastestlap.util.Constants.USER_COLLISION_ERROR;
import static com.the_coffe_coders.fastestlap.util.Constants.WEAK_PASSWORD_ERROR;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListPopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.SpinnerAdapter;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.user.User;
import com.the_coffe_coders.fastestlap.repository.user.IUserRepository;
import com.the_coffe_coders.fastestlap.ui.home.HomePageActivity;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModel;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModelFactory;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;
import com.the_coffe_coders.fastestlap.util.SharedPreferencesUtils;

import org.apache.commons.validator.routines.EmailValidator;

import java.util.Objects;

public class SignUpFragment extends DialogFragment {

    private static final String TAG = "RegisterPopup";
    private final String[] drivers = Constants.DRIVER_FULLNAME.keySet().toArray(new String[0]);
    private final String[] constructors = Constants.TEAM_FULLNAME.keySet().toArray(new String[0]);
    private UserViewModel userViewModel;
    private TextInputEditText textInputEmail, textInputPassword;
    private String favouriteDriverKey, favouriteConstructorKey;
    private TextView favouriteDriverText;
    private MaterialCardView changeDriverButton;
    private ListPopupWindow listPopupWindowDrivers;
    private TextView favouriteConstructorText;
    private MaterialCardView changeConstructorButton;
    private ListPopupWindow listPopupWindowConstructors;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign_up, container, false);

        IUserRepository userRepository = ServiceLocator.getInstance().getUserRepository((Application) requireActivity().getApplicationContext());


        userViewModel = new ViewModelProvider(this, new UserViewModelFactory(userRepository)).get(UserViewModel.class);
        userViewModel.setAuthenticationError(false);

        textInputEmail = view.findViewById(R.id.textInputEmail);
        textInputPassword = view.findViewById(R.id.textInputPassword);

        favouriteDriverText = view.findViewById(R.id.favourite_driver_text);
        changeDriverButton = view.findViewById(R.id.change_driver_button);
        changeDriverButton.setOnClickListener(v -> {
            if (listPopupWindowDrivers.isShowing()) {
                listPopupWindowDrivers.dismiss();
            } else {
                listPopupWindowDrivers.show();
            }
        });

        favouriteConstructorText = view.findViewById(R.id.favourite_constructor_text);
        changeConstructorButton = view.findViewById(R.id.change_constructor_button);
        changeConstructorButton.setOnClickListener(v -> {
            if (listPopupWindowConstructors.isShowing()) {
                listPopupWindowConstructors.dismiss();
            } else {
                listPopupWindowConstructors.show();
            }
        });

        listPopupWindowDrivers = createListPopupWindow(drivers, changeDriverButton, true);
        listPopupWindowConstructors = createListPopupWindow(constructors, changeConstructorButton, false);

        Button submitButton = view.findViewById(R.id.submit_button);

        submitButton.setOnClickListener(v -> {
            String email = Objects.requireNonNull(textInputEmail.getText()).toString();
            String password = Objects.requireNonNull(textInputPassword.getText()).toString();
            checkSignUpCredentials(email, password);
        });

        return view;
    }

    private void checkSignUpCredentials(String email, String password) {
        Log.d(TAG, "Email: " + email);
        Log.d(TAG, "Password: " + password);

        if (isEmailOk(email) & isPasswordOk(password)) {
            //binding.progressBar.setVisibility(View.VISIBLE);
            if (!userViewModel.isAuthenticationError()) {
                Log.i(TAG, "Getting user");

                userViewModel.getUserMutableLiveData(email, password, false).observe(
                        this, result -> {
                            if (result.isSuccess()) {
                                User user = ((Result.UserSuccess) result).getData();
                                //saveLoginData(email, password, user.getIdToken());

                                userViewModel.setAuthenticationError(false);
                                Log.i(TAG, "User: " + user.toString());

                                SharedPreferencesUtils sharedPreferencesUtils = new SharedPreferencesUtils(this.getContext());
                                saveSharedPreferences(sharedPreferencesUtils, user);

                                Intent intent = new Intent(requireContext(), HomePageActivity.class);
                                Log.i(TAG, "Starting HomePageActivity");
                                startActivity(intent);
                            } else {
                                userViewModel.setAuthenticationError(true);
                                Toast.makeText(getContext(), "Email already registered", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                userViewModel.getUser(email, password, false);
            }
            //binding.progressBar.setVisibility(View.GONE);
        } else {
            userViewModel.setAuthenticationError(true);
            Snackbar.make(requireView(),
                    "Error Mail Login", Snackbar.LENGTH_SHORT).show();
        }
    }

    private String getErrorMessage(String message) {
        switch (message) {
            case WEAK_PASSWORD_ERROR:
                return WEAK_PASSWORD_ERROR;
            case USER_COLLISION_ERROR:
                return USER_COLLISION_ERROR;
            default:
                return UNEXPECTED_ERROR;
        }
    }

    private void saveSharedPreferences(SharedPreferencesUtils sharedPreferencesUtils, User user) {
        Log.i(TAG, "Saving user preferences");
        Log.i(TAG, "Favourite driver: " + favouriteDriverKey);
        Log.i(TAG, "Favourite constructor: " + favouriteConstructorKey);

        sharedPreferencesUtils.writeStringData(Constants.SHARED_PREFERENCES_FILENAME,
                Constants.SHARED_PREFERENCES_FAVORITE_DRIVER,
                favouriteDriverKey);

        sharedPreferencesUtils.writeStringData(Constants.SHARED_PREFERENCES_FILENAME,
                Constants.SHARED_PREFERENCES_FAVORITE_TEAM,
                favouriteConstructorKey);

        sharedPreferencesUtils.writeStringData(Constants.SHARED_PREFERENCES_FILENAME,
                Constants.SHARED_PREFERENCES_AUTO_LOGIN,
                "false");

        userViewModel.saveUserPreferences(
                favouriteDriverKey,
                favouriteConstructorKey,
                "false",
                user.getIdToken()
        );
    }

    private ListPopupWindow createListPopupWindow(String[] items, MaterialCardView anchorView, boolean isDriver) {
        ListPopupWindow listPopupWindow = new ListPopupWindow(requireContext());
        listPopupWindow.setAdapter(new SpinnerAdapter(requireContext(), items, isDriver));
        listPopupWindow.setAnchorView(anchorView);
        listPopupWindow.setWidth((int) (220 * getResources().getDisplayMetrics().density)); // Set the width to 220dp
        listPopupWindow.setHeight((int) (250 * getResources().getDisplayMetrics().density)); // Set the height to 250dp
        listPopupWindow.setBackgroundDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.round_background_grey));
        listPopupWindow.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItemKey = items[position];
            if (isDriver) {
                favouriteDriverKey = selectedItemKey;
                favouriteDriverText.setText(getString(Constants.DRIVER_FULLNAME.get(favouriteDriverKey)));

            } else {
                favouriteConstructorKey = selectedItemKey;
                favouriteConstructorText.setText(getString(Constants.TEAM_FULLNAME.get(favouriteConstructorKey)));
            }
            listPopupWindow.dismiss();
        });
        return listPopupWindow;
    }

    private boolean isEmailOk(String email) {
        // Check if the email is valid through the use of this library:
        // https://commons.apache.org/proper/commons-validator/
        if (!EmailValidator.getInstance().isValid((email))) {
            //emailEditText.setError("Error Email Login");
            Toast.makeText(getContext(), "Invalid email", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            textInputEmail.setError(null);
            return true;
        }
    }

    private boolean isPasswordOk(String password) {
        // Check if the password length is correct
        if (password.isEmpty() || password.length() < Constants.MINIMUM_LENGTH_PASSWORD) {
            //passwordEditText.setError("Error Password Login");
            Toast.makeText(getContext(), "Invalid password", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            textInputPassword.setError(null);
            return true;
        }
    }
}