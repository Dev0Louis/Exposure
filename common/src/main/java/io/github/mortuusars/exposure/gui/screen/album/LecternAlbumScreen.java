package io.github.mortuusars.exposure.gui.screen.album;

import io.github.mortuusars.exposure.gui.screen.element.textbox.TextBox;
import io.github.mortuusars.exposure.menu.AlbumMenu;
import io.github.mortuusars.exposure.menu.LecternAlbumMenu;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class LecternAlbumScreen extends AlbumScreen {
    private final LecternAlbumMenu menu;

    private final ScreenHandlerListener listener = new ScreenHandlerListener() {
        public void onSlotUpdate(ScreenHandler containerToSend, int dataSlotIndex, ItemStack stack) { }

        public void onPropertyUpdate(ScreenHandler containerMenu, int dataSlotIndex, int value) {
            if (dataSlotIndex == 1) {
                getScreenHandler().setCurrentSpreadIndex(value);
                pager.setPage(value);
                for (Page page : pages) {
                    page.noteWidget
                            .ifLeft(TextBox::setCursorToEnd)
                            .ifRight(textBlock -> textBlock.setMessage(getNoteComponent(page.side)));
                }
            }
        }
    };

    public LecternAlbumScreen(AlbumMenu menu, PlayerInventory playerInventory, Text title) {
        super(menu, playerInventory, title);
        this.menu = (LecternAlbumMenu) menu;
        menu.addListener(listener);
    }

    @Override
    protected void init() {
        super.init();

        if (player.canModifyBlocks()) {
            addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, b -> this.close())
                    .dimensions(this.width / 2 - 100, y + 196, 98, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.translatable("lectern.take_book"), b -> sendButtonClick(LecternAlbumMenu.TAKE_BOOK_BUTTON))
                    .dimensions(this.width / 2 + 2, y + 196, 98, 20).build());
        }
    }

    public void removed() {
        super.removed();
        this.menu.removeListener(this.listener);
    }
}
