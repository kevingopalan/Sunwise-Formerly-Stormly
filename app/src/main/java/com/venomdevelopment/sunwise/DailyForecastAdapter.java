package com.venomdevelopment.sunwise;

import android.content.Context;
import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;

import java.util.List;

public class DailyForecastAdapter extends RecyclerView.Adapter<DailyForecastAdapter.ViewHolder> {

    private List<SpannableString> mTemperatureData;
    private List<String> mTimeData;
    private List<String> mIconData; // We won't be actively using this for a separate icon
    private List<String> mPrecipitationData;
    private List<String> mHumidityData;
    private List<String> mLottieAnimData;
    private List<String> mDescriptionData;
    private LayoutInflater mInflater;
    private Context mContext;
    private int expandedPosition = RecyclerView.NO_POSITION;
    private OnItemExpandListener expandListener;

    public interface OnItemExpandListener {
        void onItemExpanded(int position);
        void onItemContracted(int position);
    }

    public void setOnItemExpandListener(OnItemExpandListener listener) {
        this.expandListener = listener;
    }

    public DailyForecastAdapter(Context context, List<SpannableString> temperatureData, List<String> timeData, List<String> iconData, List<String> precipitationData, List<String> humidityData, List<String> lottieAnimData, List<String> descriptionData) {
        this.mInflater = LayoutInflater.from(context);
        this.mTemperatureData = temperatureData;
        this.mTimeData = timeData;
        this.mIconData = iconData;
        this.mPrecipitationData = precipitationData;
        this.mHumidityData = humidityData;
        this.mLottieAnimData = lottieAnimData;
        this.mDescriptionData = descriptionData;
        this.mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.viewholder_daily, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        SpannableString temperature = mTemperatureData.get(position);
        String time = mTimeData.get(position);
        String icon = mIconData.get(position); // We still receive it, but might not use it
        String precipitation = mPrecipitationData.get(position);
        String humidity = mHumidityData.get(position);
        String lottieAnim = mLottieAnimData.get(position);
        String description = mDescriptionData.get(position);

        holder.myTextView.setText(temperature);
        holder.myTextViewTime.setText(time);
        holder.precipitationTextView.setText(precipitation);
        holder.humidityTextView.setText(humidity);
        holder.descriptionTextView.setText(description);

        final boolean isExpanded = holder.getAdapterPosition() == expandedPosition;
        holder.precLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.arrow.setImageResource(isExpanded ? R.drawable.baseline_keyboard_arrow_up_24 : R.drawable.baseline_keyboard_arrow_down_24);
        holder.itemView.setActivated(isExpanded);

        // We will only use the Lottie animation
        String animationName = extractAnimationNameFromIcon(lottieAnim != null && !lottieAnim.isEmpty() ? lottieAnim : icon);
        int animationResId = mContext.getResources().getIdentifier(animationName, "raw", mContext.getPackageName());
        if (animationResId == 0) {
            Log.w("DailyForecastAdapter", "Missing animation for: " + animationName + ", falling back to not_available");
            animationResId = mContext.getResources().getIdentifier("not_available", "raw", mContext.getPackageName());
        }
        
        // Additional safety check
        if (animationResId == 0) {
            Log.e("DailyForecastAdapter", "Even not_available animation not found, hiding animation view");
            holder.lottieAnimationView.setVisibility(View.GONE);
            return;
        }
        
        try {
            holder.lottieAnimationView.setVisibility(View.VISIBLE);
            holder.lottieAnimationView.setAnimation(animationResId);
            holder.lottieAnimationView.loop(true);
            holder.lottieAnimationView.playAnimation();
        } catch (Exception e) {
            Log.e("DailyForecastAdapter", "Error loading animation: " + animationName, e);
            holder.lottieAnimationView.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    expandedPosition = isExpanded ? RecyclerView.NO_POSITION : adapterPosition;
                    notifyItemChanged(adapterPosition);
                    if (isExpanded) {
                        if (expandListener != null) {
                            expandListener.onItemContracted(adapterPosition);
                        }
                    } else {
                        if (expandListener != null) {
                            expandListener.onItemExpanded(adapterPosition);
                        }
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mTemperatureData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView myTextView; // Maps to tempTxt for colored temperature
        TextView myTextViewTime; // Maps to hourTxt for the day
        TextView precipitationTextView;
        TextView humidityTextView;
        LottieAnimationView lottieAnimationView;
        TextView descriptionTextView;
        LinearLayout precLayout; // Add this reference
        ImageView arrow; // Add this reference

        ViewHolder(View itemView) {
            super(itemView);
            myTextView = itemView.findViewById(R.id.tempTxt);
            myTextViewTime = itemView.findViewById(R.id.hourTxt);
            precipitationTextView = itemView.findViewById(R.id.precipitationTxt);
            humidityTextView = itemView.findViewById(R.id.humidityTxt);
            lottieAnimationView = itemView.findViewById(R.id.animation_view);
            descriptionTextView = itemView.findViewById(R.id.statTxt);
            precLayout = itemView.findViewById(R.id.precipitationLayout); // Initialize
            arrow = itemView.findViewById(R.id.arrow); // Initialize
        }
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
            Log.e("DailyForecastAdapter", "Error extracting animation name from icon: " + iconUrl, e);
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
                
            // Thunderstorm conditions
            case "day/tsra": case "night/tsra":
                return "tstorm";
            case "day/tsra_sct": case "night/tsra_sct":
                return "tstorm";
            case "day/tsra_hi": case "night/tsra_hi":
                return "tstorm";
                
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
                Log.w("DailyForecastAdapter", "Unknown icon name: " + iconName + ", using clear_day as fallback");
                return "clear_day";
        }
    }
}