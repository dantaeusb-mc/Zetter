package me.dantaeusb.zetter.capability.canvastracker;

import me.dantaeusb.zetter.core.ZetterCapabilities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CanvasTrackerProvider implements ICapabilitySerializable<CompoundTag> {
    private final Direction NO_SPECIFIC_SIDE = null;
    private final CanvasTracker canvasTracker;

    /**
     * @todo: datafix to CanvasTracker
     */
    private final String TAG_NAME_CANVAS_TRACKER = "canvasTracker";

    public CanvasTrackerProvider(Level world) {
        if (world.isClientSide()) {
            this.canvasTracker = new CanvasClientTracker();
        } else {
            this.canvasTracker = new CanvasServerTracker();
        }

        this.canvasTracker.setLevel(world);
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
        if (ZetterCapabilities.CANVAS_TRACKER == capability) {
            return (LazyOptional<T>)LazyOptional.of(()-> this.canvasTracker);
        }

        return LazyOptional.empty();
    }

    /**
     * Write all the capability state information to NBT
     * We need to save data only for Server Implementation of the capability
     */
    public CompoundTag serializeNBT() {
        CompoundTag compoundTag = new CompoundTag();

        if (this.canvasTracker.getLevel() == null || this.canvasTracker.getLevel().isClientSide()) {
            return compoundTag;
        }

        Tag canvasTrackerTag = CanvasTrackerStorage.save(this.canvasTracker);
        compoundTag.put(TAG_NAME_CANVAS_TRACKER, canvasTrackerTag);

        return compoundTag;
    }

    /**
     * Read the capability state information out of NBT
     * We need to get the data only for Server Implementation of the capability
     */
    public void deserializeNBT(CompoundTag compoundTag) {
        if (this.canvasTracker.getLevel() == null || this.canvasTracker.getLevel().isClientSide()) {
            return;
        }

        Tag canvasTrackerTag = compoundTag.get(TAG_NAME_CANVAS_TRACKER);

        if (canvasTrackerTag == null) {
            return;
        }

        CanvasTrackerStorage.load(this.canvasTracker, canvasTrackerTag);
    }
}
