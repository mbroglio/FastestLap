package com.the_coffe_coders.fastestlap.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.news.News;
import com.the_coffe_coders.fastestlap.util.UIUtils;
import java.util.List;

public class NewsRecyclerAdapter extends RecyclerView.Adapter<NewsRecyclerAdapter.NewsViewHolder> {
    private final List<News> newsList;
    private final Context context;
    private final SparseBooleanArray expandedPositions = new SparseBooleanArray();

    public NewsRecyclerAdapter(List<News> newsList, Context context) {
        this.newsList = newsList;
        this.context = context;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.news_item, parent, false);

        return new NewsViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        News news = newsList.get(position);

        UIUtils.loadImageWithGlide(
                context, 
                news.getImageUrl(), 
                holder.newsImageView,
                ()-> UIUtils.multipleSetTextViewText(
                        new String[]{
                                news.getTitle(),
                                UIUtils.getTimeAgo(news.getDate(), context)},
                        new TextView[]{
                                holder.titleTextView,
                                holder.dateTextView
                        }));

        UIUtils.setTextViewTextWithCondition(news.getDescription() != null,
                UIUtils.formatXmlText(news.getDescription()),
                "",
                holder.descriptionTextView);

        boolean expanded = expandedPositions.get(position, false);
        holder.descriptionTextView.setVisibility(expanded ? View.VISIBLE : View.GONE);
        holder.linkLayout.setVisibility(expanded ? View.VISIBLE : View.GONE);
        holder.newsImageView.setVisibility(expanded ? View.GONE : View.VISIBLE);

        holder.newsLayout.setOnClickListener(v -> {
            boolean isExpanded = expandedPositions.get(position, false);
            if (isExpanded) {
                expandedPositions.put(position, false);
                holder.descriptionTextView.setVisibility(View.GONE);
                holder.linkLayout.setVisibility(View.GONE);
                holder.newsImageView.setVisibility(View.VISIBLE);
            } else {
                expandedPositions.put(position, true);
                holder.descriptionTextView.setVisibility(View.VISIBLE);
                holder.linkLayout.setVisibility(View.VISIBLE);
                holder.newsImageView.setVisibility(View.GONE);
            }
        });

        holder.linkLayout.setOnClickListener(v -> {
            String link = news.getLink();
            if (link != null && !link.isEmpty()) {
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(browserIntent);
                } catch (Exception e) {
                    Toast.makeText(context,
                            R.string.link_error, Toast.LENGTH_SHORT).show();
                    Log.e("News Adapter", "Failed to open link: " + link, e);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return newsList.size();
    }

    public static class NewsViewHolder extends RecyclerView.ViewHolder {

        final RelativeLayout newsLayout, linkLayout;
        final TextView titleTextView, dateTextView, descriptionTextView;
        final ImageView newsImageView;
        
        public NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            newsLayout = itemView.findViewById(R.id.news_layout);
            titleTextView = itemView.findViewById(R.id.title_layout);
            newsImageView = itemView.findViewById(R.id.news_image);
            dateTextView = itemView.findViewById(R.id.date_text_layout);
            descriptionTextView = itemView.findViewById(R.id.description_text_layout);
            linkLayout = itemView.findViewById(R.id.link_text_layout);
        }
    }
}
