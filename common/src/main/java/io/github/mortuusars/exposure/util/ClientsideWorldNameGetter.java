package io.github.mortuusars.exposure.util;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.nio.file.Path;
import net.minecraft.client.MinecraftClient;

public class ClientsideWorldNameGetter {
    public static String getWorldName() {
        try {
            if (MinecraftClient.getInstance().isConnectedToLocalServer()) {
                if (MinecraftClient.getInstance().getServer() != null)
                    return MinecraftClient.getInstance().getServer().getSaveProperties().getLevelName()
                            .replace('.', '_'); // Folder name has underscores instead of dots.
                else {
                    String gameDirectory = MinecraftClient.getInstance().runDirectory.getAbsolutePath();
                    Path savesDir = Path.of(gameDirectory, "/saves");

                    File[] dirs = savesDir.toFile().listFiles((dir, name) -> new File(dir, name).isDirectory());

                    if (dirs == null || dirs.length == 0)
                        return "";

                    File lastModified = dirs[0];

                    for (File dir : dirs) {
                        if (dir.lastModified() > lastModified.lastModified())
                            lastModified = dir;
                    }

                    return lastModified.getName();
                }
            }
            else if (MinecraftClient.getInstance().getCurrentServerEntry() != null) {
                return MinecraftClient.getInstance().getCurrentServerEntry().name;
            }
            else {
                return "Unknown";
            }
        } catch (Exception e) {
            LogUtils.getLogger().error("Failed to get level name: " + e);
            return "Unknown";
        }
    }
}
