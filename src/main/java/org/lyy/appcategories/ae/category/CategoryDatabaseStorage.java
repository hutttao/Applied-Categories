package org.lyy.appcategories.ae.category;

import java.util.UUID;

public interface CategoryDatabaseStorage {
    CategoryDatabase load(UUID id);

    void save(UUID id, CategoryDatabase database);

    void markDirty(UUID id);

    boolean exists(UUID id);
}
