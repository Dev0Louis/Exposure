package io.github.mortuusars.exposure.gui.screen;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Either;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.camera.infrastructure.FrameData;
import io.github.mortuusars.exposure.data.storage.ExposureExporter;
import io.github.mortuusars.exposure.gui.screen.element.Pager;
import io.github.mortuusars.exposure.item.PhotographItem;
import io.github.mortuusars.exposure.render.PhotographRenderProperties;
import io.github.mortuusars.exposure.render.PhotographRenderer;
import io.github.mortuusars.exposure.util.ClientsideWorldNameGetter;
import io.github.mortuusars.exposure.util.ItemAndStack;
import io.github.mortuusars.exposure.util.PagingDirection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.InputUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class PhotographScreen extends ZoomableScreen {
    public static final Identifier WIDGETS_TEXTURE = Exposure.resource("textures/gui/widgets.png");

    private final List<ItemAndStack<PhotographItem>> photographs;
    private final List<String> savedExposures = new ArrayList<>();

    private final Pager pager = new Pager(Exposure.SoundEvents.CAMERA_LENS_RING_CLICK.get());

    public PhotographScreen(List<ItemAndStack<PhotographItem>> photographs) {
        super(Text.empty());
        Preconditions.checkState(!photographs.isEmpty(), "No photographs to display.");
        this.photographs = photographs;

        // Query all photographs:
        for (ItemAndStack<PhotographItem> photograph : photographs) {
            @Nullable Either<String, Identifier> idOrTexture = photograph.getItem()
                    .getIdOrTexture(photograph.getStack());
            if (idOrTexture != null)
                idOrTexture.ifLeft(id -> ExposureClient.getExposureStorage().getOrQuery(id));
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        zoomFactor = (float) height / ExposureClient.getExposureRenderer().getSize();

        TexturedButtonWidget previousButton = new TexturedButtonWidget(0, (int) (height / 2f - 16 / 2f), 16, 16,
                0, 0, 16, WIDGETS_TEXTURE, 256, 256,
                button -> pager.changePage(PagingDirection.PREVIOUS), Text.translatable("gui.exposure.previous_page"));
        addDrawableChild(previousButton);

        TexturedButtonWidget nextButton = new TexturedButtonWidget(width - 16, (int) (height / 2f - 16 / 2f), 16, 16,
                16, 0, 16, WIDGETS_TEXTURE, 256, 256,
                button -> pager.changePage(PagingDirection.NEXT), Text.translatable("gui.exposure.next_page"));
        addDrawableChild(nextButton);

        pager.init(photographs.size(), true, previousButton, nextButton);
    }

    @Override
    public void render(@NotNull DrawContext guiGraphics, int mouseX, int mouseY, float partialTick) {
        pager.update();

        renderBackground(guiGraphics);

        guiGraphics.getMatrices().push();
        guiGraphics.getMatrices().translate(0, 0, 500); // Otherwise exposure will overlap buttons
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.getMatrices().pop();

        guiGraphics.getMatrices().push();

        guiGraphics.getMatrices().translate(x, y, 0);
        guiGraphics.getMatrices().translate(width / 2f, height / 2f, 0);
        guiGraphics.getMatrices().scale(scale, scale, scale);
        guiGraphics.getMatrices().translate(ExposureClient.getExposureRenderer().getSize() / -2f, ExposureClient.getExposureRenderer().getSize() / -2f, 0);

        VertexConsumerProvider.Immediate bufferSource = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());

        ArrayList<ItemAndStack<PhotographItem>> photos = new ArrayList<>(photographs);
        Collections.rotate(photos, -pager.getCurrentPage());
        PhotographRenderer.renderStackedPhotographs(photos, guiGraphics.getMatrices(), bufferSource, LightmapTextureManager.MAX_LIGHT_COORDINATE, 255, 255, 255, 255);

        bufferSource.draw();

        guiGraphics.getMatrices().pop();

        ItemAndStack<PhotographItem> photograph = photographs.get(pager.getCurrentPage());

        Either<String, Identifier> idOrTexture = photograph.getItem().getIdOrTexture(photograph.getStack());
        if (client.player != null && client.player.isCreative() && idOrTexture != null) {
            guiGraphics.drawTextWithShadow(textRenderer, "?", width - textRenderer.getWidth("?") - 10, 10, 0xFFFFFFFF);

            if (mouseX > width - 20 && mouseX < width && mouseY < 20) {
                List<Text> lines = new ArrayList<>();

                String exposureName = idOrTexture.map(id -> id, Identifier::toString);
                lines.add(Text.literal(exposureName));

                lines.add(Text.translatable("gui.exposure.photograph_screen.drop_as_item_tooltip", Text.literal("CTRL + I")));

                lines.add(idOrTexture.map(
                        id -> Text.translatable("gui.exposure.photograph_screen.copy_id_tooltip", "CTRL + C"),
                        texture -> Text.translatable("gui.exposure.photograph_screen.copy_texture_path_tooltip", "CTRL + C")));

                guiGraphics.drawTooltip(textRenderer, lines, Optional.empty(), mouseX, mouseY + 20);
            }
        }

        if (Config.Client.SAVE_EXPOSURE_TO_FILE_WHEN_VIEWED.get())
            trySaveToFile(photograph);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (Screen.hasControlDown() && player != null && player.isCreative()) {
            ItemAndStack<PhotographItem> photograph = photographs.get(pager.getCurrentPage());

            if (keyCode == InputUtil.GLFW_KEY_C) {
                @Nullable Either<String, Identifier> idOrTexture = photograph.getItem().getIdOrTexture(photograph.getStack());
                if (idOrTexture != null) {
                    String text = idOrTexture.map(id -> id, Identifier::toString);
                    MinecraftClient.getInstance().keyboard.setClipboard(text);
                    player.sendMessage(Text.translatable("gui.exposure.photograph_screen.copied_message", text), false);
                }
                return true;
            }

            if (keyCode == InputUtil.GLFW_KEY_I) {
                if (MinecraftClient.getInstance().interactionManager != null) {
                    MinecraftClient.getInstance().interactionManager.dropCreativeStack(photograph.getStack().copy());
                    player.sendMessage(Text.translatable("gui.exposure.photograph_screen.item_dropped_message",
                            photograph.getStack().toString()), false);
                }
                return true;
            }
        }

        return pager.handleKeyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return pager.handleKeyReleased(keyCode, scanCode, modifiers) || super.keyReleased(keyCode, scanCode, modifiers);
    }

    private void trySaveToFile(ItemAndStack<PhotographItem> photograph) {
        if (MinecraftClient.getInstance().player == null || photograph.getStack().getNbt() == null)
            return;

        NbtCompound tag = photograph.getStack().getNbt();
        if (tag == null
                || MinecraftClient.getInstance().player == null
                || !tag.contains(FrameData.PHOTOGRAPHER_ID, NbtElement.INT_ARRAY_TYPE)
                || !tag.getUuid(FrameData.PHOTOGRAPHER_ID).equals(MinecraftClient.getInstance().player.getUuid())) {
            return;
        }

        @Nullable Either<String, Identifier> idOrTexture = photograph.getItem().getIdOrTexture(photograph.getStack());
        if (idOrTexture == null)
            return;

        idOrTexture.ifLeft(id -> {
            PhotographRenderProperties properties = PhotographRenderProperties.get(photograph.getStack());
            String filename = properties != PhotographRenderProperties.DEFAULT ? id + "_" + properties.getId() : id;

            if (savedExposures.contains(filename))
                return;

            ExposureClient.getExposureStorage().getOrQuery(id).ifPresent(exposure -> {
                savedExposures.add(filename);

                new Thread(() -> new ExposureExporter(filename)
                        .withDefaultFolder()
                        .organizeByWorld(Config.Client.EXPOSURE_SAVING_LEVEL_SUBFOLDER.get(), ClientsideWorldNameGetter::getWorldName)
                        .withModifier(properties.getModifier())
                        .withSize(Config.Client.EXPOSURE_SAVING_SIZE.get())
                        .save(exposure), "ExposureSaving").start();
            });
        });
    }
}
