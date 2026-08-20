package org.lyy.mektmc;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import org.lyy.mektmc.ini.ModCreativeTab;
import org.lyy.mektmc.client.ClientSetup;
import org.lyy.mektmc.network.NetworkHandler;
import org.lyy.mektmc.parts.CategorizedTerminalPart;
import org.lyy.mektmc.registries.BlockEntityRegistry;
import org.lyy.mektmc.registries.BlockRegistry;
import org.lyy.mektmc.registries.ItemRegistry;
import org.lyy.mektmc.registries.MenuRegistry;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Mektmc.MODID)
public class Mektmc {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "mektmc";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public Mektmc(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        appeng.api.parts.PartModels.registerModels(
                CategorizedTerminalPart.MODEL_OFF,
                CategorizedTerminalPart.MODEL_ON,
                CategorizedTerminalPart.MODEL_BASE,
                CategorizedTerminalPart.MODEL_STATUS_OFF,
                CategorizedTerminalPart.MODEL_STATUS_ON,
                CategorizedTerminalPart.MODEL_STATUS_HAS_CHANNEL
        );

        BlockRegistry.BLOCKS.register(modEventBus);
        ItemRegistry.ITEMS.register(modEventBus);
        BlockEntityRegistry.BLOCK_ENTITY_TYPES.register(modEventBus);
        MenuRegistry.MENUS.register(modEventBus);
        ModCreativeTab.TABS.register(modEventBus);
        modEventBus.addListener(NetworkHandler::register);
        modEventBus.addListener(BlockEntityRegistry::registerCapabilities);
        modEventBus.addListener(ClientSetup::registerScreens);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");



    }

}
