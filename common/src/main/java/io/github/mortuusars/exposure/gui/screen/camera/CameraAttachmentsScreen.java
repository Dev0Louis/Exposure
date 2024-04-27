package io.github.mortuusars.exposure.gui.screen.camera;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.infrastructure.FocalRange;
import io.github.mortuusars.exposure.item.CameraItem;
import io.github.mortuusars.exposure.menu.CameraAttachmentsMenu;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class CameraAttachmentsScreen extends HandledScreen<CameraAttachmentsMenu> {
    public static final Identifier TEXTURE = Exposure.id("textures/gui/camera_attachments.png");

    public CameraAttachmentsScreen(CameraAttachmentsMenu menu, PlayerInventory playerInventory, Text title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        this.backgroundHeight = 185;
        playerInventoryTitleY = this.backgroundHeight - 94;
        super.init();
    }

    @Override
    public void render(@NotNull DrawContext guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.drawMouseoverTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(@NotNull DrawContext guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.drawTexture(TEXTURE, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);

        Slot filmSlot = handler.slots.get(CameraItem.FILM_ATTACHMENT.slot());
        if (!filmSlot.hasStack())
            guiGraphics.drawTexture(TEXTURE, x + filmSlot.x - 1, y + filmSlot.y - 1, 238, 0, 18, 18);

        Slot flashSlot = handler.slots.get(CameraItem.FLASH_ATTACHMENT.slot());
        if (!flashSlot.hasStack())
            guiGraphics.drawTexture(TEXTURE, x + flashSlot.x - 1, y + flashSlot.y - 1, 238, 18, 18, 18);
        else
            guiGraphics.drawTexture(TEXTURE, x + 99, y + 7, 0, 185, 24, 28);

        Slot lensSlot = handler.slots.get(CameraItem.LENS_ATTACHMENT.slot());
        boolean hasLens = lensSlot.hasStack();
        if (hasLens)
            guiGraphics.drawTexture(TEXTURE, x + 103, y + 49, 24, 185, 31, 35);
        else
            guiGraphics.drawTexture(TEXTURE, x + lensSlot.x - 1, y + lensSlot.y - 1, 238, 36, 18, 18);

        Slot filterSlot = handler.slots.get(CameraItem.FILTER_ATTACHMENT.slot());
        if (filterSlot.hasStack()) {
            int x = hasLens ? 116 : 106;
            int y = hasLens ? 58 : 53;

            float r = 1f;
            float g = 1f;
            float b = 1f;

            Identifier key = Registries.ITEM.getId(filterSlot.getStack().getItem());
            if (key.getNamespace().equals("minecraft") && key.getPath().contains("_stained_glass_pane")) {
                String colorString = key.getPath().replace("_stained_glass_pane", "");
                DyeColor color = DyeColor.byName(colorString, DyeColor.WHITE);
                int rgb = color.getFireworkColor();
                r = MathHelper.clamp(((rgb >> 16) & 0xFF) / 255f, 0f, 1f);
                g = MathHelper.clamp((((rgb >> 8) & 0xFF) / 255f), 0f, 1f);
                b = MathHelper.clamp((rgb & 0xFF) / 255f, 0f, 1f);
            }

            RenderSystem.setShaderColor(r, g, b, 1f);

            if (!filterSlot.getStack().isOf(Items.GLASS_PANE))
                guiGraphics.drawTexture(TEXTURE, x + x, y + y, 55, 185, 15, 23); // Opaque part

            guiGraphics.drawTexture(TEXTURE, x + x, y + y, 70, 185, 15, 23); // Glares
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            guiGraphics.drawTexture(TEXTURE, x + filterSlot.x - 1, y + filterSlot.y - 1, 238, 54, 18, 18);
        }

        RenderSystem.disableBlend();
    }

    @Override
    protected @NotNull List<Text> getTooltipFromItem(ItemStack stack) {
        List<Text> tooltip = super.getTooltipFromItem(stack);
        if (stack.isIn(Exposure.Tags.Items.LENSES) && focusedSlot != null && focusedSlot.getStack().equals(stack)) {
            tooltip.add(Text.translatable("gui.exposure.viewfinder.focal_length", FocalRange.ofStack(stack).asString())
                    .formatted(Formatting.GOLD));
        }
        return tooltip;
    }
}
