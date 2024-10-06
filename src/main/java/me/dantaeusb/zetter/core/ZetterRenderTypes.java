package me.dantaeusb.zetter.core;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import me.dantaeusb.zetter.Zetter;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.io.IOException;

public class ZetterRenderTypes {
  @Nullable
  private static ShaderInstance colorPickerShader;

  @Nullable
  public static ShaderInstance getColorPickerShader() {
    return ZetterRenderTypes.colorPickerShader;
  }

  public static void setColorPickerRenderMode(RenderMode renderMode) {
    ZetterRenderTypes.colorPickerShader.safeGetUniform("Mode").set(renderMode.id);
  }

  public static void setColorPickerHsl(Vector3f hsl) {
    ZetterRenderTypes.colorPickerShader.safeGetUniform("HSL").set(hsl);
  }

  @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = Zetter.MOD_ID, value = Dist.CLIENT)
  public static class ModClientEvents {
    @SubscribeEvent
    public static void shaderRegistry(RegisterShadersEvent event) throws IOException {
      event.registerShader(new ShaderInstance(event.getResourceProvider(), new ResourceLocation(Zetter.MOD_ID, "rendertype_color_picker"), DefaultVertexFormat.BLIT_SCREEN), shaderInstance -> {
        ZetterRenderTypes.colorPickerShader = shaderInstance;
      });
    }
  }

  public enum RenderMode {
    OK_HUE_SATURATION(0),
    RGB_HUE_SATURATION(1),
    OK_HUE_HORIZONTAL(2),
    OK_SATURATION_HORIZONTAL(3),
    OK_LIGHTNESS_HORIZONTAL(4),
    OK_LIGHTNESS_VERTICAL(5),
    RGB_HUE_HORIZONTAL(6),
    RGB_SATURATION_HORIZONTAL(7),
    RGB_LIGHTNESS_HORIZONTAL(8),
    RGB_LIGHTNESS_VERTICAL(9),
    OK_OPACITY_HORIZONTAL(10),
    RGB_OPACITY_HORIZONTAL(11);

    public final int id;

    RenderMode(int id) {
      this.id = id;
    }
  }
}
