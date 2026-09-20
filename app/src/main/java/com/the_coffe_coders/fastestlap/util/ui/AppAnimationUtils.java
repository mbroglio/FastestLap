package com.the_coffe_coders.fastestlap.util.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.the_coffe_coders.fastestlap.R;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Centralized utility class in util.ui package dedicated to defining and managing animations across the application:
 * - Intro screen animation sequence (logo slide-in, app name slide-up, typewriter credits, progress indicator)
 * - Letter-by-letter typing entrance with realistic mechanical tremor / vibration shake
 * - Multi-axis decaying shake animations (scuotimento / tremolio)
 * - General typewriter text animation
 * - Safe lifecycle management and cancellation
 */
public class AppAnimationUtils {

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final WeakHashMap<View, List<Runnable>> RUNNABLES_MAP = new WeakHashMap<>();
    private static final WeakHashMap<View, Animator> ANIMATORS_MAP = new WeakHashMap<>();

    @FunctionalInterface
    public interface OnLetterTypedListener {
        void onLetterTyped(int index, char letter);
    }

    /**
     * Executes the complete intro screen animation sequence:
     * 1. Slides in the app logo (with optional engine roar audio callback).
     * 2. Slides up the app name (after 1800ms).
     * 3. Types the app credits letter by letter (after 1000ms, with typewriter sound callback).
     * 4. Reveals the circular progress indicator.
     * 5. Invokes onComplete callback (e.g. to navigate to the welcome page).
     *
     * @param appLogo             Logo ImageView to animate with slide_in
     * @param appName             App name TextView to animate with slide_up
     * @param appCredits          Credits TextView for typewriter typing
     * @param creditsText         Complete credits text string
     * @param progressIndicator   Optional progress indicator to show at the end
     * @param onLogoStart         Optional callback triggered when logo animation begins
     * @param onCreditsLetterTyped Optional callback invoked on each typed character of the credits
     * @param onComplete          Optional callback invoked at the conclusion of the intro sequence
     */
    public static void animateIntroSequence(
            @NonNull final View appLogo,
            @NonNull final TextView appName,
            @NonNull final TextView appCredits,
            @NonNull final String creditsText,
            @Nullable final View progressIndicator,
            @Nullable final Runnable onLogoStart,
            @Nullable final OnLetterTypedListener onCreditsLetterTyped,
            @Nullable final Runnable onComplete
    ) {
        if (!isViewAlive(appLogo)) return;

        cancelAnimations(appLogo, appName, appCredits, progressIndicator);
        Context context = appLogo.getContext();
        final List<Runnable> sequenceRunnables = new ArrayList<>();

        // 1. Logo slide-in animation
        Animation logoAnimation = android.view.animation.AnimationUtils.loadAnimation(context, R.anim.slide_in);
        appLogo.setVisibility(View.VISIBLE);
        appLogo.startAnimation(logoAnimation);

        if (onLogoStart != null) {
            onLogoStart.run();
        }

        // 2. Wait for logo slide-in (~1800ms), then slide up app name
        Runnable nameStep = () -> {
            if (!isViewAlive(appName)) return;
            Animation nameAnimation = android.view.animation.AnimationUtils.loadAnimation(context, R.anim.slide_up);
            appName.setVisibility(View.VISIBLE);
            appName.startAnimation(nameAnimation);

            // 3. Wait for app name animation (~1000ms), then start typewriter typing on credits
            Runnable creditsStep = () -> {
                if (!isViewAlive(appCredits)) return;
                appCredits.setVisibility(View.VISIBLE);
                appCredits.setText("");

                animateTextTyping(appCredits, creditsText, 90, onCreditsLetterTyped, () -> {
                    // 4. When credits finish typing, show progress indicator
                    if (progressIndicator != null && isViewAlive(progressIndicator)) {
                        progressIndicator.setVisibility(View.VISIBLE);
                    }

                    // 5. Brief 800ms pause before proceeding to welcome page
                    Runnable finishStep = () -> {
                        if (!isViewAlive(appLogo)) return;
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    };
                    MAIN_HANDLER.postDelayed(finishStep, 800);
                    sequenceRunnables.add(finishStep);
                });
            };
            MAIN_HANDLER.postDelayed(creditsStep, 1000);
            sequenceRunnables.add(creditsStep);
        };
        MAIN_HANDLER.postDelayed(nameStep, 1800);
        sequenceRunnables.add(nameStep);

        RUNNABLES_MAP.put(appLogo, sequenceRunnables);
    }

    /**
     * Animates text entering letter by letter (typewriter style).
     *
     * @param textView         Target TextView to display the animated text
     * @param fullText         The complete string to type
     * @param charDelayMs      Delay between characters in milliseconds
     * @param letterListener   Optional callback invoked for each letter typed
     * @param onComplete       Optional callback invoked when all letters are typed
     */
    public static void animateTextTyping(
            @NonNull final TextView textView,
            @Nullable final String fullText,
            final long charDelayMs,
            @Nullable final OnLetterTypedListener letterListener,
            @Nullable final Runnable onComplete
    ) {
        if (textView == null) return;
        cancelAnimations(textView);

        if (fullText == null || fullText.isEmpty()) {
            textView.setText("");
            if (onComplete != null && isViewAlive(textView)) {
                onComplete.run();
            }
            return;
        }

        textView.setVisibility(View.VISIBLE);
        textView.setText("");

        final int length = fullText.length();
        final List<Runnable> scheduledRunnables = new ArrayList<>();

        for (int i = 0; i < length; i++) {
            final int index = i;
            final char c = fullText.charAt(index);
            final String partialText = fullText.substring(0, index + 1);

            Runnable letterRunnable = () -> {
                if (!isViewAlive(textView)) return;
                textView.setText(partialText);

                if (letterListener != null) {
                    letterListener.onLetterTyped(index, c);
                }

                if (index == length - 1) {
                    if (onComplete != null && isViewAlive(textView)) {
                        onComplete.run();
                    }
                }
            };

            long delay = (i + 1) * Math.max(10, charDelayMs);
            MAIN_HANDLER.postDelayed(letterRunnable, delay);
            scheduledRunnables.add(letterRunnable);
        }

        RUNNABLES_MAP.put(textView, scheduledRunnables);
    }

    /**
     * Animates text entering letter by letter and, once all letters have arrived at their
     * final position, executes a realistic racing tremor / vibration shake before invoking
     * the completion callback.
     *
     * @param textView         Target TextView to display the animated text
     * @param fullText         The complete string to be animated
     * @param charDelayMs      Delay in milliseconds between successive letters (e.g. 70-90ms)
     * @param letterListener   Optional callback invoked for each letter rendered (e.g. for sound effects)
     * @param onComplete       Optional callback invoked when typing, shake, and hold complete
     */
    public static void animateTextTypingWithTremor(
            @NonNull final TextView textView,
            @Nullable final String fullText,
            final long charDelayMs,
            @Nullable final OnLetterTypedListener letterListener,
            @Nullable final Runnable onComplete
    ) {
        if (textView == null) return;
        cancelAnimations(textView);

        if (fullText == null || fullText.isEmpty()) {
            textView.setText("");
            if (onComplete != null && isViewAlive(textView)) {
                onComplete.run();
            }
            return;
        }

        textView.setVisibility(View.VISIBLE);
        textView.setText("");
        textView.setTranslationX(0f);
        textView.setTranslationY(0f);
        textView.setRotation(0f);

        final int length = fullText.length();
        final List<Runnable> scheduledRunnables = new ArrayList<>();

        for (int i = 0; i < length; i++) {
            final int index = i;
            final char c = fullText.charAt(index);
            final String partialText = fullText.substring(0, index + 1);

            Runnable letterRunnable = () -> {
                if (!isViewAlive(textView)) return;
                textView.setText(partialText);

                if (letterListener != null) {
                    letterListener.onLetterTyped(index, c);
                }

                // When the final letter has arrived at the destination, trigger tremor / shake
                if (index == length - 1) {
                    Runnable shakeTrigger = () -> {
                        if (!isViewAlive(textView)) return;
                        animateShake(textView, 450, () -> {
                            // Hold for 250ms so user can comfortably read the settled title
                            Runnable endRunnable = () -> {
                                if (!isViewAlive(textView)) return;
                                if (onComplete != null) {
                                    onComplete.run();
                                }
                            };
                            MAIN_HANDLER.postDelayed(endRunnable, 250);
                            scheduledRunnables.add(endRunnable);
                        });
                    };
                    MAIN_HANDLER.postDelayed(shakeTrigger, 80);
                    scheduledRunnables.add(shakeTrigger);
                }
            };

            long delay = (i + 1) * Math.max(20, charDelayMs);
            MAIN_HANDLER.postDelayed(letterRunnable, delay);
            scheduledRunnables.add(letterRunnable);
        }

        RUNNABLES_MAP.put(textView, scheduledRunnables);
    }

    /**
     * Convenience overload using default 80ms char delay without sound listener.
     */
    public static void animateTextTypingWithTremor(
            @NonNull final TextView textView,
            @Nullable final String fullText,
            @Nullable final Runnable onComplete
    ) {
        animateTextTypingWithTremor(textView, fullText, 80, null, onComplete);
    }

    /**
     * Executes a realistic multi-axis mechanical vibration / tremor (scuotimento) on a view,
     * decaying smoothly to rest over the specified duration.
     *
     * @param view         View to shake
     * @param durationMs   Duration in milliseconds of the shake animation (e.g. 450ms)
     * @param onComplete   Optional callback invoked after the animation ends
     */
    public static void animateShake(
            @NonNull final View view,
            final long durationMs,
            @Nullable final Runnable onComplete
    ) {
        if (view == null) return;
        cancelAnimations(view);

        if (!isViewAlive(view)) return;

        float density = view.getResources().getDisplayMetrics().density;
        if (density <= 0) density = 1f;

        // Translation X keyframes: rapid decaying horizontal oscillation
        Keyframe kx0 = Keyframe.ofFloat(0.00f, 0f);
        Keyframe kx1 = Keyframe.ofFloat(0.08f, -14f * density);
        Keyframe kx2 = Keyframe.ofFloat(0.16f, 14f * density);
        Keyframe kx3 = Keyframe.ofFloat(0.24f, -11f * density);
        Keyframe kx4 = Keyframe.ofFloat(0.32f, 11f * density);
        Keyframe kx5 = Keyframe.ofFloat(0.40f, -8f * density);
        Keyframe kx6 = Keyframe.ofFloat(0.48f, 8f * density);
        Keyframe kx7 = Keyframe.ofFloat(0.56f, -6f * density);
        Keyframe kx8 = Keyframe.ofFloat(0.64f, 6f * density);
        Keyframe kx9 = Keyframe.ofFloat(0.72f, -4f * density);
        Keyframe kx10 = Keyframe.ofFloat(0.80f, 4f * density);
        Keyframe kx11 = Keyframe.ofFloat(0.88f, -2f * density);
        Keyframe kx12 = Keyframe.ofFloat(0.94f, 2f * density);
        Keyframe kx13 = Keyframe.ofFloat(1.00f, 0f);

        PropertyValuesHolder pvhX = PropertyValuesHolder.ofKeyframe(
                View.TRANSLATION_X,
                kx0, kx1, kx2, kx3, kx4, kx5, kx6, kx7, kx8, kx9, kx10, kx11, kx12, kx13
        );

        // Translation Y keyframes: subtle vertical rumble
        Keyframe ky0 = Keyframe.ofFloat(0.00f, 0f);
        Keyframe ky1 = Keyframe.ofFloat(0.12f, -5f * density);
        Keyframe ky2 = Keyframe.ofFloat(0.28f, 5f * density);
        Keyframe ky3 = Keyframe.ofFloat(0.44f, -3f * density);
        Keyframe ky4 = Keyframe.ofFloat(0.60f, 3f * density);
        Keyframe ky5 = Keyframe.ofFloat(0.76f, -2f * density);
        Keyframe ky6 = Keyframe.ofFloat(0.90f, 1f * density);
        Keyframe ky7 = Keyframe.ofFloat(1.00f, 0f);

        PropertyValuesHolder pvhY = PropertyValuesHolder.ofKeyframe(
                View.TRANSLATION_Y,
                ky0, ky1, ky2, ky3, ky4, ky5, ky6, ky7
        );

        // Rotation keyframes: subtle angular vibration
        Keyframe kr0 = Keyframe.ofFloat(0.00f, 0f);
        Keyframe kr1 = Keyframe.ofFloat(0.12f, -1.6f);
        Keyframe kr2 = Keyframe.ofFloat(0.28f, 1.6f);
        Keyframe kr3 = Keyframe.ofFloat(0.48f, -1.0f);
        Keyframe kr4 = Keyframe.ofFloat(0.68f, 1.0f);
        Keyframe kr5 = Keyframe.ofFloat(0.88f, -0.4f);
        Keyframe kr6 = Keyframe.ofFloat(1.00f, 0f);

        PropertyValuesHolder pvhR = PropertyValuesHolder.ofKeyframe(
                View.ROTATION,
                kr0, kr1, kr2, kr3, kr4, kr5, kr6
        );

        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(view, pvhX, pvhY, pvhR);
        animator.setDuration(durationMs > 0 ? durationMs : 450);

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setTranslationX(0f);
                view.setTranslationY(0f);
                view.setRotation(0f);
                ANIMATORS_MAP.remove(view);
                if (onComplete != null && isViewAlive(view)) {
                    onComplete.run();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                view.setTranslationX(0f);
                view.setTranslationY(0f);
                view.setRotation(0f);
                ANIMATORS_MAP.remove(view);
            }
        });

        ANIMATORS_MAP.put(view, animator);
        animator.start();
    }

    /**
     * Executes shake animation with default duration (450ms).
     */
    public static void animateShake(@NonNull final View view) {
        animateShake(view, 450, null);
    }

    /**
     * Cancels any pending runnables and active animators for the given views,
     * clearing animations and resetting translations and rotations to zero.
     */
    public static void cancelAnimations(@Nullable View... views) {
        if (views == null) return;
        for (View view : views) {
            if (view == null) continue;

            List<Runnable> runnables = RUNNABLES_MAP.remove(view);
            if (runnables != null) {
                for (Runnable r : runnables) {
                    MAIN_HANDLER.removeCallbacks(r);
                }
            }

            Animator anim = ANIMATORS_MAP.remove(view);
            if (anim != null) {
                anim.cancel();
            }

            view.clearAnimation();
            view.setTranslationX(0f);
            view.setTranslationY(0f);
            view.setRotation(0f);
        }
    }

    private static boolean isViewAlive(View view) {
        if (view == null) return false;
        Context context = view.getContext();
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            return !activity.isFinishing() && !activity.isDestroyed();
        }
        return true;
    }
}
