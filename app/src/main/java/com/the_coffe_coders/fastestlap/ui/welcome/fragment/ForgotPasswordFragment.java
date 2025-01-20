package com.the_coffe_coders.fastestlap.ui.welcome.fragment;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.repository.user.IUserRepository;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModel;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModelFactory;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;


import org.apache.commons.validator.routines.EmailValidator;

import java.util.Objects;

public class ForgotPasswordFragment extends DialogFragment {

    private static final String TAG = "ForgotPasswordFragment";
    private TextInputEditText emailEditText;
    private RelativeLayout sendEmailButton;
    private FirebaseAuth mAuth;
    private ProgressBar progressIndicator;
    private UserViewModel userViewModel;

    public interface OnFragmentInteractionListener {
        void onFragmentDismissed(String value);
    }

    private OnFragmentInteractionListener mListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public ForgotPasswordFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forgot_password, container, false);

        IUserRepository userRepository = ServiceLocator.getInstance().getUserRepository((Application) requireActivity().getApplicationContext());

        userViewModel = new ViewModelProvider(this, new UserViewModelFactory(userRepository)).get(UserViewModel.class);

        mAuth = FirebaseAuth.getInstance();

        emailEditText = view.findViewById(R.id.textInputEmail);
        sendEmailButton = view.findViewById(R.id.send_email_button);
        sendEmailButton.setVisibility(View.VISIBLE);
        progressIndicator = view.findViewById(R.id.progress_indicator);
        progressIndicator.setVisibility(View.INVISIBLE);


        sendEmailButton.setOnClickListener(v -> {
            if (emailEditText.getText() != null && isEmailOk(emailEditText.getText().toString())) {
                String email = emailEditText.getText().toString();
                userViewModel.getUserMutableLiveData(email, "default", true).observe(
                        this, result -> {
                            if (result.isSuccess()) {
                                progressIndicator.setVisibility(View.VISIBLE);
                                sendEmailButton.setVisibility(View.INVISIBLE);
                                mAuth.sendPasswordResetEmail(email)
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(getContext(), "Email sent, check your inbox", Toast.LENGTH_SHORT).show();
                                                if (mListener != null) {
                                                    mListener.onFragmentDismissed("true");
                                                }
                                                dismiss();
                                            } else {
                                                progressIndicator.setVisibility(View.INVISIBLE);
                                                sendEmailButton.setVisibility(View.VISIBLE);
                                                emailEditText.setError("Error Email Login");
                                                Toast.makeText(getContext(), "Error sending email", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }else{
                                Toast.makeText(getContext(), "Email not found", Toast.LENGTH_SHORT).show();
                            }

                        });
            }
        });

        return view;
    }

    private boolean isEmailOk(String email) {
        // Check if the email is valid through the use of this library:
        // https://commons.apache.org/proper/commons-validator/
        if (!EmailValidator.getInstance().isValid((email))) {
            //emailEditText.setError("Error Email Login");
            Toast.makeText(getContext(), "Invalid email", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            emailEditText.setError(null);
            return true;
        }
    }

}