package org.lyy.mektmc.registries;

import appeng.api.networking.security.IActionHost;
import appeng.menu.implementations.MenuTypeBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lyy.mektmc.Mektmc;
import org.lyy.mektmc.menu.CategorizedTerminalMenu;
import org.lyy.mektmc.menu.CategoryIndexMenu;

public final class MenuRegistry {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(BuiltInRegistries.MENU, Mektmc.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<CategoryIndexMenu>> CATEGORY_INDEX =
            MENUS.register("category_index", () -> IMenuTypeExtension.create(CategoryIndexMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<CategorizedTerminalMenu>> CATEGORIZED_TERMINAL =
            MENUS.register("categorized_terminal", () -> MenuTypeBuilder
                    .<CategorizedTerminalMenu, IActionHost>create(CategorizedTerminalMenu::new, IActionHost.class)
                    .withMenuTitle(host -> Component.translatable("container.mektmc.categorized_terminal"))
                    .buildUnregistered(ResourceLocation.fromNamespaceAndPath(Mektmc.MODID, "categorized_terminal")));

    private MenuRegistry() {}
}
