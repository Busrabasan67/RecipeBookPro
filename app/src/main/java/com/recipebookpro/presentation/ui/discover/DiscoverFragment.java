package com.recipebookpro.presentation.ui.discover;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.recipebookpro.R;
import com.recipebookpro.data.remote.MLKitTranslationService;
import com.recipebookpro.domain.model.Cookbook;
import com.recipebookpro.domain.model.Recipe;
import com.recipebookpro.domain.model.User;
import com.recipebookpro.domain.service.TranslationService;
import com.recipebookpro.presentation.ui.discover.adapter.DiscoverRecipeAdapter;
import com.recipebookpro.presentation.ui.discover.adapter.DiscoverRecipeAdapter.ScoredRecipe;
import com.recipebookpro.presentation.ui.kitchen.CookbookDetailActivity;
import com.recipebookpro.presentation.ui.kitchen.adapter.CookbookAdapter;
import com.recipebookpro.presentation.ui.recipe.RecipeDetailActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DiscoverFragment extends Fragment implements DiscoverRecipeAdapter.OnDiscoverInteractionListener, CookbookAdapter.OnCookbookClickListener {

    private EditText etSearch;
    private ChipGroup chipGroupIngredients;
    private RecyclerView rvDiscoverResults, rvPublicCookbooks;
    private ProgressBar progressDiscover;
    private TextView tvDiscoverEmpty, tvResultsTitle, tvPublicCookbooksLabel;

    private FirebaseFirestore db;
    private DiscoverRecipeAdapter adapter;
    private CookbookAdapter cookbookAdapter;
    private final List<ScoredRecipe> results = new ArrayList<>();
    private final List<Cookbook> publicCookbooks = new ArrayList<>();
    private final Map<String, User> ownersById = new HashMap<>();

    private TranslationService translationService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_discover, container, false);

        db = FirebaseFirestore.getInstance();
        translationService = new MLKitTranslationService(requireContext());

        etSearch = view.findViewById(R.id.etDiscoverSearch);
        MaterialButton btnSearch = view.findViewById(R.id.btnSearchByIngredient);
        chipGroupIngredients = view.findViewById(R.id.chipGroupIngredients);
        rvDiscoverResults = view.findViewById(R.id.rvDiscoverResults);
        rvPublicCookbooks = view.findViewById(R.id.rvPublicCookbooks);
        progressDiscover = view.findViewById(R.id.progressDiscover);
        tvDiscoverEmpty = view.findViewById(R.id.tvDiscoverEmpty);
        tvResultsTitle = view.findViewById(R.id.tvResultsTitle);
        tvPublicCookbooksLabel = view.findViewById(R.id.tvPublicCookbooksLabel);

        // Keyboard "Search" action trigger
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                performSearch();
                return true;
            }
            return false;
        });

        adapter = new DiscoverRecipeAdapter(results, this);
        rvDiscoverResults.setLayoutManager(new LinearLayoutManager(getContext()));
        rvDiscoverResults.setAdapter(adapter);

        cookbookAdapter = new CookbookAdapter(publicCookbooks, this);
        rvPublicCookbooks.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvPublicCookbooks.setAdapter(cookbookAdapter);

        btnSearch.setOnClickListener(v -> performSearch());

        populateIngredients();
        loadAllPublicRecipes(new ArrayList<>(), "", "");
        return view;
    }

    private void populateIngredients() {
        String[] commonIngredients = {"Yumurta", "Süt", "Un", "Şeker", "Yağ", "Tuz", "Domates", "Patates", "Tavuk", "Et", "Soğan", "Sarımsak", "Biber", "Mısır"};
        for (String ing : commonIngredients) {
            Chip chip = new Chip(requireContext());
            chip.setText(ing);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> performSearch());
            chipGroupIngredients.addView(chip);
        }
    }

    private void performSearch() {
        hideKeyboard();
        List<String> selected = getSelectedIngredients();
        String textQuery = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";

        progressDiscover.setVisibility(View.VISIBLE);
        tvDiscoverEmpty.setVisibility(View.GONE);
        rvDiscoverResults.setVisibility(View.GONE);
        tvResultsTitle.setVisibility(View.GONE);
        setPublicCookbooksVisibility(false);

        if (textQuery.isEmpty()) {
            loadAllPublicRecipes(selected, textQuery, "");
            return;
        }

        // 1. Immediate Manual Patch for common search terms (Instant results)
        String manualTranslated = applyManualPatchForSearch(textQuery);
        if (!manualTranslated.isEmpty()) {
            loadAllPublicRecipes(selected, textQuery, manualTranslated);
            return;
        }

        // 2. Fallback to TranslationService
        translationService.translateSingleField(textQuery, "tr", "en")
                .addOnSuccessListener(translated -> {
                    if (translated != null && !translated.equalsIgnoreCase(textQuery)) {
                        loadAllPublicRecipes(selected, textQuery, translated);
                    } else {
                        translationService.translateSingleField(textQuery, "en", "tr")
                                .addOnSuccessListener(trTranslated -> {
                                    loadAllPublicRecipes(selected, textQuery, trTranslated);
                                })
                                .addOnFailureListener(e -> loadAllPublicRecipes(selected, textQuery, ""));
                    }
                })
                .addOnFailureListener(e -> loadAllPublicRecipes(selected, textQuery, ""));
    }

    private void loadAllPublicRecipes(List<String> selectedIngredients, String textQuery, String translatedQuery) {
        Set<String> collectedRecipeIds = new LinkedHashSet<>();
        List<Recipe> allRecipes = new ArrayList<>();
        publicCookbooks.clear();
        
        final int[] pending = {4};

        Runnable checkDone = () -> {
            pending[0]--;
            if (pending[0] == 0) {
                loadAllOwnersForSearch(allRecipes, () -> 
                    filterAndDisplay(allRecipes, selectedIngredients, textQuery, translatedQuery)
                );
            }
        };

        for (String field : new String[]{"isPublic", "public"}) {
            db.collection("recipes")
                    .whereEqualTo(field, true)
                    .get()
                    .addOnSuccessListener(snap -> {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            if (collectedRecipeIds.add(doc.getId())) {
                                allRecipes.add(Recipe.fromDocument(doc));
                            }
                        }
                        checkDone.run();
                    })
                    .addOnFailureListener(e -> checkDone.run());
        }

        for (String field : new String[]{"isPublic", "public"}) {
            db.collection("cookbooks")
                    .whereEqualTo(field, true)
                    .get()
                    .addOnSuccessListener(cookbookSnap -> {
                        List<String> recipeIds = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : cookbookSnap) {
                            Cookbook book = Cookbook.fromDocument(doc);
                            publicCookbooks.add(book);
                            if (book.getRecipeIds() != null) {
                                for (String rid : book.getRecipeIds()) {
                                    if (!collectedRecipeIds.contains(rid)) {
                                        recipeIds.add(rid);
                                        collectedRecipeIds.add(rid);
                                    }
                                }
                            }
                        }

                        if (recipeIds.isEmpty()) {
                            checkDone.run();
                            return;
                        }

                        List<List<String>> chunks = partition(recipeIds, 10);
                        final int[] chunkPending = {chunks.size()};

                        for (List<String> chunk : chunks) {
                            db.collection("recipes")
                                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                                    .get()
                                    .addOnSuccessListener(recipeSnap -> {
                                        for (DocumentSnapshot doc : recipeSnap.getDocuments()) {
                                            if (collectedRecipeIds.add(doc.getId())) {
                                                allRecipes.add(Recipe.fromDocument(doc));
                                            }
                                        }
                                        chunkPending[0]--;
                                        if (chunkPending[0] == 0) checkDone.run();
                                    })
                                    .addOnFailureListener(e -> {
                                        chunkPending[0]--;
                                        if (chunkPending[0] == 0) checkDone.run();
                                    });
                        }
                    })
                    .addOnFailureListener(e -> checkDone.run());
        }
    }

    private void filterAndDisplay(List<Recipe> allRecipes, List<String> selectedIngredients, String textQuery, String translatedQuery) {
        if (!isAdded()) return;

        List<Recipe> filtered = new ArrayList<>();
        Set<String> selectedSet = new HashSet<>(selectedIngredients);

        for (Recipe r : allRecipes) {
            Set<String> recipeSearchTerms = getRecipeSearchTerms(r);

            if (!selectedSet.isEmpty()) {
                boolean hasAny = false;
                for (String sel : selectedSet) {
                    String translatedSel = applyManualPatchForSearch(sel);
                    for (String term : recipeSearchTerms) {
                        if (term.contains(sel) || sel.contains(term) || 
                            (!translatedSel.isEmpty() && (term.contains(translatedSel) || translatedSel.contains(term)))) {
                            hasAny = true;
                            break;
                        }
                    }
                    if (hasAny) break;
                }
                if (!hasAny) continue;
            }

            if (!textQuery.isEmpty()) {
                String q1 = textQuery.toLowerCase();
                String q2 = translatedQuery != null ? translatedQuery.toLowerCase() : "";
                
                boolean matchInTitleDesc = r.getTitle().toLowerCase().contains(q1) || 
                                         r.getDescription().toLowerCase().contains(q1) ||
                                         (!q2.isEmpty() && (r.getTitle().toLowerCase().contains(q2) || r.getDescription().toLowerCase().contains(q2)));
                
                boolean matchAuthor = false;
                User owner = ownersById.get(r.getUserId());
                if (owner != null && owner.getDisplayName() != null) {
                    String authorName = owner.getDisplayName().toLowerCase();
                    if (authorName.contains(q1) || (!q2.isEmpty() && authorName.contains(q2))) {
                        matchAuthor = true;
                    }
                }

                boolean matchInTerms = false;
                for (String term : recipeSearchTerms) {
                    if (term.contains(q1) || (!q2.isEmpty() && term.contains(q2))) {
                        matchInTerms = true;
                        break;
                    }
                }
                
                if (!matchInTitleDesc && !matchInTerms && !matchAuthor) {
                    continue;
                }
            }

            filtered.add(r);
        }

        scoreAndDisplay(filtered, selectedIngredients);
    }

    private void loadAllOwnersForSearch(List<Recipe> recipes, Runnable onDone) {
        Set<String> uids = new HashSet<>();
        for (Recipe r : recipes) {
            if (r.getUserId() != null) uids.add(r.getUserId());
        }
        
        if (uids.isEmpty()) {
            onDone.run();
            return;
        }

        List<com.google.android.gms.tasks.Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String uid : uids) {
            if (!ownersById.containsKey(uid)) {
                tasks.add(db.collection("users").document(uid).get());
            }
        }

        if (tasks.isEmpty()) {
            onDone.run();
            return;
        }

        com.google.android.gms.tasks.Tasks.whenAllComplete(tasks)
                .addOnCompleteListener(t -> {
                    for (com.google.android.gms.tasks.Task<DocumentSnapshot> task : tasks) {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            User u = task.getResult().toObject(User.class);
                            if (u != null) {
                                u.setUid(task.getResult().getId());
                                ownersById.put(u.getUid(), u);
                            }
                        }
                    }
                    onDone.run();
                });
    }

    private Set<String> getRecipeSearchTerms(Recipe r) {
        Set<String> terms = new HashSet<>();
        if (r.getIngredientNames() != null) {
            for (String name : r.getIngredientNames()) terms.add(name.toLowerCase().trim());
        }
        if (r.getIngredients() != null) {
            for (Recipe.Ingredient ing : r.getIngredients()) {
                terms.add(ing.getName().toLowerCase().trim());
                if (ing.getUnit() != null) terms.add(ing.getUnit().toLowerCase().trim());
                if (ing.getTranslatedName() != null && !ing.getTranslatedName().isEmpty()) {
                    terms.add(ing.getTranslatedName().toLowerCase().trim());
                }
                if (ing.getTranslatedUnit() != null && !ing.getTranslatedUnit().isEmpty()) {
                    terms.add(ing.getTranslatedUnit().toLowerCase().trim());
                }
            }
        }
        return terms;
    }

    private void scoreAndDisplay(List<Recipe> recipes, List<String> selectedIngredients) {
        results.clear();
        Set<String> selectedSet = new HashSet<>(selectedIngredients);

        for (Recipe recipe : recipes) {
            Set<String> recipeIngs = getRecipeSearchTerms(recipe);

            int matchPercent;
            List<String> missing = new ArrayList<>();

            if (selectedSet.isEmpty()) {
                matchPercent = 100;
            } else {
                int matched = 0;
                for (String sel : selectedSet) {
                    boolean found = false;
                    String trans = applyManualPatchForSearch(sel);
                    for (String ri : recipeIngs) {
                        if (ri.contains(sel) || sel.contains(ri) || 
                            (!trans.isEmpty() && (ri.contains(trans) || trans.contains(ri)))) {
                            found = true;
                            break;
                        }
                    }
                    if (found) matched++;
                    else missing.add(sel);
                }
                matchPercent = (matched * 100) / selectedSet.size();
            }

            results.add(new ScoredRecipe(recipe, matchPercent, missing));
        }

        results.sort((a, b) -> Integer.compare(b.matchPercent, a.matchPercent));
        adapter.setOwnerMap(ownersById);
        
        String resultsText = getString(R.string.results) + " (" + results.size() + ")";
        tvResultsTitle.setText(resultsText);
        
        updateResultVisibility();
    }

    private void updateResultVisibility() {
        progressDiscover.setVisibility(View.GONE);
        if (results.isEmpty()) {
            tvDiscoverEmpty.setVisibility(View.VISIBLE);
            rvDiscoverResults.setVisibility(View.GONE);
            tvResultsTitle.setVisibility(View.GONE);
        } else {
            tvDiscoverEmpty.setVisibility(View.GONE);
            rvDiscoverResults.setVisibility(View.VISIBLE);
            tvResultsTitle.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }

        if (!publicCookbooks.isEmpty()) {
            tvPublicCookbooksLabel.setVisibility(View.VISIBLE);
            rvPublicCookbooks.setVisibility(View.VISIBLE);
            cookbookAdapter.notifyDataSetChanged();
        } else {
            tvPublicCookbooksLabel.setVisibility(View.GONE);
            rvPublicCookbooks.setVisibility(View.GONE);
        }
    }

    private void setPublicCookbooksVisibility(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        tvPublicCookbooksLabel.setVisibility(visibility);
        rvPublicCookbooks.setVisibility(visibility);
    }

    private List<String> getSelectedIngredients() {
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < chipGroupIngredients.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupIngredients.getChildAt(i);
            if (chip.isChecked()) {
                selected.add(chip.getText().toString().toLowerCase());
            }
        }
        return selected;
    }

    private String applyManualPatchForSearch(String ingredient) {
        String lower = ingredient.toLowerCase().trim();
        Map<String, String> patches = new HashMap<>();
        // English -> Turkish
        patches.put("egg", "yumurta");
        patches.put("milk", "süt");
        patches.put("water", "su");
        patches.put("flour", "un");
        patches.put("corn", "mısır");
        patches.put("span", "karış");
        patches.put("sugar", "şeker");
        patches.put("oil", "yağ");
        patches.put("salt", "tuz");
        // Turkish -> English
        patches.put("yumurta", "egg");
        patches.put("süt", "milk");
        patches.put("su", "water");
        patches.put("un", "flour");
        patches.put("mısır", "corn");
        patches.put("karış", "span");
        patches.put("şeker", "sugar");
        patches.put("yağ", "oil");
        patches.put("tuz", "salt");
        
        return patches.getOrDefault(lower, "");
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    @Override
    public void onRecipeClick(Recipe recipe) {
        android.content.Intent intent = new android.content.Intent(getContext(), RecipeDetailActivity.class);
        intent.putExtra("recipe", recipe);
        startActivity(intent);
    }

    @Override
    public void onAddMissingToShopping(Recipe recipe, List<String> missing) {
        Toast.makeText(getContext(), R.string.recipe_saved, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAuthorClick(String userId) {
        // Handle author click (e.g. open profile)
    }

    @Override
    public void onToggleFollowAuthor(String userId, boolean currentlyFollowing) {
        // Handle follow/unfollow
    }

    @Override
    public void onCookbookClick(Cookbook cookbook) {
        android.content.Intent intent = new android.content.Intent(getContext(), CookbookDetailActivity.class);
        intent.putExtra("cookbook", cookbook);
        startActivity(intent);
    }

    private void hideKeyboard() {
        View view = requireActivity().getCurrentFocus();
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (translationService != null) translationService.close();
    }
}
