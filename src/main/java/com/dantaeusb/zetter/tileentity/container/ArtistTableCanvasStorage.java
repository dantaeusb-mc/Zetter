package com.dantaeusb.zetter.tileentity.container;

import com.dantaeusb.zetter.menu.ArtistTableMenu;
import com.dantaeusb.zetter.core.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.items.ItemStackHandler;

import java.util.function.Predicate;

/**
 * This class was copied from a tutorial and I believe is no longer needed, better migrate to
 * SimpleContainer. Currently we have a 3-level system of storing ItemStacks
 * causing us to do a lot of unnecessary calls.
 * Either use SimpleContainer or ItemStackHandler directly
 */

public class ArtistTableCanvasStorage implements Container {
    public static final int STORAGE_SIZE = ArtistTableMenu.CANVAS_SLOT_COUNT;

    private final ItemStackHandler stackHandler;

    public static ArtistTableCanvasStorage createForTileEntity(Predicate<Player> canPlayerAccessInventoryLambda,
                                                               Notify markDirtyNotificationLambda) {
        return new ArtistTableCanvasStorage(canPlayerAccessInventoryLambda, markDirtyNotificationLambda);
    }

    public static ArtistTableCanvasStorage createForClientSideContainer() {
        return new ArtistTableCanvasStorage();
    }

    private ArtistTableCanvasStorage() {
        this.stackHandler = new ItemStackHandler(STORAGE_SIZE) {

        };
    }

    private ArtistTableCanvasStorage(Predicate<Player> canPlayerAccessInventoryLambda, Notify markDirtyNotificationLambda) {
        this();
        this.canPlayerAccessInventoryLambda = canPlayerAccessInventoryLambda;
        this.markDirtyNotificationLambda = markDirtyNotificationLambda;
    }

    public CompoundTag serializeNBT()  {
        return stackHandler.serializeNBT();
    }

    public void deserializeNBT(CompoundTag nbt)   {
        stackHandler.deserializeNBT(nbt);
    }

    public void setCanPlayerAccessInventoryLambda(Predicate<Player> canPlayerAccessInventoryLambda) {
        this.canPlayerAccessInventoryLambda = canPlayerAccessInventoryLambda;
    }

    public void setMarkDirtyNotificationLambda(Notify markDirtyNotificationLambda) {
        this.markDirtyNotificationLambda = markDirtyNotificationLambda;
    }

    public void setOpenInventoryNotificationLambda(Notify openInventoryNotificationLambda) {
        this.openInventoryNotificationLambda = openInventoryNotificationLambda;
    }

    public void setCloseInventoryNotificationLambda(Notify closeInventoryNotificationLambda) {
        this.closeInventoryNotificationLambda = closeInventoryNotificationLambda;
    }

    @Override
    public boolean stillValid(Player player) {
        return canPlayerAccessInventoryLambda.test(player);  // on the client, this does nothing. on the server, ask our parent TileEntity.
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        if (stack.getItem() == ModItems.CANVAS) {
            return stackHandler.isItemValid(index, stack);
        }

        return false;
    }

    @FunctionalInterface
    public interface Notify {   // Some folks use Runnable, but I prefer not to use it for non-thread-related tasks
        void invoke();
    }

    @Override
    public void setChanged() {
        markDirtyNotificationLambda.invoke();
    }

    @Override
    public void startOpen(Player player) {
        openInventoryNotificationLambda.invoke();
    }

    @Override
    public void stopOpen(Player player) {
        closeInventoryNotificationLambda.invoke();
    }

    //---------These following methods are called by Vanilla container methods to manipulate the inventory contents ---

    @Override
    public int getContainerSize() {
        return stackHandler.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < stackHandler.getSlots(); ++i) {
            if (!stackHandler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        return stackHandler.getStackInSlot(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        return stackHandler.extractItem(index, count, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        int maxPossibleItemStackSize = stackHandler.getSlotLimit(index);
        return stackHandler.extractItem(index, maxPossibleItemStackSize, false);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        stackHandler.setStackInSlot(index, stack);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < stackHandler.getSlots(); ++i) {
            stackHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    private Predicate<Player> canPlayerAccessInventoryLambda = x-> true;

    private Notify markDirtyNotificationLambda = ()->{};

    private Notify openInventoryNotificationLambda = ()->{};

    private Notify closeInventoryNotificationLambda = ()->{};
}
