package io.github.mortuusars.exposure.advancement.predicate;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import io.github.mortuusars.exposure.camera.infrastructure.FrameData;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.entity.LocationPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class EntityInFramePredicate {
    public static final EntityInFramePredicate ANY = new EntityInFramePredicate(null, LocationPredicate.ANY, NumberRange.FloatRange.ANY);

    @Nullable
    private final String id;
    private final LocationPredicate location;
    private final NumberRange.FloatRange distance;

    public EntityInFramePredicate(@Nullable Identifier id, LocationPredicate location, NumberRange.FloatRange distance) {
        this.id = id != null ? id.toString() : null;
        this.location = location;
        this.distance = distance;
    }

    public boolean matches(ServerPlayerEntity player, NbtCompound entityInfoTag) {
        if (this.equals(ANY))
            return true;

        if (id != null && !id.equals(entityInfoTag.getString(FrameData.ENTITY_ID)))
            return false;

        if (!locationMatches(player, entityInfoTag))
            return false;

        if (!distance.test(entityInfoTag.getFloat(FrameData.ENTITY_DISTANCE)))
            return false;

        return true;
    }

    private boolean locationMatches(ServerPlayerEntity player, NbtCompound entityInfoTag) {
        NbtList posList = entityInfoTag.getList(FrameData.ENTITY_POSITION, NbtElement.INT_TYPE);
        if (posList.size() < 3)
            return false;

        int x = posList.getInt(0);
        int y = posList.getInt(1);
        int z = posList.getInt(2);

        return location.test(player.getServerWorld(), x, y, z);
    }

    public JsonElement serializeToJson() {
        if (this == ANY)
            return JsonNull.INSTANCE;

        JsonObject json = new JsonObject();
        if (id != null)
            json.addProperty("id", id);

        if (!location.equals(LocationPredicate.ANY))
            json.add("location", location.toJson());

        if (!distance.isDummy())
            json.add("distance", distance.toJson());

        return json;
    }

    public static EntityInFramePredicate fromJson(@Nullable JsonElement json) {
        if (json == null || json.isJsonNull())
            return ANY;

        JsonObject jsonobject = JsonHelper.asObject(json, "entity");

        String id = null;
        if (jsonobject.has("id"))
            id = jsonobject.get("id").getAsString();

        LocationPredicate location = LocationPredicate.fromJson(jsonobject.getAsJsonObject("location"));
        NumberRange.FloatRange distance = NumberRange.FloatRange.fromJson(jsonobject.getAsJsonObject("distance"));

        return new EntityInFramePredicate(id != null ? new Identifier(id) : null, location, distance);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityInFramePredicate that = (EntityInFramePredicate) o;
        return Objects.equals(id, that.id) && Objects.equals(location, that.location) && Objects.equals(distance, that.distance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, location, distance);
    }
}
