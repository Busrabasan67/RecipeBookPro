package com.recipebookpro.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Base activity that enables edge-to-edge display and handles
 * system bar insets for all activities in the app.
 * <p>
 * Subclasses should call {@code super.onCreate()} and then
 * {@link #applyInsetsToView(View)} on their root view.
 */
public abstract class BaseActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "settings";
    private static final String KEY_THEME = "theme";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int selectedTheme = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(selectedTheme);

        // Enable edge-to-edge before calling super
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
    }

    /**
     * Applies window insets to the given view so that its padding
     * adjusts for the status bar (top) and navigation bar (bottom).
     *
     * @param rootView the root view of the activity's layout
     */
    protected void applyInsetsToView(View rootView) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /**
     * Applies only top inset (status bar) padding to the given view.
     * Useful when the bottom is handled by BottomNavigationView.
     *
     * @param view the view to pad at top
     */
    protected void applyTopInsetToView(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), insets.top, v.getPaddingRight(), v.getPaddingBottom());
            return windowInsets; // Don't consume — let children handle bottom
        });
    }

    /**
     * Applies only bottom inset (navigation bar) padding to the given view.
     *
     * @param view the view to pad at bottom
     */
    protected void applyBottomInsetToView(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
