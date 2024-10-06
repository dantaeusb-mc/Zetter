package me.dantaeusb.zetter.item;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.entity.item.CanvasHolderEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PaletteItem extends Item {
  public static final String NBT_TAG_NAME_PALETTE_COLORS = "paletteColors";
  public static int PALETTE_SIZE = 16;

  public PaletteItem() {
    super(new Properties().durability(8192));
  }

  /**
   * For custom repairs with color saving
   *
   * @param stack
   * @return
   */
  @Override
  public boolean isRepairable(ItemStack stack) {
    return false;
  }

  @Override
  public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
    ItemStack paletteStack = player.getItemInHand(hand);

    double pickRange = player.getBlockReach();
    Vec3 eyePosition = player.getEyePosition(1.0F);
    Vec3 viewVector = player.getViewVector(1.0F);
    Vec3 targetPosition = eyePosition.add(viewVector.x * pickRange, viewVector.y * pickRange, viewVector.z * pickRange);
    // Perhaps needs to be extended to be more of a frustum
    AABB bb = player.getBoundingBox().expandTowards(targetPosition.scale(pickRange)).inflate(1.0D);

    List<Entity> canvasHolders = level.getEntities(player, bb, entity -> entity instanceof CanvasHolderEntity);

    if (canvasHolders.isEmpty()) {
      return InteractionResultHolder.fail(player.getItemInHand(hand));
    }

    CanvasHolderEntity closestCanvasHolder = null;

    double closestDistance = Double.MAX_VALUE;

    if (canvasHolders.size() > 1) {
      for (Entity entity : canvasHolders) {
        if (!((CanvasHolderEntity) entity).playerCanDraw(player)) {
          continue;
        }

        double distance = entity.distanceToSqr(player);

        if (distance < closestDistance) {
          closestDistance = distance;
          closestCanvasHolder = (CanvasHolderEntity) entity;
        }
      }
    } else {
      Entity canvasHolder = canvasHolders.get(0);

      if (((CanvasHolderEntity) canvasHolder).playerCanDraw(player)) {
        closestCanvasHolder = (CanvasHolderEntity) canvasHolders.get(0);
      }
    }

    if (closestCanvasHolder == null) {
      return InteractionResultHolder.fail(player.getItemInHand(hand));
    }

    if (level.isClientSide()) {
      Minecraft.getInstance().setScreen(
          new PaintingScreen(paletteStack, closestCanvasHolder)
      );
    }

    ItemStack itemstack = player.getItemInHand(hand);
    player.awardStat(Stats.ITEM_USED.get(this));

    return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
  }

  public static int[] getPaletteColors(ItemStack stack) {
    CompoundTag compoundNBT = stack.getTag();

    int[] paletteColors;
    if (compoundNBT != null && compoundNBT.contains(NBT_TAG_NAME_PALETTE_COLORS)) {
      paletteColors = compoundNBT.getIntArray(NBT_TAG_NAME_PALETTE_COLORS);

      if (paletteColors.length != PALETTE_SIZE) {
        int[] fixedPaletteColors = new int[PALETTE_SIZE];

        System.arraycopy(paletteColors, 0, fixedPaletteColors, 0, paletteColors.length);

        final int[] defaultPaletteColors = getDefaultPaletteColors();

        if (PALETTE_SIZE - paletteColors.length >= 0) {
          System.arraycopy(defaultPaletteColors, paletteColors.length, fixedPaletteColors, paletteColors.length, PALETTE_SIZE - paletteColors.length);
        }

        paletteColors = fixedPaletteColors;

        setPaletteColors(stack, paletteColors);
      }
    } else {
      paletteColors = getDefaultPaletteColors();
    }

    return paletteColors;
  }

  public static int[] getDefaultPaletteColors() {
    int[] paletteColors = new int[PALETTE_SIZE];

    paletteColors[0] = 0xFFAA0000; //low red
    paletteColors[1] = 0xFFFF5555; //high red
    paletteColors[2] = 0xFFAA5500; //low yellow
    paletteColors[3] = 0xFFFFFF55; //high yellow
    paletteColors[4] = 0xFF00AA00; //low green
    paletteColors[5] = 0xFF55FF55; //high green
    paletteColors[6] = 0xFF55FFFF; //low cyan
    paletteColors[7] = 0xFF00AAAA; //high cyan
    paletteColors[8] = 0xFF0000AA; //low blue
    paletteColors[9] = 0xFF5555FF; //high blue
    paletteColors[10] = 0xFFAA00AA; //low magenta
    paletteColors[11] = 0xFFFF55FF; //high magenta
    paletteColors[12] = 0xFF555555; //low gray
    paletteColors[13] = 0xFFAAAAAA; //gray
    paletteColors[14] = 0xFF000000; //black
    paletteColors[15] = 0xFFFFFFFF; //white

    return paletteColors;
  }

  /**
   * Sets key id
   *
   * @param stack         the stack
   * @param paletteColors the new colors
   */
  public static void setPaletteColors(ItemStack stack, int[] paletteColors) {
    if (paletteColors.length != PALETTE_SIZE) {
      return;
    }

    CompoundTag compoundNBT = stack.getOrCreateTag();
    compoundNBT.putIntArray(NBT_TAG_NAME_PALETTE_COLORS, paletteColors);
  }

  public static void updatePaletteColor(ItemStack stack, int paletteSlot, int color) {
    int[] colors = PaletteItem.getPaletteColors(stack);
    colors[paletteSlot] = color;

    PaletteItem.setPaletteColors(stack, colors);
  }
}