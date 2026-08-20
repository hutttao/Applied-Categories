package org.lyy.mektmc.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CategoryDiskItem extends Item {
    private static final String DATABASE_ID_TAG = "category_database_id";

    public CategoryDiskItem(Properties properties) {
        super(properties);
    }

    public Optional<UUID> getDatabaseId(ItemStack stack) {
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!data.hasUUID(DATABASE_ID_TAG)) {
            return Optional.empty();
        }
        return Optional.of(data.getUUID(DATABASE_ID_TAG));
    }

    public UUID ensureDatabaseId(ItemStack stack) {
        var existing = getDatabaseId(stack);
        if (existing.isPresent()) {
            return existing.get();
        }
        var id = UUID.randomUUID();
        setDatabaseId(stack, id);
        return id;
    }

    public void setDatabaseId(ItemStack stack, UUID id) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putUUID(DATABASE_ID_TAG, id));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        getDatabaseId(stack).ifPresent(id -> tooltip.add(Component.translatable("tooltip.mektmc.category_disk.database", id.toString())));
    }
}
