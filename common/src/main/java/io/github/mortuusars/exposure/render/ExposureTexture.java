package io.github.mortuusars.exposure.render;

import com.mojang.logging.LogUtils;
import net.minecraft.client.MinecraftClient;

import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.Executor;

public class ExposureTexture extends ResourceTexture {
    @Nullable
    private NativeImage image;

    public ExposureTexture(Identifier location) {
        super(location);
    }

    public static @Nullable ExposureTexture getTexture(Identifier location) {
        TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();

        @Nullable AbstractTexture existingTexture = textureManager.textures.get(location);
        if (existingTexture != null) {
            return existingTexture instanceof ExposureTexture exposureTexture ? exposureTexture : null;
        }

        try {
            ExposureTexture texture = new ExposureTexture(location);
            textureManager.registerTexture(location, texture);
            return texture;
        }
        catch (Exception e) {
            LogUtils.getLogger().error("Cannot load texture [" + location + "]. " + e);
            return null;
        }
    }

    public @Nullable NativeImage getImage() {
        if (this.image != null)
            return image;

        try {
            NativeImage image = super.loadTextureData(MinecraftClient.getInstance().getResourceManager()).getImage();
            this.image = image;
            return image;
        } catch (IOException e) {
            LogUtils.getLogger().error("Cannot load texture: " + e);
            return null;
        }
    }

    @Override
    public void registerTexture(@NotNull TextureManager pTextureManager, @NotNull ResourceManager pResourceManager, @NotNull Identifier pPath, @NotNull Executor pExecutor) {
        super.registerTexture(pTextureManager, pResourceManager, pPath, pExecutor);
        image = null;
    }

    @Override
    public void close() {
        super.close();

        if (this.image != null) {
            image.close();
            image = null;
        }
    }
}
