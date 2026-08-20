package org.lyy.mektmc.blockentity;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.util.AECableType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public abstract class ManagedGridBlockEntity extends BlockEntity implements IInWorldGridNodeHost, IActionHost {
    private final IManagedGridNode mainNode;

    protected ManagedGridBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState, double idlePowerUsage) {
        super(type, pos, blockState);
        this.mainNode = GridHelper.createManagedNode(this, new IGridNodeListener<ManagedGridBlockEntity>() {
                    @Override
                    public void onSaveChanges(ManagedGridBlockEntity host, IGridNode node) {
                        host.setChanged();
                    }
                })
                .setTagName("main")
                .setInWorldNode(true)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setIdlePowerUsage(idlePowerUsage)
                .setExposedOnSides(EnumSet.allOf(Direction.class));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            GridHelper.onFirstTick(this, ManagedGridBlockEntity::onFirstTick);
        }
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level != null && !level.isClientSide) {
            GridHelper.onFirstTick(this, ManagedGridBlockEntity::onFirstTick);
        }
    }

    private void onFirstTick() {
        if (level != null && !level.isClientSide && !mainNode.isReady()) {
            mainNode.create(level, worldPosition);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        mainNode.loadFromNBT(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        mainNode.saveToNBT(tag);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        mainNode.destroy();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        mainNode.destroy();
    }

    @Nullable
    @Override
    public IGridNode getGridNode(Direction dir) {
        return mainNode.getNode();
    }

    @Override
    public IGridNode getActionableNode() {
        return mainNode.getNode();
    }

    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return AECableType.SMART;
    }

    public IManagedGridNode getMainNode() {
        return mainNode;
    }
}
