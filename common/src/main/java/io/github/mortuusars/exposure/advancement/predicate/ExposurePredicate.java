package io.github.mortuusars.exposure.advancement.predicate;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import io.github.mortuusars.exposure.advancement.BooleanPredicate;
import io.github.mortuusars.exposure.camera.infrastructure.FrameData;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.predicate.NbtPredicate;
import net.minecraft.predicate.NumberRange;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.JsonHelper;

public class ExposurePredicate {
    public static final ExposurePredicate ANY = new ExposurePredicate(BooleanPredicate.ANY,
            NumberRange.FloatRange.ANY,
            NumberRange.FloatRange.ANY,
            NbtPredicate.ANY,
            NumberRange.IntRange.ANY,
            NumberRange.IntRange.ANY,
            EntityInFramePredicate.ANY);

    private final BooleanPredicate owner;
    private final NumberRange.FloatRange shutterSpeedMS;
    private final NumberRange.FloatRange focalLength;
    private final NbtPredicate nbt;
    private final NumberRange.IntRange lightLevel;
    private final NumberRange.IntRange entitiesInFrameCount;
    private final EntityInFramePredicate entityInFrame;

    public ExposurePredicate(BooleanPredicate ownerPredicate,
                             NumberRange.FloatRange shutterSpeedMS,
                             NumberRange.FloatRange focalLength,
                             NbtPredicate nbtPredicate,
                             NumberRange.IntRange lightLevel,
                             NumberRange.IntRange entitiesInFrameCount,
                             EntityInFramePredicate entityInFramePredicate) {
        this.owner = ownerPredicate;
        this.shutterSpeedMS = shutterSpeedMS;
        this.focalLength = focalLength;
        this.nbt = nbtPredicate;
        this.lightLevel = lightLevel;
        this.entitiesInFrameCount = entitiesInFrameCount;
        this.entityInFrame = entityInFramePredicate;
    }

    public boolean matches(ServerPlayerEntity player, NbtCompound tag) {
        if (!ownerMatches(player, tag))
            return false;

        if (!shutterSpeedMS.test(tag.getFloat(FrameData.SHUTTER_SPEED_MS)))
            return false;

        if (!focalLength.test(tag.getFloat(FrameData.FOCAL_LENGTH)))
            return false;

        if (!nbt.test(tag))
            return false;

        if (!lightLevel.test(tag.getInt(FrameData.LIGHT_LEVEL)))
            return false;

        if (!entitiesMatch(player, tag))
            return false;

        return true;
    }

    private boolean ownerMatches(ServerPlayerEntity player, NbtCompound tag) {
        if (owner.equals(BooleanPredicate.ANY))
            return true;

        if (!tag.contains("PhotographerId", NbtElement.INT_ARRAY_TYPE))
            return false;

        UUID photographerId = tag.getUuid("PhotographerId");
        UUID playerId = player.getUuid();

        return owner.matches(photographerId.equals(playerId));
    }

    private boolean entitiesMatch(ServerPlayerEntity player, NbtCompound tag) {
        if (tag.contains(FrameData.ENTITIES_IN_FRAME, NbtElement.LIST_TYPE)) {
            NbtList entities = tag.getList(FrameData.ENTITIES_IN_FRAME, NbtElement.COMPOUND_TYPE);

            if (!entitiesInFrameCount.test(entities.size()))
                return false;

            for (int i = 0; i < entities.size(); i++) {
                if (entityInFrame.matches(player, entities.getCompound(i)))
                    return true;
            }
        }
        else {
            return entityInFrame.equals(EntityInFramePredicate.ANY) && entitiesInFrameCount.test(0);
        }

        return false;
    }

    public JsonElement serializeToJson() {
        if (this == ANY)
            return JsonNull.INSTANCE;

        JsonObject json = new JsonObject();

        if (!owner.equals(BooleanPredicate.ANY))
            json.add("owner", owner.serializeToJson());

        if (!shutterSpeedMS.isDummy())
            json.add("shutter_speed_ms", shutterSpeedMS.toJson());

        if (!focalLength.isDummy())
            json.add("focal_length", focalLength.toJson());

        if (!nbt.equals(NbtPredicate.ANY))
            json.add("nbt", nbt.toJson());

        if (!lightLevel.isDummy())
            json.add("light_level", lightLevel.toJson());

        if (!entitiesInFrameCount.isDummy())
            json.add("entities_count", entitiesInFrameCount.toJson());

        if (!entityInFrame.equals(EntityInFramePredicate.ANY))
            json.add("entity_in_frame", entityInFrame.serializeToJson());

        return json;
    }

    public static ExposurePredicate fromJson(@Nullable JsonElement json) {
        if (json == null || json.isJsonNull())
            return ANY;

        JsonObject jsonobject = JsonHelper.asObject(json, "exposure");

        return new ExposurePredicate(
                BooleanPredicate.fromJson(jsonobject.get("owner")),
                NumberRange.FloatRange.fromJson(jsonobject.get("shutter_speed_ms")),
                NumberRange.FloatRange.fromJson(jsonobject.get("focal_length")),
                NbtPredicate.fromJson(jsonobject.get("nbt")),
                NumberRange.IntRange.fromJson(jsonobject.get("light_level")),
                NumberRange.IntRange.fromJson(jsonobject.get("entities_count")),
                EntityInFramePredicate.fromJson(jsonobject.get("entity_in_frame")));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExposurePredicate that = (ExposurePredicate) o;
        return Objects.equals(owner, that.owner) && Objects.equals(shutterSpeedMS, that.shutterSpeedMS) && Objects.equals(focalLength, that.focalLength) && Objects.equals(nbt, that.nbt) && Objects.equals(entitiesInFrameCount, that.entitiesInFrameCount) && Objects.equals(entityInFrame, that.entityInFrame);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, shutterSpeedMS, focalLength, nbt, entitiesInFrameCount, entityInFrame);
    }
}
