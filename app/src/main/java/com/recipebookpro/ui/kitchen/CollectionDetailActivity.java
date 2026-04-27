package com.recipebookpro.ui.kitchen;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.recipebookpro.R;
import com.recipebookpro.model.Recipe;
import com.recipebookpro.model.RecipeCollection;
import com.recipebookpro.ui.BaseActivity;
import com.recipebookpro.adapter.RecipeAdapter;
import com.recipebookpro.ui.recipe.RecipeDetailActivity;

import java.util.ArrayList;
import java.util.List;

public class CollectionDetailActivity extends BaseActivity {

    public static final String EXTRA_COLLECTION_ID = "collection_id";

    private String collectionId;
    private RecipeCollection collection;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private MaterialToolbar toolbar;
    private RecyclerView rvRecipes;
    private ProgressBar progress;
    private TextView tvEmpty;

    private RecipeAdapter adapter;
    private List<Recipe> recipeList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection_detail);

        applyInsetsToView(findViewById(R.id.collectionDetailRoot));

        collectionId = getIntent().getStringExtra(EXTRA_COLLECTION_ID);
        if (TextUtils.isEmpty(collectionId)) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        initViews();
        loadCollection();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarCollectionDetail);
        rvRecipes = findViewById(R.id.rvCollectionRecipes);
        progress = findViewById(R.id.progressCollectionDetail);
        tvEmpty = findViewById(R.id.tvCollectionEmpty);

        toolbar.setNavigationOnClickListener(v -> finish());

        rvRecipes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecipeAdapter(recipe -> {
            Intent intent = new Intent(this, RecipeDetailActivity.class);
            intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE, recipe);
            startActivity(intent);
        });
        rvRecipes.setAdapter(adapter);
    }

    private void loadCollection() {
        progress.setVisibility(View.VISIBLE);
        db.collection("collections").document(collectionId).addSnapshotListener((doc, e) -> {
            if (e != null || doc == null || !doc.exists()) {
                progress.setVisibility(View.GONE);
                Toast.makeText(this, R.string.collection_not_found, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            collection = RecipeCollection.fromDocument(doc);
            toolbar.setTitle(collection.getEmoji() + " " + collection.getName());
            loadRecipes();
        });
    }

    private void loadRecipes() {
        List<String> ids = collection.getRecipeIds();
        if (ids == null || ids.isEmpty()) {
            recipeList.clear();
            adapter.notifyDataSetChanged();
            tvEmpty.setVisibility(View.VISIBLE);
            progress.setVisibility(View.GONE);
            return;
        }

        tvEmpty.setVisibility(View.GONE);

        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        List<List<String>> chunks = partition(ids, 10);

        for (List<String> chunk : chunks) {
            tasks.add(db.collection("recipes")
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk).get());
        }

        Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            recipeList.clear();
            for (Object result : results) {
                QuerySnapshot snap = (QuerySnapshot) result;
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    recipeList.add(Recipe.fromDocument(doc));
                }
            }
            adapter.setRecipeList(recipeList);
            progress.setVisibility(View.GONE);
        }).addOnFailureListener(e -> progress.setVisibility(View.GONE));
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(new ArrayList<>(list.subList(i, Math.min(list.size(), i + size))));
        }
        return partitions;
    }
}
