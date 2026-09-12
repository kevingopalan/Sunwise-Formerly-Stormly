package com.venomdevelopment.sunwise;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

public class SavedLocationAdapter extends RecyclerView.Adapter<SavedLocationAdapter.ViewHolder> {

    private List<String> locations;
    private OnLocationClickListener listener;
    private Map<String, WeatherViewModel.WeatherSummary> weatherSummaries;

    public interface OnLocationClickListener {
        void onLocationClick(String location);
    }

    public SavedLocationAdapter(List<String> locations, OnLocationClickListener listener) {
        this.locations = locations;
        this.listener = listener;
        this.weatherSummaries = null;
    }

    public void setWeatherSummaries(Map<String, WeatherViewModel.WeatherSummary> weatherSummaries) {
        this.weatherSummaries = weatherSummaries;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saved_location, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String location = locations.get(position);
        holder.locationNameTextView.setText(location);

        String temperature = null;
        String weatherIcon = null;
        if (weatherSummaries != null && weatherSummaries.get(location) != null) {
            WeatherViewModel.WeatherSummary summary = weatherSummaries.get(location);
            temperature = summary.temperature;
            weatherIcon = summary.icon;
        }
        if (temperature != null) {
            holder.locationTemperatureTextView.setText(formatTemperature(holder.itemView.getContext(), temperature));
        } else {
            holder.locationTemperatureTextView.setText("--");
            Log.d("SavedLocationAdapter", "Temperature not found or queried for: " + location);
        }
        if (weatherIcon != null) {
            Log.d("SavedLocationAdapter", "Received icon URL for " + location + ": " + weatherIcon);
            String animationName = WeatherIconUtils.getAnimationResourceName(weatherIcon);
            Log.d("SavedLocationAdapter", "Extracted animation name for " + location + ": " + animationName);
            int animationResId = holder.itemView.getContext().getResources()
                    .getIdentifier(animationName, "raw", holder.itemView.getContext().getPackageName());
            Log.d("SavedLocationAdapter", "Resource ID for " + animationName + ": " + animationResId);
            if (animationResId == 0) {
                Log.w("SavedLocationAdapter", "Missing animation for: " + animationName + ", falling back to not_available");
                animationResId = holder.itemView.getContext().getResources().getIdentifier("not_available", "raw", holder.itemView.getContext().getPackageName());
            }
            
            // Additional safety check
            if (animationResId == 0) {
                Log.e("SavedLocationAdapter", "Even not_available animation not found, hiding animation view");
                holder.locationAnimationView.setVisibility(View.GONE);
                return;
            }
            
            try {
                holder.locationAnimationView.setVisibility(View.VISIBLE);
                holder.locationAnimationView.setAnimation(animationResId);
                holder.locationAnimationView.loop(true);
                holder.locationAnimationView.playAnimation();
            } catch (Exception e) {
                Log.e("SavedLocationAdapter", "Error loading animation: " + animationName, e);
                holder.locationAnimationView.setVisibility(View.GONE);
            }
        } else {
            Log.d("SavedLocationAdapter", "No icon URL for " + location);
            holder.locationAnimationView.setAnimation(R.raw.skeletonscreen); // Default
        }
        holder.itemView.setOnClickListener(v -> {
            listener.onLocationClick(location);
        });
    }

    private String formatTemperature(Context context, String tempStr) {
        SharedPreferences prefs = context.getSharedPreferences("SunwiseSettings", Context.MODE_PRIVATE);
        String unit = prefs.getString("unit", "us");
        double tempVal;
        try {
            tempVal = Double.parseDouble(tempStr.replaceAll("[^\\d.-]", ""));
        } catch (Exception e) {
            return tempStr;
        }
        double displayTemp = tempVal;
        String unitLabel = "°F";
        switch (unit) {
            case "si":
            case "ca":
            case "uk":
                displayTemp = (tempVal - 32) * 5.0 / 9.0;
                unitLabel = "°C";
                break;
            case "us":
            default:
                displayTemp = tempVal;
                unitLabel = "°F";
                break;
        }
        // Always round to whole numbers for a consumer-friendly display
        return Math.round(displayTemp) + unitLabel;
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView locationNameTextView;
        TextView locationTemperatureTextView;
        LottieAnimationView locationAnimationView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            locationNameTextView = itemView.findViewById(R.id.locationNameTextView);
            locationTemperatureTextView = itemView.findViewById(R.id.locationTemperatureTextView);
            locationAnimationView = itemView.findViewById(R.id.locationAnimationView);
        }
    }
}