package org.lyy.mektmc.ae.category;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

public final class CategoryDatabaseSavedData extends SavedData {
    private CategoryDatabase database;

    public CategoryDatabaseSavedData() {
        this(new CategoryDatabase());
    }

    public CategoryDatabaseSavedData(CategoryDatabase database) {
        this.database = database;
    }

    public CategoryDatabase database() {
        return database;
    }

    public void setDatabase(CategoryDatabase database) {
        this.database = database;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("database", database.save(registries));
        return tag;
    }

    public static CategoryDatabaseSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        return new CategoryDatabaseSavedData(CategoryDatabase.load(tag.getCompound("database"), registries));
    }
}
