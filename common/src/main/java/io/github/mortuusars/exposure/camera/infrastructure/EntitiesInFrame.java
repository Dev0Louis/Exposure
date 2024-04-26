package io.github.mortuusars.exposure.camera.infrastructure;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.util.Fov;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class EntitiesInFrame {
    public static List<Entity> get(PlayerEntity player, double fov, int limit, boolean inSelfieMode) {
        double currentFov = fov / Exposure.CROP_FACTOR;
        double currentFocalLength = Fov.fovToFocalLength(currentFov);
        Vec3d cameraPos = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();

        List<Entity> entities = player.getWorld().getOtherEntities(player, new Box(player.getBlockPos()).expand(128),
                entity -> entity instanceof LivingEntity);

        entities.sort((entity, entity2) -> {
            float dist1 = player.distanceTo(entity);
            float dist2 = player.distanceTo(entity2);
            if (dist1 == dist2) return 0;
            return dist1 > dist2 ? 1 : -1;
        });

        List<Entity> entitiesInFrame = new ArrayList<>();

        for (Entity entity : entities) {
            if (entitiesInFrame.size() >= limit)
                break;

            if (!isInFOV(currentFov, entity))
                continue; // Not in frame

            if (getPerceivedDistance(cameraPos, entity) > currentFocalLength)
                continue; // Too far to be in frame

            if (!player.canSee(entity))
                continue; // Not visible

            entitiesInFrame.add(entity);
        }

        if (inSelfieMode)
            entitiesInFrame.add(0, player);

        return entitiesInFrame;
    }

    /**
     * Gets the distance in blocks to the target entity. Perceived == adjusted relative to the size of entity's bounding box.
     */
    public static double getPerceivedDistance(Vec3d cameraPos, Entity entity) {
        double distanceInBlocks = Math.sqrt(entity.squaredDistanceTo(cameraPos));

        Box boundingBox = entity.getVisibilityBoundingBox();
        double size = boundingBox.getAverageSideLength();
        if (Double.isNaN(size) || size == 0.0)
            size = 0.1;

        double sizeModifier = (size - 1.0) * 0.6 + 1.0;
        return (distanceInBlocks / sizeModifier) / Exposure.CROP_FACTOR;
    }

    public static boolean isInFOV(double fov, Entity target) {
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        Vec3d cameraLookAngle = new Vec3d(camera.getHorizontalPlane());
        Vec3d targetEyePos = target.getPos().add(0, target.getStandingEyeHeight(), 0);

        // Valid angles form a circle instead of square.
        // Due to this, entities in the corners of a frame are not considered "in frame".
        // I'm too dumb at maths to fix this.
        
        double relativeAngle = getRelativeAngle(cameraPos, cameraLookAngle, targetEyePos);
        return relativeAngle <= fov / 2f;
    }

    /**
     * L    T (Target)
     * | D /
     * |--/
     * | /
     * |/
     * C (Camera), L (Camera look angle)
     */
    public static double getRelativeAngle(Vec3d cameraPos, Vec3d cameraLookAngle, Vec3d targetEyePos) {
        Vec3d originToTargetAngle = targetEyePos.subtract(cameraPos).normalize();
        return Math.toDegrees(Math.acos(cameraLookAngle.dotProduct(originToTargetAngle)));
    }
}
