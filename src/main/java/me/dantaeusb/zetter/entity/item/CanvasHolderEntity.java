package me.dantaeusb.zetter.entity.item;

import com.mojang.math.Axis;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ItemStackHandlerListener;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.entity.item.container.CanvasContainer;
import me.dantaeusb.zetter.entity.item.state.CanvasState;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Everything that holds a canvas a player can paint on: keeps the canvas
 * item, keeps the painting state and knows where the canvas is placed
 * in the world
 */
public abstract class CanvasHolderEntity extends Entity implements ItemStackHandlerListener {
  private static final String NBT_TAG_STORAGE = "storage";
  private static final String NBT_TAG_STORAGE_LEGACY = "Storage";
  private static final String NBT_TAG_CANVAS_CODE = "CanvasCode";

  private static final EntityDataAccessor<String> DATA_ID_CANVAS_CODE = SynchedEntityData.defineId(CanvasHolderEntity.class, EntityDataSerializers.STRING);

  protected CanvasState canvasState;

  protected CanvasContainer easelContainer;
  protected final LazyOptional<ItemStackHandler> easelContainerOptional = LazyOptional.of(() -> this.easelContainer);

  /** The list of players currently using this canvas holder */
  protected ArrayList<Player> playersUsing = new ArrayList<>();
  protected HashMap<UUID, ItemStack> playersPalettes = new HashMap<>();

  protected boolean canUndo = false;
  protected boolean canRedo = false;

  protected BlockPos pos;

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
    this.createInventory();
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

    int blockWidth = 1;
    int blockHeight = 1;

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

    int blockWidth = 1;
    int blockHeight = 1;

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
    this.entityData.define(DATA_ID_CANVAS_CODE, "");
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

  public boolean hasCanvas() {
    return this.getCanvasCode() != null;
  }

  public ItemStack getCanvasStack() {
    return this.easelContainer.getCanvasStack();
  }

  public boolean playerCanDraw(Player player) {
    return this.hasCanvas() && this.isInFrontOfCanvas(player.getEyePosition());
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
    if (!this.playersUsing.contains(player)) {
      this.playersUsing.add(player);
    }

    this.playersPalettes.put(player.getUUID(), paletteStack);
  }

  public void removePlayerUsing(Player player) {
    this.playersUsing.remove(player);
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
    return this.canUndo;
  }

  public boolean canRedo() {
    return this.canRedo;
  }

  public boolean undo() {
    if (!this.canUndo) {
      return false;
    }

    return this.canvasState.undo();
  }

  public boolean redo() {
    if (!this.canRedo) {
      return false;
    }

    return this.canvasState.redo();
  }

  /*
   * Inventory
   */

  protected void createInventory() {
    CanvasContainer currentEaselStorage = this.easelContainer;
    this.easelContainer = new CanvasContainer(this);

    if (currentEaselStorage != null) {
      currentEaselStorage.removeListener(this);
      int i = Math.min(currentEaselStorage.getSlots(), this.easelContainer.getSlots());

      for (int j = 0; j < i; ++j) {
        ItemStack itemstack = currentEaselStorage.getStackInSlot(j);
        if (!itemstack.isEmpty()) {
          this.easelContainer.setStackInSlot(j, itemstack.copy());
        }
      }
    }

    this.easelContainer.addListener(this);
  }

  /**
   * This is temporary for migrating from BE to Entity
   *
   * @return
   */
  public CanvasContainer getEaselContainer() {
    return this.easelContainer;
  }

  /**
   * If canvas does not exist, set to null
   * If exists but not initialized, set to default
   * If exists and initialized, use code
   */
  protected void updateEntityDataFromInventory() {
    ItemStack canvasStack = this.easelContainer.getCanvasStack();

    if (canvasStack.isEmpty()) {
      this.setCanvasCode(null);
      this.updateCanvasVectors();
      return;
    }

    String canvasCode = CanvasItem.getCanvasCode(canvasStack);

    if (canvasCode == null) {
      int[] size = CanvasItem.getBlockSize(canvasStack);
      assert size != null && size.length == 2;

      canvasCode = CanvasData.getDefaultCanvasCode(size[0], size[1]);
    }

    this.setCanvasCode(canvasCode);
    this.updateCanvasVectors();
  }

  public void containerChanged(ItemStackHandler easelContainer, int slot) {
    ItemStack canvasStack = ((CanvasContainer) easelContainer).getCanvasStack();
    String newCanvasCode = null;
    String existingCanvasCode = null;

    if (!canvasStack.isEmpty()) {
      newCanvasCode = CanvasItem.getCanvasCode(canvasStack);

      // Initialize canvas
      if (newCanvasCode == null) {
        CanvasItem.getCanvasData(canvasStack, this.level());
        newCanvasCode = CanvasItem.getCanvasCode(canvasStack);
      }
    }

    if (this.getEaselContainer().getCanvas() != null) {
      existingCanvasCode = this.getEaselContainer().getCanvas().code;
    }

    // @todo: [HIGH] Supposedly won't work on client if new canvas is not yet initialized, because it'll have nullish code
    // Canvas changed, drop state
    if (newCanvasCode == null || !newCanvasCode.equals(existingCanvasCode)) {
      this.canvasState.reset();
    }

    this.updateEntityDataFromInventory();
  }

  public boolean canPlayerAccessInventory(Player player) {
    // @todo: [HIGH] Implement check
    return true;
  }

  @Override
  public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction direction) {
    if (capability == ForgeCapabilities.ITEM_HANDLER
        && (direction == null || direction == Direction.UP || direction == Direction.DOWN)) {
      return this.easelContainerOptional.cast();
    }

    return super.getCapability(capability, direction);
  }

  /*
   * Entity
   */

  public Packet<ClientGamePacketListener> getAddEntityPacket() {
    return NetworkHooks.getEntitySpawningPacket(this);
  }

  public void addAdditionalSaveData(CompoundTag compoundTag) {
    compoundTag.put(NBT_TAG_STORAGE, this.easelContainer.serializeNBT());

    if (this.getCanvasCode() != null) {
      compoundTag.putString(NBT_TAG_CANVAS_CODE, this.getCanvasCode());
    }
  }

  public void readAdditionalSaveData(CompoundTag compoundTag) {
    this.createInventory();

    if (compoundTag.contains(NBT_TAG_STORAGE_LEGACY)) {
      this.easelContainer.deserializeNBT(compoundTag.getCompound(NBT_TAG_STORAGE_LEGACY));
    } else {
      this.easelContainer.deserializeNBT(compoundTag.getCompound(NBT_TAG_STORAGE));
    }

    final String canvasCode = compoundTag.getString(NBT_TAG_CANVAS_CODE);

    if (canvasCode != null) {
      this.setCanvasCode(canvasCode);
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

  @Override
  public InteractionResult interact(Player player, InteractionHand hand) {
    ItemStack heldItem = player.getItemInHand(hand);

    if (player.isCrouching() && heldItem.isEmpty()) {
      ItemStack canvasStack = this.easelContainer.extractCanvasStack();
      player.setItemInHand(hand, canvasStack);
      return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    if (heldItem.is(ZetterItems.CANVAS.get())) {
      if (this.easelContainer.getCanvasStack().isEmpty() && this.easelContainer.isItemValid(CanvasContainer.CANVAS_SLOT, heldItem)) {
        this.easelContainer.setCanvasStack(heldItem);
        player.setItemInHand(hand, ItemStack.EMPTY);

        return InteractionResult.sidedSuccess(this.level().isClientSide());
      }
    }

    return InteractionResult.PASS;
  }

  /**
   * Check history, check that still exists,
   * check if need to keep information
   */
  public void tick() {
    super.tick();
    this.tick++;

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
    List<Player> possiblyUsingPlayers = this.level().getEntitiesOfClass(Player.class, new AABB(this.pos.offset(-5, -5, -5), this.pos.offset(5, 5, 5)));

    /**
     * @todo: [MED] Sending a packet just in case?
     */
    this.playersUsing.removeIf(player ->
        !possiblyUsingPlayers.contains(player)
            || !this.canPlayerAccessInventory(player)
            || !player.isAlive()
            || !player.getItemInHand(player.getUsedItemHand()).is(ZetterItems.PALETTE.get())
    );

    return this.playersUsing;
  }

  public void setPos(double x, double y, double z) {
    this.pos = new BlockPos((int) x, (int) y, (int) z);
    this.setPosRaw(x, y, z);
    this.setBoundingBox(this.makeBoundingBox());
    this.hasImpulse = true;
  }

  public BlockPos getPos() {
    return this.pos;
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
          this.dropAllContents(this.level(), this.pos);
        }
      }
      return true;
    }
  }

  public void move(MoverType mover, Vec3 move) {
    if (!this.level().isClientSide() && !this.isRemoved() && move.lengthSqr() > 0.0D) {
      this.kill();
      this.dropItem(null);
      this.dropAllContents(this.level(), this.pos);
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
      this.dropAllContents(this.level(), this.pos);
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
   * When this tile entity is destroyed, drop all of its contents into the world
   *
   * @param level
   * @param blockPos
   */
  public void dropAllContents(Level level, BlockPos blockPos) {
    for (int i = 0; i < this.easelContainer.getSlots(); i++) {
      Containers.dropItemStack(level, blockPos.getX(), blockPos.getY(), blockPos.getZ(), this.easelContainer.getStackInSlot(i));
    }
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
