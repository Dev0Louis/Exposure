package io.github.mortuusars.exposure.block;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.PlatformHelper;
import io.github.mortuusars.exposure.block.entity.Lightroom;
import io.github.mortuusars.exposure.block.entity.LightroomBlockEntity;
import io.github.mortuusars.exposure.item.DevelopedFilmItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LightroomBlock extends Block implements BlockEntityProvider {
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    public static final BooleanProperty LIT = RedstoneTorchBlock.LIT;

    public LightroomBlock(Settings properties) {
        super(properties);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(LIT, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, LIT);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext pContext) {
        return this.getDefaultState().with(FACING, pContext.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public void onPlaced(@NotNull World pLevel, @NotNull BlockPos pPos, @NotNull BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
        if (pStack.hasCustomName()) {
            BlockEntity blockentity = pLevel.getBlockEntity(pPos);
            if (blockentity instanceof LightroomBlockEntity lightroomBlockEntity)
                lightroomBlockEntity.setCustomName(pStack.getName());
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onStateReplaced(BlockState state, @NotNull World level, @NotNull BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.isOf(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof LightroomBlockEntity lightroomBlockEntity) {
                if (level instanceof ServerWorld serverLevel) {
                    ItemScatterer.spawn(serverLevel, pos, lightroomBlockEntity);
                    lightroomBlockEntity.dropStoredExperience(null);
                }

                level.updateComparators(pos, this);
            }

            super.onStateReplaced(state, level, pos, newState, isMoving);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean hasComparatorOutput(@NotNull BlockState state) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getComparatorOutput(@NotNull BlockState blockState, World level, @NotNull BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof LightroomBlockEntity lightroomBlockEntity) {
            ItemStack filmStack = lightroomBlockEntity.getStack(Lightroom.FILM_SLOT);
            if (filmStack.isEmpty() || !filmStack.hasNbt() || !(filmStack.getItem() instanceof DevelopedFilmItem developedFilmItem))
                return 0;

            int exposedFrames = developedFilmItem.getExposedFramesCount(filmStack);
            int currentFrame = lightroomBlockEntity.getSelectedFrameIndex();

            return MathHelper.floor((currentFrame + 1f) / exposedFrames * 14.0F) + 1;
        }
        else
            return 0;
    }


    @SuppressWarnings("deprecation")
    @Override
    public @NotNull BlockState rotate(BlockState pState, BlockRotation pRotation) {
        return pState.with(FACING, pRotation.rotate(pState.get(FACING)));
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull BlockState mirror(BlockState pState, BlockMirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.get(FACING)));
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull ActionResult onUse(@NotNull BlockState blockState, World level, @NotNull BlockPos pos, @NotNull PlayerEntity player, @NotNull Hand hand, @NotNull BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof LightroomBlockEntity lightroomBlockEntity))
            return ActionResult.FAIL;

        player.incrementStat(Exposure.Stats.INTERACT_WITH_LIGHTROOM);

        if (player instanceof ServerPlayerEntity serverPlayer)
            PlatformHelper.openMenu(serverPlayer, lightroomBlockEntity, buffer -> buffer.writeBlockPos(pos));

        return ActionResult.success(level.isClient);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborUpdate(@NotNull BlockState state, World level, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean pIsMoving) {
        if (!level.isClient) {
            if (!state.get(LIT)) {
                for (Direction direction : Direction.values()) {
                    BlockPos relative = pos.offset(direction);
                    if (relative.equals(fromPos) && level.getEmittedRedstonePower(relative, direction) > 0
                            && level.getBlockEntity(pos) instanceof LightroomBlockEntity lightroomBlockEntity) {
                        lightroomBlockEntity.startPrintingProcess(true);
                        return;
                    }
                }
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return createBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World level, BlockState state, BlockEntityType<T> blockEntityType) {
        return getBlockTicker(level, state, blockEntityType);
    }

    @SuppressWarnings("unused")
    public static <T extends BlockEntity> BlockEntityTicker<T> getBlockTicker(World level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (!level.isClient && blockEntityType.equals(Exposure.BlockEntityTypes.LIGHTROOM.get()))
            return LightroomBlockEntity::serverTick;

        return null;
    }
}
