package com.recipebookpro;

import android.app.Application;

import androidx.annotation.NonNull;

import coil.ImageLoader;
import coil.ImageLoaderFactory;
import coil.disk.DiskCache;
import coil.memory.MemoryCache;

public class RecipeBookProApplication extends Application implements ImageLoaderFactory {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @NonNull
    @Override
    public ImageLoader newImageLoader() {
        // Simplified ImageLoader configuration to avoid potential runtime issues with CoilUtils
        return new ImageLoader.Builder(this)
                .memoryCache(new MemoryCache.Builder(this)
                        .maxSizePercent(0.25)
                        .build())
                .diskCache(new DiskCache.Builder()
                        .directory(new java.io.File(getCacheDir(), "sticker_cache"))
                        .maxSizeBytes(100 * 1024 * 1024) // 100MB
                        .build())
                .crossfade(true)
                .build();
    }
}
