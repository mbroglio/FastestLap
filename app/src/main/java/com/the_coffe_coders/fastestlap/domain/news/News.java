package com.the_coffe_coders.fastestlap.domain.news;

import android.content.Context;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class News {
    private String title;
    private String link;
    private String imageUrl;

    /**
     * Formats and returns the publication date for display in the UI.
     * Subclasses (F1News, JuniorNews) provide their specific date formatting logic.
     *
     * @param context Android context for localized string resources
     * @return Formatted date string (e.g. "5 hours ago", "2 days ago")
     */
    public abstract String getFormattedDate(Context context);

    /**
     * Returns the raw date representation.
     */
    public abstract String getDate();

    /**
     * Returns the article description, if available.
     * Defaults to null for news types that do not have a description.
     */
    public String getDescription() {
        return null;
    }

    /**
     * Returns whether this news item has a non-empty description.
     */
    public boolean hasDescription() {
        return getDescription() != null && !getDescription().trim().isEmpty();
    }
}