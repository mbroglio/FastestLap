package com.the_coffe_coders.fastestlap.util.glide;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;

@com.bumptech.glide.annotation.GlideModule
public class GlideModule extends AppGlideModule {
    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        // Configure dynamic Memory Cache and Bitmap Pool size based on device RAM and screens
        MemorySizeCalculator calculator = new MemorySizeCalculator.Builder(context)
                .setMemoryCacheScreens(2.5f)
                .setBitmapPoolScreens(3.0f)
                .build();
        builder.setMemoryCache(new LruResourceCache(calculator.getMemoryCacheSize()));
        builder.setBitmapPool(new LruBitmapPool(calculator.getBitmapPoolSize()));

        // Set the disk cache size to 500MB
        int diskCacheSizeBytes = 500 * 1024 * 1024; // 500 MB
        builder.setDiskCache(new InternalCacheDiskCacheFactory(context, "glide_cache", diskCacheSizeBytes));

        // Default request options: cache both stream and decoded resource, enable memory caching
        builder.setDefaultRequestOptions(new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .downsample(DownsampleStrategy.AT_MOST)
                .skipMemoryCache(false)
        );
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}