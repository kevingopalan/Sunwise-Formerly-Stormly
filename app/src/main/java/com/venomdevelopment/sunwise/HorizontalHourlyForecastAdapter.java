package com.venomdevelopment.sunwise;

import android.content.Context;
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

public class HorizontalHourlyForecastAdapter extends RecyclerView.Adapter<HorizontalHourlyForecastAdapter.ViewHolder> {

    private List<String> mTemperatureData;
    private List<String> mTimeData;
    private List<String> mIconData;
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

    public HorizontalHourlyForecastAdapter(Context context, List<String> temperatureData, List<String> timeData, List<String> iconData, List<String> precipitationData, List<String> humidityData, List<String> lottieAnimData, List<String> descriptionData) {
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
        View view = mInflater.inflate(R.layout.viewholder_hourly, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        String temperature = mTemperatureData.get(position);
        String time = mTimeData.get(position);
        String icon = mIconData.get(position);
        String precipitation = mPrecipitationData.get(position);
        String humidity = mHumidityData.get(position);
        String lottieAnim = mLottieAnimData.get(position);
        String description = mDescriptionData.get(position);

        holder.tempTxt.setText(temperature);
        holder.hourTxt.setText(time);
        holder.precipitationTxt.setText(precipitation);
        holder.humidityTxt.setText(humidity);
        holder.statTxt.setText(description);

        final boolean isExpanded = holder.getAdapterPosition() == expandedPosition;
        holder.precipitationLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.arrow.setImageResource(isExpanded ? R.drawable.baseline_keyboard_arrow_up_24 : R.drawable.baseline_keyboard_arrow_down_24);
        holder.itemView.setActivated(isExpanded);

        if (lottieAnim != null && !lottieAnim.isEmpty()) {
            holder.animationView.setAnimation(mContext.getResources().getIdentifier(lottieAnim, "raw", mContext.getPackageName()));
            holder.animationView.loop(true);
            holder.animationView.playAnimation();
        } else {
            holder.animationView.cancelAnimation();
            holder.animationView.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    final boolean isExpanded = adapterPosition == expandedPosition;
                    expandedPosition = isExpanded ? RecyclerView.NO_POSITION : adapterPosition;
                    notifyItemChanged(adapterPosition);
                    if (expandListener != null) {
                        if (isExpanded) {
                            expandListener.onItemContracted(adapterPosition);
                        } else {
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
        TextView tempTxt;
        TextView hourTxt;
        TextView precipitationTxt;
        TextView humidityTxt;
        LottieAnimationView animationView;
        TextView statTxt;
        LinearLayout precipitationLayout;
        ImageView arrow;

        ViewHolder(View itemView) {
            super(itemView);
            tempTxt = itemView.findViewById(R.id.tempTxt);
            hourTxt = itemView.findViewById(R.id.hourTxt);
            precipitationTxt = itemView.findViewById(R.id.precipitationTxt);
            humidityTxt = itemView.findViewById(R.id.humidityTxt);
            animationView = itemView.findViewById(R.id.animation_view);
            statTxt = itemView.findViewById(R.id.statTxt);
            precipitationLayout = itemView.findViewById(R.id.precipitationLayout);
            arrow = itemView.findViewById(R.id.arrow);
        }
    }
}