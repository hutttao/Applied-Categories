package org.lyy.mektmc.ini;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lyy.mektmc.Mektmc;
import org.lyy.mektmc.registries.ItemRegistry;

public final class ModCreativeTab {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Mektmc.MODID);

    /** 独立创造标签页 "lyan"，代表物品为 AE2 无线终端（延迟解析，避免注册时序问题）。 */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> LYAN_TAB =
            TABS.register("lyan", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mektmc.lyan"))
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
