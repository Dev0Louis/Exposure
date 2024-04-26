package io.github.mortuusars.exposure.block.entity;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.block.FlashBlock;
import java.util.Objects;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class FlashBlockEntity extends BlockEntity {
    private int ticks;
    public FlashBlockEntity(BlockPos pos, BlockState blockState) {
        super(Exposure.BlockEntityTypes.FLASH.get(), pos, blockState);
        ticks = 6;
    }

    @SuppressWarnings("unused")
    public static <T extends BlockEntity> void serverTick(World level, BlockPos blockPos, BlockState blockState, T blockEntity) {
        if (blockEntity instanceof FlashBlockEntity flashBlockEntity)
            flashBlockEntity.tick();
    }

    protected void tick() {
        ticks--;
        if (ticks <= 0) {
            BlockState blockState = Objects.requireNonNull(world).getBlockState(getPos());
            world.setBlockState(getPos(), blockState.get(FlashBlock.WATERLOGGED) ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        }
    }
}
