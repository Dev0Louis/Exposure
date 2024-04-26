package io.github.mortuusars.exposure.item;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.PlatformHelper;
import io.github.mortuusars.exposure.block.FlashBlock;
import io.github.mortuusars.exposure.camera.capture.Capture;
import io.github.mortuusars.exposure.camera.capture.CaptureManager;
import io.github.mortuusars.exposure.camera.capture.component.*;
import io.github.mortuusars.exposure.camera.capture.converter.DitheringColorConverter;
import io.github.mortuusars.exposure.camera.infrastructure.*;
import io.github.mortuusars.exposure.camera.viewfinder.ViewfinderClient;
import io.github.mortuusars.exposure.menu.CameraAttachmentsMenu;
import io.github.mortuusars.exposure.network.Packets;
import io.github.mortuusars.exposure.network.packet.client.StartExposureS2CP;
import io.github.mortuusars.exposure.network.packet.server.CameraInHandAddFrameC2SP;
import io.github.mortuusars.exposure.sound.OnePerPlayerSounds;
import io.github.mortuusars.exposure.util.CameraInHand;
import io.github.mortuusars.exposure.util.ColorChannel;
import io.github.mortuusars.exposure.util.ItemAndStack;
import io.github.mortuusars.exposure.util.LevelUtil;
import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class CameraItem extends Item {
    public record AttachmentType(String id, int slot, Predicate<ItemStack> stackValidator) {
        @Override
        public String toString() {
            return "AttachmentType{" +
                    "id='" + id + '\'' +
                    ", slot=" + slot +
                    '}';
        }
    }

    public static final AttachmentType FILM_ATTACHMENT = new AttachmentType("Film", 0, stack -> stack.getItem() instanceof FilmRollItem);
    public static final AttachmentType FLASH_ATTACHMENT = new AttachmentType("Flash", 1, stack -> stack.isIn(Exposure.Tags.Items.FLASHES));
    public static final AttachmentType LENS_ATTACHMENT = new AttachmentType("Lens", 2, stack -> stack.isIn(Exposure.Tags.Items.LENSES));
    public static final AttachmentType FILTER_ATTACHMENT = new AttachmentType("Filter", 3, stack -> stack.isIn(Exposure.Tags.Items.FILTERS));
    public static final List<AttachmentType> ATTACHMENTS = List.of(
            FILM_ATTACHMENT,
            FLASH_ATTACHMENT,
            LENS_ATTACHMENT,
            FILTER_ATTACHMENT);

    public static final List<ShutterSpeed> SHUTTER_SPEEDS = List.of(
            new ShutterSpeed("15\""),
            new ShutterSpeed("8\""),
            new ShutterSpeed("4\""),
            new ShutterSpeed("2\""),
            new ShutterSpeed("1\""),
            new ShutterSpeed("2"),
            new ShutterSpeed("4"),
            new ShutterSpeed("8"),
            new ShutterSpeed("15"),
            new ShutterSpeed("30"),
            new ShutterSpeed("60"),
            new ShutterSpeed("125"),
            new ShutterSpeed("250"),
            new ShutterSpeed("500")
    );

    public CameraItem(Settings properties) {
        super(properties);
    }

    @Override
    public int getMaxUseTime(@NotNull ItemStack stack) {
        return 1000;
    }

    @Override
    public void appendTooltip(@NotNull ItemStack stack, @Nullable World level, @NotNull List<Text> components, @NotNull TooltipContext isAdvanced) {
        if (Config.Client.CAMERA_SHOW_OPEN_WITH_SNEAK_IN_TOOLTIP.get()) {
            components.add(Text.translatable("item.exposure.camera.sneak_to_open_tooltip").formatted(Formatting.GRAY));
        }
    }

    public boolean isActive(ItemStack stack) {
        return stack.getNbt() != null && stack.getNbt().getBoolean("Active");
    }

    public void setActive(ItemStack stack, boolean active) {
        stack.getOrCreateNbt().putBoolean("Active", active);
        setSelfieMode(stack, false);
    }

    public void activate(PlayerEntity player, ItemStack stack) {
        if (!isActive(stack)) {
            setActive(stack, true);
            player.emitGameEvent(GameEvent.EQUIP); // Sends skulk vibrations
            playCameraSound(player, Exposure.SoundEvents.VIEWFINDER_OPEN.get(), 0.35f, 0.9f, 0.2f);
        }
    }

    public void deactivate(PlayerEntity player, ItemStack stack) {
        if (isActive(stack)) {
            setActive(stack, false);
            player.emitGameEvent(GameEvent.EQUIP);
            playCameraSound(player, Exposure.SoundEvents.VIEWFINDER_CLOSE.get(), 0.35f, 0.9f, 0.2f);
        }
    }

    public boolean isInSelfieMode(ItemStack stack) {
        return stack.getNbt() != null && stack.getNbt().getBoolean("Selfie");
    }

    public void setSelfieMode(ItemStack stack, boolean selfie) {
        stack.getOrCreateNbt().putBoolean("Selfie", selfie);
    }

    public void setSelfieModeWithEffects(PlayerEntity player, ItemStack stack, boolean selfie) {
        setSelfieMode(stack, selfie);
        player.getWorld().playSoundFromEntity(player, player, Exposure.SoundEvents.CAMERA_LENS_RING_CLICK.get(),  SoundCategory.PLAYERS, 1f, 1.5f);
    }

    public boolean isShutterOpen(ItemStack stack) {
        return stack.getNbt() != null && stack.getNbt().getBoolean("ShutterOpen");
    }

    public void setShutterOpen(World level, ItemStack stack, ShutterSpeed shutterSpeed, boolean exposingFrame, boolean flashHasFired) {
        NbtCompound tag = stack.getOrCreateNbt();
        tag.putBoolean("ShutterOpen", true);
        tag.putInt("ShutterTicks", Math.max(shutterSpeed.getTicks(), 1));
        tag.putLong("ShutterCloseTimestamp", level.getTime() + Math.max(shutterSpeed.getTicks(), 1));
        if (exposingFrame)
            tag.putBoolean("ExposingFrame", true);
        if (flashHasFired)
            tag.putBoolean("FlashHasFired", true);
    }

    public void setShutterClosed(ItemStack stack) {
        NbtCompound tag = stack.getNbt();
        if (tag != null) {
            tag.remove("ShutterOpen");
            tag.remove("ShutterTicks");
            tag.remove("ShutterCloseTimestamp");
            tag.remove("ExposingFrame");
            tag.remove("FlashHasFired");
        }
    }

    public void openShutter(PlayerEntity player, ItemStack stack, ShutterSpeed shutterSpeed, boolean exposingFrame, boolean flashHasFired) {
        setShutterOpen(player.getWorld(), stack, shutterSpeed, exposingFrame, flashHasFired);

        player.emitGameEvent(GameEvent.ITEM_INTERACT_FINISH);
        playCameraSound(player, Exposure.SoundEvents.SHUTTER_OPEN.get(), exposingFrame ? 0.7f : 0.5f,
                exposingFrame ? 1.1f : 1.25f, 0.2f);
        if (shutterSpeed.getMilliseconds() > 500) // More than 1/2
            OnePerPlayerSounds.play(player, Exposure.SoundEvents.SHUTTER_TICKING.get(), SoundCategory.PLAYERS, 1f, 1f);
    }

    public void closeShutter(PlayerEntity player, ItemStack stack) {
        long closedAtTimestamp = stack.getNbt() != null ? stack.getNbt().getLong("ShutterCloseTimestamp") : -1;
        boolean exposingFrame = stack.getNbt() != null && stack.getNbt().getBoolean("ExposingFrame");
        boolean flashHasFired = stack.getNbt() != null && stack.getNbt().getBoolean("FlashHasFired");

        setShutterClosed(stack);

        if (player.getWorld().getTime() - closedAtTimestamp < 50) { // Skip effects if shutter "was closed" long ago
            player.emitGameEvent(GameEvent.ITEM_INTERACT_FINISH);
            player.getItemCooldownManager().set(this, flashHasFired ? 10 : 2);
            playCameraSound(player, Exposure.SoundEvents.SHUTTER_CLOSE.get(), 0.7f, 1.1f, 0.2f);
            if (exposingFrame) {
                ItemAndStack<FilmRollItem> film = getFilm(stack).orElseThrow();

                float fullness = (float) film.getItem().getExposedFramesCount(film.getStack()) / film.getItem().getMaxFrameCount(film.getStack());
                boolean lastFrame = fullness == 1f;

                if (lastFrame)
                    OnePerPlayerSounds.play(player, Exposure.SoundEvents.FILM_ADVANCE_LAST.get(), SoundCategory.PLAYERS, 1f, 1f);
                else {
                    OnePerPlayerSounds.play(player, Exposure.SoundEvents.FILM_ADVANCE.get(), SoundCategory.PLAYERS,
                            1f, 0.9f + 0.1f * fullness);
                }
            }
        }
    }

    @SuppressWarnings("unused")
    public void playCameraSound(PlayerEntity player, SoundEvent sound, float volume, float pitch) {
        playCameraSound(player, sound, volume, pitch, 0f);
    }

    public void playCameraSound(PlayerEntity player, SoundEvent sound, float volume, float pitch, float pitchVariety) {
        if (pitchVariety > 0f)
            pitch = pitch - (pitchVariety / 2f) + (player.getRandom().nextFloat() * pitchVariety);
        player.getWorld().playSoundFromEntity(player, player, sound, SoundCategory.PLAYERS, volume, pitch);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull World level, @NotNull Entity entity, int slotId, boolean isSelected) {
        if (!(entity instanceof PlayerEntity player))
            return;

        if (isShutterOpen(stack)) {
            if (stack.getNbt() != null && stack.getNbt().contains("ShutterTicks")) {
                int ticks = stack.getNbt().getInt("ShutterTicks");
                if (ticks <= 0)
                    closeShutter(player, stack);
                else {
                    ticks--;
                    stack.getNbt().putInt("ShutterTicks", ticks);
                }
            }
            else {
                closeShutter(player, stack);
            }
        }

        boolean inOffhand = player.getOffHandStack().equals(stack);
        boolean inHand = isSelected || inOffhand;

        if (!inHand) {
            deactivate(player, stack);
        }
    }

    @Override
    public @NotNull ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (player != null) {
            Hand hand = context.getHand();
            if (hand == Hand.MAIN_HAND && CameraInHand.getActiveHand(player) == Hand.OFF_HAND)
                return ActionResult.PASS;

            return useCamera(player, hand);
        }
        return ActionResult.CONSUME; // To not play attack animation.
    }

    @Override
    public @NotNull TypedActionResult<ItemStack> use(@NotNull World level, @NotNull PlayerEntity player, @NotNull Hand hand) {
        if (hand == Hand.MAIN_HAND && CameraInHand.getActiveHand(player) == Hand.OFF_HAND)
            return TypedActionResult.pass(player.getStackInHand(hand));

        useCamera(player, hand);
        return TypedActionResult.consume(player.getStackInHand(hand));
    }

    public ActionResult useCamera(PlayerEntity player, Hand hand) {
        if (player.getItemCooldownManager().isCoolingDown(this))
            return ActionResult.FAIL;

        ItemStack stack = player.getStackInHand(hand);
        if (stack.isEmpty() || stack.getItem() != this)
            return ActionResult.PASS;

        boolean active = isActive(stack);

        if (!active && player.shouldCancelInteraction()) {
            if (isShutterOpen(stack)) {
                player.sendMessage(Text.translatable("item.exposure.camera.camera_attachments.fail.shutter_open")
                        .formatted(Formatting.RED), true);
                return ActionResult.FAIL;
            }

            openCameraAttachmentsMenu(player, hand);
            return ActionResult.SUCCESS;
        }

        if (!active) {
            activate(player, stack);
            player.getItemCooldownManager().set(this, 4);

            if (player.getWorld().isClient) {
                // Release use key after activating. Otherwise, right click will be still held and camera will take a shot
                CameraItemClientExtensions.releaseUseButton();
            }

            return ActionResult.CONSUME; // Consume to not play animation
        }

        playCameraSound(player, Exposure.SoundEvents.CAMERA_RELEASE_BUTTON_CLICK.get(), 0.3f, 1f, 0.1f);

        Optional<ItemAndStack<FilmRollItem>> filmAttachment = getFilm(stack);

        if (filmAttachment.isEmpty())
            return ActionResult.FAIL;

        ItemAndStack<FilmRollItem> film = filmAttachment.get();
        boolean exposingFilm = film.getItem().canAddFrame(film.getStack());

        if (!exposingFilm)
            return ActionResult.FAIL;

        if (isShutterOpen(stack))
            return ActionResult.FAIL;

        int lightLevel = LevelUtil.getLightLevelAt(player.getWorld(), player.getBlockPos());

        boolean flashHasFired = shouldFlashFire(player, stack) && tryUseFlash(player, stack);

        ShutterSpeed shutterSpeed = getShutterSpeed(stack);

        openShutter(player, stack, shutterSpeed, true, flashHasFired);

        player.incrementStat(Exposure.Stats.FILM_FRAMES_EXPOSED);

        if (player instanceof ServerPlayerEntity serverPlayer) {
            Packets.sendToClient(new StartExposureS2CP(createExposureId(player), hand, flashHasFired, lightLevel), serverPlayer);
        }

        return ActionResult.CONSUME; // Consume to not play animation
    }

    public void exposeFrameClientside(PlayerEntity player, Hand hand, String exposureId, boolean flashHasFired, int lightLevel) {
        Preconditions.checkState(player.getWorld().isClient, "Should only be called on client.");

        ItemStack cameraStack = player.getStackInHand(hand);

        NbtCompound frame = createFrameTag(player, cameraStack, exposureId, flashHasFired, lightLevel);

        Capture capture = createCapture(player, cameraStack, exposureId, frame, flashHasFired);
        CaptureManager.enqueue(capture);

        addFrameToFilm(cameraStack, frame);

        Packets.sendToServer(new CameraInHandAddFrameC2SP(hand, frame));
    }

    public void addFrameToFilm(ItemStack cameraStack, NbtCompound frame) {
        ItemAndStack<FilmRollItem> film = getFilm(cameraStack)
                .orElseThrow(() -> new IllegalStateException("Camera should have film inserted."));

        film.getItem().addFrame(film.getStack(), frame);
        setFilm(cameraStack, film.getStack());
    }

    protected boolean shouldFlashFire(PlayerEntity player, ItemStack cameraStack) {
        if (getAttachment(cameraStack, FLASH_ATTACHMENT).isEmpty())
            return false;

        return switch (getFlashMode(cameraStack)) {
            case OFF -> false;
            case ON -> true;
            case AUTO -> LevelUtil.getLightLevelAt(player.getWorld(), player.getBlockPos()) < 8;
        };
    }

    @SuppressWarnings("unused")
    public boolean tryUseFlash(PlayerEntity player, ItemStack cameraStack) {
        World level = player.getWorld();
        BlockPos playerHeadPos = player.getBlockPos().up();
        @Nullable BlockPos flashPos = null;

        if (level.getBlockState(playerHeadPos).isAir() || level.getFluidState(playerHeadPos).isEqualAndStill(Fluids.WATER))
            flashPos = playerHeadPos;
        else {
            for (Direction direction : Direction.values()) {
                BlockPos pos = playerHeadPos.offset(direction);
                if (level.getBlockState(pos).isAir() || level.getFluidState(pos).isEqualAndStill(Fluids.WATER)) {
                    flashPos = pos;
                }
            }
        }

        if (flashPos == null)
            return false;

        level.setBlockState(flashPos, Exposure.Blocks.FLASH.get().getDefaultState()
                .with(FlashBlock.WATERLOGGED, level.getFluidState(flashPos)
                        .isEqualAndStill(Fluids.WATER)), Block.field_31022);
        level.playSoundFromEntity(player, player, Exposure.SoundEvents.FLASH.get(), SoundCategory.PLAYERS, 1f, 1f);

        player.emitGameEvent(GameEvent.PRIME_FUSE);
        player.incrementStat(Exposure.Stats.FLASHES_TRIGGERED);

        // Send particles to other players:
        if (level instanceof ServerWorld serverLevel && player instanceof ServerPlayerEntity serverPlayer) {
            Vec3d pos = player.getPos();
            pos = pos.add(0, 1, 0).add(player.getRotationVector().multiply(0.5, 0, 0.5));
            ParticleS2CPacket packet = new ParticleS2CPacket(ParticleTypes.FLASH, false,
                    pos.x, pos.y, pos.z, 0, 0, 0, 0, 0);
            for (ServerPlayerEntity pl : serverLevel.getPlayers()) {
                if (!pl.equals(serverPlayer)) {
                    pl.networkHandler.sendPacket(packet);
                    Random r = serverLevel.getRandom();
                    for (int i = 0; i < 4; i++) {
                        pl.networkHandler.sendPacket(new ParticleS2CPacket(ParticleTypes.END_ROD, false,
                                pos.x + r.nextFloat() * 0.5f - 0.25f, pos.y + r.nextFloat() * 0.5f + 0.2f, pos.z + r.nextFloat() * 0.5f - 0.25f,
                                0, 0, 0, 0, 0));
                    }
                }
            }
        }
        return true;
    }

    protected NbtCompound createFrameTag(PlayerEntity player, ItemStack cameraStack, String exposureId, boolean flash, int lightLevel) {
        World level = player.getWorld();

        NbtCompound tag = new NbtCompound();

        tag.putString(FrameData.ID, exposureId);
        tag.putString(FrameData.TIMESTAMP, Util.getFormattedCurrentTime());
        tag.putString(FrameData.PHOTOGRAPHER, player.getEntityName());
        tag.putUuid(FrameData.PHOTOGRAPHER_ID, player.getUuid());

        // Chromatic only for black and white:
        Boolean isBW = getAttachment(cameraStack, FILM_ATTACHMENT)
                .map(f -> f.getItem() instanceof IFilmItem filmItem && filmItem.getType() == FilmType.BLACK_AND_WHITE)
                .orElse(false);
        if (isBW) {
            getAttachment(cameraStack, FILTER_ATTACHMENT).flatMap(ColorChannel::fromStack).ifPresent(c -> {
                tag.putBoolean(FrameData.CHROMATIC, true);
                tag.putString(FrameData.CHROMATIC_CHANNEL, c.asString());
            });
        }

        if (flash)
            tag.putBoolean(FrameData.FLASH, true);
        if (isInSelfieMode(cameraStack))
            tag.putBoolean(FrameData.SELFIE, true);

        NbtList pos = new NbtList();
        pos.add(NbtInt.of(player.getBlockPos().getX()));
        pos.add(NbtInt.of(player.getBlockPos().getY()));
        pos.add(NbtInt.of(player.getBlockPos().getZ()));
        tag.put(FrameData.POSITION, pos);

        tag.putString(FrameData.DIMENSION, player.getWorld().getRegistryKey().getValue().toString());

        player.getWorld().getBiome(player.getBlockPos()).getKey().map(RegistryKey::getValue)
                .ifPresent(biome -> tag.putString(FrameData.BIOME, biome.toString()));

        int surfaceHeight = level.getTopY(Heightmap.Type.WORLD_SURFACE_WG, player.getBlockX(), player.getBlockZ());
        level.calculateAmbientDarkness();
        int skyLight = level.getLightLevel(LightType.SKY, player.getBlockPos());

        if (player.isSubmergedInWater())
            tag.putBoolean(FrameData.UNDERWATER, true);

        if (player.getBlockY() < surfaceHeight && skyLight < 4)
            tag.putBoolean(FrameData.IN_CAVE, true);
        else if (!player.isSubmergedInWater()){
            Biome.Precipitation precipitation = level.getBiome(player.getBlockPos()).value().getPrecipitation(player.getBlockPos());
            if (level.isThundering() && precipitation != Biome.Precipitation.NONE)
                tag.putString(FrameData.WEATHER, precipitation == Biome.Precipitation.SNOW ? "Snowstorm" : "Thunder");
            else if (level.isRaining() && precipitation != Biome.Precipitation.NONE)
                tag.putString(FrameData.WEATHER, precipitation == Biome.Precipitation.SNOW ? "Snow" : "Rain");
            else
                tag.putString(FrameData.WEATHER, "Clear");
        }

        tag.putInt(FrameData.LIGHT_LEVEL, lightLevel);
        tag.putFloat(FrameData.SUN_ANGLE, level.getSkyAngleRadians(0));

        List<Entity> entitiesInFrame = EntitiesInFrame.get(player, ViewfinderClient.getCurrentFov(), 12, isInSelfieMode(cameraStack));
        if (!entitiesInFrame.isEmpty()) {
            NbtList entities = new NbtList();

            for (Entity entity : entitiesInFrame) {
                NbtCompound entityInfoTag = createEntityInFrameInfo(entity, player, cameraStack);
                if (entityInfoTag.isEmpty())
                    continue;

                entities.add(entityInfoTag);

                // Duplicate entity id as a separate field in the tag.
                // Can then be used by FTBQuests nbt matching (it's hard to match from a list), for example.
                tag.putBoolean(entityInfoTag.getString(FrameData.ENTITY_ID), true);
            }

            if (!entities.isEmpty())
                tag.put(FrameData.ENTITIES_IN_FRAME, entities);
        }

        return tag;
    }

    protected NbtCompound createEntityInFrameInfo(Entity entity, PlayerEntity photographer, ItemStack cameraStack) {
        NbtCompound tag = new NbtCompound();
        Identifier entityRL = Registries.ENTITY_TYPE.getId(entity.getType());

        tag.putString(FrameData.ENTITY_ID, entityRL.toString());

        NbtList pos = new NbtList();
        pos.add(NbtInt.of((int) entity.getX()));
        pos.add(NbtInt.of((int) entity.getY()));
        pos.add(NbtInt.of((int) entity.getZ()));
        tag.put(FrameData.ENTITY_POSITION, pos);

        tag.putFloat(FrameData.ENTITY_DISTANCE, photographer.distanceTo(entity));

        if (entity instanceof PlayerEntity player)
            tag.putString(FrameData.ENTITY_PLAYER_NAME, player.getEntityName());

        return tag;
    }

    protected void openCameraAttachmentsMenu(PlayerEntity player, Hand hand) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            ItemStack cameraStack = player.getStackInHand(hand);

            NamedScreenHandlerFactory menuProvider = new NamedScreenHandlerFactory() {
                @Override
                public @NotNull Text getDisplayName() {
                    return cameraStack.getName();
                }

                @Override
                public @NotNull ScreenHandler createMenu(int containerId, @NotNull PlayerInventory playerInventory, @NotNull PlayerEntity player) {
                    return new CameraAttachmentsMenu(containerId, playerInventory, cameraStack);
                }
            };

            PlatformHelper.openMenu(serverPlayer, menuProvider, buffer -> buffer.writeItemStack(cameraStack));
        }
    }

    protected String createExposureId(PlayerEntity player) {
        // This method is called only server-side and then gets sent to client in a packet
        // because gameTime is different between client/server, and IDs won't match.
        return player.getName().getString() + "_" + player.getWorld().getTime();
    }

    public FocalRange getFocalRange(ItemStack cameraStack) {
        return getAttachment(cameraStack, LENS_ATTACHMENT).map(FocalRange::ofStack).orElse(getDefaultFocalRange());
    }

    public FocalRange getDefaultFocalRange() {
        return FocalRange.getDefault();
    }

    @SuppressWarnings("unused")
    protected Capture createCapture(PlayerEntity player, ItemStack cameraStack, String exposureId, NbtCompound frameData, boolean flash) {
        ItemAndStack<FilmRollItem> film = getFilm(cameraStack).orElseThrow();
        int frameSize = film.getItem().getFrameSize(film.getStack());
        float brightnessStops = getShutterSpeed(cameraStack).getStopsDifference(ShutterSpeed.DEFAULT);

        ArrayList<ICaptureComponent> components = new ArrayList<>();
        components.add(new BaseComponent());
        if (flash)
            components.add(new FlashComponent());
        if (brightnessStops != 0)
            components.add(new BrightnessComponent(brightnessStops));
        if (film.getItem().getType() == FilmType.BLACK_AND_WHITE) {
            Optional<ItemStack> filter = getAttachment(cameraStack, FILTER_ATTACHMENT);
            filter.flatMap(ColorChannel::fromStack).ifPresentOrElse(
                    channel -> components.add(new SelectiveChannelBlackAndWhiteComponent(channel)),
                    () -> components.add(new BlackAndWhiteComponent()));
        }

        components.add(new ExposureStorageSaveComponent(exposureId, true));

        return new Capture(exposureId, frameData)
                .setFilmType(film.getItem().getType())
                .size(frameSize)
                .brightnessStops(brightnessStops)
                .components(components)
                .converter(new DitheringColorConverter());
    }

    /**
     * This method is called after we take a screenshot (or immediately if not capturing but should show effects). Otherwise, due to the delays (flash, etc) - particles would be captured as well.
     */
    @SuppressWarnings("unused")
    public void spawnClientsideFlashEffects(@NotNull PlayerEntity player, ItemStack cameraStack) {
        Preconditions.checkState(player.getWorld().isClient, "This methods should only be called client-side.");
        World level = player.getWorld();
        Vec3d pos = player.getPos();
        Vec3d lookAngle = player.getRotationVector();
        pos = pos.add(0, 1, 0).add(lookAngle.multiply(0.8f, 0.8f, 0.8f));

        Random r = level.getRandom();
        for (int i = 0; i < 3; i++) {
            level.addParticle(ParticleTypes.END_ROD,
                    pos.x + r.nextFloat() - 0.5f,
                    pos.y + r.nextFloat() + 0.15f,
                    pos.z + r.nextFloat() - 0.5f,
                    lookAngle.x * 0.025f + r.nextFloat() * 0.025f,
                    lookAngle.y * 0.025f + r.nextFloat() * 0.025f,
                    lookAngle.z * 0.025f + r.nextFloat() * 0.025f);
        }
    }

    // ---

    @SuppressWarnings("unused")
    public List<AttachmentType> getAttachmentTypes(ItemStack cameraStack) {
        return ATTACHMENTS;
    }

    public Optional<AttachmentType> getAttachmentTypeForSlot(ItemStack cameraStack, int slot) {
        List<AttachmentType> attachmentTypes = getAttachmentTypes(cameraStack);
        for (AttachmentType attachmentType : attachmentTypes) {
            if (attachmentType.slot == slot)
                return Optional.of(attachmentType);
        }
        return Optional.empty();
    }

    public Optional<ItemAndStack<FilmRollItem>> getFilm(ItemStack cameraStack) {
        return getAttachment(cameraStack, FILM_ATTACHMENT).map(ItemAndStack::new);
    }

    public void setFilm(ItemStack cameraStack, ItemStack filmStack) {
        setAttachment(cameraStack, FILM_ATTACHMENT, filmStack);
    }

    public Optional<ItemStack> getAttachment(ItemStack cameraStack, AttachmentType attachmentType) {
        if (cameraStack.getNbt() != null && cameraStack.getNbt().contains(attachmentType.id, NbtElement.COMPOUND_TYPE)) {
            ItemStack itemStack = ItemStack.fromNbt(cameraStack.getNbt().getCompound(attachmentType.id));
            if (!itemStack.isEmpty())
                return Optional.of(itemStack);
        }
        return Optional.empty();
    }

    public void setAttachment(ItemStack cameraStack, AttachmentType attachmentType, ItemStack attachmentStack) {
        if (attachmentStack.isEmpty()) {
            if (cameraStack.getNbt() != null)
                cameraStack.getOrCreateNbt().remove(attachmentType.id);
        } else {
            Preconditions.checkState(attachmentType.stackValidator.test(attachmentStack),
                    attachmentStack + " is not valid for the '" + attachmentType + "' attachment type.");

            cameraStack.getOrCreateNbt().put(attachmentType.id, attachmentStack.writeNbt(new NbtCompound()));
        }

        if (attachmentType == LENS_ATTACHMENT)
            setZoom(cameraStack, getFocalRange(cameraStack).min());
    }

    // ---

    /**
     * Returns all possible Shutter Speeds for this camera.
     */
    @SuppressWarnings("unused")
    public List<ShutterSpeed> getAllShutterSpeeds(ItemStack cameraStack) {
        return SHUTTER_SPEEDS;
    }

    public ShutterSpeed getShutterSpeed(ItemStack cameraStack) {
        return ShutterSpeed.loadOrDefault(cameraStack.getOrCreateNbt());
    }

    public void setShutterSpeed(ItemStack cameraStack, ShutterSpeed shutterSpeed) {
        shutterSpeed.save(cameraStack.getOrCreateNbt());
    }

    public float getFocalLength(ItemStack cameraStack) {
        return cameraStack.hasNbt() ? cameraStack.getOrCreateNbt().getFloat("Zoom") : getFocalRange(cameraStack).min();
    }

    public void setZoom(ItemStack cameraStack, double focalLength) {
        cameraStack.getOrCreateNbt().putDouble("Zoom", focalLength);
    }

    public CompositionGuide getCompositionGuide(ItemStack cameraStack) {
        if (!cameraStack.hasNbt() || !cameraStack.getOrCreateNbt().contains("CompositionGuide", NbtElement.STRING_TYPE))
            return CompositionGuides.NONE;

        return CompositionGuides.byIdOrNone(cameraStack.getOrCreateNbt().getString("CompositionGuide"));
    }

    public void setCompositionGuide(ItemStack cameraStack, CompositionGuide guide) {
        cameraStack.getOrCreateNbt().putString("CompositionGuide", guide.getId());
    }

    public FlashMode getFlashMode(ItemStack cameraStack) {
        if (!cameraStack.hasNbt() || !cameraStack.getOrCreateNbt().contains("FlashMode", NbtElement.STRING_TYPE))
            return FlashMode.OFF;

        return FlashMode.byIdOrOff(cameraStack.getOrCreateNbt().getString("FlashMode"));
    }

    public void setFlashMode(ItemStack cameraStack, FlashMode flashMode) {
        cameraStack.getOrCreateNbt().putString("FlashMode", flashMode.getId());
    }
}
