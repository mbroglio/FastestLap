package com.the_coffe_coders.fastestlap.ui.bio.handler;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.util.Constants;
import com.the_coffe_coders.fastestlap.util.SharedPreferencesUtils;

public class FavoriteBioHandler {

    private final SharedPreferencesUtils sharedPreferencesUtils;

    public FavoriteBioHandler(Context context) {
        this.sharedPreferencesUtils = new SharedPreferencesUtils(context);
    }

    public void updateFavoriteIcon(Menu menu, int menuItemId, String itemId, String preferenceKey) {
        if (menu == null) {
            return;
        }

        MenuItem favoriteItem = menu.findItem(menuItemId);
        if (favoriteItem == null) {
            return;
        }

        String favoriteId = getFavoriteId(preferenceKey);
        favoriteItem.setIcon(isFavorite(itemId, favoriteId)
                ? R.drawable.star_fav
                : R.drawable.baseline_star_border_24);
    }

    public String toggleFavorite(String itemId,
                                 String preferenceKey,
                                 MenuItem menuItem,
                                 FavoriteChangedCallback favoriteChangedCallback) {
        if (itemId == null || menuItem == null) {
            return null;
        }

        String favoriteId = getFavoriteId(preferenceKey);
        String updatedFavoriteId;

        if (isFavorite(itemId, favoriteId)) {
            updatedFavoriteId = "null";
            sharedPreferencesUtils.writeStringData(Constants.SHARED_PREFERENCES_FILENAME, preferenceKey, updatedFavoriteId);
            menuItem.setIcon(R.drawable.baseline_star_border_24);
        } else {
            updatedFavoriteId = itemId;
            sharedPreferencesUtils.writeStringData(Constants.SHARED_PREFERENCES_FILENAME, preferenceKey, updatedFavoriteId);
            menuItem.setIcon(R.drawable.star_fav);
        }

        if (favoriteChangedCallback != null) {
            favoriteChangedCallback.onFavoriteChanged(updatedFavoriteId);
        }

        return updatedFavoriteId;
    }

    public String getFavoriteId(String preferenceKey) {
        return sharedPreferencesUtils.readStringData(
                Constants.SHARED_PREFERENCES_FILENAME,
                preferenceKey);
    }

    private boolean isFavorite(String itemId, String favoriteId) {
        return itemId != null && itemId.equals(favoriteId);
    }

    @FunctionalInterface
    public interface FavoriteChangedCallback {
        void onFavoriteChanged(String favoriteId);
    }
}
