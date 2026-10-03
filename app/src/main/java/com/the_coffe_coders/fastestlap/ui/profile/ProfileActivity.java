package com.the_coffe_coders.fastestlap.ui.profile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final Map<String, SwitchConfig> switchConfigs = new HashMap<>();
    private final List<MaterialSwitch> f2SubSwitches = new ArrayList<>();
    private final List<MaterialSwitch> f3SubSwitches = new ArrayList<>();
    private View f2Container;
    private View f3Container;
    private boolean isUpdatingSwitchesProgrammatically = false;

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


        setupLanguageSwitch();

        // Setup Session Notification preferences (F1, F2, F3)
        setupSessionNotificationSwitches();
        fetchRemoteNotificationPreferences();

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
        fetchRemoteNotificationPreferences();
        //updateNotificationPermissionStatus();
    }

    private static class SwitchConfig {
        final MaterialSwitch switchView;
        final String prefKey;
        final boolean defaultValue;

        SwitchConfig(MaterialSwitch switchView, String prefKey, boolean defaultValue) {
            this.switchView = switchView;
            this.prefKey = prefKey;
            this.defaultValue = defaultValue;
        }
    }

    private void setupLanguageSwitch() {
        LocaleListCompat appLocales = AppCompatDelegate.getApplicationLocales();
        String currentLanguage = appLocales.toLanguageTags();
        boolean isEnglish = currentLanguage.toLowerCase(java.util.Locale.ROOT).startsWith("en");
        if (currentLanguage.isEmpty()) {
            isEnglish = getResources().getConfiguration().getLocales().get(0).getLanguage().toLowerCase(java.util.Locale.ROOT).startsWith("en");
        }
        MaterialSwitch languageSwitch = findViewById(R.id.language_switch);
        if (languageSwitch != null) {
            languageSwitch.setChecked(isEnglish);
            languageSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    setLocale("en-GB");
                } else {
                    setLocale("it-IT");
                }
            });
        }
    }

    private void setupSessionNotificationSwitches() {
        switchConfigs.clear();
        f2SubSwitches.clear();
        f3SubSwitches.clear();

        // Collapsible dropdown sections
        setupDropdownSection(R.id.f1_dropdown_header, R.id.f1_sessions_container, R.id.f1_dropdown_arrow, true);
        setupDropdownSection(R.id.f2_dropdown_header, R.id.f2_sessions_container, R.id.f2_dropdown_arrow, false);
        setupDropdownSection(R.id.f3_dropdown_header, R.id.f3_sessions_container, R.id.f3_dropdown_arrow, false);

        f2Container = findViewById(R.id.f2_sessions_container);
        f3Container = findViewById(R.id.f3_sessions_container);

        // F1 Switches (defaults: true)
        registerSwitch(R.id.f1_notif_race_switch, Constants.PREF_NOTIF_F1_RACE, true);
        registerSwitch(R.id.f1_notif_qualifying_switch, Constants.PREF_NOTIF_F1_QUALIFYING, true);
        registerSwitch(R.id.f1_notif_sprint_switch, Constants.PREF_NOTIF_F1_SPRINT, true);
        registerSwitch(R.id.f1_notif_practice_switch, Constants.PREF_NOTIF_F1_PRACTICE, true);

        // F2 Master & Sub-switches
        registerSwitch(R.id.f2_notif_master_switch, Constants.PREF_NOTIF_F2_ENABLED, false);
        registerSubSwitch(f2SubSwitches, R.id.f2_notif_news_switch, Constants.PREF_NOTIF_F2_NEWS, true);
        registerSubSwitch(f2SubSwitches, R.id.f2_notif_feature_switch, Constants.PREF_NOTIF_F2_FEATURE, true);
        registerSubSwitch(f2SubSwitches, R.id.f2_notif_sprint_switch, Constants.PREF_NOTIF_F2_SPRINT, true);
        registerSubSwitch(f2SubSwitches, R.id.f2_notif_qualifying_switch, Constants.PREF_NOTIF_F2_QUALIFYING, true);
        registerSubSwitch(f2SubSwitches, R.id.f2_notif_practice_switch, Constants.PREF_NOTIF_F2_PRACTICE, false);

        // F3 Master & Sub-switches
        registerSwitch(R.id.f3_notif_master_switch, Constants.PREF_NOTIF_F3_ENABLED, false);
        registerSubSwitch(f3SubSwitches, R.id.f3_notif_news_switch, Constants.PREF_NOTIF_F3_NEWS, true);
        registerSubSwitch(f3SubSwitches, R.id.f3_notif_feature_switch, Constants.PREF_NOTIF_F3_FEATURE, true);
        registerSubSwitch(f3SubSwitches, R.id.f3_notif_sprint_switch, Constants.PREF_NOTIF_F3_SPRINT, true);
        registerSubSwitch(f3SubSwitches, R.id.f3_notif_qualifying_switch, Constants.PREF_NOTIF_F3_QUALIFYING, true);
        registerSubSwitch(f3SubSwitches, R.id.f3_notif_practice_switch, Constants.PREF_NOTIF_F3_PRACTICE, false);

        applyNotificationPreferencesToUI();
    }

    private MaterialSwitch registerSwitch(int switchId, String prefKey, boolean defaultValue) {
        MaterialSwitch sw = findViewById(switchId);
        if (sw != null) {
            switchConfigs.put(prefKey, new SwitchConfig(sw, prefKey, defaultValue));
            sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isUpdatingSwitchesProgrammatically) return;
                handleSwitchChanged(prefKey, isChecked);
            });
        }
        return sw;
    }

    private void registerSubSwitch(List<MaterialSwitch> subList, int switchId, String prefKey, boolean defaultValue) {
        MaterialSwitch sw = registerSwitch(switchId, prefKey, defaultValue);
        if (sw != null) {
            subList.add(sw);
        }
    }

    private void handleSwitchChanged(String key, boolean isChecked) {
        SharedPreferences prefs = getSharedPreferences(Constants.SHARED_PREFERENCES_FILENAME, MODE_PRIVATE);
        prefs.edit().putBoolean(key, isChecked).apply();

        if (currentUser != null && currentUser.getIdToken() != null) {
            userViewModel.saveUserNotificationPreference(key, isChecked, currentUser.getIdToken());
        }
        AppNotificationManager.getInstance().syncSessionTopicSubscriptions(ProfileActivity.this, false);

        if (Constants.PREF_NOTIF_F2_ENABLED.equals(key)) {
            updateCategoryEnabled(f2Container, isChecked, f2SubSwitches);
        } else if (Constants.PREF_NOTIF_F3_ENABLED.equals(key)) {
            updateCategoryEnabled(f3Container, isChecked, f3SubSwitches);
        }
    }

    private void setupDropdownSection(int headerId, int containerId, int arrowId, boolean startExpanded) {
        View header = findViewById(headerId);
        View container = findViewById(containerId);
        ImageView arrow = findViewById(arrowId);

        if (container != null) {
            container.setVisibility(startExpanded ? View.VISIBLE : View.GONE);
        }
        if (arrow != null) {
            arrow.setRotation(startExpanded ? 180f : 0f);
        }
        if (header != null && container != null && arrow != null) {
            header.setOnClickListener(v -> {
                boolean isExpanded = container.getVisibility() == View.VISIBLE;
                container.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
                arrow.animate().rotation(isExpanded ? 0f : 180f).setDuration(200).start();
            });
        }
    }

    private void applyNotificationPreferencesToUI() {
        SharedPreferences prefs = getSharedPreferences(Constants.SHARED_PREFERENCES_FILENAME, MODE_PRIVATE);
        isUpdatingSwitchesProgrammatically = true;
        try {
            for (SwitchConfig config : switchConfigs.values()) {
                if (config.switchView != null) {
                    config.switchView.setChecked(prefs.getBoolean(config.prefKey, config.defaultValue));
                }
            }
            updateCategoryEnabled(f2Container, prefs.getBoolean(Constants.PREF_NOTIF_F2_ENABLED, false), f2SubSwitches);
            updateCategoryEnabled(f3Container, prefs.getBoolean(Constants.PREF_NOTIF_F3_ENABLED, false), f3SubSwitches);
        } finally {
            isUpdatingSwitchesProgrammatically = false;
        }
    }

    private void updateCategoryEnabled(View container, boolean isEnabled, List<MaterialSwitch> subSwitches) {
        if (container != null) {
            container.setAlpha(isEnabled ? 1.0f : 0.5f);
        }
        if (subSwitches != null) {
            for (MaterialSwitch sw : subSwitches) {
                if (sw != null) sw.setEnabled(isEnabled);
            }
        }
    }

    private void fetchRemoteNotificationPreferences() {
        if (currentUser == null && userViewModel != null) {
            currentUser = userViewModel.getLoggedUser();
        }
        if (currentUser != null && currentUser.getIdToken() != null) {
            userViewModel.getNotificationPreferences(currentUser.getIdToken())
                    .addOnSuccessListener(remotePrefs -> {
                        if (remotePrefs != null && !remotePrefs.isEmpty()) {
                            Log.i(TAG, "Restoring notification preferences from ViewModel in ProfileActivity for user " + currentUser.getIdToken());
                            SharedPreferences prefs = getSharedPreferences(Constants.SHARED_PREFERENCES_FILENAME, MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            for (Map.Entry<String, Boolean> entry : remotePrefs.entrySet()) {
                                editor.putBoolean(entry.getKey(), entry.getValue());
                            }
                            editor.apply();
                            applyNotificationPreferencesToUI();
                            AppNotificationManager.getInstance().syncSessionTopicSubscriptions(ProfileActivity.this, false);
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch notification preferences from ViewModel", e));
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
