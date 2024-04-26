package io.github.mortuusars.exposure.camera.capture;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import io.github.mortuusars.exposure.camera.infrastructure.FrameData;
import java.util.ArrayList;
import java.util.Collection;
import net.minecraft.nbt.NbtCompound;

@SuppressWarnings("unused")
public class CapturedFramesHistory {
    private static final ArrayList<NbtCompound> lastExposures = new ArrayList<>();
    private static int limit = 32;
    public static Collection<NbtCompound> get() {
        return ImmutableList.copyOf(lastExposures);
    }

    public static void add(NbtCompound frame) {
        if (frame.getString(FrameData.ID).isEmpty())
            LogUtils.getLogger().warn(frame + " - frame might not be valid. No ID is present.");

        lastExposures.add(0, frame);

        while (lastExposures.size() > limit) {
            lastExposures.remove(limit);
        }
    }

    public static int getLimit() {
        return limit;
    }

    public static void setLimit(int limit) {
        CapturedFramesHistory.limit = limit;
    }

    public static void clear() {
        lastExposures.clear();
    }
}
