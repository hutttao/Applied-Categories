package org.lyy.mektmc.blockentity;

import appeng.api.stacks.AEKey;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.lyy.mektmc.ae.category.CategoryDatabase;
import org.lyy.mektmc.ae.category.CategoryDef;
import org.lyy.mektmc.ae.category.CategoryIndexHost;
import org.lyy.mektmc.ae.category.SavedDataCategoryDatabaseStorage;
import org.lyy.mektmc.items.CategoryDiskItem;
import org.lyy.mektmc.menu.CategoryIndexMenu;
import org.lyy.mektmc.registries.BlockEntityRegistry;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public class CategoryIndexBlockEntity extends ManagedGridBlockEntity implements Container, MenuProvider, CategoryIndexHost {
    private ItemStack disk = ItemStack.EMPTY;
    private UUID databaseId;
    private CategoryDatabase database;

    public CategoryIndexBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.CATEGORY_INDEX.get(), pos, blockState, 1.0);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        disk = tag.contains("disk") ? ItemStack.parseOptional(registries, tag.getCompound("disk")) : ItemStack.EMPTY;
        databaseId = null;
        database = null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!disk.isEmpty()) {
            tag.put("disk", disk.saveOptional(registries));
        }
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return disk.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? disk : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != 0 || disk.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        var removed = disk.split(amount);
        if (disk.isEmpty()) {
            databaseId = null;
            database = null;
        }
        setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot != 0) {
            return ItemStack.EMPTY;
        }
        var old = disk;
        disk = ItemStack.EMPTY;
        databaseId = null;
        database = null;
        return old;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != 0) {
            return;
        }
        disk = stack;
        if (!disk.isEmpty() && disk.getItem() instanceof CategoryDiskItem diskItem && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            databaseId = diskItem.ensureDatabaseId(disk);
            database = new SavedDataCategoryDatabaseStorage(serverLevel).load(databaseId);
        } else {
            databaseId = null;
            database = null;
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && stack.getItem() instanceof CategoryDiskItem;
    }

    @Override
    public void clearContent() {
        disk = ItemStack.EMPTY;
        databaseId = null;
        database = null;
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mektmc.category_index");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CategoryIndexMenu(containerId, playerInventory, this);
    }

    @Override
    public UUID getDatabaseId() {
        ensureDatabase();
        return databaseId;
    }

    @Override
    public Collection<CategoryDef> getCategories() {
        return ensureDatabase() ? database.getCategories() : Set.of();
    }

    @Override
    public Set<UUID> getCategoriesFor(AEKey key) {
        return ensureDatabase() ? database.getCategoriesFor(key) : Set.of();
    }

    @Override
    public boolean addCategory(AEKey key, UUID categoryId) {
        if (!ensureDatabase() || !database.addCategory(key, categoryId)) {
            return false;
        }
        persistDatabase();
        return true;
    }

    @Override
    public boolean removeCategory(AEKey key, UUID categoryId) {
        if (!ensureDatabase() || !database.removeCategory(key, categoryId)) {
            return false;
        }
        persistDatabase();
        return true;
    }

    @Override
    public CategoryDef createCategory(String name, int color) {
        if (!ensureDatabase()) {
            throw new IllegalStateException("No category disk inserted");
        }
        var def = database.createCategory(name, color);
        persistDatabase();
        return def;
    }

    @Override
    public boolean renameCategory(UUID categoryId, String newName) {
        if (!ensureDatabase() || !database.renameCategory(categoryId, newName)) {
            return false;
        }
        persistDatabase();
        return true;
    }

    @Override
    public boolean deleteCategory(UUID categoryId) {
        if (!ensureDatabase() || !database.deleteCategory(categoryId)) {
            return false;
        }
        persistDatabase();
        return true;
    }

    @Override
    public boolean isOnlineAndValid() {
        return getMainNode().isOnline() && ensureDatabase() && findActiveIndexInGrid() == this;
    }

    @Nullable
    public CategoryIndexBlockEntity findActiveIndexInGrid() {
        var grid = getMainNode().getGrid();
        if (grid == null) {
            return null;
        }
        CategoryIndexBlockEntity found = null;
        for (var index : grid.getActiveMachines(CategoryIndexBlockEntity.class)) {
            if (index.ensureDatabase()) {
                if (found != null) {
                    return null;
                }
                found = index;
            }
        }
        return found;
    }

    private boolean ensureDatabase() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel) || disk.isEmpty() || !(disk.getItem() instanceof CategoryDiskItem diskItem)) {
            return false;
        }
        if (database == null || databaseId == null) {
            databaseId = diskItem.ensureDatabaseId(disk);
            database = new SavedDataCategoryDatabaseStorage(serverLevel).load(databaseId);
            setChanged();
        }
        return database != null;
    }

    private void persistDatabase() {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel && databaseId != null && database != null) {
            new SavedDataCategoryDatabaseStorage(serverLevel).save(databaseId, database);
        }
    }
}
