package io.github.mortuusars.exposure.menu;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class AlbumPhotographSlot extends Slot {
    private boolean isActive;

    public AlbumPhotographSlot(Inventory container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canTakeItems(PlayerEntity player) {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }

    public void setActive(boolean value) {
        isActive = value;
    }
}
