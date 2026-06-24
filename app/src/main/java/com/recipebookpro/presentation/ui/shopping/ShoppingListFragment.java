package com.recipebookpro.presentation.ui.shopping;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.recipebookpro.R;
import com.recipebookpro.domain.model.ShoppingList;
import com.recipebookpro.presentation.ui.shopping.adapter.ShoppingListAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShoppingListFragment extends Fragment {

    private RecyclerView rvShoppingLists;
    private TextView tvEmptyLists;
    private ProgressBar progressShoppingLists;
    
    private ShoppingListAdapter adapter;
    private List<ShoppingList> listCollection = new ArrayList<>();
    
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shopping_list, container, false);

        rvShoppingLists = view.findViewById(R.id.rvShoppingLists);
        tvEmptyLists = view.findViewById(R.id.tvEmptyShoppingLists);
        progressShoppingLists = view.findViewById(R.id.progressShoppingLists);
        ExtendedFloatingActionButton fabAddList = view.findViewById(R.id.fabAddShoppingList);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        rvShoppingLists.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ShoppingListAdapter(listCollection, list -> {
            Intent intent = new Intent(getContext(), ShoppingListDetailActivity.class);
            intent.putExtra(ShoppingListDetailActivity.EXTRA_LIST_ID, list.getId());
            startActivity(intent);
        });
        
        adapter.setOnLongClickListener(list -> confirmDeleteList(list));
        rvShoppingLists.setAdapter(adapter);

        fabAddList.setOnClickListener(v -> showCreateListDialog());

        loadShoppingLists();

        return view;
    }

    private Map<String, ShoppingList> ownerListsMap = new HashMap<>();
    private Map<String, ShoppingList> collabListsMap = new HashMap<>();
    private ListenerRegistration ownerListener;
    private ListenerRegistration collabListener;

    private void loadShoppingLists() {
        if (currentUser == null) return;
        progressShoppingLists.setVisibility(View.VISIBLE);

        if (ownerListener != null) ownerListener.remove();
        if (collabListener != null) collabListener.remove();
        
        ownerListener = db.collection("shopping_lists")
                .whereEqualTo("userId", currentUser.getUid())
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        ownerListsMap.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            ownerListsMap.put(doc.getId(), ShoppingList.fromDocument(doc));
                        }
                        mergeAndDisplayLists();
                    }
                });

        collabListener = db.collection("shopping_lists")
                .whereArrayContains("collaboratorIds", currentUser.getUid())
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        collabListsMap.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            collabListsMap.put(doc.getId(), ShoppingList.fromDocument(doc));
                        }
                        mergeAndDisplayLists();
                    }
                });
    }

    private void mergeAndDisplayLists() {
        progressShoppingLists.setVisibility(View.GONE);
        listCollection.clear();
        Map<String, ShoppingList> all = new HashMap<>(ownerListsMap);
        all.putAll(collabListsMap);
        
        listCollection.addAll(all.values());
        listCollection.sort((l1, l2) -> Long.compare(l2.getCreatedAt(), l1.getCreatedAt()));
        
        adapter.notifyDataSetChanged();
        tvEmptyLists.setVisibility(listCollection.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void confirmDeleteList(ShoppingList list) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_recipe)
                .setMessage(R.string.shopping_list_delete_confirm)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    db.collection("shopping_lists").document(list.getId()).delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), R.string.recipe_deleted, Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(getContext(), R.string.error_generic, Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadShoppingLists();
    }

    @Override
    public void onDestroyView() {
        if (ownerListener != null) ownerListener.remove();
        if (collabListener != null) collabListener.remove();
        super.onDestroyView();
    }

    private void showCreateListDialog() {
        if (currentUser == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.shopping_new_list_title);

        final EditText input = new EditText(requireContext());
        input.setHint(R.string.shopping_new_list_hint);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        FrameLayout container = new FrameLayout(requireContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 50;
        params.rightMargin = 50;
        input.setLayoutParams(params);
        container.addView(input);

        builder.setView(container);

        builder.setPositiveButton(R.string.shopping_create, (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                ShoppingList newList = new ShoppingList(currentUser.getUid(), name);
                db.collection("shopping_lists").add(newList)
                  .addOnSuccessListener(documentReference -> Toast.makeText(getContext(), R.string.shopping_list_created, Toast.LENGTH_SHORT).show())
                  .addOnFailureListener(e -> Toast.makeText(getContext(), R.string.error_generic, Toast.LENGTH_SHORT).show());
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
