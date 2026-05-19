package com.recipebookpro.util;

import android.content.Context;
import android.text.TextUtils;

import com.google.android.gms.tasks.Tasks;
import com.recipebookpro.data.remote.MLKitTranslationService;
import com.recipebookpro.domain.model.LocalizedText;
import com.recipebookpro.presentation.ui.LocaleHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fills and validates TR/EN sides of {@link LocalizedText} entries via ML Kit.
 */
public final class BilingualTextHelper {

    public interface Callback {
        void onComplete(List<LocalizedText> items);
    }

    public interface SingleCallback {
        void onComplete(LocalizedText item);
    }

    private BilingualTextHelper() {
    }

    public static List<LocalizedText> mergeLegacyStrings(List<String> legacy, List<LocalizedText> i18n) {
        if (i18n != null && !i18n.isEmpty()) {
            return new ArrayList<>(i18n);
        }
        List<LocalizedText> result = new ArrayList<>();
        if (legacy == null) {
            return result;
        }
        for (String text : legacy) {
            if (!TextUtils.isEmpty(text)) {
                result.add(LocalizedText.fromLegacy(text.trim()));
            }
        }
        return result;
    }

    /**
     * Ensures both TR/EN are stored and the side for the current app language is correct.
     */
    public static void syncForAppLanguage(Context context, LocalizedText item, String uiLang,
                                          SingleCallback callback) {
        if (item == null) {
            callback.onComplete(new LocalizedText());
            return;
        }
        syncAllForAppLanguage(context, java.util.Collections.singletonList(item), uiLang, list -> {
            callback.onComplete(list.isEmpty() ? item : list.get(0));
        });
    }

    public static void syncAllForAppLanguage(Context context, List<LocalizedText> items, String uiLang,
                                            Callback callback) {
        if (items == null || items.isEmpty()) {
            callback.onComplete(new ArrayList<>());
            return;
        }
        String ui = ShoppingIngredientLocaleFix.normalizeTargetLang(uiLang);
        sanitizeInvalidSides(items, ui);
        ensureComplete(context, items, completed -> callback.onComplete(completed));
    }

    /**
     * Clears wrong-language copies (e.g. Turkish duplicated into {@code en}) so ML Kit re-translates.
     */
    private static void sanitizeInvalidSides(List<LocalizedText> items, String uiLang) {
        for (LocalizedText item : items) {
            if (item == null) {
                continue;
            }
            if (!TextUtils.isEmpty(item.getTr()) && !TextUtils.isEmpty(item.getEn())
                    && item.getTr().trim().equalsIgnoreCase(item.getEn().trim())) {
                if ("en".equals(uiLang) && LocalizedText.looksTurkish(item.getTr())) {
                    item.setEn(null);
                } else if ("tr".equals(uiLang) && LocalizedText.looksEnglish(item.getEn())) {
                    item.setTr(null);
                }
            }
            if (item.needsLangSide("tr")) {
                if (!TextUtils.isEmpty(item.getEn()) && LocalizedText.looksEnglish(item.getEn())
                        && TextUtils.isEmpty(item.getTr())) {
                    // will translate en -> tr
                } else if (LocalizedText.looksTurkish(item.getTr()) && TextUtils.isEmpty(item.getEn())) {
                    // ok, fill en
                } else if (!TextUtils.isEmpty(item.getTr()) && !LocalizedText.looksTurkish(item.getTr())
                        && TextUtils.isEmpty(item.getEn())) {
                    // tr field has english typo - swap via clear
                    item.setEn(item.getTr());
                    item.setTr(null);
                }
            }
            if (item.needsLangSide("en")) {
                if (!TextUtils.isEmpty(item.getEn()) && LocalizedText.looksTurkish(item.getEn())) {
                    item.setEn(null);
                }
            }
        }
    }

    public static void ensureComplete(Context context, List<LocalizedText> items, Callback callback) {
        if (items == null || items.isEmpty()) {
            callback.onComplete(new ArrayList<>());
            return;
        }
        List<LocalizedText> needsWork = new ArrayList<>();
        for (LocalizedText item : items) {
            if (item.needsTranslation() || item.needsLangSide("tr") || item.needsLangSide("en")) {
                needsWork.add(item);
            }
        }
        if (needsWork.isEmpty()) {
            callback.onComplete(items);
            return;
        }

        MLKitTranslationService svc = new MLKitTranslationService(context);
        List<com.google.android.gms.tasks.Task<String>> tasks = new ArrayList<>();
        List<LocalizedText> taskTargets = new ArrayList<>();
        List<String> taskTargetsLang = new ArrayList<>();

        for (LocalizedText item : needsWork) {
            if (item.needsLangSide("en") || (item.needsTranslation() && !TextUtils.isEmpty(item.getTr()))) {
                String source = !TextUtils.isEmpty(item.getTr()) ? item.getTr() : item.getEn();
                if (!TextUtils.isEmpty(source) && (item.needsLangSide("en") || TextUtils.isEmpty(item.getEn()))) {
                    String srcLang = LocalizedText.looksTurkish(source) ? "tr" : "en";
                    tasks.add(svc.translateSingleField(source, srcLang, "en"));
                    taskTargets.add(item);
                    taskTargetsLang.add("en");
                }
            }
            if (item.needsLangSide("tr") || (item.needsTranslation() && !TextUtils.isEmpty(item.getEn()))) {
                String source = !TextUtils.isEmpty(item.getEn()) ? item.getEn() : item.getTr();
                if (!TextUtils.isEmpty(source) && (item.needsLangSide("tr") || TextUtils.isEmpty(item.getTr()))) {
                    String srcLang = LocalizedText.looksTurkish(source) ? "tr" : "en";
                    tasks.add(svc.translateSingleField(source, srcLang, "tr"));
                    taskTargets.add(item);
                    taskTargetsLang.add("tr");
                }
            }
        }

        if (tasks.isEmpty()) {
            svc.close();
            callback.onComplete(items);
            return;
        }

        svc.prepareModel("tr", "en").addOnSuccessListener(unused ->
                Tasks.whenAllComplete(tasks).addOnCompleteListener(done -> {
                    for (int i = 0; i < tasks.size() && i < taskTargets.size(); i++) {
                        com.google.android.gms.tasks.Task<String> task = tasks.get(i);
                        LocalizedText target = taskTargets.get(i);
                        String targetLang = taskTargetsLang.get(i);
                        if (task.isSuccessful() && task.getResult() != null) {
                            String translated = task.getResult().trim();
                            if ("en".equals(targetLang)) {
                                target.setEn(translated);
                            } else {
                                target.setTr(translated);
                            }
                        }
                    }
                    svc.close();
                    callback.onComplete(items);
                })
        ).addOnFailureListener(e -> {
            svc.close();
            callback.onComplete(items);
        });
    }

    public static List<String> labelsForLang(List<LocalizedText> items, String lang) {
        List<String> labels = new ArrayList<>();
        if (items == null) {
            return labels;
        }
        String normalized = ShoppingIngredientLocaleFix.normalizeTargetLang(lang);
        for (LocalizedText item : items) {
            String label = item.getForLang(normalized);
            if (!TextUtils.isEmpty(label)) {
                labels.add(label);
            }
        }
        return labels;
    }
}
