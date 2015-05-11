/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2015] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.dao.mongo;


import com.codenvy.api.dao.mongo.RecipeDaoImpl.FromDBObjectToRecipeFunction;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.machine.server.GroupImpl;
import org.eclipse.che.api.machine.server.PermissionsImpl;
import org.eclipse.che.api.machine.server.RecipeImpl;
import org.eclipse.che.api.machine.shared.Group;
import org.eclipse.che.api.machine.shared.Recipe;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * Tests for {@link RecipeDaoImpl}
 *
 * @author Eugene Voevodin
 */
public class RecipeDaoTest extends BaseDaoTest {

    private static final FromDBObjectToRecipeFunction FROM_OBJECT_TO_RECIPE_FUNCTION = new FromDBObjectToRecipeFunction();

    private RecipeDaoImpl recipeDao;

    @BeforeMethod
    public void setUp() throws Exception {
        setUp("recipes");
        recipeDao = new RecipeDaoImpl(db, "recipes");
    }

    @Test
    public void shouldBeAbleToGetRecipeById() throws Exception {
        final Group group = new GroupImpl("workspace/admin", "workspace123", asList("read"));
        final Map<String, List<String>> users = singletonMap("user123", asList("read", "write"));
        final Recipe example = new RecipeImpl().withId("recipe123")
                                               .withCreator("someone")
                                               .withScript("script content")
                                               .withType("script-type")
                                               .withTags(asList("tag1", "tag2"))
                                               .withPermissions(new PermissionsImpl(users, asList(group)));
        collection.save(recipeDao.asDBObject(example));

        final Recipe recipe = recipeDao.getById(example.getId());

        assertEquals(recipe, example);
    }

    @Test(expectedExceptions = NotFoundException.class,
          expectedExceptionsMessageRegExp = "Recipe with id 'fake' was not found")
    public void shouldThrowNotFoundExceptionWhenRecipeWithGivenIdWasNotFound() throws Exception {
        recipeDao.getById("fake");
    }

    @Test
    public void shouldBeAbleToCreateNewRecipe() throws Exception {
        final Group group = new GroupImpl("workspace/admin", "workspace123", asList("read"));
        final Map<String, List<String>> users = singletonMap("user123", asList("read", "write"));
        final Recipe example = new RecipeImpl().withId("recipe123")
                                               .withCreator("someone")
                                               .withScript("script content")
                                               .withType("script-type")
                                               .withTags(asList("tag1", "tag2"))
                                               .withPermissions(new PermissionsImpl(users, asList(group)));

        recipeDao.create(example);

        final DBObject dbObj = collection.findOne(example.getId());
        assertEquals(FROM_OBJECT_TO_RECIPE_FUNCTION.apply(dbObj), example);
    }

    @Test(expectedExceptions = ConflictException.class,
          expectedExceptionsMessageRegExp = "Recipe with id 'recipe123' already exists")
    public void shouldThrowConflictExceptionWhenRecipeWithSameIdAsGivenRecipeIdAlreadyExists() throws Exception {
        collection.save(new BasicDBObject("_id", "recipe123"));

        recipeDao.create(new RecipeImpl().withId("recipe123"));
    }

    @Test
    public void shouldBeAbleToGetRecipesByCreator() throws Exception {
        final Recipe example = new RecipeImpl().withId("recipe123")
                                               .withCreator("someone")
                                               .withScript("script content")
                                               .withType("script-type")
                                               .withTags(asList("tag1", "tag2"));
        final Recipe example2 = copy(example).withId("recipe234");
        final Recipe example3 = copy(example).withId("recipe345").withCreator("other-user");
        collection.save(recipeDao.asDBObject(example));
        collection.save(recipeDao.asDBObject(example2));
        collection.save(recipeDao.asDBObject(example3));

        List<Recipe> recipes = recipeDao.getByCreator("someone", 0, 10);
        assertEquals(new HashSet<>(recipes), new HashSet<>(asList(example, copy(example2))));

        recipes = recipeDao.getByCreator("other-user", 0, 10);
        assertEquals(recipes, asList(example3));
    }

    @Test
    public void shouldBeAbleToGetRecipesByCreatorWithLimitAndSkipCount() throws Exception {
        final Recipe example = new RecipeImpl().withId("recipe123")
                                               .withCreator("someone")
                                               .withScript("script content")
                                               .withType("script-type")
                                               .withTags(asList("tag1", "tag2"));
        for (int i = 0; i < 15; i++) {
            collection.save(recipeDao.asDBObject(copy(example).withId(Integer.toString(i))));
        }

        final List<Recipe> recipes = recipeDao.getByCreator("someone", 5, 10);
        assertEquals(recipes.size(), 10);
        final Set<String> ids = FluentIterable.from(recipes)
                                              .transform(new Function<Recipe, String>() {
                                                  @Nullable
                                                  @Override
                                                  public String apply(Recipe recipe) {
                                                      return recipe.getId();
                                                  }
                                              }).toSet();
        assertEquals(ids, new HashSet<>(asList("5", "6", "7", "8", "9", "10", "11", "12", "13", "14")));
    }

    @Test
    public void shouldReturnEmptyRecipesListWhenFoundRecipesCountIsFewerThanSkipCount() throws Exception {
        final Recipe example = new RecipeImpl().withId("recipe123")
                                               .withCreator("someone")
                                               .withScript("script content")
                                               .withType("script-type")
                                               .withTags(asList("tag1", "tag2"));
        collection.save(recipeDao.asDBObject(example));

        final List<Recipe> recipes = recipeDao.getByCreator("someone", 5, 10);

        assertEquals(recipes.size(), 0);
    }

    @Test
    public void shouldBeRemoveRecipeById() throws Exception {
        final Recipe example = new RecipeImpl().withId("recipe123")
                                               .withCreator("someone")
                                               .withScript("script content")
                                               .withType("script-type")
                                               .withTags(asList("tag1", "tag2"));
        collection.save(recipeDao.asDBObject(example));

        recipeDao.remove(example.getId());

        assertNull(collection.findOne(example.getId()));
    }

    @Test
    public void shouldBeAbleToUpdateRecipe() throws Exception {
        final Group group = new GroupImpl("workspace/admin", "workspace123", asList("read"));
        final Map<String, List<String>> users = new HashMap<>();
        users.put("user123", asList("read", "write"));
        final Recipe example = new RecipeImpl().withId("recipe123")
                                               .withCreator("someone")
                                               .withScript("script content")
                                               .withType("script-type")
                                               .withTags(new ArrayList<>(asList("tag1", "tag2")))
                                               .withPermissions(new PermissionsImpl(users, new ArrayList<>(asList(group))));
        collection.save(recipeDao.asDBObject(example));

        example.setType("new-script-type");
        example.setScript("new-script-content");
        example.getPermissions().getUsers().put("user234", asList("read"));
        example.getPermissions().getGroups().add(new GroupImpl("workspace/developer", "workspace123", asList("read")));

        recipeDao.update(example);

        assertEquals(FROM_OBJECT_TO_RECIPE_FUNCTION.apply(collection.findOne(example.getId())), example);
    }

    @Test(expectedExceptions = NotFoundException.class,
          expectedExceptionsMessageRegExp = "Recipe with id 'fake' was not found")
    public void shouldThrowNotFoundExceptionIfRecipeWithIdInUpdateWasNotFound() throws Exception {
        recipeDao.update(new RecipeImpl().withId("fake"));
    }

    @Test(enabled = false)//test mognodb server doesn't support $elemMatch
    public void shouldBeAbleToSearchRecipesByTagsAndType() throws Exception {
        final Group group = new GroupImpl("public", "null", asList("read", "search"));
        final Recipe example = new RecipeImpl().withId("recipe123")
                                               .withCreator("someone")
                                               .withScript("script content")
                                               .withType("script-type")
                                               .withTags(asList("tag1", "tag2"))
                                               .withPermissions(new PermissionsImpl(null, asList(group)));
        final Recipe example2 = copy(example).withId("recipe234")
                                             .withTags(asList("tag1", "tag2", "tag3"));
        final Recipe example3 = copy(example).withId("recipe345")
                                             .withTags(asList("tag1"));
        final Recipe example4 = copy(example).withId("recipe456")
                                             .withType("another type")
                                             .withPermissions(null);
        collection.save(recipeDao.asDBObject(example));
        collection.save(recipeDao.asDBObject(example2));
        collection.save(recipeDao.asDBObject(example3));
        collection.save(recipeDao.asDBObject(example4));

        final List<Recipe> recipes = recipeDao.search(asList("tag1", "tag2"), "script-type", 0, 10);

        assertEquals(new HashSet<>(recipes), new HashSet<>(asList(example, example2)));
    }

    @Test(enabled = false)//test mognodb server doesn't support $elemMatch
    public void shouldBeAbleToSearchRecipesByTags() throws Exception {
        final Group group = new GroupImpl("public", "null", asList("read", "search"));
        final Recipe example = new RecipeImpl().withId("recipe123")
                                               .withCreator("someone")
                                               .withScript("script content")
                                               .withType("script-type")
                                               .withTags(asList("tag1", "tag2"))
                                               .withPermissions(new PermissionsImpl(null, asList(group)));
        final Recipe example2 = copy(example).withId("recipe234")
                                             .withTags(asList("tag1", "tag2", "tag3"));
        final Recipe example3 = copy(example).withId("recipe345")
                                             .withTags(asList("tag1"));
        final Recipe example4 = copy(example).withId("recipe456")
                                             .withType("another type");
        collection.save(recipeDao.asDBObject(example));
        collection.save(recipeDao.asDBObject(example2));
        collection.save(recipeDao.asDBObject(example3));
        collection.save(recipeDao.asDBObject(example4));

        final List<Recipe> recipes = recipeDao.search(asList("tag1", "tag2"), null, 0, 10);

        assertEquals(new HashSet<>(recipes), new HashSet<>(asList(example, example2, example4)));
    }

    private Recipe copy(Recipe recipe) {
        return new RecipeImpl().withId(recipe.getId())
                               .withCreator(recipe.getCreator())
                               .withType(recipe.getType())
                               .withScript(recipe.getScript())
                               .withTags(recipe.getTags())
                               .withPermissions(recipe.getPermissions());
    }
}