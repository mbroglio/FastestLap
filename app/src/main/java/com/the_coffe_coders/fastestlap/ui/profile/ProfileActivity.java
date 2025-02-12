package com.the_coffe_coders.fastestlap.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ScrollView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.user.User;
import com.the_coffe_coders.fastestlap.repository.user.IUserRepository;
import com.the_coffe_coders.fastestlap.ui.home.HomePageActivity;
import com.the_coffe_coders.fastestlap.ui.welcome.WelcomeActivity;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModel;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModelFactory;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;
import com.the_coffe_coders.fastestlap.util.SharedPreferencesUtils;
import com.the_coffe_coders.fastestlap.util.UIUtils;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";
    private final boolean isEmailModified = false;
    SharedPreferencesUtils sharedPreferencesUtils = new SharedPreferencesUtils(this);
    User currentUser;
    private GestureDetector tapDetector;
    private String originalEmail;
    private TextInputEditText emailText;
    private CheckBox autoLoginCheckBox;
    private Button saveButton;
    private Button dismissButton;
    private Button signOutButton;
    private String autoLoginKey;
    private boolean isCheckBoxChanged = false;

    private boolean isFromLogin;

    private IUserRepository userRepository;
    private UserViewModel userViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);

        UIUtils.applyWindowInsets(toolbar);

        ScrollView scrollView = findViewById(R.id.profile_layout);
        UIUtils.applyWindowInsets(scrollView);

        isFromLogin = getIntent().getBooleanExtra("from_login", false);
        Log.i(TAG, "from login: " + isFromLogin);


        if (isFromLogin) {
            toolbar.setNavigationOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, HomePageActivity.class);
                startActivity(intent);
            });
        } else {
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }


        userRepository = ServiceLocator.getInstance().getUserRepository(getApplication());
        userViewModel = new ViewModelProvider(getViewModelStore(), new UserViewModelFactory(userRepository)).get(UserViewModel.class);

        currentUser = userViewModel.getLoggedUser();

        autoLoginCheckBox = findViewById(R.id.remember_me_checkbox);
        saveButton = findViewById(R.id.save_button);
        dismissButton = findViewById(R.id.dismiss_button);
        signOutButton = findViewById(R.id.sign_out_button);

        setAutoLoginCheckBox();
        boolean autoLogin = autoLoginCheckBox.isChecked();
        Log.i(TAG, "Auto Login status: " + autoLogin);

        autoLoginCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.i(TAG, "Auto Login status on click: " + autoLoginCheckBox.isChecked());
            if (autoLoginCheckBox.isChecked() != autoLogin) {
                isCheckBoxChanged = true;
                checkForChanges();
            } else {
                isCheckBoxChanged = false;
                checkForChanges();
            }
        });

        SharedPreferencesUtils sharedPreferencesUtils = new SharedPreferencesUtils(this);
        sharedPreferencesUtils.readStringData(Constants.SHARED_PREFERENCES_FILENAME, Constants.SHARED_PREFERENCES_AUTO_LOGIN);

        emailText = findViewById(R.id.email_text);
        emailText.setText(userViewModel.getLoggedUser().getEmail());

        originalEmail = emailText.getText().toString();

        dismissButton.setOnClickListener(v -> {
            checkForChanges();
        });

        signOutButton.setOnClickListener(v -> {
            userViewModel.logout();
            Intent intent = new Intent(ProfileActivity.this, WelcomeActivity.class);
            startActivity(intent);
        });
    }

    private void checkForChanges() {
        boolean hasChanges = isEmailModified || isCheckBoxChanged;

        saveButton.setVisibility(hasChanges ? View.VISIBLE : View.INVISIBLE);
        saveButton.setEnabled(hasChanges);
        dismissButton.setVisibility(hasChanges ? View.VISIBLE : View.INVISIBLE);
        dismissButton.setEnabled(hasChanges);

        saveButton.setOnClickListener(v -> {
            updatePreferences();
        });
    }

    private void updatePreferences() {
        saveSharedPreferences(sharedPreferencesUtils, currentUser);

        Intent intent = new Intent(ProfileActivity.this, HomePageActivity.class);
        startActivity(intent);
    }

    private void saveSharedPreferences(SharedPreferencesUtils sharedPreferencesUtils, User user) {
        Log.i(TAG, "Saving user preferences");

        if (isCheckBoxChanged) {
            sharedPreferencesUtils.writeStringData(Constants.SHARED_PREFERENCES_FILENAME,
                    Constants.SHARED_PREFERENCES_AUTO_LOGIN,
                    String.valueOf(autoLoginCheckBox.isChecked()));
        } else {
            autoLoginKey = getAutoLoginValue();
        }

        userViewModel.saveUserAutoLoginPreferences(String.valueOf(autoLoginCheckBox.isChecked()), user.getIdToken());
    }

    private void setAutoLoginCheckBox() {
        String autoLogin = getAutoLoginValue();
        if (autoLogin != null) {
            autoLoginCheckBox.setChecked(Boolean.parseBoolean(autoLogin));
        }
    }

    private String getAutoLoginValue() {
        String autoLogin = sharedPreferencesUtils.readStringData(Constants.SHARED_PREFERENCES_FILENAME, Constants.SHARED_PREFERENCES_AUTO_LOGIN);
        Log.i(TAG, "Auto Login: " + autoLogin);
        return autoLogin;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}