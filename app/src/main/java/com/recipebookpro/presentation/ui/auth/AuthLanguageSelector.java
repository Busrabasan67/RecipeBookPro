package com.recipebookpro.presentation.ui.auth;

import android.app.Activity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.color.MaterialColors;
import com.recipebookpro.R;
import com.recipebookpro.presentation.ui.LocaleHelper;

final class AuthLanguageSelector {

    private static final String LANG_TR = "tr";
    private static final String LANG_EN = "en";
    private static final long ANIMATION_DURATION_MS = 220L;

    private AuthLanguageSelector() {
    }

    static void setup(@NonNull Activity activity) {
        View selector = activity.findViewById(R.id.languageSelector);
        View thumb = activity.findViewById(R.id.languageSelectorThumb);
        TextView tvTr = activity.findViewById(R.id.tvLangTr);
        TextView tvEn = activity.findViewById(R.id.tvLangEn);

        String currentLanguage = normalizedLanguage(activity);
        selector.post(() -> updateSelection(selector, thumb, tvTr, tvEn, currentLanguage, false));

        tvTr.setOnClickListener(v -> setLanguage(activity, selector, thumb, tvTr, tvEn, LANG_TR));
        tvEn.setOnClickListener(v -> setLanguage(activity, selector, thumb, tvTr, tvEn, LANG_EN));
        selector.setOnClickListener(v -> {
            String nextLanguage = LANG_EN.equals(normalizedLanguage(activity)) ? LANG_TR : LANG_EN;
            setLanguage(activity, selector, thumb, tvTr, tvEn, nextLanguage);
        });
    }

    private static void setLanguage(
            Activity activity,
            View selector,
            View thumb,
            TextView tvTr,
            TextView tvEn,
            String language
    ) {
        if (language.equals(normalizedLanguage(activity))) {
            return;
        }

        updateSelection(selector, thumb, tvTr, tvEn, language, true);
        LocaleHelper.setLocale(activity, language);
        activity.recreate();
    }

    private static void updateSelection(
            View selector,
            View thumb,
            TextView tvTr,
            TextView tvEn,
            String language,
            boolean animate
    ) {
        boolean englishSelected = LANG_EN.equals(language);
        float target = englishSelected ? selectedOffset(selector) : 0f;

        if (animate) {
            thumb.animate()
                    .translationX(target)
                    .setDuration(ANIMATION_DURATION_MS)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        } else {
            thumb.setTranslationX(target);
        }

        int selectedColor = MaterialColors.getColor(selector, com.google.android.material.R.attr.colorOnPrimary);
        int unselectedColor = MaterialColors.getColor(selector, com.google.android.material.R.attr.colorOnSurfaceVariant);
        tvTr.setTextColor(englishSelected ? unselectedColor : selectedColor);
        tvEn.setTextColor(englishSelected ? selectedColor : unselectedColor);
        tvTr.setSelected(!englishSelected);
        tvEn.setSelected(englishSelected);
    }

    private static float selectedOffset(View selector) {
        int horizontalPadding = selector.getPaddingLeft() + selector.getPaddingRight();
        return (selector.getWidth() - horizontalPadding) / 2f;
    }

    private static String normalizedLanguage(Activity activity) {
        return LANG_EN.equals(LocaleHelper.getLanguage(activity)) ? LANG_EN : LANG_TR;
    }
}
