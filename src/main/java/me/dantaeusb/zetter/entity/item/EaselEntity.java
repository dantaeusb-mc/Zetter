package me.dantaeusb.zetter.entity.item;

import me.dantaeusb.zetter.core.ZetterItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/**
 * Standing easel: a small rack that holds canvases up to 2x2 blocks,
 * leaning back like a real easel does.
 */
public class EaselEntity extends CanvasHolderEntity {
    private static final int[] MAX_CANVAS_BLOCK_SIZE = new int[]{2, 2};

    /**
     * Middle of the rack, right above the ledge the canvas stands on
     */
    private static final Vector3f CANVAS_ANCHOR = new Vector3f(0.0f, 0.78125f, -0.25f);

    /**
     * Same lean as the rack of the model has, 10 degrees
     */
    private static final float CANVAS_LEAN = 0.1745f;

    public EaselEntity(EntityType<? extends EaselEntity> type, Level world) {
        super(type, world);
    }

    @Override
    protected Vector3f getCanvasAnchor(int blockWidth, int blockHeight) {
        return new Vector3f(CANVAS_ANCHOR);
    }

    @Override
    protected float getCanvasLean() {
        return CANVAS_LEAN;
    }

    @Override
    public int[] getMaxCanvasBlockSize() {
        return MAX_CANVAS_BLOCK_SIZE;
    }

    @Override
    protected Item getHolderItem() {
        return ZetterItems.EASEL.get();
    }
}
