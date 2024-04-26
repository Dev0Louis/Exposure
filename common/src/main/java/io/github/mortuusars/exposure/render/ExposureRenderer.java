package io.github.mortuusars.exposure.render;

import com.mojang.datafixers.util.Either;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.render.modifiers.IPixelModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class ExposureRenderer implements AutoCloseable {
    private final Map<String, ExposureInstance> cache = new HashMap<>();

    public int getSize() {
        return 256;
    }

    public void render(@NotNull Either<String, Identifier> idOrTexture, IPixelModifier modifier, MatrixStack poseStack, VertexConsumerProvider bufferSource) {
        render(idOrTexture, modifier, poseStack, bufferSource, 0, 0, getSize(), getSize());
    }

    public void render(@NotNull Either<String, Identifier> idOrTexture, IPixelModifier modifier,
                       MatrixStack poseStack, VertexConsumerProvider bufferSource, float x, float y, float width, float height) {
        render(idOrTexture, modifier, poseStack, bufferSource, x, y, x + width, y + height,
                0, 0, 1, 1, LightmapTextureManager.MAX_LIGHT_COORDINATE, 255, 255, 255, 255);
    }

    public void render(@NotNull Either<String, Identifier> idOrTexture, IPixelModifier modifier,
                       MatrixStack poseStack, VertexConsumerProvider bufferSource,
                       int packedLight, int r, int g, int b, int a) {
        render(idOrTexture, modifier, poseStack, bufferSource, 0, 0, getSize(), getSize(), packedLight, r, g, b, a);
    }

    public void render(@NotNull Either<String, Identifier> idOrTexture, IPixelModifier modifier,
                       MatrixStack poseStack, VertexConsumerProvider bufferSource, float x, float y, float width, float height,
                       int packedLight, int r, int g, int b, int a) {
        render(idOrTexture, modifier, poseStack, bufferSource, x, y, x + width, y + height,
                0, 0, 1, 1, packedLight, r, g, b, a);
    }

    public void render(@NotNull Either<String, Identifier> idOrTexture, IPixelModifier modifier,
                       MatrixStack poseStack, VertexConsumerProvider bufferSource, float minX, float minY, float maxX, float maxY,
                       float minU, float minV, float maxU, float maxV, int packedLight, int r, int g, int b, int a) {
        @Nullable ExposureImage exposure = idOrTexture.map(
                id -> ExposureClient.getExposureStorage().getOrQuery(id)
                        .map(data -> new ExposureImage(id, data))
                        .orElse(null),
                texture -> {
                    @Nullable ExposureTexture exposureTexture = ExposureTexture.getTexture(texture);
                    if (exposureTexture != null)
                        return new ExposureImage(texture.toString(), exposureTexture);
                    else
                        return null;
                }
        );

        if (exposure != null) {
            String id = idOrTexture.map(expId -> expId, Identifier::toString);
            getOrCreateExposureInstance(id, exposure, modifier)
                    .draw(poseStack, bufferSource, minX, minY, maxX, maxY, minU, minV, maxU, maxV, packedLight, r, g, b, a);
        }
    }

    private ExposureInstance getOrCreateExposureInstance(String id, ExposureImage exposure, IPixelModifier modifier) {
        String instanceId = id + modifier.getIdSuffix();
        return (this.cache).compute(instanceId, (expId, expData) -> {
            if (expData == null) {
                return new ExposureInstance(expId, exposure, modifier);
            } else {
                expData.replaceData(exposure);
                return expData;
            }
        });
    }

    public void clearData() {
        for (ExposureInstance instance : cache.values()) {
            instance.close();
        }

        cache.clear();
    }

    public void clearDataSingle(@NotNull String exposureId, boolean allVariants) {
        // Using cache.entrySet().removeIf(...) would be simpler, but it wouldn't let us .close() the instance
        for(Iterator<Map.Entry<String, ExposureInstance>> it = cache.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, ExposureInstance> entry = it.next();
            if(allVariants ? entry.getKey().startsWith(exposureId) : entry.getKey().equals(exposureId)) {
                entry.getValue().close();
                it.remove();

                if (!allVariants)
                    break;
            }
        }
    }

    @Override
    public void close() {
        clearData();
    }

    static class ExposureInstance implements AutoCloseable {
        private final RenderLayer renderType;

        private ExposureImage exposure;
        private NativeImageBackedTexture texture;
        private final IPixelModifier pixelModifier;
        private boolean requiresUpload = true;

        ExposureInstance(String id, ExposureImage exposure, IPixelModifier modifier) {
            this.exposure = exposure;
            this.texture = new NativeImageBackedTexture(exposure.getWidth(), exposure.getHeight(), true);
            this.pixelModifier = modifier;
            String textureId = createTextureId(id);
            Identifier resourcelocation = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture(textureId, this.texture);
            this.renderType = RenderLayer.getText(resourcelocation);
        }

        private static String createTextureId(String exposureId) {
            String id = "exposure/" + exposureId.toLowerCase();
            id = id.replace(':', '_');

            // Player nicknames can have non az09 chars
            // we need to remove all invalid chars from the id to create ResourceLocation,
            // otherwise it crashes
            Pattern pattern = Pattern.compile("[^a-z0-9_.-]");
            Matcher matcher = pattern.matcher(id);

            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                matcher.appendReplacement(sb, String.valueOf(matcher.group().hashCode()));
            }
            matcher.appendTail(sb);

            return sb.toString();
        }

        private void replaceData(ExposureImage exposure) {
            boolean hasChanged = !this.exposure.getName().equals(exposure.getName());
            this.exposure = exposure;
            if (hasChanged) {
                this.texture = new NativeImageBackedTexture(exposure.getWidth(), exposure.getHeight(), true);
            }
            this.requiresUpload |= hasChanged;
        }

        @SuppressWarnings("unused")
        public void forceUpload() {
            this.requiresUpload = true;
        }

        private void updateTexture() {
            if (texture.getImage() == null)
                return;

            for (int y = 0; y < this.exposure.getHeight(); y++) {
                for (int x = 0; x < this.exposure.getWidth(); x++) {
                    int ABGR = this.exposure.getPixelABGR(x, y);
                    ABGR = pixelModifier.modifyPixel(ABGR);
                    this.texture.getImage().setColor(x, y, ABGR); // Texture is in BGR format
                }
            }

            this.texture.upload();
        }

        void draw(MatrixStack poseStack, VertexConsumerProvider bufferSource, float minX, float minY, float maxX, float maxY,
                  float minU, float minV, float maxU, float maxV, int packedLight, int r, int g, int b, int a) {
            if (this.requiresUpload) {
                this.updateTexture();
                this.requiresUpload = false;
            }

            Matrix4f matrix4f = poseStack.peek().getPositionMatrix();
            VertexConsumer vertexconsumer = bufferSource.getBuffer(this.renderType);
            vertexconsumer.vertex(matrix4f, minX, maxY, 0).color(r, g, b, a).texture(minU, maxV).light(packedLight).next();
            vertexconsumer.vertex(matrix4f, maxX, maxY, 0).color(r, g, b, a).texture(maxU, maxV).light(packedLight).next();
            vertexconsumer.vertex(matrix4f, maxX, minY, 0).color(r, g, b, a).texture(maxU, minV).light(packedLight).next();
            vertexconsumer.vertex(matrix4f, minX, minY, 0).color(r, g, b, a).texture(minU, minV).light(packedLight).next();
        }

        public void close() {
            this.texture.close();
        }
    }
}
