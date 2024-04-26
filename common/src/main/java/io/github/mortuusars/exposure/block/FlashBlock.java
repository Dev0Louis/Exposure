package io.github.mortuusars.exposure.block;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.block.entity.FlashBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Items;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public class FlashBlock extends Block implements BlockEntityProvider, Waterloggable {
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    public FlashBlock(Settings properties) {
        super(properties);
        this.setDefaultState(this.stateManager.getDefaultState().with(WATERLOGGED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(WATERLOGGED);
    }

    @Override
    public boolean isTransparent(@NotNull BlockState blockState, @NotNull BlockView level, @NotNull BlockPos pos) {
        return true;
    }

    @Override
    public @NotNull BlockRenderType getRenderType(@NotNull BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Override
    public @NotNull VoxelShape getOutlineShape(@NotNull BlockState pState, @NotNull BlockView pLevel, @NotNull BlockPos pPos, ShapeContext pContext) {
        return pContext.isHolding(Items.LIGHT) ? VoxelShapes.fullCube() : VoxelShapes.empty();
    }

    @Override
    public float getAmbientOcclusionLightLevel(@NotNull BlockState state, @NotNull BlockView level, @NotNull BlockPos pos) {
        return 1.0F;
    }

    @Override
    public @NotNull BlockState getStateForNeighborUpdate(BlockState pState, @NotNull Direction pDirection, @NotNull BlockState pNeighborState, @NotNull WorldAccess pLevel, @NotNull BlockPos pCurrentPos, @NotNull BlockPos pNeighborPos) {
        if (pState.get(WATERLOGGED)) {
            pLevel.scheduleFluidTick(pCurrentPos, Fluids.WATER, Fluids.WATER.getTickRate(pLevel));
        }

        return super.getStateForNeighborUpdate(pState, pDirection, pNeighborState, pLevel, pCurrentPos, pNeighborPos);
    }

    public @NotNull FluidState getFluidState(BlockState pState) {
        return pState.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(pState);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(@NotNull BlockPos pos, @NotNull BlockState blockState) {
        return new FlashBlockEntity(pos, blockState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World level, @NotNull BlockState state, @NotNull BlockEntityType<T> blockEntityType) {
        if (!level.isClient && blockEntityType == Exposure.BlockEntityTypes.FLASH.get())
            return FlashBlockEntity::serverTick;

        return null;
    }
}
