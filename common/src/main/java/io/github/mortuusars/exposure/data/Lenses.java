package io.github.mortuusars.exposure.data;

import io.github.mortuusars.exposure.camera.infrastructure.FocalRange;
import io.github.mortuusars.exposure.network.Packets;
import io.github.mortuusars.exposure.network.packet.IPacket;
import io.github.mortuusars.exposure.network.packet.client.SyncLensesS2CP;
import org.jetbrains.annotations.Nullable;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

public class Lenses {
    private static ConcurrentMap<Ingredient, FocalRange> lenses = new ConcurrentHashMap<>();

    public static void reload(ConcurrentMap<Ingredient, FocalRange> newLenses) {
        lenses.clear();
        lenses = newLenses;
    }

    public static Optional<FocalRange> getFocalRangeOf(ItemStack stack) {
        for (var lens : lenses.entrySet()) {
            if (lens.getKey().test(stack))
                return Optional.of(lens.getValue());
        }

        return Optional.empty();
    }

    public static IPacket getSyncToClientPacket() {
        return new SyncLensesS2CP(new ConcurrentHashMap<>(lenses));
    }

    public static void onDatapackSync(PlayerManager playerList, @Nullable ServerPlayerEntity excludePlayer) {
        IPacket packet = getSyncToClientPacket();
        Packets.sendToClients(packet, playerList, excludePlayer);
    }
}
