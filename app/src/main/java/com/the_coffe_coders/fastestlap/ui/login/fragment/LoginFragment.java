package com.the_coffe_coders.fastestlap.ui.login.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.ui.login.IntroScreen;

public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";
    private IntroScreen introScreen;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;

    public LoginFragment() {
        // Required empty public constructor
    }

    public static LoginFragment newInstance() {
        return new LoginFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        introScreen = new IntroScreen(view, getContext());
        introScreen.showIntroScreen();

        emailEditText = view.findViewById(R.id.textInputEmail);
        passwordEditText = view.findViewById(R.id.textInputPassword);

        View rootView = view.findViewById(R.id.login_fragment_layout);
        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetHintPosition();
            }
        });

        Button registerButton = view.findViewById(R.id.RegisterButton);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRegisterPopup();
            }
        });

        return view;
    }

    private void showRegisterPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.register_pop_up, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);

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