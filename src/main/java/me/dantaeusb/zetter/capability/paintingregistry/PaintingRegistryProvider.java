package me.dantaeusb.zetter.capability.paintingregistry;

import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PaintingRegistryProvider implements ICapabilitySerializable<INBT> {
    private final PaintingRegistry paintingRegistry;

    private final String TAG_NAME_PAINTING_REGISTRY = "PaintingRegistry";

    public PaintingRegistryProvider(World world) {
        if (!world.isClientSide()) {
            PaintingRegistry paintingRegistryCapability = new PaintingRegistry();
            paintingRegistryCapability.setLevel(world);

            this.paintingRegistry = paintingRegistryCapability;
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
        if (PaintingRegistryCapability.CAPABILITY_PAINTING_REGISTRY == capability) {
            return (LazyOptional<T>)LazyOptional.of(()-> this.paintingRegistry);
        }

        return LazyOptional.empty();
    }

    /**
     * Write all the capability state information to NBT
     * We need to save data only for Server Implementation of the capability
     */
    public INBT serializeNBT() {
        return PaintingRegistryCapability.CAPABILITY_PAINTING_REGISTRY.getStorage().writeNBT(
            PaintingRegistryCapability.CAPABILITY_PAINTING_REGISTRY,
            this.paintingRegistry,
            null
        );
    }

    /**
     * Read the capability state information out of NBT
     * We need to get the data only for Server Implementation of the capability
     */
    public void deserializeNBT(INBT tag) {
        PaintingRegistryCapability.CAPABILITY_PAINTING_REGISTRY.getStorage().readNBT(
            PaintingRegistryCapability.CAPABILITY_PAINTING_REGISTRY,
            this.paintingRegistry,
            null,
            tag
        );
    }
}
