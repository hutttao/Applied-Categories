package org.lyy.mektmc.ae.category;

import appeng.api.stacks.AEKey;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CategoryDatabase {
    public static final int CURRENT_VERSION = 1;

    private final Map<UUID, CategoryDef> categories = new LinkedHashMap<>();
    private final Map<AEKey, Set<UUID>> keyToCategories = new LinkedHashMap<>();
    private long lastModified;

    public CategoryDatabase() {
        this.lastModified = System.currentTimeMillis();
    }

    public Collection<CategoryDef> getCategories() {
        var sorted = new ArrayList<>(categories.values());
        sorted.sort(Comparator.comparingInt(CategoryDef::sortOrder).thenComparing(CategoryDef::name));
        return Collections.unmodifiableList(sorted);
    }

    public Set<UUID> getCategoriesFor(AEKey key) {
        var ids = keyToCategories.get(key);
        return ids == null ? Set.of() : Collections.unmodifiableSet(ids);
    }

    public CategoryDef createCategory(String name, int color) {
        if (categories.size() >= CategoryLimits.MAX_CATEGORIES) {
            throw new IllegalStateException("Too many categories");
        }
        var nextOrder = categories.values().stream().mapToInt(CategoryDef::sortOrder).max().orElse(-1) + 1;
        var def = new CategoryDef(UUID.randomUUID(), name, color, nextOrder, false);
        categories.put(def.id(), def);
        markModified();
        return def;
    }

    public boolean renameCategory(UUID categoryId, String newName) {
        var existing = categories.get(categoryId);
        if (existing == null || existing.builtin()) {
            return false;
        }
        categories.put(categoryId, existing.renamed(newName));
        markModified();
        return true;
    }

    public boolean deleteCategory(UUID categoryId) {
        var existing = categories.get(categoryId);
        if (existing == null || existing.builtin()) {
            return false;
        }
        categories.remove(categoryId);
        keyToCategories.values().removeIf(ids -> {
            ids.remove(categoryId);
            return ids.isEmpty();
        });
        markModified();
        return true;
    }

    public boolean addCategory(AEKey key, UUID categoryId) {
        if (!categories.containsKey(categoryId) || assignmentCount() >= CategoryLimits.MAX_ASSIGNMENTS) {
            return false;
        }
        var ids = keyToCategories.computeIfAbsent(key, ignored -> new LinkedHashSet<>());
        if (ids.add(categoryId)) {
            markModified();
            return true;
        }
        return false;
    }

    public boolean removeCategory(AEKey key, UUID categoryId) {
        var ids = keyToCategories.get(key);
        if (ids == null || !ids.remove(categoryId)) {
            return false;
        }
        if (ids.isEmpty()) {
            keyToCategories.remove(key);
        }
        markModified();
        return true;
    }

    public long lastModified() {
        return lastModified;
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        tag.putInt("version", CURRENT_VERSION);
        tag.putLong("last_modified", lastModified);

        var categoryList = new ListTag();
        for (var def : getCategories()) {
            var categoryTag = new CompoundTag();
            categoryTag.putUUID("id", def.id());
            categoryTag.putString("name", def.name());
            categoryTag.putInt("color", def.color());
            categoryTag.putInt("sort_order", def.sortOrder());
            categoryTag.putBoolean("builtin", def.builtin());
            categoryList.add(categoryTag);
        }
        tag.put("categories", categoryList);

        var assignmentList = new ListTag();
        for (var entry : keyToCategories.entrySet()) {
            var ids = entry.getValue();
            if (ids.isEmpty()) {
                continue;
            }
            var assignmentTag = new CompoundTag();
            assignmentTag.put("key", CategoryKeySerializer.write(entry.getKey(), registries));
            var idList = new ListTag();
            for (var id : ids) {
                if (categories.containsKey(id)) {
                    idList.add(StringTag.valueOf(id.toString()));
                }
            }
            if (!idList.isEmpty()) {
                assignmentTag.put("categories", idList);
                assignmentList.add(assignmentTag);
            }
        }
        tag.put("assignments", assignmentList);
        return tag;
    }

    public static CategoryDatabase load(CompoundTag tag, HolderLookup.Provider registries) {
        var database = new CategoryDatabase();
        database.categories.clear();
        database.keyToCategories.clear();

        if (tag.getInt("version") != CURRENT_VERSION) {
            return database;
        }

        database.lastModified = tag.getLong("last_modified");
        var categoryList = tag.getList("categories", Tag.TAG_COMPOUND);
        for (var i = 0; i < categoryList.size(); i++) {
            var categoryTag = categoryList.getCompound(i);
            if (!categoryTag.hasUUID("id")) {
                continue;
            }
            var def = new CategoryDef(
                    categoryTag.getUUID("id"),
                    categoryTag.getString("name"),
                    categoryTag.getInt("color"),
                    categoryTag.getInt("sort_order"),
                    categoryTag.getBoolean("builtin")
            );
            database.categories.put(def.id(), def);
        }

        var assignmentList = tag.getList("assignments", Tag.TAG_COMPOUND);
        for (var i = 0; i < assignmentList.size(); i++) {
            var assignmentTag = assignmentList.getCompound(i);
            var keyTag = assignmentTag.getCompound("key");
            var key = CategoryKeySerializer.read(keyTag, registries);
            if (key == null) {
                continue;
            }

            var idList = assignmentTag.getList("categories", Tag.TAG_STRING);
            var ids = new LinkedHashSet<UUID>();
            for (var j = 0; j < idList.size(); j++) {
                try {
                    var id = UUID.fromString(idList.getString(j));
                    if (database.categories.containsKey(id)) {
                        ids.add(id);
                    }
                } catch (IllegalArgumentException ignored) {
                    // Ignore stale or corrupted category references.
                }
            }
            if (!ids.isEmpty()) {
                database.keyToCategories.put(key, ids);
            }
        }
        return database;
    }

    private int assignmentCount() {
        var total = 0;
        for (var ids : keyToCategories.values()) {
            total += ids.size();
        }
        return total;
    }

    private void markModified() {
        lastModified = System.currentTimeMillis();
    }
}
