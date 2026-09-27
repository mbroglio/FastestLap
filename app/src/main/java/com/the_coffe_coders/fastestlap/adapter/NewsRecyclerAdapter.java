package com.the_coffe_coders.fastestlap.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.news.News;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class NewsRecyclerAdapter extends RecyclerView.Adapter<NewsRecyclerAdapter.NewsViewHolder> {
    private final List<News> newsList;
    private final Context context;
    private final Set<String> expandedNewsIds;
    private final Runnable onImageLoaded;
    private final int itemsToWaitFor;

    public NewsRecyclerAdapter(List<News> newsList, Context context, Runnable onImageLoaded, int itemsToWaitFor, Set<String> expandedNewsIds) {
        this.newsList = new ArrayList<>(newsList != null ? newsList : Collections.emptyList());
        this.context = context;
        this.onImageLoaded = onImageLoaded;
        this.itemsToWaitFor = itemsToWaitFor;
        this.expandedNewsIds = expandedNewsIds != null ? expandedNewsIds : new HashSet<>();
    }

    public NewsRecyclerAdapter(List<News> newsList, Context context, Runnable onImageLoaded, int itemsToWaitFor) {
        this(newsList, context, onImageLoaded, itemsToWaitFor, new HashSet<>());
    }

    public void updateNewsList(List<News> newNewsList) {
        if (newNewsList == null) return;

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return newsList.size();
            }

            @Override
            public int getNewListSize() {
                return newNewsList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                News oldItem = newsList.get(oldItemPosition);
                News newItem = newNewsList.get(newItemPosition);
                return getNewsId(oldItem).equals(getNewsId(newItem));
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                News oldItem = newsList.get(oldItemPosition);
                News newItem = newNewsList.get(newItemPosition);
                return Objects.equals(oldItem.getTitle(), newItem.getTitle())
                        && Objects.equals(oldItem.getDate(), newItem.getDate())
                        && Objects.equals(oldItem.getDescription(), newItem.getDescription())
                        && Objects.equals(oldItem.getImageUrl(), newItem.getImageUrl())
                        && Objects.equals(oldItem.getLink(), newItem.getLink());
            }
        });

        newsList.clear();
        newsList.addAll(newNewsList);
        diffResult.dispatchUpdatesTo(this);
    }

    public boolean isExpanded(News news) {
        if (news == null) return false;
        String id = getNewsId(news);
        return expandedNewsIds.contains(id);
    }

    public void clearExpandedStates() {
        expandedNewsIds.clear();
    }

    public static String getNewsId(News news) {
        if (news == null) return "";
        if (news.getLink() != null && !news.getLink().trim().isEmpty()) {
            return news.getLink().trim();
        }
        if (news.getTitle() != null && !news.getTitle().trim().isEmpty()) {
            return news.getTitle().trim();
        }
        return String.valueOf(news.hashCode());
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

        UIUtils.multipleSetTextViewText(
                new String[]{
                        news.getTitle(),
                        UIUtils.getTimeAgo(news.getDate(), context)},
                new TextView[]{
                        holder.titleTextView,
                        holder.dateTextView
                });

        Runnable imageCallback = null;
        if (position < itemsToWaitFor && onImageLoaded != null) {
            imageCallback = onImageLoaded;
        }

        UIUtils.loadImageWithGlide(
                context,
                news.getImageUrl(),
                holder.newsImageView,
                imageCallback);

        UIUtils.setTextViewTextWithCondition(news.getDescription() != null,
                UIUtils.formatXmlText(news.getDescription()),
                "",
                holder.descriptionTextView);

        boolean expanded = isExpanded(news);
        holder.descriptionTextView.setVisibility(expanded ? View.VISIBLE : View.GONE);
        holder.linkLayout.setVisibility(expanded ? View.VISIBLE : View.GONE);
        boolean hasImage = news.getImageUrl() != null && !news.getImageUrl().trim().isEmpty();
        holder.newsImageView.setVisibility(hasImage ? View.VISIBLE : View.GONE);

        holder.newsLayout.setOnClickListener(v -> {
            int adapterPos = holder.getBindingAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION || adapterPos >= newsList.size()) {
                return;
            }

            News currentNews = newsList.get(adapterPos);
            String newsId = getNewsId(currentNews);
            boolean wasExpanded = expandedNewsIds.contains(newsId);
            boolean willExpand = !wasExpanded;

            if (willExpand) {
                expandedNewsIds.add(newsId);
            } else {
                expandedNewsIds.remove(newsId);
            }

            ViewGroup parent = (ViewGroup) holder.itemView.getParent();
            if (parent != null) {
                TransitionSet transitionSet = new TransitionSet();
                transitionSet.setOrdering(TransitionSet.ORDERING_TOGETHER);
                transitionSet.addTransition(new ChangeBounds());
                transitionSet.addTransition(new Fade());
                transitionSet.setDuration(220);
                transitionSet.setInterpolator(new DecelerateInterpolator());
                TransitionManager.beginDelayedTransition(parent, transitionSet);
            }

            holder.descriptionTextView.setVisibility(willExpand ? View.VISIBLE : View.GONE);
            holder.linkLayout.setVisibility(willExpand ? View.VISIBLE : View.GONE);
        });

        holder.linkLayout.setOnClickListener(v -> {
            int adapterPos = holder.getBindingAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION || adapterPos >= newsList.size()) {
                return;
            }
            News currentNews = newsList.get(adapterPos);
            String link = currentNews.getLink();
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

        final View newsLayout, linkLayout;
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
