package com.dantaeusb.zetter.deprecated.block.entity;

import com.dantaeusb.zetter.deprecated.block.EaselBlock;
import com.dantaeusb.zetter.menu.EaselContainerMenu;
import com.dantaeusb.zetter.core.ModItems;
import com.dantaeusb.zetter.core.ModBlockEntities;
import com.dantaeusb.zetter.item.CanvasItem;
import com.dantaeusb.zetter.tileentity.container.EaselContainer;
import com.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Containers;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class EaselBlockEntity extends BlockEntity {
    private final EaselContainer easelContainer; // two items: canvas and palette

    private static final String EASEL_STORAGE_TAG = "storage";

    /** The list of players currently using this easel */
    private ArrayList<Player> playersUsing = new ArrayList<>();
    private int ticksSinceSync;

    public EaselBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EASEL_TILE_ENTITY, pos, state);

        this.easelContainer = new EaselContainer();
    }

    public boolean canPlayerAccessInventory(Player player) {
        if (this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        } else {
            return player.distanceToSqr((double)this.worldPosition.getX() + 0.5D, (double)this.worldPosition.getY() + 0.5D, (double)this.worldPosition.getZ() + 0.5D) <= 64.0D;
        }
    }

    public static void tick(Level world, BlockPos pos, BlockState state, EaselBlockEntity entity) {
        if (++entity.ticksSinceSync > 200) {
            entity.playersUsing = entity.calculatePlayersUsing();
        }
    }

    // specific

    public EaselContainer getEaselContainer() {
        return this.easelContainer;
    }

    public boolean hasCanvas() {
        ItemStack canvasStack = this.getCanvasStack();

        return !canvasStack.isEmpty();
    }

    public @Nullable ItemStack getCanvasStack() {
        return this.easelContainer.getItem(EaselContainer.CANVAS_SLOT);
    }

    public boolean putCanvasStack(ItemStack itemStack) {
        if (itemStack.getItem() != ModItems.CANVAS) {
            return false;
        }

        if (this.hasCanvas()) {
            return false;
        }

        // Initialize data if it's not yet
        CanvasItem.getCanvasData(itemStack, this.level);
        this.easelContainer.setCanvasStack(itemStack);

        return true;
    }

    /**
     * Returns current canvas name or empty string if no canvas assigned
     * @return
     */
    public String getCanvasName() {
        ItemStack canvasStack = this.getCanvasStack();

        if (canvasStack != null && canvasStack.isEmpty()) {
            return "";
        }

        return CanvasItem.getCanvasCode(canvasStack);
    }

    @Nullable
    public CanvasData getCanvasData() {
        ItemStack canvasStack = this.getCanvasStack();

        if (canvasStack.isEmpty() || canvasStack.getItem() != ModItems.CANVAS) {
            return null;
        }

        return  CanvasItem.getCanvasData(canvasStack, this.level);
    }

    // track using players to send packets

    public ArrayList<Player> calculatePlayersUsing() {
        ArrayList<Player> usingPlayers = new ArrayList<>();

        for(Player player : this.level.getEntitiesOfClass(Player.class, new AABB(this.worldPosition.offset(-5, -5, -5), this.worldPosition.offset(5, 5, 5)))) {
            if (player.containerMenu instanceof EaselContainerMenu) {
                EaselContainer storage = ((EaselContainerMenu)player.containerMenu).getEaselContainer();

                if (storage == this.getEaselContainer()) {
                    usingPlayers.add(player);
                }
            }
        }

        return usingPlayers;
    }

    public ArrayList<Player> getPlayersUsing() {
        return this.playersUsing;
    }

    // render

    @Override
    public AABB getRenderBoundingBox()
    {
        return new AABB(this.getBlockPos(), this.getBlockPos().offset(1, 2, 1));
    }

    // NBT stack

    @Override
    public CompoundTag save(CompoundTag compoundTag)
    {
        super.save(compoundTag);

        compoundTag.put(EASEL_STORAGE_TAG, this.easelContainer.createTag());

        return compoundTag;
    }

    @Override
    public void load(CompoundTag compoundTag)
    {
        super.load(compoundTag);

        this.easelContainer.fromTag(compoundTag.getList(EASEL_STORAGE_TAG, 10));
    }

    // network stack

    @Override
    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {
        CompoundTag nbtTagCompound = new CompoundTag();
        save(nbtTagCompound);

        int tileEntityType = 42;
        return new ClientboundBlockEntityDataPacket(this.worldPosition, tileEntityType, nbtTagCompound);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        load(packet.getTag());
    }

    @Override
    public CompoundTag getUpdateTag()
    {
        CompoundTag nbtTagCompound = new CompoundTag();
        save(nbtTagCompound);
        return nbtTagCompound;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag)
    {
        this.load(tag);
    }

    /**
     * When this tile entity is destroyed, drop all of its contents into the world
     * @param world
     * @param blockPos
     */
    public void dropAllContents(Level world, BlockPos blockPos) {
        Containers.dropContents(world, blockPos, this.easelContainer);
    }
}
