package org.lyy.appcategories.parts;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.items.parts.PartModels;
import appeng.parts.PartModel;
import appeng.parts.reporting.AbstractTerminalPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import org.lyy.appcategories.AppliedCategories;
import org.lyy.appcategories.registries.MenuRegistry;

public class CategorizedTerminalPart extends AbstractTerminalPart {
    @PartModels
    public static final ResourceLocation MODEL_OFF = ResourceLocation.fromNamespaceAndPath(AppliedCategories.MODID, "part/terminal_off");
    @PartModels
    public static final ResourceLocation MODEL_ON = ResourceLocation.fromNamespaceAndPath(AppliedCategories.MODID, "part/terminal_on");
    @PartModels
    public static final ResourceLocation MODEL_BASE = ResourceLocation.fromNamespaceAndPath(AppliedCategories.MODID, "part/display_base");
    @PartModels
    public static final ResourceLocation MODEL_STATUS_OFF = ResourceLocation.fromNamespaceAndPath(AppliedCategories.MODID, "part/display_status_off");
    @PartModels
    public static final ResourceLocation MODEL_STATUS_ON = ResourceLocation.fromNamespaceAndPath(AppliedCategories.MODID, "part/display_status_on");
    @PartModels
    public static final ResourceLocation MODEL_STATUS_HAS_CHANNEL = ResourceLocation.fromNamespaceAndPath(AppliedCategories.MODID, "part/display_status_has_channel");

    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF, MODEL_STATUS_OFF);
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_ON);
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_HAS_CHANNEL);

    public CategorizedTerminalPart(IPartItem<?> partItem) {
        super(partItem);
    }

    @Override
    public IPartModel getStaticModels() {
        return selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }

    @Override
    public MenuType<?> getMenuType(Player player) {
        return MenuRegistry.CATEGORIZED_TERMINAL.get();
    }
}
