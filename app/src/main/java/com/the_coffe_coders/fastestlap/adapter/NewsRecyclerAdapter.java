package com.the_coffe_coders.fastestlap.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.news.F1News;
import com.the_coffe_coders.fastestlap.domain.news.JuniorNews;
import com.the_coffe_coders.fastestlap.domain.news.News;
import com.the_coffe_coders.fastestlap.util.service.NetworkUtils;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class NewsRecyclerAdapter extends RecyclerView.Adapter<NewsRecyclerAdapter.NewsViewHolder> {
    public static final int VIEW_TYPE_F1 = 0;
    public static final int VIEW_TYPE_JUNIOR = 1;
    public static final int VIEW_TYPE_LOAD_MORE = 2;

    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    private final List<News> newsList;
    private final Context context;
    private final Set<String> expandedNewsIds;
    private final Runnable onImageLoaded;
    private final int itemsToWaitFor;

    private boolean loadMoreEnabled = false;
    private boolean loadingMore = false;
    private OnLoadMoreListener onLoadMoreListener;

    private NetworkUtils networkLiveData;

    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        this.onLoadMoreListener = listener;
    }

    public void setLoadMoreEnabled(boolean enabled) {
        if (this.loadMoreEnabled != enabled) {
            boolean hadFooter = this.loadMoreEnabled && !newsList.isEmpty();
            this.loadMoreEnabled = enabled;
            boolean hasFooter = this.loadMoreEnabled && !newsList.isEmpty();
            if (!hadFooter && hasFooter) {
                notifyItemInserted(newsList.size());
            } else if (hadFooter && !hasFooter) {
                notifyItemRemoved(newsList.size());
            }
        }
    }

    public int getNewsCount() {
        return newsList != null ? newsList.size() : 0;
    }

    public void setLoadingMore(boolean loading) {
        if (this.loadingMore != loading) {
            this.loadingMore = loading;
            if (loadMoreEnabled && !newsList.isEmpty()) {
                notifyItemChanged(newsList.size());
            }
        }
    }

    public NewsRecyclerAdapter(List<News> newsList, Context context, Runnable onImageLoaded, int itemsToWaitFor, Set<String> expandedNewsIds) {
        this.newsList = new ArrayList<>(newsList != null ? newsList : Collections.emptyList());
        this.context = context;
        this.onImageLoaded = onImageLoaded;
        this.itemsToWaitFor = itemsToWaitFor;
        this.expandedNewsIds = expandedNewsIds != null ? expandedNewsIds : new HashSet<>();
        this.networkLiveData = new NetworkUtils(context);
    }

    public void updateNewsList(List<News> newNewsList) {
        if (newNewsList == null) return;

        if (newsList.isEmpty()) {
            newsList.clear();
            newsList.addAll(newNewsList);
            notifyDataSetChanged();
            return;
        }

        if (newNewsList.size() > newsList.size() && isPrefix(newsList, newNewsList)) {
            int insertPosition = newsList.size();
            int insertCount = newNewsList.size() - newsList.size();
            newsList.clear();
            newsList.addAll(newNewsList);
            notifyItemRangeInserted(insertPosition, insertCount);
            return;
        }

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
                if (oldItem.getClass() != newItem.getClass()) return false;
                if (!Objects.equals(oldItem.getTitle(), newItem.getTitle())
                        || !Objects.equals(oldItem.getImageUrl(), newItem.getImageUrl())
                        || !Objects.equals(oldItem.getLink(), newItem.getLink())) {
                    return false;
                }
                if (oldItem instanceof F1News && newItem instanceof F1News) {
                    F1News oldF1 = (F1News) oldItem;
                    F1News newF1 = (F1News) newItem;
                    return Objects.equals(oldF1.getDate(), newF1.getDate())
                            && Objects.equals(oldF1.getDescription(), newF1.getDescription())
                            && Objects.equals(oldF1.getCategory(), newF1.getCategory());
                }
                if (oldItem instanceof JuniorNews && newItem instanceof JuniorNews) {
                    JuniorNews oldJ = (JuniorNews) oldItem;
                    JuniorNews newJ = (JuniorNews) newItem;
                    return Objects.equals(oldJ.getPublishedAgo(), newJ.getPublishedAgo())
                            && Objects.equals(oldJ.getSeriesId(), newJ.getSeriesId());
                }
                return true;
            }
        });

        newsList.clear();
        newsList.addAll(newNewsList);
        diffResult.dispatchUpdatesTo(this);
    }

    private boolean isPrefix(List<News> prefix, List<News> full) {
        if (prefix.size() > full.size()) return false;
        for (int i = 0; i < prefix.size(); i++) {
            if (!getNewsId(prefix.get(i)).equals(getNewsId(full.get(i)))) {
                return false;
            }
        }
        return true;
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

    @Override
    public int getItemViewType(int position) {
        if (loadMoreEnabled && !newsList.isEmpty() && position == newsList.size()) {
            return VIEW_TYPE_LOAD_MORE;
        }
        if (position >= 0 && position < newsList.size()) {
            News news = newsList.get(position);
            if (news instanceof JuniorNews) {
                return VIEW_TYPE_JUNIOR;
            }
        }
        return VIEW_TYPE_F1;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_LOAD_MORE) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.junior_news_load_more_item, parent, false);
            return new LoadMoreViewHolder(view);
        }
        int layoutRes = (viewType == VIEW_TYPE_JUNIOR) ? R.layout.junior_news_item : R.layout.news_item;
        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutRes, parent, false);

        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_LOAD_MORE) {
            if (holder instanceof LoadMoreViewHolder) {
                LoadMoreViewHolder lmHolder = (LoadMoreViewHolder) holder;
                if (loadingMore) {
                    if (lmHolder.progressBar != null) lmHolder.progressBar.setVisibility(View.VISIBLE);
                    if (lmHolder.loadMoreText != null) lmHolder.loadMoreText.setText(R.string.loading_more);
                    if (lmHolder.loadMoreButton != null) lmHolder.loadMoreButton.setEnabled(false);
                } else {
                    if (lmHolder.progressBar != null) lmHolder.progressBar.setVisibility(View.GONE);
                    if (lmHolder.loadMoreText != null) lmHolder.loadMoreText.setText(R.string.load_more);
                    if (lmHolder.loadMoreButton != null) lmHolder.loadMoreButton.setEnabled(true);
                }
                if (lmHolder.loadMoreButton != null) {
                    if (!newsList.isEmpty() && newsList.get(0) instanceof JuniorNews) {
                        JuniorNews juniorNews = (JuniorNews) newsList.get(0);
                        if (juniorNews.getSeriesId() != null && juniorNews.getSeriesId().equals("f2")) {
                            lmHolder.loadMoreButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.formula_2)));
                        } else {
                            lmHolder.loadMoreButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.app_primary_red)));
                        }
                    }

                    lmHolder.loadMoreButton.setOnClickListener(v -> {
                        if (!loadingMore && onLoadMoreListener != null) {
                            onLoadMoreListener.onLoadMore();
                        }
                    });
                }
            }
            return;
        }

        News news = newsList.get(position);

        if (holder.titleTextView != null && holder.dateTextView != null) {
            UIUtils.multipleSetTextViewText(
                    new String[]{
                            news.getTitle(),
                            news.getFormattedDate(context)},
                    new TextView[]{
                            holder.titleTextView,
                            holder.dateTextView
                    });
        } else {
            if (holder.titleTextView != null) {
                holder.titleTextView.setText(news.getTitle());
            }
            if (holder.dateTextView != null) {
                holder.dateTextView.setText(news.getFormattedDate(context));
            }
        }

        if (holder.dateTextView != null) {
            if (news instanceof JuniorNews) {
                JuniorNews juniorNews = (JuniorNews) news;
                if (juniorNews.getSeriesId() != null && juniorNews.getSeriesId().equals("f2")) {
                    holder.dateTextView.setTextColor(ContextCompat.getColor(context, R.color.formula_2));
                } else {
                    holder.dateTextView.setTextColor(ContextCompat.getColor(context, R.color.app_primary_red));
                }
            } else {
                holder.dateTextView.setTextColor(ContextCompat.getColor(context, R.color.app_primary_red));
            }
        }

        if (holder.newsImageView != null) {
            Runnable imageCallback = null;
            if (position < itemsToWaitFor && onImageLoaded != null) {
                imageCallback = onImageLoaded;
            }

            UIUtils.loadImageWithGlide(
                    context,
                    news.getImageUrl(),
                    holder.newsImageView,
                    imageCallback);

            boolean hasImage = news.getImageUrl() != null && !news.getImageUrl().trim().isEmpty();
            holder.newsImageView.setVisibility(hasImage ? View.VISIBLE : View.GONE);
        }

        boolean hasDesc = news.hasDescription();
        if (holder.descriptionTextView != null) {
            UIUtils.setTextViewTextWithCondition(hasDesc,
                    UIUtils.formatXmlText(news.getDescription()),
                    "",
                    holder.descriptionTextView);
        }

        boolean expanded = isExpanded(news);
        if (holder.descriptionTextView != null) {
            holder.descriptionTextView.setVisibility((expanded && hasDesc) ? View.VISIBLE : View.GONE);
        }
        if (holder.linkLayout != null) {
            holder.linkLayout.setVisibility(expanded ? View.VISIBLE : View.GONE);
        }

        if (holder.newsLayout != null) {
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

                boolean currentHasDesc = currentNews.hasDescription();
                if (holder.descriptionTextView != null) {
                    holder.descriptionTextView.setVisibility((willExpand && currentHasDesc) ? View.VISIBLE : View.GONE);
                }
                if (holder.linkLayout != null) {
                    holder.linkLayout.setVisibility(willExpand ? View.VISIBLE : View.GONE);
                }
            });
        }

        if (holder.linkLayout != null) {
            holder.linkLayout.setOnClickListener(v -> {
                int adapterPos = holder.getBindingAdapterPosition();
                if (adapterPos == RecyclerView.NO_POSITION || adapterPos >= newsList.size()) {
                    return;
                }
                News currentNews = newsList.get(adapterPos);
                String link = currentNews.getLink();
                if (link != null && !link.isEmpty()) {
                    if (!networkLiveData.isConnected()) {
                        Toast.makeText(context, R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
                        return;
                    }
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
    }

    @Override
    public int getItemCount() {
        return newsList.size() + (loadMoreEnabled && !newsList.isEmpty() ? 1 : 0);
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

    public static class LoadMoreViewHolder extends NewsViewHolder {
        final View loadMoreButton;
        final TextView loadMoreText;
        final android.widget.ProgressBar progressBar;

        public LoadMoreViewHolder(@NonNull View itemView) {
            super(itemView);
            loadMoreButton = itemView.findViewById(R.id.load_more_button);
            loadMoreText = itemView.findViewById(R.id.load_more_text);
            progressBar = itemView.findViewById(R.id.load_more_progress);
        }
    }
}
