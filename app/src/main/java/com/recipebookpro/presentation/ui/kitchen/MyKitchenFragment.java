package com.recipebookpro.presentation.ui.kitchen;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.recipebookpro.R;
import com.recipebookpro.presentation.adapter.RecipeAdapter;
import com.recipebookpro.domain.model.Cookbook;
import com.recipebookpro.domain.model.Recipe;
import com.recipebookpro.presentation.ui.recipe.RecipeDetailActivity;
import com.recipebookpro.presentation.ui.kitchen.adapter.CookbookAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyKitchenFragment extends Fragment {

    private RecyclerView rvCookbooks;
    private RecyclerView rvFollowedCookbooks;
    private RecyclerView rvCollabCookbooks;
    private RecyclerView rvLikedRecipes;
    private TextView tvEmptyCookbooks;
    private TextView tvEmptyFollowed;
    private TextView tvEmptyLikedRecipes;
    private MaterialTextView tvInvitationsLabel;
    private MaterialTextView tvCollabLabel;
    private LinearLayout containerInvitations;
    private ProgressBar progressKitchen;

    private CookbookAdapter cookbookAdapter;
    private CookbookAdapter followedAdapter;
    private CookbookAdapter collabAdapter;
    private RecipeAdapter likedRecipeAdapter;

    private List<Cookbook> myCookbooks = new ArrayList<>();
    private List<Cookbook> followedCookbooks = new ArrayList<>();
    private List<Cookbook> collabCookbooks = new ArrayList<>();
    private List<Recipe> likedRecipes = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_kitchen, container, false);
        
        rvCookbooks = view.findViewById(R.id.rvCookbooks);
        tvEmptyCookbooks = view.findViewById(R.id.tvEmptyCookbooks);
        progressKitchen = view.findViewById(R.id.progressKitchen);
        ExtendedFloatingActionButton fabAddCookbook = view.findViewById(R.id.fabAddCookbook);

        rvFollowedCookbooks = view.findViewById(R.id.rvFollowedCookbooks);
        rvCollabCookbooks = view.findViewById(R.id.rvCollabCookbooks);
        rvLikedRecipes = view.findViewById(R.id.rvLikedRecipes);
        tvEmptyFollowed = view.findViewById(R.id.tvEmptyFollowed);
        tvEmptyLikedRecipes = view.findViewById(R.id.tvEmptyLikedRecipes);
        tvInvitationsLabel = view.findViewById(R.id.tvInvitationsLabel);
        tvCollabLabel = view.findViewById(R.id.tvCollabLabel);
        containerInvitations = view.findViewById(R.id.containerInvitations);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        setupRecyclerViews();

        fabAddCookbook.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), CookbookAddEditActivity.class));
        });

        loadData();
        loadInvitations();
        
        return view;
    }

    private void setupRecyclerViews() {
        if (rvCookbooks != null) {
            rvCookbooks.setLayoutManager(new GridLayoutManager(getContext(), 2));
            cookbookAdapter = new CookbookAdapter(myCookbooks, this::openCookbook);
            rvCookbooks.setAdapter(cookbookAdapter);
        }

        if (rvFollowedCookbooks != null) {
            rvFollowedCookbooks.setLayoutManager(new GridLayoutManager(getContext(), 2));
            followedAdapter = new CookbookAdapter(followedCookbooks, this::openCookbook);
            rvFollowedCookbooks.setAdapter(followedAdapter);
        }

        if (rvCollabCookbooks != null) {
            rvCollabCookbooks.setLayoutManager(new GridLayoutManager(getContext(), 2));
            collabAdapter = new CookbookAdapter(collabCookbooks, this::openCookbook);
            rvCollabCookbooks.setAdapter(collabAdapter);
        }

        if (rvLikedRecipes != null) {
            rvLikedRecipes.setLayoutManager(new LinearLayoutManager(getContext()));
            likedRecipeAdapter = new RecipeAdapter(recipe -> {
                Intent intent = new Intent(getContext(), RecipeDetailActivity.class);
                intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE, recipe);
                startActivity(intent);
            });
            rvLikedRecipes.setAdapter(likedRecipeAdapter);
        }
    }

    private void loadData() {
        if (currentUser == null) return;
        if (progressKitchen != null) progressKitchen.setVisibility(View.VISIBLE);

        db.collection("cookbooks")
          .whereEqualTo("userId", currentUser.getUid())
          .addSnapshotListener((value, error) -> {
              if (error != null || !isAdded()) return;
              myCookbooks.clear();
              if (value != null) {
                  for (QueryDocumentSnapshot doc : value) {
                      myCookbooks.add(Cookbook.fromDocument(doc));
                  }
                  Collections.sort(myCookbooks, (b1, b2) -> Long.compare(b2.getCreatedAt(), b1.getCreatedAt()));
              }
              if (cookbookAdapter != null) cookbookAdapter.notifyDataSetChanged();
              updateEmptyState(tvEmptyCookbooks, myCookbooks);
              checkProgress();
          });

        db.collection("cookbooks")
          .whereArrayContains("followerIds", currentUser.getUid())
          .addSnapshotListener((value, error) -> {
              if (error != null || !isAdded()) return;
              followedCookbooks.clear();
              if (value != null) {
                  for (QueryDocumentSnapshot doc : value) {
                      followedCookbooks.add(Cookbook.fromDocument(doc));
                  }
                  Collections.sort(followedCookbooks, (b1, b2) -> Long.compare(b2.getCreatedAt(), b1.getCreatedAt()));
              }
              if (followedAdapter != null) followedAdapter.notifyDataSetChanged();
              updateEmptyState(tvEmptyFollowed, followedCookbooks);
              checkProgress();
          });

        db.collection("cookbooks")
          .whereArrayContains("collaboratorIds", currentUser.getUid())
          .addSnapshotListener((value, error) -> {
              if (error != null || !isAdded()) return;
              collabCookbooks.clear();
              if (value != null) {
                  for (QueryDocumentSnapshot doc : value) {
                      collabCookbooks.add(Cookbook.fromDocument(doc));
                  }
                  Collections.sort(collabCookbooks, (b1, b2) -> Long.compare(b2.getCreatedAt(), b1.getCreatedAt()));
              }
              if (collabAdapter != null) collabAdapter.notifyDataSetChanged();
              if (tvCollabLabel != null) {
                  tvCollabLabel.setVisibility(collabCookbooks.isEmpty() ? View.GONE : View.VISIBLE);
              }
              checkProgress();
          });

        db.collection("users").document(currentUser.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null || !isAdded()) return;
                    if (value == null || !value.exists()) {
                        likedRecipes.clear();
                        if (likedRecipeAdapter != null) likedRecipeAdapter.setRecipeList(likedRecipes);
                        updateEmptyState(tvEmptyLikedRecipes, likedRecipes);
                        checkProgress();
                        return;
                    }
                    List<String> likedRecipeIds = (List<String>) value.get("likedRecipeIds");
                    loadLikedRecipes(likedRecipeIds);
                });
    }

    private void loadInvitations() {
        if (currentUser == null) return;

        db.collection("cookbook_invitations")
                .whereEqualTo("toUserId", currentUser.getUid())
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {
                    if (error != null || !isAdded()) return;
                    containerInvitations.removeAllViews();

                    if (value == null || value.isEmpty()) {
                        tvInvitationsLabel.setVisibility(View.GONE);
                        return;
                    }

                    tvInvitationsLabel.setVisibility(View.VISIBLE);

                    for (QueryDocumentSnapshot doc : value) {
                        String invitationId = doc.getId();
                        String cookbookName = doc.getString("cookbookName");
                        String fromUserName = doc.getString("fromUserName");
                        String cookbookId = doc.getString("cookbookId");

                        View card = LayoutInflater.from(getContext())
                                .inflate(R.layout.item_invitation, containerInvitations, false);

                        MaterialTextView tvCookbook = card.findViewById(R.id.tvInvitationCookbook);
                        MaterialTextView tvFrom = card.findViewById(R.id.tvInvitationFrom);
                        MaterialButton btnAccept = card.findViewById(R.id.btnAccept);
                        MaterialButton btnDecline = card.findViewById(R.id.btnDecline);

                        tvCookbook.setText(cookbookName != null ? cookbookName : "Defter");
                        tvFrom.setText(fromUserName != null ? fromUserName + " sizi davet etti" : "Davet");

                        btnAccept.setOnClickListener(v -> acceptInvitation(invitationId, cookbookId));
                        btnDecline.setOnClickListener(v -> declineInvitation(invitationId));

                        containerInvitations.addView(card);
                    }
                });
    }

    private void acceptInvitation(String invitationId, String cookbookId) {
        if (currentUser == null || cookbookId == null) return;

        db.collection("cookbook_invitations").document(invitationId)
                .update("status", "accepted")
                .addOnSuccessListener(aVoid -> {
                    db.collection("cookbooks").document(cookbookId)
                            .update("collaboratorIds", FieldValue.arrayUnion(currentUser.getUid()));
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), R.string.invitation_accepted, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), R.string.error_generic, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void declineInvitation(String invitationId) {
        db.collection("cookbook_invitations").document(invitationId)
                .update("status", "declined")
                .addOnSuccessListener(aVoid -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), R.string.invitation_declined, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkProgress() {
        if (progressKitchen != null) progressKitchen.setVisibility(View.GONE);
    }

    private void updateEmptyState(TextView tv, List<?> list) {
        if (tv != null) {
            tv.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void openCookbook(Cookbook book) {
        Intent intent = new Intent(getContext(), CookbookDetailActivity.class);
        intent.putExtra(CookbookDetailActivity.EXTRA_COOKBOOK_ID, book.getId());
        startActivity(intent);
    }

    private void loadLikedRecipes(List<String> likedRecipeIds) {
        if (likedRecipeIds == null || likedRecipeIds.isEmpty()) {
            likedRecipes.clear();
            if (likedRecipeAdapter != null) likedRecipeAdapter.setRecipeList(likedRecipes);
            updateEmptyState(tvEmptyLikedRecipes, likedRecipes);
            checkProgress();
            return;
        }

        List<List<String>> chunks = partition(likedRecipeIds, 10);
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        for (List<String> chunk : chunks) {
            tasks.add(db.collection("recipes")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get());
        }

        Tasks.whenAllSuccess(tasks)
                .addOnSuccessListener(results -> {
                    likedRecipes.clear();
                    for (Object result : results) {
                        QuerySnapshot snapshot = (QuerySnapshot) result;
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            likedRecipes.add(Recipe.fromDocument(doc));
                        }
                    }
                    if (likedRecipeAdapter != null) likedRecipeAdapter.setRecipeList(likedRecipes);
                    updateEmptyState(tvEmptyLikedRecipes, likedRecipes);
                    checkProgress();
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), R.string.error_generic, Toast.LENGTH_SHORT).show();
                    }
                    checkProgress();
                });
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> parts = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            parts.add(new ArrayList<>(list.subList(i, Math.min(list.size(), i + size))));
        }
        return parts;
    }
}
