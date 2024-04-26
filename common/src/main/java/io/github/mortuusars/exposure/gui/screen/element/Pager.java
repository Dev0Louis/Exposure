package io.github.mortuusars.exposure.gui.screen.element;

import io.github.mortuusars.exposure.util.PagingDirection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

@SuppressWarnings({"UnusedReturnValue", "BooleanMethodIsAlwaysInverted"})
public class Pager {
    public long lastChangedAt;
    public int changeCooldownMS = 50;
    public boolean playSound = true;
    public SoundEvent changeSoundEvent;

    protected Identifier texture;
    protected int pages;
    protected boolean cycled;
    protected int currentPage;
    protected PressableWidget previousButton;
    protected PressableWidget nextButton;

    public Pager(SoundEvent changeSoundEvent) {
        this.changeSoundEvent = changeSoundEvent;
    }

    public void init(int pages, boolean cycled, PressableWidget previousPageButton, PressableWidget nextPageButton) {
        this.pages = pages;
        this.cycled = cycled;
        setPage(getCurrentPage());

        this.previousButton = previousPageButton;
        this.nextButton = nextPageButton;
        update();
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setPage(int value) {
        this.currentPage = MathHelper.clamp(value, 0, this.pages);
    }

    public void update() {
        previousButton.visible = canChangePage(PagingDirection.PREVIOUS);
        nextButton.visible = canChangePage(PagingDirection.NEXT);
    }

    public boolean handleKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (MinecraftClient.getInstance().options.leftKey.matchesKey(keyCode, scanCode) || keyCode == InputUtil.GLFW_KEY_LEFT) {
            if (!isOnCooldown())
                changePage(PagingDirection.PREVIOUS);
            return true;
        }
        else if (MinecraftClient.getInstance().options.rightKey.matchesKey(keyCode, scanCode) || keyCode == InputUtil.GLFW_KEY_RIGHT) {
            if (!isOnCooldown())
                changePage(PagingDirection.NEXT);
            return true;
        }
        else
            return false;
    }

    public boolean handleKeyReleased(int keyCode, int scanCode, int modifiers) {
        if (MinecraftClient.getInstance().options.rightKey.matchesKey(keyCode, scanCode) || keyCode == InputUtil.GLFW_KEY_RIGHT
                || MinecraftClient.getInstance().options.leftKey.matchesKey(keyCode, scanCode) || keyCode == InputUtil.GLFW_KEY_LEFT) {
            lastChangedAt = 0;
            return true;
        }

        return false;
    }

    private boolean isOnCooldown() {
        return Util.getMeasuringTimeMs() - lastChangedAt < changeCooldownMS;
    }

    public boolean canChangePage(PagingDirection direction) {
        int newIndex = getCurrentPage() + direction.getValue();
        return pages > 1 && (cycled || (0 <= newIndex && newIndex < pages));
    }

    public boolean changePage(PagingDirection direction) {
        if (!canChangePage(direction))
            return false;

        int oldPage = currentPage;
        int newPage = getCurrentPage() + direction.getValue();

        if (cycled && newPage >= pages)
            newPage = 0;
        else if (cycled && newPage < 0)
            newPage = pages - 1;

        if (oldPage == newPage)
            return false;

        setPage(newPage);
        onPageChanged(direction, oldPage, currentPage);
        return true;
    }

    public void onPageChanged(PagingDirection pagingDirection, int prevPage, int currentPage) {
        lastChangedAt = Util.getMeasuringTimeMs();
        if (playSound)
            playChangeSound();
    }

    protected void playChangeSound() {
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(
                getChangeSound(), 0.8f, 1f));
    }

    protected SoundEvent getChangeSound() {
        return changeSoundEvent;
    }
}
