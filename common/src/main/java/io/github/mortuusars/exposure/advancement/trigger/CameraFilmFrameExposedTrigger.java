package io.github.mortuusars.exposure.advancement.trigger;

import com.google.gson.JsonObject;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.advancement.predicate.ExposurePredicate;
import io.github.mortuusars.exposure.item.CameraItem;
import io.github.mortuusars.exposure.util.ItemAndStack;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.AbstractCriterionConditions;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.AdvancementEntityPredicateSerializer;
import net.minecraft.predicate.entity.LocationPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public class CameraFilmFrameExposedTrigger extends AbstractCriterion<CameraFilmFrameExposedTrigger.TriggerInstance> {
    public static final Identifier ID = Exposure.resource("frame_exposed");

    public @NotNull Identifier getId() {
        return ID;
    }

    @Override
    protected @NotNull TriggerInstance conditionsFromJson(JsonObject json, @NotNull LootContextPredicate predicate,
                                                      @NotNull AdvancementEntityPredicateDeserializer deserializationContext) {
        LocationPredicate location = LocationPredicate.fromJson(json.get("location"));
        ExposurePredicate exposure = ExposurePredicate.fromJson(json.get("exposure"));
        return new TriggerInstance(predicate, location, exposure);
    }

    public void trigger(ServerPlayerEntity player, ItemAndStack<CameraItem> camera, NbtCompound frame) {
        this.trigger(player, triggerInstance -> triggerInstance.matches(player, camera, frame));
    }

    public static class TriggerInstance extends AbstractCriterionConditions {
        private final LocationPredicate locationPredicate;
        private final ExposurePredicate exposurePredicate;

        public TriggerInstance(LootContextPredicate predicate, LocationPredicate locationPredicate, ExposurePredicate exposurePredicate) {
            super(ID, predicate);
            this.locationPredicate = locationPredicate;
            this.exposurePredicate = exposurePredicate;
        }

        public boolean matches(ServerPlayerEntity player, ItemAndStack<CameraItem> camera, NbtCompound frame) {
            if (!locationPredicate.test(player.getServerWorld(), player.getX(), player.getY(), player.getZ()))
                return false;

            return exposurePredicate.matches(player, frame);
        }

        public @NotNull JsonObject toJson(@NotNull AdvancementEntityPredicateSerializer conditions) {
            JsonObject jsonobject = super.toJson(conditions);
            if (this.exposurePredicate != ExposurePredicate.ANY)
                jsonobject.add("exposure", this.exposurePredicate.serializeToJson());

            if (this.locationPredicate != LocationPredicate.ANY)
                jsonobject.add("location", this.locationPredicate.toJson());

            return jsonobject;
        }
    }
}
