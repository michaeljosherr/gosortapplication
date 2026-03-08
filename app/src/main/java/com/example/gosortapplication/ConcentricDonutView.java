package com.example.gosortapplication;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ConcentricDonutView extends View {

    private final Paint slicePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint holePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<Integer> colors = new ArrayList<>();
    private final List<Float> targetValues = new ArrayList<>();
    private final List<Float> displayedValues = new ArrayList<>();
    private ValueAnimator animator;

    // Gap between slices in degrees
    private static final float GAP_DEGREES = 2.5f;
    // Hole size as fraction of radius (0.5 = 50% hole)
    private static final float HOLE_RADIUS_FRACTION = 0.55f;

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
        slicePaint.setStyle(Paint.Style.FILL);

        holePaint.setStyle(Paint.Style.FILL);
        holePaint.setColor(0xFFFFFFFF); // white hole — matches card background

        gapPaint.setStyle(Paint.Style.FILL);
        gapPaint.setColor(0xFFFFFFFF); // white gap between slices
    }

    public void setData(int[] vals, int[] cols) {
        colors.clear();
        targetValues.clear();
        if (vals == null || cols == null) return;

        int n = Math.min(vals.length, cols.length);
        float total = 0;
        for (int i = 0; i < n; i++) total += Math.max(vals[i], 0);

        for (int i = 0; i < n; i++) {
            // Store as percentage of total (0-100)
            float pct = total > 0 ? (Math.max(vals[i], 0) / total) * 100f : 0f;
            targetValues.add(pct);
            colors.add(cols[i]);
        }

        // Sync displayedValues size
        while (displayedValues.size() < targetValues.size()) displayedValues.add(0f);
        while (displayedValues.size() > targetValues.size()) displayedValues.remove(displayedValues.size() - 1);

        if (animator != null && animator.isRunning()) animator.cancel();

        final List<Float> start = new ArrayList<>(displayedValues);
        final List<Float> end = new ArrayList<>(targetValues);

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(700);
        animator.addUpdateListener(animation -> {
            float frac = (float) animation.getAnimatedValue();
            for (int i = 0; i < end.size(); i++) {
                float s = i < start.size() ? start.get(i) : 0f;
                float interpolated = s + (end.get(i) - s) * frac;
                if (i < displayedValues.size()) displayedValues.set(i, interpolated);
                else displayedValues.add(interpolated);
            }
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (displayedValues.isEmpty()) return;

        float total = 0;
        for (Float v : displayedValues) if (v != null && v > 0) total += v;
        if (total <= 0) return;

        int w = getWidth();
        int h = getHeight();
        int cx = w / 2;
        int cy = h / 2;

        float radius = Math.min(w, h) * 0.48f;
        float holeRadius = radius * HOLE_RADIUS_FRACTION;

        RectF oval = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);

        // Count non-zero slices for gap calculation
        int nonZeroCount = 0;
        for (Float v : displayedValues) if (v != null && v > 0) nonZeroCount++;

        float totalGaps = GAP_DEGREES * nonZeroCount;
        float availableDegrees = 360f - totalGaps;

        float startAngle = -90f; // start at top

        for (int i = 0; i < displayedValues.size(); i++) {
            float val = displayedValues.get(i);
            if (val <= 0) continue;

            float sweep = (val / total) * availableDegrees;
            int color = i < colors.size() ? colors.get(i) : 0xFF4A90E2;

            slicePaint.setColor(color);
            canvas.drawArc(oval, startAngle, sweep, true, slicePaint);

            startAngle += sweep + GAP_DEGREES;
        }

        // Draw white hole in center to make it a donut
        canvas.drawCircle(cx, cy, holeRadius, holePaint);
    }
}