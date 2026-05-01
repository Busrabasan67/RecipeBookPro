package com.recipebookpro.presentation.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.recipebookpro.R;
import com.recipebookpro.presentation.ui.LocaleHelper;
import com.recipebookpro.presentation.ui.MainActivity;

public class SettingsBottomSheet extends BottomSheetDialogFragment {

    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE);

        MaterialButton btnPassword = view.findViewById(R.id.btnChangePassword);
        RadioGroup rgTheme = view.findViewById(R.id.rgTheme);
        RadioGroup rgLanguage = view.findViewById(R.id.rgLanguage);

        btnPassword.setOnClickListener(v -> showChangePasswordDialog());

        int currentTheme = prefs.getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (currentTheme == AppCompatDelegate.MODE_NIGHT_NO) {
            ((RadioButton) view.findViewById(R.id.rbThemeLight)).setChecked(true);
        } else if (currentTheme == AppCompatDelegate.MODE_NIGHT_YES) {
            ((RadioButton) view.findViewById(R.id.rbThemeDark)).setChecked(true);
        } else {
            ((RadioButton) view.findViewById(R.id.rbThemeSystem)).setChecked(true);
        }

        rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            if (checkedId == R.id.rbThemeLight) mode = AppCompatDelegate.MODE_NIGHT_NO;
            else if (checkedId == R.id.rbThemeDark) mode = AppCompatDelegate.MODE_NIGHT_YES;
            
            prefs.edit().putInt("theme", mode).apply();
            AppCompatDelegate.setDefaultNightMode(mode);
        });

        String lang = LocaleHelper.getLanguage(requireContext());
        if ("en".equals(lang)) {
            ((RadioButton) view.findViewById(R.id.rbLangEn)).setChecked(true);
        } else {
            ((RadioButton) view.findViewById(R.id.rbLangTr)).setChecked(true);
        }

        rgLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            String newLang = (checkedId == R.id.rbLangEn) ? "en" : "tr";
            LocaleHelper.setLocale(requireContext(), newLang);
            
            // Restart MainActivity to apply language change
            Intent intent = new Intent(requireContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        SwitchMaterial swWifiOnly = view.findViewById(R.id.swWifiOnly);
        boolean isWifiOnly = prefs.getBoolean("download_wifi_only", true);
        swWifiOnly.setChecked(isWifiOnly);
        
        swWifiOnly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("download_wifi_only", isChecked).apply();
        });
    }

    private void showChangePasswordDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(requireContext(), R.string.error_generic, Toast.LENGTH_SHORT).show();
            return;
        }

        TextInputLayout tilCurrent = new TextInputLayout(requireContext());
        tilCurrent.setHint(getString(R.string.current_password));
        TextInputEditText etCurrent = new TextInputEditText(requireContext());
        etCurrent.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        tilCurrent.addView(etCurrent);

        TextInputLayout tilNew = new TextInputLayout(requireContext());
        tilNew.setHint(getString(R.string.new_password));
        TextInputEditText etNew = new TextInputEditText(requireContext());
        etNew.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        tilNew.addView(etNew);

        TextInputLayout tilConfirm = new TextInputLayout(requireContext());
        tilConfirm.setHint(getString(R.string.confirm_new_password));
        TextInputEditText etConfirm = new TextInputEditText(requireContext());
        etConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        tilConfirm.addView(etConfirm);

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * requireContext().getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(tilCurrent);
        container.addView(tilNew);
        container.addView(tilConfirm);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.change_password)
                .setView(container)
                .setPositiveButton(R.string.update, (dialog, which) -> {
                    String current = etCurrent.getText() != null ? etCurrent.getText().toString() : "";
                    String newer = etNew.getText() != null ? etNew.getText().toString() : "";
                    String confirm = etConfirm.getText() != null ? etConfirm.getText().toString() : "";
                    updatePasswordWithReauth(user, current, newer, confirm);
                })
                .setNeutralButton(R.string.forgot_password, (dialog, which) -> sendPasswordResetEmail(user))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updatePasswordWithReauth(FirebaseUser user, String currentPassword, String newPassword, String confirmPassword) {
        if (currentPassword.trim().isEmpty() || newPassword.trim().isEmpty() || confirmPassword.trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPassword.length() < 6) {
            Toast.makeText(requireContext(), R.string.password_too_short, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(requireContext(), R.string.passwords_not_match, Toast.LENGTH_SHORT).show();
            return;
        }

        user.reauthenticate(EmailAuthProvider.getCredential(user.getEmail(), currentPassword))
                .addOnSuccessListener(unused -> user.updatePassword(newPassword)
                        .addOnSuccessListener(v -> Toast.makeText(requireContext(), R.string.password_updated, Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(requireContext(), getString(R.string.error_with_reason, e.getMessage()), Toast.LENGTH_SHORT).show()))
                .addOnFailureListener(e -> Toast.makeText(requireContext(), R.string.current_password_incorrect, Toast.LENGTH_SHORT).show());
    }

    private void sendPasswordResetEmail(FirebaseUser user) {
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_generic, Toast.LENGTH_SHORT).show();
            return;
        }
        String email = user.getEmail().trim();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.fetchSignInMethodsForEmail(email)
                .addOnSuccessListener(result -> {
                    java.util.List<String> methods = result.getSignInMethods();
                    if (methods == null || !methods.contains(EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD)) {
                        showCreatePasswordDialogForProviderUser(user);
                        return;
                    }
                    auth.sendPasswordResetEmail(email)
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(requireContext(), R.string.reset_password_email_sent, Toast.LENGTH_LONG).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(requireContext(), getString(R.string.error_with_reason, e.getMessage()), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), getString(R.string.error_with_reason, e.getMessage()), Toast.LENGTH_SHORT).show());
    }

    private void showCreatePasswordDialogForProviderUser(FirebaseUser user) {
        Toast.makeText(requireContext(), R.string.reset_password_not_available_for_provider, Toast.LENGTH_LONG).show();

        TextInputLayout tilNew = new TextInputLayout(requireContext());
        tilNew.setHint(getString(R.string.new_password));
        TextInputEditText etNew = new TextInputEditText(requireContext());
        etNew.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        tilNew.addView(etNew);

        TextInputLayout tilConfirm = new TextInputLayout(requireContext());
        tilConfirm.setHint(getString(R.string.confirm_new_password));
        TextInputEditText etConfirm = new TextInputEditText(requireContext());
        etConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        tilConfirm.addView(etConfirm);

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * requireContext().getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(tilNew);
        container.addView(tilConfirm);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.create_password_title)
                .setMessage(R.string.create_password_message)
                .setView(container)
                .setPositiveButton(R.string.create_password_action, (dialog, which) -> {
                    String newPassword = etNew.getText() != null ? etNew.getText().toString() : "";
                    String confirmPassword = etConfirm.getText() != null ? etConfirm.getText().toString() : "";
                    createPasswordForProviderUser(user, newPassword, confirmPassword);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void createPasswordForProviderUser(FirebaseUser user, String newPassword, String confirmPassword) {
        if (newPassword.trim().isEmpty() || confirmPassword.trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPassword.length() < 6) {
            Toast.makeText(requireContext(), R.string.password_too_short, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(requireContext(), R.string.passwords_not_match, Toast.LENGTH_SHORT).show();
            return;
        }

        user.updatePassword(newPassword)
                .addOnSuccessListener(unused ->
                        Toast.makeText(requireContext(), R.string.password_created_for_provider, Toast.LENGTH_LONG).show())
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), getString(R.string.error_with_reason, e.getMessage()), Toast.LENGTH_SHORT).show());
    }
}
