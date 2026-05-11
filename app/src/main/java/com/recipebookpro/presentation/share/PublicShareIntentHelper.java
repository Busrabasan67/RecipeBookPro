package com.recipebookpro.presentation.share;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.recipebookpro.BuildConfig;
import com.recipebookpro.R;
import com.recipebookpro.domain.model.Cookbook;
import com.recipebookpro.domain.model.Recipe;

import java.util.Locale;

/**
 * Paylaşımda ilk satırda gerçek {@code https://} adresi kullanılır (WhatsApp bağlantı gösterir).
 * Varsayılan: Firebase Hosting’de yayınlanan {@code open.html} (proje kökünde firebase.json + sharing/).
 */
public final class PublicShareIntentHelper {

    private static final int MAX_SHARE_DESCRIPTION_CHARS = 600;

    private static final String QUERY_KIND_RECIPE = "recipe";
    private static final String QUERY_KIND_COOKBOOK = "cookbook";

    private PublicShareIntentHelper() {}

    public static Intent createRecipeShareChooserIntent(Context context, Recipe recipe) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, plainRecipeTitle(recipe));
        send.putExtra(Intent.EXTRA_TEXT, buildRecipeSharePlainBody(context, recipe));
        return Intent.createChooser(send, context.getString(R.string.share_recipe));
    }

    public static Intent createCookbookShareChooserIntent(Context context, Cookbook cookbook) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, cookbook.getName());
        send.putExtra(Intent.EXTRA_TEXT, buildCookbookSharePlainBody(context, cookbook));
        return Intent.createChooser(send, context.getString(R.string.share_cookbook));
    }

    private static String plainRecipeTitle(Recipe recipe) {
        if (recipe == null) return "";
        String lang = safeLang(Locale.getDefault().getLanguage());
        return recipe.getDisplayTitle(lang);
    }

    private static String safeLang(String lang) {
        return TextUtils.isEmpty(lang) ? "en" : lang;
    }

    private static String normalizedBridgeBase() {
        String b = BuildConfig.SHARE_HTTPS_REDIRECT_BASE.trim();
        while (b.endsWith("/") && b.length() > 1) {
            b = b.substring(0, b.length() - 1);
        }
        return b;
    }

    private static String httpsBridgeOpenUrl(String kind, String firebaseDocId) {
        return Uri.parse(normalizedBridgeBase()).buildUpon()
                .appendQueryParameter("k", kind)
                .appendQueryParameter("id", firebaseDocId)
                .build()
                .toString();
    }

    private static String playStoreUrl() {
        return "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID;
    }

    private static String buildRecipeSharePlainBody(Context context, Recipe recipe) {
        if (recipe == null) return "";
        String lang = safeLang(Locale.getDefault().getLanguage());
        String title = recipe.getDisplayTitle(lang);
        String description = shorten(recipe.getDisplayDescription(lang));

        StringBuilder sb = new StringBuilder();
        sb.append(httpsBridgeOpenUrl(QUERY_KIND_RECIPE, recipe.getId())).append("\n\n");

        sb.append(title);
        if (!description.isEmpty()) {
            sb.append("\n\n").append(description);
        }

        sb.append("\n\n").append(context.getString(R.string.share_open_link_prompt));
        sb.append("\n").append(context.getString(R.string.share_install_recipebook, playStoreUrl()));
        return sb.toString();
    }

    private static String buildCookbookSharePlainBody(Context context, Cookbook cookbook) {
        String readable = context.getString(R.string.share_cookbook_message, cookbook.getName());

        StringBuilder sb = new StringBuilder();
        sb.append(httpsBridgeOpenUrl(QUERY_KIND_COOKBOOK, cookbook.getId())).append("\n\n");
        sb.append(readable);
        sb.append("\n\n").append(context.getString(R.string.share_open_link_prompt));
        sb.append("\n").append(context.getString(R.string.share_install_recipebook, playStoreUrl()));
        return sb.toString();
    }

    private static String shorten(String description) {
        if (description == null) return "";
        String t = description.trim();
        if (t.isEmpty()) return "";
        if (t.length() <= MAX_SHARE_DESCRIPTION_CHARS) return t;
        return t.substring(0, MAX_SHARE_DESCRIPTION_CHARS) + "…";
    }
}
