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
    private final Paint dateTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint precipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint precipAmtBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint precipAmtTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint snowAmtBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fadePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Drawable gradientDrawable;
    private Drawable precipGradientDrawable;

    // NEW: Drawables for the pills
    private Drawable rainPillIcon;
    private Drawable snowPillIcon;

    private final float widthPerItem = 180f;
    private final float horizontalOffset = 80f;
    private List<WeatherPoint> points = new ArrayList<>();

    private boolean useUsUnits = true;
    private boolean hasRain = false;
    private boolean hasSnow = false;

    public static class WeatherPoint {
        String time;
        String date;
        int temp;
        int precipChance;
        double precipAmount;
        double snowAmount;
        Drawable icon;

        public WeatherPoint(String time, String date, int temp, int precipChance, double precipAmount, double snowAmount, Drawable icon) {
            this.time = time;
            this.date = date;
            this.temp = temp;
            this.precipChance = precipChance;
            this.precipAmount = precipAmount;
            this.snowAmount = snowAmount;
            this.icon = icon;
        }

        public WeatherPoint(String time, int temp, int precipChance, double precipAmount, double snowAmount, Drawable icon) {
            this(time, null, temp, precipChance, precipAmount, snowAmount, icon);
        }
    }

    public WeatherHourlyChart(Context context, AttributeSet attrs) {
        super(context, attrs);

        int onSurfaceColor = ContextCompat.getColor(context, R.color.md_theme_onSurface);
        int chartBarColor = ContextCompat.getColor(context, R.color.chart_bar);
        int chartPrecipColor = ContextCompat.getColor(context, R.color.chart_prec);
        int precAmtColor = ContextCompat.getColor(context, R.color.chart_precamt);
        int snowAmtColor = ContextCompat.getColor(context, R.color.chart_snowamt);

        gradientDrawable = ContextCompat.getDrawable(context, R.drawable.linegraphgradient);
        precipGradientDrawable = ContextCompat.getDrawable(context, R.drawable.precgraphgradient);

        // NEW: Load and tint pill icons
        rainPillIcon = ContextCompat.getDrawable(context, R.drawable.humidityicon);
        if (rainPillIcon != null) {
            rainPillIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        }

        snowPillIcon = ContextCompat.getDrawable(context, R.drawable.baseline_ac_unit_24);
        if (snowPillIcon != null) {
            snowPillIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        }

        Typeface montreg = ResourcesCompat.getFont(context, R.font.montreg);
        Typeface montsemibold = ResourcesCompat.getFont(context, R.font.montsemibold);

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
        timeTextPaint.setTextSize(34f);
        timeTextPaint.setTextAlign(Paint.Align.CENTER);
        timeTextPaint.setTypeface(montreg);

        dateTextPaint.setColor(onSurfaceColor);
        dateTextPaint.setTextSize(32f);
        dateTextPaint.setTextAlign(Paint.Align.CENTER);
        dateTextPaint.setTypeface(montsemibold);

        precipTextPaint.setColor(chartPrecipColor);
        precipTextPaint.setTextSize(32f);
        precipTextPaint.setTextAlign(Paint.Align.CENTER);
        precipTextPaint.setTypeface(montsemibold);

        precipAmtBgPaint.setColor(precAmtColor);
        precipAmtBgPaint.setStyle(Paint.Style.FILL);

        snowAmtBgPaint.setColor(snowAmtColor);
        snowAmtBgPaint.setStyle(Paint.Style.FILL);

        precipAmtTextPaint.setColor(Color.WHITE);
        precipAmtTextPaint.setTextSize(30f);
        precipAmtTextPaint.setTextAlign(Paint.Align.CENTER);
        precipAmtTextPaint.setTypeface(montsemibold);

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
        this.hasRain = false;
        this.hasSnow = false;

        for (WeatherPoint p : this.points) {
            if (p.precipAmount > 0.005) this.hasRain = true;
            if (p.snowAmount > 0.005) this.hasSnow = true;
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

        int extraHeight = 0;
        if (hasRain) extraHeight += 50;
        if (hasSnow) extraHeight += 50;

        int height = MeasureSpec.getSize(heightMeasureSpec) + extraHeight;
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

        float topPadding = 270f;

        float bottomPadding = 20f;
        if (hasRain) bottomPadding += 60f;
        if (hasSnow) bottomPadding += 60f;

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

            float p_cp1y = Math.min(pYs[curr] + dy1_p, precip0PercentY);
            float p_cp2y = Math.min(pYs[next] - dy2_p, precip0PercentY);
            float p_endy = Math.min(pYs[next], precip0PercentY);

            precipFillPath.cubicTo(xs[curr] + dx1, p_cp1y,
                    xs[next] - dx2, p_cp2y,
                    xs[next], p_endy);

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

            if (p.date != null && !p.date.isEmpty()) {
                canvas.drawText(p.date, x, 46f, dateTextPaint);
            }
            canvas.drawText(p.time, x, 84f, timeTextPaint);

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

        float pillHeight = 52f;
        float currentPillY = getHeight() - 25f;

        if (hasSnow) {
            float snowPillYCenter = currentPillY;
            float snowPillTop = snowPillYCenter - pillHeight / 2f;
            float snowPillBottom = snowPillYCenter + pillHeight / 2f;
            currentPillY -= 60f;

            int k = 0;
            while (k < points.size()) {
                if (points.get(k).snowAmount > 0.005) {
                    int startIdx = k;
                    double sumAmount = 0;

                    while (k < points.size() && points.get(k).snowAmount > 0.005) {
                        sumAmount += points.get(k).snowAmount;
                        k++;
                    }
                    int endIdx = k - 1;

                    float startX = (startIdx * widthPerItem) + firstXOffset;
                    float endX = (endIdx * widthPerItem) + firstXOffset;

                    float rectLeft = startX - 70f;
                    float rectRight = endX + 70f;

                    canvas.drawRoundRect(rectLeft, snowPillTop, rectRight, snowPillBottom, 26f, 26f, snowAmtBgPaint);

                    // NEW: Draw Snow Icon
                    if (snowPillIcon != null) {
                        int iconSize = 36;
                        int iconLeft = (int) (rectLeft + 20f);
                        int iconTop = (int) (snowPillYCenter - iconSize / 2f);
                        snowPillIcon.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize);
                        snowPillIcon.draw(canvas);
                    }

                    String unitStr = useUsUnits ? "in" : "cm";
                    double displayAmt = useUsUnits ? sumAmount : (sumAmount / 10.0);
                    String amtStr = useUsUnits
                            ? String.format(Locale.US, "%.2f %s", displayAmt, unitStr)
                            : String.format(Locale.US, "%.1f %s", displayAmt, unitStr);

                    float textX = (startX + endX) / 2f;
                    float textY = snowPillYCenter - ((precipAmtTextPaint.descent() + precipAmtTextPaint.ascent()) / 2f);

                    canvas.drawText(amtStr, textX, textY, precipAmtTextPaint);
                } else {
                    k++;
                }
            }
        }

        // 2. Draw Rain Pills
        if (hasRain) {
            float rainPillYCenter = currentPillY;
            float rainPillTop = rainPillYCenter - pillHeight / 2f;
            float rainPillBottom = rainPillYCenter + pillHeight / 2f;

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

                    canvas.drawRoundRect(rectLeft, rainPillTop, rectRight, rainPillBottom, 26f, 26f, precipAmtBgPaint);

                    // NEW: Draw Rain Icon
                    if (rainPillIcon != null) {
                        int iconSize = 36;
                        int iconLeft = (int) (rectLeft + 20f);
                        int iconTop = (int) (rainPillYCenter - iconSize / 2f);
                        rainPillIcon.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize);
                        rainPillIcon.draw(canvas);
                    }

                    String unitStr = useUsUnits ? "in" : "mm";
                    String amtStr = useUsUnits
                            ? String.format(Locale.US, "%.2f %s", sumAmount, unitStr)
                            : String.format(Locale.US, "%.1f %s", sumAmount, unitStr);

                    float textX = (startX + endX) / 2f;
                    float textY = rainPillYCenter - ((precipAmtTextPaint.descent() + precipAmtTextPaint.ascent()) / 2f);

                    canvas.drawText(amtStr, textX, textY, precipAmtTextPaint);
                } else {
                    j++;
                }
            }
        }
    }
}