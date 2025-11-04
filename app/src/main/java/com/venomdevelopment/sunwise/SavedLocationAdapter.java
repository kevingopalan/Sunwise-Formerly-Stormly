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
            String animationName = extractAnimationNameFromIcon(weatherIcon);
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
            holder.locationAnimationView.setImageResource(android.R.drawable.ic_dialog_info); // Default
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

    private String extractAnimationNameFromIcon(String iconUrl) {
        if (iconUrl == null || iconUrl.isEmpty()) {
            return "clear_day";
        }
        try {
            String iconName = iconUrl;
            if (iconUrl.startsWith("http")) {
                int idx = iconUrl.indexOf("/icons/land/");
                if (idx != -1) {
                    iconName = iconUrl.substring(idx + 12); // after /icons/land/
                    int qIdx = iconName.indexOf('?');
                    if (qIdx != -1) iconName = iconName.substring(0, qIdx);
                    int cIdx = iconName.indexOf(',');
                    if (cIdx != -1) iconName = iconName.substring(0, cIdx);
                }
            }
            // Handle complex weather conditions (e.g., "day/sct/tsra_hi")
            String[] weatherParts = iconName.split("/");
            if (weatherParts.length > 2) {
                String primaryWeather = weatherParts[weatherParts.length - 1];
                String dayNight = weatherParts[0];
                iconName = dayNight + "/" + primaryWeather;
            }
            return convertIconToAnimationName(iconName);
        } catch (Exception e) {
            Log.e("SavedLocationAdapter", "Error extracting animation name from icon: " + iconUrl, e);
        }
        return "clear_day";
    }

    private String convertIconToAnimationName(String iconName) {
        // Thunderstorm and tornado mappings
        switch (iconName) {
            case "day/sct/tsra_hi":
            case "day/sct/tsra":
            case "night/sct/tsra_hi":
            case "night/sct/tsra":
            case "day/tsra": case "night/tsra":
            case "day/tsra_sct": case "night/tsra_sct":
            case "day/tsra_hi": case "night/tsra_hi":
            case "day/tornado": case "night/tornado":
                return "lightning_bolt";
        }
        
        // Original mappings
        switch (iconName) {
            // Clear/Sunny conditions
            case "day/skc": case "night/skc":
                return "clear_day";
            case "day/few": case "night/few":
                return "clear_day"; // A few clouds, use clear
            case "day/sct": case "night/sct":
                return "partly_cloudy_day";
            case "day/bkn": case "night/bkn":
                return "cloudy";
            case "day/ovc": case "night/ovc":
                return "overcast";
                
            // Windy conditions
            case "day/wind_skc": case "night/wind_skc":
                return "clear_day";
            case "day/wind_few": case "night/wind_few":
                return "clear_day";
            case "day/wind_sct": case "night/wind_sct":
                return "partly_cloudy_day";
            case "day/wind_bkn": case "night/wind_bkn":
                return "cloudy";
            case "day/wind_ovc": case "night/wind_ovc":
                return "overcast";
                
            // Precipitation conditions
            case "day/snow": case "night/snow":
                return "snow";
            case "day/rain_snow": case "night/rain_snow":
                return "snow"; // Mixed, prefer snow animation
            case "day/rain_sleet": case "night/rain_sleet":
                return "sleet";
            case "day/snow_sleet": case "night/snow_sleet":
                return "sleet";
            case "day/fzra": case "night/fzra":
                return "sleet";
            case "day/rain_fzra": case "night/rain_fzra":
                return "sleet";
            case "day/snow_fzra": case "night/snow_fzra":
                return "sleet";
            case "day/sleet": case "night/sleet":
                return "sleet";
            case "day/rain": case "night/rain":
                return "rain";
            case "day/rain_showers": case "night/rain_showers":
                return "rain";
            case "day/rain_showers_hi": case "night/rain_showers_hi":
                return "rain";
                
            // Severe weather
            case "day/hurricane": case "night/hurricane":
                return "hurricane";
            case "day/tropical_storm": case "night/tropical_storm":
                return "hurricane"; // Use hurricane animation
            case "day/blizzard": case "night/blizzard":
                return "snow"; // Use snow animation
                
            // Atmospheric conditions
            case "day/dust": case "night/dust":
                return "dust";
            case "day/smoke": case "night/smoke":
                return "overcast"; // No smoke animation, use overcast
            case "day/haze": case "night/haze":
                return "haze";
            case "day/fog": case "night/fog":
                return "fog";
                
            // Temperature extremes
            case "day/hot": case "night/hot":
                return "clear_day";
            case "day/cold": case "night/cold":
                return "clear_day";
                
            // Legacy/fallback cases
            case "day/clear": case "night/clear":
                return "clear_day";
            case "day/partly_cloudy": case "night/partly_cloudy":
                return "partly_cloudy_day";
            case "day/mostly_cloudy": case "night/mostly_cloudy":
                return "cloudy";
            case "day/drizzle": case "night/drizzle":
                return "rain";
            case "day/wind": case "night/wind":
                return "wind";
                
            default:
                Log.w("SavedLocationAdapter", "Unknown icon name: " + iconName + ", using clear_day as fallback");
                return "clear_day";
        }
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