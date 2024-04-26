package io.github.mortuusars.exposure.camera.viewfinder;

import io.github.mortuusars.exposure.item.CameraItem;
import io.github.mortuusars.exposure.network.Packets;
import io.github.mortuusars.exposure.network.packet.server.CameraSetSelfieModeC2SP;
import io.github.mortuusars.exposure.util.ItemAndStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.Hand;

public class SelfieClient {
    public static void update(ItemAndStack<CameraItem> camera, Hand activeHand, boolean effects) {
        if (MinecraftClient.getInstance().player == null)
            return;

        boolean selfieMode = MinecraftClient.getInstance().options.getPerspective() == Perspective.THIRD_PERSON_FRONT;

        if (effects)
            camera.getItem().setSelfieModeWithEffects(MinecraftClient.getInstance().player, camera.getStack(), selfieMode);
        else
            camera.getItem().setSelfieMode(camera.getStack(), selfieMode);

        Packets.sendToServer(new CameraSetSelfieModeC2SP(activeHand, selfieMode, effects));
    }
}
