package io.github.mortuusars.exposure.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

public class LevelUtil {
    public static int getLightLevelAt(World level, BlockPos pos) {
        level.calculateAmbientDarkness(); // This updates 'getSkyDarken' on the client. Otherwise, it always returns 0.
        int skyBrightness = level.getLightLevel(LightType.SKY, pos);
        int blockBrightness = level.getLightLevel(LightType.BLOCK, pos);
        return skyBrightness < 15 ?
                Math.max(blockBrightness, (int) (skyBrightness * ((15 - level.getAmbientDarkness()) / 15f))) :
                Math.max(blockBrightness, 15 - level.getAmbientDarkness());
    }
}
