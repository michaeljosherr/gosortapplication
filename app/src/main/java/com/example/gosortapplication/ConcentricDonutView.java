package com.example.gosortapplication;

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

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Integer> colors = new ArrayList<>();
    private final List<Float> values = new ArrayList<>();

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
        values.clear();
        colors.clear();
        if (vals == null || cols == null) return;
        int n = Math.min(vals.length, cols.length);
        for (int i = 0; i < n; i++) {
            values.add((float) vals[i]);
            colors.add(cols[i]);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (values.isEmpty()) return;

        float sum = 0f;
        for (Float v : values) sum += v;
        if (sum <= 0f) return;

        int w = getWidth();
        int h = getHeight();
        int cx = w / 2;
        int cy = h / 2;

        // base radius is half of min dimension * 0.9
        float baseRadius = Math.min(w, h) * 0.45f;
        float ringWidth = baseRadius / (values.size() * 2f + 1f); // spacing

        // draw each ring from outer to inner
        for (int i = 0; i < values.size(); i++) {
            float radius = baseRadius - i * (ringWidth * 2f);
            float stroke = ringWidth * 1.8f;

            RectF oval = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);

            // background track
            backgroundPaint.setStrokeWidth(stroke);
            canvas.drawArc(oval, 0, 360, false, backgroundPaint);

            // colored arc
            arcPaint.setStrokeWidth(stroke);
            arcPaint.setColor(colors.get(i));
            float sweep = (values.get(i) / sum) * 360f;
            // start at -90 to start at top
            canvas.drawArc(oval, -90f, sweep, false, arcPaint);
        }
    }
}
