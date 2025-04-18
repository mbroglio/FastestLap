package com.the_coffe_coders.fastestlap.ui.welcome;

import static com.the_coffe_coders.fastestlap.util.Constants.UNEXPECTED_ERROR;
import static com.the_coffe_coders.fastestlap.util.Constants.USER_COLLISION_ERROR;
import static com.the_coffe_coders.fastestlap.util.Constants.WEAK_PASSWORD_ERROR;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.repository.user.IUserRepository;
import com.the_coffe_coders.fastestlap.ui.home.HomePageActivity;
import com.the_coffe_coders.fastestlap.ui.welcome.fragment.ForgotPasswordFragment;
import com.the_coffe_coders.fastestlap.ui.welcome.fragment.SignUpFragment;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModel;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModelFactory;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;
import com.the_coffe_coders.fastestlap.util.UIUtils;

import org.apache.commons.validator.routines.EmailValidator;

public class WelcomeActivity extends AppCompatActivity implements ForgotPasswordFragment.OnFragmentInteractionListener {

    private static final String TAG = "LoginFragment";

    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private UserViewModel userViewModel;
    private FirebaseAuth mAuth;

    private boolean fromSignOut;
    private boolean fromEmailVerification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_welcome);

        LinearLayout loginLayout = findViewById(R.id.activity_login);
        UIUtils.applyWindowInsets(loginLayout);

        mAuth = FirebaseAuth.getInstance();

        // fromSignOut = getIntent().getBooleanExtra("SIGN_OUT", false);

        IUserRepository userRepository = ServiceLocator.getInstance().getUserRepository((Application) getApplicationContext());

        userViewModel = new ViewModelProvider(getViewModelStore(), new UserViewModelFactory(userRepository)).get(UserViewModel.class);
        userViewModel.setAuthenticationError(false);

        emailEditText = findViewById(R.id.textInputEmail);
        passwordEditText = findViewById(R.id.textInputPassword);

        View rootView = findViewById(R.id.login_activity_layout);
        rootView.setOnClickListener(v -> resetHintPosition());

        Button registerButton = findViewById(R.id.RegisterButton);
        registerButton.setOnClickListener(v -> {
            FragmentManager fragmentManager = getSupportFragmentManager();
            SignUpFragment signUpFragment = new SignUpFragment();
            signUpFragment.show(fragmentManager, "SignUpFragment");
        });

        Button loginButton = findViewById(R.id.LoginButton);
        loginButton.setOnClickListener(v -> {
            if (emailEditText.getText() != null && isEmailOk(emailEditText.getText().toString())) {
                if (passwordEditText.getText() != null && isPasswordOk(passwordEditText.getText().toString())) {
                    mAuth.signInWithEmailAndPassword(emailEditText.getText().toString(), passwordEditText.getText().toString())
                            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {

                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {

                                        userViewModel.getUserPreferences(userViewModel.getLoggedUser().getIdToken());

                                        // Sign in success, update UI with the signed-in user's information
                                        Log.d(TAG, "signInWithEmail:success");
                                        Intent intent = new Intent(WelcomeActivity.this, HomePageActivity.class);
                                        intent.putExtra("CALLER", "WelcomeActivity");
                                        startActivity(intent);

                                    } else {
                                        // If sign in fails, display a message to the user.
                                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                                        Toast.makeText(WelcomeActivity.this, "Authentication failed.",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });

        Button forgotPasswordButton = findViewById(R.id.forgotten_password_button);
        forgotPasswordButton.setOnClickListener(v -> {
            FragmentManager fragmentManager = getSupportFragmentManager();
            ForgotPasswordFragment forgotPasswordFragment = new ForgotPasswordFragment();
            forgotPasswordFragment.show(fragmentManager, "ForgotPasswordFragment");
        });
    }

    private boolean isEmailOk(String email) {
        // Check if the email is valid through the use of this library:
        // https://commons.apache.org/proper/commons-validator/
        if (!EmailValidator.getInstance().isValid((email))) {
            //emailEditText.setError("Error Email Login");
            Toast.makeText(this, "Invalid email", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            emailEditText.setError(null);
            return true;
        }
    }

    private boolean isPasswordOk(String password) {
        // Check if the password length is correct
        if (password.isEmpty() || password.length() < Constants.MINIMUM_LENGTH_PASSWORD) {
            //passwordEditText.setError("Error Password Login");
            Toast.makeText(this, "Invalid password", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            passwordEditText.setError(null);
            return true;
        }
    }

    private String getErrorMessage(String message) {
        switch (message) {
            case WEAK_PASSWORD_ERROR:
                return WEAK_PASSWORD_ERROR;
            case USER_COLLISION_ERROR:
                return USER_COLLISION_ERROR;
            default:
                return UNEXPECTED_ERROR;
        }
    }

    @Override
    public void onFragmentDismissed(String value) {
        fromEmailVerification = Boolean.parseBoolean(value);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void resetHintPosition() {
        emailEditText.clearFocus();
        passwordEditText.clearFocus();
    }
}