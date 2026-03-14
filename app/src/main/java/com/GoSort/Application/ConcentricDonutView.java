package com.GoSort.Application;

import android.content.Context;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ConcentricDonutView extends View {

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Integer> colors = new ArrayList<>();
    // target values (what setData was asked to show)
    private final List<Float> targetValues = new ArrayList<>();
    // currently displayed (animated) values
    private final List<Float> displayedValues = new ArrayList<>();
    private ValueAnimator animator;

    public ConcentricDonutView(Context context) {
        super(context);
        init();
    }

    public ConcentricDonutView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ConcentricDonutView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setColor(0xFFECECEC); // light gray background ring
        backgroundPaint.setStrokeCap(Paint.Cap.ROUND);

        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setData(int[] vals, int[] cols) {
        colors.clear();
        targetValues.clear();
        if (vals == null || cols == null) return;
        int n = Math.min(vals.length, cols.length);
        for (int i = 0; i < n; i++) {
            targetValues.add((float) vals[i]);
            colors.add(cols[i]);
        }

        // Ensure displayedValues has same size
        while (displayedValues.size() < targetValues.size()) {
            displayedValues.add(0f);
        }
        if (displayedValues.size() > targetValues.size()) {
            // trim extras
            for (int i = displayedValues.size() - 1; i >= targetValues.size(); i--) {
                displayedValues.remove(i);
            }
        }

        // If there's an ongoing animation, cancel it
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }

        // If displayedValues are equal to targetValues, just redraw
        boolean equal = true;
        for (int i = 0; i < targetValues.size(); i++) {
            if (i >= displayedValues.size() || !displayedValues.get(i).equals(targetValues.get(i))) {
                equal = false; break;
            }
        }
        if (equal) {
            invalidate();
            return;
        }

        // Animate from displayedValues -> targetValues
        final List<Float> start = new ArrayList<>(displayedValues);
        final List<Float> end = new ArrayList<>(targetValues);
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(600);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float frac = (float) animation.getAnimatedValue();
                // interpolate each value
                for (int i = 0; i < end.size(); i++) {
                    float s = i < start.size() ? start.get(i) : 0f;
                    float t = end.get(i);
                    float v = s + (t - s) * frac;
                    if (i < displayedValues.size()) {
                        displayedValues.set(i, v);
                    } else {
                        displayedValues.add(v);
                    }
                }
                invalidate();
            }
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

    if (displayedValues.isEmpty()) return;

    // If all values are zero or negative, nothing to draw
    boolean anyPositive = false;
    for (Float v : displayedValues) {
        if (v != null && v > 0f) { anyPositive = true; break; }
    }
    if (!anyPositive) return;

        int w = getWidth();
        int h = getHeight();
        int cx = w / 2;
        int cy = h / 2;

        // base radius is half of min dimension * 0.9
        float baseRadius = Math.min(w, h) * 0.45f;
        int count = displayedValues.size();
        float ringWidth = baseRadius / (count * 2f + 1f); // spacing

        // draw each ring from outer to inner
        for (int i = 0; i < count; i++) {
            float radius = baseRadius - i * (ringWidth * 2f);
            float stroke = ringWidth * 1.8f;

            RectF oval = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);

            // background track
            backgroundPaint.setStrokeWidth(stroke);
            canvas.drawArc(oval, 0, 360, false, backgroundPaint);

            // colored arc
            arcPaint.setStrokeWidth(stroke);
            int color = (i < colors.size()) ? colors.get(i) : 0xFF4A90E2;
            arcPaint.setColor(color);
            // Treat each value as a percentage (0..100). Cap at 100 so 100% == full circle.
            float raw = displayedValues.get(i);
            if (raw < 0f) raw = 0f;
            float pct = Math.min(raw, 100f) / 100f;
            float sweep = pct * 360f;
            // start at -90 to start at top
            canvas.drawArc(oval, -90f, sweep, false, arcPaint);
        }
    }
}
