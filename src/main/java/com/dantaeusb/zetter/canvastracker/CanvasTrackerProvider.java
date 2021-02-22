package com.dantaeusb.zetter.canvastracker;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CanvasTrackerProvider implements ICapabilitySerializable<CompoundNBT> {
    private final Direction NO_SPECIFIC_SIDE = null;
    private final ICanvasTracker canvasTracker;

    private final String NBT_TAG_NAME_CANVAS_TRACKER = "canvasTracker";

    public CanvasTrackerProvider(World world) {
        if (world.isRemote()) {
            this.canvasTracker = new CanvasClientTracker(world);
        } else {
            this.canvasTracker = new CanvasServerTracker(world);
        }
    }

    /**
     * Asks the Provider if it has the given capability
     * @param capability<T> capability to be checked for
     * @param facing the side of the provider being checked (null = no particular side)
     * @param <T> The interface instance that is used
     * @return a lazy-initialisation supplier of the interface instance that is used to access this capability
     *         In this case, we don't actually use lazy initialisation because the instance is very quick to create.
     *         See CapabilityProviderFlowerBag for an example of lazy initialisation
     */
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        if (CanvasTrackerCapability.CAPABILITY_CANVAS_TRACKER == capability) {
            return (LazyOptional<T>)LazyOptional.of(()-> this.canvasTracker);
        }

        return LazyOptional.empty();
    }

    /**
     * Write all the capability state information to NBT
     * We need to save data only for Server Implementation of the capability
     */
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();

        if (this.canvasTracker.getWorld() == null || this.canvasTracker.getWorld().isRemote()) {
            return nbt;
        }

        Capability<ICanvasTracker> test = CanvasTrackerCapability.CAPABILITY_CANVAS_TRACKER;
        INBT canvasTrackerNBT = test.writeNBT(this.canvasTracker, NO_SPECIFIC_SIDE);
        nbt.put(NBT_TAG_NAME_CANVAS_TRACKER, canvasTrackerNBT);

        return nbt;
    }

    /**
     * Read the capability state information out of NBT
     * We need to get the data only for Server Implementation of the capability
     */
    public void deserializeNBT(CompoundNBT nbt) {
        if (this.canvasTracker.getWorld() == null || this.canvasTracker.getWorld().isRemote()) {
            return;
        }

        INBT canvasTrackerNBT = nbt.get(NBT_TAG_NAME_CANVAS_TRACKER);
        CanvasTrackerCapability.CAPABILITY_CANVAS_TRACKER.readNBT(this.canvasTracker, NO_SPECIFIC_SIDE, canvasTrackerNBT);
    }
}
