package com.recipebookpro.domain.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.recipebookpro.domain.model.Recipe;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class RecipeFamilyDeduplicatorTest {

    @Test
    public void sameSourceRecipeId_keepsNewestRecipe() {
        Recipe olderCopy = recipe("copy-1", "source-1", 100L);
        Recipe newerCopy = recipe("copy-2", "source-1", 200L);

        List<Recipe> result = RecipeFamilyDeduplicator.keepLatestRecipePerFamily(
                Arrays.asList(olderCopy, newerCopy));

        assertEquals(1, result.size());
        assertEquals("copy-2", result.get(0).getId());
    }

    @Test
    public void emptySourceRecipeId_keepsUserRecipesSeparate() {
        Recipe firstUserRecipe = recipe("user-1", "", 100L);
        Recipe secondUserRecipe = recipe("user-2", "", 200L);

        List<Recipe> result = RecipeFamilyDeduplicator.keepLatestRecipePerFamily(
                Arrays.asList(firstUserRecipe, secondUserRecipe));

        assertEquals(2, result.size());
        assertEquals("user-1", result.get(0).getId());
        assertEquals("user-2", result.get(1).getId());
    }

    @Test
    public void originalAndCopy_areSameFamilyAndNewestWins() {
        Recipe original = recipe("source-1", "", 100L);
        Recipe copy = recipe("copy-1", "source-1", 200L);

        List<Recipe> result = RecipeFamilyDeduplicator.keepLatestRecipePerFamily(
                Arrays.asList(original, copy));

        assertEquals(1, result.size());
        assertEquals("copy-1", result.get(0).getId());
    }

    @Test
    public void copyOfCopy_resolvesToRootFamilyWhenIntermediateExists() {
        Recipe root = recipe("source-1", "", 100L);
        Recipe firstCopy = recipe("copy-1", "source-1", 200L);
        Recipe secondCopy = recipe("copy-2", "copy-1", 300L);

        List<Recipe> result = RecipeFamilyDeduplicator.keepLatestRecipePerFamily(
                Arrays.asList(root, firstCopy, secondCopy));

        assertEquals(1, result.size());
        assertEquals("copy-2", result.get(0).getId());
    }

    @Test
    public void nullAndEmptyLists_returnEmptyResult() {
        assertTrue(RecipeFamilyDeduplicator.keepLatestRecipePerFamily(null).isEmpty());
        assertTrue(RecipeFamilyDeduplicator.keepLatestRecipePerFamily(Arrays.asList()).isEmpty());
    }

    @Test
    public void missingIdentityRecipes_doNotCollapseTogether() {
        Recipe first = recipe("", "", 100L);
        Recipe second = recipe("", "", 200L);

        List<Recipe> result = RecipeFamilyDeduplicator.keepLatestRecipePerFamily(
                Arrays.asList(first, second));

        assertEquals(2, result.size());
    }

    @Test
    public void sameCreatedAt_keepsLaterRecipeInInputOrder() {
        Recipe first = recipe("copy-1", "source-1", 100L);
        Recipe second = recipe("copy-2", "source-1", 100L);

        List<Recipe> result = RecipeFamilyDeduplicator.keepLatestRecipePerFamily(
                Arrays.asList(first, second));

        assertEquals(1, result.size());
        assertEquals("copy-2", result.get(0).getId());
    }

    private Recipe recipe(String id, String sourceRecipeId, long createdAt) {
        Recipe recipe = new Recipe();
        recipe.setId(id);
        recipe.setSourceRecipeId(sourceRecipeId);
        recipe.setCreatedAt(createdAt);
        recipe.setTitle(id);
        return recipe;
    }
}
