package org.lyy.mektmc.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.lyy.mektmc.menu.CategoryIndexMenu;

public class CategoryIndexScreen extends AbstractContainerScreen<CategoryIndexMenu> {
    private static final ResourceLocation AE2_ME_CHEST =
            ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/me_chest.png");

    public CategoryIndexScreen(CategoryIndexMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 168;
        inventoryLabelY = 73;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // 父类 AbstractContainerScreen.render 不保证调用 renderTooltip（NeoForge 内联版），
        // 这里显式兜底绘制槽位 tooltip，确保磁盘/背包物品悬停时显示具体信息。
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(AE2_ME_CHEST, x, y, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0xFF404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF404040, false);
    }
}
