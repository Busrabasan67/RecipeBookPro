package com.recipebookpro.presentation.ui.discover;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.recipebookpro.R;
import com.recipebookpro.domain.model.Cookbook;
import com.recipebookpro.presentation.ui.BaseActivity;
import com.recipebookpro.presentation.ui.kitchen.CookbookDetailActivity;
import com.recipebookpro.presentation.ui.kitchen.adapter.CookbookAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PublicCookbooksActivity extends BaseActivity {

    private RecyclerView rvCookbooks;
    private CookbookAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupSort;

    private List<Cookbook> allCookbooks = new ArrayList<>();
    private List<Cookbook> filteredCookbooks = new ArrayList<>();
    private String currentSearch = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_cookbooks);

        applyTopInsetToView(findViewById(R.id.appBarPublicCookbooks));
        applyBottomInsetToView(findViewById(R.id.rvPublicCookbooksAll));

        initViews();
        loadPublicCookbooks();
    }

    private void initViews() {
        rvCookbooks = findViewById(R.id.rvPublicCookbooksAll);
        progressBar = findViewById(R.id.progressCookbooks);
        tvEmpty = findViewById(R.id.tvEmptyCookbooks);
        etSearch = findViewById(R.id.etSearchCookbooks);
        chipGroupSort = findViewById(R.id.chipGroupCookbookSort);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbarPublicCookbooks);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        adapter = new CookbookAdapter(filteredCookbooks, new CookbookAdapter.OnCookbookClickListener() {
            @Override
            public void onCookbookClick(Cookbook cookbook) {
                PublicCookbooksActivity.this.onCookbookClick(cookbook);
            }
        });
        rvCookbooks.setLayoutManager(new GridLayoutManager(this, 2));
        rvCookbooks.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s.toString().toLowerCase().trim();
                applyFilterAndSort();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        chipGroupSort.setOnCheckedStateChangeListener(new ChipGroup.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull ChipGroup group, @NonNull List<Integer> checkedIds) {
                applyFilterAndSort();
            }
        });
    }

    private void loadPublicCookbooks() {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseFirestore.getInstance().collection("cookbooks")
                .whereEqualTo("isPublic", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allCookbooks.clear();
                    queryDocumentSnapshots.getDocuments().forEach(doc -> {
                        Cookbook book = Cookbook.fromDocument(doc);
                        if (book != null) allCookbooks.add(book);
                    });
                    applyFilterAndSort();
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.load_failed, Toast.LENGTH_SHORT).show();
                });
    }

    private void applyFilterAndSort() {
        filteredCookbooks.clear();
        for (Cookbook book : allCookbooks) {
            if (book.getName().toLowerCase().contains(currentSearch) ||
                book.getDescription().toLowerCase().contains(currentSearch)) {
                filteredCookbooks.add(book);
            }
        }

        // Sorting
        int checkedId = chipGroupSort.getCheckedChipId();
        if (checkedId == R.id.chipSortPopular) {
            Collections.sort(filteredCookbooks, (c1, c2) -> Integer.compare(c2.getFollowerCount(), c1.getFollowerCount()));
        } else if (checkedId == R.id.chipSortNewest) {
            Collections.sort(filteredCookbooks, (c1, c2) -> Long.compare(c2.getCreatedAt(), c1.getCreatedAt()));
        } else if (checkedId == R.id.chipSortMostRecipes) {
            Collections.sort(filteredCookbooks, (c1, c2) -> {
                int count1 = c1.getRecipeIds() != null ? c1.getRecipeIds().size() : 0;
                int count2 = c2.getRecipeIds() != null ? c2.getRecipeIds().size() : 0;
                return Integer.compare(count2, count1);
            });
        }

        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(filteredCookbooks.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void onCookbookClick(Cookbook cookbook) {
        Intent intent = new Intent(this, CookbookDetailActivity.class);
        intent.putExtra(CookbookDetailActivity.EXTRA_COOKBOOK_ID, cookbook.getId());
        startActivity(intent);
    }
}
