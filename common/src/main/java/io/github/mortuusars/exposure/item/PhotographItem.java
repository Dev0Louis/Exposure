package io.github.mortuusars.exposure.item;

import com.google.common.base.Preconditions;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.PlatformHelper;
import io.github.mortuusars.exposure.camera.infrastructure.FrameData;
import io.github.mortuusars.exposure.entity.PhotographEntity;
import io.github.mortuusars.exposure.gui.ClientGUI;
import io.github.mortuusars.exposure.gui.component.PhotographTooltip;
import io.github.mortuusars.exposure.util.ItemAndStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.item.TooltipData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ClickType;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringHelper;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public class PhotographItem extends Item {
    public PhotographItem(Settings properties) {
        super(properties);
    }

    public @Nullable Either<String, Identifier> getIdOrTexture(ItemStack stack) {
        if (stack.getNbt() == null)
            return null;

        String id = stack.getNbt().getString(FrameData.ID);
        if (!id.isEmpty())
            return Either.left(id);

        String resource = stack.getNbt().getString(FrameData.TEXTURE);
        if (!resource.isEmpty())
            return Either.right(new Identifier(resource));

        return null;
    }

    public void setId(ItemStack stack, @NotNull String id) {
        Preconditions.checkState(!StringHelper.isEmpty(id), "'id' cannot be null or empty.");
        stack.getOrCreateNbt().putString(FrameData.ID, id);
    }

    public void setTexture(ItemStack stack, @NotNull Identifier resourceLocation) {
        stack.getOrCreateNbt().putString(FrameData.TEXTURE, resourceLocation.toString());
    }

    @Override
    public @NotNull Optional<TooltipData> getTooltipData(@NotNull ItemStack stack) {
        return getIdOrTexture(stack) != null ? Optional.of(new PhotographTooltip(stack)) : Optional.empty();
    }

    @Override
    public void appendTooltip(@NotNull ItemStack stack, @Nullable World level, @NotNull List<Text> tooltipComponents, @NotNull TooltipContext isAdvanced) {
        if (stack.getNbt() != null) {
            int generation = stack.getNbt().getInt("generation");
            if (generation > 0)
                tooltipComponents.add(Text.translatable("item.exposure.photograph.generation." + generation)
                        .formatted(Formatting.GRAY));

            String photographerName = stack.getNbt().getString(FrameData.PHOTOGRAPHER);
            if (!photographerName.isEmpty() && Config.Client.PHOTOGRAPH_SHOW_PHOTOGRAPHER_IN_TOOLTIP.get()) {
                tooltipComponents.add(Text.translatable("item.exposure.photograph.photographer_tooltip",
                                Text.literal(photographerName).formatted(Formatting.WHITE))
                        .formatted(Formatting.GRAY));
            }

            // The value is not constant here
            //noinspection ConstantValue
            if (generation < 2 && !PlatformHelper.isModLoaded("jei") && Config.Client.RECIPE_TOOLTIPS_WITHOUT_JEI.get()) {
                ClientGUI.addPhotographCopyingTooltip(stack, level, tooltipComponents, isAdvanced);
            }

            if (isAdvanced.isAdvanced()) {
                @Nullable Either<String, Identifier> idOrTexture = getIdOrTexture(stack);
                if (idOrTexture != null) {
                    String text = idOrTexture.map(id -> "Exposure Id: " + id, texture -> "Texture: " + texture);
                    tooltipComponents.add(Text.literal(text).formatted(Formatting.DARK_GRAY));
                }
            }
        }
    }

    @Override
    public @NotNull ActionResult useOnBlock(ItemUsageContext context) {
        BlockPos clickedPos = context.getBlockPos();
        Direction direction = context.getSide();
        BlockPos resultPos = clickedPos.offset(direction);
        PlayerEntity player = context.getPlayer();
        ItemStack itemStack = context.getStack();
        if (player == null || player.getWorld().isOutOfHeightLimit(resultPos) || !player.canPlaceOn(resultPos, direction, itemStack))
            return ActionResult.FAIL;

        World level = context.getWorld();
        PhotographEntity photographEntity = new PhotographEntity(level, resultPos, direction, itemStack.copy());

        if (photographEntity.canStayAttached()) {
            if (!level.isClient) {
                photographEntity.onPlace();
                level.emitGameEvent(player, GameEvent.ENTITY_PLACE, photographEntity.getPos());
                level.spawnEntity(photographEntity);
            }

            itemStack.decrement(1);
            return ActionResult.success(level.isClient);
        }

        return ActionResult.FAIL;
    }

    @Override
    public @NotNull TypedActionResult<ItemStack> use(@NotNull World level, PlayerEntity player, @NotNull Hand hand) {
        ItemStack itemInHand = player.getStackInHand(hand);

        if (getIdOrTexture(itemInHand) == null)
            LogUtils.getLogger().warn("No Id or Texture is defined. - " + itemInHand);

        if (level.isClient) {
            ClientGUI.openPhotographScreen(List.of(new ItemAndStack<>(itemInHand)));
            player.playSound(Exposure.SoundEvents.PHOTOGRAPH_RUSTLE.get(), 0.6f, 1.1f);
        }

        return TypedActionResult.success(itemInHand);
    }

    @Override
    public boolean onClicked(@NotNull ItemStack stack, @NotNull ItemStack other, @NotNull Slot slot, @NotNull ClickType action, @NotNull PlayerEntity player, @NotNull StackReference access) {
        if (action != ClickType.RIGHT)
            return false;

        if (other.getItem() instanceof PhotographItem) {
            StackedPhotographsItem stackedPhotographsItem = Exposure.Items.STACKED_PHOTOGRAPHS.get();
            ItemStack stackedPhotographsStack = new ItemStack(stackedPhotographsItem);

            stackedPhotographsItem.addPhotographOnTop(stackedPhotographsStack, stack);
            stackedPhotographsItem.addPhotographOnTop(stackedPhotographsStack, other);
            slot.setStackNoCallbacks(ItemStack.EMPTY);
            access.set(stackedPhotographsStack);

            StackedPhotographsItem.playAddSoundClientside(player);

            return true;
        }

        return false;
    }
}
