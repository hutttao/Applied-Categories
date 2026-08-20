package org.lyy.appcategories.ini;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lyy.appcategories.AppliedCategories;
import org.lyy.appcategories.registries.ItemRegistry;

public final class ModCreativeTab {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AppliedCategories.MODID);

    /** Applied Categories 创造标签页，代表物品为 AE2 无线终端（延迟解析，避免注册时序问题）。 */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> APPLIED_CATEGORIES_TAB =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.appcategories.main"))
                    .icon(() -> new ItemStack(BuiltInRegistries.ITEM.get(
                            ResourceLocation.fromNamespaceAndPath("ae2", "wireless_terminal"))))
                    .displayItems((params, output) -> {
                        output.accept(ItemRegistry.CATEGORY_INDEX.get());
                        output.accept(ItemRegistry.CATEGORIZED_TERMINAL.get());
                        output.accept(ItemRegistry.CATEGORY_DISK.get());
                    })
                    .build());

    private ModCreativeTab() {}
}
