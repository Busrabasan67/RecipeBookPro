package com.recipebookpro.ui.recipe;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.recipebookpro.R;
import com.recipebookpro.model.Recipe;

public class AddRecipeActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etDescription, etIngredients, etSteps;
    private MaterialButton btnSave;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbarAddRecipe);
        toolbar.setNavigationOnClickListener(v -> finish());

        etTitle = findViewById(R.id.etRecipeTitle);
        etDescription = findViewById(R.id.etRecipeDescription);
        etIngredients = findViewById(R.id.etRecipeIngredients);
        etSteps = findViewById(R.id.etRecipeSteps);
        btnSave = findViewById(R.id.btnSaveRecipe);

        btnSave.setOnClickListener(v -> saveRecipe());
    }

    private void saveRecipe() {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        String ingredients = etIngredients.getText() != null ? etIngredients.getText().toString().trim() : "";
        String steps = etSteps.getText() != null ? etSteps.getText().toString().trim() : "";

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(ingredients) || TextUtils.isEmpty(steps)) {
            Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, R.string.login_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);

        String docId = db.collection("recipes").document().getId();
        Recipe recipe = new Recipe(docId, currentUser.getUid(), title, description,
                ingredients, steps, System.currentTimeMillis());

        db.collection("recipes").document(docId).set(recipe)
                .addOnCompleteListener(task -> {
                    btnSave.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, R.string.recipe_saved, Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, R.string.recipe_save_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
