package io.github.mortuusars.exposure;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.block.FlashBlock;
import io.github.mortuusars.exposure.block.LightroomBlock;
import io.github.mortuusars.exposure.block.entity.FlashBlockEntity;
import io.github.mortuusars.exposure.block.entity.LightroomBlockEntity;
import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import io.github.mortuusars.exposure.command.argument.ExposureLookArgument;
import io.github.mortuusars.exposure.command.argument.ExposureSizeArgument;
import io.github.mortuusars.exposure.command.argument.ShaderLocationArgument;
import io.github.mortuusars.exposure.command.argument.TextureLocationArgument;
import io.github.mortuusars.exposure.entity.PhotographEntity;
import io.github.mortuusars.exposure.item.*;
import io.github.mortuusars.exposure.menu.AlbumMenu;
import io.github.mortuusars.exposure.menu.CameraAttachmentsMenu;
import io.github.mortuusars.exposure.menu.LecternAlbumMenu;
import io.github.mortuusars.exposure.menu.LightroomMenu;
import io.github.mortuusars.exposure.recipe.FilmDevelopingRecipe;
import io.github.mortuusars.exposure.recipe.PhotographAgingRecipe;
import io.github.mortuusars.exposure.recipe.PhotographCopyingRecipe;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.MapColor;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvent;
import net.minecraft.stat.StatFormatter;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;


public class Exposure {
    public static final String ID = "exposure";

    public static final int DEFAULT_FILM_SIZE = 320;
    public static final float CROP_FACTOR = 1.142857f;

    public static void init() {
        Blocks.init();
        BlockEntityTypes.init();
        Items.init();
        EntityTypes.init();
        MenuTypes.init();
        RecipeSerializers.init();
        SoundEvents.init();
        ArgumentTypes.init();
    }

    public static void initServer(MinecraftServer server) {
        ExposureServer.init(server);
    }

    /**
     * Creates resource location in the mod namespace with the given path.
     */
    public static Identifier id(String path) {
        return new Identifier(ID, path);
    }

    public static class Blocks {
        public static final Supplier<LightroomBlock> LIGHTROOM = Register.block("lightroom",
                () -> new LightroomBlock(AbstractBlock.Settings.create()
                        .mapColor(MapColor.BROWN)
                        .strength(2.5f)
                        .sounds(BlockSoundGroup.WOOD)
                        .luminance(state -> 15)));

        public static final Supplier<FlashBlock> FLASH = Register.block("flash",
                () -> new FlashBlock(AbstractBlock.Settings.copy(net.minecraft.block.Blocks.AIR)
                        .strength(-1.0F, 3600000.8F)
                        .dropsNothing()
                        .mapColor(MapColor.CLEAR)
                        .nonOpaque()
                        .noCollision()
                        .luminance(state -> 15)));

        static void init() {
        }
    }

    public static class BlockEntityTypes {
        public static final Supplier<BlockEntityType<LightroomBlockEntity>> LIGHTROOM =
                Register.blockEntityType("lightroom", () -> Register.newBlockEntityType(LightroomBlockEntity::new, Blocks.LIGHTROOM.get()));

        public static final Supplier<BlockEntityType<FlashBlockEntity>> FLASH =
                Register.blockEntityType("flash", () -> Register.newBlockEntityType(FlashBlockEntity::new, Blocks.FLASH.get()));

        static void init() {
        }
    }

    public static class Items {
        public static final Supplier<CameraItem> CAMERA = Register.item("camera",
                () -> new CameraItem(new Item.Settings()
                        .maxCount(1)));

        public static final Supplier<FilmRollItem> BLACK_AND_WHITE_FILM = Register.item("black_and_white_film",
                () -> new FilmRollItem(FilmType.BLACK_AND_WHITE, Exposure.DEFAULT_FILM_SIZE, MathHelper.packRgb(0.8F, 0.8F, 0.9F),
                        new Item.Settings()
                                .maxCount(16)));

        public static final Supplier<FilmRollItem> COLOR_FILM = Register.item("color_film",
                () -> new FilmRollItem(FilmType.COLOR, Exposure.DEFAULT_FILM_SIZE, MathHelper.packRgb(0.4F, 0.4F, 1.0F), new Item.Settings()
                        .maxCount(16)));

        public static final Supplier<DevelopedFilmItem> DEVELOPED_BLACK_AND_WHITE_FILM = Register.item("developed_black_and_white_film",
                () -> new DevelopedFilmItem(FilmType.BLACK_AND_WHITE, new Item.Settings()
                        .maxCount(1)));

        public static final Supplier<DevelopedFilmItem> DEVELOPED_COLOR_FILM = Register.item("developed_color_film",
                () -> new DevelopedFilmItem(FilmType.COLOR, new Item.Settings()
                        .maxCount(1)));

        public static final Supplier<PhotographItem> PHOTOGRAPH = Register.item("photograph",
                () -> new PhotographItem(new Item.Settings()
                        .maxCount(1)));

        public static final Supplier<Item> CHROMATIC_SHEET = Register.item("chromatic_sheet",
                () -> new ChromaticSheetItem(new Item.Settings()
                        .maxCount(1)));

        public static final Supplier<PhotographItem> AGED_PHOTOGRAPH = Register.item("aged_photograph",
                () -> new PhotographItem(new Item.Settings()
                        .maxCount(1)));

        public static final Supplier<StackedPhotographsItem> STACKED_PHOTOGRAPHS = Register.item("stacked_photographs",
                () -> new StackedPhotographsItem(new Item.Settings()
                        .maxCount(1)));

        public static final Supplier<AlbumItem> ALBUM = Register.item("album",
                () -> new AlbumItem(new Item.Settings()
                        .maxCount(1)));
        public static final Supplier<SignedAlbumItem> SIGNED_ALBUM = Register.item("signed_album",
                () -> new SignedAlbumItem(new Item.Settings()
                        .maxCount(1)));

        public static final Supplier<BlockItem> LIGHTROOM = Register.item("lightroom",
                () -> new BlockItem(Blocks.LIGHTROOM.get(), new Item.Settings()));

        static void init() {
        }
    }

    public static class EntityTypes {
        public static final Supplier<EntityType<PhotographEntity>> PHOTOGRAPH = Register.entityType("photograph",
                PhotographEntity::new, SpawnGroup.MISC, 0.5F, 0.5F, 128, false, Integer.MAX_VALUE);

        static void init() {
        }
    }

    public static class MenuTypes {
        public static final Supplier<ScreenHandlerType<CameraAttachmentsMenu>> CAMERA = Register.menuType("camera", CameraAttachmentsMenu::fromBuffer);
        public static final Supplier<ScreenHandlerType<AlbumMenu>> ALBUM = Register.menuType("album", AlbumMenu::fromBuffer);
        public static final Supplier<ScreenHandlerType<LecternAlbumMenu>> LECTERN_ALBUM = Register.menuType("lectern_album", LecternAlbumMenu::fromBuffer);
        public static final Supplier<ScreenHandlerType<LightroomMenu>> LIGHTROOM = Register.menuType("lightroom", LightroomMenu::fromBuffer);

        static void init() {
        }
    }

    public static class RecipeSerializers {
        public static final Supplier<RecipeSerializer<?>> FILM_DEVELOPING = Register.recipeSerializer("film_developing",
                FilmDevelopingRecipe.Serializer::new);
        public static final Supplier<RecipeSerializer<?>> PHOTOGRAPH_CLONING = Register.recipeSerializer("photograph_copying",
                PhotographCopyingRecipe.Serializer::new);
        public static final Supplier<RecipeSerializer<?>> PHOTOGRAPH_AGING = Register.recipeSerializer("photograph_aging",
                PhotographAgingRecipe.Serializer::new);

        static void init() {
        }
    }

    public static class SoundEvents {
        public static final Supplier<SoundEvent> VIEWFINDER_OPEN = register("item", "camera.viewfinder_open");
        public static final Supplier<SoundEvent> VIEWFINDER_CLOSE = register("item", "camera.viewfinder_close");
        public static final Supplier<SoundEvent> SHUTTER_OPEN = register("item", "camera.shutter_open");
        public static final Supplier<SoundEvent> SHUTTER_CLOSE = register("item", "camera.shutter_close");
        public static final Supplier<SoundEvent> SHUTTER_TICKING = register("item", "camera.shutter_ticking");
        public static final Supplier<SoundEvent> FILM_ADVANCE = register("item", "camera.film_advance");
        public static final Supplier<SoundEvent> FILM_ADVANCE_LAST = register("item", "camera.film_advance_last");
        public static final Supplier<SoundEvent> CAMERA_BUTTON_CLICK = register("item", "camera.button_click");
        public static final Supplier<SoundEvent> CAMERA_RELEASE_BUTTON_CLICK = register("item", "camera.release_button_click");
        public static final Supplier<SoundEvent> CAMERA_DIAL_CLICK = register("item", "camera.dial_click");
        public static final Supplier<SoundEvent> CAMERA_LENS_RING_CLICK = register("item", "camera.lens_ring_click");
        public static final Supplier<SoundEvent> FILTER_PLACE = register("item", "camera.filter_place");
        public static final Supplier<SoundEvent> FLASH = register("item", "camera.flash");

        public static final Supplier<SoundEvent> PHOTOGRAPH_PLACE = register("item", "photograph.place");
        public static final Supplier<SoundEvent> PHOTOGRAPH_BREAK = register("item", "photograph.break");
        public static final Supplier<SoundEvent> PHOTOGRAPH_RUSTLE = register("item", "photograph.rustle");

        public static final Supplier<SoundEvent> LIGHTROOM_PRINT = register("block", "lightroom.print");

        private static Supplier<SoundEvent> register(String category, String key) {
            Preconditions.checkState(category != null && !category.isEmpty(), "'category' should not be empty.");
            Preconditions.checkState(key != null && !key.isEmpty(), "'key' should not be empty.");
            String path = category + "." + key;
            return Register.soundEvent(path, () -> SoundEvent.of(Exposure.id(path)));
        }

        static void init() {
        }
    }

    public static class Stats {
        private static final Map<Identifier, StatFormatter> STATS = new HashMap<>();

        public static final Identifier INTERACT_WITH_LIGHTROOM =
                register(Exposure.id("interact_with_lightroom"), StatFormatter.DEFAULT);
        public static final Identifier FILM_FRAMES_EXPOSED =
                register(Exposure.id("film_frames_exposed"), StatFormatter.DEFAULT);
        public static final Identifier FLASHES_TRIGGERED =
                register(Exposure.id("flashes_triggered"), StatFormatter.DEFAULT);

        @SuppressWarnings("SameParameterValue")
        private static Identifier register(Identifier location, StatFormatter formatter) {
            STATS.put(location, formatter);
            return location;
        }

        public static void register() {
            STATS.forEach((location, formatter) -> {
                Registry.register(Registries.CUSTOM_STAT, location, location);
                net.minecraft.stat.Stats.CUSTOM.getOrCreateStat(location, formatter);
            });
        }
    }

    public static class Tags {
        public static class Items {
            public static final TagKey<Item> FILM_ROLLS = TagKey.of(RegistryKeys.ITEM, Exposure.id("film_rolls"));
            public static final TagKey<Item> DEVELOPED_FILM_ROLLS = TagKey.of(RegistryKeys.ITEM, Exposure.id("developed_film_rolls"));
            public static final TagKey<Item> CYAN_PRINTING_DYES = TagKey.of(RegistryKeys.ITEM, Exposure.id("cyan_printing_dyes"));
            public static final TagKey<Item> MAGENTA_PRINTING_DYES = TagKey.of(RegistryKeys.ITEM, Exposure.id("magenta_printing_dyes"));
            public static final TagKey<Item> YELLOW_PRINTING_DYES = TagKey.of(RegistryKeys.ITEM, Exposure.id("yellow_printing_dyes"));
            public static final TagKey<Item> BLACK_PRINTING_DYES = TagKey.of(RegistryKeys.ITEM, Exposure.id("black_printing_dyes"));
            public static final TagKey<Item> PHOTO_PAPERS = TagKey.of(RegistryKeys.ITEM, Exposure.id("photo_papers"));
            public static final TagKey<Item> PHOTO_AGERS = TagKey.of(RegistryKeys.ITEM, Exposure.id("photo_agers"));
            public static final TagKey<Item> FLASHES = TagKey.of(RegistryKeys.ITEM, Exposure.id("flashes"));
            public static final TagKey<Item> LENSES = TagKey.of(RegistryKeys.ITEM, Exposure.id("lenses"));
            public static final TagKey<Item> FILTERS = TagKey.of(RegistryKeys.ITEM, Exposure.id("filters"));

            public static final TagKey<Item> RED_FILTERS = TagKey.of(RegistryKeys.ITEM, Exposure.id("red_filters"));
            public static final TagKey<Item> GREEN_FILTERS = TagKey.of(RegistryKeys.ITEM, Exposure.id("green_filters"));
            public static final TagKey<Item> BLUE_FILTERS = TagKey.of(RegistryKeys.ITEM, Exposure.id("blue_filters"));
        }
    }

    public static class ArgumentTypes {
        public static final Supplier<ArgumentSerializer<ExposureSizeArgument, ConstantArgumentSerializer<ExposureSizeArgument>.Properties>> EXPOSURE_SIZE =
                Register.commandArgumentType("exposure_size", ExposureSizeArgument.class, ConstantArgumentSerializer.of(ExposureSizeArgument::new));
        public static final Supplier<ArgumentSerializer<ExposureLookArgument, ConstantArgumentSerializer<ExposureLookArgument>.Properties>> EXPOSURE_LOOK =
                Register.commandArgumentType("exposure_look", ExposureLookArgument.class, ConstantArgumentSerializer.of(ExposureLookArgument::new));
        public static final Supplier<ArgumentSerializer<ShaderLocationArgument, ConstantArgumentSerializer<ShaderLocationArgument>.Properties>> SHADER_LOCATION =
                Register.commandArgumentType("shader_location", ShaderLocationArgument.class, ConstantArgumentSerializer.of(ShaderLocationArgument::new));
        public static final Supplier<ArgumentSerializer<TextureLocationArgument, ConstantArgumentSerializer<TextureLocationArgument>.Properties>> TEXTURE_LOCATION =
                Register.commandArgumentType("texture_location", TextureLocationArgument.class, ConstantArgumentSerializer.of(TextureLocationArgument::new));
        public static void init() { }
    }
}
