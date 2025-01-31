package com.the_coffe_coders.fastestlap.ui.welcome;

import static com.the_coffe_coders.fastestlap.util.Constants.UNEXPECTED_ERROR;
import static com.the_coffe_coders.fastestlap.util.Constants.USER_COLLISION_ERROR;
import static com.the_coffe_coders.fastestlap.util.Constants.WEAK_PASSWORD_ERROR;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.graphics.LinearGradient;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.BeginSignInResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.repository.user.IUserRepository;
import com.the_coffe_coders.fastestlap.ui.home.HomePageActivity;
import com.the_coffe_coders.fastestlap.ui.profile.ProfileActivity;
import com.the_coffe_coders.fastestlap.ui.welcome.fragment.ForgotPasswordFragment;
import com.the_coffe_coders.fastestlap.ui.welcome.fragment.SignUpFragment;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModel;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModelFactory;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.IntroScreen;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;
import com.the_coffe_coders.fastestlap.util.UIUtils;

import org.apache.commons.validator.routines.EmailValidator;

import java.util.Objects;

public class WelcomeActivity extends AppCompatActivity implements ForgotPasswordFragment.OnFragmentInteractionListener {

    private static final String TAG = "LoginFragment";
    private static final int RC_SIGN_IN = 9001; // Request code for Google Sign-In

    private IntroScreen introScreen;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private UserViewModel userViewModel;
    private SignInClient oneTapClient;
    private ActivityResultLauncher<IntentSenderRequest> activityResultLauncher;
    private ActivityResultContracts.StartIntentSenderForResult startIntentSenderForResult;
    private BeginSignInRequest signInRequest;
    private FirebaseAuth mAuth;

    private GestureDetector tapDetector;

    private boolean fromSignOut;
    private boolean fromEmailVerification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        //UIUtils.hideSystemUI(this);
        setContentView(R.layout.activity_welcome);

        LinearLayout loginLayout = findViewById(R.id.activity_login);
        UIUtils.applyWindowInsets(loginLayout);

        //tapDetector = UIUtils.createTapDetector(this);

        mAuth = FirebaseAuth.getInstance();

        fromSignOut = getIntent().getBooleanExtra("SIGN_OUT", false);

        IUserRepository userRepository = ServiceLocator.getInstance().getUserRepository((Application) getApplicationContext());

        userViewModel = new ViewModelProvider(getViewModelStore(), new UserViewModelFactory(userRepository)).get(UserViewModel.class);
        userViewModel.setAuthenticationError(false);

        introScreen = new IntroScreen(findViewById(android.R.id.content), this);

        emailEditText = findViewById(R.id.textInputEmail);
        passwordEditText = findViewById(R.id.textInputPassword);

        oneTapClient = Identity.getSignInClient(this);
        signInRequest = BeginSignInRequest.builder()
                .setPasswordRequestOptions(BeginSignInRequest.PasswordRequestOptions.builder()
                        .setSupported(true)
                        .build())
                .setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        // Your server's client ID, not your Android client ID.
                        .setServerClientId(getString(R.string.default_web_client_id))
                        // Only show accounts previously used to sign in.
                        .setFilterByAuthorizedAccounts(false)
                        .build())
                // Automatically sign in when exactly one credential is retrieved.
                .setAutoSelectEnabled(true)
                .build();

        startIntentSenderForResult = new ActivityResultContracts.StartIntentSenderForResult();

        activityResultLauncher = registerForActivityResult(startIntentSenderForResult, activityResult -> {
            if (activityResult.getResultCode() == Activity.RESULT_OK) {
                Log.d(TAG, "result.getResultCode() == Activity.RESULT_OK");
                try {
                    SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(activityResult.getData());
                    String idToken = credential.getGoogleIdToken();
                    if (idToken != null) {
                        // Got an ID token from Google. Use it to authenticate with Firebase.
                        AuthCredential googleCredential = GoogleAuthProvider.getCredential(idToken, null);
                        firebaseAuthWithGoogle(googleCredential);
                    }
                } catch (ApiException e) {
                    Snackbar.make(this.findViewById(android.R.id.content),
                            UNEXPECTED_ERROR, Snackbar.LENGTH_SHORT).show();
                }
            }
        });

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

        Button loginGoogleButton = findViewById(R.id.GoogleLoginButton);
        loginGoogleButton.setOnClickListener(v -> oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(this, new OnSuccessListener<BeginSignInResult>() {
                    @Override
                    public void onSuccess(BeginSignInResult result) {
                        Log.d(TAG, "onSuccess from oneTapClient.beginSignIn(BeginSignInRequest)");
                        IntentSenderRequest intentSenderRequest =
                                new IntentSenderRequest.Builder(result.getPendingIntent()).build();
                        activityResultLauncher.launch(intentSenderRequest);
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // No saved credentials found. Launch the One Tap sign-up flow, or
                        // do nothing and continue presenting the signed-out UI.
                        Log.d(TAG, Objects.requireNonNull(e.getLocalizedMessage()));

                        Snackbar.make(findViewById(android.R.id.content),
                                UNEXPECTED_ERROR,
                                Snackbar.LENGTH_SHORT).show();
                    }
                }));

        Button forgotPasswordButton = findViewById(R.id.forgotten_password_button);
        forgotPasswordButton.setOnClickListener(v -> {
            FragmentManager fragmentManager = getSupportFragmentManager();
            ForgotPasswordFragment forgotPasswordFragment = new ForgotPasswordFragment();
            forgotPasswordFragment.show(fragmentManager, "ForgotPasswordFragment");
        });
    }

    private void firebaseAuthWithGoogle(AuthCredential googleCredential) {
        mAuth.signInWithCredential(googleCredential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            userViewModel.getUserPreferences(userViewModel.getLoggedUser().getIdToken());
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            Intent intent = new Intent(WelcomeActivity.this, ProfileActivity.class);
                            startActivity(intent);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            // ... (handle sign-in failure)
                        }
                    }
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
    public void onStart() {

        Log.i("OnStart", Boolean.toString(fromSignOut));
        super.onStart();

        if (userViewModel.getLoggedUser() != null) {
            introScreen.showForAutoLogin();
            Log.i("OnStart", "Show Intro Screen AutoLogin");
            userViewModel.isAutoLoginEnabled(userViewModel.getLoggedUser().getIdToken()).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    boolean autoLoginEnabled = task.getResult();
                    if (autoLoginEnabled) {
                        new Handler().postDelayed(() -> {
                            startActivity(new Intent(WelcomeActivity.this, HomePageActivity.class));
                        }, 500); // 2 seconds delay
                    }
                }else {
                    Log.e(TAG, "Failed to get auto_login value", task.getException());
                }
            });

        } else if (!fromSignOut && !fromEmailVerification) {
            Log.i("OnStart", "Show Intro Screen");
            introScreen.showIntroScreen();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (introScreen != null) {
            introScreen.releaseResources();
        }
    }

    /*
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        tapDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

     */

    @Override
    protected void onResume() {
        //UIUtils.hideSystemUI(this);
        super.onResume();
    }

    private void resetHintPosition() {
        emailEditText.clearFocus();
        passwordEditText.clearFocus();
    }

}