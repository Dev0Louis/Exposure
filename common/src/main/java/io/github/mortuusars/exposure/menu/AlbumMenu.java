package io.github.mortuusars.exposure.menu;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.item.AlbumItem;
import io.github.mortuusars.exposure.item.AlbumPage;
import io.github.mortuusars.exposure.item.PhotographItem;
import io.github.mortuusars.exposure.network.Packets;
import io.github.mortuusars.exposure.network.packet.server.AlbumSignC2SP;
import io.github.mortuusars.exposure.util.ItemAndStack;
import io.github.mortuusars.exposure.util.Side;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class AlbumMenu extends ScreenHandler {
    public static final int CANCEL_ADDING_PHOTO_BUTTON = -1;
    public static final int PREVIOUS_PAGE_BUTTON = 0;
    public static final int NEXT_PAGE_BUTTON = 1;
    public static final int LEFT_PAGE_PHOTO_BUTTON = 2;
    public static final int RIGHT_PAGE_PHOTO_BUTTON = 3;
    public static final int ENTER_SIGN_MODE_BUTTON = 4;
    public static final int SIGN_BUTTON = 5;
    public static final int CANCEL_SIGNING_BUTTON = 6;

    protected final ItemAndStack<AlbumItem> album;
    protected final boolean editable;

    protected final List<AlbumPage> pages;

    protected final List<AlbumPhotographSlot> photographSlots = new ArrayList<>();
    protected final List<AlbumPlayerInventorySlot> playerInventorySlots = new ArrayList<>();

    protected Property currentSpreadIndex = Property.create();

    @Nullable
    protected Side sideBeingAddedTo = null;
    protected boolean signing;
    protected String title = "";

    protected final Map<Integer, Consumer<PlayerEntity>> buttonActions = new HashMap<>() {{
        put(CANCEL_ADDING_PHOTO_BUTTON, p -> {
            sideBeingAddedTo = null;
            if (!getCursorStack().isEmpty()) {
                p.getInventory().offerOrDrop(getCursorStack());
                setCursorStack(ItemStack.EMPTY);
            }
            updatePlayerInventorySlots();
        });
        put(PREVIOUS_PAGE_BUTTON, p -> {
            onButtonClick(p, CANCEL_ADDING_PHOTO_BUTTON);
            setCurrentSpreadIndex(Math.max(0, getCurrentSpreadIndex() - 1));
        });
        put(NEXT_PAGE_BUTTON, p -> {
            onButtonClick(p, CANCEL_ADDING_PHOTO_BUTTON);
            setCurrentSpreadIndex(Math.min((getPages().size() - 1) / 2, getCurrentSpreadIndex() + 1));
        });
        put(LEFT_PAGE_PHOTO_BUTTON, p -> onPhotoButtonPress(p, Side.LEFT));
        put(RIGHT_PAGE_PHOTO_BUTTON, p -> onPhotoButtonPress(p, Side.RIGHT));
        put(ENTER_SIGN_MODE_BUTTON, p -> {
            signing = true;
            sideBeingAddedTo = null;
        });
        put(SIGN_BUTTON, p -> signAlbum(p));
        put(CANCEL_SIGNING_BUTTON, p -> signing = false);
    }};

    public AlbumMenu(int containerId, PlayerInventory playerInventory, ItemAndStack<AlbumItem> album, boolean editable) {
        this(Exposure.MenuTypes.ALBUM.get(), containerId, playerInventory, album, editable);
    }

    protected AlbumMenu(ScreenHandlerType<? extends ScreenHandler> type, int containerId, PlayerInventory playerInventory, ItemAndStack<AlbumItem> album, boolean editable) {
        super(type, containerId);
        this.album = album;
        this.editable = editable;

        List<AlbumPage> albumPages = album.getItem().getPages(album.getStack());
        pages = isAlbumEditable() ? new ArrayList<>(albumPages) : albumPages;

        if (isAlbumEditable()) {
            while (pages.size() < album.getItem().getMaxPages()) {
                addEmptyPage();
            }
        }

        addPhotographSlots();
        addPlayerInventorySlots(playerInventory, 70, 115);
        addProperty(currentSpreadIndex);
    }

    private void addPhotographSlots() {
        ItemStack[] photographs = pages.stream().map(AlbumPage::getPhotographStack).toArray(ItemStack[]::new);
        SimpleInventory container = new SimpleInventory(photographs);

        for (int i = 0; i < container.size(); i++) {
            int x = i % 2 == 0 ? 71 : 212;
            int y = 67;
            AlbumPhotographSlot slot = new AlbumPhotographSlot(container, i, x, y);
            addSlot(slot);
            photographSlots.add(slot);
        }

        container.addListener(c -> {
            List<AlbumPage> pages = getPages();
            for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
                AlbumPage page = pages.get(pageIndex);
                ItemStack stack = container.getStack(pageIndex);
                page.setPhotographStack(stack);
            }
            updateAlbumStack();
        });
    }

    private void addPlayerInventorySlots(PlayerInventory playerInventory, int x, int y) {
        // Player inventory slots
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                AlbumPlayerInventorySlot slot = new AlbumPlayerInventorySlot(playerInventory, column + row * 9 + 9,
                        x + column * 18, y + row * 18);
                addSlot(slot);
                playerInventorySlots.add(slot);
            }
        }

        // Player hotbar slots
        // Hotbar should go after main inventory for Shift+Click to work properly.
        for (int index = 0; index < 9; ++index) {
            boolean disabled = index == playerInventory.selectedSlot && playerInventory.getMainHandStack().getItem() instanceof AlbumItem;
            AlbumPlayerInventorySlot slot = new AlbumPlayerInventorySlot(playerInventory, index,
                    x + index * 18, y + 58) {
                @Override
                public boolean canTakeItems(PlayerEntity player) {
                    return !disabled;
                }

                @Override
                public boolean canInsert(ItemStack stack) {
                    return !disabled;
                }
            };
            addSlot(slot);
            playerInventorySlots.add(slot);
        }
    }

    protected void updatePlayerInventorySlots() {
        boolean isInAddingPhotographMode = isInAddingPhotographMode();
        for (AlbumPlayerInventorySlot slot : playerInventorySlots) {
            slot.setActive(isInAddingPhotographMode);
        }
    }

    public boolean isAlbumEditable() {
        return editable;
    }

    public boolean isInAddingPhotographMode() {
        return getSideBeingAddedTo() != null;
    }

    public boolean isInSigningMode() {
        return signing;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String authorName) {
        this.title = authorName;
    }

    public boolean canSignAlbum() {
        for (AlbumPage page : getPages()) {
            if (!page.getPhotographStack().isEmpty() || page.getNote().left().map(note -> !note.isEmpty()).orElse(false))
                return true;
        }
        return false;
    }

    protected void signAlbum(PlayerEntity player) {
        if (!player.getWorld().isClient)
            return;

        if (!canSignAlbum())
            throw new IllegalStateException("Cannot sign the album.\n" + Arrays.toString(getPages().toArray()));

        Packets.sendToServer(new AlbumSignC2SP(title));
    }

    public void updateAlbumStack() {
        List<AlbumPage> pages = getPages();
        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            AlbumPage page = pages.get(pageIndex);
            album.getItem().setPage(album.getStack(), page, pageIndex);
        }
    }

    protected void addEmptyPage() {
        AlbumPage page = album.getItem().createEmptyPage();
        pages.add(page);
        album.getItem().addPage(album.getStack(), page);
    }

    public List<AlbumPlayerInventorySlot> getPlayerInventorySlots() {
        return playerInventorySlots;
    }

    public List<AlbumPage> getPages() {
        return pages;
    }

    public Optional<AlbumPage> getPage(Side side) {
        return getPage(getCurrentSpreadIndex() * 2 + side.getIndex());
    }

    public Optional<AlbumPage> getPage(int pageIndex) {
        if (pageIndex <= getPages().size() - 1)
            return Optional.ofNullable(getPages().get(pageIndex));

        return Optional.empty();
    }

    public Optional<AlbumPhotographSlot> getPhotographSlot(Side side) {
        return getPhotographSlot(getCurrentSpreadIndex() * 2 + (side == Side.LEFT ? 0 : 1));
    }

    public Optional<AlbumPhotographSlot> getPhotographSlot(int index) {
        if (index >= 0 && index < photographSlots.size())
            return Optional.ofNullable(photographSlots.get(index));

        return Optional.empty();
    }

    public ItemStack getPhotograph(Side side) {
        return getPhotographSlot(side).map(Slot::getStack).orElse(ItemStack.EMPTY);
    }

    public int getCurrentSpreadIndex() {
        return this.currentSpreadIndex.get();
    }

    public void setCurrentSpreadIndex(int spreadIndex) {
        this.currentSpreadIndex.set(spreadIndex);
    }

    public @Nullable Side getSideBeingAddedTo() {
        return sideBeingAddedTo;
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        @Nullable Consumer<PlayerEntity> buttonAction = buttonActions.get(id);
        if (buttonAction != null) {
            buttonAction.accept(player);
            return true;
        }

        return false;
    }

    private void onPhotoButtonPress(PlayerEntity player, Side side) {
        Preconditions.checkArgument(isAlbumEditable(),
                "Photo Button should be disabled and hidden when Album is not editable. " + album.getStack());

        Optional<AlbumPhotographSlot> photographSlot = getPhotographSlot(side);
        if (photographSlot.isEmpty())
            return;

        AlbumPhotographSlot slot = photographSlot.get();
        if (!slot.hasStack()) {
            sideBeingAddedTo = side;
        }
        else {
            ItemStack stack = slot.getStack();
            if (!player.getInventory().insertStack(stack))
                player.dropItem(stack, false);

            slot.setStackNoCallbacks(ItemStack.EMPTY);
        }

        updatePlayerInventorySlots();
    }

    @Override
    public void onSlotClick(int slotId, int button, SlotActionType clickType, PlayerEntity player) {
        // Both sides

        if (sideBeingAddedTo == null || slotId < 0 || slotId >= slots.size()) {
            super.onSlotClick(slotId, button, clickType, player);
            return;
        }

        Slot slot = slots.get(slotId);
        ItemStack stack = slot.getStack();

        if (button == InputUtil.field_32000
                && slot instanceof AlbumPlayerInventorySlot
                && stack.getItem() instanceof PhotographItem
                && getCursorStack().isEmpty()) {
            int pageIndex = getCurrentSpreadIndex() * 2 + sideBeingAddedTo.getIndex();
            Optional<AlbumPhotographSlot> photographSlot = getPhotographSlot(pageIndex);
            if (photographSlot.isEmpty() || !photographSlot.get().getStack().isEmpty())
                return;

            photographSlot.get().setStackNoCallbacks(stack);
            slot.setStackNoCallbacks(ItemStack.EMPTY);

            if (player.getWorld().isClient)
                player.playSound(Exposure.SoundEvents.PHOTOGRAPH_PLACE.get(), 0.8f, 1.1f);

            sideBeingAddedTo = null;
            updatePlayerInventorySlots();
        }
        else
            super.onSlotClick(slotId, button, clickType, player);
    }

    @Override
    public @NotNull ItemStack quickMove(PlayerEntity player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return !isAlbumEditable() || (player.getMainHandStack().getItem() instanceof AlbumItem
                || player.getOffHandStack().getItem() instanceof AlbumItem);
    }

    public static AlbumMenu fromBuffer(int containerId, PlayerInventory inventory, PacketByteBuf buffer) {
        return new AlbumMenu(containerId, inventory, new ItemAndStack<>(buffer.readItemStack()), buffer.readBoolean());
    }
}
