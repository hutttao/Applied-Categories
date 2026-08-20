package org.lyy.mektmc.ae.category;

import java.util.UUID;

public record CategoryDef(UUID id, String name, int color, int sortOrder, boolean builtin) {
    public CategoryDef {
        if (id == null) {
            throw new IllegalArgumentException("Category id cannot be null");
        }
        name = sanitizeName(name);
    }

    public CategoryDef renamed(String newName) {
        return new CategoryDef(id, newName, color, sortOrder, builtin);
    }

    private static String sanitizeName(String name) {
        if (name == null) {
            return "";
        }
        var trimmed = name.trim();
        return trimmed.length() > CategoryLimits.MAX_NAME_LENGTH
                ? trimmed.substring(0, CategoryLimits.MAX_NAME_LENGTH)
                : trimmed;
    }
}
