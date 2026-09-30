package com.the_coffe_coders.fastestlap.domain.news;

import android.content.Context;

import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

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
public class F1News extends News {
    private String category;
    private String description;
    private String date;

    public F1News(String title, String link, String description, String date, String category, String imageUrl) {
        super(title, link, imageUrl);
        this.description = description;
        this.date = date;
        this.category = category;
    }

    @Override
    public String getFormattedDate(Context context) {
        if (date != null && !date.trim().isEmpty()) {
            return UIUtils.getTimeAgo(date, context);
        }
        return "";
    }

    @Override
    public String getDate() {
        return date;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }
}
