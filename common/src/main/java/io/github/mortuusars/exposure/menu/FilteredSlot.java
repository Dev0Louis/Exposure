package io.github.mortuusars.exposure.menu;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class FilteredSlot extends Slot {
    private final Consumer<SlotChangedArgs> onItemChanged;
    private final int maxStackSize;
    private final int slot;
    private final Predicate<ItemStack> mayPlacePredicate;

    public FilteredSlot(Inventory container, int slot, int x, int y, int maxStackSize, Consumer<SlotChangedArgs> onItemChanged,
                        Predicate<ItemStack> mayPlacePredicate) {
        super(container, slot, x, y);
        Preconditions.checkArgument(maxStackSize > 0 && maxStackSize <= 64, maxStackSize + " is not valid. (1-64)");
        this.slot = slot;
        this.maxStackSize = maxStackSize;
        this.onItemChanged = onItemChanged;
        this.mayPlacePredicate = mayPlacePredicate;
    }

    public int getSlotId() {
        return slot;
    }

    @Override
    public int getMaxItemCount() {
        return Math.min(inventory.getMaxCountPerStack(), maxStackSize);
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return mayPlacePredicate.test(stack);
    }

    @Override
    public void setStackNoCallbacks(ItemStack stack) {
        ItemStack oldStack = getStack().copy();
        super.setStackNoCallbacks(stack);
        onItemChanged.accept(new SlotChangedArgs(this, oldStack, getStack()));
    }

    @Override
    public @NotNull ItemStack takeStack(int amount) {
        ItemStack oldStack = getStack().copy();
        ItemStack removed = super.takeStack(amount);
        ItemStack newStack = getStack();
        onItemChanged.accept(new SlotChangedArgs(this, oldStack, newStack));
        return removed;
    }

    public record SlotChangedArgs(FilteredSlot slot, ItemStack oldStack, ItemStack newStack) {}
}
