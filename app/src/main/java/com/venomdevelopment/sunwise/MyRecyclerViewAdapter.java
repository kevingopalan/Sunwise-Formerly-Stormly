package com.venomdevelopment.sunwise;

import android.content.Context;
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
import java.util.ArrayList;

public class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyRecyclerViewAdapter.ViewHolder> {

    private final List<String> mData;
    private final List<String> hrData;
    private final List<String> icon;
    private final List<String> mPrec;
    private final List<String> mdesc;
    private final List<String> lottieAnim;

    private final List<String> mHumidity;
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private List<Boolean> isExpanded; // To store the expanded state of each item

    // data is passed into the constructor
    MyRecyclerViewAdapter(Context context, List<String> data, List<String> time, List<String> icon, List<String> prec, List<String> humidity, List<String> lottieAnimL, List<String> desc) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        this.hrData = time;
        this.icon = icon;
        this.mPrec = prec;
        this.mHumidity = humidity;
        this.lottieAnim = lottieAnimL;
        this.mdesc = desc;
        this.isExpanded = new ArrayList<>(data.size());
        for (int i = 0; i < data.size(); i++) {
            this.isExpanded.add(false); // Initialize all items as collapsed
        }
    }

    // inflates the row layout from xml when needed
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.viewholder_daily, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String hourlyForecast = mData.get(position);
        String hourlyTime = hrData.get(position);
        String prec = mPrec.get(position);
        String humidity = mHumidity.get(position);
        String lottieAnimString = lottieAnim.get(position);
        String descstring = mdesc.get(position);
        holder.myHumidity.setText(humidity);
        holder.myTextView.setText(hourlyForecast);
        holder.myHrView.setText(hourlyTime);
        holder.descTxt.setText(descstring);
        holder.animationView.setAnimation(holder.itemView.getResources().getIdentifier(lottieAnimString, "raw", holder.itemView.getContext().getPackageName()));
        holder.animationView.loop(true);
        holder.animationView.playAnimation();
        holder.myPrec.setText(prec);

        // Set the visibility based on the stored state for this item
        holder.precLayout.setVisibility(isExpanded.get(position) ? View.VISIBLE : View.GONE);
        holder.arrow.setImageResource(isExpanded.get(position) ? R.drawable.baseline_keyboard_arrow_up_24 : R.drawable.baseline_keyboard_arrow_down_24);

        holder.itemView.setOnClickListener(v -> {
            // Toggle the expanded state for this item
            isExpanded.set(position, !isExpanded.get(position));
            Log.d("Adapter", "Item at position " + position + " expanded: " + isExpanded.get(position));
            // Notify the adapter that this item has changed so it can be redrawn
            notifyItemChanged(position);
        });
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView myTextView;
        TextView myHrView;
        TextView myPrec;
        TextView myHumidity;
        LinearLayout precLayout;
        LottieAnimationView animationView;
        ImageView arrow;
        TextView descTxt;

        ViewHolder(View itemView) {
            super(itemView);
            myTextView = itemView.findViewById(R.id.tempTxt);
            myHrView = itemView.findViewById(R.id.hourTxt);
            myPrec = itemView.findViewById(R.id.precipitationTxt);
            precLayout = itemView.findViewById(R.id.precipitationLayout);
            myHumidity = itemView.findViewById(R.id.humidityTxt);
            animationView = itemView.findViewById(R.id.animation_view);
            arrow = itemView.findViewById(R.id.arrow);
            descTxt = itemView.findViewById(R.id.statTxt);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    // convenience method for getting data at click position
    String getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}