package org.lyy.mektmc.ae.category;

import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SavedDataCategoryDatabaseStorage implements CategoryDatabaseStorage {
    private static final String FILE_PREFIX = "mektmc_category_database_";
    private static final SavedData.Factory<CategoryDatabaseSavedData> FACTORY =
            new SavedData.Factory<>(CategoryDatabaseSavedData::new, CategoryDatabaseSavedData::load);

    private final DimensionDataStorage storage;
    private final Map<UUID, CategoryDatabaseSavedData> loaded = new HashMap<>();

    public SavedDataCategoryDatabaseStorage(ServerLevel level) {
        this.storage = level.getServer().overworld().getDataStorage();
    }

    @Override
    public CategoryDatabase load(UUID id) {
        return data(id).database();
    }

    @Override
    public void save(UUID id, CategoryDatabase database) {
        data(id).setDatabase(database);
    }

    @Override
    public void markDirty(UUID id) {
        data(id).setDirty();
    }

    @Override
    public boolean exists(UUID id) {
        return storage.get(FACTORY, fileId(id)) != null;
    }

    private CategoryDatabaseSavedData data(UUID id) {
        return loaded.computeIfAbsent(id, key -> storage.computeIfAbsent(FACTORY, fileId(key)));
    }

    private static String fileId(UUID id) {
        return FILE_PREFIX + id;
    }
}
