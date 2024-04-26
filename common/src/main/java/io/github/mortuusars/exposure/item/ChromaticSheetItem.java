package io.github.mortuusars.exposure.item;

import com.google.common.base.Preconditions;
import com.mojang.logging.LogUtils;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureServer;
import io.github.mortuusars.exposure.camera.capture.processing.FloydDither;
import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import io.github.mortuusars.exposure.camera.infrastructure.FrameData;
import io.github.mortuusars.exposure.data.storage.ExposureSavedData;
import io.github.mortuusars.exposure.network.Packets;
import io.github.mortuusars.exposure.network.packet.client.WaitForExposureChangeS2CP;
import io.github.mortuusars.exposure.util.ColorChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.block.MapColor;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class ChromaticSheetItem extends Item {
    public static final String EXPOSURES_TAG = "Exposures";
    public ChromaticSheetItem(Settings properties) {
        super(properties);
    }

    public List<NbtCompound> getExposures(ItemStack stack) {
        if (stack.getNbt() == null || !stack.getNbt().contains(EXPOSURES_TAG, NbtElement.LIST_TYPE))
            return Collections.emptyList();

        NbtList channelsList = stack.getNbt().getList(EXPOSURES_TAG, NbtElement.COMPOUND_TYPE);
        return channelsList.stream().map(t -> (NbtCompound)t).collect(Collectors.toList());
    }

    public void addExposure(ItemStack stack, NbtCompound frame) {
        NbtList channelsList = getOrCreateExposuresTag(stack);
        channelsList.add(frame);
        stack.getOrCreateNbt().put(EXPOSURES_TAG, channelsList);
    }

    private NbtList getOrCreateExposuresTag(ItemStack stack) {
        NbtCompound tag = stack.getOrCreateNbt();
        NbtList list = tag.getList(EXPOSURES_TAG, NbtElement.COMPOUND_TYPE);
        tag.put(EXPOSURES_TAG, list);
        return list;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World level, List<Text> tooltipComponents, TooltipContext isAdvanced) {
        List<NbtCompound> exposures = getExposures(stack);

        if (!exposures.isEmpty()) {
            MutableText component = Text.translatable("gui.exposure.channel.red")
                    .fillStyle(Style.EMPTY.withColor(ColorChannel.RED.getRepresentationColor()));

            if (exposures.size() >= 2){
                component.append(Text.translatable("gui.exposure.channel.separator").formatted(Formatting.GRAY));
                component.append(Text.translatable("gui.exposure.channel.green")
                        .fillStyle(Style.EMPTY.withColor(ColorChannel.GREEN.getRepresentationColor())));
            }

            if (exposures.size() >= 3) {
                component.append(Text.translatable("gui.exposure.channel.separator").formatted(Formatting.GRAY));
                component.append(Text.translatable("gui.exposure.channel.blue")
                        .fillStyle(Style.EMPTY.withColor(ColorChannel.BLUE.getRepresentationColor())));
            }

            tooltipComponents.add(component);

            if (exposures.size() >= 3) {
                component.append(Text.translatable("item.exposure.chromatic_sheet.use_tooltip").formatted(Formatting.GRAY));
            }
        }

    }

    @Override
    public @NotNull TypedActionResult<ItemStack> use(World level, PlayerEntity player, Hand usedHand) {
        ItemStack stack = player.getStackInHand(usedHand);

        if (!level.isClient && getExposures(stack).size() >= 3) {
            ItemStack result = finalize(level, stack);
            player.setStackInHand(usedHand, result);
            return TypedActionResult.success(result);
        }

        return super.use(level, player, usedHand);
    }

    public ItemStack finalize(@NotNull World level, ItemStack stack) {
        Preconditions.checkState(!level.isClient, "Can only finalize server-side.");

        List<NbtCompound> exposures = getExposures(stack);

        Preconditions.checkState(exposures.size() >= 3, 
                "Finalizing Chromatic Fragment requires 3 exposures. " + stack);

        NbtCompound redTag = exposures.get(0);
        String redId = redTag.getString(FrameData.ID);
        Optional<ExposureSavedData> redOpt = ExposureServer.getExposureStorage().getOrQuery(redId);
        if (redOpt.isEmpty()) {
            LogUtils.getLogger().error("Cannot create Chromatic Photograph: Red channel exposure '" + redId + "' is not found.");
            return stack;
        }

        NbtCompound greenTag = exposures.get(1);
        String greenId = greenTag.getString(FrameData.ID);
        Optional<ExposureSavedData> greenOpt = ExposureServer.getExposureStorage().getOrQuery(greenId);
        if (greenOpt.isEmpty()) {
            LogUtils.getLogger().error("Cannot create Chromatic Photograph: Green channel exposure '" + greenId + "' is not found.");
            return stack;
        }

        NbtCompound blueTag = exposures.get(2);
        String blueId = blueTag.getString(FrameData.ID);
        Optional<ExposureSavedData> blueOpt = ExposureServer.getExposureStorage().getOrQuery(blueId);
        if (blueOpt.isEmpty()) {
            LogUtils.getLogger().error("Cannot create Chromatic Photograph: Blue channel exposure '" + blueId + "' is not found.");
            return stack;
        }

        String name;
        int underscoreIndex = redId.lastIndexOf("_");
        if (underscoreIndex != -1)
            name = redId.substring(0, underscoreIndex);
        else
            name = Integer.toString(redId.hashCode());

        String id = String.format("%s_chromatic_%s", name, level.getTime());
        
        ItemStack photograph = new ItemStack(Exposure.Items.PHOTOGRAPH.get());

        // It would probably be better to make a tag that contains properties common to all 3 tags,
        // but it's tricky to implement, and it wouldn't be noticed most of the time.
        NbtCompound tag = redTag.copy();
        tag = tag.copyFrom(greenTag);
        tag = tag.copyFrom(blueTag);

        tag.remove(FrameData.CHROMATIC_CHANNEL);

        tag.putString(FrameData.ID, id);
        tag.putBoolean(FrameData.CHROMATIC, true);
        tag.putString(FrameData.RED_CHANNEL, redId);
        tag.putString(FrameData.GREEN_CHANNEL, greenId);
        tag.putString(FrameData.BLUE_CHANNEL, blueId);
        tag.putString(FrameData.TYPE, FilmType.COLOR.asString());

        photograph.setNbt(tag);

        Packets.sendToAllClients(new WaitForExposureChangeS2CP(id));

        new Thread(() -> {
            try {
                processAndSaveTrichrome(redOpt.get(), greenOpt.get(), blueOpt.get(), id);
            } catch (Exception e) {
                LogUtils.getLogger().error("Cannot process and save Chromatic Photograph: " + e);
            }
        }).start();

        return photograph;
    }

    protected void processAndSaveTrichrome(ExposureSavedData red, ExposureSavedData green, ExposureSavedData blue, String id) {
        int width = Math.min(red.getWidth(), Math.min(green.getWidth(), blue.getWidth()));
        int height = Math.min(red.getHeight(), Math.min(green.getHeight(), blue.getHeight()));
        if (width <= 0 ||height <= 0) {
            LogUtils.getLogger().error("Cannot create Chromatic Photograph: Width and Height should be larger than 0. " +
                    "Width '{}', Height: '{}'.", width, height);
            return;
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int r = MapColor.getRenderColor(red.getPixel(x, y)) >> 16 & 0xFF;
                int g = MapColor.getRenderColor(green.getPixel(x, y)) >> 8 & 0xFF;
                int b = MapColor.getRenderColor(blue.getPixel(x, y)) & 0xFF;

                int rgb = 0xFF << 24 | r << 16 | g << 8 | b;

                image.setRGB(x, y, rgb);
            }
        }

        byte[] mapColorPixels = FloydDither.ditherWithMapColors(image);

        NbtCompound properties = new NbtCompound();
        properties.putString(ExposureSavedData.TYPE_PROPERTY, FilmType.COLOR.asString());
        long unixTime = System.currentTimeMillis() / 1000L;
        properties.putLong(ExposureSavedData.TIMESTAMP_PROPERTY, unixTime);

        ExposureSavedData resultData = new ExposureSavedData(image.getWidth(), image.getHeight(), mapColorPixels, properties);
        ExposureServer.getExposureStorage().put(id, resultData);

        // Because we save exposure off-thread, and item was already created before chromatic processing has even begun -
        // we need to update clients, otherwise, client wouldn't know that and will think that the exposure is missing.
        ExposureServer.getExposureStorage().sendExposureChanged(id);
    }
}
