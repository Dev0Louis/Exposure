package io.github.mortuusars.exposure.gui.screen.camera.button;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.infrastructure.CompositionGuide;
import io.github.mortuusars.exposure.camera.infrastructure.CompositionGuides;
import io.github.mortuusars.exposure.camera.infrastructure.SynchronizedCameraInHandActions;
import io.github.mortuusars.exposure.util.CameraInHand;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class CompositionGuideButton extends CycleButton {
    private final List<CompositionGuide> guides;

    public CompositionGuideButton(Screen screen, int x, int y, int width, int height, int u, int v,  Identifier texture) {
        super(screen, x, y, width, height, u, v, height, texture);
        guides = CompositionGuides.getGuides();

        CameraInHand camera = CameraInHand.getActive(MinecraftClient.getInstance().player);
        Preconditions.checkState(!camera.isEmpty(), "Player must hold an active camera at this point.");
        CompositionGuide guide = camera.getItem().getCompositionGuide(camera.getStack());

        int currentGuideIndex = 0;

        for (int i = 0; i < guides.size(); i++) {
            if (guides.get(i).getId().equals(guide.getId())) {
                currentGuideIndex = i;
                break;
            }
        }

        setupButtonElements(guides.size(), currentGuideIndex);
    }

    @Override
    public void playDownSound(SoundManager handler) {
        handler.play(PositionedSoundInstance.master(Exposure.SoundEvents.CAMERA_BUTTON_CLICK.get(),
                Objects.requireNonNull(MinecraftClient.getInstance().world).random.nextFloat() * 0.15f + 0.93f, 0.7f));
    }

    @Override
    public void renderWidget(@NotNull DrawContext guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);

        // Icon
        guiGraphics.drawTexture(Exposure.id("textures/gui/viewfinder/icon/composition_guide/" + guides.get(currentIndex).getId() + ".png"),
                getX(), getY() + 4, 0, 0, 0, 15, 14, 15, 14);
    }

    @Override
    public void renderToolTip(@NotNull DrawContext guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawTooltip(MinecraftClient.getInstance().textRenderer, List.of(Text.translatable("gui.exposure.viewfinder.composition_guide.tooltip"),
                ((MutableText) getMessage()).formatted(Formatting.GRAY)), Optional.empty(), mouseX, mouseY);
    }

    @Override
    public @NotNull Text getMessage() {
        return guides.get(currentIndex).translate();
    }

    @Override
    protected void onCycle() {
        SynchronizedCameraInHandActions.setCompositionGuide(guides.get(currentIndex));
    }
}
