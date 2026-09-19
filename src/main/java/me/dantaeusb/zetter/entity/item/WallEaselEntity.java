package me.dantaeusb.zetter.entity.item;

import com.mojang.math.Axis;
import me.dantaeusb.zetter.core.ZetterItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Wall easel: a 5 blocks tall rack that holds canvases up to 4x4 blocks
 * standing straight on the ledge of the bottom plank.
 *
 * Unlike the standing easel it does not lean, and it is wide enough that
 * its bounding box has to follow the rotation of the entity.
 */
public class WallEaselEntity extends CanvasHolderEntity {
    private static final int[] MAX_CANVAS_BLOCK_SIZE = new int[]{4, 4};

    /**
     * Middle of the rack, right above the ledge of the bottom plank.
     * Canvas leans on the planks, which start one pixel behind it.
     */
    private static final Vector3f CANVAS_ANCHOR = new Vector3f(0.0f, 0.3125f, 0.0f);

    /*
     * Model bounds, blocks. Model space, so Z is pointing away
     * from the front of the easel, see WallEaselModel
     */
    private static final float MODEL_HALF_WIDTH = 2.25f;
    private static final float MODEL_HEIGHT = 5.0f;
    private static final float MODEL_FRONT = -0.25f;
    private static final float MODEL_BACK = 0.4375f;

    public WallEaselEntity(EntityType<? extends WallEaselEntity> type, Level world) {
        super(type, world);
    }

    @Override
    protected Vector3f getCanvasAnchor(int blockWidth, int blockHeight) {
        return new Vector3f(CANVAS_ANCHOR);
    }

    @Override
    protected float getCanvasLean() {
        return 0.0f;
    }

    @Override
    public int[] getMaxCanvasBlockSize() {
        return MAX_CANVAS_BLOCK_SIZE;
    }

    @Override
    protected Item getHolderItem() {
        return ZetterItems.WALL_EASEL.get();
    }

    @Override
    public void setYRot(float yRot) {
        super.setYRot(yRot);

        this.setBoundingBox(this.makeBoundingBox());
    }

    @Override
    protected AABB makeBoundingBox() {
        return makeBoundingBox(this.position(), this.getYRot());
    }

    /**
     * Easel is way wider than it is deep, so instead of using entity dimensions
     * we rotate the model bounds the same way the renderer rotates the model and
     * take the box that contains them.
     *
     * @param position entity position, middle of the bottom edge of the easel
     * @param yRot entity rotation
     */
    public static AABB makeBoundingBox(Vec3 position, float yRot) {
        final Quaternionf entityRotation = Axis.YP.rotationDegrees(180.0F - yRot);

        float minX = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for (float modelX : new float[]{-MODEL_HALF_WIDTH, MODEL_HALF_WIDTH}) {
            for (float modelZ : new float[]{MODEL_FRONT, MODEL_BACK}) {
                Vector3f corner = entityRotation.transform(new Vector3f(modelX, 0.0f, modelZ));

                minX = Math.min(minX, corner.x());
                minZ = Math.min(minZ, corner.z());
                maxX = Math.max(maxX, corner.x());
                maxZ = Math.max(maxZ, corner.z());
            }
        }

        return new AABB(
            position.x() + minX, position.y(), position.z() + minZ,
            position.x() + maxX, position.y() + MODEL_HEIGHT, position.z() + maxZ
        );
    }
}
