package com.the_coffe_coders.fastestlap.ui.profile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
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
    SharedPreferencesUtils sharedPreferencesUtils;
    User currentUser;
    private CheckBox autoLoginCheckBox;
    private Button saveButton;
    private Button dismissButton;
    private boolean isCheckBoxChanged = false;
    private boolean initialAutoLoginState = false;
    private boolean isFromLogin;

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

        // Initialize repositories and view models
        IUserRepository userRepository = ServiceLocator.getInstance().getUserRepository(getApplication());
        userViewModel = new ViewModelProvider(getViewModelStore(), new UserViewModelFactory(userRepository)).get(UserViewModel.class);
        sharedPreferencesUtils = new SharedPreferencesUtils(this);

        // Get current user
        currentUser = userViewModel.getLoggedUser();

        // Initialize UI elements
        autoLoginCheckBox = findViewById(R.id.remember_me_checkbox);
        saveButton = findViewById(R.id.save_button);
        dismissButton = findViewById(R.id.dismiss_button);
        Button signOutButton = findViewById(R.id.sign_out_button);
        TextInputEditText emailText = findViewById(R.id.email_text);

        // Set email from current user
        if (currentUser != null && currentUser.getEmail() != null) {
            emailText.setText(currentUser.getEmail());
        }

        // Fetch and set the auto-login preference
        fetchAutoLoginPreference();

        // Set the checkbox listener
        autoLoginCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.i(TAG, "Auto Login status changed to: " + isChecked);
            isCheckBoxChanged = isChecked != initialAutoLoginState;
            checkForChanges();
        });

        dismissButton.setOnClickListener(v -> {
            // Reset checkbox to original state
            autoLoginCheckBox.setChecked(initialAutoLoginState);
            isCheckBoxChanged = false;
            checkForChanges();

            if (isFromLogin) {
                UIUtils.navigateToHomePage(this);
            } else {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        signOutButton.setOnClickListener(v -> {
            userViewModel.logout();
            SharedPreferences sharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILENAME, MODE_PRIVATE);
            sharedPreferences.edit().clear().apply();
            UIUtils.navigateToWelcomePage(this);
            finish();
        });

        MaterialSwitch languageSwitch = findViewById(R.id.language_switch);
        LocaleListCompat appLocales = AppCompatDelegate.getApplicationLocales();
        String currentLanguage = appLocales.toLanguageTags();
        languageSwitch.setChecked(currentLanguage.equals("en-GB"));

        languageSwitch.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if (languageSwitch.isChecked()) {
                setLocale("en-GB");
            } else {
                setLocale("it-IT");
            }
        }));

        // Hide action buttons initially
        saveButton.setVisibility(View.INVISIBLE);
        dismissButton.setVisibility(View.INVISIBLE);
    }

    private void setLocale(String languageCode) {
        LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(languageCode);
        AppCompatDelegate.setApplicationLocales(appLocale);
        Toast.makeText(this, getString(R.string.language_set, languageCode), Toast.LENGTH_SHORT).show();
    }

    /**
     * Fetches the auto-login preference from both SharedPreferences and Firebase
     */
    private void fetchAutoLoginPreference() {
        // First check local SharedPreferences
        String localAutoLogin = sharedPreferencesUtils.readStringData(Constants.SHARED_PREFERENCES_FILENAME, Constants.SHARED_PREFERENCES_AUTO_LOGIN);

        Log.i(TAG, "Local SharedPreferences Auto Login: " + localAutoLogin);

        // Set initial value from local preference if available
        if (localAutoLogin != null && !localAutoLogin.isEmpty()) {
            initialAutoLoginState = Boolean.parseBoolean(localAutoLogin);
            autoLoginCheckBox.setChecked(initialAutoLoginState);
        }

        // Then check Firebase for the most up-to-date preference
        if (currentUser != null && currentUser.getIdToken() != null) {
            // Using the Task-based method from the ViewModel
            userViewModel.isAutoLoginEnabled(currentUser.getIdToken()).addOnSuccessListener(isEnabled -> {
                Log.i(TAG, "Firebase Auto Login Preference: " + isEnabled);

                // Update the checkbox and state if remote value is different
                if (isEnabled != autoLoginCheckBox.isChecked()) {
                    initialAutoLoginState = isEnabled;
                    autoLoginCheckBox.setChecked(isEnabled);

                    // Also update local preferences to stay in sync
                    sharedPreferencesUtils.writeStringData(Constants.SHARED_PREFERENCES_FILENAME, Constants.SHARED_PREFERENCES_AUTO_LOGIN, String.valueOf(isEnabled));
                }
                isCheckBoxChanged = false;
                checkForChanges();
            }).addOnFailureListener(e -> Log.e(TAG, "Failed to fetch auto login preference from Firebase", e));
        }
    }

    private void checkForChanges() {
        boolean hasChanges = isCheckBoxChanged;

        saveButton.setVisibility(hasChanges ? View.VISIBLE : View.INVISIBLE);
        saveButton.setEnabled(hasChanges);
        dismissButton.setVisibility(hasChanges ? View.VISIBLE : View.INVISIBLE);
        dismissButton.setEnabled(hasChanges);

        saveButton.setOnClickListener(v -> updatePreferences());
    }

    private void updatePreferences() {
        boolean autoLoginValue = autoLoginCheckBox.isChecked();
        Log.i(TAG, "Saving auto login preference: " + autoLoginValue);

        // Update local preferences
        sharedPreferencesUtils.writeStringData(Constants.SHARED_PREFERENCES_FILENAME, Constants.SHARED_PREFERENCES_AUTO_LOGIN, String.valueOf(autoLoginValue));

        // Update remote preferences if user is logged in
        if (currentUser != null && currentUser.getIdToken() != null) {
            userViewModel.saveUserAutoLoginPreferences(String.valueOf(autoLoginValue), currentUser.getIdToken());
        }

        // Reset change flag
        initialAutoLoginState = autoLoginValue;
        isCheckBoxChanged = false;

        // Return to previous screen
        if (isFromLogin) {
            UIUtils.navigateToHomePage(this);
        } else {
            getOnBackPressedDispatcher().onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh preferences when activity resumes
        fetchAutoLoginPreference();
    }
}