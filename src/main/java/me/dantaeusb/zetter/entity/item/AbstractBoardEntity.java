package me.dantaeusb.zetter.entity.item;

import com.google.common.collect.Maps;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.item.BlackboardImplement;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.DrawingData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.Map;

/**
 * Anything with a slate of its own that is drawn on in place with chalk, rather
 * than by opening a screen: a board on a wall, a board on a stand.
 *
 * The slate is part of the board, so unlike an easel's canvas it has a fixed size
 * and resolution, exists from the moment the board does and goes when it goes.
 */
public abstract class AbstractBoardEntity extends CanvasHolderEntity {
    private static final EntityDataAccessor<String> DATA_ID_MATERIAL =
        SynchedEntityData.defineId(AbstractBoardEntity.class, EntityDataSerializers.STRING);

    private static final String NBT_TAG_MATERIAL = "Material";

    /**
     * Fixed, unlike an easel's. Slate textures are drawn to this and chalk is sized in these pixels.
     */
    public static final AbstractCanvasData.Resolution CANVAS_RESOLUTION = AbstractCanvasData.Resolution.x32;

    public AbstractBoardEntity(EntityType<? extends AbstractBoardEntity> type, Level level) {
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

    /**
     * Canvas pixels along each edge that fall under the frame rather than on the
     * slate, {horizontal, vertical}, with a little to spare so the smallest brush
     * cannot reach under it
     */
    protected abstract float[] getCanvasMargins();

    /**
     * The slate is part of the board, so its size is known before its pixels are
     */
    @Override
    protected int[] getCanvasBlockSizeFallback() {
        return this.getMaxCanvasBlockSize();
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
     * Called while the entity is still being constructed, so the block size it reads
     * has to be a constant of the board
     *
     * @return
     */
    @Override
    protected String getInitialCanvasCode() {
        final int[] size = this.getMaxCanvasBlockSize();

        return DrawingData.getDefaultCanvasCode(size[0], size[1]);
    }

    /**
     * Fixed at the size and resolution the slate texture is drawn for.
     *
     * @return
     */
    @Override
    public CanvasData createCanvasData() {
        final int[] size = this.getMaxCanvasBlockSize();

        final CanvasData drawing = DrawingData.create(
            this.getUUID(), CANVAS_RESOLUTION,
            size[0], size[1], this.getInitialCanvasColor(), this.level()
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

        final float[] margins = this.getCanvasMargins();

        return posX >= margins[0] && posX <= canvasData.getWidth() - margins[0]
            && posY >= margins[1] && posY <= canvasData.getHeight() - margins[1];
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
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);

        if (compoundTag.contains(NBT_TAG_MATERIAL)) {
            this.setMaterial(Materials.fromString(compoundTag.getString(NBT_TAG_MATERIAL)));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);

        compoundTag.putString(NBT_TAG_MATERIAL, this.getMaterial().toString());
    }

    /**
     * Wood the board is made of, and with it the slate. Every plank type can be made
     * into one, so the list follows the game's rather than the painting frames',
     * which stop short of the newer woods.
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
         * Planks this wood is made of, which is also what its boards are crafted from
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
