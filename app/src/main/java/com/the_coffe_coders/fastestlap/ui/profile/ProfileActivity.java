package com.the_coffe_coders.fastestlap.ui.profile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.user.User;
import com.the_coffe_coders.fastestlap.repository.user.IUserRepository;
import com.the_coffe_coders.fastestlap.ui.home.HomePageActivity;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModel;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModelFactory;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.notification.AppNotificationManager;
import com.the_coffe_coders.fastestlap.util.service.NetworkUtils;
import com.the_coffe_coders.fastestlap.util.service.ServiceLocator;
import com.the_coffe_coders.fastestlap.util.service.SharedPreferencesUtils;
import com.the_coffe_coders.fastestlap.util.ui.NavigationUtils;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";
    SharedPreferencesUtils sharedPreferencesUtils;
    User currentUser;
    private CheckBox autoLoginCheckBox;
    private Button saveButton;
    private Button dismissButton;
    private TextView notificationPermissionStatus;
    private boolean isCheckBoxChanged = false;
    private boolean initialAutoLoginState = false;
    private boolean isFromLogin;

    private UserViewModel userViewModel;
    private NetworkUtils networkLiveData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        networkLiveData = new NetworkUtils(this);

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

        profileAccessButtons();



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
                NavigationUtils.navigateToHomePage(this);
            } else {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });


        LocaleListCompat appLocales = AppCompatDelegate.getApplicationLocales();
        String currentLanguage = appLocales.toLanguageTags();
        boolean isEnglish = currentLanguage.toLowerCase(java.util.Locale.ROOT).startsWith("en");
        if (currentLanguage.isEmpty()) {
            isEnglish = getResources().getConfiguration().getLocales().get(0).getLanguage().toLowerCase(java.util.Locale.ROOT).startsWith("en");
        }
        MaterialSwitch languageSwitch = findViewById(R.id.language_switch);
        languageSwitch.setChecked(isEnglish);

        languageSwitch.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if (languageSwitch.isChecked()) {
                setLocale("en-GB");
            } else {
                setLocale("it-IT");
            }
        }));

        // Setup Session Notification preferences (F1, F2, F3)
        setupSessionNotificationSwitches();

        // Hide action buttons initially
        saveButton.setVisibility(View.INVISIBLE);
        dismissButton.setVisibility(View.INVISIBLE);
    }

    private void profileAccessButtons() {
        Button signOutButton = findViewById(R.id.sign_out_button);
        Button loginButton = findViewById(R.id.login_button);

        if (networkLiveData.isConnected() && userViewModel.getLoggedUser() != null) {
            signOutButton.setVisibility(View.VISIBLE);
            loginButton.setVisibility(View.GONE);

            signOutButton.setOnClickListener(v -> {
                userViewModel.logout();
                SharedPreferences sharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILENAME, MODE_PRIVATE);
                sharedPreferences.edit().clear().apply();
                NavigationUtils.navigateToWelcomePage(this);
                finish();
            });
        } else {
            signOutButton.setVisibility(View.GONE);
            autoLoginCheckBox.setVisibility(View.GONE);
            loginButton.setVisibility(View.VISIBLE);
            loginButton.setOnClickListener(v ->
                    NavigationUtils.showProfileManageDialogs(getSupportFragmentManager(), 2, currentUser.getEmail()));

        }


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
            NavigationUtils.navigateToHomePage(this);
        } else {
            getOnBackPressedDispatcher().onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh preferences when activity resumes
        fetchAutoLoginPreference();
        //updateNotificationPermissionStatus();
    }

    private void setupSessionNotificationSwitches() {
        SharedPreferences prefs = getSharedPreferences(Constants.SHARED_PREFERENCES_FILENAME, MODE_PRIVATE);

        // F1 Switches
        MaterialSwitch f1RaceSwitch = findViewById(R.id.f1_notif_race_switch);
        MaterialSwitch f1QualiSwitch = findViewById(R.id.f1_notif_qualifying_switch);
        MaterialSwitch f1SprintSwitch = findViewById(R.id.f1_notif_sprint_switch);
        MaterialSwitch f1PracticeSwitch = findViewById(R.id.f1_notif_practice_switch);

        // F2 Switches & Container
        MaterialSwitch f2MasterSwitch = findViewById(R.id.f2_notif_master_switch);
        android.widget.LinearLayout f2SessionsContainer = findViewById(R.id.f2_sessions_container);
        MaterialSwitch f2FeatureSwitch = findViewById(R.id.f2_notif_feature_switch);
        MaterialSwitch f2SprintSwitch = findViewById(R.id.f2_notif_sprint_switch);
        MaterialSwitch f2QualiSwitch = findViewById(R.id.f2_notif_qualifying_switch);
        MaterialSwitch f2PracticeSwitch = findViewById(R.id.f2_notif_practice_switch);

        // F3 Switches & Container
        MaterialSwitch f3MasterSwitch = findViewById(R.id.f3_notif_master_switch);
        android.widget.LinearLayout f3SessionsContainer = findViewById(R.id.f3_sessions_container);
        MaterialSwitch f3FeatureSwitch = findViewById(R.id.f3_notif_feature_switch);
        MaterialSwitch f3SprintSwitch = findViewById(R.id.f3_notif_sprint_switch);
        MaterialSwitch f3QualiSwitch = findViewById(R.id.f3_notif_qualifying_switch);
        MaterialSwitch f3PracticeSwitch = findViewById(R.id.f3_notif_practice_switch);

        // Dropdown Headers and Arrows
        View f1DropdownHeader = findViewById(R.id.f1_dropdown_header);
        android.widget.ImageView f1DropdownArrow = findViewById(R.id.f1_dropdown_arrow);
        android.widget.LinearLayout f1SessionsContainer = findViewById(R.id.f1_sessions_container);

        View f2DropdownHeader = findViewById(R.id.f2_dropdown_header);
        android.widget.ImageView f2DropdownArrow = findViewById(R.id.f2_dropdown_arrow);

        View f3DropdownHeader = findViewById(R.id.f3_dropdown_header);
        android.widget.ImageView f3DropdownArrow = findViewById(R.id.f3_dropdown_arrow);

        // F1 is expanded by default
        if (f1SessionsContainer != null) f1SessionsContainer.setVisibility(View.VISIBLE);
        if (f1DropdownArrow != null) f1DropdownArrow.setRotation(180f);
        if (f1DropdownHeader != null && f1SessionsContainer != null && f1DropdownArrow != null) {
            f1DropdownHeader.setOnClickListener(v -> {
                boolean isExpanded = f1SessionsContainer.getVisibility() == View.VISIBLE;
                f1SessionsContainer.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
                f1DropdownArrow.animate().rotation(isExpanded ? 0f : 180f).setDuration(200).start();
            });
        }

        // F2 is hidden by default as a drop down menu
        if (f2SessionsContainer != null) f2SessionsContainer.setVisibility(View.GONE);
        if (f2DropdownArrow != null) f2DropdownArrow.setRotation(0f);
        if (f2DropdownHeader != null && f2SessionsContainer != null && f2DropdownArrow != null) {
            f2DropdownHeader.setOnClickListener(v -> {
                boolean isExpanded = f2SessionsContainer.getVisibility() == View.VISIBLE;
                f2SessionsContainer.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
                f2DropdownArrow.animate().rotation(isExpanded ? 0f : 180f).setDuration(200).start();
            });
        }

        // F3 is hidden by default as a drop down menu
        if (f3SessionsContainer != null) f3SessionsContainer.setVisibility(View.GONE);
        if (f3DropdownArrow != null) f3DropdownArrow.setRotation(0f);
        if (f3DropdownHeader != null && f3SessionsContainer != null && f3DropdownArrow != null) {
            f3DropdownHeader.setOnClickListener(v -> {
                boolean isExpanded = f3SessionsContainer.getVisibility() == View.VISIBLE;
                f3SessionsContainer.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
                f3DropdownArrow.animate().rotation(isExpanded ? 0f : 180f).setDuration(200).start();
            });
        }

        // Set initial states from preferences
        if (f1RaceSwitch != null) f1RaceSwitch.setChecked(prefs.getBoolean(Constants.PREF_NOTIF_F1_RACE, true));
        if (f1QualiSwitch != null) f1QualiSwitch.setChecked(prefs.getBoolean(Constants.PREF_NOTIF_F1_QUALIFYING, true));
        if (f1SprintSwitch != null) f1SprintSwitch.setChecked(prefs.getBoolean(Constants.PREF_NOTIF_F1_SPRINT, true));
        if (f1PracticeSwitch != null) f1PracticeSwitch.setChecked(prefs.getBoolean(Constants.PREF_NOTIF_F1_PRACTICE, true));

        boolean f2Enabled = prefs.getBoolean(Constants.PREF_NOTIF_F2_ENABLED, false);
        if (f2MasterSwitch != null) f2MasterSwitch.setChecked(f2Enabled);
        if (f2FeatureSwitch != null) f2FeatureSwitch.setChecked(prefs.getBoolean(Constants.PREF_NOTIF_F2_FEATURE, true));
        if (f2SprintSwitch != null) f2SprintSwitch.setChecked(prefs.getBoolean(Constants.PREF_NOTIF_F2_SPRINT, true));
        if (f2QualiSwitch != null) f2QualiSwitch.setChecked(prefs.getBoolean(Constants.PREF_NOTIF_F2_QUALIFYING, true));
        if (f2PracticeSwitch != null) f2PracticeSwitch.setChecked(prefs.getBoolean(Constants.PREF_NOTIF_F2_PRACTICE, false));
        updateSubSwitchesState(f2SessionsContainer, f2Enabled, f2FeatureSwitch, f2SprintSwitch, f2QualiSwitch, f2PracticeSwitch);

        boolean f3Enabled = prefs.getBoolean(Constants.PREF_NOTIF_F3_ENABLED, false);
        if (f3MasterSwitch != null) f3MasterSwitch.setChecked(f3Enabled);
        if (f3FeatureSwitch != null) f3FeatureSwitch.setChecked(prefs.getBoolean(Constants.PREF_NOTIF_F3_FEATURE, true));
        if (f3SprintSwitch != null) f3SprintSwitch.setChecked(prefs.getBoolean(Constants.PREF_NOTIF_F3_SPRINT, true));
        if (f3QualiSwitch != null) f3QualiSwitch.setChecked(prefs.getBoolean(Constants.PREF_NOTIF_F3_QUALIFYING, true));
        if (f3PracticeSwitch != null) f3PracticeSwitch.setChecked(prefs.getBoolean(Constants.PREF_NOTIF_F3_PRACTICE, false));
        updateSubSwitchesState(f3SessionsContainer, f3Enabled, f3FeatureSwitch, f3SprintSwitch, f3QualiSwitch, f3PracticeSwitch);

        // Listener helper lambda
        java.util.function.BiConsumer<String, Boolean> onPrefChanged = (key, value) -> {
            prefs.edit().putBoolean(key, value).apply();
            AppNotificationManager.getInstance().syncSessionTopicSubscriptions(ProfileActivity.this);
        };

        if (f1RaceSwitch != null) f1RaceSwitch.setOnCheckedChangeListener((v, c) -> onPrefChanged.accept(Constants.PREF_NOTIF_F1_RACE, c));
        if (f1QualiSwitch != null) f1QualiSwitch.setOnCheckedChangeListener((v, c) -> onPrefChanged.accept(Constants.PREF_NOTIF_F1_QUALIFYING, c));
        if (f1SprintSwitch != null) f1SprintSwitch.setOnCheckedChangeListener((v, c) -> onPrefChanged.accept(Constants.PREF_NOTIF_F1_SPRINT, c));
        if (f1PracticeSwitch != null) f1PracticeSwitch.setOnCheckedChangeListener((v, c) -> onPrefChanged.accept(Constants.PREF_NOTIF_F1_PRACTICE, c));

        if (f2MasterSwitch != null) f2MasterSwitch.setOnCheckedChangeListener((v, c) -> {
            onPrefChanged.accept(Constants.PREF_NOTIF_F2_ENABLED, c);
            updateSubSwitchesState(f2SessionsContainer, c, f2FeatureSwitch, f2SprintSwitch, f2QualiSwitch, f2PracticeSwitch);
        });
        if (f2FeatureSwitch != null) f2FeatureSwitch.setOnCheckedChangeListener((v, c) -> onPrefChanged.accept(Constants.PREF_NOTIF_F2_FEATURE, c));
        if (f2SprintSwitch != null) f2SprintSwitch.setOnCheckedChangeListener((v, c) -> onPrefChanged.accept(Constants.PREF_NOTIF_F2_SPRINT, c));
        if (f2QualiSwitch != null) f2QualiSwitch.setOnCheckedChangeListener((v, c) -> onPrefChanged.accept(Constants.PREF_NOTIF_F2_QUALIFYING, c));
        if (f2PracticeSwitch != null) f2PracticeSwitch.setOnCheckedChangeListener((v, c) -> onPrefChanged.accept(Constants.PREF_NOTIF_F2_PRACTICE, c));

        if (f3MasterSwitch != null) f3MasterSwitch.setOnCheckedChangeListener((v, c) -> {
            onPrefChanged.accept(Constants.PREF_NOTIF_F3_ENABLED, c);
            updateSubSwitchesState(f3SessionsContainer, c, f3FeatureSwitch, f3SprintSwitch, f3QualiSwitch, f3PracticeSwitch);
        });
        if (f3FeatureSwitch != null) f3FeatureSwitch.setOnCheckedChangeListener((v, c) -> onPrefChanged.accept(Constants.PREF_NOTIF_F3_FEATURE, c));
        if (f3SprintSwitch != null) f3SprintSwitch.setOnCheckedChangeListener((v, c) -> onPrefChanged.accept(Constants.PREF_NOTIF_F3_SPRINT, c));
        if (f3QualiSwitch != null) f3QualiSwitch.setOnCheckedChangeListener((v, c) -> onPrefChanged.accept(Constants.PREF_NOTIF_F3_QUALIFYING, c));
        if (f3PracticeSwitch != null) f3PracticeSwitch.setOnCheckedChangeListener((v, c) -> onPrefChanged.accept(Constants.PREF_NOTIF_F3_PRACTICE, c));
    }

    private void updateSubSwitchesState(View container, boolean isEnabled, MaterialSwitch... switches) {
        if (container != null) {
            container.setAlpha(isEnabled ? 1.0f : 0.5f);
        }
        if (switches != null) {
            for (MaterialSwitch sw : switches) {
                if (sw != null) sw.setEnabled(isEnabled);
            }
        }
    }

    /*
    private void setupNotificationSection() {
        notificationPermissionStatus = findViewById(R.id.notification_permission_status);
        Button testNotificationButton = findViewById(R.id.test_notification_button);

        if (testNotificationButton != null) {
            testNotificationButton.setOnClickListener(v ->
                    AppNotificationManager.getInstance().sendTestNotification(this)
            );
        }

        updateNotificationPermissionStatus();
    }


    private void updateNotificationPermissionStatus() {
        if (notificationPermissionStatus == null) return;
        boolean hasPermission = AppNotificationManager.getInstance().hasNotificationPermission(this);
        if (hasPermission) {
            notificationPermissionStatus.setText(R.string.notification_status_granted);
            notificationPermissionStatus.setTextColor(ContextCompat.getColor(this, R.color.status_green));
            notificationPermissionStatus.setOnClickListener(v ->
                    AppNotificationManager.getInstance().openNotificationSettings(this)
            );
        } else {
            notificationPermissionStatus.setText(R.string.notification_status_denied);
            notificationPermissionStatus.setTextColor(ContextCompat.getColor(this, R.color.app_primary_red));
            notificationPermissionStatus.setOnClickListener(v -> {
                AppNotificationManager.getInstance().requestNotificationPermission(this);
                AppNotificationManager.getInstance().openNotificationSettings(this);
            });
        }
    }
    */
}
