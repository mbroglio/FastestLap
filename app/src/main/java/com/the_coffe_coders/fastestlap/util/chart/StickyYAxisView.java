package com.the_coffe_coders.fastestlap.util.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.the_coffe_coders.fastestlap.R;

public class StickyYAxisView extends View {

    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int totalPositions = 21; // 1 to 20 + DNF
    private float topPadding;
    private float bottomPadding;

    public StickyYAxisView(Context context) {
        super(context);
        init(context);
    }

    public StickyYAxisView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public StickyYAxisView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        topPadding = dpToPx(36f);
        bottomPadding = dpToPx(48f);

        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setTextSize(dpToPx(10f));
        textPaint.setColor(Color.parseColor("#B1ADAD")); // timer_gray

        try {
            Typeface tf = ResourcesCompat.getFont(context, R.font.orbitron_bold);
            if (tf != null) {
                textPaint.setTypeface(tf);
            }
        } catch (Exception ignored) {
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        }

        borderPaint.setColor(Color.argb(50, 255, 255, 255));
        borderPaint.setStrokeWidth(dpToPx(1f));

        tickPaint.setColor(Color.argb(35, 255, 255, 255));
        tickPaint.setStrokeWidth(dpToPx(1f));
    }

    public void setGeometry(float topPaddingPx, float bottomPaddingPx, int totalPositions) {
        this.topPadding = topPaddingPx;
        this.bottomPadding = bottomPaddingPx;
        this.totalPositions = totalPositions;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int height = getHeight();
        int width = getWidth();
        if (height <= 0 || width <= 0) return;

        float availableHeight = height - topPadding - bottomPadding;
        if (availableHeight <= 0) return;

        float stepY = availableHeight / (totalPositions - 1);
        float textRightMargin = dpToPx(8f);

        for (int p = 1; p <= totalPositions; p++) {
            float y = topPadding + (p - 1) * stepY;

            // Draw horizontal tick matching chart grid line
            canvas.drawLine(width - dpToPx(6f), y, width, y, tickPaint);

            String label;
            if (p == totalPositions) {
                label = "DNF";
                textPaint.setColor(Color.parseColor("#FF6B6B")); // Coral red for DNF
            } else if (p == 1) {
                label = "P1";
                textPaint.setColor(Color.parseColor("#FFD700")); // Gold
            } else if (p == 2) {
                label = "P2";
                textPaint.setColor(Color.parseColor("#D1D5DB")); // Silver
            } else if (p == 3) {
                label = "P3";
                textPaint.setColor(Color.parseColor("#CD7F32")); // Bronze
            } else {
                label = "P" + p;
                textPaint.setColor(Color.parseColor("#9E9E9E"));
            }

            // Vertically center text with the tick line
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float textY = y - (fm.ascent + fm.descent) / 2f;

            canvas.drawText(label, width - textRightMargin, textY, textPaint);
        }

        // Draw vertical right divider line
        canvas.drawLine(width - 1f, 0, width - 1f, height, borderPaint);
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
