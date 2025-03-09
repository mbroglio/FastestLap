package com.the_coffe_coders.fastestlap.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.SpinnerAdapter;
import com.the_coffe_coders.fastestlap.domain.user.User;
import com.the_coffe_coders.fastestlap.repository.user.IUserRepository;
import com.the_coffe_coders.fastestlap.ui.home.HomePageActivity;
import com.the_coffe_coders.fastestlap.ui.welcome.WelcomeActivity;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModel;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModelFactory;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;
import com.the_coffe_coders.fastestlap.util.SharedPreferencesUtils;
import com.the_coffe_coders.fastestlap.util.SimpleTextWatcher;
import com.the_coffe_coders.fastestlap.util.UIUtils;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";
    //private boolean isProfileImageModified = false;
    //private boolean isProfileNameModified = false;
    private final boolean isEmailModified = false;
    SharedPreferencesUtils sharedPreferencesUtils = new SharedPreferencesUtils(this);
    User currentUser;
    private GestureDetector tapDetector;
    //private String originalUsername;
    //private String originalPassword;
    private String originalEmail;
    private String originalDriver;
    //private int originalProfileImage;
    //private float originalTextSize;
    //private Typeface originalTypeface;

    //private ImageView profileImage;
    //private TextView usernameText;
    private String originalConstructor;
    private TextInputEditText emailText;
    private TextView favouriteDriverText;
    private MaterialCardView changeDriverButton;
    private ListPopupWindow listPopupWindowDrivers;
    private TextView favouriteConstructorText;
    private MaterialCardView changeConstructorButton;
    private ListPopupWindow listPopupWindowConstructors;
    private CheckBox autoLoginCheckBox;
    private Button saveButton;
    private Button dismissButton;
    private Button signOutButton;
    //private String provisionalUsername;
    //private int provisionalProfileImage;
    private String provisionalFavDriver;
    private String provisionalFavConstructor;
    private String favouriteDriverKey;
    private String favouriteConstructorKey;
    //private String provisionalEmail;
    //private String provisionalPassword;
    private String autoLoginKey;
    //private boolean isPasswordModified = false;
    private boolean isFavDriverModified = false;
    private boolean isFavConstructorModified = false;
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

        favouriteDriverText = findViewById(R.id.favourite_driver_text);
        favouriteConstructorText = findViewById(R.id.favourite_constructor_text);

        if (getFavoriteDriverId() == null || getFavoriteTeamId() == null) {
            if (getFavoriteDriverId() == null) {
                favouriteDriverText.setText("No favourite driver selected");
            }
            if (getFavoriteTeamId() == null) {
                favouriteConstructorText.setText("No favourite constructor selected");
            }
            toolbar.setNavigationOnClickListener(v -> Toast.makeText(this, "Please select a favourite driver and constructor", Toast.LENGTH_SHORT).show());
        } else {
            favouriteDriverText.setText(Constants.DRIVER_FULLNAME.get(getFavoriteDriverId()));
            favouriteConstructorText.setText(Constants.TEAM_FULLNAME.get(getFavoriteTeamId()));
            if (isFromLogin) {
                toolbar.setNavigationOnClickListener(v -> {
                    Intent intent = new Intent(ProfileActivity.this, HomePageActivity.class);
                    startActivity(intent);
                });
            } else {
                toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
            }

        }

        userRepository = ServiceLocator.getInstance().getUserRepository(getApplication());
        userViewModel = new ViewModelProvider(getViewModelStore(), new UserViewModelFactory(userRepository)).get(UserViewModel.class);

        currentUser = userViewModel.getLoggedUser();

        changeDriverButton = findViewById(R.id.change_driver_button);
        changeConstructorButton = findViewById(R.id.change_constructor_button);

        autoLoginCheckBox = findViewById(R.id.remember_me_checkbox);
        saveButton = findViewById(R.id.save_button);
        dismissButton = findViewById(R.id.dismiss_button);
        signOutButton = findViewById(R.id.sign_out_button);

        String[] drivers = Constants.DRIVER_FULLNAME.keySet().toArray(new String[0]);
        String[] constructors = Constants.TEAM_FULLNAME.keySet().toArray(new String[0]);

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

        // Initialize ListPopupWindow for drivers and constructors
        listPopupWindowDrivers = createListPopupWindow(drivers, changeDriverButton, true);
        listPopupWindowConstructors = createListPopupWindow(constructors, changeConstructorButton, false);

        // Handle change driver button click
        changeDriverButton.setOnClickListener(v -> {
            if (listPopupWindowDrivers.isShowing()) {
                listPopupWindowDrivers.dismiss();
            } else {
                listPopupWindowDrivers.show();
            }
        });

        // Handle change constructor button click
        changeConstructorButton.setOnClickListener(v -> {
            if (listPopupWindowConstructors.isShowing()) {
                listPopupWindowConstructors.dismiss();
            } else {
                listPopupWindowConstructors.show();
            }
        });

        //profileImage = findViewById(R.id.profile_image);
        //originalProfileImage = getOriginalProfileImage(profileImage);

        /*
        usernameText = findViewById(R.id.profile_text);
        ImageView editProfile = findViewById(R.id.edit_profile);
        editProfile.setOnClickListener(v -> {
            EditProfilePopup editProfilePopup = new EditProfilePopup(this, findViewById(R.id.activity_user_layout), originalProfileImage, originalUsername);
            editProfilePopup.show();
        });
        */

        /*
        EditText passwordText = findViewById(R.id.password_text);
        ImageView togglePasswordVisibility = findViewById(R.id.toggle_password_visibility);
        ImageView editPassword = findViewById(R.id.edit_password);
        ImageView revertPassword = findViewById(R.id.revert_password);
        ImageView checkPassword = findViewById(R.id.confirm_edit_password);
        */

        emailText = findViewById(R.id.email_text);
        emailText.setText(userViewModel.getLoggedUser().getEmail());
        /*
        ImageView editEmail = findViewById(R.id.edit_email);
        ImageView revertEmail = findViewById(R.id.revert_email);
        ImageView confirmEmail = findViewById(R.id.confirm_edit_email);
        */
        // Store original textSize, typeface, and background
        //originalTextSize = passwordText.getTextSize();
        //originalTypeface = passwordText.getTypeface();

        // Store original values
        //originalUsername = usernameText.getText().toString();
        //originalPassword = passwordText.getText().toString();
        originalEmail = emailText.getText().toString();
        originalDriver = favouriteDriverText.getText().toString();
        originalConstructor = favouriteConstructorText.getText().toString();

        /*
        togglePasswordVisibility.setOnClickListener(v -> {
            if (passwordText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                passwordText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                togglePasswordVisibility.setImageResource(R.drawable.eye_off_icon_white);
            } else {
                passwordText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                togglePasswordVisibility.setImageResource(R.drawable.eye_icon_white);
            }
            passwordText.setSelection(passwordText.getText().length());
            // Reapply original textSize and typeface
            passwordText.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalTextSize);
            passwordText.setTypeface(originalTypeface);
        });


        editPassword.setOnClickListener(v -> {
            startEditing(passwordText, editPassword, checkPassword);
            checkForChanges();
        });

        checkPassword.setOnClickListener(v -> {
            abortEditing(passwordText, editPassword, checkPassword, provisionalPassword);
        });

        revertPassword.setOnClickListener(v -> {
            revertEditing(passwordText, originalPassword, revertPassword, provisionalPassword);
        });



        editEmail.setOnClickListener(v -> {
            startEditing(emailText, editEmail, confirmEmail);
            checkForChanges();
        });

        confirmEmail.setOnClickListener(v -> {
            provisionalEmail = abortEditing(emailText, editEmail, confirmEmail);
        });

        revertEmail.setOnClickListener(v -> {
            provisionalEmail = revertEditing(emailText, originalEmail, revertEmail);
        });


        // Add text change listeners
        passwordText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isPasswordModified = !s.toString().equals(originalPassword);
                revertPassword.setVisibility(isPasswordModified ? View.VISIBLE : View.INVISIBLE);
                checkForChanges();
            }
        });

        emailText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isEmailModified = !s.toString().equals(originalEmail);
                revertEmail.setVisibility(isEmailModified ? View.VISIBLE : View.INVISIBLE);
                checkForChanges();
            }
        });

         */

        favouriteDriverText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isFavDriverModified = !s.toString().equals(originalDriver);
                if (isFavDriverModified) {
                    provisionalFavDriver = s.toString();
                }
                checkForChanges();
            }
        });

        favouriteConstructorText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isFavConstructorModified = !s.toString().equals(originalConstructor);
                if (isFavConstructorModified) {
                    provisionalFavConstructor = s.toString();
                }
                checkForChanges();
            }
        });

        // Set dismiss button listener
        dismissButton.setOnClickListener(v -> {
            /*
            if (isEmailModified) {
                provisionalEmail = dismissChanges(emailText, originalEmail, editEmail, confirmEmail, revertEmail);
            }

            if (isPasswordModified) {
                dismissChanges(passwordText, originalPassword, editPassword, checkPassword, revertPassword, provisionalPassword);
            }
            */
            if (isFavDriverModified) {
                favouriteDriverText.setText(originalDriver);
                provisionalFavDriver = null;
            }
            if (isFavConstructorModified) {
                favouriteConstructorText.setText(originalConstructor);
                provisionalFavConstructor = null;
            }
            /*
            if (isProfileImageModified) {
                profileImage.setImageResource(originalProfileImage);
                isProfileImageModified = false;
            }
            if (isProfileNameModified) {
                usernameText.setText(originalUsername);
                isProfileNameModified = false;
            }
            */
            checkForChanges();
        });

        signOutButton.setOnClickListener(v -> {
            userViewModel.logout();
            Intent intent = new Intent(ProfileActivity.this, WelcomeActivity.class);
            startActivity(intent);
        });
    }
    /*
    private int getOriginalProfileImage(ImageView profileImage) {
        Drawable currentDrawable = profileImage.getDrawable();
        int image = 0;
        if (currentDrawable != null) {
            if (currentDrawable.getConstantState().equals(AppCompatResources.getDrawable(this, R.drawable.boy_icon).getConstantState())) {
                image = R.drawable.boy_icon;
            } else if (currentDrawable.getConstantState().equals(AppCompatResources.getDrawable(this, R.drawable.girl_icon).getConstantState())) {
                image = R.drawable.girl_icon;
            } else if (currentDrawable.getConstantState().equals(AppCompatResources.getDrawable(this, R.drawable.anonymous_user_icon).getConstantState())) {
                image = R.drawable.anonymous_user_icon;
            }
        }
        return image;
    }
     */

    private ListPopupWindow createListPopupWindow(String[] items, MaterialCardView anchorView, boolean isDriver) {
        ListPopupWindow listPopupWindow = new ListPopupWindow(this);
        listPopupWindow.setAdapter(new SpinnerAdapter(this, items, isDriver));
        listPopupWindow.setAnchorView(anchorView);
        listPopupWindow.setWidth((int) (220 * getResources().getDisplayMetrics().density)); // Set the width to 220dp
        listPopupWindow.setHeight((int) (250 * getResources().getDisplayMetrics().density)); // Set the height to 250dp
        listPopupWindow.setBackgroundDrawable(getDrawable(R.drawable.round_background_grey));
        listPopupWindow.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItemKey = items[position];
            if (isDriver) {
                favouriteDriverKey = selectedItemKey;
                favouriteDriverText.setText(getString(Constants.DRIVER_FULLNAME.get(selectedItemKey)));
            } else {
                favouriteConstructorKey = selectedItemKey;
                favouriteConstructorText.setText(getString(Constants.TEAM_FULLNAME.get(selectedItemKey)));
            }
            listPopupWindow.dismiss();
            checkForChanges();
        });
        return listPopupWindow;
    }

    private String abortEditing(EditText inputText, ImageView editText, ImageView checkText) {
        inputText.setEnabled(false);
        String provisionalText = inputText.getText().toString();
        editText.setVisibility(View.VISIBLE);
        checkText.setVisibility(View.INVISIBLE);
        return provisionalText;
    }

    private void startEditing(EditText inputText, ImageView editText, ImageView checkText) {
        inputText.setEnabled(true);
        inputText.requestFocus();
        inputText.setSelection(inputText.getText().length());
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.showSoftInput(inputText, InputMethodManager.SHOW_IMPLICIT);
        editText.setVisibility(View.INVISIBLE);
        checkText.setVisibility(View.VISIBLE);

    }

    private String revertEditing(EditText inputText, String originalText, ImageView revertText) {
        inputText.setText(originalText);
        inputText.setSelection(inputText.getText().length());
        revertText.setVisibility(View.INVISIBLE);
        return null;
    }

    private String dismissChanges(EditText inputText, String originalText, ImageView editText, ImageView checkText, ImageView revertText) {
        inputText.setText(originalText);
        inputText.setEnabled(false);
        editText.setVisibility(View.VISIBLE);
        checkText.setVisibility(View.INVISIBLE);
        revertText.setVisibility(View.INVISIBLE);
        return null;
    }

    private void checkForChanges() {
        boolean hasChanges = isEmailModified || isFavDriverModified || isFavConstructorModified || isCheckBoxChanged;

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
        if (favouriteDriverKey != null) {
            sharedPreferencesUtils.writeStringData(Constants.SHARED_PREFERENCES_FILENAME,
                    Constants.SHARED_PREFERENCES_FAVORITE_DRIVER,
                    favouriteDriverKey);
        } else {
            favouriteDriverKey = getFavoriteDriverId();
        }

        if (favouriteConstructorKey != null) {
            sharedPreferencesUtils.writeStringData(Constants.SHARED_PREFERENCES_FILENAME,
                    Constants.SHARED_PREFERENCES_FAVORITE_TEAM,
                    favouriteConstructorKey);
        } else {
            favouriteConstructorKey = getFavoriteTeamId();
        }

        if (isCheckBoxChanged) {
            sharedPreferencesUtils.writeStringData(Constants.SHARED_PREFERENCES_FILENAME,
                    Constants.SHARED_PREFERENCES_AUTO_LOGIN,
                    String.valueOf(autoLoginCheckBox.isChecked()));
        } else {
            autoLoginKey = getAutoLoginValue();
        }

        userViewModel.saveUserPreferences(
                favouriteDriverKey,
                favouriteConstructorKey,
                String.valueOf(autoLoginCheckBox.isChecked()),
                user.getIdToken()
        );
    }

    private void setAutoLoginCheckBox() {
        String autoLogin = getAutoLoginValue();
        if (autoLogin != null) {
            autoLoginCheckBox.setChecked(Boolean.parseBoolean(autoLogin));
        }
    }

    private String getFavoriteDriverId() {
        String favoriteDriver = sharedPreferencesUtils.readStringData(Constants.SHARED_PREFERENCES_FILENAME, Constants.SHARED_PREFERENCES_FAVORITE_DRIVER);
        Log.i(TAG, "Favorite Driver: " + favoriteDriver);
        return favoriteDriver;
    }

    private String getFavoriteTeamId() {
        String favoriteTeam = sharedPreferencesUtils.readStringData(Constants.SHARED_PREFERENCES_FILENAME, Constants.SHARED_PREFERENCES_FAVORITE_TEAM);
        Log.i(TAG, "Favorite Team: " + favoriteTeam);
        return favoriteTeam;
    }

    private String getAutoLoginValue() {
        String autoLogin = sharedPreferencesUtils.readStringData(Constants.SHARED_PREFERENCES_FILENAME, Constants.SHARED_PREFERENCES_AUTO_LOGIN);
        Log.i(TAG, "Auto Login: " + autoLogin);
        return autoLogin;
    }

    /*
    @Override
    public void onProfileImageChanged(int newProfileImage) {
        profileImage.setImageResource(newProfileImage);
        provisionalProfileImage = newProfileImage;
        isProfileImageModified = true;
        checkForChanges();
    }

    @Override
    public void onUsernameChanged(String newUsername) {
        usernameText.setText(newUsername);
        provisionalUsername = newUsername;
        isProfileNameModified = true;
        checkForChanges();
    }
    */

    @Override
    protected void onResume() {
        super.onResume();
    }
}