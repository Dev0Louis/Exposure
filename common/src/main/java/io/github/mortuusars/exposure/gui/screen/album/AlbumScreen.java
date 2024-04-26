package io.github.mortuusars.exposure.gui.screen.album;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.gui.screen.element.Pager;
import io.github.mortuusars.exposure.gui.screen.element.TextBlock;
import io.github.mortuusars.exposure.gui.screen.element.textbox.HorizontalAlignment;
import io.github.mortuusars.exposure.gui.screen.element.textbox.TextBox;
import io.github.mortuusars.exposure.item.AlbumPage;
import io.github.mortuusars.exposure.item.PhotographItem;
import io.github.mortuusars.exposure.menu.AlbumMenu;
import io.github.mortuusars.exposure.menu.AlbumPlayerInventorySlot;
import io.github.mortuusars.exposure.network.Packets;
import io.github.mortuusars.exposure.network.packet.server.AlbumSyncNoteC2SP;
import io.github.mortuusars.exposure.util.ItemAndStack;
import io.github.mortuusars.exposure.util.PagingDirection;
import io.github.mortuusars.exposure.util.Side;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class AlbumScreen extends HandledScreen<AlbumMenu> {
    public static final Identifier TEXTURE = Exposure.resource("textures/gui/album.png");
    public static final int MAIN_FONT_COLOR = 0xFFB59774;
    public static final int SECONDARY_FONT_COLOR = 0xFFEFE4CA;
    public static final int SELECTION_COLOR = 0xFF8888FF;
    public static final int SELECTION_UNFOCUSED_COLOR = 0xFFBBBBFF;

    @NotNull
    protected final MinecraftClient client;
    @NotNull
    protected final PlayerEntity player;
    @NotNull
    protected final ClientPlayerInteractionManager gameMode;

    protected final Pager pager = new Pager(SoundEvents.ITEM_BOOK_PAGE_TURN) {
        @Override
        public void onPageChanged(PagingDirection pagingDirection, int prevPage, int currentPage) {
            super.onPageChanged(pagingDirection, prevPage, currentPage);
            sendButtonClick(pagingDirection == PagingDirection.PREVIOUS ? AlbumMenu.PREVIOUS_PAGE_BUTTON : AlbumMenu.NEXT_PAGE_BUTTON);
        }
    };

    protected final List<Page> pages = new ArrayList<>();

    @Nullable
    protected ButtonWidget enterSignModeButton;

    public AlbumScreen(AlbumMenu menu, PlayerInventory playerInventory, Text title) {
        super(menu, playerInventory, title);
        client = MinecraftClient.getInstance();
        player = Objects.requireNonNull(client.player);
        gameMode = Objects.requireNonNull(client.interactionManager);
    }

    @Override
    protected void handledScreenTick() {
        forEachPage(page -> page.noteWidget.ifLeft(TextBox::tick));
    }

    @Override
    protected void init() {
        this.backgroundWidth = 298;
        this.backgroundHeight = 188;
        super.init();

        titleY = -999;
        playerInventoryTitleX = 69;
        playerInventoryTitleY = -999;

        pages.clear();

        // LEFT:
        Page leftPage = createPage(Side.LEFT, 0);
        pages.add(leftPage);

        TexturedButtonWidget previousButton = new TexturedButtonWidget(x + 12, y + 164, 13, 15,
                216, 188, 15, TEXTURE, 512, 512,
                button -> pager.changePage(PagingDirection.PREVIOUS), Text.translatable("gui.exposure.previous_page"));
        previousButton.setTooltip(Tooltip.of(Text.translatable("gui.exposure.previous_page")));
        addDrawableChild(previousButton);

        // RIGHT:
        Page rightPage = createPage(Side.RIGHT, 140);
        pages.add(rightPage);

        TexturedButtonWidget nextButton = new TexturedButtonWidget(x + 273, y + 164, 13, 15,
                229, 188, 15, TEXTURE, 512, 512,
                button -> pager.changePage(PagingDirection.NEXT), Text.translatable("gui.exposure.next_page"));
        nextButton.setTooltip(Tooltip.of(Text.translatable("gui.exposure.next_page")));
        addDrawableChild(nextButton);

        // MISC:
        if (getScreenHandler().isAlbumEditable()) {
            enterSignModeButton = new TexturedButtonWidget(x - 23, y + 17, 22, 22, 242, 188,
                    22, TEXTURE, 512, 512,
                    b -> enterSignMode(), Text.translatable("gui.exposure.album.sign"));
            enterSignModeButton.setTooltip(Tooltip.of(Text.translatable("gui.exposure.album.sign")));
            addDrawableChild(enterSignModeButton);
        }

        int spreadsCount = (int) Math.ceil(getScreenHandler().getPages().size() / 2f);
        pager.init(spreadsCount, false, previousButton, nextButton);
    }

    protected Page createPage(Side side, int xOffset) {
        int x1 = x + xOffset;
        int y1 = y;

        Rect2i page = new Rect2i(x1, y1, 149, 188);
        Rect2i photo = new Rect2i(x1 + 25, y1 + 21, 108, 108);
        Rect2i exposure = new Rect2i(x1 + 31, y1 + 27, 96, 96);
        Rect2i note = new Rect2i(x1 + 22, y1 + 133, 114, 27);

        PhotographSlotButton photographButton = new PhotographSlotButton(exposure, photo.getX(), photo.getY(),
                photo.getWidth(), photo.getHeight(), 0, 188, 108, TEXTURE, 512, 512,
                b -> {
                    PhotographSlotButton button = (PhotographSlotButton) b;
                    ItemStack photograph = button.getPhotograph();
                    if (photograph.isEmpty()) {
                        if (button.isEditable) {
                            sendButtonClick(side == Side.LEFT ? AlbumMenu.LEFT_PAGE_PHOTO_BUTTON : AlbumMenu.RIGHT_PAGE_PHOTO_BUTTON);
                            button.playDownSound(client.getSoundManager());
                        }
                    } else
                        inspectPhotograph(photograph);
                },
                b -> {
                    PhotographSlotButton button = (PhotographSlotButton) b;
                    if (button.isEditable && !button.getPhotograph().isEmpty()) {
                        sendButtonClick(side == Side.LEFT ? AlbumMenu.LEFT_PAGE_PHOTO_BUTTON : AlbumMenu.RIGHT_PAGE_PHOTO_BUTTON);
                        client.getSoundManager().play(PositionedSoundInstance.master(
                                Exposure.SoundEvents.PHOTOGRAPH_PLACE.get(), 0.7f, 1.1f));
                    }
                }, () -> getScreenHandler().getPhotograph(side), getScreenHandler().isAlbumEditable()) {
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return !isInAddingMode() && super.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public boolean isHovered() {
                return !isInAddingMode() && super.isHovered();
            }
        };
        addDrawableChild(photographButton);

        Either<TextBox, TextBlock> noteWidget;
        if (getScreenHandler().isAlbumEditable()) {
            TextBox textBox = new TextBox(textRenderer, note.getX(), note.getY(), note.getWidth(), note.getHeight(),
                    () -> getScreenHandler().getPage(side).map(p -> p.getNote().left().orElseThrow()).orElse(""),
                    text -> onNoteChanged(side, text))
                    .setFontColor(MAIN_FONT_COLOR, MAIN_FONT_COLOR)
                    .setSelectionColor(SELECTION_COLOR, SELECTION_UNFOCUSED_COLOR);
            textBox.horizontalAlignment = HorizontalAlignment.CENTER;
            addDrawableChild(textBox);
            noteWidget = Either.left(textBox);
        } else {
            TextBlock textBlock = new TextBlock(textRenderer, note.getX(), note.getY(),
                    note.getWidth(), note.getHeight(), getNoteComponent(side), this::handleTextClick);
            textBlock.fontColor = MAIN_FONT_COLOR;
            textBlock.alignment = HorizontalAlignment.CENTER;
            textBlock.drawShadow = false;

            //  TextBlock is rendered manually to not be a part of TAB navigation.
            //  addRenderableWidget(textBlock);

            noteWidget = Either.right(textBlock);
        }

        return new Page(side, page, photo, exposure, note, photographButton, noteWidget);
    }

    protected void onNoteChanged(Side side, String noteText) {
        getScreenHandler().getPage(side).ifPresent(page -> {
            page.setNote(Either.left(noteText));
            int pageIndex = getScreenHandler().getCurrentSpreadIndex() * 2 + side.getIndex();
            Packets.sendToServer(new AlbumSyncNoteC2SP(pageIndex, noteText));
        });
    }

    
    // RENDER
    
    @Override
    public void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTick) {
        pager.update();

        if (enterSignModeButton != null)
            enterSignModeButton.visible = getScreenHandler().canSignAlbum();

        boolean isInAddingPhotographMode = isInAddingMode();

        // Note should be hidden when adding photograph because it's drawn over the slots. Blit offset does not help.
        forEachPage(page -> page.getNoteWidget().visible = !isInAddingPhotographMode);

        for (Page page : pages) {
            page.photographButton.visible = !getScreenHandler().getPhotograph(page.side).isEmpty()
                    || (!isInAddingPhotographMode && getScreenHandler().isAlbumEditable());
        }

        playerInventoryTitleY = isInAddingPhotographMode ? getScreenHandler().getPlayerInventorySlots().get(0).y - 12 : -999;

        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        for (Page page : pages) {
            ClickableWidget noteWidget = page.getNoteWidget();
            if (noteWidget instanceof TextBlock textBlock) {
                textBlock.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        if (isInAddingPhotographMode) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            for (Slot slot : getScreenHandler().slots) {
                if (!slot.getStack().isEmpty() && !(slot.getStack().getItem() instanceof PhotographItem)) {
                    guiGraphics.drawTexture(TEXTURE, x + slot.x - 1, y + slot.y - 1, 350, 176, 404,
                            18, 18, 512, 512);
                }
            }
            RenderSystem.disableBlend();
        }

        this.drawMouseoverTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext guiGraphics, int mouseX, int mouseY) {
        guiGraphics.getMatrices().push();
        guiGraphics.getMatrices().translate(0, 0, 15);
        super.drawForeground(guiGraphics, mouseX, mouseY);

        guiGraphics.getMatrices().pop();
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext guiGraphics, int x, int y) {
        if (isInAddingMode() && focusedSlot != null && !focusedSlot.getStack()
                .isEmpty() && !(focusedSlot.getStack().getItem() instanceof PhotographItem))
            return; // Do not render tooltips for greyed-out items

        if (!isInAddingMode()) {
            for (Page page : pages) {
                if (page.photographButton.isSelected()) {
                    page.photographButton.renderTooltip(guiGraphics, x, y);
                    return;
                }

                if (getScreenHandler().isAlbumEditable() && page.isMouseOver(page.noteArea, x, y)) {
                    List<Text> tooltip = new ArrayList<>();
                    tooltip.add(Text.translatable("gui.exposure.album.note"));

                    if (!page.getNoteWidget().isFocused())
                        tooltip.add(Text.translatable("gui.exposure.album.left_click_to_edit"));

                    boolean hasText = page.noteWidget.left().map(box -> !box.getText().isEmpty()).orElse(false);
                    if (hasText)
                        tooltip.add(Text.translatable("gui.exposure.album.right_click_to_clear"));

                    guiGraphics.drawTooltip(this.textRenderer, tooltip, Optional.empty(), x, y);

                    return;
                }
            }
        }

        super.drawMouseoverTooltip(guiGraphics, x, y);
    }

    @Override
    protected @NotNull List<Text> getTooltipFromItem(ItemStack stack) {
        List<Text> tooltipLines = super.getTooltipFromItem(stack);
        if (isInAddingMode() && focusedSlot != null && focusedSlot.getStack() == stack
                && stack.getItem() instanceof PhotographItem) {
            tooltipLines.add(Text.empty());
            tooltipLines.add(Text.translatable("gui.exposure.album.left_click_to_add"));
        }
        return tooltipLines;
    }

    @Override
    protected void drawBackground(DrawContext guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.drawTexture(TEXTURE, x, y, 0, 0, 0,
                backgroundWidth, backgroundHeight, 512, 512);

        if (enterSignModeButton != null && enterSignModeButton.visible) {
            guiGraphics.drawTexture(TEXTURE, x - 27, y + 14, 447, 0,
                    27, 28, 512, 512);
        }

        int currentSpreadIndex = getScreenHandler().getCurrentSpreadIndex();
        drawPageNumbers(guiGraphics, currentSpreadIndex);

        if (isInAddingMode()) {
            @Nullable Side pageBeingAddedTo = getScreenHandler().getSideBeingAddedTo();
            for (Page page : pages) {
                if (page.side == pageBeingAddedTo) {
                    guiGraphics.drawTexture(TEXTURE, page.photoArea.getX(), page.photoArea.getY(), 10, 0, 296,
                            page.photoArea.getWidth(), page.photoArea.getHeight(), 512, 512);
                    break;
                }
            }

            AlbumPlayerInventorySlot firstSlot = getScreenHandler().getPlayerInventorySlots().get(0);
            int x = firstSlot.x - 8;
            int y = firstSlot.y - 18;
            guiGraphics.drawTexture(TEXTURE, x + x, y + y, 10, 0, 404, 176, 100, 512, 512);
        }
    }

    protected void drawPageNumbers(DrawContext guiGraphics, int currentSpreadIndex) {
        TextRenderer font = client.textRenderer;

        String leftPageNumber = Integer.toString(currentSpreadIndex * 2 + 1);
        String rightPageNumber = Integer.toString(currentSpreadIndex * 2 + 2);

        guiGraphics.drawText(font, leftPageNumber, x + 71 + (8 - font.getWidth(leftPageNumber) / 2),
                y + 167, SECONDARY_FONT_COLOR, false);

        guiGraphics.drawText(font, rightPageNumber, x + 212 + (8 - font.getWidth(rightPageNumber) / 2),
                y + 167, SECONDARY_FONT_COLOR, false);
    }


    // CONTROLS:

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isInAddingMode()) {
            if (!isHoveringOverInventory(mouseX, mouseY)
                && (!isClickOutsideBounds(mouseX, mouseY, x, y, button) || getScreenHandler().getCursorStack().isEmpty())) {
                sendButtonClick(AlbumMenu.CANCEL_ADDING_PHOTO_BUTTON);
                return true;
            }

            return super.mouseClicked(mouseX, mouseY, button);
        }

        for (Page page : pages) {
            if (getScreenHandler().isAlbumEditable() && button == InputUtil.field_32002 && page.isMouseOver(page.noteArea, mouseX, mouseY)) {
                page.noteWidget.ifLeft(box -> {
                    box.setText(""); // Clear the note
                });
                return true;
            }
        }

        boolean handled = super.mouseClicked(mouseX, mouseY, button);

        for (Page page : pages) {
            ClickableWidget noteWidget = page.getNoteWidget();
            if (noteWidget instanceof TextBlock textBlock && textBlock.mouseClicked(mouseX, mouseY, button)) {
                handled = true;
                break;
            }
        }

        for (Page page : pages) {
            if (page.getNoteWidget().isFocused() && !page.isMouseOver(page.noteArea, mouseX, mouseY)) {
                setFocused(null);
                return true;
            }
        }

        if (!(getFocused() instanceof TextBox))
            setFocused(null); // Clear focus on mouse click because it's annoying. But keep on textbox to type.

        return handled;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (cursorDragging && !getScreenHandler().getCursorStack().isEmpty() && getScreenHandler().getCursorStack().getCount() == 1) {
            cursorDragging = false; // Fixes weird issue with carried item not placing when dragging slightly
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean handleTextClick(@Nullable Style style) {
        if (style == null)
            return false;

        ClickEvent clickEvent = style.getClickEvent();
        if (clickEvent == null)
            return false;
        else if (clickEvent.getAction() == ClickEvent.Action.CHANGE_PAGE) {
            String pageIndexStr = clickEvent.getValue();
            int pageIndex = Integer.parseInt(pageIndexStr) - 1;
            forcePage(pageIndex);
            return true;
        }

        boolean handled = super.handleTextClick(style);
        if (handled && clickEvent.getAction() == ClickEvent.Action.RUN_COMMAND)
            close();
        return handled;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isInAddingMode())
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        else
            return this.getFocused() != null && this.isDragging() && button == 0
                    && this.getFocused().mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    protected void sendButtonClick(int buttonId) {
        getScreenHandler().onButtonClick(player, buttonId);
        gameMode.clickButton(getScreenHandler().syncId, buttonId);

        if (buttonId == AlbumMenu.CANCEL_ADDING_PHOTO_BUTTON)
            setFocused(null);

        if (buttonId == AlbumMenu.PREVIOUS_PAGE_BUTTON || buttonId == AlbumMenu.NEXT_PAGE_BUTTON) {
            for (Page page : pages) {
                page.noteWidget
                        .ifLeft(TextBox::setCursorToEnd)
                        .ifRight(textBlock -> textBlock.setMessage(getNoteComponent(page.side)));
            }
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean isHoveringOverInventory(double mouseX, double mouseY) {
        if (!isInAddingMode())
            return false;

        AlbumPlayerInventorySlot firstSlot = getScreenHandler().getPlayerInventorySlots().get(0);
        int x = firstSlot.x - 8;
        int y = firstSlot.y - 18;
        return isPointWithinBounds(x, y, 176, 100, mouseX, mouseY);
    }

    protected boolean isHoveringOverSignElement(double mouseX, double mouseY) {
        return enterSignModeButton == null
                || (enterSignModeButton.visible && isPointWithinBounds(x - 27, y + 14, 27, 28, mouseX, mouseY));
    }

    @Override
    protected boolean isClickOutsideBounds(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        return super.isClickOutsideBounds(mouseX, mouseY, guiLeft, guiTop, mouseButton)
                && !isHoveringOverInventory(mouseX, mouseY)
                && !isHoveringOverSignElement(mouseX, mouseY);
    }

    @SuppressWarnings("UnusedReturnValue")
    protected boolean forcePage(int pageIndex) {
        try {
            int newSpreadIndex = pageIndex / 2;

            if (newSpreadIndex == getScreenHandler().getCurrentSpreadIndex() || newSpreadIndex < 0
                    || newSpreadIndex > getScreenHandler().getPages().size() / 2) {
                return false;
            }

            PagingDirection pagingDirection = newSpreadIndex < getScreenHandler().getCurrentSpreadIndex()
                    ? PagingDirection.PREVIOUS : PagingDirection.NEXT;

            int pageChanges = 0; // Safeguard against infinite loop. Probably not needed. But I don't mind it.
            while (newSpreadIndex != getScreenHandler().getCurrentSpreadIndex() || !pager.canChangePage(pagingDirection)) {
                if (pageChanges > 16)
                    break;

                pager.changePage(pagingDirection);
                pageChanges++;
            }
            return true;
        } catch (Exception e) {
            LogUtils.getLogger().error("Cannot force page: " + e);
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputUtil.GLFW_KEY_TAB)
            return super.keyPressed(keyCode, scanCode, modifiers);

        for (Page page : pages) {
            ClickableWidget widget = page.noteWidget.map(box -> box, block -> block);
            if (widget.isFocused()) {
                if (keyCode == InputUtil.GLFW_KEY_ESCAPE) {
                    this.setFocused(null);
                    return true;
                }

                return widget.keyPressed(keyCode, scanCode, modifiers);
            }
        }

        if (isInAddingMode() && (client.options.inventoryKey.matchesKey(keyCode, scanCode)
                || keyCode == InputUtil.GLFW_KEY_ESCAPE)) {
            sendButtonClick(AlbumMenu.CANCEL_ADDING_PHOTO_BUTTON);
            return true;
        }

        return pager.handleKeyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        for (Page page : pages) {
            if (page.noteWidget.map(box -> box, block -> block).isFocused())
                return super.keyReleased(keyCode, scanCode, modifiers);
        }

        return pager.handleKeyReleased(keyCode, scanCode, modifiers) || super.keyReleased(keyCode, scanCode, modifiers);
    }


    // MISC:
    
    protected void inspectPhotograph(ItemStack photograph) {
        if (!(photograph.getItem() instanceof PhotographItem))
            return;

        client.setScreen(new AlbumPhotographScreen(this, List.of(new ItemAndStack<>(photograph))));
        client.getSoundManager()
                .play(PositionedSoundInstance.master(Exposure.SoundEvents.PHOTOGRAPH_RUSTLE.get(),
                        player.getWorld().getRandom().nextFloat() * 0.2f + 1.3f, 0.75f));
    }

    @NotNull
    protected Text getNoteComponent(Side page) {
        return getScreenHandler().getPage(page)
                .map(AlbumPage::getNote)
                .map(n -> n.map(Text::literal, comp -> comp))
                .orElse(Text.empty());
    }

    protected void enterSignMode() {
        if (isInAddingMode())
            sendButtonClick(AlbumMenu.CANCEL_ADDING_PHOTO_BUTTON);

        client.setScreen(new AlbumSigningScreen(this, TEXTURE, 512, 512));
    }

    protected boolean isInAddingMode() {
        return getScreenHandler().isInAddingPhotographMode();
    }
    
    protected void forEachPage(Consumer<Page> pageAction) {
        for (Page page : pages) {
            pageAction.accept(page);
        }
    }

    protected class Page {
        public final Side side;
        public final Rect2i pageArea;
        public final Rect2i photoArea;
        public final Rect2i exposureArea;
        public final Rect2i noteArea;

        public final PhotographSlotButton photographButton;
        public final Either<TextBox, TextBlock> noteWidget;

        private Page(Side side, Rect2i pageArea, Rect2i photoArea, Rect2i exposureArea, Rect2i noteArea,
                     PhotographSlotButton photographButton, Either<TextBox, TextBlock> noteWidget) {
            this.side = side;
            this.pageArea = pageArea;
            this.photoArea = photoArea;
            this.exposureArea = exposureArea;
            this.noteArea = noteArea;
            this.photographButton = photographButton;
            this.noteWidget = noteWidget;
        }

        public boolean isMouseOver(Rect2i area, double mouseX, double mouseY) {
            return isPointWithinBounds(area.getX() - x, area.getY() - y,
                    area.getWidth(), area.getHeight(), mouseX, mouseY);
        }

        public ClickableWidget getNoteWidget() {
            return noteWidget.map(box -> box, block -> block);
        }
    }
}
