package me.dantaeusb.zetter.capability.paintingregistry;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Painting registry can be used to control
 * and moderate painting that exist on server.
 * It is just a list of strings.
 *
 * @todo: [HIGH] Serializer needs more love, it's poor
 */
public class PaintingRegistry implements INBTSerializable<CompoundTag> {
    private final Level world;
    private ArrayList<String> paintingCanvasCodeList = new ArrayList<>();

    public PaintingRegistry(Level world) {
        super();

        this.world = world;
    }

    /**
     * World accessor for canvas tracker
     *
     * @return
     */
    public Level getWorld() {
        return this.world;
    }

    public void addPaintingCanvasCode(String canvasCode) {
        this.paintingCanvasCodeList.add(canvasCode);
    }

    public List<String> getPaintingCanvasCodes() {
        return Collections.unmodifiableList(this.paintingCanvasCodeList);
    }

    // Needed because serializable constructor has to accept empty/default. But we need level.
    // Wait, let's see: we can set it via setter or constructor.
    // Let's implement setter or let the builder handle level instantiation.
    private Level level;
    public void setWorld(Level world) {
        this.level = world;
    }

    @Override
    public CompoundTag serializeNBT(net.minecraft.core.HolderLookup.Provider provider) {
        CompoundTag compoundTag = new CompoundTag();

        if (this.getWorld() == null || this.getWorld().isClientSide()) {
            return compoundTag;
        }

        Tag paintingRegistryTag = PaintingRegistryStorage.save(this);
        compoundTag.put("PaintingRegistry", paintingRegistryTag);

        return compoundTag;
    }

    @Override
    public void deserializeNBT(net.minecraft.core.HolderLookup.Provider provider, CompoundTag compoundTag) {
        if (this.getWorld() == null || this.getWorld().isClientSide()) {
            return;
        }

        Tag paintingRegistryTag = compoundTag.get("PaintingRegistry");

        if (paintingRegistryTag == null) {
            return;
        }

        PaintingRegistryStorage.load(this, paintingRegistryTag);
    }
}
