package me.dantaeusb.zetter.capability.paintingregistry;

import me.dantaeusb.zetter.core.ZetterCapabilities;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PaintingRegistryProvider implements ICapabilitySerializable<CompoundNBT> {
    private final PaintingRegistry paintingRegistryCapability;

    private final String TAG_NAME_PAINTING_REGISTRY = "PaintingRegistry";

    public PaintingRegistryProvider(World world) {
        if (!world.isClientSide()) {
            this.paintingRegistryCapability = new PaintingRegistry(world);
        } else {
            throw new IllegalArgumentException("Painting Registry should exist only on server in overworld");
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
    @Nullable
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        if (ZetterCapabilities.PAINTING_REGISTRY == capability) {
            return (LazyOptional<T>)LazyOptional.of(()-> this.paintingRegistryCapability);
        }

        return LazyOptional.empty();
    }

    /**
     * Write all the capability state information to NBT
     * We need to save data only for Server Implementation of the capability
     */
    public CompoundNBT serializeNBT() {
        CompoundNBT compoundTag = new CompoundNBT();

        if (this.paintingRegistryCapability.getWorld() == null || this.paintingRegistryCapability.getWorld().isClientSide()) {
            return compoundTag;
        }

        INBT paintingRegistryTag = this.paintingRegistryCapability.serializeNBT();
        compoundTag.put(TAG_NAME_PAINTING_REGISTRY, paintingRegistryTag);

        return compoundTag;
    }

    /**
     * Read the capability state information out of NBT
     * We need to get the data only for Server Implementation of the capability
     */
    public void deserializeNBT(CompoundNBT compoundTag) {
        if (this.paintingRegistryCapability.getWorld() == null || this.paintingRegistryCapability.getWorld().isClientSide()) {
            return;
        }

        INBT paintingRegistryTag = compoundTag.get(TAG_NAME_PAINTING_REGISTRY);
        this.paintingRegistryCapability.deserializeNBT(paintingRegistryTag);
    }
}
