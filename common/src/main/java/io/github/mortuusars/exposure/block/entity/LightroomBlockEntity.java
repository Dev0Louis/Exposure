package io.github.mortuusars.exposure.block.entity;

import com.google.common.base.Preconditions;
import com.mojang.logging.LogUtils;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureServer;
import io.github.mortuusars.exposure.block.LightroomBlock;
import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import io.github.mortuusars.exposure.camera.infrastructure.FrameData;
import io.github.mortuusars.exposure.data.storage.ExposureSavedData;
import io.github.mortuusars.exposure.item.*;
import io.github.mortuusars.exposure.menu.LightroomMenu;
import io.github.mortuusars.exposure.util.ItemAndStack;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

@SuppressWarnings("unused")
public class LightroomBlockEntity extends LockableContainerBlockEntity implements SidedInventory {
    public static final int CONTAINER_DATA_SIZE = 3;
    public static final int CONTAINER_DATA_PROGRESS_ID = 0;
    public static final int CONTAINER_DATA_PRINT_TIME_ID = 1;
    public static final int CONTAINER_DATA_SELECTED_FRAME_ID = 2;

    protected final PropertyDelegate containerData = new PropertyDelegate() {
        public int get(int id) {
            return switch (id) {
                case CONTAINER_DATA_PROGRESS_ID -> LightroomBlockEntity.this.progress;
                case CONTAINER_DATA_PRINT_TIME_ID -> LightroomBlockEntity.this.printTime;
                case CONTAINER_DATA_SELECTED_FRAME_ID -> LightroomBlockEntity.this.getSelectedFrameIndex();
                default -> 0;
            };
        }

        public void set(int id, int value) {
            if (id == CONTAINER_DATA_PROGRESS_ID)
                LightroomBlockEntity.this.progress = value;
            else if (id == CONTAINER_DATA_PRINT_TIME_ID)
                LightroomBlockEntity.this.printTime = value;
            else if (id == CONTAINER_DATA_SELECTED_FRAME_ID)
                LightroomBlockEntity.this.setSelectedFrame(value);
            markDirty();
        }

        public int size() {
            return CONTAINER_DATA_SIZE;
        }
    };

    protected DefaultedList<ItemStack> items = DefaultedList.ofSize(Lightroom.SLOTS, ItemStack.EMPTY);

    protected int selectedFrame;
    protected int progress;
    protected int printTime;
    protected int storedExperience;
    protected boolean advanceFrame;
    protected Lightroom.Process process = Lightroom.Process.REGULAR;

    public LightroomBlockEntity(BlockPos pos, BlockState blockState) {
        super(Exposure.BlockEntityTypes.LIGHTROOM.get(), pos, blockState);
    }

    public static <T extends BlockEntity> void serverTick(World level, BlockPos blockPos, BlockState blockState, T blockEntity) {
        if (blockEntity instanceof LightroomBlockEntity lightroomBlockEntity)
            lightroomBlockEntity.tick();
    }

    protected void tick() {
        if (printTime <= 0 || !canPrint()) {
            stopPrintingProcess();
            return;
        }

        if (progress < printTime) {
            progress++;
            if (progress % 55 == 0 && printTime - progress > 12 && world != null)
                world.playSound(null, getPos(), Exposure.SoundEvents.LIGHTROOM_PRINT.get(), SoundCategory.BLOCKS,
                        1f, world.getRandom().nextFloat() * 0.3f + 1f);
            return;
        }

        if (tryPrint()) {
            onFramePrinted();
        }

        stopPrintingProcess();
    }

    protected void onFramePrinted() {
        if (advanceFrame)
            advanceFrame();
    }

    protected void advanceFrame() {
        ItemAndStack<DevelopedFilmItem> film = new ItemAndStack<>(getStack(Lightroom.FILM_SLOT));
        int frames = film.getItem().getExposedFramesCount(film.getStack());

        if (getSelectedFrameIndex() >= frames - 1) { // On last frame
            if (canEjectFilm())
                ejectFilm();
        } else {
            setSelectedFrame(getSelectedFrameIndex() + 1);
            markDirty();
        }
    }

    public boolean isAdvancingFrameOnPrint() {
        return advanceFrame;
    }

    protected boolean canEjectFilm() {
        if (world == null || world.isClient || getStack(Lightroom.FILM_SLOT).isEmpty())
            return false;

        BlockPos pos = getPos();
        Direction facing = world.getBlockState(pos).get(LightroomBlock.FACING);

        return !world.getBlockState(pos.offset(facing)).isOpaque();
    }

    protected void ejectFilm() {
        if (world == null || world.isClient || getStack(Lightroom.FILM_SLOT).isEmpty())
            return;

        BlockPos pos = getPos();
        Direction facing = world.getBlockState(pos).get(LightroomBlock.FACING);
        ItemStack filmStack = removeStack(Lightroom.FILM_SLOT, 1);

        Vec3i normal = facing.getVector();
        Vec3d point = Vec3d.ofCenter(pos).add(normal.getX() * 0.75f, normal.getY() * 0.75f, normal.getZ() * 0.75f);
        ItemEntity itemEntity = new ItemEntity(world, point.x, point.y, point.z, filmStack);
        itemEntity.setVelocity(normal.getX() * 0.05f, normal.getY() * 0.05f + 0.15f, normal.getZ() * 0.05f);
        itemEntity.setToDefaultPickupDelay();
        world.spawnEntity(itemEntity);

        inventoryContentsChanged(Lightroom.FILM_SLOT);
    }

    public int getSelectedFrameIndex() {
        return selectedFrame;
    }

    public void setSelectedFrame(int index) {
        if (selectedFrame != index) {
            selectedFrame = index;
            stopPrintingProcess();
        }
    }

    /*
        Process setting stays where it was set by the player, But if the frame does not support it -
        image would be printed with supported process (probably regular), WITHOUT changing the process setting -
        this means that this process will be used again if some other image is supporting it.

        This was done to not reset Process in automated setups, if there were some image on a film that doesn't support selected process.
     */

    /**
     * @return Process SETTING. Can be different from actual process that would be used to print an image
     * (if frame does not support this process, for example).
     */
    public Lightroom.Process getProcess() {
        return process;
    }

    /**
     * @return Process, that will be used to print an image.
     */
    public Lightroom.Process getActualProcess(ItemStack filmStack) {
        ItemStack film = getStack(Lightroom.FILM_SLOT);

        if (!isSelectedFrameChromatic(film, getSelectedFrameIndex()))
            return Lightroom.Process.REGULAR;

        return process;
    }

    public void setProcess(Lightroom.Process process) {
        this.process = process;
        stopPrintingProcess();
        markDirty();
    }

    public Optional<NbtCompound> getSelectedFrame(ItemStack film) {
        if (!film.isEmpty() && film.getItem() instanceof DevelopedFilmItem developedFilm) {
            NbtList frames = developedFilm.getExposedFrames(film);
            if (frames.size() > getSelectedFrameIndex())
                return Optional.of(frames.getCompound(selectedFrame));
        }

        return Optional.empty();
    }

    public boolean isSelectedFrameChromatic(ItemStack film, int selectedFrame) {
        return getSelectedFrame(film)
                .map(frame -> frame.getBoolean(FrameData.CHROMATIC))
                .orElse(false);
    }

    public void startPrintingProcess(boolean advanceFrameOnFinish) {
        if (!canPrint())
            return;

        ItemStack filmStack = getStack(Lightroom.FILM_SLOT);
        if (!(filmStack.getItem() instanceof DevelopedFilmItem film))
            return;

        if (getActualProcess(filmStack) == Lightroom.Process.CHROMATIC)
            printTime = Config.Common.LIGHTROOM_CHROMATIC_PRINT_TIME.get();
        else if (film.getType() == FilmType.BLACK_AND_WHITE)
            printTime = Config.Common.LIGHTROOM_BW_PRINT_TIME.get();
        else
            printTime = Config.Common.LIGHTROOM_COLOR_PRINT_TIME.get();

        advanceFrame = advanceFrameOnFinish;

        if (world != null) {
            world.setBlockState(getPos(), world.getBlockState(getPos())
                    .with(LightroomBlock.LIT, true), Block.NOTIFY_LISTENERS);
            world.playSound(null, getPos(), Exposure.SoundEvents.LIGHTROOM_PRINT.get(), SoundCategory.BLOCKS,
                    1f, world.getRandom().nextFloat() * 0.3f + 1f);
        }
    }

    public void stopPrintingProcess() {
        progress = 0;
        printTime = 0;
        advanceFrame = false;
        if (world != null && world.getBlockState(getPos()).getBlock() instanceof LightroomBlock)
            world.setBlockState(getPos(), world.getBlockState(getPos())
                    .with(LightroomBlock.LIT, false), Block.NOTIFY_LISTENERS);
    }

    public boolean isPrinting() {
        return printTime > 0;
    }

    public boolean canPrint() {
        if (getSelectedFrameIndex() < 0) // Upper bound is checked further down
            return false;

        ItemStack filmStack = getStack(Lightroom.FILM_SLOT);
        if (!(filmStack.getItem() instanceof DevelopedFilmItem developedFilm) || !developedFilm.hasExposedFrame(filmStack, getSelectedFrameIndex()))
            return false;

        Lightroom.Process process = getActualProcess(filmStack);

        ItemStack paperStack = getStack(Lightroom.PAPER_SLOT);

        return isPaperValidForPrint(paperStack, filmStack, process)
                && hasDyesForPrint(filmStack, paperStack, process)
                && canOutputToResultSlot(getStack(Lightroom.RESULT_SLOT), filmStack, process);
    }

    public boolean canPrintInCreativeMode() {
        if (getSelectedFrameIndex() < 0) // Upper bound is checked further down
            return false;

        ItemStack filmStack = getStack(Lightroom.FILM_SLOT);
        if (!(filmStack.getItem() instanceof DevelopedFilmItem developedFilm) || !developedFilm.hasExposedFrame(filmStack, getSelectedFrameIndex()))
            return false;

        return canOutputToResultSlot(getStack(Lightroom.RESULT_SLOT), filmStack, getActualProcess(filmStack));
    }

    protected boolean isPaperValidForPrint(ItemStack paperStack, ItemStack filmStack, Lightroom.Process process) {
        if (paperStack.isEmpty())
            return false;

        return process != Lightroom.Process.REGULAR || paperStack.isIn(Exposure.Tags.Items.PHOTO_PAPERS);
    }

    protected boolean hasDyesForPrint(ItemStack film, ItemStack paper, Lightroom.Process process) {
        int[] dyeSlots = getRequiredDyeSlotsForPrint(film, paper, process);

        for (int slot : dyeSlots) {
            if (getStack(slot).isEmpty())
                return false;
        }

        return true;
    }

    public boolean canOutputToResultSlot(ItemStack resultStack, ItemStack filmStack, Lightroom.Process process) {
        if (isSelectedFrameChromatic(filmStack, getSelectedFrameIndex()) && process == Lightroom.Process.CHROMATIC)
            return resultStack.isEmpty();

        return resultStack.isEmpty() || resultStack.getItem() instanceof PhotographItem
                || (resultStack.getItem() instanceof StackedPhotographsItem stackedPhotographsItem
                && stackedPhotographsItem.canAddPhotograph(resultStack));
    }

    protected int[] getRequiredDyeSlotsForPrint(ItemStack film, ItemStack paper, Lightroom.Process process) {
        if (!(film.getItem() instanceof IFilmItem filmItem))
            return ArrayUtils.EMPTY_INT_ARRAY;

        if (process == Lightroom.Process.REGULAR) {
            return filmItem.getType() == FilmType.COLOR ? Lightroom.DYES_FOR_COLOR : Lightroom.DYES_FOR_BW;
        }

        if (process == Lightroom.Process.CHROMATIC) {
            int chromaticStep = getChromaticStep(paper);
            if (chromaticStep == 0) return Lightroom.DYES_FOR_CHROMATIC_RED;
            if (chromaticStep == 1) return Lightroom.DYES_FOR_CHROMATIC_GREEN;
            if (chromaticStep == 2) return Lightroom.DYES_FOR_CHROMATIC_BLUE;
        }

        return ArrayUtils.EMPTY_INT_ARRAY;
    }

    protected int getChromaticStep(ItemStack paper) {
        if (!(paper.getItem() instanceof ChromaticSheetItem chromaticFragment))
            return 0;

        return chromaticFragment.getExposures(paper).size();
    }

    public boolean tryPrint() {
        Preconditions.checkState(world != null && !world.isClient, "Cannot be called clientside.");
        if (!canPrint())
            return false;

        ItemStack filmStack = getStack(Lightroom.FILM_SLOT);
        if (!(filmStack.getItem() instanceof DevelopedFilmItem developedFilm))
            return false;

        Optional<NbtCompound> selectedFrame = getSelectedFrame(filmStack);
        if (selectedFrame.isEmpty()) {
            LogUtils.getLogger().error("Unable to get selected frame '{}' : {}", getSelectedFrameIndex(), filmStack);
            return false;
        }

        NbtCompound frame = selectedFrame.get().copy();
        Lightroom.Process process = getActualProcess(filmStack);
        ItemStack paperStack = getStack(Lightroom.PAPER_SLOT);

        ItemStack printResult = createPrintResult(frame, filmStack, paperStack, process);
        putPrintResultInOutputSlot(printResult);

        // Consume items required for printing:
        int[] dyesSlots = getRequiredDyeSlotsForPrint(filmStack, paperStack, process);
        for (int slot : dyesSlots) {
            getStack(slot).decrement(1);
        }
        getStack(Lightroom.PAPER_SLOT).decrement(1);

        world.playSound(null, getPos(), Exposure.SoundEvents.PHOTOGRAPH_RUSTLE.get(), SoundCategory.PLAYERS, 0.8f, 1f);

        storeExperienceForPrint(filmStack, frame, process, printResult);

        if (process != Lightroom.Process.CHROMATIC) { // Chromatics create new exposure. Marking is not needed.
            // Mark exposure as printed
            String id = frame.getString(FrameData.ID);
            if (!id.isEmpty()) {
                ExposureServer.getExposureStorage().getOrQuery(id).ifPresent(exposure -> {
                    NbtCompound properties = exposure.getProperties();
                    if (!properties.getBoolean(ExposureSavedData.WAS_PRINTED_PROPERTY)) {
                        properties.putBoolean(ExposureSavedData.WAS_PRINTED_PROPERTY, true);
                        exposure.markDirty();
                    }
                });
            }
        }

        return true;
    }

    protected void storeExperienceForPrint(ItemStack film, NbtCompound frame, Lightroom.Process process, ItemStack result) {
        if (world == null)
            return;

        int xp = 0;
        if (process == Lightroom.Process.CHROMATIC) {
            // Printing intermediate channels does not grant xp. Only finished photograph does.
            xp = result.getItem() instanceof ChromaticSheetItem ? 0 : Config.Common.LIGHTROOM_EXPERIENCE_PER_PRINT_CHROMATIC.get();
        }
        else if (film.getItem() instanceof IFilmItem filmItem)
            xp = filmItem.getType() == FilmType.COLOR
                    ? Config.Common.LIGHTROOM_EXPERIENCE_PER_PRINT_COLOR.get()
                    : Config.Common.LIGHTROOM_EXPERIENCE_PER_PRINT_BW.get();

        if (xp > 0) {
            float variability = world.getRandom().nextFloat() * 0.3f + 1f;
            int variableXp = (int)Math.max(1, Math.ceil(xp * variability));
            storedExperience += variableXp;
        }
    }

    public void printInCreativeMode() {
        Preconditions.checkState(world != null && !world.isClient, "Cannot be called clientside.");
        if (!canPrintInCreativeMode())
            return;

        ItemStack filmStack = getStack(Lightroom.FILM_SLOT);
        if (!(filmStack.getItem() instanceof DevelopedFilmItem developedFilm))
            return;

        Optional<NbtCompound> selectedFrame = getSelectedFrame(filmStack);
        if (selectedFrame.isEmpty()) {
            LogUtils.getLogger().error("Unable to get selected frame '{}' : {}", getSelectedFrameIndex(), filmStack);
            return;
        }

        NbtCompound frame = selectedFrame.get().copy();
        Lightroom.Process process = getActualProcess(filmStack);
        ItemStack paperStack = getStack(Lightroom.PAPER_SLOT);

        ItemStack printResult = createPrintResult(frame, filmStack, paperStack, process);
        putPrintResultInOutputSlot(printResult);

        if (process == Lightroom.Process.CHROMATIC)
            getStack(Lightroom.PAPER_SLOT).decrement(1);

        world.playSound(null, getPos(), Exposure.SoundEvents.PHOTOGRAPH_RUSTLE.get(), SoundCategory.PLAYERS, 0.8f, 1f);

        if (process != Lightroom.Process.CHROMATIC) { // Chromatics create new exposure. Marking is not needed.
            // Mark exposure as printed
            String id = frame.getString(FrameData.ID);
            if (!id.isEmpty()) {
                ExposureServer.getExposureStorage().getOrQuery(id).ifPresent(exposure -> {
                    NbtCompound properties = exposure.getProperties();
                    if (!properties.getBoolean(ExposureSavedData.WAS_PRINTED_PROPERTY)) {
                        properties.putBoolean(ExposureSavedData.WAS_PRINTED_PROPERTY, true);
                        exposure.markDirty();
                    }
                });
            }
        }
    }

    protected void putPrintResultInOutputSlot(ItemStack printResult) {
        ItemStack resultStack = getStack(Lightroom.RESULT_SLOT);
        if (resultStack.isEmpty())
            resultStack = printResult;
        else if (resultStack.getItem() instanceof PhotographItem) {
            StackedPhotographsItem stackedPhotographsItem = Exposure.Items.STACKED_PHOTOGRAPHS.get();
            ItemStack newStackedPhotographs = new ItemStack(stackedPhotographsItem);
            stackedPhotographsItem.addPhotographOnTop(newStackedPhotographs, resultStack);
            stackedPhotographsItem.addPhotographOnTop(newStackedPhotographs, printResult);
            resultStack = newStackedPhotographs;
        } else if (resultStack.getItem() instanceof StackedPhotographsItem stackedPhotographsItem) {
            stackedPhotographsItem.addPhotographOnTop(resultStack, printResult);
        } else {
            LogUtils.getLogger().error("Unexpected item in result slot: " + resultStack);
            return;
        }
        setStack(Lightroom.RESULT_SLOT, resultStack);
    }

    protected ItemStack createPrintResult(NbtCompound frame, ItemStack film, ItemStack paper, Lightroom.Process process) {
        if (!(film.getItem() instanceof IFilmItem filmItem))
            throw new IllegalStateException("Film stack is invalid: " + film);

        paper = paper.copy();

        // Type is currently used to decide what dyes are used when copying photograph.
        // Or for quests. Or other purposes. It's important.
        frame.putString(FrameData.TYPE, process == Lightroom.Process.REGULAR
                ? filmItem.getType().asString() : FilmType.COLOR.asString());

        if (process == Lightroom.Process.CHROMATIC) {
            ItemAndStack<ChromaticSheetItem> chromaticFragment = new ItemAndStack<>(
                    paper.getItem() instanceof ChromaticSheetItem ? paper
                            : new ItemStack(Exposure.Items.CHROMATIC_SHEET.get()));

            chromaticFragment.getItem().addExposure(chromaticFragment.getStack(), frame);

            if (chromaticFragment.getItem().getExposures(chromaticFragment.getStack()).size() >= 3)
                return chromaticFragment.getItem().finalize(Objects.requireNonNull(world), chromaticFragment.getStack());

            return chromaticFragment.getStack();
        }
        else {
            ItemStack photographStack = new ItemStack(Exposure.Items.PHOTOGRAPH.get());
            photographStack.setNbt(frame);
            return photographStack;
        }
    }

    public void dropStoredExperience(@Nullable PlayerEntity player) {
        if (world instanceof ServerWorld serverLevel && storedExperience > 0) {
            ExperienceOrbEntity.spawn(serverLevel, Vec3d.ofCenter(getPos()), storedExperience);
            storedExperience = 0;
            markDirty();
        }
    }


    // Container

    @Override
    protected @NotNull Text getContainerName() {
        return Text.translatable("block.exposure.lightroom");
    }

    @Override
    protected @NotNull ScreenHandler createScreenHandler(int containerId, PlayerInventory inventory) {
        return new LightroomMenu(containerId, inventory, this, containerData);
    }

    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == Lightroom.FILM_SLOT) return stack.getItem() instanceof DevelopedFilmItem;
        else if (slot == Lightroom.CYAN_SLOT) return stack.isIn(Exposure.Tags.Items.CYAN_PRINTING_DYES);
        else if (slot == Lightroom.MAGENTA_SLOT) return stack.isIn(Exposure.Tags.Items.MAGENTA_PRINTING_DYES);
        else if (slot == Lightroom.YELLOW_SLOT) return stack.isIn(Exposure.Tags.Items.YELLOW_PRINTING_DYES);
        else if (slot == Lightroom.BLACK_SLOT) return stack.isIn(Exposure.Tags.Items.BLACK_PRINTING_DYES);
        else if (slot == Lightroom.PAPER_SLOT)
            return stack.isIn(Exposure.Tags.Items.PHOTO_PAPERS) ||
                    (stack.getItem() instanceof ChromaticSheetItem chromatic && chromatic.getExposures(stack).size() < 3);
        else if (slot == Lightroom.RESULT_SLOT)
            return stack.getItem() instanceof PhotographItem || stack.getItem() instanceof ChromaticSheetItem;
        return false;
    }

    protected void inventoryContentsChanged(int slot) {
        if (slot == Lightroom.FILM_SLOT)
            setSelectedFrame(0);

        markDirty();
    }

    @Override
    public boolean canPlayerUse(@NotNull PlayerEntity player) {
        return this.world != null && this.world.getBlockEntity(this.pos) == this
                && player.squaredDistanceTo(this.pos.getX() + 0.5D,
                this.pos.getY() + 0.5D,
                this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public int @NotNull [] getAvailableSlots(@NotNull Direction face) {
        return Lightroom.ALL_SLOTS;
    }

    @Override
    public boolean canInsert(int index, @NotNull ItemStack itemStack, @Nullable Direction direction) {
        if (direction == Direction.DOWN)
            return false;
        return isValid(index, itemStack);
    }

    @Override
    public boolean isValid(int index, ItemStack stack) {
        return index != Lightroom.RESULT_SLOT && isItemValidForSlot(index, stack) && super.isValid(index, stack);
    }

    @Override
    public boolean canExtract(int index, @NotNull ItemStack pStack, @NotNull Direction direction) {
        for (int outputSlot : Lightroom.OUTPUT_SLOTS) {
            if (index == outputSlot)
                return true;
        }
        return false;
    }


    // Load/Save
    @Override
    public void readNbt(@NotNull NbtCompound tag) {
        super.readNbt(tag);

        this.items = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
        Inventories.readNbt(tag, this.items);

        // Backwards compatibility:
        if (tag.contains("Inventory", NbtElement.COMPOUND_TYPE)) {
            NbtCompound inventory = tag.getCompound("Inventory");
            NbtList itemsList = inventory.getList("Items", NbtElement.COMPOUND_TYPE);

            for (int i = 0; i < itemsList.size(); i++) {
                NbtCompound itemTags = itemsList.getCompound(i);
                int slot = itemTags.getInt("Slot");

                if (slot >= 0 && slot < items.size())
                    items.set(slot, ItemStack.fromNbt(itemTags));
            }
        }

        this.setSelectedFrame(tag.getInt("SelectedFrame"));
        this.progress = tag.getInt("Progress");
        this.printTime = tag.getInt("PrintTime");
        this.storedExperience = tag.getInt("PrintedPhotographsCount");
        this.advanceFrame = tag.getBoolean("AdvanceFrame");
        this.process = Lightroom.Process.fromStringOrDefault(tag.getString("Process"), Lightroom.Process.REGULAR);
    }

    @Override
    protected void writeNbt(@NotNull NbtCompound tag) {
        super.writeNbt(tag);
        Inventories.writeNbt(tag, items);
        if (getSelectedFrameIndex() > 0)
            tag.putInt("SelectedFrame", getSelectedFrameIndex());
        if (progress > 0)
            tag.putInt("Progress", progress);
        if (printTime > 0)
            tag.putInt("PrintTime", printTime);
        if (storedExperience > 0)
            tag.putInt("PrintedPhotographsCount", storedExperience);
        if (advanceFrame)
            tag.putBoolean("AdvanceFrame", true);
        if (process != Lightroom.Process.REGULAR)
            tag.putString("Process", process.asString());
    }

    protected DefaultedList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    public int size() {
        return Lightroom.SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return getItems().stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public @NotNull ItemStack getStack(int slot) {
        return getItems().get(slot);
    }

    @Override
    public @NotNull ItemStack removeStack(int slot, int amount) {
        ItemStack itemStack = Inventories.splitStack(getItems(), slot, amount);
        if (!itemStack.isEmpty())
            inventoryContentsChanged(slot);
        return itemStack;
    }

    @Override
    public @NotNull ItemStack removeStack(int slot) {
        return Inventories.removeStack(getItems(), slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.getItems().set(slot, stack);
        if (stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack());
        }
        inventoryContentsChanged(slot);
    }

    @Override
    public void clear() {
        getItems().clear();
        inventoryContentsChanged(-1);
    }

    @Override
    public void markDirty() {
        super.markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(getPos(), getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
        }
    }


    // Sync:

    @Override
    @Nullable
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public @NotNull NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }
}
