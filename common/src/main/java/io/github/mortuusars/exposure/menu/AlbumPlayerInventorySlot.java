package io.github.mortuusars.exposure.menu;

import net.minecraft.inventory.Inventory;
import net.minecraft.screen.slot.Slot;

public class AlbumPlayerInventorySlot extends Slot {

    private boolean isActive;

    public AlbumPlayerInventorySlot(Inventory container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }

    public void setActive(boolean value) {
        isActive = value;
    }
}
