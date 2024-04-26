package io.github.mortuusars.exposure;

import com.mojang.brigadier.arguments.ArgumentType;
import dev.architectury.injectables.annotations.ExpectPlatform;
import io.github.mortuusars.exposure.command.argument.ShaderLocationArgument;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;

public class Register {
    @ExpectPlatform
    public static <T extends Block> Supplier<T> block(String id, Supplier<T> supplier) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends BlockEntityType<E>, E extends BlockEntity> Supplier<T> blockEntityType(String id, Supplier<T> sup) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends BlockEntity> BlockEntityType<T> newBlockEntityType(BlockEntitySupplier<T> blockEntitySupplier, Block... validBlocks) {
        throw new AssertionError();
    }

    @FunctionalInterface
    public interface BlockEntitySupplier<T extends BlockEntity> {

        @NotNull T create(BlockPos pos, BlockState state);
    }

    @ExpectPlatform
    public static <T extends Item> Supplier<T> item(String id, Supplier<T> supplier) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Entity> Supplier<EntityType<T>> entityType(String id, EntityType.EntityFactory<T> factory,
                                                                        SpawnGroup category, float width, float height,
                                                                        int clientTrackingRange, boolean velocityUpdates, int updateInterval) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends SoundEvent> Supplier<T> soundEvent(String id, Supplier<T> supplier) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends ScreenHandlerType<E>, E extends ScreenHandler> Supplier<T> menuType(String id, MenuTypeSupplier<E> supplier) {
        throw new AssertionError();
    }

    @FunctionalInterface
    public interface MenuTypeSupplier<T extends ScreenHandler> {
        @NotNull T create(int windowId, PlayerInventory playerInv, PacketByteBuf extraData);
    }

    @ExpectPlatform
    public static Supplier<RecipeSerializer<?>> recipeSerializer(String filmDeveloping, Supplier<RecipeSerializer<?>> supplier) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <A extends ArgumentType<?>, T extends ArgumentSerializer.ArgumentTypeProperties<A>, I extends ArgumentSerializer<A, T>>
            Supplier<ArgumentSerializer<A, T>> commandArgumentType(String id, Class<A> infoClass, I argumentTypeInfo) {
        throw new AssertionError();
    }
}
