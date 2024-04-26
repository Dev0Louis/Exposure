package io.github.mortuusars.exposure.network.packet.client;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.infrastructure.FocalRange;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.handler.ClientPacketsHandler;
import io.github.mortuusars.exposure.network.packet.IPacket;
import org.jetbrains.annotations.Nullable;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;

public record SyncLensesS2CP(ConcurrentMap<Ingredient, FocalRange> lenses) implements IPacket {
    public static final Identifier ID = Exposure.resource("sync_lenses");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public PacketByteBuf toBuffer(PacketByteBuf buffer) {
        buffer.writeVarInt(this.lenses.size());
        for (var lens : this.lenses.entrySet()) {
            Ingredient ingredient = lens.getKey();
            ingredient.write(buffer);
            FocalRange focalRange = lens.getValue();
            focalRange.write(buffer);
        }
        return buffer;
    }

    public static SyncLensesS2CP fromBuffer(PacketByteBuf buffer) {
        ConcurrentMap<Ingredient, FocalRange> lenses = new ConcurrentHashMap<>();

        int lensCount = buffer.readVarInt();
        for (int i = 0; i < lensCount; i++) {
            Ingredient ingredient = Ingredient.fromPacket(buffer);
            FocalRange focalRange = FocalRange.fromNetwork(buffer);
            lenses.put(ingredient, focalRange);
        }

        return new SyncLensesS2CP(lenses);
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable PlayerEntity player) {
        ClientPacketsHandler.syncLenses(this);
        return true;
    }
}
