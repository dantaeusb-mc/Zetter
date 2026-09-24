package me.dantaeusb.zetter.entity.item;

import com.google.common.collect.Maps;
import com.mojang.math.Axis;
import me.dantaeusb.zetter.core.ZetterItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.item.BlackboardImplement;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.DrawingData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Map;

/**
 * Blackboard: a framed slate that hangs flat on a wall and is drawn on in place
 * with chalk, rather than by opening a screen.
 */
public class BlackboardEntity extends CanvasHolderEntity {
    private static final EntityDataAccessor<String> DATA_ID_MATERIAL =
        SynchedEntityData.defineId(BlackboardEntity.class, EntityDataSerializers.STRING);

    private static final String NBT_TAG_MATERIAL = "Material";

    /**
     * A board is a piece of furniture rather than a canvas cut to order, so it is
     * always this size and the slate texture is drawn to match
     */
    public static final int BLOCK_WIDTH = 3;
    public static final int BLOCK_HEIGHT = 2;

    private static final int[] MAX_CANVAS_BLOCK_SIZE = new int[]{BLOCK_WIDTH, BLOCK_HEIGHT};

    /**
     * Fixed, unlike an easel's. The slate texture is drawn to this and chalk is sized in these pixels.
     */
    public static final AbstractCanvasData.Resolution CANVAS_RESOLUTION = AbstractCanvasData.Resolution.x32;

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

    public BlackboardEntity(EntityType<? extends BlackboardEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();

        this.entityData.define(DATA_ID_MATERIAL, Materials.OAK.toString());
    }

    public Materials getMaterial() {
        return Materials.fromString(this.entityData.get(DATA_ID_MATERIAL));
    }

    public void setMaterial(Materials material) {
        this.entityData.set(DATA_ID_MATERIAL, material.toString());
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
     * The slate is part of the board, so its size is known before its pixels are
     */
    @Override
    protected int[] getCanvasBlockSizeFallback() {
        return MAX_CANVAS_BLOCK_SIZE;
    }

    /**
     * Nothing at all: the board's own slate is what shows through where no chalk
     * has been laid down, so a clean board is an empty canvas rather than a white one
     */
    @Override
    public int getInitialCanvasColor() {
        return 0x00000000;
    }

    /**
     * @return
     */
    @Override
    protected String getInitialCanvasCode() {
        return DrawingData.getDefaultCanvasCode(BLOCK_WIDTH, BLOCK_HEIGHT);
    }

    /**
     * Fixed at the size and resolution the slate texture is drawn for.
     *
     * @return
     */
    @Override
    public CanvasData createCanvasData() {
        final CanvasData drawing = DrawingData.create(
            this.getUUID(), CANVAS_RESOLUTION,
            BLOCK_WIDTH, BLOCK_HEIGHT, this.getInitialCanvasColor(), this.level()
        );

        this.setCanvasCode(DrawingData.getCanvasCode(this.getUUID()));

        return drawing;
    }

    /**
     * Anything meant to be used on a board directly — chalk, a sponge. Letting a
     * palette open a screen on one would put the whole painting interface in front of
     * a surface meant to be worked on in place.
     *
     * @param stack
     * @return
     */
    @Override
    public boolean acceptsImplement(ItemStack stack) {
        return stack.getItem() instanceof BlackboardImplement;
    }

    /**
     * Frame's width in canvas pixels, plus a little. The board's frame is a block
     * pixel deep all round, which is two canvas pixels at this resolution, and half
     * a pixel more keeps the smallest brush from reaching under it.
     */
    public static final float CANVAS_MARGIN = 2.5f;

    /**
     * Whether a point on the slate can be drawn on, or falls under the frame
     *
     * @param posX
     * @param posY
     * @return
     */
    public boolean isDrawablePixel(float posX, float posY) {
        final CanvasData canvasData = this.getCanvasData();

        if (canvasData == null) {
            return false;
        }

        return posX >= CANVAS_MARGIN && posX <= canvasData.getWidth() - CANVAS_MARGIN
            && posY >= CANVAS_MARGIN && posY <= canvasData.getHeight() - CANVAS_MARGIN;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);

        if (compoundTag.contains(NBT_TAG_MATERIAL)) {
            this.setMaterial(Materials.fromString(compoundTag.getString(NBT_TAG_MATERIAL)));
        }
    }

    /**
     * Take the drawing with the board. Only when the board is actually gone: one
     * whose chunk unloaded, or whose world is shutting down, is coming back, and its
     * drawing has to be waiting for it.
     *
     * @param reason
     */
    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide() && reason.shouldDestroy()) {
            final String canvasCode = this.getCanvasCode();
            final CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(this.level());

            if (canvasCode != null && canvasTracker != null && !AbstractCanvasData.isDefaultCode(canvasCode)) {
                canvasTracker.unregisterCanvasData(canvasCode);
            }
        }

        super.remove(reason);
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

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);

        compoundTag.putString(NBT_TAG_MATERIAL, this.getMaterial().toString());
    }


    /**
     * Wood the frame is made of. Every plank type can be made into one, so the list
     * follows the game's rather than the frames', which stop short of the newer woods.
     */
    public enum Materials {
        OAK("oak", Surfaces.GREEN),
        SPRUCE("spruce", Surfaces.GREEN),
        BIRCH("birch", Surfaces.BROWN),
        JUNGLE("jungle", Surfaces.GREEN),
        ACACIA("acacia", Surfaces.NAVY),
        DARK_OAK("dark_oak", Surfaces.BLACK),
        MANGROVE("mangrove", Surfaces.BLACK),
        CHERRY("cherry", Surfaces.NAVY),
        BAMBOO("bamboo", Surfaces.GREEN),
        CRIMSON("crimson", Surfaces.BLACK),
        WARPED("warped", Surfaces.BROWN);

        private static final Map<String, Materials> LOOKUP = Maps.uniqueIndex(
            Arrays.asList(Materials.values()),
            Materials::toString
        );

        private final String text;
        private final Surfaces surface;

        Materials(final String text, final Surfaces surface) {
            this.text = text;
            this.surface = surface;
        }

        /**
         * Slate this wood is paired with.
         *
         * @return
         */
        public Surfaces getSurface() {
            return this.surface;
        }

        @Override
        public String toString() {
            return this.text;
        }

        public static Materials fromString(String text) {
            return LOOKUP.getOrDefault(text, OAK);
        }

        /**
         * Planks this wood is made of, which is also what its board is crafted from
         *
         * @return
         */
        public String getPlanksItem() {
            return this == BAMBOO ? "minecraft:bamboo_planks" : "minecraft:" + this.text + "_planks";
        }
    }

    /**
     * What the slate itself is made to look like. Kept apart from the wood because
     * they answer different questions, and because a board reads at distance by how
     * far its frame sits from its surface in lightness rather than in hue.
     */
    public enum Surfaces {
        BLACK("black"),
        NAVY("navy"),
        GREEN("green"),
        BROWN("brown");

        private final String text;

        Surfaces(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return this.text;
        }
    }
}
