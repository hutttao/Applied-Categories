package org.lyy.appcategories.client.screen;

import appeng.api.config.TerminalStyle;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AEKeyTypes;
import appeng.api.stacks.AmountFormat;
import appeng.client.gui.me.common.StackSizeRenderer;
import appeng.client.Point;
import appeng.client.gui.widgets.Scrollbar;
import appeng.core.AEConfig;
import appeng.core.AppEng;
import appeng.core.network.serverbound.SwitchGuisPacket;
import appeng.integration.abstraction.ItemListMod;
import appeng.menu.me.crafting.CraftingStatusMenu;
import guideme.PageAnchor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import com.mojang.blaze3d.platform.InputConstants;
import org.lyy.appcategories.ae.category.CategoryIds;
import org.lyy.appcategories.client.CategoryClientCache;
import org.lyy.appcategories.menu.CategorizedTerminalMenu;
import org.lyy.appcategories.network.CategoryAssignPacket;
import org.lyy.appcategories.network.CategoryCreatePacket;
import org.lyy.appcategories.network.CategoryDeletePacket;
import org.lyy.appcategories.network.CategoryExtractPacket;
import org.lyy.appcategories.network.CategoryInsertCarriedPacket;
import org.lyy.appcategories.network.CategoryRemovePacket;
import org.lyy.appcategories.network.CategoryRequestSnapshotPacket;
import org.lyy.appcategories.network.CategorySetActivePacket;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.lang.reflect.Field;

public class CategorizedTerminalScreen extends AbstractContainerScreen<CategorizedTerminalMenu> {
    private static final ResourceLocation AE2_TERMINAL =
            ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/terminal.png");
    private static final ResourceLocation AE2_STATES =
            ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/states.png");
    private static final ResourceLocation AE2_TEXT_FIELD =
            ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/text_field.png");
    private static final ResourceLocation AE2_BACKGROUND =
            ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/background.png");
    private static final ResourceLocation AE2_CHECKBOX =
            ResourceLocation.fromNamespaceAndPath("ae2", "textures/guis/checkbox.png");
    private static final int HEADER_HEIGHT = 17;
    private static final int ROW_HEIGHT = 18;
    private static final int BOTTOM_HEIGHT = 99;
    private static final int GRID_LEFT = 8;
    private static final int GRID_TOP = 18;
    private static final int GRID_COLUMNS = 9;
    private static final int SEARCH_LEFT = 80;
    private static final int SEARCH_TOP = 4;
    private static final int SEARCH_WIDTH = 89;
    private static final int SEARCH_HEIGHT = 12;
    private static final int CATEGORY_TOP_OFFSET = -22;
    private static final int CATEGORY_LEFT_OFFSET = 4;
    private static final int CATEGORY_WIDTH = 22;
    private static final int CATEGORY_HEIGHT = 22;
    private static final int TOOLBAR_X_OFFSET = -22;
    private static final int TOOLBAR_Y_OFFSET = 4;
    private static final int TOOLBAR_STEP = 22;
    private static final int TOOLBAR_WIDTH = 18;
    private static final int TOOLBAR_HEIGHT = 20;
    private static final int CRAFTING_STATUS_LEFT = 171;
    private static final int CRAFTING_STATUS_TOP = -5;
    private static final int CRAFTING_STATUS_WIDTH = 20;
    private static final int CRAFTING_STATUS_HEIGHT = 20;
    private static final int SETTINGS_WIDTH = 200;
    private static final int SETTINGS_HEIGHT = 216;
    private static final Field SLOT_X_FIELD = findSlotField("x");
    private static final Field SLOT_Y_FIELD = findSlotField("y");
    private UUID activeCategory = CategoryIds.ALL;
    private String searchText = "";
    private EditBox searchField;
    private SortMode sortMode = SortMode.NAME;
    private ViewMode viewMode = ViewMode.ALL;
    private AEKeyType selectedKeyType;
    private boolean sortAscending = true;
    private TerminalStyleButton terminalStyle;
    private ToolbarAction focusedToolbarAction;
    private boolean settingsOpen;
    /** AE2 原版滚动条组件（big_scroller 滑块，渲染/交互全部由 AE2 处理，与原版终端一致）。 */
    private final Scrollbar scrollbar = new Scrollbar(Scrollbar.BIG);
    private int gridRows = 4;
    private int categoryScroll;
    private boolean craftingStatusFocused;

    public CategorizedTerminalScreen(CategorizedTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        terminalStyle = TerminalStyleButton.from(AEConfig.instance().getTerminalStyle());
        updateTerminalDimensions();
    }

    @Override
    protected void init() {
        updateTerminalDimensions();
        super.init();
        updateScrollbarLayout();
        positionPlayerInventorySlots();
        String oldSearch = searchText;
        searchField = new EditBox(font,
                leftPos + SEARCH_LEFT + 2,
                topPos + SEARCH_TOP + 2,
                SEARCH_WIDTH - 4 - font.width("_"),
                SEARCH_HEIGHT - 4,
                Component.empty());
        searchField.setBordered(false);
        searchField.setTextColor(0xFFFFFFFF);
        searchField.setTextColorUneditable(0xFFB8B8B8);
        searchField.setMaxLength(80);
        searchField.setValue(oldSearch);
        searchField.setResponder(value -> searchText = value);
        addRenderableWidget(searchField);
        PacketDistributor.sendToServer(new CategoryRequestSnapshotPacket(menu.containerId));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // 兜底：父类不保证调用 renderTooltip，槽位（背包/磁盘）悬停时显式绘制 tooltip
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        if (settingsOpen) {
            drawTerminalSettingsPanel(guiGraphics, mouseX, mouseY);
        }
        renderVirtualTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        drawAe2TerminalBackground(guiGraphics, x, y);
        drawAe2Toolbars(guiGraphics, x, y, mouseX, mouseY);
        drawAe2Search(guiGraphics, x + SEARCH_LEFT, y + SEARCH_TOP);

        var snapshot = CategoryClientCache.snapshot(menu.containerId);
        if (snapshot == null) {
            guiGraphics.drawString(font, Component.translatable("screen.appcategories.categorized_terminal.loading"), x + 10, y + 60, 0xFFD6E5EE, false);
            return;
        }
        // 设置面板打开时隐藏分类栏与物品网格，避免物品透过面板显示
        if (settingsOpen) {
            return;
        }
        activeCategory = snapshot.activeCategory();
        drawCategoryButtons(guiGraphics, mouseX, mouseY, snapshot);

        if (snapshot.status() != org.lyy.appcategories.network.CategorySnapshotPacket.Status.OK) {
            guiGraphics.drawString(font, Component.translatable("screen.appcategories.categorized_terminal.status." + snapshot.status().name().toLowerCase()), x + 10, y + 60, 0xFFFF7777, false);
        }
        drawStacks(guiGraphics, mouseX, mouseY, snapshot);
        drawHoveredGridSlot(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0xFF404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF404040, false);
    }

    private void drawCategoryButtons(GuiGraphics guiGraphics, int mouseX, int mouseY, org.lyy.appcategories.network.CategorySnapshotPacket snapshot) {
        clampCategoryScroll(snapshot);
        int x = categoryBarX();
        int y = categoryBarY();
        x = drawCategoryButton(guiGraphics, x, y, "全", CategoryIds.ALL.equals(activeCategory), mouseX, mouseY);
        x = drawCategoryButton(guiGraphics, x, y, "未", CategoryIds.UNCATEGORIZED.equals(activeCategory), mouseX, mouseY);

        int visibleSlots = visibleUserCategorySlots(snapshot);
        for (int i = categoryScroll; i < snapshot.categories().size() && i < categoryScroll + visibleSlots; i++) {
            var category = snapshot.categories().get(i);
            x = drawCategoryButton(guiGraphics, x, y, Integer.toString(i + 1), category.id().equals(activeCategory), mouseX, mouseY);
        }

        int controlsX = categoryControlsX(snapshot);
        if (snapshot.status() == org.lyy.appcategories.network.CategorySnapshotPacket.Status.OK) {
            drawCategoryButton(guiGraphics, controlsX, y, "+", false, mouseX, mouseY);
            controlsX += CATEGORY_WIDTH;
        }
        if (isSelectedKnownUserCategory(snapshot)) {
            drawCategoryIconButton(guiGraphics, controlsX, y, 96, 0, false, mouseX, mouseY);
        }
    }

    private int drawCategoryButton(GuiGraphics guiGraphics, int x, int y, String text, boolean selected, int mouseX, int mouseY) {
        boolean hovered = isWithin(mouseX, mouseY, x, y, CATEGORY_WIDTH, CATEGORY_HEIGHT);
        int srcV = selected ? 150 : hovered ? 128 : 128;
        int srcU = hovered && !selected ? 150 : 128;
        drawIcon(guiGraphics, x, y, srcU, srcV, CATEGORY_WIDTH, CATEGORY_HEIGHT);
        guiGraphics.drawCenteredString(font, text, x + 11, y + 7 + (hovered && !selected ? 1 : 0), selected ? 0xFF000000 : 0xFF404040);
        return x + CATEGORY_WIDTH;
    }

    private void drawCategoryIconButton(GuiGraphics guiGraphics, int x, int y, int iconU, int iconV, boolean selected, int mouseX, int mouseY) {
        boolean hovered = isWithin(mouseX, mouseY, x, y, CATEGORY_WIDTH, CATEGORY_HEIGHT);
        int srcV = selected ? 150 : 128;
        int srcU = hovered && !selected ? 150 : 128;
        drawIcon(guiGraphics, x, y, srcU, srcV, CATEGORY_WIDTH, CATEGORY_HEIGHT);
        drawIcon(guiGraphics, x + 3, y + 3 + (hovered && !selected ? 1 : 0), iconU, iconV, 16, 16);
    }

    private void drawStacks(GuiGraphics guiGraphics, int mouseX, int mouseY, org.lyy.appcategories.network.CategorySnapshotPacket snapshot) {
        int startX = leftPos + GRID_LEFT;
        int startY = topPos + GRID_TOP;
        var stacks = visibleStacks(snapshot);
        int maxScroll = maxGridScroll(stacks.size());
        scrollbar.setRange(0, maxScroll, 1);
        int start = scrollbar.getCurrentScroll() * GRID_COLUMNS;
        int end = Math.min(stacks.size(), start + gridLimit());
        for (int i = start; i < end; i++) {
            var entry = stacks.get(i);
            int idx = i - start;
            int x = startX + (idx % GRID_COLUMNS) * 18;
            int y = startY + (idx / GRID_COLUMNS) * 18;
            var display = entry.key().wrapForDisplayOrFilter();
            guiGraphics.renderItem(display, x, y);
            // 与 AE2 原版终端完全一致：AEKey.formatAmount + StackSizeRenderer
            // （AmountFormat.SLOT = 最多 4 字符缩写，如 1.23K / 2.2Q，永不溢出格子）
            var text = entry.key().formatAmount(entry.amount(), AmountFormat.SLOT);
            StackSizeRenderer.renderSizeLabel(guiGraphics, font, x, y, text);
        }
        // 使用 AE2 原版滚动条组件渲染（big_scroller 滑块；无法翻页时 AE2 自动绘制禁用态滑块，与原版一致）
        scrollbar.drawForegroundLayer(guiGraphics, scrollbar.getBounds(), new Point(mouseX, mouseY));
    }

    /** 网格总行数对应的最大滚动行数。 */
    private int maxGridScroll(int totalStacks) {
        return Math.max(0, (totalStacks + GRID_COLUMNS - 1) / GRID_COLUMNS - gridRows);
    }

    /** 滚动条位置与高度：坐标取自 AE2 官方布局（assets/ae2/screens/terminals/base_terminal.json，left=175, top=18）。 */
    private void updateScrollbarLayout() {
        scrollbar.setPosition(new Point(leftPos + 175, topPos + GRID_TOP));
        scrollbar.setHeight(gridRows * ROW_HEIGHT);
    }

    private boolean isScrollbarArea(double mouseX, double mouseY) {
        Rect2i bounds = scrollbar.getBounds();
        return mouseX >= bounds.getX() && mouseX < bounds.getX() + bounds.getWidth()
                && mouseY >= bounds.getY() && mouseY < bounds.getY() + bounds.getHeight();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isSearchBox(mouseX, mouseY)) {
            if (button == 1 && searchField != null) {
                searchField.setValue("");
            }
            setSearchFocused(true);
            if (searchField != null) {
                searchField.mouseClicked(mouseX, mouseY, button);
            }
            return true;
        }
        setSearchFocused(false);

        if (isCraftingStatusButton(mouseX, mouseY)) {
            if (button == 0 || button == 1) {
                craftingStatusFocused = true;
                openCraftingStatus();
                return true;
            }
            return true;
        }

        ToolbarAction toolbarAction = toolbarActionAt(mouseX, mouseY);
        if (toolbarAction != null) {
            if (button == 0 || button == 1) {
                focusedToolbarAction = toolbarAction;
                handleToolbarClick(toolbarAction, button == 1);
                return true;
            }
            return true;
        }
        focusedToolbarAction = null;
        craftingStatusFocused = false;

        if (settingsOpen) {
            // 设置面板打开时所有点击归面板处理；点击面板外=关闭面板，不穿透到物品操作
            handleSettingsClick(mouseX, mouseY, button);
            return true;
        }

        var snapshot = CategoryClientCache.snapshot(menu.containerId);
        if (snapshot != null && isScrollbarArea(mouseX, mouseY) && scrollbar.onMouseDown(new Point((int) mouseX, (int) mouseY), button)) {
            return true;
        }
        if (snapshot != null) {
            UUID clicked = tabAt(mouseX, mouseY, snapshot);
            if (clicked != null) {
                if (button == 0 || button == 1) {
                    playToolbarClick();
                    activeCategory = clicked;
                    PacketDistributor.sendToServer(new CategorySetActivePacket(menu.containerId, clicked));
                }
                return true;
            }
            if (button == 0 && isCreateCategoryButton(mouseX, mouseY, snapshot)) {
                playToolbarClick();
                PacketDistributor.sendToServer(new CategoryCreatePacket(menu.containerId, Component.translatable("screen.appcategories.categorized_terminal.default_category_name", snapshot.categories().size() + 1).getString(), 0x4F7FA3));
                return true;
            }
            if (button == 0 && isDeleteCategoryButton(mouseX, mouseY, snapshot) && CategoryIds.isUserCategory(activeCategory)) {
                playToolbarClick();
                PacketDistributor.sendToServer(new CategoryDeletePacket(menu.containerId, activeCategory));
                activeCategory = CategoryIds.ALL;
                return true;
            }
        }

        AEKey key = stackAt(mouseX, mouseY, snapshot);
        if (key != null) {
            if (CategoryIds.isUserCategory(activeCategory) && hasControlDown()) {
                PacketDistributor.sendToServer(new CategoryAssignPacket(menu.containerId, key, activeCategory));
                return true;
            }
            if (CategoryIds.isUserCategory(activeCategory) && hasAltDown()) {
                PacketDistributor.sendToServer(new CategoryRemovePacket(menu.containerId, key, activeCategory));
                return true;
            }
            if (button == 0 || button == 1 || button == 2) {
                PacketDistributor.sendToServer(new CategoryExtractPacket(menu.containerId, key, button, hasShiftDown()));
                return true;
            }
            return true;
        }
        if (isGridArea(mouseX, mouseY)
                && !hasShiftDown()
                && !menu.getCarried().isEmpty()
                && (button == 0 || button == 1)) {
            PacketDistributor.sendToServer(new CategoryInsertCarriedPacket(menu.containerId, button == 1));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        var snapshot = CategoryClientCache.snapshot(menu.containerId);
        if (deltaY != 0.0D && snapshot != null && isCategoryBar(mouseX, mouseY)) {
            int visibleSlots = visibleUserCategorySlots(snapshot);
            int maxScroll = Math.max(0, snapshot.categories().size() - visibleSlots);
            if (maxScroll > 0) {
                categoryScroll += deltaY < 0.0D ? 1 : -1;
                categoryScroll = Math.max(0, Math.min(maxScroll, categoryScroll));
                return true;
            }
        }
        if (deltaY != 0.0D && hasShiftDown() && isGridArea(mouseX, mouseY)) {
            AEKey key = stackAt(mouseX, mouseY, snapshot);
            int times = Math.max(1, (int) Math.abs(deltaY));
            for (int i = 0; i < times; i++) {
                if (deltaY > 0.0D) {
                    if (key != null) {
                        PacketDistributor.sendToServer(new CategoryExtractPacket(menu.containerId, key, 3, false));
                    } else if (!menu.getCarried().isEmpty()) {
                        PacketDistributor.sendToServer(new CategoryInsertCarriedPacket(menu.containerId, true));
                    }
                } else if (key != null) {
                    PacketDistributor.sendToServer(new CategoryExtractPacket(menu.containerId, key, 4, false));
                }
            }
            return true;
        }
        if (deltaY != 0.0D && !hasShiftDown() && isGridArea(mouseX, mouseY) && snapshot != null) {
            if (scrollbar.onMouseWheel(new Point((int) mouseX, (int) mouseY), deltaY)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollbar.onMouseDrag(new Point((int) mouseX, (int) mouseY), button)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        scrollbar.onMouseUp(new Point((int) mouseX, (int) mouseY), button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        scrollbar.tick();
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchField != null && searchField.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchField != null && searchField.isFocused()) {
            if (keyCode == InputConstants.KEY_ESCAPE || keyCode == InputConstants.KEY_RETURN) {
                setSearchFocused(false);
                return true;
            }
            if (searchField.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private UUID tabAt(double mouseX, double mouseY, org.lyy.appcategories.network.CategorySnapshotPacket snapshot) {
        int x = categoryBarX();
        int y = categoryBarY();
        if (isWithin(mouseX, mouseY, x, y, CATEGORY_WIDTH, CATEGORY_HEIGHT)) {
            return CategoryIds.ALL;
        }
        x += CATEGORY_WIDTH;
        if (isWithin(mouseX, mouseY, x, y, CATEGORY_WIDTH, CATEGORY_HEIGHT)) {
            return CategoryIds.UNCATEGORIZED;
        }
        x += CATEGORY_WIDTH;
        int visibleSlots = visibleUserCategorySlots(snapshot);
        for (int i = categoryScroll; i < snapshot.categories().size() && i < categoryScroll + visibleSlots; i++) {
            var category = snapshot.categories().get(i);
            if (isWithin(mouseX, mouseY, x, y, CATEGORY_WIDTH, CATEGORY_HEIGHT)) {
                return category.id();
            }
            x += CATEGORY_WIDTH;
        }
        return null;
    }

    private boolean isCreateCategoryButton(double mouseX, double mouseY, org.lyy.appcategories.network.CategorySnapshotPacket snapshot) {
        if (snapshot.status() != org.lyy.appcategories.network.CategorySnapshotPacket.Status.OK) {
            return false;
        }
        return isWithin(mouseX, mouseY, categoryControlsX(snapshot), categoryBarY(), CATEGORY_WIDTH, CATEGORY_HEIGHT);
    }

    private boolean isDeleteCategoryButton(double mouseX, double mouseY, org.lyy.appcategories.network.CategorySnapshotPacket snapshot) {
        if (!isSelectedKnownUserCategory(snapshot)) {
            return false;
        }
        int x = categoryControlsX(snapshot);
        if (snapshot.status() == org.lyy.appcategories.network.CategorySnapshotPacket.Status.OK) {
            x += CATEGORY_WIDTH;
        }
        return isWithin(mouseX, mouseY, x, categoryBarY(), CATEGORY_WIDTH, CATEGORY_HEIGHT);
    }

    private int categoryBarX() {
        return leftPos + CATEGORY_LEFT_OFFSET;
    }

    private int categoryBarY() {
        return Math.max(2, topPos + CATEGORY_TOP_OFFSET);
    }

    private int categoryBarRight() {
        return leftPos + imageWidth + 24;
    }

    private boolean isCategoryBar(double mouseX, double mouseY) {
        return isWithin(mouseX, mouseY, categoryBarX(), categoryBarY(), categoryBarRight() - categoryBarX(), CATEGORY_HEIGHT);
    }

    private int categoryControlCount(org.lyy.appcategories.network.CategorySnapshotPacket snapshot) {
        int count = snapshot.status() == org.lyy.appcategories.network.CategorySnapshotPacket.Status.OK ? 1 : 0;
        if (isSelectedKnownUserCategory(snapshot)) {
            count++;
        }
        return count;
    }

    private int visibleUserCategorySlots(org.lyy.appcategories.network.CategorySnapshotPacket snapshot) {
        int totalSlots = Math.max(0, (categoryBarRight() - categoryBarX()) / CATEGORY_WIDTH);
        return Math.max(0, totalSlots - 2 - categoryControlCount(snapshot));
    }

    private int categoryControlsX(org.lyy.appcategories.network.CategorySnapshotPacket snapshot) {
        return categoryBarRight() - categoryControlCount(snapshot) * CATEGORY_WIDTH;
    }

    private void clampCategoryScroll(org.lyy.appcategories.network.CategorySnapshotPacket snapshot) {
        int maxScroll = Math.max(0, snapshot.categories().size() - visibleUserCategorySlots(snapshot));
        categoryScroll = Math.max(0, Math.min(maxScroll, categoryScroll));
    }

    private boolean isSelectedKnownUserCategory(org.lyy.appcategories.network.CategorySnapshotPacket snapshot) {
        if (!CategoryIds.isUserCategory(activeCategory)) {
            return false;
        }
        for (var category : snapshot.categories()) {
            if (category.id().equals(activeCategory)) {
                return true;
            }
        }
        return false;
    }

    private boolean isGridArea(double mouseX, double mouseY) {
        int startX = leftPos + GRID_LEFT;
        int startY = topPos + GRID_TOP;
        return mouseX >= startX
                && mouseX < startX + GRID_COLUMNS * 18
                && mouseY >= startY
                && mouseY < startY + gridRows * 18;
    }

    private AEKey stackAt(double mouseX, double mouseY, org.lyy.appcategories.network.CategorySnapshotPacket snapshot) {
        if (snapshot == null) {
            return null;
        }
        if (!isGridArea(mouseX, mouseY)) {
            return null;
        }
        int startX = leftPos + GRID_LEFT;
        int startY = topPos + GRID_TOP;
        int col = (int) ((mouseX - startX) / 18);
        int row = (int) ((mouseY - startY) / 18);
        if (col < 0 || col >= GRID_COLUMNS || row < 0 || row >= gridRows) {
            return null;
        }
        var stacks = visibleStacks(snapshot);
        int index = (scrollbar.getCurrentScroll() + row) * GRID_COLUMNS + col;
        if (index >= stacks.size()) {
            return null;
        }
        return stacks.get(index).key();
    }

    private List<org.lyy.appcategories.network.CategorySnapshotPacket.StackEntry> visibleStacks(org.lyy.appcategories.network.CategorySnapshotPacket snapshot) {
        var needle = searchText.toLowerCase(java.util.Locale.ROOT);
        var result = new ArrayList<org.lyy.appcategories.network.CategorySnapshotPacket.StackEntry>();
        for (var entry : snapshot.stacks()) {
            if (viewMode == ViewMode.CRAFTABLE || !acceptsSelectedType(entry.key())) {
                continue;
            }
            var display = entry.key().wrapForDisplayOrFilter();
            if (needle.isBlank() || display.getHoverName().getString().toLowerCase(java.util.Locale.ROOT).contains(needle)) {
                result.add(entry);
            }
        }
        result.sort(sortComparator());
        if (!sortAscending) {
            java.util.Collections.reverse(result);
        }
        return result;
    }

    private Comparator<org.lyy.appcategories.network.CategorySnapshotPacket.StackEntry> sortComparator() {
        return switch (sortMode) {
            case NAME -> Comparator.comparing(entry -> entry.key().wrapForDisplayOrFilter().getHoverName().getString(), String.CASE_INSENSITIVE_ORDER);
            case AMOUNT -> Comparator.comparingLong(org.lyy.appcategories.network.CategorySnapshotPacket.StackEntry::amount);
            case MOD -> Comparator.comparing(entry -> {
                ItemStack display = entry.key().wrapForDisplayOrFilter();
                return BuiltInRegistries.ITEM.getKey(display.getItem()).getNamespace();
            }, String.CASE_INSENSITIVE_ORDER);
        };
    }

    private boolean acceptsSelectedType(AEKey key) {
        return selectedKeyType == null || key.getType() == selectedKeyType;
    }

    private int gridLimit() {
        return GRID_COLUMNS * gridRows;
    }

    private void updateTerminalDimensions() {
        int maxRows = height <= 0 ? 4 : Math.max(4, (height - 32 - HEADER_HEIGHT - BOTTOM_HEIGHT) / ROW_HEIGHT);
        gridRows = canRepositionSlots() ? Math.max(4, terminalStyle.rows(maxRows)) : 4;
        imageWidth = 195;
        imageHeight = HEADER_HEIGHT + ROW_HEIGHT * gridRows + BOTTOM_HEIGHT;
        inventoryLabelY = HEADER_HEIGHT + ROW_HEIGHT * gridRows + 4;
    }

    private void rebuildTerminalLayout() {
        updateTerminalDimensions();
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;
        updateScrollbarLayout();
        positionPlayerInventorySlots();
        if (searchField != null) {
            searchField.setX(leftPos + SEARCH_LEFT + 2);
            searchField.setY(topPos + SEARCH_TOP + 2);
        }
    }

    private void positionPlayerInventorySlots() {
        int top = HEADER_HEIGHT + ROW_HEIGHT * gridRows + 15;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9;
                if (slotIndex < menu.slots.size()) {
                    setSlotPosition(menu.slots.get(slotIndex), 8 + col * 18, top + row * 18);
                }
            }
        }
        for (int col = 0; col < 9; col++) {
            int slotIndex = 27 + col;
            if (slotIndex < menu.slots.size()) {
                setSlotPosition(menu.slots.get(slotIndex), 8 + col * 18, top + 58);
            }
        }
    }

    private static boolean canRepositionSlots() {
        return SLOT_X_FIELD != null && SLOT_Y_FIELD != null;
    }

    private static Field findSlotField(String name) {
        try {
            Field field = Slot.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException ignored) {
            return null;
        }
    }

    private static void setSlotPosition(Slot slot, int x, int y) {
        if (!canRepositionSlots()) {
            return;
        }
        try {
            SLOT_X_FIELD.setInt(slot, x);
            SLOT_Y_FIELD.setInt(slot, y);
        } catch (IllegalAccessException ignored) {
        }
    }

    private void drawAe2TerminalBackground(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(AE2_TERMINAL, x, y, 0.0F, 0.0F, 195, HEADER_HEIGHT, 256, 256);
        y += HEADER_HEIGHT;
        guiGraphics.blit(AE2_TERMINAL, x, y, 0.0F, 17.0F, 195, ROW_HEIGHT, 256, 256);
        y += ROW_HEIGHT;
        for (int row = 2; row < gridRows; row++) {
            guiGraphics.blit(AE2_TERMINAL, x, y, 0.0F, 35.0F, 195, ROW_HEIGHT, 256, 256);
            y += ROW_HEIGHT;
        }
        guiGraphics.blit(AE2_TERMINAL, x, y, 0.0F, 53.0F, 195, ROW_HEIGHT, 256, 256);
        y += ROW_HEIGHT;
        guiGraphics.blit(AE2_TERMINAL, x, y, 0.0F, 71.0F, 195, BOTTOM_HEIGHT, 256, 256);
    }

    private void drawAe2Toolbars(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        int left = x + TOOLBAR_X_OFFSET;
        int buttonY = y + TOOLBAR_Y_OFFSET;
        for (ToolbarAction action : ToolbarAction.values()) {
            drawToolbarButton(guiGraphics, left, buttonY, action.iconU(this), action.iconV(this), mouseX, mouseY,
                    focusedToolbarAction == action);
            buttonY += TOOLBAR_STEP;
        }

        int craftingX = x + CRAFTING_STATUS_LEFT;
        int craftingY = y + CRAFTING_STATUS_TOP;
        boolean craftingHovered = isCraftingStatusButton(mouseX, mouseY);
        drawIcon(guiGraphics, craftingX, craftingY, 160, craftingHovered || craftingStatusFocused ? 224 : 192,
                craftingHovered || craftingStatusFocused ? 22 : CRAFTING_STATUS_WIDTH,
                craftingHovered || craftingStatusFocused ? 22 : CRAFTING_STATUS_HEIGHT);
        drawIcon(guiGraphics, craftingX + 2, craftingY + 1, 48, 144, 16, 16);
        if (menu.activeCraftingJobs != -1) {
            int labelX = craftingX + (CRAFTING_STATUS_WIDTH - 18) / 2;
            int labelY = craftingY + (CRAFTING_STATUS_HEIGHT - 18) / 2;
            StackSizeRenderer.renderSizeLabel(guiGraphics, font, labelX, labelY, Integer.toString(menu.activeCraftingJobs));
        }
    }

    private static void drawToolbarButton(GuiGraphics guiGraphics, int x, int y, int iconU, int iconV, int mouseX, int mouseY, boolean focused) {
        boolean hovered = isWithin(mouseX, mouseY, x, y, TOOLBAR_WIDTH, TOOLBAR_HEIGHT);
        int offset = hovered ? 1 : 0;
        int bgU = hovered ? 212 : focused ? 194 : 176;
        drawIcon(guiGraphics, x, y + offset, bgU, 128, TOOLBAR_WIDTH, TOOLBAR_HEIGHT);
        drawIcon(guiGraphics, x + 1, y + 2 + offset, iconU, iconV, 16, 16);
    }

    private void drawAe2Search(GuiGraphics guiGraphics, int x, int y) {
        boolean focused = searchField != null && searchField.isFocused();
        int srcY = focused ? 24 : 0;
        guiGraphics.blit(AE2_TEXT_FIELD, x, y, 0.0F, srcY, 1, SEARCH_HEIGHT, 128, 128);
        guiGraphics.blit(AE2_TEXT_FIELD, x + 1, y, 1.0F, srcY, SEARCH_WIDTH - 2, SEARCH_HEIGHT, 128, 128);
        guiGraphics.blit(AE2_TEXT_FIELD, x + SEARCH_WIDTH - 1, y, 127.0F, srcY, 1, SEARCH_HEIGHT, 128, 128);
        if (searchText.isEmpty() && !focused) {
            guiGraphics.drawString(font, Component.translatable("screen.appcategories.categorized_terminal.search_hint"), x + 3, y + 2, 0xFFE6E6E6, false);
        }
    }

    private boolean isSearchBox(double mouseX, double mouseY) {
        return isWithin(mouseX, mouseY, leftPos + SEARCH_LEFT, topPos + SEARCH_TOP, SEARCH_WIDTH, SEARCH_HEIGHT);
    }

    private boolean isCraftingStatusButton(double mouseX, double mouseY) {
        return isWithin(mouseX, mouseY, leftPos + CRAFTING_STATUS_LEFT, topPos + CRAFTING_STATUS_TOP,
                CRAFTING_STATUS_WIDTH, CRAFTING_STATUS_HEIGHT);
    }

    private static boolean isWithin(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static void drawIcon(GuiGraphics guiGraphics, int x, int y, int u, int v, int width, int height) {
        guiGraphics.blit(AE2_STATES, x, y, u, v, width, height, 256, 256);
    }

    private void setSearchFocused(boolean focused) {
        if (searchField != null) {
            searchField.setFocused(focused);
            setFocused(focused ? searchField : null);
        }
    }

    private ToolbarAction toolbarActionAt(double mouseX, double mouseY) {
        int x = leftPos + TOOLBAR_X_OFFSET;
        int y = topPos + TOOLBAR_Y_OFFSET;
        for (ToolbarAction action : ToolbarAction.values()) {
            if (isWithin(mouseX, mouseY, x, y, TOOLBAR_WIDTH, TOOLBAR_HEIGHT)) {
                return action;
            }
            y += TOOLBAR_STEP;
        }
        return null;
    }

    private void handleToolbarClick(ToolbarAction action, boolean backwards) {
        playToolbarClick();
        switch (action) {
            case HELP -> openAeGuide();
            case SORT_BY -> sortMode = sortMode.next(backwards);
            case VIEW_MODE -> viewMode = viewMode.next(backwards);
            case TYPE_FILTER -> cycleSelectedKeyType(backwards);
            case SORT_DIRECTION -> sortAscending = !sortAscending;
            case SETTINGS -> settingsOpen = !settingsOpen;
            case TERMINAL_STYLE -> {
                terminalStyle = terminalStyle.next(backwards);
                AEConfig.instance().setTerminalStyle(terminalStyle.aeStyle);
                rebuildTerminalLayout();
            }
        }
    }

    private void playToolbarClick() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private void showToolbarMessage(Component component) {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(component, true);
        }
    }

    private boolean handleSettingsClick(double mouseX, double mouseY, int button) {
        if (button != 0 && button != 1) {
            return false;
        }
        int x = settingsPanelX();
        int y = settingsPanelY();
        if (isWithin(mouseX, mouseY, x + 176, y - 5, 25, 22)) {
            playToolbarClick();
            settingsOpen = false;
            return true;
        }
        if (!isWithin(mouseX, mouseY, x, y, SETTINGS_WIDTH, SETTINGS_HEIGHT)) {
            settingsOpen = false;
            return false;
        }
        SettingsControl control = settingsControlAt(mouseX, mouseY);
        if (control != null && control.active(this)) {
            playToolbarClick();
            control.toggle(AEConfig.instance());
        }
        return true;
    }

    private SettingsControl settingsControlAt(double mouseX, double mouseY) {
        int panelX = settingsPanelX();
        int panelY = settingsPanelY();
        for (SettingsControl control : SettingsControl.values()) {
            if (!control.visible(this)) {
                continue;
            }
            if (isWithin(mouseX, mouseY, panelX + control.x, panelY + control.y, control.width, 14)) {
                return control;
            }
        }
        return null;
    }

    private void drawTerminalSettingsPanel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = settingsPanelX();
        int y = settingsPanelY();
        drawGeneratedBackground(guiGraphics, x, y, SETTINGS_WIDTH, SETTINGS_HEIGHT);
        drawIcon(guiGraphics, x + 176, y - 5, 128, isWithin(mouseX, mouseY, x + 176, y - 5, 25, 22) ? 224 : 192, 25, 22);
        drawIcon(guiGraphics, x + 179, y - 1, 96, 16, 16, 16);
        guiGraphics.drawString(font, Component.translatable("gui.ae2.TerminalSettingsTitle"), x + 8, y + 7, 0xFF404040, false);
        guiGraphics.drawString(font, Component.translatable("gui.ae2.SearchSettingsTitle"), x + 8, y + 110, 0xFF404040, false);

        for (SettingsControl control : SettingsControl.values()) {
            if (control.visible(this)) {
                drawSettingsControl(guiGraphics, mouseX, mouseY, control);
            }
        }
    }

    private void drawGeneratedBackground(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        int border = 4;
        int size = 256;
        int tiledSize = size - 2 * border;
        guiGraphics.blit(AE2_BACKGROUND, x, y, 0, 0, border, border, size, size);
        guiGraphics.blit(AE2_BACKGROUND, x + width - border, y, size - border, 0, border, border, size, size);
        guiGraphics.blit(AE2_BACKGROUND, x, y + height - border, 0, size - border, border, border, size, size);
        guiGraphics.blit(AE2_BACKGROUND, x + width - border, y + height - border, size - border, size - border, border, border, size, size);

        int innerWidth = width - 2 * border;
        int innerHeight = height - 2 * border;
        for (int cx = 0; cx < innerWidth; cx += tiledSize) {
            int tileWidth = Math.min(tiledSize, innerWidth - cx);
            guiGraphics.blit(AE2_BACKGROUND, x + border + cx, y, border, 0, tileWidth, border, size, size);
            guiGraphics.blit(AE2_BACKGROUND, x + border + cx, y + height - border, border, size - border, tileWidth, border, size, size);
            for (int cy = 0; cy < innerHeight; cy += tiledSize) {
                int tileHeight = Math.min(tiledSize, innerHeight - cy);
                guiGraphics.blit(AE2_BACKGROUND, x + border + cx, y + border + cy, border, border, tileWidth, tileHeight, size, size);
            }
        }
        for (int cy = 0; cy < innerHeight; cy += tiledSize) {
            int tileHeight = Math.min(tiledSize, innerHeight - cy);
            guiGraphics.blit(AE2_BACKGROUND, x, y + border + cy, 0, border, border, tileHeight, size, size);
            guiGraphics.blit(AE2_BACKGROUND, x + width - border, y + border + cy, size - border, border, border, tileHeight, size, size);
        }
    }

    private void drawSettingsControl(GuiGraphics guiGraphics, int mouseX, int mouseY, SettingsControl control) {
        int x = settingsPanelX() + control.x;
        int y = settingsPanelY() + control.y;
        boolean active = control.active(this);
        boolean hovered = active && isWithin(mouseX, mouseY, x, y, control.width, 14);
        boolean selected = control.value(AEConfig.instance());
        if (control.radio) {
            int u = hovered ? 42 : 28;
            int v = selected ? 14 : 0;
            guiGraphics.blit(AE2_CHECKBOX, x, y, u, v, 14, 14, 64, 64);
            drawSettingsLabel(guiGraphics, control.label(this), x + 16, y + 4, control.width - 16, active);
        } else {
            int u = hovered ? 22 : 0;
            int v = selected ? 40 : 28;
            guiGraphics.blit(AE2_CHECKBOX, x, y + 1, u, v, 22, 12, 64, 64);
            drawSettingsLabel(guiGraphics, control.label(this), x + 26, y + 4, control.width - 26, active);
        }
    }

    private void drawSettingsLabel(GuiGraphics guiGraphics, Component label, int x, int y, int width, boolean active) {
        int color = active ? 0xFF404040 : 0xFF888888;
        var lines = font.split(label, width);
        int lineY = lines.size() <= 1 ? y : y - 3;
        for (var line : lines) {
            guiGraphics.drawString(font, line, x, lineY, color, false);
            lineY += font.lineHeight;
        }
    }

    private int settingsPanelX() {
        return leftPos;
    }

    private int settingsPanelY() {
        return Math.max(4, categoryBarY());
    }

    private void openAeGuide() {
        AppEng.instance().openGuideAtAnchor(
                new PageAnchor(ResourceLocation.fromNamespaceAndPath("ae2", "items-blocks-machines/terminals.md"), null));
    }

    private void openCraftingStatus() {
        playToolbarClick();
        PacketDistributor.sendToServer(SwitchGuisPacket.openSubMenu(CraftingStatusMenu.TYPE));
    }

    private void cycleSelectedKeyType(boolean backwards) {
        var keyTypes = new ArrayList<>(AEKeyTypes.getAll());
        keyTypes.sort(Comparator
                .comparingInt((AEKeyType type) -> type == AEKeyType.items() ? 0 : type == AEKeyType.fluids() ? 1 : 2)
                .thenComparing(type -> type.getDescription().getString(), String.CASE_INSENSITIVE_ORDER));
        if (keyTypes.isEmpty()) {
            selectedKeyType = null;
            return;
        }
        if (selectedKeyType == null) {
            selectedKeyType = backwards ? keyTypes.get(keyTypes.size() - 1) : keyTypes.get(0);
            return;
        }
        int index = keyTypes.indexOf(selectedKeyType);
        if (index < 0) {
            selectedKeyType = null;
            return;
        }
        int next = index + (backwards ? -1 : 1);
        selectedKeyType = next < 0 || next >= keyTypes.size() ? null : keyTypes.get(next);
    }

    private Component typeFilterTooltip() {
        if (selectedKeyType != null) {
            return selectedKeyType.getDescription();
        }
        var keyTypes = new ArrayList<>(AEKeyTypes.getAll());
        keyTypes.sort(Comparator
                .comparingInt((AEKeyType type) -> type == AEKeyType.items() ? 0 : type == AEKeyType.fluids() ? 1 : 2)
                .thenComparing(type -> type.getDescription().getString(), String.CASE_INSENSITIVE_ORDER));
        return Component.literal(String.join(", ", keyTypes.stream()
                .map(type -> type.getDescription().getString())
                .toList()));
    }

    private void renderVirtualTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (settingsOpen) {
            return;
        }
        if (isCraftingStatusButton(mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, Component.translatable("gui.ae2.CraftingStatus"), mouseX, mouseY);
            return;
        }
        ToolbarAction toolbarAction = toolbarActionAt(mouseX, mouseY);
        if (toolbarAction != null) {
            guiGraphics.renderComponentTooltip(font, toolbarAction.tooltip(this), mouseX, mouseY);
            return;
        }
        if (settingsOpen && isWithin(mouseX, mouseY, settingsPanelX(), settingsPanelY(), SETTINGS_WIDTH, SETTINGS_HEIGHT)) {
            return;
        }
        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            return;
        }
        var snapshot = CategoryClientCache.snapshot(menu.containerId);
        AEKey key = stackAt(mouseX, mouseY, snapshot);
        if (key != null) {
            ItemStack display = key.wrapForDisplayOrFilter();
            guiGraphics.renderTooltip(font, display, mouseX, mouseY);
            return;
        }
        Component tooltip = categoryTooltip(mouseX, mouseY, snapshot);
        if (tooltip != null) {
            guiGraphics.renderTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    private Component categoryTooltip(double mouseX, double mouseY, org.lyy.appcategories.network.CategorySnapshotPacket snapshot) {
        if (snapshot == null) {
            return null;
        }
        int x = categoryBarX();
        int y = categoryBarY();
        if (isWithin(mouseX, mouseY, x, y, CATEGORY_WIDTH, CATEGORY_HEIGHT)) {
            return Component.translatable("screen.appcategories.categorized_terminal.category.all");
        }
        x += CATEGORY_WIDTH;
        if (isWithin(mouseX, mouseY, x, y, CATEGORY_WIDTH, CATEGORY_HEIGHT)) {
            return Component.translatable("screen.appcategories.categorized_terminal.category.uncategorized");
        }
        x += CATEGORY_WIDTH;
        int visibleSlots = visibleUserCategorySlots(snapshot);
        for (int i = categoryScroll; i < snapshot.categories().size() && i < categoryScroll + visibleSlots; i++) {
            var category = snapshot.categories().get(i);
            if (isWithin(mouseX, mouseY, x, y, CATEGORY_WIDTH, CATEGORY_HEIGHT)) {
                return Component.literal(category.name());
            }
            x += CATEGORY_WIDTH;
        }
        if (isCreateCategoryButton(mouseX, mouseY, snapshot)) {
            return Component.translatable("screen.appcategories.categorized_terminal.create_category");
        }
        if (isDeleteCategoryButton(mouseX, mouseY, snapshot)) {
            return Component.translatable("screen.appcategories.categorized_terminal.delete_category");
        }
        return null;
    }

    private void drawHoveredGridSlot(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!isGridArea(mouseX, mouseY)) {
            return;
        }
        int slotX = leftPos + GRID_LEFT + ((mouseX - leftPos - GRID_LEFT) / 18) * 18;
        int slotY = topPos + GRID_TOP + ((mouseY - topPos - GRID_TOP) / 18) * 18;
        guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0x80FFFFFF);
    }

    private enum ToolbarAction {
        HELP,
        SORT_BY,
        VIEW_MODE,
        TYPE_FILTER,
        SORT_DIRECTION,
        SETTINGS,
        TERMINAL_STYLE;

        int iconU(CategorizedTerminalScreen screen) {
            return switch (this) {
                case HELP -> 176;
                case SORT_BY -> screen.sortMode.u;
                case VIEW_MODE -> screen.viewMode.u;
            case TYPE_FILTER -> 160;
                case SORT_DIRECTION -> screen.sortAscending ? 0 : 16;
                case SETTINGS -> 32;
                case TERMINAL_STYLE -> screen.terminalStyle.u;
            };
        }

        int iconV(CategorizedTerminalScreen screen) {
            return switch (this) {
                case HELP -> 0;
                case SORT_BY -> screen.sortMode.v;
                case VIEW_MODE -> screen.viewMode.v;
                case TYPE_FILTER -> 16;
                case SORT_DIRECTION -> 48;
                case SETTINGS -> 64;
                case TERMINAL_STYLE -> screen.terminalStyle.v;
            };
        }

        List<Component> tooltip(CategorizedTerminalScreen screen) {
            return switch (this) {
                case HELP -> List.of(
                        Component.translatable("gui.tooltips.ae2.OpenGuide"),
                        Component.translatable("gui.tooltips.ae2.OpenGuideDetail"));
                case SORT_BY -> List.of(
                        Component.translatable("gui.tooltips.ae2.SortBy"),
                        screen.sortMode.tooltip);
                case VIEW_MODE -> List.of(
                        Component.translatable("gui.tooltips.ae2.View"),
                        screen.viewMode.tooltip);
                case TYPE_FILTER -> List.of(
                        Component.translatable("gui.ae2.ConfigureVisibleTypes"),
                        screen.typeFilterTooltip());
                case SORT_DIRECTION -> List.of(
                        Component.translatable("gui.tooltips.ae2.SortOrder"),
                        Component.translatable(screen.sortAscending
                                ? "gui.tooltips.ae2.Ascending"
                                : "gui.tooltips.ae2.Descending"));
                case SETTINGS -> List.of(Component.translatable("gui.tooltips.ae2.TerminalSettings"));
                case TERMINAL_STYLE -> List.of(
                        Component.translatable("gui.tooltips.ae2.TerminalStyle"),
                        screen.terminalStyle.tooltip);
            };
        }
    }

    private enum SortMode {
        NAME(0, 64, Component.translatable("gui.tooltips.ae2.ItemName")),
        AMOUNT(16, 64, Component.translatable("gui.tooltips.ae2.NumberOfItems")),
        MOD(96, 64, Component.translatable("gui.tooltips.ae2.Mod"));

        final int u;
        final int v;
        final Component tooltip;

        SortMode(int u, int v, Component tooltip) {
            this.u = u;
            this.v = v;
            this.tooltip = tooltip;
        }

        SortMode next(boolean backwards) {
            SortMode[] values = values();
            int offset = backwards ? values.length - 1 : 1;
            return values[(ordinal() + offset) % values.length];
        }
    }

    private enum ViewMode {
        STORED(0, 16, Component.translatable("gui.tooltips.ae2.StoredItems")),
        ALL(32, 16, Component.translatable("gui.tooltips.ae2.StoredCraftable")),
        CRAFTABLE(48, 16, Component.translatable("gui.tooltips.ae2.Craftable"));

        final int u;
        final int v;
        final Component tooltip;

        ViewMode(int u, int v, Component tooltip) {
            this.u = u;
            this.v = v;
            this.tooltip = tooltip;
        }

        ViewMode next(boolean backwards) {
            ViewMode[] values = values();
            int offset = backwards ? values.length - 1 : 1;
            return values[(ordinal() + offset) % values.length];
        }
    }

    private enum TerminalStyleButton {
        SMALL(0, 208, TerminalStyle.SMALL, Component.translatable("gui.tooltips.ae2.TerminalStyle_Small")),
        MEDIUM(16, 208, TerminalStyle.MEDIUM, Component.translatable("gui.tooltips.ae2.TerminalStyle_Medium")),
        TALL(32, 208, TerminalStyle.TALL, Component.translatable("gui.tooltips.ae2.TerminalStyle_Tall")),
        FULL(48, 208, TerminalStyle.FULL, Component.translatable("gui.tooltips.ae2.TerminalStyle_Full"));

        final int u;
        final int v;
        final TerminalStyle aeStyle;
        final Component tooltip;

        TerminalStyleButton(int u, int v, TerminalStyle aeStyle, Component tooltip) {
            this.u = u;
            this.v = v;
            this.aeStyle = aeStyle;
            this.tooltip = tooltip;
        }

        TerminalStyleButton next(boolean backwards) {
            TerminalStyleButton[] values = values();
            int offset = backwards ? values.length - 1 : 1;
            return values[(ordinal() + offset) % values.length];
        }

        int rows(int maxRows) {
            return aeStyle.getRows(maxRows);
        }

        static TerminalStyleButton from(TerminalStyle style) {
            for (TerminalStyleButton value : values()) {
                if (value.aeStyle == style) {
                    return value;
                }
            }
            return SMALL;
        }
    }

    private String externalSearchName() {
        return ItemListMod.isEnabled() ? ItemListMod.getShortName() : "REI/EMI";
    }

    private enum SettingsControl {
        PIN_AUTO_CRAFTED(10, 25, 180, false) {
            @Override
            boolean value(AEConfig config) {
                return config.isPinAutoCraftedItems();
            }

            @Override
            void toggle(AEConfig config) {
                config.setPinAutoCraftedItems(!config.isPinAutoCraftedItems());
            }

            @Override
            Component label(CategorizedTerminalScreen screen) {
                return Component.translatable("gui.ae2.TerminalSettingsPinAutoCraftedItems");
            }
        },
        NOTIFY_FINISHED_JOBS(10, 50, 180, false) {
            @Override
            boolean value(AEConfig config) {
                return config.isNotifyForFinishedCraftingJobs();
            }

            @Override
            void toggle(AEConfig config) {
                config.setNotifyForFinishedCraftingJobs(!config.isNotifyForFinishedCraftingJobs());
            }

            @Override
            Component label(CategorizedTerminalScreen screen) {
                return Component.translatable("gui.ae2.TerminalSettingsNotifyForFinishedJobs");
            }
        },
        CLEAR_GRID_ON_CLOSE(10, 82, 180, false) {
            @Override
            boolean value(AEConfig config) {
                return config.isClearGridOnClose();
            }

            @Override
            void toggle(AEConfig config) {
                config.setClearGridOnClose(!config.isClearGridOnClose());
            }

            @Override
            Component label(CategorizedTerminalScreen screen) {
                return Component.translatable("gui.ae2.TerminalSettingsClearGridOnClose");
            }
        },
        USE_INTERNAL_SEARCH(10, 124, 75, true) {
            @Override
            boolean value(AEConfig config) {
                return !config.isUseExternalSearch();
            }

            @Override
            void toggle(AEConfig config) {
                config.setUseExternalSearch(false);
            }

            @Override
            Component label(CategorizedTerminalScreen screen) {
                return Component.translatable("gui.ae2.SearchSettingsUseInternalSearch");
            }
        },
        USE_EXTERNAL_SEARCH(95, 124, 75, true) {
            @Override
            boolean value(AEConfig config) {
                return config.isUseExternalSearch();
            }

            @Override
            void toggle(AEConfig config) {
                config.setUseExternalSearch(true);
            }

            @Override
            boolean active(CategorizedTerminalScreen screen) {
                return ItemListMod.isEnabled();
            }

            @Override
            Component label(CategorizedTerminalScreen screen) {
                return Component.translatable("gui.ae2.SearchSettingsUseExternalSearch", screen.externalSearchName());
            }
        },
        REMEMBER_SEARCH(10, 160, 150, false) {
            @Override
            boolean value(AEConfig config) {
                return config.isRememberLastSearch();
            }

            @Override
            void toggle(AEConfig config) {
                config.setRememberLastSearch(!config.isRememberLastSearch());
            }

            @Override
            boolean visible(CategorizedTerminalScreen screen) {
                return !AEConfig.instance().isUseExternalSearch();
            }

            @Override
            Component label(CategorizedTerminalScreen screen) {
                return Component.translatable("gui.ae2.SearchSettingsRememberSearch");
            }
        },
        AUTO_FOCUS_SEARCH(10, 176, 150, false) {
            @Override
            boolean value(AEConfig config) {
                return config.isAutoFocusSearch();
            }

            @Override
            void toggle(AEConfig config) {
                config.setAutoFocusSearch(!config.isAutoFocusSearch());
            }

            @Override
            boolean visible(CategorizedTerminalScreen screen) {
                return !AEConfig.instance().isUseExternalSearch();
            }

            @Override
            Component label(CategorizedTerminalScreen screen) {
                return Component.translatable("gui.ae2.SearchSettingsAutoFocus");
            }
        },
        SYNC_WITH_EXTERNAL(10, 192, 150, false) {
            @Override
            boolean value(AEConfig config) {
                return config.isSyncWithExternalSearch();
            }

            @Override
            void toggle(AEConfig config) {
                config.setSyncWithExternalSearch(!config.isSyncWithExternalSearch());
            }

            @Override
            boolean visible(CategorizedTerminalScreen screen) {
                return !AEConfig.instance().isUseExternalSearch();
            }

            @Override
            Component label(CategorizedTerminalScreen screen) {
                return Component.translatable("gui.ae2.SearchSettingsSyncWithExternal", screen.externalSearchName());
            }
        },
        CLEAR_EXTERNAL_SEARCH(10, 160, 150, false) {
            @Override
            boolean value(AEConfig config) {
                return config.isClearExternalSearchOnOpen();
            }

            @Override
            void toggle(AEConfig config) {
                config.setClearExternalSearchOnOpen(!config.isClearExternalSearchOnOpen());
            }

            @Override
            boolean visible(CategorizedTerminalScreen screen) {
                return AEConfig.instance().isUseExternalSearch();
            }

            @Override
            Component label(CategorizedTerminalScreen screen) {
                return Component.translatable("gui.ae2.SearchSettingsClearExternal", screen.externalSearchName());
            }
        };

        final int x;
        final int y;
        final int width;
        final boolean radio;

        SettingsControl(int x, int y, int width, boolean radio) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.radio = radio;
        }

        abstract boolean value(AEConfig config);

        abstract void toggle(AEConfig config);

        abstract Component label(CategorizedTerminalScreen screen);

        boolean visible(CategorizedTerminalScreen screen) {
            return true;
        }

        boolean active(CategorizedTerminalScreen screen) {
            return true;
        }
    }

}
