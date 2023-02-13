package me.dantaeusb.zetter.capability.canvastracker;

import me.dantaeusb.zetter.core.ZetterCapabilities;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CanvasTrackerProvider implements ICapabilitySerializable<CompoundNBT> {
    private final Direction NO_SPECIFIC_SIDE = null;
    private final CanvasTracker canvasTrackerCapability;

    /**
     * @todo: datafix to CanvasTracker
     */
    private final String TAG_NAME_CANVAS_TRACKER = "canvasTracker";

    public CanvasTrackerProvider(World world) {
        if (world.isClientSide()) {
            this.canvasTrackerCapability = new CanvasClientTracker();
        } else {
            this.canvasTrackerCapability = new CanvasServerTracker();
        }

        this.canvasTrackerCapability.setLevel(world);
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
            return (LazyOptional<T>)LazyOptional.of(()-> this.canvasTrackerCapability);
        }

        return LazyOptional.empty();
    }

    /**
     * Write all the capability state information to NBT
     * We need to save data only for Server Implementation of the capability
     */
    public CompoundNBT serializeNBT() {
        CompoundNBT compoundTag = new CompoundNBT();

        if (this.canvasTrackerCapability.getLevel() == null || this.canvasTrackerCapability.getLevel().isClientSide()) {
            return compoundTag;
        }

        INBT canvasTrackerTag = ((CanvasServerTracker) this.canvasTrackerCapability).serializeNBT();
        compoundTag.put(TAG_NAME_CANVAS_TRACKER, canvasTrackerTag);

        return compoundTag;
    }

    /**
     * Read the capability state information out of NBT
     * We need to get the data only for Server Implementation of the capability
     */
    public void deserializeNBT(CompoundNBT compoundTag) {
        if (this.canvasTrackerCapability.getLevel() == null || this.canvasTrackerCapability.getLevel().isClientSide()) {
            return;
        }

        INBT canvasTrackerTag = compoundTag.get(TAG_NAME_CANVAS_TRACKER);
        ((CanvasServerTracker) this.canvasTrackerCapability).deserializeNBT(canvasTrackerTag);
    }
}
