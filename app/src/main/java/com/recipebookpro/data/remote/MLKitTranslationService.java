package com.recipebookpro.data.remote;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.recipebookpro.domain.service.TranslationService;

import java.util.HashMap;
import java.util.Map;

public class MLKitTranslationService implements TranslationService {

    private static final String TAG = "MLKitTranslation";
    private final Context context;
    private final Map<String, Translator> translators = new HashMap<>();

    public MLKitTranslationService(Context context) {
        this.context = context;
    }

    @Override
    public Task<Void> prepareModel(String sourceLang, String targetLang) {
        Log.d(TAG, "Preparing models for " + sourceLang + " -> " + targetLang);
        return getTranslator(sourceLang, targetLang).onSuccessTask(translator -> {
            Log.d(TAG, "Triggering manual download if needed...");
            return translator.downloadModelIfNeeded(new DownloadConditions.Builder().build());
        });
    }

    @Override
    public Task<String> translateSingleField(String text, String sourceLang, String targetLang) {
        if (text == null || text.trim().isEmpty()) {
            return Tasks.forResult("");
        }

        return getTranslator(sourceLang, targetLang).onSuccessTask(translator -> {
            return translator.translate(text).addOnFailureListener(e -> {
                Log.e(TAG, "Translation failed for text: " + text, e);
            });
        });
    }

    private Task<Translator> getTranslator(String source, String target) {
        String key = source + "_" + target;
        if (translators.containsKey(key)) {
            return Tasks.forResult(translators.get(key));
        }

        try {
            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(source)
                    .setTargetLanguage(target)
                    .build();

            Translator translator = Translation.getClient(options);
            translators.put(key, translator);
            
            // Ensure lifecycle is managed
            // Note: In a real app, we might want to close these in onDestroy
            
            return Tasks.forResult(translator);
        } catch (Exception e) {
            Log.e(TAG, "Error creating translator", e);
            return Tasks.forException(e);
        }
    }

    @Override
    public void close() {
        for (Translator translator : translators.values()) {
            translator.close();
        }
        translators.clear();
    }
}
