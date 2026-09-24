package me.dantaeusb.zetter.entity.item;

import me.dantaeusb.zetter.core.ItemStackHandlerListener;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.entity.item.container.CanvasContainer;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * A holder that paints on a canvas item slotted into it.
 */
public abstract class AbstractEaselEntity extends CanvasHolderEntity implements ItemStackHandlerListener {
  private static final String NBT_TAG_STORAGE = "storage";
  private static final String NBT_TAG_STORAGE_LEGACY = "Storage";

  protected CanvasContainer easelContainer;
  protected final LazyOptional<ItemStackHandler> easelContainerOptional = LazyOptional.of(() -> this.easelContainer);

  public AbstractEaselEntity(EntityType<?> entityType, Level level) {
    super(entityType, level);

    this.createInventory();
  }

  /*
   * Canvas
   */

  /**
   * The canvas the item stands for, made for real and written back into the item's
   * tag, so that taking it out and slotting it in elsewhere brings the painting with
   * it. Resolution comes from the item too: a canvas cut at one setting keeps that
   * setting even if the server's has changed since.
   *
   * @return
   */
  @Override
  public @Nullable CanvasData createCanvasData() {
    final ItemStack canvasStack = this.getCanvasStack();

    if (canvasStack.isEmpty()) {
      return null;
    }

    final int[] size = CanvasItem.getBlockSize(canvasStack);
    assert size != null && size.length == 2;

    final AbstractCanvasData.Resolution resolution = AbstractCanvasData.Resolution.get(CanvasItem.getResolution(canvasStack));

    final CanvasData canvasData = CanvasItem.createEmpty(
        canvasStack, resolution, size[0], size[1], this.getInitialCanvasColor(), this.level()
    );

    /*
     * createEmpty writes the code into the stack's tag directly, which does not go
     * through the container, so without this the holder would keep reporting the
     * placeholder code and we would initialize the canvas again on the next action
     */
    this.easelContainer.changed();

    return canvasData;
  }

  public ItemStack getCanvasStack() {
    return this.easelContainer.getCanvasStack();
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
   * @return
   */
  protected @Nullable String readCanvasCodeFromInventory() {
    final ItemStack canvasStack = this.getCanvasStack();

    if (canvasStack.isEmpty()) {
      return null;
    }

    final String canvasCode = CanvasItem.getCanvasCode(canvasStack);

    if (canvasCode != null) {
      return canvasCode;
    }

    final int[] size = CanvasItem.getBlockSize(canvasStack);
    assert size != null && size.length == 2;

    return CanvasData.getDefaultCanvasCode(size[0], size[1]);
  }

  protected void updateEntityDataFromInventory() {
    this.setCanvasCode(this.readCanvasCodeFromInventory());
    this.updateCanvasVectors();
  }

  /**
   * @todo: [HIGH] Supposedly won't work on client if new canvas is not yet initialized, because it'll have nullish code
   *
   * @param easelContainer
   * @param slot
   */
  public void containerChanged(ItemStackHandler easelContainer, int slot) {
    final String newCanvasCode = this.readCanvasCodeFromInventory();

    /*
     * A different canvas is a different painting, and the history belongs to the one
     * on its way out — so this has to happen while the holder still reports that
     * one, or the reset would be sent out against the canvas that just arrived
     */
    if (!Objects.equals(newCanvasCode, this.getCanvasCode())) {
      this.canvasState.reset();
    }

    this.updateEntityDataFromInventory();
  }

  /**
   * Canvases can be loaded and unloaded by machine as well as by hand
   *
   * @param capability
   * @param direction
   * @return
   * @param <T>
   */
  @Override
  public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction direction) {
    if (capability == ForgeCapabilities.ITEM_HANDLER
        && (direction == null || direction == Direction.UP || direction == Direction.DOWN)) {
      return this.easelContainerOptional.cast();
    }

    return super.getCapability(capability, direction);
  }

  @Override
  public InteractionResult interact(Player player, InteractionHand hand) {
    final ItemStack heldItem = player.getItemInHand(hand);

    if (player.isCrouching() && heldItem.isEmpty()) {
      if (!this.hasCanvas()) {
        return InteractionResult.PASS;
      }

      if (!this.level().isClientSide()) {
        player.setItemInHand(hand, this.easelContainer.extractCanvasStack());
      }

      return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    // Reads the held item and this easel's size, both of which a client has
    if (!heldItem.is(ZetterItems.CANVAS.get())
        || !this.easelContainer.isItemValid(CanvasContainer.CANVAS_SLOT, heldItem)) {
      return InteractionResult.PASS;
    }

    if (!this.level().isClientSide()) {
      /*
       * Swapped rather than refused: a canvas offered to an occupied easel takes the
       * place of the one in it and that one goes back to the player, who is standing
       * right there with their hands full of the thing they just tried to hang
       */
      final ItemStack replacedStack = this.easelContainer.extractCanvasStack();

      // The slot holds one, and a player carrying a stack of blanks should keep the rest
      this.easelContainer.setCanvasStack(heldItem.split(1));

      if (!replacedStack.isEmpty()) {
        player.getInventory().placeItemBackInInventory(replacedStack);
      }
    }

    return InteractionResult.sidedSuccess(this.level().isClientSide());
  }

  /**
   * When this tile entity is destroyed, drop all of its contents into the world
   *
   * @param level
   * @param blockPos
   */
  @Override
  public void dropAllContents(Level level, BlockPos blockPos) {
    for (int i = 0; i < this.easelContainer.getSlots(); i++) {
      Containers.dropItemStack(level, blockPos.getX(), blockPos.getY(), blockPos.getZ(), this.easelContainer.getStackInSlot(i));
    }
  }

  /*
   * Entity
   */

  @Override
  public void addAdditionalSaveData(CompoundTag compoundTag) {
    super.addAdditionalSaveData(compoundTag);

    compoundTag.put(NBT_TAG_STORAGE, this.easelContainer.serializeNBT());
  }

  @Override
  public void readAdditionalSaveData(CompoundTag compoundTag) {
    this.createInventory();

    if (compoundTag.contains(NBT_TAG_STORAGE_LEGACY)) {
      this.easelContainer.deserializeNBT(compoundTag.getCompound(NBT_TAG_STORAGE_LEGACY));
    } else {
      this.easelContainer.deserializeNBT(compoundTag.getCompound(NBT_TAG_STORAGE));
    }

    super.readAdditionalSaveData(compoundTag);
  }
}
