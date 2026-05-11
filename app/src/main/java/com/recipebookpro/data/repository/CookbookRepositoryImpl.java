package com.recipebookpro.data.repository;
import com.google.firebase.firestore.FirebaseFirestore;
import com.recipebookpro.domain.repository.CookbookRepository;
import java.util.ArrayList;
public class CookbookRepositoryImpl implements CookbookRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    @Override
    public void getCookbooks(String userId, OnCookbooksLoadedListener listener) {
        db.collection("cookbooks").whereEqualTo("userId", userId).get()
            .addOnSuccessListener(query -> listener.onLoaded(new ArrayList<>()))
            .addOnFailureListener(listener::onError);
    }
}
