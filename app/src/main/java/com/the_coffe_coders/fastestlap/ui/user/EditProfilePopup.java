// EditProfilePopup.java
package com.the_coffe_coders.fastestlap.ui.user;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.viewpager.widget.ViewPager;

import com.google.android.material.textfield.TextInputEditText;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.ProfileImageAdapter;
import com.the_coffe_coders.fastestlap.utils.SimpleTextWatcher;

public class EditProfilePopup {

    private final Context context;
    private final View parentView;
    private final int originalProfileImage;
    private final String originalUsername;
    private int provisionalProfileImage;
    private boolean isUsernameModified = false;
    private boolean isProfileImageModified = false;
    private boolean isConfermIconClicked = false;
    LinearLayout buttonsLayout;
    Button dismissButton;
    Button saveButton;

    public EditProfilePopup(Context context, View parentView, int originalProfileImage, String originalUsername) {
        this.context = context;
        this.parentView = parentView;
        this.originalProfileImage = originalProfileImage;
        this.originalUsername = originalUsername;
    }

    public void show() {
        // Inflate the popup layout
        View popupView = LayoutInflater.from(context).inflate(R.layout.edit_profile_popup, null);

        int width = context.getResources().getDisplayMetrics().widthPixels * 3 / 4;

        // Create the popup window
        PopupWindow popupWindow = new PopupWindow(popupView,
                width,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);

        // Set the background drawable
        popupWindow.setBackgroundDrawable(context.getDrawable(R.drawable.round_background_grey));

        // Initialize the carousel and EditText
        LinearLayout profileImageCarouselLayout = popupView.findViewById(R.id.profile_image_layout);
        ViewPager profileImageCarousel = popupView.findViewById(R.id.profile_image_carousel);
        TextInputEditText profileNameText = popupView.findViewById(R.id.profile_name_text);
        ImageView editProfileName = popupView.findViewById(R.id.edit_profile_name);
        ImageView confirmEditProfileName = popupView.findViewById(R.id.confirm_edit_profile_name);
        ImageView revertEditProfileName = popupView.findViewById(R.id.revert_edit_profile_name);

        ImageView confirmEditProfileImage = popupView.findViewById(R.id.confirm_edit_profile_image);
        ImageView revertEditProfileImage = popupView.findViewById(R.id.revert_edit_profile_image);

        buttonsLayout = popupView.findViewById(R.id.buttons_layout);
        dismissButton = popupView.findViewById(R.id.dismiss_button);
        saveButton = popupView.findViewById(R.id.save_button);

        // Set up the carousel adapter
        int[] profileImages = {R.drawable.boy_icon, R.drawable.girl_icon};
        ProfileImageAdapter adapter = new ProfileImageAdapter(context, profileImages);
        profileImageCarousel.setAdapter(adapter);

        // Update the visibility of the confirm and revert icons
        profileImageCarousel.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                if (profileImages[position] != originalProfileImage) {
                    isProfileImageModified = true;
                    confirmEditProfileImage.setVisibility(View.VISIBLE);
                    revertEditProfileImage.setVisibility(View.VISIBLE);
                } else {
                    isProfileImageModified = false;
                    confirmEditProfileImage.setVisibility(View.INVISIBLE);
                    revertEditProfileImage.setVisibility(View.INVISIBLE);
                }
                checkForChanges();
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        // Handle the confirm profile image click
        confirmEditProfileImage.setOnClickListener(v -> {
            profileImageCarouselLayout.setBackground(context.getDrawable(R.drawable.background_all_margins_green_filled_white));
            provisionalProfileImage = profileImages[profileImageCarousel.getCurrentItem()];
            confirmEditProfileImage.setVisibility(View.INVISIBLE);
            isConfermIconClicked = true;
        });

        // Handle the revert profile image click
        revertEditProfileImage.setOnClickListener(v -> {
            revertEditingProfileImage(profileImages, profileImageCarousel,
                    profileImageCarouselLayout, confirmEditProfileImage, revertEditProfileImage);
        });

        // Handle the edit profile name click
        editProfileName.setOnClickListener(v -> {
            startEditing(profileNameText, editProfileName, confirmEditProfileName);
        });

        // Handle the confirm profile name click
        confirmEditProfileName.setOnClickListener(v -> {
            abortEditing(profileNameText, editProfileName, confirmEditProfileName);
        });

        // Handle the revert profile name click
        revertEditProfileName.setOnClickListener(v -> {
            revertEditing(profileNameText, originalUsername, revertEditProfileName);
        });

        profileNameText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isUsernameModified = !s.toString().equals(originalUsername);
                revertEditProfileName.setVisibility(isUsernameModified ? View.VISIBLE : View.INVISIBLE);
                checkForChanges();
            }
        });

        // Handle the dismiss button click
        dismissButton.setOnClickListener(v -> {
            if(isProfileImageModified){
                revertEditingProfileImage(profileImages, profileImageCarousel,
                        profileImageCarouselLayout, confirmEditProfileImage, revertEditProfileImage);
            }
            if(isUsernameModified){
                revertEditing(profileNameText, originalUsername, revertEditProfileName);
            }
            checkForChanges();
        });

        // Show the popup window
        popupWindow.showAtLocation(parentView, Gravity.CENTER, 0, 0);
    }

    private void startEditing(TextInputEditText inputText, ImageView editText, ImageView checkText) {
        inputText.setEnabled(true);
        inputText.requestFocus();
        inputText.setSelection(inputText.getText().length());
        editText.setVisibility(View.INVISIBLE);
        checkText.setVisibility(View.VISIBLE);
    }

    private void abortEditing(TextInputEditText inputText, ImageView editText, ImageView checkText) {
        inputText.setEnabled(false);
        editText.setVisibility(View.VISIBLE);
        checkText.setVisibility(View.INVISIBLE);
    }

    private void revertEditing(TextInputEditText inputText, String originalText, ImageView revertText) {
        inputText.setText(originalText);
        inputText.setSelection(inputText.getText().length());
        revertText.setVisibility(View.INVISIBLE);
    }

    private void revertEditingProfileImage(int[] profileImages, ViewPager profileImageCarousel, LinearLayout profileImageCarouselLayout, ImageView confirmEditProfileImage, ImageView revertEditProfileImage) {
        for (int i = 0; i < profileImages.length; i++) {
            if (profileImages[i] == originalProfileImage) {
                profileImageCarousel.setCurrentItem(i);
                break;
            }
        }
        provisionalProfileImage = originalProfileImage;
        profileImageCarouselLayout.setBackground(context.getDrawable(R.drawable.background_all_margins_filled_white));
        confirmEditProfileImage.setVisibility(View.INVISIBLE);
        revertEditProfileImage.setVisibility(View.INVISIBLE);
    }


    private void checkForChanges() {
        boolean hasChanges = isUsernameModified || isProfileImageModified;
        buttonsLayout.setVisibility(hasChanges ? View.VISIBLE : View.INVISIBLE);

    }



}
