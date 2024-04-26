package io.github.mortuusars.exposure.menu;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.block.entity.Lightroom;
import io.github.mortuusars.exposure.block.entity.LightroomBlockEntity;
import io.github.mortuusars.exposure.item.DevelopedFilmItem;
import io.github.mortuusars.exposure.item.IFilmItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.MathHelper;

public class LightroomMenu extends ScreenHandler {
    public static final int PRINT_BUTTON_ID = 0;
    public static final int PRINT_CREATIVE_BUTTON_ID = 1;
    public static final int PREVIOUS_FRAME_BUTTON_ID = 2;
    public static final int NEXT_FRAME_BUTTON_ID = 3;
    public static final int TOGGLE_PROCESS_BUTTON_ID = 4;

    private final LightroomBlockEntity lightroomBlockEntity;
    private final PropertyDelegate data;

    private NbtList frames = new NbtList();

    public LightroomMenu(int containerId, final PlayerInventory playerInventory, final LightroomBlockEntity blockEntity, PropertyDelegate containerData) {
        super(Exposure.MenuTypes.LIGHTROOM.get(), containerId);
        this.lightroomBlockEntity = blockEntity;
        this.data = containerData;

        {
            this.addSlot(new Slot(blockEntity, Lightroom.FILM_SLOT, -20, 42) {
                @Override
                public boolean canInsert(ItemStack stack) {
                    return blockEntity.isItemValidForSlot(Lightroom.FILM_SLOT, stack);
                }

                @Override
                public void markDirty() {
                    frames = getStack().getItem() instanceof DevelopedFilmItem developedFilm ?
                            developedFilm.getExposedFrames(getStack()) : new NbtList();
                    if (lightroomBlockEntity.getWorld() != null && !lightroomBlockEntity.getWorld().isClient)
                        data.set(LightroomBlockEntity.CONTAINER_DATA_SELECTED_FRAME_ID, 0);
                    super.markDirty();
                }
            });

            this.addSlot(new Slot(blockEntity, Lightroom.PAPER_SLOT, 8, 92){
                @Override
                public boolean canInsert(ItemStack stack) {
                    return blockEntity.isItemValidForSlot(Lightroom.PAPER_SLOT, stack);
                }
            });
            this.addSlot(new Slot(blockEntity, Lightroom.CYAN_SLOT, 42, 92){
                @Override
                public boolean canInsert(ItemStack stack) {
                    return blockEntity.isItemValidForSlot(Lightroom.CYAN_SLOT, stack);
                }
            });
            this.addSlot(new Slot(blockEntity, Lightroom.MAGENTA_SLOT, 60, 92){
                @Override
                public boolean canInsert(ItemStack stack) {
                    return blockEntity.isItemValidForSlot(Lightroom.MAGENTA_SLOT, stack);
                }
            });
            this.addSlot(new Slot(blockEntity, Lightroom.YELLOW_SLOT, 78, 92){
                @Override
                public boolean canInsert(ItemStack stack) {
                    return blockEntity.isItemValidForSlot(Lightroom.YELLOW_SLOT, stack);
                }
            });
            this.addSlot(new Slot(blockEntity, Lightroom.BLACK_SLOT, 96, 92){
                @Override
                public boolean canInsert(ItemStack stack) {
                    return blockEntity.isItemValidForSlot(Lightroom.BLACK_SLOT, stack);
                }
            });

            // OUTPUT
            this.addSlot(new Slot(blockEntity, Lightroom.RESULT_SLOT, 148, 92) {
                @Override
                public boolean canInsert(@NotNull ItemStack stack) {
                    return false;
                }

                @Override
                public void onTakeItem(@NotNull PlayerEntity player, @NotNull ItemStack pStack) {
                    super.onTakeItem(player, pStack);
                    blockEntity.dropStoredExperience(player);
                }

                @Override
                public void onQuickTransfer(@NotNull ItemStack oldStackIn, @NotNull ItemStack newStackIn) {
                    super.onQuickTransfer(oldStackIn, newStackIn);
                    blockEntity.dropStoredExperience(playerInventory.player);
                }
            });
        }

        // Player inventory slots
        for(int row = 0; row < 3; ++row) {
            for(int column = 0; column < 9; ++column) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 127 + row * 18));
            }
        }

        // Player hotbar slots
        // Hotbar should go after main inventory for Shift+Click to work properly.
        for(int index = 0; index < 9; ++index) {
            this.addSlot(new Slot(playerInventory, index, 8 + index * 18, 185));
        }

        this.addProperties(data);
    }

    public static LightroomMenu fromBuffer(int containerID, PlayerInventory playerInventory, PacketByteBuf buffer) {
        return new LightroomMenu(containerID, playerInventory, getBlockEntity(playerInventory, buffer),
                new ArrayPropertyDelegate(LightroomBlockEntity.CONTAINER_DATA_SIZE));
    }

    public LightroomBlockEntity getBlockEntity() {
        return lightroomBlockEntity;
    }

    public PropertyDelegate getData() {
        return data;
    }

    public NbtList getExposedFrames() {
        return frames;
    }

    public @Nullable NbtCompound getFrameIdByIndex(int index) {
        return index >= 0 && index < getExposedFrames().size() ? getExposedFrames().getCompound(index) : null;
    }

    public int getSelectedFrame() {
        return data.get(LightroomBlockEntity.CONTAINER_DATA_SELECTED_FRAME_ID);
    }

    public boolean isPrinting() {
        return data.get(LightroomBlockEntity.CONTAINER_DATA_PRINT_TIME_ID) > 0;
    }

    public int getTotalFrames() {
        ItemStack filmStack = getBlockEntity().getStack(Lightroom.FILM_SLOT);
        return (!filmStack.isEmpty() && filmStack.getItem() instanceof IFilmItem filmItem) ? filmItem.getExposedFramesCount(filmStack) : 0;
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int buttonId) {
        Preconditions.checkState(!player.getWorld().isClient, "This should be server-side only.");

        if (buttonId == PREVIOUS_FRAME_BUTTON_ID || buttonId == NEXT_FRAME_BUTTON_ID) {
            ItemStack filmStack = getBlockEntity().getStack(Lightroom.FILM_SLOT);
            if (!filmStack.isEmpty() && filmStack.getItem() instanceof DevelopedFilmItem) {
                int frames = getTotalFrames();
                if (frames == 0)
                    return true;

                int selectedFrame = data.get(LightroomBlockEntity.CONTAINER_DATA_SELECTED_FRAME_ID);
                selectedFrame = selectedFrame + (buttonId == NEXT_FRAME_BUTTON_ID ? 1 : -1);
                selectedFrame = MathHelper.clamp(selectedFrame, 0, frames - 1);
                data.set(LightroomBlockEntity.CONTAINER_DATA_SELECTED_FRAME_ID, selectedFrame);
                return true;
            }
        }

        if (buttonId == TOGGLE_PROCESS_BUTTON_ID) {
            Lightroom.Process currentProcess = getBlockEntity().getProcess();
            getBlockEntity().setProcess(currentProcess == Lightroom.Process.CHROMATIC ? Lightroom.Process.REGULAR : Lightroom.Process.CHROMATIC);
        }

        if (buttonId == PRINT_BUTTON_ID) {
            getBlockEntity().startPrintingProcess(false);
            return true;
        }

        if (buttonId == PRINT_CREATIVE_BUTTON_ID) {
            if (player.isCreative())
                getBlockEntity().printInCreativeMode();
        }

        return false;
    }

    @Override
    public @NotNull ItemStack quickMove(@NotNull PlayerEntity player, int index) {
        Slot slot = slots.get(index);
        ItemStack clickedStack = slot.getStack();
        ItemStack returnedStack = clickedStack.copy();

         if (index < Lightroom.SLOTS) {
            if (!insertItem(clickedStack, Lightroom.SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }

            if (index == Lightroom.RESULT_SLOT)
                slot.onQuickTransfer(clickedStack, returnedStack);
        }
        else if (index < slots.size()) {
            if (!insertItem(clickedStack, 0, Lightroom.SLOTS, false))
                return ItemStack.EMPTY;
        }

        if (clickedStack.isEmpty()) {
            slot.setStackNoCallbacks(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }

        return returnedStack;
    }

    /**
     * Fixed method to respect slot photo limit.
     */
    @Override
    protected boolean insertItem(ItemStack movedStack, int startIndex, int endIndex, boolean reverseDirection) {
        boolean hasRemainder = false;
        int i = startIndex;
        if (reverseDirection) {
            i = endIndex - 1;
        }
        if (movedStack.isStackable()) {
            while (!movedStack.isEmpty() && !(!reverseDirection ? i >= endIndex : i < startIndex)) {
                Slot slot = this.slots.get(i);
                ItemStack slotStack = slot.getStack();
                if (!slotStack.isEmpty() && ItemStack.canCombine(movedStack, slotStack)) {
                    int maxSize;
                    int j = slotStack.getCount() + movedStack.getCount();
                    if (j <= (maxSize = Math.min(slot.getMaxItemCount(), movedStack.getMaxCount()))) {
                        movedStack.setCount(0);
                        slotStack.setCount(j);
                        slot.markDirty();
                        hasRemainder = true;
                    } else if (slotStack.getCount() < maxSize) {
                        movedStack.decrement(maxSize - slotStack.getCount());
                        slotStack.setCount(maxSize);
                        slot.markDirty();
                        hasRemainder = true;
                    }
                }
                if (reverseDirection) {
                    --i;
                    continue;
                }
                ++i;
            }
        }
        if (!movedStack.isEmpty()) {
            i = reverseDirection ? endIndex - 1 : startIndex;
            while (!(!reverseDirection ? i >= endIndex : i < startIndex)) {
                Slot slot1 = this.slots.get(i);
                ItemStack itemmovedStack1 = slot1.getStack();
                if (itemmovedStack1.isEmpty() && slot1.canInsert(movedStack)) {
                    if (movedStack.getCount() > slot1.getMaxItemCount()) {
                        slot1.setStack(movedStack.split(slot1.getMaxItemCount()));
                    } else {
                        slot1.setStack(movedStack.split(movedStack.getCount()));
                    }
                    slot1.markDirty();
                    hasRemainder = true;
                    break;
                }
                if (reverseDirection) {
                    --i;
                    continue;
                }
                ++i;
            }
        }
        return hasRemainder;
    }

    @Override
    public boolean canUse(@NotNull PlayerEntity player) {
        return lightroomBlockEntity.canPlayerUse(player);
    }

    private static LightroomBlockEntity getBlockEntity(final PlayerInventory playerInventory, final PacketByteBuf data) {
        Objects.requireNonNull(playerInventory, "playerInventory cannot be null");
        Objects.requireNonNull(data, "data cannot be null");
        final BlockEntity blockEntityAtPos = playerInventory.player.getWorld().getBlockEntity(data.readBlockPos());
        if (blockEntityAtPos instanceof LightroomBlockEntity blockEntity)
            return blockEntity;
        throw new IllegalStateException("Block entity is not correct! " + blockEntityAtPos);
    }
}
