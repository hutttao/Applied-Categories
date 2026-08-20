package org.lyy.mektmc.menu;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lyy.mektmc.ae.category.CategoryIds;
import org.lyy.mektmc.blockentity.CategorizedTerminalBlockEntity;
import org.lyy.mektmc.blockentity.CategoryIndexBlockEntity;
import org.lyy.mektmc.network.CategorySnapshotPacket;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.UUID;

public class CategorizedTerminalMenu extends AEBaseMenu {
    private final IActionHost terminal;
    private final Player player;
    private UUID activeCategory = CategoryIds.ALL;
    private long lastInventorySize = -1;
    @GuiSync(100)
    public int activeCraftingJobs = -1;

    public CategorizedTerminalMenu(MenuType<?> menuType, int containerId, Inventory playerInventory, IActionHost terminal) {
        super(menuType, containerId, playerInventory, terminal);
        this.terminal = terminal;
        this.player = playerInventory.player;
        addPlayerInventory(playerInventory, 8, 104);
    }

    @Override
    public boolean stillValid(Player player) {
        if (!super.stillValid(player) || terminal == null) {
            return false;
        }
        if (terminal instanceof CategorizedTerminalBlockEntity blockEntity) {
            return !blockEntity.isRemoved() && player.distanceToSqr(
                    blockEntity.getBlockPos().getX() + 0.5,
                    blockEntity.getBlockPos().getY() + 0.5,
                    blockEntity.getBlockPos().getZ() + 0.5) <= 64.0;
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (player.level().isClientSide) {
            return ItemStack.EMPTY;
        }
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (insertStack(stack, stack.getCount()) <= 0) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        // Shift 快速放入不走自定义网络包，这里主动推送快照，保证界面即时刷新
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, createSnapshot());
        }
        return original;
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            updateActiveCraftingJobs();
            pushSnapshotIfInventoryChanged();
        }
        super.broadcastChanges();
    }

    /**
     * 轮询网格缓存条目数：条目数变化时推送新快照，兜底覆盖
     * 不经过自定义网络包的库存变动（外部自动化/管道等），延迟最多 1 tick。
     * 注意：仅条目数变化（新增/消失物品）会触发；同条目数量增减由各操作包即时推送。
     */
    private void pushSnapshotIfInventoryChanged() {
        IGrid grid = getGrid();
        if (grid == null) {
            return;
        }
        KeyCounter counter = grid.getStorageService().getCachedInventory();
        if (counter == null) {
            return;
        }
        long size = counter.size();
        if (size != lastInventorySize) {
            lastInventorySize = size;
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, createSnapshot());
            }
        }
    }

    public void setActiveCategory(UUID activeCategory) {
        this.activeCategory = activeCategory == null ? CategoryIds.ALL : activeCategory;
    }

    public CategorySnapshotPacket createSnapshot() {
        var categories = new ArrayList<CategorySnapshotPacket.CategoryEntry>();
        var stacks = new ArrayList<CategorySnapshotPacket.StackEntry>();
        var status = CategorySnapshotPacket.Status.NO_INDEX;

        CategoryIndexBlockEntity index = getCategoryIndex();
        if (index == null) {
            status = categoryIndexCount() > 1
                    ? CategorySnapshotPacket.Status.CONFLICT
                    : terminal != null && terminal.getActionableNode() != null && terminal.getActionableNode().isOnline()
                    ? CategorySnapshotPacket.Status.NO_INDEX
                    : CategorySnapshotPacket.Status.OFFLINE;
        } else if (!index.isOnlineAndValid()) {
            status = CategorySnapshotPacket.Status.CONFLICT;
        } else {
            status = CategorySnapshotPacket.Status.OK;
            for (var category : index.getCategories()) {
                categories.add(new CategorySnapshotPacket.CategoryEntry(category.id(), category.name(), category.color(), category.sortOrder(), category.builtin()));
            }
        }

        IGrid grid = getGrid();
        if (grid != null) {
            KeyCounter counter = grid.getStorageService().getCachedInventory();
            for (Object2LongMap.Entry<AEKey> entry : counter) {
                var key = entry.getKey();
                var ids = index == null ? java.util.Set.<UUID>of() : index.getCategoriesFor(key);
                if (CategoryIds.isUserCategory(activeCategory) && !ids.contains(activeCategory)) {
                    continue;
                }
                if (CategoryIds.UNCATEGORIZED.equals(activeCategory) && !ids.isEmpty()) {
                    continue;
                }
                stacks.add(CategorySnapshotPacket.StackEntry.fromKey(key, entry.getLongValue(), ids));
            }
        }

        return new CategorySnapshotPacket(containerId, activeCategory, status, categories, stacks);
    }

    public boolean createCategory(String name, int color) {
        CategoryIndexBlockEntity index = getCategoryIndex();
        if (index == null || !index.isOnlineAndValid()) {
            return false;
        }
        try {
            return index.createCategory(name, color) != null;
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    public boolean deleteCategory(UUID categoryId) {
        CategoryIndexBlockEntity index = getCategoryIndex();
        if (index == null || !index.isOnlineAndValid() || !index.deleteCategory(categoryId)) {
            return false;
        }
        if (categoryId.equals(activeCategory)) {
            activeCategory = CategoryIds.ALL;
        }
        return true;
    }

    public boolean renameCategory(UUID categoryId, String name) {
        CategoryIndexBlockEntity index = getCategoryIndex();
        return index != null && index.isOnlineAndValid() && index.renameCategory(categoryId, name);
    }

    public boolean assign(AEKey key, UUID categoryId) {
        CategoryIndexBlockEntity index = getCategoryIndex();
        return index != null && index.isOnlineAndValid() && index.addCategory(key, categoryId);
    }

    public boolean remove(AEKey key, UUID categoryId) {
        CategoryIndexBlockEntity index = getCategoryIndex();
        return index != null && index.isOnlineAndValid() && index.removeCategory(key, categoryId);
    }

    public long insertCarried(boolean singleItem) {
        if (getGrid() == null) {
            return 0;
        }
        ItemStack carried = getCarried();
        if (carried.isEmpty()) {
            return 0;
        }
        ItemStack remainder = carried.copy();
        long inserted = insertStack(remainder, singleItem ? 1 : remainder.getCount());
        if (inserted > 0) {
            setCarried(remainder);
        }
        return inserted;
    }

    public void handleGridClick(ServerPlayer player, AEKey key, int mouseButton, boolean quickMove) {
        IGrid grid = getGrid();
        if (terminal == null || grid == null || key == null) {
            return;
        }

        MEStorage storage = grid.getStorageService().getInventory();
        IActionSource source = IActionSource.ofMachine(terminal);
        if (!(key instanceof AEItemKey clickedItem)) {
            return;
        }

        if (mouseButton == 2) {
            if (player.getAbilities().instabuild) {
                ItemStack stack = clickedItem.toStack();
                stack.setCount(stack.getMaxStackSize());
                setCarried(stack);
            }
            return;
        }

        if (mouseButton == 3) {
            insertCarried(true);
            return;
        }

        if ((quickMove && mouseButton == 1) || mouseButton == 4) {
            ItemStack carried = getCarried();
            if (!carried.isEmpty()) {
                if (carried.getCount() >= carried.getMaxStackSize() || !clickedItem.matches(carried)) {
                    return;
                }
            }
            long extracted = storage.extract(clickedItem, 1, Actionable.MODULATE, source);
            if (extracted > 0) {
                if (carried.isEmpty()) {
                    setCarried(clickedItem.toStack());
                } else {
                    carried.grow(1);
                }
            }
            return;
        }

        if (quickMove) {
            moveOneStackToPlayer(clickedItem, storage, source);
            return;
        }

        if (mouseButton == 1) {
            if (!getCarried().isEmpty()) {
                insertCarried(true);
                return;
            }
            long extracted = storage.extract(clickedItem, clickedItem.getMaxStackSize(), Actionable.SIMULATE, source);
            if (extracted > 0) {
                extracted = (extracted + 1) >> 1;
                extracted = storage.extract(clickedItem, extracted, Actionable.MODULATE, source);
            }
            setCarried(extracted > 0 ? clickedItem.toStack((int) extracted) : ItemStack.EMPTY);
        } else {
            if (!getCarried().isEmpty()) {
                insertCarried(false);
                return;
            }
            long extracted = storage.extract(clickedItem, clickedItem.getMaxStackSize(), Actionable.MODULATE, source);
            setCarried(extracted > 0 ? clickedItem.toStack((int) extracted) : ItemStack.EMPTY);
        }
    }

    private boolean moveOneStackToPlayer(AEItemKey what, MEStorage storage, IActionSource source) {
        long available = storage.extract(what, what.getMaxStackSize(), Actionable.SIMULATE, source);
        if (available <= 0) {
            return false;
        }
        var destinationSlots = getQuickMoveDestinationSlots(what.toStack(), false);
        for (var destinationSlot : destinationSlots) {
            int amount = getPlaceableAmount(destinationSlot, what);
            if (amount <= 0) {
                continue;
            }
            long extracted = storage.extract(what, amount, Actionable.MODULATE, source);
            if (extracted <= 0) {
                return false;
            }
            ItemStack currentItem = destinationSlot.getItem();
            if (currentItem.isEmpty()) {
                destinationSlot.setByPlayer(what.toStack((int) extracted));
            } else {
                destinationSlot.setByPlayer(currentItem.copyWithCount(currentItem.getCount() + (int) extracted));
            }
            destinationSlot.setChanged();
            return true;
        }
        return false;
    }

    @Nullable
    private CategoryIndexBlockEntity getCategoryIndex() {
        IGrid grid = getGrid();
        if (grid == null) {
            return null;
        }
        CategoryIndexBlockEntity found = null;
        for (var index : grid.getActiveMachines(CategoryIndexBlockEntity.class)) {
            if (index.isOnlineAndValid()) {
                if (found != null) {
                    return null;
                }
                found = index;
            }
        }
        return found;
    }

    private long insertStack(ItemStack stack, long amount) {
        IGrid grid = getGrid();
        if (terminal == null || grid == null || stack.isEmpty() || amount <= 0) {
            return 0;
        }
        var key = appeng.api.stacks.AEItemKey.of(stack);
        if (key == null) {
            return 0;
        }
        MEStorage storage = grid.getStorageService().getInventory();
        long inserted = storage.insert(key, Math.min(amount, stack.getCount()), Actionable.MODULATE, IActionSource.ofMachine(terminal));
        if (inserted > 0) {
            stack.shrink((int) inserted);
            CategoryIndexBlockEntity index = getCategoryIndex();
            if (index != null && CategoryIds.isUserCategory(activeCategory) && index.isOnlineAndValid()) {
                index.addCategory(key, activeCategory);
            }
        }
        return inserted;
    }

    private void updateActiveCraftingJobs() {
        IGrid grid = getGrid();
        if (grid == null) {
            activeCraftingJobs = -1;
            return;
        }
        int activeJobs = 0;
        for (var cpu : grid.getCraftingService().getCpus()) {
            if (cpu.isBusy()) {
                activeJobs++;
            }
        }
        activeCraftingJobs = activeJobs;
    }

    private int categoryIndexCount() {
        IGrid grid = getGrid();
        if (grid == null) {
            return 0;
        }
        var count = 0;
        for (var ignored : grid.getActiveMachines(CategoryIndexBlockEntity.class)) {
            count++;
        }
        return count;
    }

    @Nullable
    private IGrid getGrid() {
        return terminal == null || terminal.getActionableNode() == null ? null : terminal.getActionableNode().getGrid();
    }

    private void addPlayerInventory(Inventory playerInventory, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, left + col * 18, top + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, left + col * 18, top + 58));
        }
    }
}
