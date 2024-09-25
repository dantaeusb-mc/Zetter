package me.dantaeusb.zetter.entity.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.Optional;

public interface CanvasHolderEntity {
  /*
   * Entity base transformations
   */
  Vec3 getPosition(float partialTicks);
  float getXRot();
  float getYRot();

  @Nullable String getCanvasCode();

  Optional<Matrix4f> getCanvasMatrixTransform(float partialTicks);

  boolean playerCanDraw(Player player);
}
