package org.lyy.mektmc.ae.category;

import appeng.api.stacks.AEKey;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public final class CategoryKeySerializer {
    private CategoryKeySerializer() {}

    public static CompoundTag write(AEKey key, HolderLookup.Provider registries) {
        return key.toTagGeneric(registries);
    }

    public static AEKey read(CompoundTag tag, HolderLookup.Provider registries) {
        return AEKey.fromTagGeneric(registries, tag);
    }
}
