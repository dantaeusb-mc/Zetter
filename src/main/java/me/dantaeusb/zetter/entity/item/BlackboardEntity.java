package me.dantaeusb.zetter.entity.item;

import com.mojang.math.Axis;
import me.dantaeusb.zetter.core.ZetterItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Blackboard: a framed slate that hangs flat on a wall and is drawn on in place
 * with chalk, rather than by opening a screen.
 */
public class BlackboardEntity extends AbstractBoardEntity {
    /**
     * A board is a piece of furniture rather than a canvas cut to order, so it is
     * always this size and the slate texture is drawn to match
     */
    public static final int BLOCK_WIDTH = 3;
    public static final int BLOCK_HEIGHT = 2;

    private static final int[] MAX_CANVAS_BLOCK_SIZE = new int[]{BLOCK_WIDTH, BLOCK_HEIGHT};

    /*
     * Model bounds, blocks. Model space, so Z is pointing away from the front of the
     * board: the back sits on the block boundary, flat against the wall, and the
     * frame stands one pixel proud of it.
     */
    private static final float MODEL_HALF_WIDTH = BLOCK_WIDTH / 2.0f;
    private static final float MODEL_HEIGHT = BLOCK_HEIGHT;
    private static final float MODEL_FRONT = 0.4375f;
    private static final float MODEL_BACK = 0.5f;

    /**
     * How far behind the front of the frame the chalk sits, block pixels.
     *
     * The drawing is on slate held inside the frame, not laid over the front of it,
     * so the frame stands proud of the chalk and the edge of the board reads as a
     * lip rather than as a line. It also keeps the slate clear of the frame's front
     * face, which the two were within a thousandth of a block of sharing.
     */
    private static final float CANVAS_INSET = 0.125f;

    /**
     * Middle of the bottom edge of the slate, set back into the frame
     */
    private static final Vector3f CANVAS_ANCHOR = new Vector3f(0.0f, 0.0f, MODEL_FRONT + CANVAS_INSET / 16.0f);

    private static final float[] CANVAS_MARGINS = new float[]{2.5f, 2.5f};

    public BlackboardEntity(EntityType<? extends BlackboardEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected Vector3f getCanvasAnchor(int blockWidth, int blockHeight) {
        return new Vector3f(CANVAS_ANCHOR);
    }

    /**
     * Hangs flat against the wall
     */
    @Override
    protected float getCanvasLean() {
        return 0.0f;
    }

    @Override
    public int[] getMaxCanvasBlockSize() {
        return MAX_CANVAS_BLOCK_SIZE;
    }

    /**
     * The board's frame is a block pixel deep all round, which is two canvas pixels
     * at this resolution, and half a pixel more keeps the smallest brush from
     * reaching under it
     */
    @Override
    protected float[] getCanvasMargins() {
        return CANVAS_MARGINS;
    }

    @Override
    protected Item getHolderItem() {
        return ZetterItems.BLACKBOARDS.get(this.getMaterial().toString()).get();
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
     * Rotated the way the renderer rotates the model, then boxed, the same way the
     * wall easel does it, see {@link WallEaselEntity#makeBoundingBox}
     *
     * @param position entity position, middle of the bottom edge of the board
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

    /**
     * A board hangs off the wall behind it rather than standing on a floor, so the
     * inherited check — which looks for something solid underneath, the way an easel
     * needs — would take down every board that was not resting on a ledge.
     *
     * @return
     */
    @Override
    public boolean survives() {
        if (!this.level().noCollision(this)) {
            return false;
        }

        if (!isSupported(this.level(), this.getPos(), this.getYRot())) {
            return false;
        }

        return this.level().getEntities(this, this.getBoundingBox(), (entity) -> entity.getType() == this.getType()).isEmpty();
    }

    /**
     * Whether there is a wall to hang on: every block the board covers needs one
     * behind it, the way a painting does, so a board cannot be left holding on by a
     * corner.
     *
     * Shared with the item so that a board is never placed somewhere it would fall
     * out of a few seconds later.
     *
     * @param level
     * @param origin block the board is centred on, its own block position
     * @param yRot entity rotation
     * @return
     */
    public static boolean isSupported(Level level, BlockPos origin, float yRot) {
        final Direction facing = Direction.fromYRot(yRot);
        final Direction behind = facing.getOpposite();
        final Direction across = facing.getClockWise();

        for (int row = 0; row < BLOCK_HEIGHT; row++) {
            for (int column = -(BLOCK_WIDTH / 2); column <= BLOCK_WIDTH / 2; column++) {
                final BlockPos support = origin.relative(across, column).above(row).relative(behind);

                if (!level.getBlockState(support).isSolid()) {
                    return false;
                }
            }
        }

        return true;
    }
}
