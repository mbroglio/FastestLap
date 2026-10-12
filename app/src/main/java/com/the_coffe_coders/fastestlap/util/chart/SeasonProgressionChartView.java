package com.the_coffe_coders.fastestlap.util.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;

import com.the_coffe_coders.fastestlap.R;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

@Getter
public class SeasonProgressionChartView extends View {

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaintSecondary = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tooltipBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tooltipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int totalRaces = 24;
    private int totalPositions = 21; // 1 to 20 + DNF
    private int teamColor = Color.parseColor("#E10600");
    private int secondaryTeamColor = Color.parseColor("#E10600");
    private boolean isConstructorMode = false;

    private float columnWidth;
    private float topPadding;
    private float bottomPadding;
    private float startPadding;
    private float endPadding;

    private List<SeasonPerformancePoint> primaryPoints = new ArrayList<>();
    private List<SeasonPerformancePoint> secondaryPoints = new ArrayList<>();

    // Selected point for tooltip popup
    private SeasonPerformancePoint selectedPoint = null;
    private float selectedPointX = 0;
    private float selectedPointY = 0;

    public SeasonProgressionChartView(Context context) {
        super(context);
        init(context);
    }

    public SeasonProgressionChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SeasonProgressionChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        columnWidth = dpToPx(64f);
        topPadding = dpToPx(36f);
        bottomPadding = dpToPx(48f);
        startPadding = dpToPx(24f);
        endPadding = dpToPx(36f);

        // Very light semi-transparent gray grid lines (approx 14% white opacity)
        gridPaint.setColor(Color.argb(35, 255, 255, 255));
        gridPaint.setStrokeWidth(dpToPx(1f));
        gridPaint.setStyle(Paint.Style.STROKE);

        linePaint.setColor(teamColor);
        linePaint.setStrokeWidth(dpToPx(3f));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        linePaintSecondary.setColor(secondaryTeamColor);
        linePaintSecondary.setStrokeWidth(dpToPx(2.5f));
        linePaintSecondary.setStyle(Paint.Style.STROKE);
        linePaintSecondary.setStrokeCap(Paint.Cap.ROUND);
        linePaintSecondary.setStrokeJoin(Paint.Join.ROUND);
        linePaintSecondary.setPathEffect(new DashPathEffect(new float[]{dpToPx(6f), dpToPx(4f)}, 0));

        pointFillPaint.setStyle(Paint.Style.FILL);
        pointStrokePaint.setStyle(Paint.Style.STROKE);
        pointStrokePaint.setStrokeWidth(dpToPx(2f));

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(dpToPx(10f));
        textPaint.setColor(Color.WHITE);

        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(dpToPx(10f));
        labelPaint.setColor(Color.parseColor("#B1ADAD")); // timer_gray

        tooltipBgPaint.setColor(Color.argb(230, 28, 29, 36));
        tooltipBgPaint.setStyle(Paint.Style.FILL);

        tooltipTextPaint.setColor(Color.WHITE);
        tooltipTextPaint.setTextSize(dpToPx(11f));
        tooltipTextPaint.setTextAlign(Paint.Align.CENTER);

        try {
            Typeface tf = ResourcesCompat.getFont(context, R.font.orbitron_bold);
            if (tf != null) {
                textPaint.setTypeface(tf);
                labelPaint.setTypeface(tf);
                tooltipTextPaint.setTypeface(tf);
            }
        } catch (Exception ignored) {
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            labelPaint.setTypeface(Typeface.DEFAULT_BOLD);
            tooltipTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        }
    }

    public void setTotalRaces(int totalRaces) {
        if (totalRaces > 0) {
            this.totalRaces = totalRaces;
            requestLayout();
            invalidate();
        }
    }

    public void setTeamColor(int color) {
        this.teamColor = color;
        this.secondaryTeamColor = ColorUtils.blendARGB(color, Color.WHITE, 0.40f);
        linePaint.setColor(teamColor);
        linePaintSecondary.setColor(secondaryTeamColor);
        invalidate();
    }
    public void setData(List<SeasonPerformancePoint> primaryPoints,
                        @Nullable List<SeasonPerformancePoint> secondaryPoints,
                        boolean isConstructor) {
        this.primaryPoints = primaryPoints != null ? new ArrayList<>(primaryPoints) : new ArrayList<>();
        this.secondaryPoints = secondaryPoints != null ? new ArrayList<>(secondaryPoints) : new ArrayList<>();
        this.isConstructorMode = isConstructor;

        this.primaryPoints.sort((a, b) -> Integer.compare(a.getRound(), b.getRound()));
        this.secondaryPoints.sort((a, b) -> Integer.compare(a.getRound(), b.getRound()));

        selectedPoint = null;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (height <= 0) {
            height = (int) dpToPx(380f);
        }

        int desiredWidth = (int) (startPadding + endPadding + totalRaces * columnWidth);
        setMeasuredDimension(desiredWidth, height);
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

        // 1. Draw horizontal grid lines (for positions 1 to 21)
        for (int p = 1; p <= totalPositions; p++) {
            float y = topPadding + (p - 1) * stepY;
            canvas.drawLine(0, y, width, y, gridPaint);
        }

        // 2. Draw vertical grid lines & X-axis labels (for rounds 1 to totalRaces)
        for (int r = 1; r <= totalRaces; r++) {
            float x = getXForRound(r);
            canvas.drawLine(x, topPadding, x, height - bottomPadding, gridPaint);

            // X-axis label: "R1", "R2", etc.
            float labelY = height - bottomPadding + dpToPx(18f);
            canvas.drawText("R" + r, x, labelY, labelPaint);

            // Sub-label: "GP"
            float subLabelY = labelY + dpToPx(14f);
            labelPaint.setTextSize(dpToPx(8.5f));
            labelPaint.setColor(Color.parseColor("#777474"));
            canvas.drawText("GP", x, subLabelY, labelPaint);
            labelPaint.setTextSize(dpToPx(10f));
            labelPaint.setColor(Color.parseColor("#B1ADAD"));
        }

        // 3. Draw secondary line if Constructor mode
        if (isConstructorMode && !secondaryPoints.isEmpty()) {
            drawProgressionLine(canvas, secondaryPoints, stepY, linePaintSecondary);
        }

        // 4. Draw primary progression line
        if (!primaryPoints.isEmpty()) {
            drawProgressionLine(canvas, primaryPoints, stepY, linePaint);
        }

        // 5. Draw secondary points if Constructor mode
        if (isConstructorMode && !secondaryPoints.isEmpty()) {
            int darkerSecondary = ColorUtils.blendARGB(secondaryTeamColor, Color.BLACK, 0.40f);
            drawDataPoints(canvas, secondaryPoints, stepY, darkerSecondary, secondaryTeamColor);
        }

        // 6. Draw primary data points (darker shade of team color)
        if (!primaryPoints.isEmpty()) {
            int darkerTeamColor = ColorUtils.blendARGB(teamColor, Color.BLACK, 0.40f);
            drawDataPoints(canvas, primaryPoints, stepY, darkerTeamColor, teamColor);
        }

        // 7. Draw Tooltip popup if a point is selected
        if (selectedPoint != null) {
            drawTooltip(canvas, selectedPoint, selectedPointX, selectedPointY);
        }
    }

    private void drawProgressionLine(Canvas canvas, List<SeasonPerformancePoint> points, float stepY, Paint paint) {
        if (points.size() < 2) return;

        Path path = new Path();
        boolean first = true;

        for (SeasonPerformancePoint pt : points) {
            if (pt.getRound() < 1 || pt.getRound() > totalRaces) continue;

            float x = getXForRound(pt.getRound());
            float y = getYForPosition(pt.getPosition(), stepY);

            if (first) {
                path.moveTo(x, y);
                first = false;
            } else {
                path.lineTo(x, y);
            }
        }

        canvas.drawPath(path, paint);
    }

    private void drawDataPoints(Canvas canvas, List<SeasonPerformancePoint> points, float stepY,
                                int fillColor, int strokeColor) {
        float pointRadius = dpToPx(6.5f);

        for (SeasonPerformancePoint pt : points) {
            if (pt.getRound() < 1 || pt.getRound() > totalRaces) continue;

            float x = getXForRound(pt.getRound());
            float y = getYForPosition(pt.getPosition(), stepY);

            pointFillPaint.setColor(fillColor);
            pointStrokePaint.setColor(strokeColor);

            if (pt.isDnf()) {
                // Special styling for DNF: cross or darker badge
                pointFillPaint.setColor(Color.parseColor("#B71C1C")); // Dark red
                canvas.drawCircle(x, y, pointRadius, pointFillPaint);

                pointStrokePaint.setColor(Color.parseColor("#FF5252"));
                canvas.drawCircle(x, y, pointRadius, pointStrokePaint);

                // Draw tiny cross inside
                Paint crossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                crossPaint.setColor(Color.WHITE);
                crossPaint.setStrokeWidth(dpToPx(1.5f));
                float delta = dpToPx(3f);
                canvas.drawLine(x - delta, y - delta, x + delta, y + delta, crossPaint);
                canvas.drawLine(x - delta, y + delta, x + delta, y - delta, crossPaint);
            } else {
                // Circle point with darker team color inside and team color ring
                canvas.drawCircle(x, y, pointRadius, pointFillPaint);
                canvas.drawCircle(x, y, pointRadius, pointStrokePaint);

                // Small center highlight dot
                pointFillPaint.setColor(Color.argb(160, 255, 255, 255));
                canvas.drawCircle(x, y, dpToPx(2f), pointFillPaint);
            }
        }
    }

    private void drawTooltip(Canvas canvas, SeasonPerformancePoint pt, float x, float y) {
        String line1 = pt.getRoundLabel() + ": " + (pt.getRaceName() != null ? pt.getRaceName() : "GP");
        String line2 = (pt.getEntityName() != null ? pt.getEntityName() + " - " : "") +
                (pt.isDnf() ? "DNF / Ritirato" : "Posizione " + pt.getPositionText());

        float padding = dpToPx(8f);
        float textWidth1 = tooltipTextPaint.measureText(line1);
        float textWidth2 = tooltipTextPaint.measureText(line2);
        float boxWidth = Math.max(textWidth1, textWidth2) + padding * 3;
        float boxHeight = dpToPx(36f);

        float boxLeft = x - boxWidth / 2f;
        float boxTop = y - boxHeight - dpToPx(14f);

        // Keep tooltip inside view bounds
        if (boxLeft < 10) boxLeft = 10;
        if (boxLeft + boxWidth > getWidth() - 10) boxLeft = getWidth() - boxWidth - 10;
        if (boxTop < 10) boxTop = y + dpToPx(16f); // Flip downward if near top

        RectF rect = new RectF(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight);
        canvas.drawRoundRect(rect, dpToPx(6f), dpToPx(6f), tooltipBgPaint);

        // Border around tooltip
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(teamColor);
        borderPaint.setStrokeWidth(dpToPx(1f));
        canvas.drawRoundRect(rect, dpToPx(6f), dpToPx(6f), borderPaint);

        float textCenterX = rect.centerX();
        float textY1 = boxTop + dpToPx(14f);
        float textY2 = textY1 + dpToPx(14f);

        tooltipTextPaint.setTextSize(dpToPx(9.5f));
        tooltipTextPaint.setColor(Color.parseColor("#B1ADAD"));
        canvas.drawText(line1, textCenterX, textY1, tooltipTextPaint);

        tooltipTextPaint.setTextSize(dpToPx(10.5f));
        tooltipTextPaint.setColor(Color.WHITE);
        canvas.drawText(line2, textCenterX, textY2, tooltipTextPaint);
    }

    private float getXForRound(int round) {
        return startPadding + (round - 0.5f) * columnWidth;
    }

    private float getYForPosition(int position, float stepY) {
        int clampedPos = Math.max(1, Math.min(position, totalPositions));
        return topPadding + (clampedPos - 1) * stepY;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float touchX = event.getX();
            float touchY = event.getY();
            float touchThreshold = dpToPx(24f);

            SeasonPerformancePoint closestPoint = null;
            float closestDist = Float.MAX_VALUE;
            float closestX = 0;
            float closestY = 0;

            float stepY = (getHeight() - topPadding - bottomPadding) / (totalPositions - 1);

            List<SeasonPerformancePoint> allPoints = new ArrayList<>(primaryPoints);
            allPoints.addAll(secondaryPoints);

            for (SeasonPerformancePoint pt : allPoints) {
                float px = getXForRound(pt.getRound());
                float py = getYForPosition(pt.getPosition(), stepY);

                float dist = (float) Math.hypot(touchX - px, touchY - py);
                if (dist < touchThreshold && dist < closestDist) {
                    closestDist = dist;
                    closestPoint = pt;
                    closestX = px;
                    closestY = py;
                }
            }

            if (closestPoint != null) {
                if (selectedPoint == closestPoint) {
                    selectedPoint = null; // Toggle off if tapped again
                } else {
                    selectedPoint = closestPoint;
                    selectedPointX = closestX;
                    selectedPointY = closestY;
                }
                invalidate();
                return true;
            } else if (selectedPoint != null) {
                selectedPoint = null;
                invalidate();
            }
        }
        return true;
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
