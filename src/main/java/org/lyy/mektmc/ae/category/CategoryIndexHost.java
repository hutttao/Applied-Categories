package org.lyy.mektmc.ae.category;

import appeng.api.stacks.AEKey;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface CategoryIndexHost {
    UUID getDatabaseId();

    Collection<CategoryDef> getCategories();

    Set<UUID> getCategoriesFor(AEKey key);

    boolean addCategory(AEKey key, UUID categoryId);

    boolean removeCategory(AEKey key, UUID categoryId);

    CategoryDef createCategory(String name, int color);

    boolean renameCategory(UUID categoryId, String newName);

    boolean deleteCategory(UUID categoryId);

    boolean isOnlineAndValid();
}
