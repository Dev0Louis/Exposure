package io.github.mortuusars.exposure.network.packet.server;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.item.CameraItem;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.packet.IPacket;
import io.github.mortuusars.exposure.util.ItemAndStack;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.Structure;

public record CameraInHandAddFrameC2SP(Hand hand, NbtCompound frame) implements IPacket {
    public static final Identifier ID = Exposure.resource("camera_in_hand_add_frame");

    @Override
    public Identifier getId() {
        return ID;
    }

    public PacketByteBuf toBuffer(PacketByteBuf buffer) {
        buffer.writeEnumConstant(hand);
        buffer.writeNbt(frame);
        return buffer;
    }

    public static CameraInHandAddFrameC2SP fromBuffer(PacketByteBuf buffer) {
        Hand hand = buffer.readEnumConstant(Hand.class);
        @Nullable NbtCompound frame = buffer.readUnlimitedNbt();
        if (frame == null)
            frame = new NbtCompound();
        return new CameraInHandAddFrameC2SP(hand, frame);
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable PlayerEntity player) {
        Preconditions.checkState(player != null, "Cannot handle packet: Player was null");
        ServerPlayerEntity serverPlayer = ((ServerPlayerEntity) player);

        ItemStack itemInHand = player.getStackInHand(hand);
        if (!(itemInHand.getItem() instanceof CameraItem cameraItem))
            throw new IllegalStateException("Item in hand in not a Camera.");

        addStructuresInfo(serverPlayer);

        cameraItem.addFrameToFilm(itemInHand, frame);
        Exposure.Advancements.FILM_FRAME_EXPOSED.trigger(serverPlayer, new ItemAndStack<>(itemInHand), frame);
        return true;
    }

    private void addStructuresInfo(@NotNull ServerPlayerEntity player) {
        Map<Structure, LongSet> allStructuresAt = player.getServerWorld().getStructureAccessor().getStructureReferences(player.getBlockPos());

        List<Structure> inside = new ArrayList<>();

        for (Structure structure : allStructuresAt.keySet()) {
            StructureStart structureAt = player.getServerWorld().getStructureAccessor().getStructureAt(player.getBlockPos(), structure);
            if (structureAt.hasChildren()) {
                inside.add(structure);
            }
        }

        Registry<Structure> structures = player.getServerWorld().getRegistryManager().get(RegistryKeys.STRUCTURE);
        NbtList structuresTag = new NbtList();

        for (Structure structure : inside) {
            Identifier key = structures.getId(structure);
            if (key != null)
                structuresTag.add(NbtString.of(key.toString()));
        }

        if (structuresTag.size() > 0) {
            frame.put("Structures", structuresTag);
        }
    }
}
