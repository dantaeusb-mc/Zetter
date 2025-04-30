package me.dantaeusb.zetter.entity.item;

import me.dantaeusb.zetter.core.tools.Color;
import me.dantaeusb.zetter.entity.item.state.CanvasState;
import me.dantaeusb.zetter.painting.Tool;
import me.dantaeusb.zetter.painting.parameters.AbstractToolParameters;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.Optional;

public abstract class CanvasHolderEntity extends Entity {
  protected CanvasState canvasState;

  protected boolean canUndo = false;
  protected boolean canRedo = false;

  public CanvasHolderEntity(EntityType<?> entityType, Level level) {
    super(entityType, level);
  }

  public void useTool(Player player, Tool tool, float posX, float posY, Color color, AbstractToolParameters parameters) {
    this.canvasState.useTool(player, tool, posX, posY, color.getARGB(), parameters);
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

  public abstract Vector3f getCanvasOffset();

  public abstract Vector3f getCanvasNormal();

  public abstract Optional<Matrix4f> getCanvasMatrixTransform(float partialTicks);

  public abstract boolean playerCanDraw(Player player);
}
