package com.venomdevelopment.sunwise;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.airbnb.lottie.LottieAnimationView;
import java.util.Collections;
import java.util.List;

public class VerticalBarForecastAdapter extends RecyclerView.Adapter<VerticalBarForecastAdapter.ViewHolder> {

    private final List<Float> mHighTempData;
    private final List<Float> mLowTempData;
    private final List<String> mTimeData;
    private final List<String> mIconData;
    private final List<String> mPrecipitationData;
    private final List<String> mHumidityData;
    private final List<String> mDescriptionData;
    private final List<String> mNightDescriptionData;
    private final List<String> mNightPrecipitationData;
    private final List<String> mNightHumidityData;
    private final List<Boolean> mIsDaytimeData;
    private final LayoutInflater mInflater;
    private final Context mContext;
    private Float mCurrentTemp;
    public void setCurrentTemp(Float currentTemp) {
        this.mCurrentTemp = currentTemp;
        notifyItemChanged(0);
    }

    private float globalMinTemp = Float.MAX_VALUE;
    private float globalMaxTemp = Float.MIN_VALUE;

    private int expandedPosition = RecyclerView.NO_POSITION;
    private OnItemExpandListener expandListener;

    public interface OnItemExpandListener {
        void onItemExpanded(int position);
        void onItemContracted(int position);
    }

    public void setOnItemExpandListener(OnItemExpandListener listener) {
        this.expandListener = listener;
    }

    public VerticalBarForecastAdapter(Context context, List<Float> highTempData, List<Float> lowTempData,
                                      List<String> timeData, List<String> iconData, List<String> precipitationData,
                                      List<String> humidityData, List<String> descriptionData,
                                      List<String> nightDescriptionData, List<String> nightPrecipitationData,
                                      List<String> nightHumidityData, List<Boolean> isDaytimeData,
                                      Float currentTemp) {
        this.mInflater = LayoutInflater.from(context);
        this.mContext = context;
        this.mHighTempData = highTempData;
        this.mLowTempData = lowTempData;
        this.mTimeData = timeData;
        this.mIconData = iconData;
        this.mPrecipitationData = precipitationData;
        this.mHumidityData = humidityData;
        this.mDescriptionData = descriptionData;
        this.mNightDescriptionData = nightDescriptionData;
        this.mNightPrecipitationData = nightPrecipitationData;
        this.mNightHumidityData = nightHumidityData;
        this.mIsDaytimeData = isDaytimeData;
        this.mCurrentTemp = currentTemp;
        calculateGlobalExtremes();
    }

    private void calculateGlobalExtremes() {
        if (mHighTempData != null && mLowTempData != null) {
            for (int i = 0; i < mHighTempData.size(); i++) {
                Float high = mHighTempData.get(i);
                Float low = mLowTempData.get(i);
                if (high != null && !high.isNaN() && high > globalMaxTemp) globalMaxTemp = high;
                if (low != null && !low.isNaN() && low < globalMinTemp) globalMinTemp = low;
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.viewholder_dailybar, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        Float highTemp = mHighTempData.get(position);
        Float lowTemp = mLowTempData.get(position);
        String time = mTimeData.get(position);
        String icon = mIconData.get(position);
        String precipitation = mPrecipitationData.get(position);
        String humidity = mHumidityData.get(position);
        String description = mDescriptionData.get(position);
        String nightDescription = mNightDescriptionData != null && position < mNightDescriptionData.size() ? mNightDescriptionData.get(position) : "---";
        String nightPrecipitation = mNightPrecipitationData != null && position < mNightPrecipitationData.size() ? mNightPrecipitationData.get(position) : "--%";
        String nightHumidity = mNightHumidityData != null && position < mNightHumidityData.size() ? mNightHumidityData.get(position) : "N/A";
        boolean isDaytime = mIsDaytimeData != null && position < mIsDaytimeData.size() && mIsDaytimeData.get(position);

        // Safely format temperatures
        holder.hiTempTxt.setText(highTemp != null && !highTemp.isNaN() ? Math.round(highTemp) + "º" : "--º");
        holder.loTempTxt.setText(lowTemp != null && !lowTemp.isNaN() ? Math.round(lowTemp) + "º" : "--º");
        holder.hourTxt.setText(time);
        holder.precipitationTxt.setText(precipitation != null && !precipitation.trim().isEmpty() ? precipitation : "--%");
        holder.humidityTxt.setText(humidity != null && !humidity.trim().isEmpty() ? humidity : "N/A");
        holder.statTxt.setText(description != null && !description.trim().isEmpty() ? description : "---");
        holder.nightStatTxt.setText(nightDescription != null && !nightDescription.trim().isEmpty() ? nightDescription : "---");
        holder.nightPrecipitationTxt.setText(nightPrecipitation != null && !nightPrecipitation.trim().isEmpty() ? nightPrecipitation : "--%");
        holder.nightHumidityTxt.setText(nightHumidity != null && !nightHumidity.trim().isEmpty() ? nightHumidity : "N/A");

        // Bind the Custom Vertical Bar
        if (holder.tempBar != null) {
            if (highTemp != null && lowTemp != null && !highTemp.isNaN() && !lowTemp.isNaN()) {
                Float dotTemp = (position == 0) ? mCurrentTemp : null;
                holder.tempBar.setTemperatureData(globalMinTemp, globalMaxTemp, lowTemp, highTemp, dotTemp);
            } else {
                holder.tempBar.clearTemperatureData();
            }
        }
        // Handle Expansion logic
        final boolean isExpanded = holder.getAdapterPosition() == expandedPosition;
        holder.precipitationLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.itemView.setActivated(isExpanded);

        // Handle Animation
        String animationName = WeatherIconUtils.getAnimationResourceName(icon, isDaytime);
        int animationResId = mContext.getResources().getIdentifier(animationName, "raw", mContext.getPackageName());

        if (animationResId == 0) {
            animationResId = mContext.getResources().getIdentifier("not_available", "raw", mContext.getPackageName());
        }

        if (animationResId == 0) {
            holder.animationView.setVisibility(View.GONE);
        } else {
            try {
                holder.animationView.setVisibility(View.VISIBLE);
                holder.animationView.setAnimation(animationResId);
                holder.animationView.loop(true);
                holder.animationView.playAnimation();
            } catch (Exception e) {
                Log.e("VerticalBarAdapter", "Error loading animation: " + animationName, e);
                holder.animationView.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return;

            final boolean currentlyExpanded = adapterPosition == expandedPosition;
            int previouslyExpanded = expandedPosition;

            if (currentlyExpanded) {
                expandedPosition = RecyclerView.NO_POSITION;
                notifyItemChanged(adapterPosition);
                if (expandListener != null) {
                    expandListener.onItemContracted(adapterPosition);
                }
                return;
            }

            expandedPosition = adapterPosition;
            if (previouslyExpanded != RecyclerView.NO_POSITION && previouslyExpanded != adapterPosition) {
                notifyItemChanged(previouslyExpanded);
            }
            notifyItemChanged(adapterPosition);

            if (expandListener != null) {
                expandListener.onItemExpanded(adapterPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mTimeData != null ? mTimeData.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView hourTxt;
        LottieAnimationView animationView;
        TextView hiTempTxt;
        TextView loTempTxt;
        VerticalTemperatureBar tempBar;
        TextView precipitationTxt;
        TextView humidityTxt;
        TextView statTxt;
        TextView nightStatTxt;
        TextView nightPrecipitationTxt;
        TextView nightHumidityTxt;
        LinearLayout precipitationLayout;

        ViewHolder(View itemView) {
            super(itemView);
            hourTxt = itemView.findViewById(R.id.hourTxt);
            animationView = itemView.findViewById(R.id.animation_view);
            hiTempTxt = itemView.findViewById(R.id.hiTempTxt);
            loTempTxt = itemView.findViewById(R.id.loTempTxt);
            tempBar = itemView.findViewById(R.id.tempBar); // Make sure your layout matches this ID
            precipitationTxt = itemView.findViewById(R.id.precipitationTxt);
            humidityTxt = itemView.findViewById(R.id.humidityTxt);
            statTxt = itemView.findViewById(R.id.statTxt);
            nightStatTxt = itemView.findViewById(R.id.nightStatTxt);
            nightPrecipitationTxt = itemView.findViewById(R.id.nightPrecipitationTxt);
            nightHumidityTxt = itemView.findViewById(R.id.nightHumidityTxt);
            precipitationLayout = itemView.findViewById(R.id.precipitationLayout);
        }
    }
}