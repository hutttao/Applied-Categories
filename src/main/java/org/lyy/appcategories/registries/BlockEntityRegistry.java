package org.lyy.appcategories.registries;

import appeng.api.AECapabilities;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lyy.appcategories.AppliedCategories;
import org.lyy.appcategories.blockentity.CategorizedTerminalBlockEntity;
import org.lyy.appcategories.blockentity.CategoryIndexBlockEntity;

public final class BlockEntityRegistry {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, AppliedCategories.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CategoryIndexBlockEntity>> CATEGORY_INDEX =
            BLOCK_ENTITY_TYPES.register("category_index", () ->
                    BlockEntityType.Builder.of(CategoryIndexBlockEntity::new, BlockRegistry.CATEGORY_INDEX.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CategorizedTerminalBlockEntity>> CATEGORIZED_TERMINAL =
            BLOCK_ENTITY_TYPES.register("categorized_terminal", () ->
                    BlockEntityType.Builder.of(CategorizedTerminalBlockEntity::new, BlockRegistry.CATEGORIZED_TERMINAL.get()).build(null));

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, CATEGORY_INDEX.get(), (blockEntity, context) -> blockEntity);
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, CATEGORIZED_TERMINAL.get(), (blockEntity, context) -> blockEntity);
    }

    private BlockEntityRegistry() {}
}
