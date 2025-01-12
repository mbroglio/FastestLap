package com.the_coffe_coders.fastestlap.ui.welcome;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.ui.home.HomePageActivity;
import com.the_coffe_coders.fastestlap.util.IntroScreen;

import java.util.Objects;

public class WelcomeActivity extends AppCompatActivity {

    private static final String TAG = "LoginFragment";
    private IntroScreen introScreen;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcome);

        introScreen = new IntroScreen(findViewById(android.R.id.content), this);
        introScreen.showIntroScreen();

        emailEditText = findViewById(R.id.textInputEmail);
        passwordEditText = findViewById(R.id.textInputPassword);

        View rootView = findViewById(R.id.login_activity_layout);
        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetHintPosition();
            }
        });

        Button registerButton = findViewById(R.id.RegisterButton);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRegisterPopup();
            }
        });

        Button loginButton = findViewById(R.id.LoginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Login button clicked");
                Intent intent = new Intent(WelcomeActivity.this, HomePageActivity.class);
                startActivity(intent);
            }
        });
    }

    private void showRegisterPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.register_pop_up, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(R.drawable.dialog_background);

        Button submitButton = dialogView.findViewById(R.id.submit_button);
        EditText emailEditText = dialogView.findViewById(R.id.textInputEmail);
        EditText passwordEditText = dialogView.findViewById(R.id.textInputPassword);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString();
                String password = passwordEditText.getText().toString();
                Log.d(TAG, "Email: " + email);
                Log.d(TAG, "Password: " + password);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (introScreen != null) {
            introScreen.releaseResources();
        }
    }

    private void resetHintPosition() {
        emailEditText.clearFocus();
        passwordEditText.clearFocus();
    }
}