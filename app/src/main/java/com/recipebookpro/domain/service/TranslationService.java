package com.recipebookpro.domain.service;

import com.google.android.gms.tasks.Task;

public interface TranslationService {
    
    /**
     * Prepares and downloads the translation model if needed.
     */
    Task<Void> prepareModel(String sourceLang, String targetLang);

    /**
     * Translates a single field using Google ML Kit.
     */
    Task<String> translateSingleField(String text, String sourceLang, String targetLang);

    /**
     * Closes resources.
     */
    void close();

    /**
     * Callback for translation progress and results.
     */
    interface TranslationCallback {
        void onSuccess(String message);
        void onFailure(Exception e);
        void onDownloadProgress(String message);
    }
}
