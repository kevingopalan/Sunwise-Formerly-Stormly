package com.venomdevelopment.sunwise;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AlertsRecyclerViewAdapter extends RecyclerView.Adapter<AlertsRecyclerViewAdapter.ViewHolder> {

    private final List<String> mData;
    private final List<String> alertTypes;
    private final List<String> alertDescriptions;  // List to hold the descriptions
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private final boolean[] expandedStates;  // Array to track expanded state for each item

    public AlertsRecyclerViewAdapter(Context context, List<String> data, List<String> alertTypes, List<String> alertDescriptions) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        this.alertTypes = alertTypes;
        this.alertDescriptions = alertDescriptions;
        this.expandedStates = new boolean[data.size()];  // Initialize the expanded states array
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == 1) {
            view = mInflater.inflate(R.layout.viewholder_alert_warning, parent, false);
        } else {
            view = mInflater.inflate(R.layout.viewholder_alert_watch, parent, false);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String alertContent = mData.get(position);
        String alertType = alertTypes.get(position);
        String description = alertDescriptions.get(position);

        if ("Warning".equalsIgnoreCase(alertType)) {
            holder.textAlertContent.setText("Warning: " + alertContent);
        } else if ("Watch".equalsIgnoreCase(alertType)) {
            holder.textAlertContent.setText("Watch: " + alertContent);
        } else if ("Advisory".equalsIgnoreCase(alertType)) {
            holder.textAlertContent.setText("Advisory: " + alertContent);
        } else {
            holder.textAlertContent.setText("Unknown: " + alertContent);
        }

        // Handle the visibility of the description and arrow direction based on the expanded state
        int arrowRes = expandedStates[position] ? R.drawable.baseline_keyboard_arrow_up_24 : R.drawable.baseline_keyboard_arrow_down_24;
        holder.textAlertContent.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_warning_24, 0, arrowRes, 0);
        holder.textAlertContent.setCompoundDrawablePadding(10);

        if (expandedStates[position]) {
            holder.textAlertDescription.setVisibility(View.VISIBLE);
            holder.textAlertDescription.setText(description);
        } else {
            holder.textAlertDescription.setVisibility(View.GONE);
        }

        // Set up click listener to toggle the expanded state
        holder.itemView.setOnClickListener(v -> {
            expandedStates[position] = !expandedStates[position];  // Toggle expanded state
            notifyItemChanged(position);  // Notify the adapter to update the specific item
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    @Override
    public int getItemViewType(int position) {
        String alertType = alertTypes.get(position);
        if ("Warning".equalsIgnoreCase(alertType)) {
            return 1;
        } else {
            return 2;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView textAlertContent;
        TextView textAlertDescription;  // TextView for the alert description

        ViewHolder(View itemView) {
            super(itemView);
            textAlertContent = itemView.findViewById(R.id.text_alertContent);
            textAlertDescription = itemView.findViewById(R.id.text_alertDescription);  // Initialize the description view
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    String getItem(int id) {
        return mData.get(id);
    }

    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}
