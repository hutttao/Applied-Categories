package org.lyy.mektmc.ae.category;

import java.util.UUID;

public final class CategoryIds {
    public static final UUID ALL = new UUID(0L, 0L);
    public static final UUID UNCATEGORIZED = new UUID(0L, 1L);

    private CategoryIds() {}

    public static boolean isUserCategory(UUID id) {
        return id != null && !ALL.equals(id) && !UNCATEGORIZED.equals(id);
    }
}
