package com.venomdevelopment.sunwise;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyRecyclerViewAdapter.ViewHolder> {

    private final List<String> mData;
    private final List<String> hrData;
    private final List<String> icon;
    private final List<String> mPrec;

    private final List<String> mHumidity;
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    MyRecyclerViewAdapter(Context context, List<String> data, List<String> time, List<String> icon, List<String> prec, List<String> humidity) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        this.hrData = time;
        this.icon = icon;
        this.mPrec = prec;
        this.mHumidity = humidity;
    }

    // inflates the row layout from xml when needed
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.viewholder_hourly, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String hourlyForecast = mData.get(position);
        String hourlyTime = hrData.get(position);
        String micon = icon.get(position);
        String prec = mPrec.get(position);
        String humidity = mHumidity.get(position);
        holder.myHumidity.setText(humidity);
        holder.myTextView.setText(hourlyForecast);
        holder.myHrView.setText(hourlyTime);
        holder.myIcon.setImageResource(holder.itemView.getResources().getIdentifier(micon, "drawable", holder.itemView.getContext().getPackageName()));
        holder.myPrec.setText(prec);
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
        ImageView myIcon;
        TextView myHumidity;

        ViewHolder(View itemView) {
            super(itemView);
            myTextView = itemView.findViewById(R.id.tempTxt);
            myHrView = itemView.findViewById(R.id.hourTxt);
            myIcon = itemView.findViewById(R.id.pic);
            myPrec = itemView.findViewById(R.id.precipitationTxt);
            myHumidity = itemView.findViewById(R.id.humidityTxt);
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