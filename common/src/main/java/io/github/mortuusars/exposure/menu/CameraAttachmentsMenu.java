package io.github.mortuusars.exposure.menu;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.item.CameraItem;
import io.github.mortuusars.exposure.sound.OnePerPlayerSounds;
import io.github.mortuusars.exposure.util.ItemAndStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

public class CameraAttachmentsMenu extends ScreenHandler {
    private final int attachmentSlots;
    private final PlayerEntity player;
    private final World level;
    private final ItemAndStack<CameraItem> camera;

    private boolean contentsInitialized;

    public CameraAttachmentsMenu(int containerId, PlayerInventory playerInventory, ItemStack cameraStack) {
        super(Exposure.MenuTypes.CAMERA.get(), containerId);
        player = playerInventory.player;
        level = playerInventory.player.getWorld();
        camera = new ItemAndStack<>(cameraStack);
        List<CameraItem.AttachmentType> attachmentTypes = camera.getItem().getAttachmentTypes(camera.getStack());

        SimpleInventory container = new SimpleInventory(getCameraAttachments(camera).toArray(ItemStack[]::new)) {
            @Override
            public int getMaxCountPerStack() {
                return 1;
            }
        };

        this.attachmentSlots = addSlotsForAttachments(container);

        addPlayerSlots(playerInventory);
    }

    @Override
    public void updateSlotStacks(int stateId, List<ItemStack> items, ItemStack carried) {
        contentsInitialized = false;
        super.updateSlotStacks(stateId, items, carried);
        contentsInitialized = true;
    }

    protected int addSlotsForAttachments(Inventory container) {
        int attachmentSlots = 0;

        int[][] slots = new int[][]{
                // SlotId, x, y, maxStackSize
                {CameraItem.FILM_ATTACHMENT.slot(), 13, 42, 1},
                {CameraItem.FLASH_ATTACHMENT.slot(), 147, 15, 1},
                {CameraItem.LENS_ATTACHMENT.slot(), 147, 43, 1},
                {CameraItem.FILTER_ATTACHMENT.slot(), 147, 71, 1}
        };

        for (int[] slot : slots) {
            Optional<CameraItem.AttachmentType> attachment = camera.getItem()
                    .getAttachmentTypeForSlot(camera.getStack(), slot[0]);

            if (attachment.isPresent()) {
                addSlot(new FilteredSlot(container, slot[0], slot[1], slot[2], slot[3],
                        this::onItemInSlotChanged, attachment.get().stackValidator()));
                attachmentSlots++;
            }
        }

        return attachmentSlots;
    }

    protected void addPlayerSlots(PlayerInventory playerInventory) {
        //Player Inventory
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, (column + row * 9) + 9, column * 18 + 8, 103 + row * 18));
            }
        }

        //Hotbar
        for (int slot = 0; slot < 9; slot++) {
            int finalSlot = slot;
            addSlot(new Slot(playerInventory, finalSlot, slot * 18 + 8, 161) {
                @Override
                public boolean canTakeItems(@NotNull PlayerEntity player) {
                    return super.canTakeItems(player) && player.getInventory().selectedSlot != finalSlot;
                }
            });
        }
    }

    protected void onItemInSlotChanged(FilteredSlot.SlotChangedArgs args) {
        if (!level.isClient) {
            camera.getItem().getAttachmentTypeForSlot(camera.getStack(), args.slot().getSlotId())
                    .ifPresent(attachmentType -> camera.getItem()
                            .setAttachment(camera.getStack(), attachmentType, args.newStack()));
            return;
        }

        if (!contentsInitialized)
            return;

        int slotId = args.slot().getSlotId();
        ItemStack oldStack = args.oldStack();
        ItemStack newStack = args.newStack();

        if (slotId == CameraItem.FILM_ATTACHMENT.slot()) {
            if (!newStack.isEmpty())
                OnePerPlayerSounds.play(player, Exposure.SoundEvents.FILM_ADVANCE.get(), SoundCategory.PLAYERS, 0.9f, 1f);
        } else if (slotId == CameraItem.FLASH_ATTACHMENT.slot()) {
            if (!newStack.isEmpty())
                OnePerPlayerSounds.play(player, Exposure.SoundEvents.CAMERA_BUTTON_CLICK.get(), SoundCategory.PLAYERS, 0.8f, 1f);
        } else if (slotId == CameraItem.LENS_ATTACHMENT.slot()) {
            if (!oldStack.isOf(newStack.getItem())) {
                OnePerPlayerSounds.play(player, newStack.isEmpty() ?
                        SoundEvents.ITEM_SPYGLASS_STOP_USING : SoundEvents.ITEM_SPYGLASS_USE, SoundCategory.PLAYERS, 0.9f, 1f);
            }
        } else if (slotId == CameraItem.FILTER_ATTACHMENT.slot()) {
            if (!newStack.isEmpty() && !oldStack.isOf(newStack.getItem())) {
                OnePerPlayerSounds.play(player, Exposure.SoundEvents.FILTER_PLACE.get(), SoundCategory.PLAYERS, 0.8f,
                        level.getRandom().nextFloat() * 0.2f + 0.9f);
            }
        }
    }

    private static DefaultedList<ItemStack> getCameraAttachments(ItemAndStack<CameraItem> camera) {
        DefaultedList<ItemStack> items = DefaultedList.of();

        List<CameraItem.AttachmentType> attachmentTypes = camera.getItem().getAttachmentTypes(camera.getStack());
        for (CameraItem.AttachmentType attachmentType : attachmentTypes) {
            items.add(camera.getItem().getAttachment(camera.getStack(), attachmentType).orElse(ItemStack.EMPTY));
        }

        return items;
    }

    @Override
    public @NotNull ItemStack quickMove(@NotNull PlayerEntity player, int slotIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot clickedSlot = this.slots.get(slotIndex);
        if (clickedSlot.hasStack()) {
            ItemStack slotStack = clickedSlot.getStack();
            itemstack = slotStack.copy();
            if (slotIndex < attachmentSlots) {
                if (!this.insertItem(slotStack, attachmentSlots, this.slots.size(), true))
                    return ItemStack.EMPTY;
            } else {
                if (!this.insertItem(slotStack, 0, attachmentSlots, false))
                    return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty())
                clickedSlot.setStackNoCallbacks(ItemStack.EMPTY);
            else
                clickedSlot.markDirty();
        }

        return itemstack;
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
                ItemStack movedStack1 = slot1.getStack();
                if (movedStack1.isEmpty() && slot1.canInsert(movedStack)) {
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
        return player.getMainHandStack().getItem() instanceof CameraItem
                || player.getOffHandStack().getItem() instanceof CameraItem;
    }

    public static CameraAttachmentsMenu fromBuffer(int containerId, PlayerInventory playerInventory, PacketByteBuf buffer) {
        return new CameraAttachmentsMenu(containerId, playerInventory, buffer.readItemStack());
    }
}
