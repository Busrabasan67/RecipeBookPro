package com.recipebookpro.ui.recipe;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.recipebookpro.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "recipe_id";
    public static final String EXTRA_RECIPE_TITLE = "recipe_title";
    public static final String EXTRA_RECIPE_DESC = "recipe_desc";
    public static final String EXTRA_RECIPE_INGREDIENTS = "recipe_ingredients";
    public static final String EXTRA_RECIPE_STEPS = "recipe_steps";
    public static final String EXTRA_RECIPE_DATE = "recipe_date";

    private MaterialTextView tvTitle, tvDescription, tvIngredients, tvSteps, tvDate;
    private MaterialButton btnDelete;
    private FirebaseFirestore db;
    private String recipeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        db = FirebaseFirestore.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbarDetail);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvTitle = findViewById(R.id.tvDetailTitle);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvIngredients = findViewById(R.id.tvDetailIngredients);
        tvSteps = findViewById(R.id.tvDetailSteps);
        tvDate = findViewById(R.id.tvDetailDate);
        btnDelete = findViewById(R.id.btnDeleteRecipe);

        recipeId = getIntent().getStringExtra(EXTRA_RECIPE_ID);
        String title = getIntent().getStringExtra(EXTRA_RECIPE_TITLE);
        String desc = getIntent().getStringExtra(EXTRA_RECIPE_DESC);
        String ingredients = getIntent().getStringExtra(EXTRA_RECIPE_INGREDIENTS);
        String steps = getIntent().getStringExtra(EXTRA_RECIPE_STEPS);
        long date = getIntent().getLongExtra(EXTRA_RECIPE_DATE, 0);

        tvTitle.setText(title != null ? title : "");
        tvDescription.setText(desc != null ? desc : "");
        tvIngredients.setText(ingredients != null ? ingredients : "");
        tvSteps.setText(steps != null ? steps : "");

        if (date > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            tvDate.setText(sdf.format(new Date(date)));
        }

        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete)
                .setMessage(R.string.confirm_delete)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteRecipe())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteRecipe() {
        if (recipeId == null) return;

        btnDelete.setEnabled(false);
        db.collection("recipes").document(recipeId).delete()
                .addOnCompleteListener(task -> {
                    btnDelete.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, R.string.recipe_deleted, Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, R.string.recipe_delete_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
