package com.venomdevelopment.sunwise;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WeatherHourlyChart extends View {
    private final Path path = new Path();
    private final Path fillPath = new Path();
    private final Path precipPath = new Path();
    private final Path precipFillPath = new Path();

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint precipLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint timeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint precipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint precipAmtBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint precipAmtTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fadePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Drawable gradientDrawable;
    private Drawable precipGradientDrawable;
    private final float widthPerItem = 180f;
    private final float horizontalOffset = 80f;
    private List<WeatherPoint> points = new ArrayList<>();

    private boolean useUsUnits = true;

    public static class WeatherPoint {
        String time;
        int temp;
        int precipChance;
        double precipAmount;
        Drawable icon;

        public WeatherPoint(String time, int temp, int precipChance, double precipAmount, Drawable icon) {
            this.time = time;
            this.temp = temp;
            this.precipChance = precipChance;
            this.precipAmount = precipAmount;
            this.icon = icon;
        }
    }

    public WeatherHourlyChart(Context context, AttributeSet attrs) {
        super(context, attrs);

        int onSurfaceColor = ContextCompat.getColor(context, R.color.md_theme_onSurface);
        int chartBarColor = ContextCompat.getColor(context, R.color.chart_bar);
        int chartPrecipColor = ContextCompat.getColor(context, R.color.chart_prec);
        int precAmtColor = ContextCompat.getColor(context, R.color.chart_precamt);

        gradientDrawable = ContextCompat.getDrawable(context, R.drawable.linegraphgradient);
        precipGradientDrawable = ContextCompat.getDrawable(context, R.drawable.precgraphgradient);

        Typeface montreg = ResourcesCompat.getFont(context, R.font.montreg);

        linePaint.setColor(chartBarColor);
        linePaint.setStrokeWidth(6f);
        linePaint.setStyle(Paint.Style.STROKE);

        precipLinePaint.setColor(chartPrecipColor);
        precipLinePaint.setStrokeWidth(4f);
        precipLinePaint.setStyle(Paint.Style.STROKE);

        textPaint.setColor(onSurfaceColor);
        textPaint.setTextSize(48f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(montreg);

        timeTextPaint.setColor(onSurfaceColor);
        timeTextPaint.setTextSize(32f);
        timeTextPaint.setTextAlign(Paint.Align.CENTER);
        timeTextPaint.setTypeface(montreg);

        precipTextPaint.setColor(chartPrecipColor);
        precipTextPaint.setTextSize(28f);
        precipTextPaint.setTextAlign(Paint.Align.CENTER);
        precipTextPaint.setTypeface(montreg);

        precipAmtBgPaint.setColor(precAmtColor);
        precipAmtBgPaint.setStyle(Paint.Style.FILL);

        precipAmtTextPaint.setColor(Color.WHITE);
        precipAmtTextPaint.setTextSize(30f);
        precipAmtTextPaint.setTextAlign(Paint.Align.CENTER);
        precipAmtTextPaint.setTypeface(montreg);

        fadePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
    }

    public void setUseUsUnits(boolean useUsUnits) {
        this.useUsUnits = useUsUnits;
    }

    public void setPoints(List<WeatherPoint> points) {
        if (this.points != null) {
            for (WeatherPoint p : this.points) {
                if (p.icon != null) p.icon.setCallback(null);
            }
        }
        this.points = points;
        for (WeatherPoint p : this.points) {
            if (p.icon != null) p.icon.setCallback(this);
        }
        requestLayout();
        invalidate();
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        if (points != null) {
            for (WeatherPoint p : points) {
                if (p.icon == who) return true;
            }
        }
        return super.verifyDrawable(who);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = points != null ? (int) ((points.size() * widthPerItem) + (horizontalOffset * 2)) : MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec) + 40;
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (points == null || points.size() < 2) return;

        int maxTemp = points.get(0).temp;
        int minTemp = points.get(0).temp;
        for (WeatherPoint p : points) {
            if (p.temp > maxTemp) maxTemp = p.temp;
            if (p.temp < minTemp) minTemp = p.temp;
        }

        float tempRange = (maxTemp == minTemp) ? 1f : (maxTemp - minTemp);
        float actualWidth = getWidth();
        float topPadding = 240f;
        float bottomPadding = 20f;
        float precipGraphHeight = 160f;

        float precip0PercentY = getHeight() - bottomPadding;
        float precip100PercentY = precip0PercentY - precipGraphHeight;

        float tempGraphBottomPadding = bottomPadding + precipGraphHeight + 40f;
        float graphHeight = getHeight() - topPadding - tempGraphBottomPadding;

        path.reset();
        fillPath.reset();
        precipPath.reset();
        precipFillPath.reset();

        float firstXOffset = (widthPerItem / 2f) + horizontalOffset;
        float backExtrapolateRatio = firstXOffset / widthPerItem;

        float tempY0 = topPadding + (((maxTemp - points.get(0).temp) / tempRange) * graphHeight);
        float tempY1 = topPadding + (((maxTemp - points.get(1).temp) / tempRange) * graphHeight);
        float precipY0 = precip0PercentY - ((points.get(0).precipChance / 100f) * precipGraphHeight);
        float precipY1 = precip0PercentY - ((points.get(1).precipChance / 100f) * precipGraphHeight);

        float startTempY = tempY0 - (tempY1 - tempY0) * backExtrapolateRatio;
        float startPrecipY = precipY0 - (precipY1 - precipY0) * backExtrapolateRatio;

        int last = points.size() - 1;
        float lastX = (last * widthPerItem) + firstXOffset;
        float forwardExtrapolateRatio = (actualWidth - lastX) / widthPerItem;

        float tempY_last = topPadding + (((maxTemp - points.get(last).temp) / tempRange) * graphHeight);
        float tempY_prev = topPadding + (((maxTemp - points.get(last - 1).temp) / tempRange) * graphHeight);
        float precipY_last = precip0PercentY - ((points.get(last).precipChance / 100f) * precipGraphHeight);
        float precipY_prev = precip0PercentY - ((points.get(last - 1).precipChance / 100f) * precipGraphHeight);

        float endTempY = tempY_last + (tempY_last - tempY_prev) * forwardExtrapolateRatio;
        float endPrecipY = precipY_last + (precipY_last - precipY_prev) * forwardExtrapolateRatio;

        int n = points.size() + 2;
        float[] xs = new float[n];
        float[] tYs = new float[n];
        float[] pYs = new float[n];

        xs[0] = 0f;
        tYs[0] = startTempY;
        pYs[0] = startPrecipY;

        for (int i = 0; i < points.size(); i++) {
            WeatherPoint p = points.get(i);
            xs[i + 1] = (i * widthPerItem) + firstXOffset;
            tYs[i + 1] = topPadding + (((maxTemp - p.temp) / tempRange) * graphHeight);
            pYs[i + 1] = precip0PercentY - ((p.precipChance / 100f) * precipGraphHeight);
        }

        xs[n - 1] = actualWidth;
        tYs[n - 1] = endTempY;
        pYs[n - 1] = endPrecipY;

        path.moveTo(xs[0], tYs[0]);
        fillPath.moveTo(xs[0], tYs[0]);

        // Removed precipPath.moveTo() from here so it can be handled dynamically in the loop
        precipFillPath.moveTo(xs[0], pYs[0]);

        float tension = 0.2f;
        boolean isDrawingPrecip = false;

        for (int i = 0; i < n - 1; i++) {
            int prev = Math.max(0, i - 1);
            int curr = i;
            int next = i + 1;
            int nextNext = Math.min(n - 1, i + 2);

            float dx1 = (xs[next] - xs[prev]) * tension;
            float dy1_t = (tYs[next] - tYs[prev]) * tension;
            float dy1_p = (pYs[next] - pYs[prev]) * tension;

            float dx2 = (xs[nextNext] - xs[curr]) * tension;
            float dy2_t = (tYs[nextNext] - tYs[curr]) * tension;
            float dy2_p = (pYs[nextNext] - pYs[curr]) * tension;

            path.cubicTo(xs[curr] + dx1, tYs[curr] + dy1_t,
                    xs[next] - dx2, tYs[next] - dy2_t,
                    xs[next], tYs[next]);
            fillPath.cubicTo(xs[curr] + dx1, tYs[curr] + dy1_t,
                    xs[next] - dx2, tYs[next] - dy2_t,
                    xs[next], tYs[next]);

            // Clamp the precipitation Y values to ensure the curve never dips below the baseline
            float p_cp1y = Math.min(pYs[curr] + dy1_p, precip0PercentY);
            float p_cp2y = Math.min(pYs[next] - dy2_p, precip0PercentY);
            float p_endy = Math.min(pYs[next], precip0PercentY);

            // Precip Fill Path must remain continuous to bound the gradient properly
            precipFillPath.cubicTo(xs[curr] + dx1, p_cp1y,
                    xs[next] - dx2, p_cp2y,
                    xs[next], p_endy);

            // Only draw the precipitation stroke if the segment is actually above the 0% baseline
            boolean isZeroSegment = (pYs[curr] >= precip0PercentY - 0.5f) &&
                    (p_endy >= precip0PercentY - 0.5f) &&
                    (p_cp1y >= precip0PercentY - 0.5f) &&
                    (p_cp2y >= precip0PercentY - 0.5f);

            if (!isZeroSegment) {
                if (!isDrawingPrecip) {
                    precipPath.moveTo(xs[curr], pYs[curr]);
                    isDrawingPrecip = true;
                }
                precipPath.cubicTo(xs[curr] + dx1, p_cp1y,
                        xs[next] - dx2, p_cp2y,
                        xs[next], p_endy);
            } else {
                isDrawingPrecip = false;
            }
        }

        int layerId = canvas.saveLayer(0f, 0f, actualWidth, getHeight(), null);

        float canvasBottom = getHeight();
        fillPath.lineTo(actualWidth, canvasBottom);
        fillPath.lineTo(0f, canvasBottom);
        fillPath.close();

        if (gradientDrawable != null) {
            canvas.save();
            canvas.clipPath(fillPath);
            int gradientTop = (int) Math.min(0f, Math.min(startTempY, endTempY) - 50f);
            gradientDrawable.setBounds(0, gradientTop, (int)actualWidth, (int)canvasBottom);
            gradientDrawable.draw(canvas);
            canvas.restore();
        }

        precipFillPath.lineTo(actualWidth, precip0PercentY);
        precipFillPath.lineTo(0f, precip0PercentY);
        precipFillPath.close();

        if (precipGradientDrawable != null) {
            canvas.save();
            canvas.clipPath(precipFillPath);
            int pGradientTop = (int) Math.min(precip100PercentY, Math.min(startPrecipY, endPrecipY) - 50f);
            precipGradientDrawable.setBounds(0, pGradientTop, (int)actualWidth, (int)precip0PercentY);
            precipGradientDrawable.draw(canvas);
            canvas.restore();
        }

        canvas.drawPath(path, linePaint);
        canvas.drawPath(precipPath, precipLinePaint);

        float fadeDist = firstXOffset;
        float p1 = (fadeDist * 0.33f) / actualWidth;
        float p2 = (fadeDist * 0.66f) / actualWidth;
        float p3 = fadeDist / actualWidth;

        LinearGradient fadeMask = new LinearGradient(
                0f, 0f, actualWidth, 0f,
                new int[]{
                        0x00000000, 0x40000000, 0xA0000000, 0xFF000000,
                        0xFF000000, 0xA0000000, 0x40000000, 0x00000000
                },
                new float[]{
                        0f, p1, p2, p3,
                        1f - p3, 1f - p2, 1f - p1, 1f
                },
                Shader.TileMode.CLAMP
        );
        fadePaint.setShader(fadeMask);
        canvas.drawRect(0f, 0f, actualWidth, getHeight(), fadePaint);

        canvas.restoreToCount(layerId);

        for (int i = 0; i < points.size(); i++) {
            WeatherPoint p = points.get(i);
            float x = (i * widthPerItem) + firstXOffset;
            float tempY = topPadding + (((maxTemp - p.temp) / tempRange) * graphHeight);
            float precipY = precip0PercentY - ((p.precipChance / 100f) * precipGraphHeight);

            canvas.drawText(p.time, x, 60f, timeTextPaint);

            if (p.icon != null) {
                int halfSize = 42;
                p.icon.setBounds((int)(x - halfSize), (int)(tempY - 170), (int)(x + halfSize), (int)(tempY - 86));
                p.icon.draw(canvas);
            }

            canvas.drawText(p.temp + "°", x, tempY - 20f, textPaint);

            if (p.precipChance > 0) {
                canvas.drawText(p.precipChance + "%", x, precipY - 15f, precipTextPaint);
            }
        }

        float pillYCenter = getHeight();
        float pillHeight = 52f;
        float pillTop = pillYCenter - pillHeight / 2f;
        float pillBottom = pillYCenter + pillHeight / 2f;

        int j = 0;
        while (j < points.size()) {
            if (points.get(j).precipAmount > 0.005) {
                int startIdx = j;
                double sumAmount = 0;

                while (j < points.size() && points.get(j).precipAmount > 0.005) {
                    sumAmount += points.get(j).precipAmount;
                    j++;
                }
                int endIdx = j - 1;

                float startX = (startIdx * widthPerItem) + firstXOffset;
                float endX = (endIdx * widthPerItem) + firstXOffset;

                float rectLeft = startX - 70f;
                float rectRight = endX + 70f;

                canvas.drawRoundRect(rectLeft, pillTop, rectRight, pillBottom, 26f, 26f, precipAmtBgPaint);

                String unitStr = useUsUnits ? "in" : "mm";
                String amtStr = useUsUnits
                        ? String.format(Locale.US, "%.2f %s", sumAmount, unitStr)
                        : String.format(Locale.US, "%.1f %s", sumAmount, unitStr);

                float textX = (startX + endX) / 2f;
                float textY = pillYCenter - ((precipAmtTextPaint.descent() + precipAmtTextPaint.ascent()) / 2f);

                canvas.drawText(amtStr, textX, textY, precipAmtTextPaint);
            } else {
                j++;
            }
        }
    }
}