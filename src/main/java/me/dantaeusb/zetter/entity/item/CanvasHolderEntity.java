package me.dantaeusb.zetter.entity.item;

import com.mojang.math.Axis;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.entity.item.state.CanvasState;
import me.dantaeusb.zetter.item.PaletteItem;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Everything that holds a canvas a player can paint on: knows which canvas that is,
 * keeps the painting state and knows where the canvas sits in the world.
 */
public abstract class CanvasHolderEntity extends Entity {
  private static final String NBT_TAG_CANVAS_CODE = "CanvasCode";

  /** Server lags behind the client's position, so reach gets a block of slack */
  private static final double REACH_TOLERANCE = 1.0D;

  private static final EntityDataAccessor<String> DATA_ID_CANVAS_CODE = SynchedEntityData.defineId(CanvasHolderEntity.class, EntityDataSerializers.STRING);

  protected CanvasState canvasState;

  /** The list of players currently using this canvas holder */
  protected ArrayList<Player> playersUsing = new ArrayList<>();
  protected HashMap<UUID, ItemStack> playersPalettes = new HashMap<>();

  protected Matrix4f canvasMatrix;
  protected Vector3f canvasOffset;
  protected Vector3f canvasNormal;
  protected Vector3f canvasU;
  protected Vector3f canvasV;

  private int canvasVectorsBlockWidth;
  private int canvasVectorsBlockHeight;
  private float canvasVectorsYRot;

  private int tick;

  public CanvasHolderEntity(EntityType<?> entityType, Level level) {
    super(entityType, level);

    this.canvasState = new CanvasState(this);
  }

  /*
   * Geometry, defined by the holder
   */

  protected abstract Vector3f getCanvasAnchor(int blockWidth, int blockHeight);

  protected abstract float getCanvasLean();

  /**
   * Largest canvas that fits this holder, blocks: {width, height}
   */
  public abstract int[] getMaxCanvasBlockSize();

  /**
   * Size to lay the canvas plane out with before any canvas data has arrived. An
   * easel has no way of knowing, but a holder whose canvas is a fixed part of it
   * does, and wants its plane in the right place from the first frame.
   */
  protected int[] getCanvasBlockSizeFallback() {
    return new int[]{1, 1};
  }

  /**
   * Colour a blank canvas starts out as. Primed white for a canvas on an easel;
   * a board's slate is behind the drawing rather than part of it, so its canvas
   * starts as nothing at all and lets the slate through.
   */
  public int getInitialCanvasColor() {
    return Helper.CANVAS_COLOR;
  }

  /**
   * Called while the entity is still being constructed, so it can only read what is
   * constant about the holder.
   *
   * @return
   */
  protected @Nullable String getInitialCanvasCode() {
    return null;
  }

  /**
   * Bring this holder's canvas into being and adopt its code.
   * Server only.
   *
   * @return
   */
  public abstract @Nullable CanvasData createCanvasData();

  protected abstract Item getHolderItem();

  /*
   * Canvas placement
   */

  /**
   * Canvas plane has its origin in the top left corner of the image,
   * X axis pointing right, Y axis pointing down and Z axis pointing
   * away from the viewer, one unit being one canvas pixel.
   *
   * All the vectors below describe that plane after the model-space
   * transformation (matrix) and after the entity rotation (world-space
   * vectors), so that the renderer and the raycasting agree on where
   * the canvas is.
   */
  protected void updateCanvasVectors() {
    final float scaleFactor = 1.0F / 16.0F;

    final CanvasData canvasData = this.getCanvasData();

    final int[] fallback = this.getCanvasBlockSizeFallback();

    int blockWidth = fallback[0];
    int blockHeight = fallback[1];

    if (canvasData != null) {
      blockWidth = canvasData.getWidth() / canvasData.getResolution().getNumeric();
      blockHeight = canvasData.getHeight() / canvasData.getResolution().getNumeric();
    }

    final Vector3f anchor = this.getCanvasAnchor(blockWidth, blockHeight);

    this.canvasMatrix = new Matrix4f();
    this.canvasMatrix.translate(anchor);
    this.canvasMatrix.scale(scaleFactor, scaleFactor, scaleFactor);
    this.canvasMatrix.rotate(Axis.XP.rotation(this.getCanvasLean()));
    // Canvas plane Y axis points down, model Y axis points up
    this.canvasMatrix.rotate(Axis.ZP.rotationDegrees(180.0f));
    // Anchor is the middle of the bottom edge of the canvas
    this.canvasMatrix.translate(-8.0f * blockWidth, -16.0f * blockHeight, 0.0f);

    /*
     * Renderer rotates the model the same way, so to get world-space
     * vectors we only need to apply that rotation on top of the matrix
     */
    final Quaternionf entityRotation = Axis.YP.rotationDegrees(180.0F - this.getYRot());

    // Projection expects the offset to be one block in from the origin corner
    this.canvasOffset = entityRotation.transform(this.canvasMatrix.transformPosition(new Vector3f(16.0f, 16.0f, 0.0f)));
    this.canvasU = entityRotation.transform(this.canvasMatrix.transformDirection(new Vector3f(1.0f, 0.0f, 0.0f)).normalize());
    this.canvasV = entityRotation.transform(this.canvasMatrix.transformDirection(new Vector3f(0.0f, 1.0f, 0.0f)).normalize());
    this.canvasNormal = entityRotation.transform(this.canvasMatrix.transformDirection(new Vector3f(0.0f, 0.0f, -1.0f)).normalize());

    this.canvasVectorsBlockWidth = blockWidth;
    this.canvasVectorsBlockHeight = blockHeight;
    this.canvasVectorsYRot = this.getYRot();
  }

  /**
   * Canvas data is loaded asynchronously on the client, and the entity can be
   * rotated at any time
   */
  protected void checkCanvasVectors() {
    if (this.canvasMatrix == null || this.canvasVectorsYRot != this.getYRot()) {
      this.updateCanvasVectors();
      return;
    }

    final CanvasData canvasData = this.getCanvasData();

    final int[] fallback = this.getCanvasBlockSizeFallback();

    int blockWidth = fallback[0];
    int blockHeight = fallback[1];

    if (canvasData != null) {
      blockWidth = canvasData.getWidth() / canvasData.getResolution().getNumeric();
      blockHeight = canvasData.getHeight() / canvasData.getResolution().getNumeric();
    }

    if (blockWidth != this.canvasVectorsBlockWidth || blockHeight != this.canvasVectorsBlockHeight) {
      this.updateCanvasVectors();
    }
  }

  /**
   * For the reasons of rapidly declining mental function,
   * this returns bottom left corner of the canvas.
   * @return
   */
  public Vector3f getCanvasOffset() {
    this.checkCanvasVectors();
    return this.canvasOffset;
  }

  public Vector3f getCanvasNormal() {
    this.checkCanvasVectors();
    return this.canvasNormal;
  }

  public Vector3f getCanvasU() {
    this.checkCanvasVectors();
    return this.canvasU;
  }

  public Vector3f getCanvasV() {
    this.checkCanvasVectors();
    return this.canvasV;
  }

  public Matrix4f getCanvasMatrixTransform(float partialTicks) {
    this.checkCanvasVectors();
    return this.canvasMatrix;
  }

  /**
   * Point of the canvas closest to the given position, world space
   */
  public Vec3 getClosestCanvasPoint(Vec3 position) {
    this.checkCanvasVectors();

    final Vec3 u = new Vec3(this.canvasU);
    final Vec3 v = new Vec3(this.canvasV);

    // Offset points one block in from the top left corner of the canvas
    final Vec3 corner = this.position().add(new Vec3(this.canvasOffset)).subtract(u).subtract(v);
    final Vec3 relative = position.subtract(corner);

    final double alongU = Mth.clamp(relative.dot(u), 0.0D, this.canvasVectorsBlockWidth);
    final double alongV = Mth.clamp(relative.dot(v), 0.0D, this.canvasVectorsBlockHeight);

    return corner.add(u.scale(alongU)).add(v.scale(alongV));
  }

  /**
   * Pixel of the canvas a ray points at, or null when there is no canvas or the ray
   * runs along its plane. The pixel can fall outside the canvas.
   *
   * @param origin
   * @param direction
   * @param partialTicks
   * @return
   */
  public @Nullable Vector2f getCanvasPixel(Vec3 origin, Vec3 direction, float partialTicks) {
    final CanvasData canvasData = this.getCanvasData();

    if (canvasData == null) {
      return null;
    }

    final Vector3f normal = this.getCanvasNormal();
    final Vector3f ray = direction.toVector3f().normalize();

    final float denominator = ray.dot(normal);

    // Looking along the canvas rather than at it
    if (Mth.abs(denominator) < Mth.EPSILON) {
      return null;
    }

    final Vector3f canvasPosition = new Vector3f(this.getCanvasOffset()).add(this.getPosition(partialTicks).toVector3f());
    final Vector3f rayOrigin = origin.toVector3f();

    final float distance = new Vector3f(canvasPosition).sub(rayOrigin).dot(normal) / denominator;
    final Vector3f relative = new Vector3f(ray).mul(distance).add(rayOrigin).sub(canvasPosition);

    /*
     * The plane vectors are one world unit, which is one block, so the distance
     * along them is in blocks and turns into pixels by the canvas resolution. The
     * offset they are measured from sits one block in from the corner, which is
     * that same resolution again.
     */
    final int resolution = canvasData.getResolution().getNumeric();

    return new Vector2f(
      relative.dot(this.getCanvasU()) * resolution + resolution,
      relative.dot(this.getCanvasV()) * resolution + resolution
    );
  }

  public boolean isInFrontOfCanvas(Vec3 position) {
    this.checkCanvasVectors();

    // Any point of the canvas will do, they all lay in the same plane
    final Vec3 canvasToPosition = position.subtract(this.getClosestCanvasPoint(position));

    return canvasToPosition.dot(new Vec3(this.canvasNormal)) > 0.0D;
  }

  /*
   * Canvas data
   */

  protected void defineSynchedData() {
    final String initialCanvasCode = this.getInitialCanvasCode();

    this.entityData.define(DATA_ID_CANVAS_CODE, initialCanvasCode == null ? "" : initialCanvasCode);
  }

  public @Nullable String getCanvasCode() {
    String canvasCode = this.entityData.get(DATA_ID_CANVAS_CODE);

    if (canvasCode.isEmpty()) {
      return null;
    }

    return canvasCode;
  }

  /**
   * Set canvas code for referencing without loading the items to
   * render canvas on easel
   *
   * @param canvasCode
   */
  protected void setCanvasCode(@Nullable String canvasCode) {
    if (canvasCode != null) {
      this.entityData.set(DATA_ID_CANVAS_CODE, canvasCode);
    } else {
      this.entityData.set(DATA_ID_CANVAS_CODE, "");
    }
  }

  public @Nullable CanvasData getCanvasData() {
    final String canvasCode = this.getCanvasCode();

    if (canvasCode == null) {
      return null;
    }

    CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(this.level());

    if (canvasTracker == null) {
      return null;
    }

    return canvasTracker.getCanvasData(canvasCode);
  }

  /**
   * Whether there is a surface to paint on at all, which is not the same as there
   * being anything painted on it yet
   *
   * @return
   */
  public boolean hasCanvas() {
    return this.getCanvasCode() != null;
  }

  public boolean playerCanDraw(Player player) {
    return this.hasCanvas() && this.isInFrontOfCanvas(player.getEyePosition());
  }

  /**
   * Whether the player is close enough to keep working with this holder. Holders are
   * several blocks tall and wide, so distance is measured to the nearest point of the
   * bounding box rather than to the entity position.
   *
   * @param player
   * @return
   */
  public boolean canPlayerAccessInventory(Player player) {
    if (this.isRemoved() || !player.isAlive()) {
      return false;
    }

    final Vec3 eyePosition = player.getEyePosition();
    final AABB boundingBox = this.getBoundingBox();

    final Vec3 closestPoint = new Vec3(
        Mth.clamp(eyePosition.x, boundingBox.minX, boundingBox.maxX),
        Mth.clamp(eyePosition.y, boundingBox.minY, boundingBox.maxY),
        Mth.clamp(eyePosition.z, boundingBox.minZ, boundingBox.maxZ)
    );

    final double reach = player.getBlockReach() + REACH_TOLERANCE;

    return eyePosition.distanceToSqr(closestPoint) <= reach * reach;
  }

  /**
   * Whether a player holding this item stack could draw on this holder with it.
   *
   * @param stack
   * @return
   */
  public boolean acceptsImplement(ItemStack stack) {
    return stack.getItem() instanceof PaletteItem;
  }

  public boolean canPlayerStartUsing(Player player) {
    if (!this.canPlayerAccessInventory(player) || !this.playerCanDraw(player)) {
      return false;
    }

    final Vec3 eyePosition = player.getEyePosition();
    final double reach = player.getBlockReach() + REACH_TOLERANCE;

    return this.getClosestCanvasPoint(eyePosition).distanceToSqr(eyePosition) <= reach * reach;
  }

  /*
   * Painting state
   */

  public CanvasState getCanvasState() {
    return this.canvasState;
  }

  public ArrayList<Player> getPlayersUsing() {
    return this.playersUsing;
  }

  public void addPlayerUsing(Player player, ItemStack paletteStack) {
    this.playersPalettes.put(player.getUUID(), paletteStack);

    if (!this.playersUsing.contains(player)) {
      this.playersUsing.add(player);
      this.canvasState.addPlayer(player);
    }
  }

  public void removePlayerUsing(Player player) {
    if (this.playersUsing.remove(player)) {
      this.canvasState.removePlayer(player);
    }

    this.playersPalettes.remove(player.getUUID());
  }

  /**
   * @todo: [MED] Rename to getPlayerPaletteStack
   * @param player
   * @return
   */
  public @Nullable ItemStack getPaletteStack(Player player) {
    return this.playersPalettes.get(player.getUUID());
  }

  public void damagePalette(Player player, int damage) {
    final int maxDamage = this.getPaletteStack(player).getMaxDamage() - 1;
    int newDamage = this.getPaletteStack(player).getDamageValue() + damage;
    newDamage = Math.min(newDamage, maxDamage);

    this.getPaletteStack(player).setDamageValue(newDamage);
  }

  public boolean canUndo() {
    return this.canvasState.canUndo();
  }

  public boolean canRedo() {
    return this.canvasState.canRedo();
  }

  public boolean undo() {
    if (!this.canUndo()) {
      return false;
    }

    return this.canvasState.undo();
  }

  public boolean redo() {
    if (!this.canRedo()) {
      return false;
    }

    return this.canvasState.redo();
  }

  /*
   * Entity
   */

  public Packet<ClientGamePacketListener> getAddEntityPacket() {
    return NetworkHooks.getEntitySpawningPacket(this);
  }

  public void addAdditionalSaveData(CompoundTag compoundTag) {
    if (this.getCanvasCode() != null) {
      compoundTag.putString(NBT_TAG_CANVAS_CODE, this.getCanvasCode());
    }
  }

  public void readAdditionalSaveData(CompoundTag compoundTag) {
    /*
     * Only when it is there: a holder that starts out with a canvas of its own has
     * already set one, and an absent tag would otherwise read back as an empty
     * string and take it away again
     */
    if (compoundTag.contains(NBT_TAG_CANVAS_CODE)) {
      this.setCanvasCode(compoundTag.getString(NBT_TAG_CANVAS_CODE));
    }
  }

  /**
   * Needed for entity interaction
   *
   * @return
   */
  public boolean isPickable() {
    return !this.isRemoved();
  }

  public boolean isPushable() {
    return false;
  }

  /**
   * Check history, check that still exists,
   * check if need to keep information
   */
  public void tick() {
    super.tick();
    this.tick++;

    // History syncs in both directions, so the state ticks on both sides
    this.canvasState.tick();

    // No need to check correctness and players on client side
    if (this.level().isClientSide()) {
      return;
    }

    this.checkBelowWorld();
    if (this.tick % 200 == 0) {
      this.checkPlayersUsing();
    }

    if (this.tick % 100 == 0) {
      if (!this.isRemoved() && !this.survives()) {
        this.discard();
        this.dropItem(null);
        this.dropAllContents(this.level(), this.getPos());
      }
    }
  }

  public boolean survives() {
    if (!this.level().noCollision(this)) {
      return false;
    } else {
      BlockPos posBelow = this.getPos().below();
      BlockState blockBelowState = this.level().getBlockState(posBelow);

      if (!blockBelowState.isSolid() && !DiodeBlock.isDiode(blockBelowState)) {
        return false;
      }

      return this.level().getEntities(this, this.getBoundingBox(), (entity) -> entity.getType() == this.getType()).isEmpty();
    }
  }

  /**
   * Normally players subscribe and unsubscribe manually when stopping drawing
   * but sometimes we might miss unsubscribing, i.e. in case of player disconnect.
   * We are ticking updates to check if player still could be using easel.
   *
   * @return
   */
  public List<Player> checkPlayersUsing() {
    // Wide enough to be a superset of canPlayerAccessInventory, which does the real check
    List<Player> possiblyUsingPlayers = this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(8.0D));

    /**
     * @todo: [MED] Sending a packet just in case?
     */
    this.playersUsing.removeIf(player -> {
      if (possiblyUsingPlayers.contains(player)
          && this.canPlayerAccessInventory(player)
          && player.isAlive()
          && (this.acceptsImplement(player.getMainHandItem())
              || this.acceptsImplement(player.getOffhandItem()))) {
        return false;
      }

      // Not removePlayerUsing(), it would modify the list we're iterating
      this.canvasState.removePlayer(player);

      return true;
    });

    return this.playersUsing;
  }

  public void setPos(double x, double y, double z) {
    this.setPosRaw(x, y, z);
    this.setBoundingBox(this.makeBoundingBox());
    this.hasImpulse = true;
  }

  /**
   * Block the easel stands in. Entity#blockPosition is floored, unlike a cast to int,
   * which in negative coordinates would pick the neighbouring block instead.
   *
   * @return
   */
  public BlockPos getPos() {
    return this.blockPosition();
  }

  /*
   * Drop contents and item then die when moved or hurt
   */

  public boolean hurt(DamageSource damageSource, float pAmount) {
    if (this.isInvulnerableTo(damageSource)) {
      return false;
    } else {
      if (!this.level().isClientSide()) {
        if (!this.isRemoved()) {
          this.kill();
          this.markHurt();
          this.dropItem(damageSource.getEntity());
          this.dropAllContents(this.level(), this.getPos());
        }
      }
      return true;
    }
  }

  public void move(MoverType mover, Vec3 move) {
    if (!this.level().isClientSide() && !this.isRemoved() && move.lengthSqr() > 0.0D) {
      this.kill();
      this.dropItem(null);
      this.dropAllContents(this.level(), this.getPos());
    }
  }

  /**
   * @param x
   * @param y
   * @param z
   * @todo: [LOW] Rename params
   */
  public void push(double x, double y, double z) {
    if (!this.level().isClientSide && !this.isRemoved() && x * x + y * y + z * z > 0.0D) {
      this.kill();
      this.dropItem(null);
      this.dropAllContents(this.level(), this.getPos());
    }
  }

  /**
   * Drop an item associated with this entity
   *
   * @param entity
   */
  public void dropItem(@Nullable Entity entity) {
    if (this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
      this.playSound(SoundEvents.PAINTING_BREAK, 1.0F, 1.0F);
      if (entity instanceof Player) {
        Player player = (Player) entity;
        if (player.getAbilities().instabuild) {
          return;
        }
      }

      this.spawnAtLocation(this.getHolderItem());
    }
  }

  /**
   * When this holder is destroyed, drop anything it was holding on somebody else's
   * behalf.
   *
   * @param level
   * @param blockPos
   */
  public void dropAllContents(Level level, BlockPos blockPos) {
  }

  @Override
  public ItemStack getPickResult() {
    return new ItemStack(this.getHolderItem());
  }

  /**
   * Copied from armor stand
   *
   * @return
   */
  public SoundEvent getRemoveItemSound() {
    return SoundEvents.ARMOR_STAND_BREAK;
  }
}
