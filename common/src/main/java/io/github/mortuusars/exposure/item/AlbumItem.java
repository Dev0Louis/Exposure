package io.github.mortuusars.exposure.item;

import com.google.common.base.Preconditions;
import com.mojang.datafixers.util.Either;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.PlatformHelper;
import io.github.mortuusars.exposure.menu.AlbumMenu;
import io.github.mortuusars.exposure.util.ItemAndStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LecternBlock;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class AlbumItem extends Item {
    public static final String TAG_PAGES = "Pages";
    public static final String TAG_TITLE = "Title";
    public static final String TAG_AUTHOR = "Author";

    public AlbumItem(Settings properties) {
        super(properties);
    }

    public int getMaxPages() {
        return 16;
    }

    public boolean isEditable() {
        return true;
    }

    public Optional<AlbumPage> getPage(ItemStack albumStack, int index) {
        Preconditions.checkElementIndex(index, getMaxPages());
        NbtCompound tag = albumStack.getNbt();
        if (tag == null || tag.isEmpty() || !tag.contains(TAG_PAGES, NbtElement.LIST_TYPE))
            return Optional.empty();

        NbtList pagesTag = getOrCreatePagesTag(albumStack);
        return pagesTag.size() - 1 >= index ?
                Optional.ofNullable(AlbumPage.fromTag(pagesTag.getCompound(index), isEditable())) : Optional.empty();
    }

    public void setPage(ItemStack albumStack, AlbumPage page, int index) {
        Preconditions.checkElementIndex(index, getMaxPages());
        NbtList pagesTag = getOrCreatePagesTag(albumStack);

        while (pagesTag.size() - 1 < index) {
            pagesTag.add(createEmptyPage().toTag(new NbtCompound()));
        }

        pagesTag.set(index, page.toTag(new NbtCompound()));
    }

    public AlbumPage createEmptyPage() {
        return new AlbumPage(ItemStack.EMPTY, isEditable() ? Either.left("") : Either.right(Text.empty()));
    }

    public List<AlbumPage> getPages(ItemStack albumStack) {
        NbtCompound tag = albumStack.getNbt();
        if (tag == null || tag.isEmpty() || !tag.contains(TAG_PAGES, NbtElement.LIST_TYPE))
            return Collections.emptyList();

        NbtList pagesList = tag.getList(TAG_PAGES, NbtElement.COMPOUND_TYPE);
        if (pagesList.isEmpty())
            return Collections.emptyList();

        List<AlbumPage> pages = new ArrayList<>();

        for (int i = 0; i < pagesList.size(); i++) {
            pages.add(AlbumPage.fromTag(pagesList.getCompound(i), isEditable()));
        }

        return pages;
    }

    public void addPage(ItemStack albumStack, AlbumPage page) {
        NbtList pages = getOrCreatePagesTag(albumStack);
        pages.add(page.toTag(new NbtCompound()));
    }

    public void addPage(ItemStack albumStack, AlbumPage page, int index) {
        NbtList pages = getOrCreatePagesTag(albumStack);
        pages.add(index, page.toTag(new NbtCompound()));
    }

    public int getPhotographsCount(ItemStack albumStack) {
        @Nullable NbtCompound tag = albumStack.getNbt();
        if (tag == null || !tag.contains(TAG_PAGES, NbtElement.LIST_TYPE))
            return 0;

        int count = 0;
        NbtList pagesTag = tag.getList(TAG_PAGES, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < pagesTag.size(); i++) {
            NbtCompound pageTag = pagesTag.getCompound(i);
            if (pageTag.contains(AlbumPage.PHOTOGRAPH_TAG, NbtElement.COMPOUND_TYPE))
                count++;
        }

        return count;
    }

    protected NbtList getOrCreatePagesTag(ItemStack albumStack) {
        NbtCompound tag = albumStack.getOrCreateNbt();
        NbtList list = tag.getList(TAG_PAGES, NbtElement.COMPOUND_TYPE);
        tag.put(TAG_PAGES, list);
        return list;
    }

    @Override
    public @NotNull TypedActionResult<ItemStack> use(World level, PlayerEntity player, Hand usedHand) {
        ItemStack itemStack = player.getStackInHand(usedHand);

        if (player instanceof ServerPlayerEntity serverPlayer)
            open(serverPlayer, itemStack, isEditable());

        player.incrementStat(Stats.USED.getOrCreateStat(this));
        return TypedActionResult.success(itemStack, level.isClient());
    }

    @Override
    public @NotNull ActionResult useOnBlock(ItemUsageContext context) {
        BlockPos blockPos = context.getBlockPos();
        World level = context.getWorld();
        BlockState blockState = level.getBlockState(blockPos);
        if (blockState.isOf(Blocks.LECTERN))
            return LecternBlock.putBookIfAbsent(context.getPlayer(), level, blockPos, blockState,
                    context.getStack()) ? ActionResult.success(level.isClient) : ActionResult.PASS;
        return ActionResult.PASS;
    }

    public void open(ServerPlayerEntity player, ItemStack albumStack, boolean editable) {
        NamedScreenHandlerFactory menuProvider = new NamedScreenHandlerFactory() {
            @Override
            public @NotNull Text getDisplayName() {
                return albumStack.getName();
            }

            @Override
            public @NotNull ScreenHandler createMenu(int containerId, @NotNull PlayerInventory playerInventory, @NotNull PlayerEntity player) {
                return new AlbumMenu(containerId, playerInventory, new ItemAndStack<>(albumStack), editable);
            }
        };

        PlatformHelper.openMenu(player, menuProvider, buffer -> {
            buffer.writeItemStack(albumStack);
            buffer.writeBoolean(editable);
        });
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World level, List<Text> tooltipComponents, TooltipContext isAdvanced) {
        if (Config.Client.ALBUM_SHOW_PHOTOS_COUNT.get()) {
            int photographsCount = getPhotographsCount(stack);
            if (photographsCount > 0)
                tooltipComponents.add(Text.translatable("item.exposure.album.tooltip.photos_count", photographsCount));
        }
    }

    public boolean shouldPlayEquipAnimation(ItemStack oldStack, ItemStack newStack) {
        return oldStack.getItem() != newStack.getItem();
    }

    public ItemStack sign(ItemStack album, String title, String author) {
        if (!(album.getItem() instanceof AlbumItem))
            throw new IllegalArgumentException("Can only sign AlbumItem's. Provided: '" + album + "'.");
        else if (!isEditable())
            throw new IllegalArgumentException("Cannot sign fixed album.");

        ItemStack albumCopy = album.copy();
        NbtList pagesTag = getOrCreatePagesTag(albumCopy);

        for (int i = pagesTag.size() - 1; i >= 0; i--) {
            NbtCompound pageTag = pagesTag.getCompound(i);
            AlbumPage page = AlbumPage.fromTag(pageTag, isEditable());

            // Remove until we have page with content
            if (page.isEmpty())
                pagesTag.remove(i);
            else
                break;
        }

        for (int i = 0; i < pagesTag.size(); i++) {
            AlbumPage page = AlbumPage.fromTag(pagesTag.getCompound(i), isEditable());
            pagesTag.set(i, page.toSigned().toTag(new NbtCompound()));
        }

        ItemStack signedAlbum = new ItemStack(Exposure.Items.SIGNED_ALBUM.get());
        signedAlbum.setNbt(albumCopy.getNbt());
        signedAlbum.getOrCreateNbt().putString(TAG_TITLE, title);
        signedAlbum.getOrCreateNbt().putString(TAG_AUTHOR, author);
        return signedAlbum;
    }
}
