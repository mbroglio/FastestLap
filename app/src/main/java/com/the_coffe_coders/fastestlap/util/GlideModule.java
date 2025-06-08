package com.the_coffe_coders.fastestlap.util;

import android.content.Context;
import androidx.annotation.NonNull;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory;
import com.bumptech.glide.module.AppGlideModule;

@com.bumptech.glide.annotation.GlideModule
public class GlideModule extends AppGlideModule {
    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        // Set the disk cache size to 500MB
        int diskCacheSizeBytes = 500 * 1024 * 1024; // 500 MB
        builder.setDiskCache(new DiskLruCacheFactory(context.getCacheDir().getPath() + "/glide_cache", diskCacheSizeBytes));
    }
}