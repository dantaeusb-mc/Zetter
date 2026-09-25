package me.dantaeusb.zetter.entity.item;

import me.dantaeusb.zetter.core.ZetterItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/**
 * Stand board: a small slate on an A-frame that stands on the floor, tilted back,
 * and is drawn on in place with chalk the same way as a blackboard.
 *
 * The slate is a block wide in canvas terms, but only fourteen pixels of it by
 * fifteen show between the posts and bars of the frame; the rest of the canvas
 * runs on under them.
 */
public class StandBoardEntity extends AbstractBoardEntity {
    public static final int BLOCK_WIDTH = 1;
    public static final int BLOCK_HEIGHT = 1;

    private static final int[] MAX_CANVAS_BLOCK_SIZE = new int[]{BLOCK_WIDTH, BLOCK_HEIGHT};

    /**
     * Lean of the front of the stand, and the slate with it, same as the model
     */
    private static final float CANVAS_LEAN = (float) Math.toRadians(15.0D);

    /*
     * Model geometry, block pixels, before the lean. The front of the stand swings
     * back from the hinge at the top; the slate hangs below it, between the top bar
     * and the ledge, and is fifteen pixels tall, so the sixteen of the canvas are
     * centred on it with half a pixel under each bar.
     */
    private static final float HINGE_HEIGHT = 23.0f;
    private static final float CANVAS_BOTTOM_BELOW_HINGE = 16.5f;

    /**
     * How far in front of the slate the chalk sits, block pixels: clear of the slate
     * face, and well behind the front of the frame, which stands a pixel proud of it
     */
    private static final float CANVAS_INSET = 0.125f;

    /**
     * Middle of the bottom edge of the canvas: down from the hinge along the leaned
     * slate, set forward off its face
     */
    private static final Vector3f CANVAS_ANCHOR = new Vector3f(0.0f, -CANVAS_BOTTOM_BELOW_HINGE, -CANVAS_INSET)
        .rotateX(CANVAS_LEAN)
        .add(0.0f, HINGE_HEIGHT, 0.0f)
        .div(16.0f);

    /**
     * The posts cover a block pixel of each side, two canvas pixels; the bars half a
     * block pixel of top and bottom, one canvas pixel. Half a canvas pixel more keeps
     * the smallest brush from reaching under them.
     */
    private static final float[] CANVAS_MARGINS = new float[]{2.5f, 1.5f};

    public StandBoardEntity(EntityType<? extends StandBoardEntity> type, Level level) {
        super(type, level);
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
    protected float[] getCanvasMargins() {
        return CANVAS_MARGINS;
    }

    @Override
    protected Item getHolderItem() {
        return ZetterItems.STANDING_BOARDS.get(this.getMaterial().toString()).get();
    }
}
