package com.recipebookpro.presentation.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;

import com.recipebookpro.presentation.ui.BaseActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.textfield.TextInputLayout;
import com.recipebookpro.R;
import com.recipebookpro.domain.model.User;
import com.recipebookpro.presentation.ui.MainActivity;

/**
 * RegisterActivity -> user registration
 */
public class RegisterActivity extends BaseActivity {

    private TextInputEditText etDisplayName;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;
    private MaterialButton btnRegister;
    private CircularProgressIndicator progressIndicator;
    private MaterialTextView tvLoginLink;
    private TextInputLayout tilDisplayName;
    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextInputLayout tilConfirmPassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        applyInsetsToView(findViewById(R.id.registerRoot));

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        rootView = findViewById(R.id.registerRoot);

        etDisplayName = findViewById(R.id.etDisplayName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        progressIndicator = findViewById(R.id.progressRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);
        tilDisplayName = findViewById(R.id.tilDisplayName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        setupRealTimeValidation();

        btnRegister.setOnClickListener(v -> registerUser());
        tvLoginLink.setOnClickListener(v -> finish());

        // Handle 'Done' action on keyboard for confirm password field
        etConfirmPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                registerUser();
                return true;
            }
            return false;
        });
    }

    private void hideKeyboard() {
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        android.view.View view = getCurrentFocus();
        if (view == null) view = new android.view.View(this);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void setupRealTimeValidation() {
        TextInputLayout tilDisplayName = findViewById(R.id.tilDisplayName);

        etDisplayName.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                String name = s.toString().trim();
                if (!TextUtils.isEmpty(name) && !name.matches("^[a-zA-ZğüşıöçĞÜŞİÖÇ\\s]+$")) {
                    tilDisplayName.setError(getString(R.string.invalid_name_format));
                } else {
                    tilDisplayName.setError(null);
                }
            }
        });

        etEmail.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                String email = s.toString().trim();
                if (!TextUtils.isEmpty(email) && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    tilEmail.setError(getString(R.string.invalid_email));
                } else {
                    tilEmail.setError(null);
                }
            }
        });

        etPassword.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                String pass = s.toString().trim();
                if (!TextUtils.isEmpty(pass) && pass.length() < 6) {
                    tilPassword.setError(getString(R.string.password_too_short));
                } else {
                    tilPassword.setError(null);
                }
                // Also re-check confirm password if it's not empty
                String confirm = etConfirmPassword.getText().toString().trim();
                if (!TextUtils.isEmpty(confirm)) {
                    if (!pass.equals(confirm)) {
                        tilConfirmPassword.setError(getString(R.string.passwords_not_match));
                    } else {
                        tilConfirmPassword.setError(null);
                    }
                }
            }
        });

        etConfirmPassword.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                String confirm = s.toString().trim();
                String pass = etPassword.getText().toString().trim();
                if (!TextUtils.isEmpty(confirm) && !confirm.equals(pass)) {
                    tilConfirmPassword.setError(getString(R.string.passwords_not_match));
                } else {
                    tilConfirmPassword.setError(null);
                }
            }
        });
    }

    private void registerUser() {
        hideKeyboard();
        String displayName = etDisplayName.getText() != null ? etDisplayName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        boolean hasError = false;
        tilDisplayName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        if (TextUtils.isEmpty(displayName)) {
            tilDisplayName.setError(getString(R.string.required_field));
            hasError = true;
        }
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.required_field));
            hasError = true;
        }
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.required_field));
            hasError = true;
        }
        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.required_field));
            hasError = true;
        }

        if (hasError) return;

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.invalid_email));
            hasError = true;
        }

        if (!displayName.matches("^[a-zA-ZğüşıöçĞÜŞİÖÇ\\s]+$")) {
            tilDisplayName.setError(getString(R.string.invalid_name_format));
            hasError = true;
        }

        if (password.length() < 6) {
            tilPassword.setError(getString(R.string.password_too_short));
            hasError = true;
        }

        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.passwords_not_match));
            hasError = true;
        }

        if (hasError) return;

        setLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            saveUserToFirestore(firebaseUser.getUid(), email, displayName);
                        } else {
                            setLoading(false);
                            showMessage(R.string.register_failed);
                        }
                    } else {
                        setLoading(false);
                        showMessage(R.string.register_failed);
                    }
                });
    }

    private void saveUserToFirestore(String uid, String email, String displayName) {
        User user = new User(uid, email, displayName, System.currentTimeMillis());

        db.collection("users").document(uid).set(user)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finishAffinity();
                    } else {
                        showMessage(R.string.register_failed);
                    }
                });
    }

    private void setLoading(boolean isLoading) {
        progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!isLoading);
    }

    private void showMessage(int messageRes) {
        Snackbar.make(rootView, messageRes, Snackbar.LENGTH_SHORT).show();
    }
}
