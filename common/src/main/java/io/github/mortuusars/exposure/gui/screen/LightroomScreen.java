package io.github.mortuusars.exposure.gui.screen;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Either;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.block.entity.Lightroom;
import io.github.mortuusars.exposure.block.entity.LightroomBlockEntity;
import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import io.github.mortuusars.exposure.camera.infrastructure.FrameData;
import io.github.mortuusars.exposure.item.DevelopedFilmItem;
import io.github.mortuusars.exposure.menu.LightroomMenu;
import io.github.mortuusars.exposure.render.modifiers.ExposurePixelModifiers;
import io.github.mortuusars.exposure.util.ColorChannel;
import io.github.mortuusars.exposure.util.PagingDirection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class LightroomScreen extends HandledScreen<LightroomMenu> {
    public static final Identifier MAIN_TEXTURE = Exposure.id("textures/gui/lightroom.png");
    public static final Identifier FILM_OVERLAYS_TEXTURE = Exposure.id("textures/gui/lightroom_film_overlays.png");
    public static final int FRAME_SIZE = 54;

    protected PlayerEntity player;
    protected ButtonWidget printButton;
    protected ChromaticProcessToggleButton processToggleButton;

    public LightroomScreen(LightroomMenu menu, PlayerInventory playerInventory, Text title) {
        super(menu, playerInventory, title);
        this.player = playerInventory.player;
    }

    @Override
    protected void init() {
        backgroundWidth = 176;
        backgroundHeight = 210;
        super.init();
        playerInventoryTitleY = 116;

        printButton = new TexturedButtonWidget(x + 117, y + 89, 22, 22, 176, 17,
                22, MAIN_TEXTURE, 256, 256, this::onPrintButtonPressed,
                Text.translatable("gui.exposure.lightroom.print"));

        MutableText tooltip = Text.translatable("gui.exposure.lightroom.print");
        if (player.isCreative()) {
            tooltip.append("\n")
                    .append(Text.translatable("gui.exposure.lightroom.print.creative_tooltip"));
        }

        printButton.setTooltip(Tooltip.of(tooltip));
        addDrawableChild(printButton);

        processToggleButton = new ChromaticProcessToggleButton(x - 19, y + 91,
                this::onProcessToggleButtonPressed, () -> getScreenHandler().getBlockEntity().getProcess());
        processToggleButton.setTooltip(Tooltip.of(Text.translatable("gui.exposure.lightroom.current_frame")));
        addDrawableChild(processToggleButton);

        updateButtons();
    }

    protected void onPrintButtonPressed(ButtonWidget button) {
        if (MinecraftClient.getInstance().interactionManager != null) {
            if (Screen.hasShiftDown() && player.isCreative())
                MinecraftClient.getInstance().interactionManager.clickButton(getScreenHandler().syncId, LightroomMenu.PRINT_CREATIVE_BUTTON_ID);
            else
                MinecraftClient.getInstance().interactionManager.clickButton(getScreenHandler().syncId, LightroomMenu.PRINT_BUTTON_ID);
        }
    }

    protected void onProcessToggleButtonPressed(ButtonWidget button) {
        if (MinecraftClient.getInstance().interactionManager != null)
            MinecraftClient.getInstance().interactionManager.clickButton(getScreenHandler().syncId, LightroomMenu.TOGGLE_PROCESS_BUTTON_ID);
    }

    @Override
    public void render(@NotNull DrawContext guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateButtons();

        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        drawMouseoverTooltip(guiGraphics, mouseX, mouseY);
    }

    protected void updateButtons() {
        printButton.active = getScreenHandler().getBlockEntity().canPrint() || (player.isCreative() && Screen.hasShiftDown() && getScreenHandler().getBlockEntity().canPrintInCreativeMode());
        printButton.visible = !getScreenHandler().isPrinting();

        processToggleButton.active = true;
        processToggleButton.visible = getScreenHandler().getExposedFrames().getCompound(
                getScreenHandler().getSelectedFrame()).getBoolean(FrameData.CHROMATIC);
    }

    @Override
    protected void drawBackground(@NotNull DrawContext guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.drawTexture(MAIN_TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight);
        guiGraphics.drawTexture(MAIN_TEXTURE, x - 27, y + 34, 0, 208, 28, 31);

        // PLACEHOLDER ICONS
        if (!getScreenHandler().slots.get(Lightroom.FILM_SLOT).hasStack())
            guiGraphics.drawTexture(MAIN_TEXTURE, x - 21, y + 41, 238, 0, 18, 18);
        if (!getScreenHandler().slots.get(Lightroom.PAPER_SLOT).hasStack())
            guiGraphics.drawTexture(MAIN_TEXTURE, x + 7, y + 91, 238, 18, 18, 18);
        if (!getScreenHandler().slots.get(Lightroom.CYAN_SLOT).hasStack())
            guiGraphics.drawTexture(MAIN_TEXTURE, x + 41, y + 91, 238, 36, 18, 18);
        if (!getScreenHandler().slots.get(Lightroom.MAGENTA_SLOT).hasStack())
            guiGraphics.drawTexture(MAIN_TEXTURE, x + 59, y + 91, 238, 54, 18, 18);
        if (!getScreenHandler().slots.get(Lightroom.YELLOW_SLOT).hasStack())
            guiGraphics.drawTexture(MAIN_TEXTURE, x + 77, y + 91, 238, 72, 18, 18);
        if (!getScreenHandler().slots.get(Lightroom.BLACK_SLOT).hasStack())
            guiGraphics.drawTexture(MAIN_TEXTURE, x + 95, y + 91, 238, 90, 18, 18);

        if (getScreenHandler().isPrinting()) {
            int progress = getScreenHandler().getData().get(LightroomBlockEntity.CONTAINER_DATA_PROGRESS_ID);
            int time = getScreenHandler().getData().get(LightroomBlockEntity.CONTAINER_DATA_PRINT_TIME_ID);
            int width = progress != 0 && time != 0 ? progress * 24 / time : 0;
            guiGraphics.drawTexture(MAIN_TEXTURE, x + 116, y + 91, 176, 0, width, 17);
        }

        NbtList frames = getScreenHandler().getExposedFrames();
        if (frames.isEmpty()) {
            guiGraphics.drawTexture(FILM_OVERLAYS_TEXTURE, x + 4, y + 15, 0, 136, 168, 68);
            return;
        }

        ItemStack filmStack = getScreenHandler().getSlot(Lightroom.FILM_SLOT).getStack();
        if (!(filmStack.getItem() instanceof DevelopedFilmItem film))
            return;

        FilmType negative = film.getType();

        int selectedFrame = getScreenHandler().getSelectedFrame();
        @Nullable NbtCompound leftFrame = getScreenHandler().getFrameIdByIndex(selectedFrame - 1);
        @Nullable NbtCompound centerFrame = getScreenHandler().getFrameIdByIndex(selectedFrame);
        @Nullable NbtCompound rightFrame = getScreenHandler().getFrameIdByIndex(selectedFrame + 1);

        RenderSystem.setShaderColor(negative.filmR, negative.filmG, negative.filmB, negative.filmA);

        // Left film part
        guiGraphics.drawTexture(FILM_OVERLAYS_TEXTURE, x + 1, y + 15, 0, leftFrame != null ? 68 : 0, 54, 68);
        // Center film part
        guiGraphics.drawTexture(FILM_OVERLAYS_TEXTURE, x + 55, y + 15, 55, rightFrame != null ? 0 : 68, 64, 68);
        // Right film part
        if (rightFrame != null) {
            boolean hasMoreFrames = selectedFrame + 2 < frames.size();
            guiGraphics.drawTexture(FILM_OVERLAYS_TEXTURE, x + 119, y + 15, 120, hasMoreFrames ? 68 : 0, 56, 68);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        MatrixStack poseStack = guiGraphics.getMatrices();

        if (leftFrame != null)
            renderFrame(leftFrame, poseStack, x + 6, y + 22, FRAME_SIZE, isOverLeftFrame(mouseX, mouseY) ? 0.8f : 0.25f, negative);
        if (centerFrame != null)
            renderFrame(centerFrame, poseStack, x + 61, y + 22, FRAME_SIZE, 0.9f, negative);
        if (rightFrame != null)
            renderFrame(rightFrame, poseStack, x + 116, y + 22, FRAME_SIZE, isOverRightFrame(mouseX, mouseY) ? 0.8f : 0.25f, negative);

        RenderSystem.setShaderColor(negative.filmR, negative.filmG, negative.filmB, negative.filmA);

        if (getScreenHandler().getBlockEntity().isAdvancingFrameOnPrint()) {
            poseStack.push();
            poseStack.translate(0, 0, 800);

            if (selectedFrame < getScreenHandler().getTotalFrames() - 1)
                guiGraphics.drawTexture(MAIN_TEXTURE, x + 111, y + 44, 200, 0, 10, 10);
            else
                guiGraphics.drawTexture(MAIN_TEXTURE, x + 111, y + 44, 210, 0, 10, 10);

            poseStack.pop();
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    protected void drawMouseoverTooltip(@NotNull DrawContext guiGraphics, int mouseX, int mouseY) {
        super.drawMouseoverTooltip(guiGraphics, mouseX, mouseY);

        boolean advancedTooltips = MinecraftClient.getInstance().options.advancedItemTooltips;
        int selectedFrame = getScreenHandler().getSelectedFrame();
        List<Text> tooltipLines = new ArrayList<>();

        int hoveredFrameIndex = -1;

        if (isOverLeftFrame(mouseX, mouseY)) {
            hoveredFrameIndex = selectedFrame - 1;
            tooltipLines.add(Text.translatable("gui.exposure.lightroom.previous_frame"));
        } else if (isOverCenterFrame(mouseX, mouseY)) {
            hoveredFrameIndex = selectedFrame;
            tooltipLines.add(Text.translatable("gui.exposure.lightroom.current_frame", Integer.toString(getScreenHandler().getSelectedFrame() + 1)));
        } else if (isOverRightFrame(mouseX, mouseY)) {
            hoveredFrameIndex = selectedFrame + 1;
            tooltipLines.add(Text.translatable("gui.exposure.lightroom.next_frame"));
        }

        if (hoveredFrameIndex != -1)
            addFrameInfoTooltipLines(tooltipLines, hoveredFrameIndex, advancedTooltips);

        guiGraphics.drawTooltip(MinecraftClient.getInstance().textRenderer, tooltipLines, Optional.empty(), mouseX, mouseY);
    }

    private void addFrameInfoTooltipLines(List<Text> tooltipLines, int frameIndex, boolean isAdvancedTooltips) {
        @Nullable NbtCompound frame = getScreenHandler().getFrameIdByIndex(frameIndex);
        if (frame != null) {
            ColorChannel.fromString(frame.getString(FrameData.CHROMATIC_CHANNEL)).ifPresent(c ->
                    tooltipLines.add(Text.translatable("gui.exposure.channel." + c.asString())
                        .fillStyle(Style.EMPTY.withColor(c.getRepresentationColor()))));

            if (isAdvancedTooltips) {
                Either<String, Identifier> idOrTexture = FrameData.getIdOrTexture(frame);
                MutableText component = idOrTexture.map(
                                id -> !id.isEmpty() ? Text.translatable("gui.exposure.frame.id",
                                        Text.literal(id).formatted(Formatting.GRAY)) : Text.empty(),
                                texture -> Text.translatable("gui.exposure.frame.texture",
                                        Text.literal(texture.toString()).formatted(Formatting.GRAY)))
                        .formatted(Formatting.DARK_GRAY);
                tooltipLines.add(component);
            }
        }
    }

    private boolean isOverLeftFrame(int mouseX, int mouseY) {
        NbtList frames = getScreenHandler().getExposedFrames();
        int selectedFrame = getScreenHandler().getSelectedFrame();
        return selectedFrame - 1 >= 0 && selectedFrame - 1 < frames.size() && isPointWithinBounds(6, 22, FRAME_SIZE, FRAME_SIZE, mouseX, mouseY);
    }

    private boolean isOverCenterFrame(int mouseX, int mouseY) {
        NbtList frames = getScreenHandler().getExposedFrames();
        int selectedFrame = getScreenHandler().getSelectedFrame();
        return selectedFrame >= 0 && selectedFrame < frames.size() && isPointWithinBounds(61, 22, FRAME_SIZE, FRAME_SIZE, mouseX, mouseY);
    }

    private boolean isOverRightFrame(int mouseX, int mouseY) {
        NbtList frames = getScreenHandler().getExposedFrames();
        int selectedFrame = getScreenHandler().getSelectedFrame();
        return selectedFrame + 1 >= 0 && selectedFrame + 1 < frames.size() && isPointWithinBounds(116, 22, FRAME_SIZE, FRAME_SIZE, mouseX, mouseY);
    }

    public void renderFrame(@Nullable NbtCompound frame, MatrixStack poseStack, float x, float y, float size, float alpha, FilmType negative) {
        if (frame == null)
            return;

        poseStack.push();
        poseStack.translate(x, y, 0);

        VertexConsumerProvider.Immediate bufferSource = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
        Either<String, Identifier> idOrTexture = FrameData.getIdOrTexture(frame);
        ExposureClient.getExposureRenderer().render(idOrTexture, ExposurePixelModifiers.NEGATIVE_FILM, poseStack, bufferSource,
                0, 0, size, size, 0, 0, 1, 1, LightmapTextureManager.MAX_LIGHT_COORDINATE,
                negative.frameR, negative.frameG, negative.frameB, MathHelper.clamp((int) Math.ceil(alpha * 255), 0, 255));

        bufferSource.draw();
        poseStack.pop();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Preconditions.checkState(client != null);
        Preconditions.checkState(client.interactionManager != null);

        if (client.options.leftKey.matchesKey(keyCode, scanCode) || keyCode == InputUtil.GLFW_KEY_LEFT) {
            changeFrame(PagingDirection.PREVIOUS);
            return true;
        } else if (client.options.rightKey.matchesKey(keyCode, scanCode) || keyCode == InputUtil.GLFW_KEY_RIGHT) {
            changeFrame(PagingDirection.NEXT);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        boolean handled = super.mouseScrolled(mouseX, mouseY, delta);

        if (!handled) {
            if (delta >= 0.0 && isOverCenterFrame((int) mouseX, (int) mouseY)) // Scroll Up
                enterFrameInspectMode();
        }

        return handled;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Preconditions.checkState(client != null);
            Preconditions.checkState(client.interactionManager != null);

            if (isOverCenterFrame((int) mouseX, (int) mouseY)) {
                enterFrameInspectMode();
                return true;
            }

            if (isOverLeftFrame((int) mouseX, (int) mouseY)) {
                changeFrame(PagingDirection.PREVIOUS);
                return true;
            }

            if (isOverRightFrame((int) mouseX, (int) mouseY)) {
                changeFrame(PagingDirection.NEXT);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void changeFrame(PagingDirection navigation) {
        if ((navigation == PagingDirection.PREVIOUS && getScreenHandler().getSelectedFrame() == 0)
                || (navigation == PagingDirection.NEXT && getScreenHandler().getSelectedFrame() == getScreenHandler().getTotalFrames() - 1))
            return;

        Preconditions.checkState(client != null);
        Preconditions.checkState(client.player != null);
        Preconditions.checkState(client.interactionManager != null);
        int buttonId = navigation == PagingDirection.NEXT ? LightroomMenu.NEXT_FRAME_BUTTON_ID : LightroomMenu.PREVIOUS_FRAME_BUTTON_ID;
        client.interactionManager.clickButton(getScreenHandler().syncId, buttonId);
        player.playSound(Exposure.SoundEvents.CAMERA_LENS_RING_CLICK.get(), 1f, client.player.getWorld()
                .getRandom().nextFloat() * 0.4f + 0.8f);

        // Update block entity clientside to faster update advance frame arrows:
        getScreenHandler().getBlockEntity().setSelectedFrame(getScreenHandler().getBlockEntity().getSelectedFrameIndex() + (navigation == PagingDirection.NEXT ? 1 : -1));
    }

    private void enterFrameInspectMode() {
        MinecraftClient.getInstance().setScreen(new FilmFrameInspectScreen(this, getScreenHandler()));
        player.playSound(Exposure.SoundEvents.CAMERA_LENS_RING_CLICK.get(), 1f, 1.3f);
    }

    @Override
    protected boolean isClickOutsideBounds(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        return super.isClickOutsideBounds(mouseX, mouseY, guiLeft, guiTop, mouseButton)
                && focusedSlot == null;
    }
}
