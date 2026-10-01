package com.the_coffe_coders.fastestlap.domain.news;

import android.content.Context;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString(callSuper = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JuniorNews extends News {
    private String publishedAgo;
    private String seriesId;

    public JuniorNews(String title, String link, String publishedAgo, String imageUrl) {
        super(title, link, imageUrl);
        this.publishedAgo = publishedAgo;
    }

    public JuniorNews(String title, String link, String publishedAgo, String imageUrl, String seriesId) {
        super(title, link, imageUrl);
        this.publishedAgo = publishedAgo;
        this.seriesId = seriesId;
    }

    @Override
    public String getFormattedDate(Context context) {
        return publishedAgo != null ? publishedAgo : "";
    }

    @Override
    public String getDate() {
        return publishedAgo;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean hasDescription() {
        return false;
    }

    public String getNewsUrl() {
        return getLink();
    }

    public void setNewsUrl(String newsUrl) {
        setLink(newsUrl);
    }

    public String getSeries() {
        return seriesId;
    }

    public void setSeries(String series) {
        this.seriesId = series;
    }

    public int getSeriesColorRes() {
        if ("f3".equalsIgnoreCase(seriesId)) {
            return com.the_coffe_coders.fastestlap.R.color.formula_3;
        }
        return com.the_coffe_coders.fastestlap.R.color.formula_2;
    }
}
