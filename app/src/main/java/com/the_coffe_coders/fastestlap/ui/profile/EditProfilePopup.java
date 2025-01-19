package com.the_coffe_coders.fastestlap.ui.profile;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.textfield.TextInputEditText;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.ProfileImageAdapter;
import com.the_coffe_coders.fastestlap.util.SimpleTextWatcher;

import java.util.Objects;


public class EditProfilePopup {

    private final Context context;
    private final View parentView;
    private final int originalProfileImage;
    private final String originalUsername;
    LinearLayout buttonsLayout;
    Button dismissButton;
    Button saveButton;
    ImageView confirmEditProfileImage;
    ImageView revertEditProfileImage;
    LinearLayout profileImageCarouselLayout;
    ViewPager profileImageCarousel;
    TextInputEditText profileNameText;
    ImageView editProfileName;
    ImageView confirmEditProfileName;
    ImageView revertEditProfileName;
    int[] profileImages = {R.drawable.boy_icon, R.drawable.girl_icon, R.drawable.anonymous_user_icon};
    private int provisionalProfileImage;
    private boolean isUsernameModified = false;
    private boolean isProfileImageModified = false;
    private boolean isConfirmIconClicked = false;


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
        popupWindow.setBackgroundDrawable(AppCompatResources.getDrawable(context, R.drawable.circle_background_dark_grey));

        // Initialize the carousel and EditText
        profileImageCarouselLayout = popupView.findViewById(R.id.profile_image_layout);
        profileImageCarousel = popupView.findViewById(R.id.profile_image_carousel);
        profileNameText = popupView.findViewById(R.id.profile_name_text);
        editProfileName = popupView.findViewById(R.id.edit_profile_name);
        confirmEditProfileName = popupView.findViewById(R.id.confirm_edit_profile_name);
        revertEditProfileName = popupView.findViewById(R.id.revert_edit_profile_name);

        confirmEditProfileImage = popupView.findViewById(R.id.confirm_edit_profile_image);
        revertEditProfileImage = popupView.findViewById(R.id.revert_edit_profile_image);

        buttonsLayout = popupView.findViewById(R.id.buttons_layout);
        dismissButton = popupView.findViewById(R.id.dismiss_button);
        saveButton = popupView.findViewById(R.id.save_button);

        // Set up the carousel adapter

        profileImages = rearrangeProfileImages(profileImages, originalProfileImage);
        ProfileImageAdapter adapter = new ProfileImageAdapter(context, profileImages);
        profileImageCarousel.setAdapter(adapter);
        profileImageCarousel.setCurrentItem(0);

        // Update the visibility of the confirm and revert icons
        profileImageCarousel.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (profileImages[position] != originalProfileImage) {
                    showImageCarouselButtons(true, View.VISIBLE, View.VISIBLE);
                } else {
                    showImageCarouselButtons(false, View.INVISIBLE, View.INVISIBLE);
                }
                checkForChanges();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        // Handle the confirm profile image click
        confirmEditProfileImage.setOnClickListener(v -> {
            confirmEditingProfileImage(profileImages, profileImageCarousel,
                    profileImageCarouselLayout, confirmEditProfileImage, revertEditProfileImage);
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
            revertChanges();
        });

        saveButton.setOnClickListener(v -> {
            saveChanges();
            popupWindow.dismiss();
        });

        // Show the popup window
        popupWindow.showAtLocation(parentView, Gravity.CENTER, 0, 0);
    }

    private void showImageCarouselButtons(boolean modified, int confirmVisibility, int revertVisibility) {
        isProfileImageModified = modified;
        confirmEditProfileImage.setVisibility(confirmVisibility);
        revertEditProfileImage.setVisibility(revertVisibility);
    }

    private int[] rearrangeProfileImages(int[] profileImages, int originalProfileImage) {
        int[] rearrangedImages = new int[profileImages.length];
        rearrangedImages[0] = originalProfileImage;
        int index = 1;
        for (int image : profileImages) {
            if (image != originalProfileImage) {
                rearrangedImages[index++] = image;
            }
        }
        return rearrangedImages;
    }

    private void startEditing(TextInputEditText inputText, ImageView editText, ImageView checkText) {
        inputText.setEnabled(true);
        inputText.requestFocus();
        inputText.setSelection(Objects.requireNonNull(inputText.getText()).length());
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(profileNameText, InputMethodManager.SHOW_IMPLICIT);
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
        inputText.setSelection(Objects.requireNonNull(inputText.getText()).length());
        revertText.setVisibility(View.INVISIBLE);
    }

    private void confirmEditingProfileImage(int[] profileImages, ViewPager profileImageCarousel, LinearLayout profileImageCarouselLayout, ImageView confirmEditProfileImage, ImageView revertEditProfileImage) {
        setViewPagerScrollable(profileImageCarousel, false);
        profileImageCarouselLayout.setBackground(AppCompatResources.getDrawable(context, R.drawable.background_all_margins_green_filled_white));
        provisionalProfileImage = profileImages[profileImageCarousel.getCurrentItem()];
        confirmEditProfileImage.setVisibility(View.INVISIBLE);
        isConfirmIconClicked = true;
        checkForChanges();
    }

    private void revertEditingProfileImage(int[] profileImages, ViewPager profileImageCarousel, LinearLayout profileImageCarouselLayout, ImageView confirmEditProfileImage, ImageView revertEditProfileImage) {
        for (int i = 0; i < profileImages.length; i++) {
            if (profileImages[i] == originalProfileImage) {
                profileImageCarousel.setCurrentItem(i);
                break;
            }
        }
        setViewPagerScrollable(profileImageCarousel, true);
        provisionalProfileImage = originalProfileImage;
        profileImageCarouselLayout.setBackground(AppCompatResources.getDrawable(context, R.drawable.background_all_margins_filled_white));
        confirmEditProfileImage.setVisibility(View.INVISIBLE);
        revertEditProfileImage.setVisibility(View.INVISIBLE);
        isConfirmIconClicked = false;
    }

    private void revertChanges() {
        if (isProfileImageModified) {
            revertEditingProfileImage(profileImages, profileImageCarousel,
                    profileImageCarouselLayout, confirmEditProfileImage, revertEditProfileImage);
        }
        if (isUsernameModified) {
            revertEditing(profileNameText, originalUsername, revertEditProfileName);
        }
        checkForChanges();
    }

    private void saveChanges() {
        if (context instanceof EditProfileListener) {
            EditProfileListener listener = (EditProfileListener) context;
            if (isProfileImageModified && isConfirmIconClicked) {
                listener.onProfileImageChanged(provisionalProfileImage);
            }
            if (isUsernameModified) {
                listener.onUsernameChanged(Objects.requireNonNull(profileNameText.getText()).toString());
            }
        }
    }

    private void checkForChanges() {
        boolean hasChanges = isUsernameModified || (isProfileImageModified && isConfirmIconClicked);
        buttonsLayout.setVisibility(hasChanges ? View.VISIBLE : View.GONE);

    }

    private void setViewPagerScrollable(ViewPager viewPager, boolean scrollable) {
        viewPager.setOnTouchListener((v, event) -> !scrollable);
    }

    public interface EditProfileListener {
        void onProfileImageChanged(int newProfileImage);

        void onUsernameChanged(String newUsername);
    }


}
