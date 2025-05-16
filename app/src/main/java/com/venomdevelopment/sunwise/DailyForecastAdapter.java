package com.venomdevelopment.sunwise;

import android.content.Context;
import android.text.SpannableString;
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
        if (lottieAnim != null && !lottieAnim.isEmpty()) {
            holder.lottieAnimationView.setAnimation(mContext.getResources().getIdentifier(lottieAnim, "raw", mContext.getPackageName()));
            holder.lottieAnimationView.loop(true);
            holder.lottieAnimationView.playAnimation();
        } else {
            holder.lottieAnimationView.cancelAnimation();
            holder.lottieAnimationView.setVisibility(View.GONE); // Or handle no animation case
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
}