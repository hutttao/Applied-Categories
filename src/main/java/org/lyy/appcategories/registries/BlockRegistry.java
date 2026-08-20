package org.lyy.appcategories.registries;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lyy.appcategories.AppliedCategories;
import org.lyy.appcategories.blocks.CategorizedTerminalBlock;
import org.lyy.appcategories.blocks.CategoryIndexBlock;

public final class BlockRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AppliedCategories.MODID);

    public static final DeferredBlock<Block> CATEGORY_INDEX =
            BLOCKS.register("category_index", () -> new CategoryIndexBlock(BlockBehaviour.Properties.of()
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> CATEGORIZED_TERMINAL =
            BLOCKS.register("categorized_terminal", () -> new CategorizedTerminalBlock(BlockBehaviour.Properties.of()
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    private BlockRegistry() {}
}
