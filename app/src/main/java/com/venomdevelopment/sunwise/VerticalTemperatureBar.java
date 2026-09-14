package com.venomdevelopment.sunwise;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;
// Please ignore the fact that it says vertical, I changed it last-minute, it's horizontal
public class VerticalTemperatureBar extends View {
    private final Paint bgPaint;
    private final Paint rangePaint;
    private final Paint dotPaint;
    private final Paint dotOutlinePaint;
    private float minWeekTemp, maxWeekTemp, minDayTemp, maxDayTemp, currentTemp;
    private boolean showCurrentTemp = false;

    public VerticalTemperatureBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(ContextCompat.getColor(context, R.color.md_theme_background));
        bgPaint.setStyle(Paint.Style.FILL);

        rangePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rangePaint.setColor(ContextCompat.getColor(context, R.color.chart_prec));
        rangePaint.setStyle(Paint.Style.FILL);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.WHITE);
        dotPaint.setStyle(Paint.Style.FILL);

        dotOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotOutlinePaint.setColor(ContextCompat.getColor(context, R.color.md_theme_surface));
        dotOutlinePaint.setStyle(Paint.Style.FILL);
    }

    public void setTemperatureData(float minWeek, float maxWeek, float minDay, float maxDay, Float current) {
        this.minWeekTemp = minWeek;
        this.maxWeekTemp = maxWeek;
        this.minDayTemp = minDay;
        this.maxDayTemp = maxDay;

        if (current != null) {
            this.currentTemp = current;
            this.showCurrentTemp = true;
        } else {
            this.showCurrentTemp = false;
        }
        invalidate();
    }

    public void clearTemperatureData() {
        this.minWeekTemp = 0f;
        this.maxWeekTemp = 0f;
        this.minDayTemp = 0f;
        this.maxDayTemp = 0f;
        this.currentTemp = 0f;
        this.showCurrentTemp = false;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float radius = height / 2f;
        float centerY = height / 2f;

        if (Float.isNaN(minDayTemp) || Float.isNaN(maxDayTemp) || Float.isNaN(minWeekTemp) || Float.isNaN(maxWeekTemp)
                || maxWeekTemp <= minWeekTemp || width <= 0 || height <= 0) {
            canvas.drawRoundRect(0, 0, width, height, radius, radius, bgPaint);
            return;
        }

        canvas.drawRoundRect(0, 0, width, height, radius, radius, bgPaint);

        float totalRange = maxWeekTemp - minWeekTemp;
        float usableWidth = width - height;
        if (totalRange <= 0 || usableWidth <= 0) return;
 
        float minFraction = (minDayTemp - minWeekTemp) / totalRange;
        float maxFraction = (maxDayTemp - minWeekTemp) / totalRange;

        float leftCapCenterX = radius + (minFraction * usableWidth);
        float rightCapCenterX = radius + (maxFraction * usableWidth);

        float leftX = leftCapCenterX - radius;
        float rightX = rightCapCenterX + radius;

        // Draw the day's temperature range
        canvas.drawRoundRect(leftX, 0, rightX, height, radius, radius, rangePaint);

        // Draw current temperature dot
        if (showCurrentTemp) {
            float currentFraction = Math.max(0f, Math.min(1f, (currentTemp - minWeekTemp) / totalRange));
            float dotCenterX = radius + (currentFraction * usableWidth);
            float dotRadius = radius * 0.75f;
            canvas.drawCircle(dotCenterX, centerY, dotRadius * 2f, dotOutlinePaint);
            canvas.drawCircle(dotCenterX, centerY, dotRadius, dotPaint);
        }
    }
}