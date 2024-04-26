package io.github.mortuusars.exposure.gui.screen.album;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.gui.screen.PhotographScreen;
import io.github.mortuusars.exposure.item.PhotographItem;
import io.github.mortuusars.exposure.util.ItemAndStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;

public class AlbumPhotographScreen extends PhotographScreen {
    private final Screen parentScreen;

    public AlbumPhotographScreen(Screen parentScreen, List<ItemAndStack<PhotographItem>> photographs) {
        super(photographs);
        this.parentScreen = parentScreen;
    }

    @Override
    public void render(@NotNull DrawContext guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (zoom.targetZoom == zoom.minZoom) {
            close();
            return;
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputUtil.GLFW_KEY_ESCAPE || client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
            zoom.set(0f);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (handled)
            return true;

        if (button == 1) { // Right Click
            zoom.set(0f);
            return true;
        }

        return false;
    }

    public void close() {
        MinecraftClient.getInstance().setScreen(parentScreen);
        if (client.player != null)
            client.player.playSound(Exposure.SoundEvents.PHOTOGRAPH_PLACE.get(), 0.7f, 1.1f);
    }
}
