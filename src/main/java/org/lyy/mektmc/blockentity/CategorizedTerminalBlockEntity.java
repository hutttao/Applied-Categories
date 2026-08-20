package org.lyy.mektmc.blockentity;

import appeng.api.networking.IGrid;
import appeng.api.storage.ILinkStatus;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.MEStorage;
import appeng.api.storage.SupplierStorage;
import appeng.api.util.IConfigManager;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.util.NullConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.lyy.mektmc.registries.BlockEntityRegistry;
import org.lyy.mektmc.registries.ItemRegistry;
import org.lyy.mektmc.registries.MenuRegistry;

public class CategorizedTerminalBlockEntity extends ManagedGridBlockEntity implements ITerminalHost {
    public CategorizedTerminalBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.CATEGORIZED_TERMINAL.get(), pos, blockState, 0.5);
    }

    @Override
    public MEStorage getInventory() {
        return new SupplierStorage(() -> {
            IGrid grid = getMainNode().getGrid();
            return grid == null ? null : grid.getStorageService().getInventory();
        });
    }

    @Override
    public ILinkStatus getLinkStatus() {
        return ILinkStatus.ofManagedNode(getMainNode());
    }

    @Override
    public IConfigManager getConfigManager() {
        return NullConfigManager.INSTANCE;
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        MenuOpener.open(MenuRegistry.CATEGORIZED_TERMINAL.get(), player, subMenu.getLocator(), true);
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return ItemRegistry.CATEGORIZED_TERMINAL.get().getDefaultInstance();
    }

    @Nullable
    public CategoryIndexBlockEntity findCategoryIndex() {
        IGrid grid = getMainNode().getGrid();
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

    public int categoryIndexCount() {
        IGrid grid = getMainNode().getGrid();
        if (grid == null) {
            return 0;
        }
        var count = 0;
        for (var ignored : grid.getActiveMachines(CategoryIndexBlockEntity.class)) {
            count++;
        }
        return count;
    }
}
