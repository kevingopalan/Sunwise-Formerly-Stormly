package com.venomdevelopment.sunwise;


import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.venomdevelopment.sunwise.R;
import com.venomdevelopment.sunwise.NewsItem;

import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private List<NewsItem> newsItems;

    public NewsAdapter(List<NewsItem> newsItems) {
        this.newsItems = newsItems;
    }

    @Override
    public NewsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(NewsViewHolder holder, int position) {
        NewsItem newsItem = newsItems.get(position);
        holder.titleTextView.setText(newsItem.getTitle());
        holder.contentTextView.setText(newsItem.getContent());
        holder.cardView.setCardBackgroundColor(Color.parseColor("#00000000"));
        Glide.with(holder.imageView.getContext())
                .load(newsItem.getImageUrl())
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return newsItems.size();
    }

    public static class NewsViewHolder extends RecyclerView.ViewHolder {

        TextView titleTextView;
        TextView contentTextView;
        CardView cardView;
        ImageView imageView;

        public NewsViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            cardView = itemView.findViewById(R.id.newsCardView);
            contentTextView = itemView.findViewById(R.id.contentTextView);
            imageView = itemView.findViewById(R.id.newsImageView);
        }
    }
}
