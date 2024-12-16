package com.the_coffe_coders.fastestlap.ui.user;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.appbar.MaterialToolbar;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.SpinnerAdapter;
import com.the_coffe_coders.fastestlap.utils.Constants;

import java.util.Set;

public class UserActivity extends AppCompatActivity {

    private boolean isEditingPassword = false;
    private boolean isEditingEmail = false;
    private String originalPassword;
    private String originalEmail;
    private String originalDriver;
    private String originalConstructor;
    private float originalTextSize;
    private Typeface originalTypeface;
    private Drawable originalBackground;

    private TextView favouriteDriverText;
    private MaterialCardView changeDriverButton;
    private ListPopupWindow listPopupWindowDrivers;

    private TextView favouriteConstructorText;
    private MaterialCardView changeConstructorButton;
    private ListPopupWindow listPopupWindowConstructors;

    private Button saveButton;
    private Button dismissButton;

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

        // Get keys from DRIVER_FULLNAME and TEAM_FULLNAME maps
        Set<String> driverKeys = Constants.DRIVER_FULLNAME.keySet();
        Set<String> teamKeys = Constants.TEAM_FULLNAME.keySet();

        // Convert keys to arrays
        String[] drivers = driverKeys.toArray(new String[0]);
        String[] constructors = teamKeys.toArray(new String[0]);

        // Initialize ListPopupWindow for drivers
        listPopupWindowDrivers = new ListPopupWindow(this);
        listPopupWindowDrivers.setAdapter(new SpinnerAdapter(this, drivers, true));
        listPopupWindowDrivers.setAnchorView(changeDriverButton);
        listPopupWindowDrivers.setWidth((int) (220 * getResources().getDisplayMetrics().density)); // Set the width to 220dp
        listPopupWindowDrivers.setHeight((int) (250 * getResources().getDisplayMetrics().density)); // Set the height to 150dp
        listPopupWindowDrivers.setBackgroundDrawable(getDrawable(R.drawable.round_background_grey));
        listPopupWindowDrivers.setOnItemClickListener((parent, view, position, id) -> {
            String selectedDriverKey = drivers[position];
            favouriteDriverText.setText(getString(Constants.DRIVER_FULLNAME.get(selectedDriverKey)));
            listPopupWindowDrivers.dismiss();
            checkForChanges();
        });

        // Handle change driver button click
        changeDriverButton.setOnClickListener(v -> {
            if (listPopupWindowDrivers.isShowing()) {
                Log.i("UserActivity", "Dismissing driver popup");
                listPopupWindowDrivers.dismiss();
            } else {
                Log.i("UserActivity", "Showing driver popup");
                listPopupWindowDrivers.show();
            }
        });

        // Initialize ListPopupWindow for constructors
        listPopupWindowConstructors = new ListPopupWindow(this);
        listPopupWindowConstructors.setAdapter(new SpinnerAdapter(this, constructors, false));
        listPopupWindowConstructors.setAnchorView(changeConstructorButton);
        listPopupWindowConstructors.setWidth((int) (220 * getResources().getDisplayMetrics().density)); // Set the width to 220dp
        listPopupWindowConstructors.setHeight((int) (250 * getResources().getDisplayMetrics().density)); // Set the height to 150dp
        listPopupWindowConstructors.setBackgroundDrawable(getDrawable(R.drawable.round_background_grey));
        listPopupWindowConstructors.setOnItemClickListener((parent, view, position, id) -> {
            String selectedConstructorKey = constructors[position];
            favouriteConstructorText.setText(getString(Constants.TEAM_FULLNAME.get(selectedConstructorKey)));
            listPopupWindowConstructors.dismiss();
            checkForChanges();
        });

        // Handle change constructor button click
        changeConstructorButton.setOnClickListener(v -> {
            if (listPopupWindowConstructors.isShowing()) {
                Log.i("UserActivity", "Dismissing constructor popup");
                listPopupWindowConstructors.dismiss();
            } else {
                Log.i("UserActivity", "Showing constructor popup");
                listPopupWindowConstructors.show();
            }
        });

        EditText passwordText = findViewById(R.id.password_text);
        ImageView togglePasswordVisibility = findViewById(R.id.toggle_password_visibility);
        ImageView editPassword = findViewById(R.id.edit_password);

        EditText emailText = findViewById(R.id.email_text);
        ImageView editEmail = findViewById(R.id.edit_email);

        // Store original textSize, typeface, and background
        originalTextSize = passwordText.getTextSize();
        originalTypeface = passwordText.getTypeface();
        originalBackground = passwordText.getBackground();

        // Store original values
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
            if (isEditingPassword) {
                // Abort editing and reset to original password
                passwordText.setText(originalPassword);
                passwordText.setEnabled(false);
                passwordText.setBackground(originalBackground);
                isEditingPassword = false;
            } else {
                // Start editing
                originalPassword = passwordText.getText().toString();
                passwordText.setEnabled(true);
                passwordText.requestFocus();
                passwordText.setSelection(passwordText.getText().length());
                isEditingPassword = true;
            }
            // Reapply original textSize and typeface
            passwordText.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalTextSize);
            passwordText.setTypeface(originalTypeface);
            // Set underline and cursor color
            checkForChanges();
        });

        editEmail.setOnClickListener(v -> {
            if (isEditingEmail) {
                // Abort editing and reset to original email
                emailText.setText(originalEmail);
                emailText.setEnabled(false);
                emailText.setBackground(originalBackground);
                isEditingEmail = false;
            } else {
                // Start editing
                originalEmail = emailText.getText().toString();
                emailText.setEnabled(true);
                emailText.requestFocus();
                emailText.setSelection(emailText.getText().length());
                isEditingEmail = true;
            }
            // Reapply original textSize and typeface
            emailText.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalTextSize);
            emailText.setTypeface(originalTypeface);
            // Set underline and cursor color
            checkForChanges();
        });

        // Add text change listeners
        passwordText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkForChanges();
            }
        });

        emailText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkForChanges();
            }
        });
    }

    private void checkForChanges() {
        String currentPassword = ((EditText) findViewById(R.id.password_text)).getText().toString();
        String currentEmail = ((EditText) findViewById(R.id.email_text)).getText().toString();
        String currentDriver = favouriteDriverText.getText().toString();
        String currentConstructor = favouriteConstructorText.getText().toString();

        boolean hasChanges = !currentPassword.equals(originalPassword) ||
                !currentEmail.equals(originalEmail) ||
                !currentDriver.equals(originalDriver) ||
                !currentConstructor.equals(originalConstructor);

        saveButton.setVisibility(hasChanges ? View.VISIBLE : View.INVISIBLE);
        saveButton.setEnabled(hasChanges);
        dismissButton.setVisibility(hasChanges ? View.VISIBLE : View.INVISIBLE);
        dismissButton.setEnabled(hasChanges);
    }
}