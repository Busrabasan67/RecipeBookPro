package com.recipebookpro.data.repository;
import com.google.firebase.firestore.FirebaseFirestore;
import com.recipebookpro.domain.repository.ShoppingRepository;
import java.util.ArrayList;
public class ShoppingRepositoryImpl implements ShoppingRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    @Override
    public void getShoppingLists(String userId, OnShoppingListsLoadedListener listener) {
        db.collection("shopping_lists").whereEqualTo("userId", userId).get()
            .addOnSuccessListener(query -> listener.onLoaded(new ArrayList<>()))
            .addOnFailureListener(listener::onError);
    }
}
