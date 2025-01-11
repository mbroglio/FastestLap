package com.the_coffe_coders.fastestlap.ui.user;

import android.graphics.Typeface;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.SpinnerAdapter;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.SimpleTextWatcher;

public class UserActivity extends AppCompatActivity implements EditProfilePopup.EditProfileListener {

    private String originalUsername;
    private String originalPassword;
    private String originalEmail;
    private String originalDriver;
    private String originalConstructor;
    private int originalProfileImage;
    private float originalTextSize;
    private Typeface originalTypeface;

    private ImageView profileImage;
    private TextView usernameText;

    private TextView favouriteDriverText;
    private MaterialCardView changeDriverButton;
    private ListPopupWindow listPopupWindowDrivers;

    private TextView favouriteConstructorText;
    private MaterialCardView changeConstructorButton;
    private ListPopupWindow listPopupWindowConstructors;

    private Button saveButton;
    private Button dismissButton;

    private String provisionalUsername;
    private int provisionalProfileImage;
    private String provisionalFavDriver;
    private String provisionalFavConstructor;
    private String provisionalEmail;
    private String provisionalPassword;

    private boolean isProfileImageModified = false;
    private boolean isProfileNameModified = false;
    private boolean isEmailModified = false;
    private boolean isPasswordModified = false;
    private boolean isFavDriverModified = false;
    private boolean isFavConstructorModified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        favouriteDriverText = findViewById(R.id.favourite_driver_text);
        changeDriverButton = findViewById(R.id.change_driver_button);

        favouriteConstructorText = findViewById(R.id.favourite_constructor_text);
        changeConstructorButton = findViewById(R.id.change_constructor_button);

        saveButton = findViewById(R.id.save_button);
        dismissButton = findViewById(R.id.dismiss_button);

        String[] drivers = Constants.DRIVER_FULLNAME.keySet().toArray(new String[0]);
        String[] constructors = Constants.TEAM_FULLNAME.keySet().toArray(new String[0]);

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

        profileImage = findViewById(R.id.profile_image);
        originalProfileImage = getOriginalProfileImage(profileImage);

        usernameText = findViewById(R.id.profile_text);
        ImageView editProfile = findViewById(R.id.edit_profile);
        editProfile.setOnClickListener(v -> {
            EditProfilePopup editProfilePopup = new EditProfilePopup(this, findViewById(R.id.activity_user_layout), originalProfileImage, originalUsername);
            editProfilePopup.show();
        });


        EditText passwordText = findViewById(R.id.password_text);
        ImageView togglePasswordVisibility = findViewById(R.id.toggle_password_visibility);
        ImageView editPassword = findViewById(R.id.edit_password);
        ImageView revertPassword = findViewById(R.id.revert_password);
        ImageView checkPassword = findViewById(R.id.confirm_edit_password);

        TextInputEditText emailText = findViewById(R.id.email_text);
        ImageView editEmail = findViewById(R.id.edit_email);
        ImageView revertEmail = findViewById(R.id.revert_email);
        ImageView checkEmail = findViewById(R.id.confirm_edit_email);

        // Store original textSize, typeface, and background
        originalTextSize = passwordText.getTextSize();
        originalTypeface = passwordText.getTypeface();

        // Store original values
        originalUsername = usernameText.getText().toString();
        originalPassword = passwordText.getText().toString();
        originalEmail = emailText.getText().toString();
        originalDriver = favouriteDriverText.getText().toString();
        originalConstructor = favouriteConstructorText.getText().toString();


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
            startEditing(emailText, editEmail, checkEmail);
            checkForChanges();
        });

        checkEmail.setOnClickListener(v -> {
            abortEditing(emailText, editEmail, checkEmail, provisionalEmail);
        });

        revertEmail.setOnClickListener(v -> {
            revertEditing(emailText, originalEmail, revertEmail, provisionalEmail);
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
            if (isEmailModified) {
                dismissChanges(emailText, originalEmail, editEmail, checkEmail, revertEmail, provisionalEmail);
            }
            if (isPasswordModified) {
                dismissChanges(passwordText, originalPassword, editPassword, checkPassword, revertPassword, provisionalPassword);
            }
            if (isFavDriverModified) {
                favouriteDriverText.setText(originalDriver);
                provisionalFavDriver = null;
            }
            if (isFavConstructorModified) {
                favouriteConstructorText.setText(originalConstructor);
                provisionalFavConstructor = null;
            }
            if (isProfileImageModified) {
                profileImage.setImageResource(originalProfileImage);
                isProfileImageModified = false;
            }
            if (isProfileNameModified) {
                usernameText.setText(originalUsername);
                isProfileNameModified = false;
            }
            checkForChanges();
        });
    }

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
                favouriteDriverText.setText(getString(Constants.DRIVER_FULLNAME.get(selectedItemKey)));
            } else {
                favouriteConstructorText.setText(getString(Constants.TEAM_FULLNAME.get(selectedItemKey)));
            }
            listPopupWindow.dismiss();
            checkForChanges();
        });
        return listPopupWindow;
    }

    private void abortEditing(EditText inputText, ImageView editText, ImageView checkText, String provisionalText) {
        inputText.setEnabled(false);
        provisionalText = inputText.getText().toString();
        editText.setVisibility(View.VISIBLE);
        checkText.setVisibility(View.INVISIBLE);
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

    private void revertEditing(EditText inputText, String originalText, ImageView revertText, String provisionalText) {
        inputText.setText(originalText);
        provisionalText = null;
        inputText.setSelection(inputText.getText().length());
        revertText.setVisibility(View.INVISIBLE);
    }

    private void dismissChanges(EditText inputText, String originalText, ImageView editText, ImageView checkText, ImageView revertText, String provisionalText) {
        inputText.setText(originalText);
        inputText.setEnabled(false);
        provisionalText = null;
        editText.setVisibility(View.VISIBLE);
        checkText.setVisibility(View.INVISIBLE);
        revertText.setVisibility(View.INVISIBLE);
    }

    private void checkForChanges() {
        boolean hasChanges = isEmailModified || isPasswordModified ||
                isFavDriverModified || isFavConstructorModified ||
                isProfileImageModified || isProfileNameModified;

        saveButton.setVisibility(hasChanges ? View.VISIBLE : View.INVISIBLE);
        saveButton.setEnabled(hasChanges);
        dismissButton.setVisibility(hasChanges ? View.VISIBLE : View.INVISIBLE);
        dismissButton.setEnabled(hasChanges);
    }


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
}