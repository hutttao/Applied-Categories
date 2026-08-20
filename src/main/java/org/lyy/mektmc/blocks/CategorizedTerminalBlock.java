package org.lyy.mektmc.blocks;

import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.lyy.mektmc.blockentity.CategorizedTerminalBlockEntity;
import org.lyy.mektmc.registries.MenuRegistry;

public class CategorizedTerminalBlock extends BaseEntityBlock {
    public static final MapCodec<CategorizedTerminalBlock> CODEC = simpleCodec(CategorizedTerminalBlock::new);
    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    private static final VoxelShape NORTH_SHAPE = box(2.0, 2.0, 0.0, 14.0, 14.0, 5.0);
    private static final VoxelShape SOUTH_SHAPE = box(2.0, 2.0, 11.0, 14.0, 14.0, 16.0);
    private static final VoxelShape WEST_SHAPE = box(0.0, 2.0, 2.0, 5.0, 14.0, 14.0);
    private static final VoxelShape EAST_SHAPE = box(11.0, 2.0, 2.0, 16.0, 14.0, 14.0);
    private static final VoxelShape UP_SHAPE = box(2.0, 11.0, 2.0, 14.0, 16.0, 14.0);
    private static final VoxelShape DOWN_SHAPE = box(2.0, 0.0, 2.0, 14.0, 5.0, 14.0);

    public CategorizedTerminalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            case UP -> UP_SHAPE;
            case DOWN -> DOWN_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof CategorizedTerminalBlockEntity blockEntity) {
            MenuOpener.open(MenuRegistry.CATEGORIZED_TERMINAL.get(), serverPlayer, MenuLocators.forBlockEntity(blockEntity));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CategorizedTerminalBlockEntity(pos, state);
    }
}
