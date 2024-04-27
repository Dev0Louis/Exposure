package io.github.mortuusars.exposure.fabric;

import com.mojang.brigadier.arguments.ArgumentType;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.Register;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.SoundEvent;
import java.util.function.Supplier;

public class RegisterImpl {
    public static <T extends Block> Supplier<T> block(String id, Supplier<T> supplier) {
        T obj = Registry.register(Registries.BLOCK, Exposure.id(id), supplier.get());
        return () -> obj;
    }

    public static <T extends BlockEntityType<E>, E extends BlockEntity> Supplier<T> blockEntityType(String id, Supplier<T> supplier) {
        T obj = Registry.register(Registries.BLOCK_ENTITY_TYPE, Exposure.id(id), supplier.get());
        return () -> obj;
    }

    public static <T extends BlockEntity> BlockEntityType<T> newBlockEntityType(Register.BlockEntitySupplier<T> blockEntitySupplier, Block... validBlocks) {
        return FabricBlockEntityTypeBuilder.create(blockEntitySupplier::create, validBlocks).build();
    }

    public static <T extends Item> Supplier<T> item(String id, Supplier<T> supplier) {
        T obj = Registry.register(Registries.ITEM, Exposure.id(id), supplier.get());
        return () -> obj;
    }

    public static <T extends Entity> Supplier<EntityType<T>> entityType(String id, EntityType.EntityFactory<T> factory,
                                                                        SpawnGroup category, float width, float height,
                                                                        int clientTrackingRange, boolean velocityUpdates, int updateInterval) {
        EntityType<T> type = Registry.register(Registries.ENTITY_TYPE, Exposure.id(id),
                FabricEntityTypeBuilder.create(category, factory)
                        .dimensions(EntityDimensions.fixed(width, height))
                        .trackRangeBlocks(clientTrackingRange)
                        .forceTrackedVelocityUpdates(velocityUpdates)
                        .trackedUpdateRate(updateInterval)
                        .build());
        return () -> type;
    }

    public static <T extends SoundEvent> Supplier<T> soundEvent(String id, Supplier<T> supplier) {
        T obj = Registry.register(Registries.SOUND_EVENT, Exposure.id(id), supplier.get());
        return () -> obj;
    }

    public static <T extends ScreenHandlerType<E>, E extends ScreenHandler> Supplier<ScreenHandlerType<E>> menuType(String id, Register.MenuTypeSupplier<E> supplier) {
        ExtendedScreenHandlerType<E> type = Registry.register(Registries.SCREEN_HANDLER, Exposure.id(id), new ExtendedScreenHandlerType<>(supplier::create));
        return () -> type;
    }

    public static Supplier<RecipeSerializer<?>> recipeSerializer(String id, Supplier<RecipeSerializer<?>> supplier) {
        RecipeSerializer<?> obj = Registry.register(Registries.RECIPE_SERIALIZER, Exposure.id(id), supplier.get());
        return () -> obj;
    }

    public static <A extends ArgumentType<?>, T extends ArgumentSerializer.ArgumentTypeProperties<A>, I extends ArgumentSerializer<A, T>>
            Supplier<ArgumentSerializer<A, T>> commandArgumentType(String id, Class<A> infoClass, I argumentTypeInfo) {
        ArgumentTypeRegistry.registerArgumentType(Exposure.id(id), infoClass, argumentTypeInfo);
        return () -> argumentTypeInfo;
    }
}
