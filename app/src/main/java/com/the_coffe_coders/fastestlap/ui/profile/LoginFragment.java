package com.the_coffe_coders.fastestlap.ui.profile;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.ui.welcome.WelcomeActivity;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.NetworkUtils;
import com.the_coffe_coders.fastestlap.util.UIUtils;

import org.apache.commons.validator.routines.EmailValidator;

import java.util.Objects;


public class LoginFragment extends DialogFragment {

    private String email;
    private TextInputEditText textInputEmail;
    private TextInputEditText textInputPassword;

    private FirebaseAuth mAuth;
    private NetworkUtils networkLiveData;

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            email = getArguments().getString("email");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        mAuth = FirebaseAuth.getInstance();
        networkLiveData = new NetworkUtils(Objects.requireNonNull(getContext()));

        textInputEmail = view.findViewById(R.id.textInputEmail);
        textInputEmail.setText(email);

        textInputPassword = view.findViewById(R.id.textInputPassword);

        Button submitButton = view.findViewById(R.id.submit_button);
        submitButton.setOnClickListener(v -> {
            String email = Objects.requireNonNull(textInputEmail.getText()).toString();
            String password = Objects.requireNonNull(textInputPassword.getText()).toString();
            manageLogin(email, password);

        });

        return view;
    }

    private void manageLogin(String email, String password) {
        if(networkLiveData.isConnected()){
            if (password != null && isPasswordOk(password)) {
                mAuth.signInWithEmailAndPassword(email,password)
                        .addOnCompleteListener(getActivity(), task -> {
                            if (task.isSuccessful()) {
                                Log.d("LoginFragment", "signInWithEmail:success");
                                UIUtils.navigateToHomePage(getContext());
                            } else {
                                Log.e("LoginFragment", "signInWithEmail:failure", task.getException());
                                Toast.makeText(getContext(), "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }else{
                Toast.makeText(getContext(), "Invalid password", Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(getContext(), "No internet connection", Toast.LENGTH_SHORT).show();
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