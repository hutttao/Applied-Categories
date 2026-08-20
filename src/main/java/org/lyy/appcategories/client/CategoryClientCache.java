package org.lyy.appcategories.client;

import org.lyy.appcategories.network.CategorySnapshotPacket;

public final class CategoryClientCache {
    private static CategorySnapshotPacket snapshot;

    private CategoryClientCache() {}

    public static void apply(CategorySnapshotPacket packet) {
        snapshot = packet;
    }

    public static CategorySnapshotPacket snapshot(int menuId) {
        return snapshot != null && snapshot.menuId() == menuId ? snapshot : null;
    }
}
