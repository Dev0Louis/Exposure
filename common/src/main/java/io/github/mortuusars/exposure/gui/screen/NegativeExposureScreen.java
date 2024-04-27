package io.github.mortuusars.exposure.gui.screen;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Either;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import io.github.mortuusars.exposure.gui.screen.element.Pager;
import io.github.mortuusars.exposure.render.ExposureImage;
import io.github.mortuusars.exposure.render.ExposureTexture;
import io.github.mortuusars.exposure.data.storage.ExposureSavedData;
import io.github.mortuusars.exposure.render.modifiers.ExposurePixelModifiers;
import io.github.mortuusars.exposure.util.GuiUtil;
import io.github.mortuusars.exposure.util.PagingDirection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class NegativeExposureScreen extends ZoomableScreen {
    public static final Identifier TEXTURE = Exposure.id("textures/gui/film_frame_inspect.png");
    public static final int BG_SIZE = 78;
    public static final int FRAME_SIZE = 54;

    private final Pager pager = new Pager(Exposure.SoundEvents.CAMERA_LENS_RING_CLICK.get());
    private final List<Either<String, Identifier>> exposures;

    public NegativeExposureScreen(List<Either<String, Identifier>> exposures) {
        super(Text.empty());
        this.exposures = exposures;
        Preconditions.checkArgument(exposures != null && !exposures.isEmpty());

        zoom.step = 2f;
        zoom.defaultZoom = 1f;
        zoom.targetZoom = 1f;
        zoom.minZoom = zoom.defaultZoom / (float)Math.pow(zoom.step, 1f);
        zoom.maxZoom = zoom.defaultZoom * (float)Math.pow(zoom.step, 5f);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        zoomFactor = 1f / (client.options.getGuiScale().getValue() + 1);

        TexturedButtonWidget previousButton = new TexturedButtonWidget(0, (int) (height / 2f - 16 / 2f), 16, 16, PhotographScreen.WIDGETS_TEXTURE, button -> pager.changePage(PagingDirection.PREVIOUS), Text.translatable("gui.exposure.previous_page"));

        addDrawableChild(previousButton);

        TexturedButtonWidget nextButton = new TexturedButtonWidget(width - 16, (int) (height / 2f - 16 / 2f), 16, 16, PhotographScreen.WIDGETS_TEXTURE, button -> pager.changePage(PagingDirection.NEXT), Text.translatable("gui.exposure.next_page"));
        addDrawableChild(nextButton);

        pager.init(exposures.size(), true, previousButton, nextButton);
    }

    @Override
    public void render(@NotNull DrawContext guiGraphics, int mouseX, int mouseY, float delta) {
        pager.update();

        renderBackground(guiGraphics, mouseX, mouseY, delta);

        super.render(guiGraphics, mouseX, mouseY, delta);

        Either<String, Identifier> idOrTexture = exposures.get(pager.getCurrentPage());

        @Nullable FilmType type = idOrTexture.map(
                id -> ExposureClient.getExposureStorage().getOrQuery(id).map(ExposureSavedData::getType)
                        .orElse(FilmType.BLACK_AND_WHITE),
                texture -> (texture.getPath().endsWith("_black_and_white") || texture.getPath()
                        .endsWith("_bw")) ? FilmType.COLOR : FilmType.BLACK_AND_WHITE);
        if (type == null)
            type = FilmType.BLACK_AND_WHITE;

        @Nullable ExposureImage exposure = idOrTexture.map(
                id -> ExposureClient.getExposureStorage().getOrQuery(id).map(data -> new ExposureImage(id, data)).orElse(null),
                texture -> {
                    @Nullable ExposureTexture exposureTexture = ExposureTexture.getTexture(texture);
                    if (exposureTexture != null)
                        return new ExposureImage(texture.toString(), exposureTexture);
                    else
                        return null;
                }
        );

        if (exposure == null)
            return;

        int width = exposure.getWidth();
        int height = exposure.getHeight();

        guiGraphics.getMatrices().push();
        guiGraphics.getMatrices().translate(x + this.width / 2f, y + this.height / 2f, 0);
        guiGraphics.getMatrices().scale(scale, scale, scale);
        guiGraphics.getMatrices().translate(-width / 2f, -height / 2f, 0);

        {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderTexture(0, TEXTURE);

            guiGraphics.getMatrices().push();
            float scale = Math.max((float) width / (FRAME_SIZE), (float) height / (FRAME_SIZE));
            guiGraphics.getMatrices().scale(scale, scale, scale);
            guiGraphics.getMatrices().translate(-12, -12, 0);

            GuiUtil.blit(guiGraphics.getMatrices(), 0, 0, BG_SIZE, BG_SIZE, 0, 0, 256, 256, 0);

            RenderSystem.setShaderColor(type.filmR, type.filmG, type.filmB, type.filmA);
            GuiUtil.blit(guiGraphics.getMatrices(), 0, 0, BG_SIZE, BG_SIZE, 0, BG_SIZE, 256, 256, 0);

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            guiGraphics.getMatrices().pop();
        }

        VertexConsumerProvider.Immediate bufferSource = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
        ExposureClient.getExposureRenderer().render(idOrTexture, ExposurePixelModifiers.NEGATIVE_FILM, guiGraphics.getMatrices(), bufferSource,
                0, 0, width, height, 0, 0, 1, 1, LightmapTextureManager.MAX_LIGHT_COORDINATE,
                type.frameR, type.frameG, type.frameB, 255);
        bufferSource.draw();

        guiGraphics.getMatrices().pop();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return pager.handleKeyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return pager.handleKeyReleased(keyCode, scanCode, modifiers) || super.keyReleased(keyCode, scanCode, modifiers);
    }
}
