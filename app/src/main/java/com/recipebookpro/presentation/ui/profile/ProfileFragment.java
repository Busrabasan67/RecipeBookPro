package com.recipebookpro.presentation.ui.profile;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.core.widget.NestedScrollView;
import android.graphics.Rect;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.recipebookpro.R;
import com.recipebookpro.domain.model.User;
import com.recipebookpro.presentation.ui.auth.LoginActivity;

import coil.Coil;
import coil.request.ImageRequest;
import coil.transform.CircleCropTransformation;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ChipGroup chipGroupAllergens;
    private TextInputEditText etCustomAllergen;
    private ChipGroup chipGroupHealthConditions;
    private ChipGroup chipGroupCustomHealthConditions;
    private TextInputEditText etCustomHealthCondition;
    private ImageView ivProfileAvatar;
    private List<String> userAllergens = new ArrayList<>();
    private List<String> userHealthConditions = new ArrayList<>();
    private List<String> userCustomHealthConditions = new ArrayList<>();
    private java.util.Map<String, String> customAllergenTranslations = new java.util.HashMap<>();
    private boolean isLoading = true;
    private Uri cameraImageUri;

    private static final java.util.Map<Integer, String> CHIP_CONDITION_MAP = new java.util.LinkedHashMap<>();
    static {
        CHIP_CONDITION_MAP.put(R.id.chipDiabetes, "diabetes");
        CHIP_CONDITION_MAP.put(R.id.chipKidney, "kidney_disease");
        CHIP_CONDITION_MAP.put(R.id.chipCardiovascular, "cardiovascular");
        CHIP_CONDITION_MAP.put(R.id.chipHypertension, "hypertension");
        CHIP_CONDITION_MAP.put(R.id.chipCeliac, "celiac");
        CHIP_CONDITION_MAP.put(R.id.chipIbs, "ibs");
    }

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean cameraGranted = Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));
                String galleryPermission = getGalleryPermission();
                boolean galleryGranted = galleryPermission == null
                        || Boolean.TRUE.equals(result.get(galleryPermission));

                if (cameraGranted && galleryGranted) {
                    showImageSourceChooser();
                    return;
                }

                if (!cameraGranted) {
                    Toast.makeText(requireContext(), R.string.permission_denied_camera, Toast.LENGTH_SHORT).show();
                }
                if (!galleryGranted) {
                    Toast.makeText(requireContext(), R.string.permission_denied_gallery, Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadProfileImage(uri);
                }
            }
    );

    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success && cameraImageUri != null) {
                    uploadProfileImage(cameraImageUri);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        MaterialTextView tvEmail = view.findViewById(R.id.tvProfileEmail);
        MaterialButton btnLogout = view.findViewById(R.id.btnLogout);
        MaterialButton btnSettings = view.findViewById(R.id.btnSettings);

        chipGroupAllergens = view.findViewById(R.id.chipGroupAllergens);
        etCustomAllergen = view.findViewById(R.id.etCustomAllergen);
        chipGroupHealthConditions = view.findViewById(R.id.chipGroupHealthConditions);
        chipGroupCustomHealthConditions = view.findViewById(R.id.chipGroupCustomHealthConditions);
        etCustomHealthCondition = view.findViewById(R.id.etCustomHealthCondition);
        ivProfileAvatar = view.findViewById(R.id.ivProfileAvatar);

        etCustomAllergen.setOnEditorActionListener((v, actionId, event) -> {
            boolean isDone = actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_GO
                    || actionId == EditorInfo.IME_NULL;
            boolean isEnterDown = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            boolean isEnterUp = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_UP;

            if (isDone || isEnterDown) {
                String custom = etCustomAllergen.getText() != null
                        ? etCustomAllergen.getText().toString().trim() : "";
                if (!custom.isEmpty()) {
                    addCustomAllergenChip(custom);
                    etCustomAllergen.setText("");
                }
                // Hide keyboard and clear focus so it doesn't jump to next field
                etCustomAllergen.clearFocus();
                android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager)
                                requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(etCustomAllergen.getWindowToken(), 0);
                return true;
            }
            // Consume UP event for Enter key too, to avoid double-trigger
            return isEnterUp;
        });


        etCustomHealthCondition.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String custom = etCustomHealthCondition.getText() != null
                        ? etCustomHealthCondition.getText().toString().trim() : "";
                if (!custom.isEmpty()) {
                    addCustomHealthConditionChip(custom);
                    etCustomHealthCondition.setText("");
                }
                return true;
            }
            return false;
        });

        for (java.util.Map.Entry<Integer, String> entry : CHIP_CONDITION_MAP.entrySet()) {
            Chip chip = view.findViewById(entry.getKey());
            if (chip != null) {
                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (!isLoading) {
                        onHealthConditionChanged();
                    }
                });
            }
        }

        if (currentUser != null) {
            tvEmail.setText(currentUser.getEmail());
            loadProfileImage(currentUser.getPhotoUrl());
        }

        ivProfileAvatar.setOnClickListener(v -> ensureImagePermissionsAndSelectSource());

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finishAffinity();
        });

        btnSettings.setOnClickListener(v -> {
            SettingsBottomSheet bottomSheet = new SettingsBottomSheet();
            bottomSheet.show(getChildFragmentManager(), "SettingsBottomSheet");
        });


        loadUserAllergens();

        NestedScrollView nsvProfile = view.findViewById(R.id.nsvProfile);

        // Robust keyboard detection using spacer
        final View keyboardSpacer = view.findViewById(R.id.keyboardSpacer);
        view.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (!isAdded()) return;
            Rect r = new Rect();
            view.getWindowVisibleDisplayFrame(r);
            int screenHeight = view.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;

            if (keyboardSpacer != null) {
                android.view.ViewGroup.LayoutParams params = keyboardSpacer.getLayoutParams();
                if (keypadHeight > screenHeight * 0.15) {
                    params.height = keypadHeight;
                } else {
                    params.height = 0;
                }
                keyboardSpacer.setLayoutParams(params);
            }
        });

        // Focus listener to scroll to focused field
        nsvProfile.getViewTreeObserver().addOnGlobalFocusChangeListener((oldFocus, newFocus) -> {
            if (newFocus != null && (newFocus instanceof android.widget.EditText || newFocus instanceof android.widget.AutoCompleteTextView)) {
                nsvProfile.postDelayed(() -> {
                    if (!isAdded()) return;
                    int[] viewPos = new int[2];
                    newFocus.getLocationOnScreen(viewPos);
                    int[] scrollPos = new int[2];
                    nsvProfile.getLocationOnScreen(scrollPos);
                    // Position the view about 300px below the top (more centered)
                    int relativeTop = viewPos[1] - scrollPos[1];
                    nsvProfile.smoothScrollBy(0, relativeTop - 50);
                }, 200);
            }
        });
    }

    private void loadUserAllergens() {
        if (currentUser == null) return;

        final String uid = currentUser.getUid();
        final com.recipebookpro.data.local.AppDatabase localDb = com.recipebookpro.data.local.AppDatabase.getDatabase(requireContext());

        // 1. Load from Room (Local) first on a background thread
        new Thread(() -> {
            com.recipebookpro.data.local.entity.UserEntity localUser = localDb.userDao().getUserByUid(uid);
            if (localUser != null && isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    userAllergens = new ArrayList<>(localUser.getAllergens());
                    userHealthConditions = localUser.getHealthConditions() != null ? new ArrayList<>(localUser.getHealthConditions()) : new ArrayList<>();
                    userCustomHealthConditions = localUser.getCustomHealthConditions() != null ? new ArrayList<>(localUser.getCustomHealthConditions()) : new ArrayList<>();
                    translateAndPopulateAllergenChips();
                    populateHealthConditionChips();
                });
            }
            
            // 2. Fetch from Firestore (Remote) and update Room
            // We don't need to be in the thread for this as Firestore is async
            requireActivity().runOnUiThread(() -> {
                db.collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            User user = doc.toObject(User.class);
                            if (user != null) {
                                // Sync profile image if missing in Firestore but present in Auth (Google)
                                if (TextUtils.isEmpty(user.getProfileImageUrl()) && currentUser.getPhotoUrl() != null) {
                                    String authPhoto = currentUser.getPhotoUrl().toString();
                                    user.setProfileImageUrl(authPhoto);
                                    db.collection("users").document(uid).update("profileImageUrl", authPhoto);
                                }

                                userAllergens = new ArrayList<>(user.getAllergens());
                                userHealthConditions = user.getHealthConditions() != null ? new ArrayList<>(user.getHealthConditions()) : new ArrayList<>();
                                userCustomHealthConditions = user.getCustomHealthConditions() != null ? new ArrayList<>(user.getCustomHealthConditions()) : new ArrayList<>();

                                // Load saved custom allergen translations
                                Object transObj = doc.get("customAllergenTranslations");
                                if (transObj instanceof java.util.Map<?, ?>) {
                                    for (java.util.Map.Entry<?, ?> e : ((java.util.Map<?, ?>) transObj).entrySet()) {
                                        if (e.getKey() instanceof String && e.getValue() instanceof String) {
                                            customAllergenTranslations.put((String) e.getKey(), (String) e.getValue());
                                        }
                                    }
                                }

                                translateAndPopulateAllergenChips();
                                populateHealthConditionChips();
                                
                                // Update Room in background
                                final User finalUser = user;
                                new Thread(() -> {
                                    localDb.userDao().insertUser(new com.recipebookpro.data.local.entity.UserEntity(
                                        uid, finalUser.getEmail(), finalUser.getDisplayName(), 
                                        finalUser.getProfileImageUrl(), finalUser.getAllergens(),
                                        finalUser.getHealthConditions(), finalUser.getCustomHealthConditions()
                                    ));
                                }).start();
                            }
                        }
                    });
            });
        }).start();
    }

    private void translateAndPopulateAllergenChips() {
        if (userAllergens == null || userAllergens.isEmpty() || !isAdded()) {
            populateAllergenChips();
            return;
        }

        // Draw immediately so the screen is not empty while ML Kit downloads models or translates
        populateAllergenChips();

        String targetLang = com.recipebookpro.presentation.ui.LocaleHelper.getLanguage(requireContext());
        String joinedText = TextUtils.join(" ", userAllergens).toLowerCase();
        
        com.google.mlkit.nl.languageid.LanguageIdentifier identifier = com.google.mlkit.nl.languageid.LanguageIdentification.getClient();
        identifier.identifyLanguage(joinedText)
                .addOnSuccessListener(sourceLang -> {
                    String finalSource = sourceLang;
                    if (sourceLang.equals("und")) {
                        int enCount = 0;
                        int trCount = 0;
                        if (joinedText.matches(".*\\b(dairy|gluten|egg|peanut|soy|fish)\\b.*")) enCount++;
                        if (joinedText.matches(".*\\b(süt|glüten|yumurta|fıstık|soya|balık)\\b.*")) trCount++;
                        if (enCount > trCount) finalSource = "en";
                        else if (trCount > enCount) finalSource = "tr";
                        else finalSource = "tr"; // default to tr
                    }
                    
                    if (!finalSource.equalsIgnoreCase(targetLang)) {
                        performAllergenTranslation(finalSource, targetLang);
                    }
                });
    }

    private void performAllergenTranslation(String sourceLang, String targetLang) {
        if (!isAdded()) return;
        com.recipebookpro.data.remote.MLKitTranslationService translationService = new com.recipebookpro.data.remote.MLKitTranslationService(requireContext());
        translationService.prepareModel(sourceLang, targetLang)
                .addOnSuccessListener(unused -> {
                    java.util.List<com.google.android.gms.tasks.Task<String>> tasks = new java.util.ArrayList<>();
                    for (String allergen : userAllergens) {
                        tasks.add(translationService.translateSingleField(allergen, sourceLang, targetLang));
                    }
                    com.google.android.gms.tasks.Tasks.whenAllComplete(tasks).addOnCompleteListener(allTasks -> {
                        if (allTasks.isSuccessful() && isAdded()) {
                            java.util.List<String> translatedList = new java.util.ArrayList<>();
                            for (com.google.android.gms.tasks.Task<String> task : tasks) {
                                if (task.isSuccessful() && task.getResult() != null) {
                                    translatedList.add(task.getResult().trim());
                                }
                            }
                            if (translatedList.size() == userAllergens.size()) {
                                userAllergens = translatedList;
                                updateAllergensInDatabase(userAllergens);
                            }
                            populateAllergenChips();
                        }
                        translationService.close();
                    });
                })
                .addOnFailureListener(e -> {
                    translationService.close();
                });
    }

    private void populateAllergenChips() {
        chipGroupAllergens.removeAllViews();
        String[] allergenTags = getResources().getStringArray(R.array.allergen_tags);

        List<String> defaultTags = new ArrayList<>();
        for (String t : allergenTags) defaultTags.add(t.toLowerCase());

        isLoading = true;
        for (String tag : allergenTags) {
            addAllergenChipInternal(tag, false);
        }

        for (String userAllergen : userAllergens) {
            boolean isDefault = false;
            for (String def : allergenTags) {
                if (def.equalsIgnoreCase(userAllergen)) { isDefault = true; break; }
            }
            if (!isDefault) {
                addAllergenChipInternal(userAllergen, true);
            }
        }
        isLoading = false;
    }

    private void addAllergenChipInternal(String tag, boolean isCustom) {
        Chip chip = new Chip(requireContext());
        chip.setText(tag);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(true);
        if (isCustom) {
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                chipGroupAllergens.removeView(chip);
                onAllergenChanged();
            });
        }

        boolean isSelected = false;
        for (String userAllergen : userAllergens) {
            if (userAllergen.equalsIgnoreCase(tag)) {
                isSelected = true;
                break;
            }
        }
        chip.setChecked(isSelected);

        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isLoading) {
                onAllergenChanged();
            }
        });

        chipGroupAllergens.addView(chip);
    }

    private void addCustomAllergenChip(String text) {
        for (int i = 0; i < chipGroupAllergens.getChildCount(); i++) {
            Chip existing = (Chip) chipGroupAllergens.getChildAt(i);
            if (existing.getText().toString().equalsIgnoreCase(text)) return;
        }

        isLoading = true;
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(true);
        chip.setChecked(true);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            chipGroupAllergens.removeView(chip);
            onAllergenChanged();
        });
        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isLoading) {
                onAllergenChanged();
            }
        });
        chipGroupAllergens.addView(chip);
        isLoading = false;
        onAllergenChanged();
        // Detect the language of this custom allergen and save a cross-language translation
        saveCustomAllergenTranslation(text);
    }

    /**
     * Uses ML Kit to detect the language of a custom allergen and translate it to the
     * opposite language (TR⇔EN). Both the original and the translation are persisted in
     * Firestore so that allergen matching works across language switches.
     */
    private void saveCustomAllergenTranslation(String original) {
        if (currentUser == null || !isAdded()) return;
        com.google.mlkit.nl.languageid.LanguageIdentifier identifier =
                com.google.mlkit.nl.languageid.LanguageIdentification.getClient();
        identifier.identifyLanguage(original.toLowerCase())
                .addOnSuccessListener(sourceLang -> {
                    String detectedLang = sourceLang.equals("und") ? "tr" : sourceLang;
                    String targetLang = detectedLang.equals("tr") ? "en" : "tr";

                    com.recipebookpro.data.remote.MLKitTranslationService svc =
                            new com.recipebookpro.data.remote.MLKitTranslationService(requireContext());
                    svc.prepareModel(detectedLang, targetLang)
                            .addOnSuccessListener(unused -> {
                                svc.translateSingleField(original, detectedLang, targetLang)
                                        .addOnSuccessListener(translated -> {
                                            if (translated == null || translated.trim().isEmpty()) return;
                                            String translatedClean = translated.trim();
                                            customAllergenTranslations.put(original, translatedClean);
                                            // Also store reverse so it's bidirectional
                                            customAllergenTranslations.put(translatedClean, original);
                                            // Persist to Firestore
                                            db.collection("users").document(currentUser.getUid())
                                                    .update("customAllergenTranslations",
                                                            customAllergenTranslations);
                                            svc.close();
                                        })
                                        .addOnFailureListener(e -> svc.close());
                            })
                            .addOnFailureListener(e -> svc.close());
                });
    }

    private void onAllergenChanged() {
        if (currentUser == null) return;

        List<String> selected = new ArrayList<>();
        for (int i = 0; i < chipGroupAllergens.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupAllergens.getChildAt(i);
            if (chip.isChecked()) {
                selected.add(chip.getText().toString());
            }
        }

        userAllergens = selected;
        updateAllergensInDatabase(selected);
    }

    private void updateAllergensInDatabase(List<String> selected) {
        if (currentUser == null) return;
        
        db.collection("users").document(currentUser.getUid())
                .update("allergens", selected);

        // Update Room
        final String uid = currentUser.getUid();
        final List<String> finalSelected = new ArrayList<>(selected);
        new Thread(() -> {
            com.recipebookpro.data.local.AppDatabase localDb = com.recipebookpro.data.local.AppDatabase.getDatabase(requireContext());
            com.recipebookpro.data.local.entity.UserEntity entity = localDb.userDao().getUserByUid(uid);
            if (entity != null) {
                entity.setAllergens(finalSelected);
                entity.setLastUpdated(System.currentTimeMillis());
                localDb.userDao().insertUser(entity);
            }
        }).start();
    }

    private void uploadProfileImage(Uri imageUri) {
        if (currentUser == null) return;
        Toast.makeText(requireContext(), R.string.loading, Toast.LENGTH_SHORT).show();

        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("profile_images/" + currentUser.getUid() + ".jpg");

        storageRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                // Update Firebase Auth profile
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setPhotoUri(uri)
                        .build();
                currentUser.updateProfile(profileUpdates);

                // Update Firestore user document
                db.collection("users").document(currentUser.getUid())
                        .update("profileImageUrl", downloadUrl)
                        .addOnSuccessListener(aVoid -> {
                            loadProfileImage(uri);
                            Toast.makeText(requireContext(), R.string.profile_image_updated, Toast.LENGTH_SHORT).show();
                        });
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(requireContext(), R.string.upload_failed, Toast.LENGTH_SHORT).show();
        });
    }

    private void loadProfileImage(Uri photoUrl) {
        if (photoUrl != null && ivProfileAvatar != null) {
            ImageRequest request = new ImageRequest.Builder(requireContext())
                    .data(photoUrl.toString())
                    .target(ivProfileAvatar)
                    .crossfade(true)
                    .transformations(new CircleCropTransformation())
                    .placeholder(R.drawable.ic_nav_profile)
                    .build();
            Coil.imageLoader(requireContext()).enqueue(request);
        }
    }

    private void ensureImagePermissionsAndSelectSource() {
        String galleryPermission = getGalleryPermission();
        boolean cameraGranted = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean galleryGranted = galleryPermission == null || ContextCompat.checkSelfPermission(
                requireContext(), galleryPermission) == PackageManager.PERMISSION_GRANTED;

        if (cameraGranted && galleryGranted) {
            showImageSourceChooser();
            return;
        }

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.permission_required)
                .setMessage(R.string.camera_permission_rationale)
                .setPositiveButton(R.string.accept, (dialog, which) -> {
                    List<String> permissionsToRequest = new ArrayList<>();
                    if (!cameraGranted) permissionsToRequest.add(Manifest.permission.CAMERA);
                    if (!galleryGranted && galleryPermission != null) permissionsToRequest.add(galleryPermission);
                    permissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private String getGalleryPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_IMAGES;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        return null;
    }

    private void showImageSourceChooser() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_photo_select_source)
                .setItems(new String[]{
                        getString(R.string.take_photo),
                        getString(R.string.choose_from_gallery)
                }, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else {
                        imagePickerLauncher.launch("image/*");
                    }
                })
                .show();
    }

    private void openCamera() {
        try {
            java.io.File photoFile = java.io.File.createTempFile(
                    "profile_",
                    ".jpg",
                    requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            );
            cameraImageUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".provider",
                    photoFile
            );
            cameraLauncher.launch(cameraImageUri);
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.camera_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void populateHealthConditionChips() {
        isLoading = true;
        
        if (getView() != null) {
            for (java.util.Map.Entry<Integer, String> entry : CHIP_CONDITION_MAP.entrySet()) {
                Chip chip = getView().findViewById(entry.getKey());
                if (chip != null) {
                    chip.setChecked(userHealthConditions.contains(entry.getValue()));
                }
            }
        }

        if (chipGroupCustomHealthConditions != null) {
            chipGroupCustomHealthConditions.removeAllViews();
            for (String condition : userCustomHealthConditions) {
                addCustomHealthConditionChipInternal(condition);
            }
        }
        
        isLoading = false;
    }

    private void addCustomHealthConditionChipInternal(String text) {
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setCheckable(false);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            chipGroupCustomHealthConditions.removeView(chip);
            onHealthConditionChanged();
        });
        chipGroupCustomHealthConditions.addView(chip);
    }

    private void addCustomHealthConditionChip(String text) {
        for (int i = 0; i < chipGroupCustomHealthConditions.getChildCount(); i++) {
            Chip existing = (Chip) chipGroupCustomHealthConditions.getChildAt(i);
            if (existing.getText().toString().equalsIgnoreCase(text)) return;
        }

        isLoading = true;
        addCustomHealthConditionChipInternal(text);
        isLoading = false;
        onHealthConditionChanged();
    }

    private void onHealthConditionChanged() {
        if (currentUser == null) return;

        List<String> conditions = collectHealthConditions();
        List<String> customConditions = collectCustomHealthConditions();

        userHealthConditions = conditions;
        userCustomHealthConditions = customConditions;

        updateHealthConditionsInDatabase(conditions, customConditions);
    }

    private List<String> collectHealthConditions() {
        List<String> result = new ArrayList<>();
        if (getView() == null) return result;
        for (java.util.Map.Entry<Integer, String> entry : CHIP_CONDITION_MAP.entrySet()) {
            Chip chip = getView().findViewById(entry.getKey());
            if (chip != null && chip.isChecked()) {
                result.add(entry.getValue());
            }
        }
        return result;
    }

    private List<String> collectCustomHealthConditions() {
        List<String> result = new ArrayList<>();
        if (chipGroupCustomHealthConditions == null) return result;
        for (int i = 0; i < chipGroupCustomHealthConditions.getChildCount(); i++) {
            Chip c = (Chip) chipGroupCustomHealthConditions.getChildAt(i);
            result.add(c.getText().toString());
        }
        return result;
    }

    private void updateHealthConditionsInDatabase(List<String> conditions, List<String> customConditions) {
        if (currentUser == null) return;

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("healthConditions", conditions);
        updates.put("customHealthConditions", customConditions);

        db.collection("users").document(currentUser.getUid()).update(updates);

        final String uid = currentUser.getUid();
        final List<String> finalConditions = new ArrayList<>(conditions);
        final List<String> finalCustomConditions = new ArrayList<>(customConditions);
        new Thread(() -> {
            com.recipebookpro.data.local.AppDatabase localDb = com.recipebookpro.data.local.AppDatabase.getDatabase(requireContext());
            com.recipebookpro.data.local.entity.UserEntity entity = localDb.userDao().getUserByUid(uid);
            if (entity != null) {
                entity.setHealthConditions(finalConditions);
                entity.setCustomHealthConditions(finalCustomConditions);
                entity.setLastUpdated(System.currentTimeMillis());
                localDb.userDao().insertUser(entity);
            }
        }).start();
    }
}
