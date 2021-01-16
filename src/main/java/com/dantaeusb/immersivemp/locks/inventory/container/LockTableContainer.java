package com.dantaeusb.immersivemp.locks.inventory.container;

import com.dantaeusb.immersivemp.locks.core.ModLockContainers;
import com.dantaeusb.immersivemp.locks.core.ModLockItems;
import com.dantaeusb.immersivemp.locks.item.ILockingItem;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

public class LockTableContainer extends Container {
    public static final int MAX_NAME_LENGTH = 35 - 4;

    protected final IInventory inventoryIn = new Inventory(2) {
        /**
         * @see net.minecraft.inventory.container.RepairContainer#field_234643_d_
         */
        public void markDirty() {
            super.markDirty();
            LockTableContainer.this.onCraftMatrixChanged(this);
        }
    };
    protected final CraftResultInventory inventoryOut = new CraftResultInventory();

    private static final int IN_MATERIAL_SLOT = 0;
    private static final int IN_TEMPLATE_SLOT = 1;

    private String lockName;
    private boolean keyMode = true;

    private final World world;
    private static final Logger LOGGER = LogManager.getLogger();
    private int materialCost = 1;

    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;

    // gui position of the player inventory grid
    public static final int PLAYER_INVENTORY_XPOS = 8;
    public static final int PLAYER_INVENTORY_YPOS = 84;

    protected final IWorldPosCallable worldPosCallable;

    public LockTableContainer(int id, PlayerInventory playerInventory) {
        this(id, playerInventory, IWorldPosCallable.DUMMY);
    }

    public LockTableContainer(int windowID, PlayerInventory invPlayer, IWorldPosCallable worldPosCallable) {
        super(ModLockContainers.LOCK_TABLE, windowID);

        if (ModLockContainers.LOCK_TABLE == null)
            throw new IllegalStateException("Must initialise containerTypeLockTableContainer before constructing a LockTableContainer!");

        this.worldPosCallable = worldPosCallable;
        this.world = invPlayer.player.world;

        this.addSlot(new Slot(this.inventoryIn, IN_MATERIAL_SLOT, 37, 47) {
            public boolean isItemValid(ItemStack stack) {
                return stack.getItem() == Items.IRON_INGOT;
            }
        });

        this.addSlot(new Slot(this.inventoryIn, IN_TEMPLATE_SLOT, 124, 47){
            public boolean isItemValid(ItemStack stack) {
                return stack.getItem() == ModLockItems.KEY_ITEM;
            }
        });

        this.addSlot(new Slot(this.inventoryOut, 2, 95, 47)  {
            public boolean isItemValid(ItemStack stack) {
                return false;
            }

            public ItemStack onTake(PlayerEntity playerIn, ItemStack stack) {
                return LockTableContainer.this.createLock(playerIn, stack);
            }
        });

        final int SLOT_X_SPACING = 18;
        final int SLOT_Y_SPACING = 18;
        final int HOTBAR_XPOS = 8;
        final int HOTBAR_YPOS = 142;

        // Add the players hotbar to the gui - the [xpos, ypos] location of each item
        for (int x = 0; x < HOTBAR_SLOT_COUNT; x++) {
            int slotNumber = x;
            addSlot(new Slot(invPlayer, slotNumber, HOTBAR_XPOS + SLOT_X_SPACING * x, HOTBAR_YPOS));
        }

        // Add the rest of the players inventory to the gui
        for (int y = 0; y < PLAYER_INVENTORY_ROW_COUNT; y++) {
            for (int x = 0; x < PLAYER_INVENTORY_COLUMN_COUNT; x++) {
                int slotNumber = HOTBAR_SLOT_COUNT + y * PLAYER_INVENTORY_COLUMN_COUNT + x;
                int xpos = PLAYER_INVENTORY_XPOS + x * SLOT_X_SPACING;
                int ypos = PLAYER_INVENTORY_YPOS + y * SLOT_Y_SPACING;
                addSlot(new Slot(invPlayer, slotNumber,  xpos, ypos));
            }
        }
    }

    public static LockTableContainer createContainerServerSide(int windowID, PlayerInventory playerInventory) {
        return new LockTableContainer(windowID, playerInventory);
    }

    public static LockTableContainer createContainerClientSide(int windowID, PlayerInventory playerInventory, net.minecraft.network.PacketBuffer extraData) {
        return new LockTableContainer(windowID, playerInventory);
    }

    protected ItemStack createLock(PlayerEntity playerIn, ItemStack outStack) {
        ItemStack materialStack = this.inventoryIn.getStackInSlot(IN_MATERIAL_SLOT);

        if (!materialStack.isEmpty() && materialStack.getCount() > this.materialCost) {
            materialStack.shrink(this.materialCost);
            this.inventoryIn.setInventorySlotContents(IN_MATERIAL_SLOT, materialStack);
        } else {
            this.inventoryIn.setInventorySlotContents(IN_MATERIAL_SLOT, ItemStack.EMPTY);
        }

        return outStack;
    }

    /**
     * Callback for when the crafting matrix is changed.
     */
    public void onCraftMatrixChanged(IInventory inventoryIn) {
        super.onCraftMatrixChanged(inventoryIn);
        if (inventoryIn == this.inventoryIn) {
            this.updateOutput();
        }
    }

    public void updateOutput() {
        ItemStack materialStack = this.inventoryIn.getStackInSlot(IN_MATERIAL_SLOT);

        if (materialStack.isEmpty()) {
            this.inventoryOut.setInventorySlotContents(0, ItemStack.EMPTY);
        } else {
            ItemStack templateStack = this.inventoryIn.getStackInSlot(IN_TEMPLATE_SLOT);
            ItemStack outStack;

            if (this.keyMode) {
                outStack = new ItemStack(ModLockItems.KEY_ITEM);
            } else {
                outStack = new ItemStack(ModLockItems.LOCK_ITEM);
            }

            if (templateStack.isEmpty()) {
                ILockingItem.setLockId(outStack, UUID.randomUUID());
            } else {
                ILockingItem.setLockId(outStack, ILockingItem.getLockId(templateStack));
            }

            if (!StringUtils.isBlank(this.lockName)) {
                outStack.setDisplayName(new StringTextComponent(this.lockName));
            }

            this.inventoryOut.setInventorySlotContents(0, outStack);
            this.detectAndSendChanges();
        }
    }

    /**
     * used by the Lock Table GUI to update the Item Name being typed by the player
     */
    public void updateItemName(String newName) {
        this.lockName = newName;

        if (this.getSlot(2).getHasStack()) {
            ItemStack outStack = this.getSlot(2).getStack();
            if (StringUtils.isBlank(newName)) {
                outStack.clearCustomName();
            } else {
                outStack.setDisplayName(new StringTextComponent(this.lockName));
            }
        }

        this.updateOutput();
    }

    public void updateKeyMode(boolean keyMode) {
        this.keyMode = keyMode;

        LOGGER.debug("Updated key mode!");

        this.updateOutput();
    }

    @Override
    public ItemStack transferStackInSlot(PlayerEntity playerIn, int sourceSlotIndex)
    {
        ItemStack outStack = ItemStack.EMPTY;
        Slot sourceSlot = this.inventorySlots.get(sourceSlotIndex);

        if (sourceSlot != null && sourceSlot.getHasStack()) {
            ItemStack sourceStack = sourceSlot.getStack();
            outStack = sourceStack.copy();

            // Output
            if (sourceSlotIndex == 2) {
                if (!this.mergeItemStack(sourceStack, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }

                sourceSlot.onSlotChange(sourceStack, outStack);

            // Not Inputs
            } else if (sourceSlotIndex != 0 && sourceSlotIndex != 1) {
                if (sourceStack.getItem() == Items.IRON_INGOT) {
                    if (!this.mergeItemStack(sourceStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (sourceStack.getItem() == ModLockItems.KEY_ITEM) {
                    if (!this.mergeItemStack(sourceStack, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (sourceSlotIndex >= 3 && sourceSlotIndex < 30) {
                    if (!this.mergeItemStack(sourceStack, 30, 39, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (sourceSlotIndex >= 30 && sourceSlotIndex < 39 && !this.mergeItemStack(sourceStack, 3, 30, false)) {
                    return ItemStack.EMPTY;
                }
            // Inputs
            } else if (!this.mergeItemStack(sourceStack, 3, 39, false)) {
                return ItemStack.EMPTY;
            }

            if (sourceStack.isEmpty()) {
                sourceSlot.putStack(ItemStack.EMPTY);
            } else {
                sourceSlot.onSlotChanged();
            }

            if (sourceStack.getCount() == outStack.getCount()) {
                return ItemStack.EMPTY;
            }

            sourceSlot.onTake(playerIn, sourceStack);
        }

        return outStack;
    }

    /**
     * Called when the container is closed.
     */
    public void onContainerClosed(PlayerEntity playerIn) {
        super.onContainerClosed(playerIn);
        this.worldPosCallable.consume((worldIn, blockPos) -> {
            this.clearContainer(playerIn, worldIn, this.inventoryIn);
        });
    }

    /**
     * Determines whether supplied player can use this container
     */
    public boolean canInteractWith(PlayerEntity playerIn) {
        return this.worldPosCallable.applyOrElse((worldPosConsumer, defaultValue) -> {
            return !this.isAnLockTable(worldPosConsumer.getBlockState(defaultValue)) ? false : playerIn.getDistanceSq((double)defaultValue.getX() + 0.5D, (double)defaultValue.getY() + 0.5D, (double)defaultValue.getZ() + 0.5D) <= 64.0D;
        }, true);
    }

    /**
     * @todo Use BlockTags {@link net.minecraft.inventory.container.RepairContainer#func_230302_a_}
     * @param blockState
     * @return
     */
    protected boolean isAnLockTable(BlockState blockState) {
        return true;
    }
}
