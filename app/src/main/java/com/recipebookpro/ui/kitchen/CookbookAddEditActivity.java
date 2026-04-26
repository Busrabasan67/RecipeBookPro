package com.recipebookpro.ui.kitchen;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.recipebookpro.R;
import com.recipebookpro.model.Cookbook;
import com.recipebookpro.ui.BaseActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import coil.Coil;
import coil.request.ImageRequest;

public class CookbookAddEditActivity extends BaseActivity {

    public static final String EXTRA_COOKBOOK_ID = "cookbook_id";

    private TextInputEditText etTitle, etDescription, etTagInput;
    private MaterialSwitch switchPublic;
    private ImageView ivCover;
    private ChipGroup chipGroupTags;
    private MaterialButton btnSave;
    private MaterialToolbar toolbar;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private Uri selectedImageUri;
    private List<String> tags = new ArrayList<>();

    private String editCookbookId;
    private Cookbook existingCookbook;
    private boolean isEditMode = false;

    private final ActivityResultLauncher<String> getContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    ivCover.setPadding(0, 0, 0, 0);
                    ImageRequest request = new ImageRequest.Builder(this)
                            .data(uri)
                            .target(ivCover)
                            .build();
                    Coil.imageLoader(this).enqueue(request);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cookbook_add_edit);

        applyInsetsToView(findViewById(R.id.cookbookAddEditRoot));

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        editCookbookId = getIntent().getStringExtra(EXTRA_COOKBOOK_ID);
        isEditMode = !TextUtils.isEmpty(editCookbookId);

        initViews();

        if (isEditMode) {
            loadExistingCookbook();
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarCookbookAddEdit);
        toolbar.setNavigationOnClickListener(v -> finish());

        etTitle = findViewById(R.id.etCookbookTitle);
        etDescription = findViewById(R.id.etCookbookDescription);
        etTagInput = findViewById(R.id.etTagInput);
        switchPublic = findViewById(R.id.switchPublic);
        ivCover = findViewById(R.id.ivCookbookCoverEdit);
        chipGroupTags = findViewById(R.id.chipGroupTags);
        btnSave = findViewById(R.id.btnSaveCookbook);
        MaterialCardView cardCover = findViewById(R.id.cardCoverImage);

        if (isEditMode) {
            toolbar.setTitle("Defteri Düzenle");
            btnSave.setText("Güncelle");
        }

        cardCover.setOnClickListener(v -> getContent.launch("image/*"));

        etTagInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
               (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String tag = etTagInput.getText().toString().trim();
                if (!tag.isEmpty() && !tags.contains(tag)) {
                    addTagChip(tag);
                    etTagInput.setText("");
                }
                return true;
            }
            return false;
        });

        btnSave.setOnClickListener(v -> saveCookbook());
    }

    private void loadExistingCookbook() {
        btnSave.setEnabled(false);
        db.collection("cookbooks").document(editCookbookId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        existingCookbook = Cookbook.fromDocument(doc);
                        populateForm();
                    } else {
                        Toast.makeText(this, "Defter bulunamadı", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    btnSave.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Yükleme hatası", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void populateForm() {
        if (existingCookbook == null) return;

        etTitle.setText(existingCookbook.getName());
        etDescription.setText(existingCookbook.getDescription());
        switchPublic.setChecked(existingCookbook.isPublic());

        if (!TextUtils.isEmpty(existingCookbook.getCoverImageUrl())) {
            ivCover.setPadding(0, 0, 0, 0);
            ImageRequest request = new ImageRequest.Builder(this)
                    .data(existingCookbook.getCoverImageUrl())
                    .target(ivCover)
                    .build();
            Coil.imageLoader(this).enqueue(request);
        }

        if (existingCookbook.getTags() != null) {
            for (String tag : existingCookbook.getTags()) {
                addTagChip(tag);
            }
        }
    }

    private void addTagChip(String tag) {
        tags.add(tag);
        Chip chip = new Chip(this);
        chip.setText(tag);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            chipGroupTags.removeView(chip);
            tags.remove(tag);
        });
        chipGroupTags.addView(chip);
    }

    private void saveCookbook() {
        String title = etTitle.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Zorunlu alan");
            return;
        }

        btnSave.setEnabled(false);

        if (selectedImageUri != null) {
            uploadImageAndSave(title, desc);
        } else {
            String existingImage = (isEditMode && existingCookbook != null)
                    ? existingCookbook.getCoverImageUrl() : "";
            saveToFirestore(title, desc, existingImage);
        }
    }

    private void uploadImageAndSave(String title, String desc) {
        String filename = "cookbooks/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference ref = FirebaseStorage.getInstance().getReference().child(filename);

        ref.putFile(selectedImageUri)
           .continueWithTask(task -> {
               if (!task.isSuccessful()) {
                   Exception ex = task.getException();
                   if (ex != null) throw ex;
                   throw new Exception("Upload failed");
               }
               return ref.getDownloadUrl();
           })
           .addOnSuccessListener(uri -> saveToFirestore(title, desc, uri.toString()))
           .addOnFailureListener(e -> {
               String existingImage = (isEditMode && existingCookbook != null)
                       ? existingCookbook.getCoverImageUrl() : "";
               Toast.makeText(this,
                       "Resim yüklenemedi, kayıt resimsiz devam ediyor",
                       Toast.LENGTH_LONG).show();
               saveToFirestore(title, desc, existingImage);
           });
    }

    private void saveToFirestore(String title, String desc, String imageUrl) {
        if (isEditMode && existingCookbook != null) {
            existingCookbook.setName(title);
            existingCookbook.setDescription(desc);
            existingCookbook.setCoverImageUrl(imageUrl);
            existingCookbook.setPublic(switchPublic.isChecked());
            existingCookbook.setTags(tags);

            db.collection("cookbooks").document(editCookbookId).set(existingCookbook)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Defter güncellendi", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Güncelleme hatası", Toast.LENGTH_SHORT).show();
                        btnSave.setEnabled(true);
                    });
        } else {
            Cookbook book = new Cookbook(currentUser.getUid(), title);
            book.setDescription(desc);
            book.setCoverImageUrl(imageUrl);
            book.setPublic(switchPublic.isChecked());
            book.setTags(tags);

            db.collection("cookbooks").add(book)
                    .addOnSuccessListener(ref -> {
                        Toast.makeText(this, "Defter kaydedildi", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Kayıt hatası", Toast.LENGTH_SHORT).show();
                        btnSave.setEnabled(true);
                    });
        }
    }
}
