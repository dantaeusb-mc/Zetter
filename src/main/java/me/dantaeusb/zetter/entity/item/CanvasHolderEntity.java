package me.dantaeusb.zetter.entity.item;

import me.dantaeusb.zetter.entity.item.state.CanvasState;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public abstract class CanvasHolderEntity extends Entity {
  protected CanvasState canvasState;

  /** The list of players currently using this canvas holder */
  protected ArrayList<Player> playersUsing = new ArrayList<>();
  protected HashMap<UUID, ItemStack> playersPalettes = new HashMap<>();

  protected boolean canUndo = false;
  protected boolean canRedo = false;

  public CanvasHolderEntity(EntityType<?> entityType, Level level) {
    super(entityType, level);

    this.canvasState = new CanvasState(this);
  }

  public ArrayList<Player> getPlayersUsing() {
    return this.playersUsing;
  }

  public void addPlayerUsing(Player player) {
    if (!this.playersUsing.contains(player)) {
      this.playersUsing.add(player);
    }
  }

  public void removePlayerUsing(Player player) {
    this.playersUsing.remove(player);
  }

  public CanvasState getCanvasState() {
    return this.canvasState;
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

    final boolean result = this.canvasState.undo();

    return result;
  }

  public boolean redo() {
    if (!this.canRedo) {
      return false;
    }

    final boolean result = this.canvasState.redo();

    return result;
  }

  public abstract @Nullable String getCanvasCode();

  public abstract @Nullable CanvasData getCanvasData();

  public abstract ItemStack getCanvasStack();

  /**
   * @todo: [MED] Rename to getPlayerPaletteStack
   * @param player
   * @return
   */
  public abstract ItemStack getPaletteStack(Player player);

  public abstract Vector3f getCanvasOffset();

  public abstract Vector3f getCanvasNormal();

  public abstract Vector3f getCanvasU();

  public abstract Vector3f getCanvasV();

  public abstract Optional<Matrix4f> getCanvasMatrixTransform(float partialTicks);

  public abstract boolean playerCanDraw(Player player);
}
