package com.venomdevelopment.sunwise;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;

import java.util.List;
import java.util.Map;

public class SavedLocationAdapter extends RecyclerView.Adapter<SavedLocationAdapter.ViewHolder> {

    private List<String> locations;
    private OnLocationClickListener listener;
    private Map<String, String> currentTemperatures; // Map of location name to current temperature
    private Map<String, String> currentWeatherIcons; // Map of location name to weather icon/animation

    public interface OnLocationClickListener {
        void onLocationClick(String location);
    }

    public SavedLocationAdapter(List<String> locations, OnLocationClickListener listener,
                                Map<String, String> currentTemperatures, Map<String, String> currentWeatherIcons) {
        this.locations = locations;
        this.listener = listener;
        this.currentTemperatures = currentTemperatures;
        this.currentWeatherIcons = currentWeatherIcons;
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

        String temperature = currentTemperatures.get(location);
        if (temperature != null) {
            holder.locationTemperatureTextView.setText(temperature);
        } else {
            holder.locationTemperatureTextView.setText("--");
        }

        String weatherIcon = currentWeatherIcons.get(location);
        if (weatherIcon != null) {
            int animationResId = holder.itemView.getContext().getResources()
                    .getIdentifier(weatherIcon, "raw", holder.itemView.getContext().getPackageName());
            if (animationResId != 0) {
                holder.locationAnimationView.setAnimation(animationResId);
            } else {
                holder.locationAnimationView.setImageResource(android.R.drawable.ic_dialog_info); // Fallback
            }
        } else {
            holder.locationAnimationView.setImageResource(android.R.drawable.ic_dialog_info); // Default
        }

        holder.itemView.setOnClickListener(v -> {
            listener.onLocationClick(location);
        });
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