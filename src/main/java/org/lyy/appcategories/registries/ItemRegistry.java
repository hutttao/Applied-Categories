package org.lyy.appcategories.registries;

import appeng.items.parts.PartItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lyy.appcategories.AppliedCategories;
import org.lyy.appcategories.items.CategoryDiskItem;
import org.lyy.appcategories.parts.CategorizedTerminalPart;

public final class ItemRegistry {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AppliedCategories.MODID);

    public static final DeferredItem<Item> CATEGORY_DISK =
            ITEMS.register("category_disk", () -> new CategoryDiskItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> CATEGORY_INDEX =
            ITEMS.register("category_index", () -> new BlockItem(BlockRegistry.CATEGORY_INDEX.get(), new Item.Properties()));

    public static final DeferredItem<Item> CATEGORIZED_TERMINAL =
            ITEMS.register("categorized_terminal", () -> new PartItem<>(new Item.Properties(), CategorizedTerminalPart.class, CategorizedTerminalPart::new));

    private ItemRegistry() {}
}
