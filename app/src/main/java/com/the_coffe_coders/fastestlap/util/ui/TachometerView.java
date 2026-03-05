package com.the_coffe_coders.fastestlap.util.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.the_coffe_coders.fastestlap.R;

import java.util.Locale;

public class TachometerView extends View {
    private Paint arcPaint;
    private Paint backgroundArcPaint;
    private Paint textPaint;
    private Paint labelPaint;
    private Paint dotPaint;
    private float percentage = 0f;
    private float currentPercentage = 0f; // The animated value
    private String label = "";
    private RectF arcRect;
    private int color;
    private ValueAnimator animator;

    public TachometerView(Context context) {
        super(context);
        init(context);
    }

    public TachometerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TachometerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // Arc paint for the percentage fill
        arcPaint = new Paint();
        arcPaint.setAntiAlias(true);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(12f);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);
        color = ContextCompat.getColor(context, R.color.ferrari_f1);
        arcPaint.setColor(color);

        // Background arc paint
        backgroundArcPaint = new Paint();
        backgroundArcPaint.setAntiAlias(true);
        backgroundArcPaint.setStyle(Paint.Style.STROKE);
        backgroundArcPaint.setStrokeWidth(12f);
        backgroundArcPaint.setColor(ContextCompat.getColor(context, R.color.timer_gray_dark));

        // Text paint for percentage
        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(ContextCompat.getColor(context, R.color.white));
        textPaint.setTextSize(40f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        // Label paint
        labelPaint = new Paint();
        labelPaint.setAntiAlias(true);
        labelPaint.setColor(ContextCompat.getColor(context, R.color.white));
        labelPaint.setTextSize(24f);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        // Dot paint for the end indicator
        dotPaint = new Paint();
        dotPaint.setAntiAlias(true);
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(color);

        arcRect = new RectF();
    }

    public void setPercentage(float percentage) {
        this.percentage = Math.min(100f, Math.max(0f, percentage));
        animateToPercentage(this.percentage);
    }

    private void animateToPercentage(float targetPercentage) {
        // Cancel any existing animation
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }

        // Create animator from current percentage to target percentage
        animator = ValueAnimator.ofFloat(currentPercentage, targetPercentage);
        animator.setDuration(5000); // 1.5 seconds animation
        animator.setInterpolator(new DecelerateInterpolator());

        animator.addUpdateListener(animation -> {
            currentPercentage = (float) animation.getAnimatedValue();
            invalidate();
        });

        animator.start();
    }

    public void setLabel(String label) {
        this.label = label;
        invalidate();
    }

    public void setColor(int color) {
        this.color = color;
        arcPaint.setColor(color);
        dotPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int size = Math.min(width, height);
        int padding = 20;

        // Set up the arc rectangle
        arcRect.set(padding, padding, size - padding, size - padding);

        // Draw background arc (full circle from 135 to 405 degrees = 270 degrees)
        canvas.drawArc(arcRect, 135, 270, false, backgroundArcPaint);

        // Draw percentage arc (using animated currentPercentage)
        float sweepAngle = (currentPercentage / 100f) * 270f;
        canvas.drawArc(arcRect, 135, sweepAngle, false, arcPaint);

        // Draw dot at the end of the arc
        if (sweepAngle > 0) {
            float endAngle = 135 + sweepAngle;
            float radius = (size - 2 * padding) / 2f;
            float centerX = size / 2f;
            float centerY = size / 2f;

            // Calculate the position of the dot at the end of the arc
            double angleInRadians = Math.toRadians(endAngle);
            float dotX = centerX + radius * (float) Math.cos(angleInRadians);
            float dotY = centerY + radius * (float) Math.sin(angleInRadians);

            // Draw the dot (slightly larger than the stroke width for visibility)
            canvas.drawCircle(dotX, dotY, 14f, dotPaint);
        }

        // Draw percentage text (using animated currentPercentage)
        String percentageText = String.format(Locale.getDefault(), "%.0f%%", currentPercentage);
        float centerX = size / 2f;
        float centerY = size / 2f;

        // Draw percentage in the center
        canvas.drawText(percentageText, centerX, centerY + 10, textPaint);

        // Draw label below percentage
        if (!label.isEmpty()) {
            canvas.drawText(label, centerX, centerY + 45, labelPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = 120; // Default size in dp
        int sizeInPx = (int) (size * getResources().getDisplayMetrics().density);

        int width = resolveSize(sizeInPx, widthMeasureSpec);
        int height = resolveSize(sizeInPx, heightMeasureSpec);

        int finalSize = Math.min(width, height);
        setMeasuredDimension(finalSize, finalSize);
    }
}

