package com.recipebookpro.domain.repository;
public interface CookbookRepository {
    void getCookbooks(String userId, OnCookbooksLoadedListener listener);
    interface OnCookbooksLoadedListener {
        void onLoaded(java.util.List<com.recipebookpro.domain.model.Cookbook> cookbooks);
        void onError(Exception e);
    }
}
