package com.recipebookpro.util;

import android.content.Context;
import com.recipebookpro.presentation.ui.LocaleHelper;

public class LocaleUtils {
    public static boolean isTurkish(Context context) {
        String lang = LocaleHelper.getLanguage(context);
        return "tr".equalsIgnoreCase(lang);
    }
}
