
/*package com.the_coffe_coders.fastestlap.ui.welcome;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.repository.user.IUserRepository;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModel;
import com.the_coffe_coders.fastestlap.ui.welcome.viewmodel.UserViewModelFactory;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

import java.util.Objects;

public class SignUp {

    private static final String TAG = "RegisterPopup";
    private final Context context;
    private UserViewModel userViewModel;
    private TextInputEditText textInputEmail, textInputPassword;

    public SignUp(Context context) {
        this.context = context;

        IUserRepository userRepository = ServiceLocator.getInstance().getUserRepository((Application) context.getApplicationContext());

        userViewModel = new ViewModelProvider((ViewModelStoreOwner) context, new UserViewModelFactory(userRepository)).get(UserViewModel.class);
        userViewModel.setAuthenticationError(false);
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.register_pop_up, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(R.drawable.dialog_background);

        Button submitButton = dialogView.findViewById(R.id.submit_button);
        textInputEmail = dialogView.findViewById(R.id.textInputEmail);
        textInputPassword = dialogView.findViewById(R.id.textInputPassword);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = Objects.requireNonNull(textInputEmail.getText()).toString();
                String password = Objects.requireNonNull(textInputPassword.getText()).toString();
                Log.d(TAG, "Email: " + email);
                Log.d(TAG, "Password: " + password);
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}

 */