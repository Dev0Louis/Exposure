package io.github.mortuusars.exposure.test.framework;

import java.util.function.Consumer;
import net.minecraft.server.network.ServerPlayerEntity;

@SuppressWarnings("ClassCanBeRecord")
public class Test {
    public final String name;
    public final Consumer<ServerPlayerEntity> test;

    public Test(String name, Consumer<ServerPlayerEntity> test) {
        this.name = name;
        this.test = test;
    }
}
