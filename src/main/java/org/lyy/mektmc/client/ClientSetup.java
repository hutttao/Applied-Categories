package org.lyy.mektmc.client;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.lyy.mektmc.client.screen.CategorizedTerminalScreen;
import org.lyy.mektmc.client.screen.CategoryIndexScreen;
import org.lyy.mektmc.registries.MenuRegistry;

public final class ClientSetup {
    private ClientSetup() {}

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(MenuRegistry.CATEGORY_INDEX.get(), CategoryIndexScreen::new);
        event.register(MenuRegistry.CATEGORIZED_TERMINAL.get(), CategorizedTerminalScreen::new);
    }
}
