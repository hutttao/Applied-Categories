package org.lyy.mektmc.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lyy.mektmc.blockentity.CategoryIndexBlockEntity;
import org.lyy.mektmc.items.CategoryDiskItem;
import org.lyy.mektmc.registries.MenuRegistry;

public class CategoryIndexMenu extends AbstractContainerMenu {
    private final Container container;

    public CategoryIndexMenu(int containerId, Inventory playerInventory, Container container) {
        super(MenuRegistry.CATEGORY_INDEX.get(), containerId);
        this.container = container;
        this.addSlot(new Slot(container, 0, 80, 37) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof CategoryDiskItem;
            }
        });
        addPlayerInventory(playerInventory, 8, 84);
    }

    public static CategoryIndexMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        var blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof CategoryIndexBlockEntity index) {
            return new CategoryIndexMenu(containerId, playerInventory, index);
        }
        return new CategoryIndexMenu(containerId, playerInventory, new SimpleContainer(1));
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            original = stack.copy();
            if (index == 0) {
                if (!moveItemStackTo(stack, 1, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.getItem() instanceof CategoryDiskItem) {
                if (!moveItemStackTo(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return original;
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
