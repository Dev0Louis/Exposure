package io.github.mortuusars.exposure.test.data;

import com.google.gson.JsonObject;
import io.github.mortuusars.exposure.advancement.predicate.ExposurePredicate;
import io.github.mortuusars.exposure.camera.infrastructure.FrameData;
import io.github.mortuusars.exposure.test.framework.ITestClass;
import io.github.mortuusars.exposure.test.framework.Test;
import java.util.List;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.JsonHelper;

public class ExposurePredicateTests implements ITestClass {
    @Override
    public List<Test> collect() {
        return List.of(
                new Test("ExposurePredicate_DeserializesProperly", this::deserializesProperly)
        );
    }

    private void deserializesProperly(ServerPlayerEntity serverPlayer) {
        String json =
            """
            {
              "owner": true,
              "shutter_speed_ms": {
                "min": 50
              },
              "entities_count": {
                "max": 5
              }
            }
            """;

        JsonObject jsonObj = JsonHelper.deserialize(json).getAsJsonObject();

        ExposurePredicate exposurePredicate = ExposurePredicate.fromJson(jsonObj);

        NbtCompound frame = new NbtCompound();
        frame.putUuid(FrameData.PHOTOGRAPHER_ID, serverPlayer.getUuid());
        frame.putFloat(FrameData.SHUTTER_SPEED_MS, 100);
        NbtList entities = new NbtList();
        entities.add(new NbtCompound());
        entities.add(new NbtCompound());
        frame.put(FrameData.ENTITIES_IN_FRAME, entities);

        assertThat(exposurePredicate.matches(serverPlayer, frame), "Deserialized predicate does not match frame: " + frame);
    }
}
