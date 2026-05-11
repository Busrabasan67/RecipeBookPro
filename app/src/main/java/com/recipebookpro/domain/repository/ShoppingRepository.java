package com.recipebookpro.domain.repository;
public interface ShoppingRepository {
    void getShoppingLists(String userId, OnShoppingListsLoadedListener listener);
    interface OnShoppingListsLoadedListener {
        void onLoaded(java.util.List<com.recipebookpro.domain.model.ShoppingList> lists);
        void onError(Exception e);
    }
}
