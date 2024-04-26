package io.github.mortuusars.exposure.item;

import com.google.common.base.Preconditions;
import com.mojang.datafixers.util.Either;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.infrastructure.FrameData;
import io.github.mortuusars.exposure.gui.ClientGUI;
import io.github.mortuusars.exposure.gui.component.PhotographTooltip;
import io.github.mortuusars.exposure.entity.PhotographEntity;
import io.github.mortuusars.exposure.util.ItemAndStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.item.TooltipData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ClickType;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public class StackedPhotographsItem extends Item {
    public static final String PHOTOGRAPHS_TAG = "Photographs";

    public StackedPhotographsItem(Settings properties) {
        super(properties);
    }

    /**
     * @return How many photographs can be stacked together.
     */
    public int getStackLimit() {
        return Config.Common.STACKED_PHOTOGRAPHS_MAX_SIZE.get();
    }

    public int getPhotographsCount(ItemStack stack) {
        return getOrCreatePhotographsListTag(stack).size();
    }

    public List<ItemAndStack<PhotographItem>> getPhotographs(ItemStack stack) {
        return getPhotographs(stack, getStackLimit());
    }

    public List<ItemAndStack<PhotographItem>> getPhotographs(ItemStack stack, int limit) {
        NbtList listTag = getOrCreatePhotographsListTag(stack);
        if (listTag.isEmpty())
            return Collections.emptyList();

        List<ItemAndStack<PhotographItem>> photographs = new ArrayList<>();
        for (int i = 0; i < Math.min(listTag.size(), limit); i++) {
            photographs.add(getPhotograph(listTag, i));
        }

        return photographs;
    }

    public boolean canAddPhotograph(ItemStack stack) {
        return getPhotographsCount(stack) < getStackLimit();
    }

    public void addPhotograph(ItemStack stack, ItemStack photographStack, int index) {
        Preconditions.checkState(index >= 0 && index <= getPhotographsCount(stack), index + " is out of bounds. Count: " + getPhotographsCount(stack));
        Preconditions.checkState(canAddPhotograph(stack), "Cannot add more photographs than this photo can store. Max count: " + getStackLimit());
        NbtList listTag = getOrCreatePhotographsListTag(stack);
        listTag.add(index, photographStack.writeNbt(new NbtCompound()));
        stack.getOrCreateNbt().put(PHOTOGRAPHS_TAG, listTag);
    }

    public void addPhotographOnTop(ItemStack stack, ItemStack photographStack) {
        addPhotograph(stack, photographStack, 0);
    }

    public void addPhotographToBottom(ItemStack stack, ItemStack photographStack) {
        addPhotograph(stack, photographStack, getPhotographsCount(stack));
    }

    public ItemAndStack<PhotographItem> removePhotograph(ItemStack stack, int index) {
        Preconditions.checkState(index >= 0 && index < getPhotographsCount(stack), index + " is out of bounds. Count: " + getPhotographsCount(stack));

        NbtList listTag = getOrCreatePhotographsListTag(stack);
        ItemStack photographStack = ItemStack.fromNbt((NbtCompound)listTag.remove(index));
        stack.getOrCreateNbt().put(PHOTOGRAPHS_TAG, listTag);

        return new ItemAndStack<>(photographStack);
    }

    public ItemAndStack<PhotographItem> removeTopPhotograph(ItemStack stack) {
        return removePhotograph(stack, 0);
    }

    public ItemAndStack<PhotographItem> removeBottomPhotograph(ItemStack stack) {
        return removePhotograph(stack, getPhotographsCount(stack) - 1);
    }

    private NbtList getOrCreatePhotographsListTag(ItemStack stack) {
        return stack.getNbt() != null ? stack.getOrCreateNbt().getList(PHOTOGRAPHS_TAG, NbtElement.COMPOUND_TYPE) : new NbtList();
    }

    private ItemAndStack<PhotographItem> getPhotograph(NbtList photographsList, int index) {
        NbtCompound stackTag = photographsList.getCompound(index);
        ItemStack stack = ItemStack.fromNbt(stackTag);
        return new ItemAndStack<>(stack);
    }

    public @Nullable Either<String, Identifier> getFirstIdOrTexture(ItemStack stack) {
        NbtList listTag = getOrCreatePhotographsListTag(stack);
        if (listTag.isEmpty())
            return null;

        NbtCompound first = listTag.getCompound(0).getCompound("tag");
        String id = first.getString(FrameData.ID);
        if (!id.isEmpty())
            return Either.left(id);

        String resource = first.getString(FrameData.TEXTURE);
        if (!resource.isEmpty())
            return Either.right(new Identifier(resource));

        return null;
    }

    public List<@Nullable Either<String, Identifier>> getTopPhotographs(ItemStack stack, int count) {
        Preconditions.checkArgument(count > 0, "count '{}' is not valid. > 0", count);

        List<@Nullable Either<String, Identifier>> photographs = new ArrayList<>();
        NbtList listTag = getOrCreatePhotographsListTag(stack);

        for (int i = 0; i < Math.min(listTag.size(), count); i++) {
            NbtCompound photographTag = listTag.getCompound(i).getCompound("tag");

            String id = photographTag.getString(FrameData.ID);
            if (!id.isEmpty()) {
                photographs.add(Either.left(id));
                continue;
            }

            String resource = photographTag.getString(FrameData.TEXTURE);
            if (!resource.isEmpty()) {
                photographs.add(Either.right(new Identifier(resource)));
                continue;
            }

            photographs.add(null);
        }

        while (photographs.size() < count) {
            photographs.add(null);
        }

        return photographs;
    }

    // ---

    @Override
    public @NotNull Optional<TooltipData> getTooltipData(@NotNull ItemStack stack) {
        List<ItemAndStack<PhotographItem>> photographs = getPhotographs(stack);
        if (photographs.isEmpty())
            return Optional.empty();

        return Optional.of(new PhotographTooltip(new ItemAndStack<>(stack)));
    }

    @Override
    public boolean onStackClicked(@NotNull ItemStack stack, @NotNull Slot slot, @NotNull ClickType action, @NotNull PlayerEntity player) {
        if (action != ClickType.RIGHT || getPhotographsCount(stack) == 0 || !slot.canInsert(new ItemStack(Exposure.Items.PHOTOGRAPH.get())))
            return false;

        ItemStack slotItem = slot.getStack();
        if (slotItem.isEmpty()) {
            ItemAndStack<PhotographItem> photograph = removeBottomPhotograph(stack);
            slot.setStackNoCallbacks(photograph.getStack());

            if (getPhotographsCount(stack) == 1)
                player.currentScreenHandler.setCursorStack(removeTopPhotograph(stack).getStack());

            playRemoveSoundClientside(player);

            return true;
        }

        if (slotItem.getItem() instanceof PhotographItem && canAddPhotograph(stack)) {
            addPhotographToBottom(stack, slotItem);
            slot.setStackNoCallbacks(ItemStack.EMPTY);

            playAddSoundClientside(player);

            return true;
        }

        return false;
    }

    @Override
    public boolean onClicked(@NotNull ItemStack stack, @NotNull ItemStack other, @NotNull Slot slot, @NotNull ClickType action, @NotNull PlayerEntity player, @NotNull StackReference access) {
        if (action != ClickType.RIGHT || !slot.canInsert(new ItemStack(Exposure.Items.PHOTOGRAPH.get())))
            return false;

        if (getPhotographsCount(stack) > 0 && other.isEmpty()) {
            ItemAndStack<PhotographItem> photograph = removeTopPhotograph(stack);
            access.set(photograph.getStack());

            if (getPhotographsCount(stack) == 1) {
                ItemAndStack<PhotographItem> lastPhotograph = removeTopPhotograph(stack);
                slot.setStackNoCallbacks(lastPhotograph.getStack());
            }

            playRemoveSoundClientside(player);

            return true;
        }

        if (other.getItem() instanceof PhotographItem) {
            if (canAddPhotograph(stack)) {
                addPhotographOnTop(stack, other);
                access.set(ItemStack.EMPTY);

                playAddSoundClientside(player);

                return true;
            }
            else
                return false;
        }

        if (other.getItem() instanceof StackedPhotographsItem otherStackedItem) {
            int otherCount = otherStackedItem.getPhotographsCount(other);
            int addedCount = 0;
            for (int i = 0; i < otherCount; i++) {
                if (canAddPhotograph(stack)) {
                    ItemAndStack<PhotographItem> photograph = otherStackedItem.removeBottomPhotograph(other);
                    addPhotographOnTop(stack, photograph.getStack());
                    addedCount++;
                }
            }

            if (otherStackedItem.getPhotographsCount(other) == 0)
                access.set(ItemStack.EMPTY);
            else if (otherStackedItem.getPhotographsCount(other) == 1)
                access.set(otherStackedItem.removeTopPhotograph(other).getStack());

            if (addedCount > 0)
                playAddSoundClientside(player);

            return true;
        }

        return false;
    }

    @Override
    public @NotNull ActionResult useOnBlock(ItemUsageContext context) {
        BlockPos clickedPos = context.getBlockPos();
        Direction direction = context.getSide();
        BlockPos resultPos = clickedPos.offset(direction);
        PlayerEntity player = context.getPlayer();
        ItemStack itemInHand = context.getStack();

        if (itemInHand.getItem() != this || getPhotographsCount(itemInHand) == 0)
            return ActionResult.FAIL;

        if (player == null || player.getWorld().isOutOfHeightLimit(resultPos) || !player.canPlaceOn(resultPos, direction, itemInHand))
            return ActionResult.FAIL;

        if (player.shouldCancelInteraction()) {
            cyclePhotographs(itemInHand, player);
            return ActionResult.SUCCESS;
        }

        ItemAndStack<PhotographItem> topPhotograph = removeTopPhotograph(itemInHand);

        World level = context.getWorld();
        PhotographEntity photographEntity = new PhotographEntity(level, resultPos, direction, topPhotograph.getStack().copy());

        if (photographEntity.canStayAttached()) {
            if (!level.isClient) {
                photographEntity.onPlace();
                level.emitGameEvent(player, GameEvent.ENTITY_PLACE, photographEntity.getPos());
                level.spawnEntity(photographEntity);
            }

            if (!player.isCreative()) {
                int photographsCount = getPhotographsCount(itemInHand);
                if (photographsCount == 0)
                    itemInHand.decrement(1);
                else if (photographsCount == 1)
                    player.setStackInHand(context.getHand(), removeTopPhotograph(itemInHand).getStack());
            }
            else {
                // Because in creative you don't get photograph back when breaking Photograph entity,
                // we don't remove placed photograph from the photo.
                addPhotographOnTop(itemInHand, topPhotograph.getStack());
            }
        }
        else {
            addPhotographOnTop(itemInHand, topPhotograph.getStack());
        }

        return ActionResult.success(level.isClient);
    }

    @Override
    public @NotNull TypedActionResult<ItemStack> use(@NotNull World level, PlayerEntity player, @NotNull Hand hand) {
        ItemStack itemInHand = player.getStackInHand(hand);

        if (player.shouldCancelInteraction()) {
            cyclePhotographs(itemInHand, player);
            return TypedActionResult.success(itemInHand);
        }

        List<ItemAndStack<PhotographItem>> photographs = getPhotographs(itemInHand);
        if (!photographs.isEmpty()) {
            if (level.isClient) {
                ClientGUI.openPhotographScreen(photographs);
                player.playSound(Exposure.SoundEvents.PHOTOGRAPH_RUSTLE.get(), 0.6f, 1.1f);
            }
            return TypedActionResult.success(itemInHand);
        }

        return TypedActionResult.fail(itemInHand);
    }

    public boolean cyclePhotographs(ItemStack stack, @Nullable PlayerEntity player) {
        if (getPhotographsCount(stack) < 2)
            return false;

        ItemAndStack<PhotographItem> topPhotograph = removeTopPhotograph(stack);
        addPhotographToBottom(stack, topPhotograph.getStack());
        if (player != null) {
            player.getWorld().playSoundFromEntity(player, player, Exposure.SoundEvents.PHOTOGRAPH_RUSTLE.get(), SoundCategory.PLAYERS, 0.6f,
                player.getWorld().getRandom().nextFloat() * 0.2f + 1.2f);
            player.emitGameEvent(GameEvent.ITEM_INTERACT_FINISH);
        }

        return true;
    }

    public static void playAddSoundClientside(PlayerEntity player) {
        if (player.getWorld().isClient)
            player.playSound(Exposure.SoundEvents.PHOTOGRAPH_RUSTLE.get(), 0.6f,
                    player.getWorld().getRandom().nextFloat() * 0.2f + 1.2f);
    }

    public static void playRemoveSoundClientside(PlayerEntity player) {
        if (player.getWorld().isClient)
            player.playSound(Exposure.SoundEvents.PHOTOGRAPH_RUSTLE.get(), 0.75f,
                    player.getWorld().getRandom().nextFloat() * 0.2f + 0.75f);
    }
}
