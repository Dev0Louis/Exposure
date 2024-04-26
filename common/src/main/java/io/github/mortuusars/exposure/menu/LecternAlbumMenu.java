package io.github.mortuusars.exposure.menu;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.item.AlbumItem;
import io.github.mortuusars.exposure.util.ItemAndStack;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;


public class LecternAlbumMenu extends AlbumMenu {
    public static final int TAKE_BOOK_BUTTON = 99;

    private final Inventory lectern;
    private final BlockPos lecternPos;
    private final World level;

    public LecternAlbumMenu(int containerId, BlockPos lecternPos, PlayerInventory playerInventory,
                            ItemAndStack<AlbumItem> album, Inventory lectern, PropertyDelegate lecternData) {
        this(Exposure.MenuTypes.LECTERN_ALBUM.get(), containerId, lecternPos, playerInventory, album, lectern, lecternData);
    }

    protected LecternAlbumMenu(ScreenHandlerType<? extends ScreenHandler> type, int containerId, BlockPos lecternPos,
                               PlayerInventory playerInventory, ItemAndStack<AlbumItem> album, Inventory lectern, PropertyDelegate lecternData) {
        super(type, containerId, playerInventory, new ItemAndStack<>(album.getStack()), false);
        checkSize(lectern, 1);
        checkDataCount(lecternData, 1);
        this.lecternPos = lecternPos;
        this.lectern = lectern;
        this.level = playerInventory.player.getWorld();
        this.addSlot(new Slot(lectern, 0, -999, -999) {
            @Override
            public void markDirty() {
                super.markDirty();
                LecternAlbumMenu.this.onContentChanged(this.inventory);
            }
        });
        this.addProperties(lecternData);
        setCurrentSpreadIndex(lecternData.get(0));
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id == TAKE_BOOK_BUTTON) {
            if (!player.canModifyBlocks())
                return false;

            ItemStack albumStack = this.lectern.removeStack(0);
            this.lectern.markDirty();
            if (!player.getInventory().insertStack(albumStack))
                player.dropItem(albumStack, false);

            return true;
        }

        return super.onButtonClick(player, id);
    }

    @Override
    public void setCurrentSpreadIndex(int spreadIndex) {
        spreadIndex = Math.max(0, spreadIndex);
        super.setCurrentSpreadIndex(spreadIndex);
        setProperty(1, spreadIndex);
    }

    @Override
    public void setProperty(int id, int data) {
        super.setProperty(id, data);
        this.sendContentUpdates();
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return level.getBlockEntity(lecternPos) instanceof LecternBlockEntity lecternBlockEntity
                && lecternBlockEntity.getBook().getItem() instanceof AlbumItem
                && Inventory.canPlayerUse(lecternBlockEntity, player);
    }

    public static LecternAlbumMenu fromBuffer(int containerId, PlayerInventory inventory, PacketByteBuf buffer) {
        return new LecternAlbumMenu(containerId, buffer.readBlockPos(), inventory,
                new ItemAndStack<>(buffer.readItemStack()), new SimpleInventory(1), new ArrayPropertyDelegate(1));
    }
}
