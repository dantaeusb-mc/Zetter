package me.dantaeusb.zetter.core;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import me.dantaeusb.zetter.Zetter;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.io.IOException;

public class ZetterRenderTypes {
  @Nullable
  private static ShaderInstance oklchShader;

  @Nullable
  public static ShaderInstance getOklchPaletteShader() {
    return ZetterRenderTypes.oklchShader;
  }

  public static void setOklchLightness(float lightness) {
    ZetterRenderTypes.oklchShader.safeGetUniform("Lightness").set(lightness);
  }

  @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = Zetter.MOD_ID, value = Dist.CLIENT)
  public static class ModClientEvents {
    @SubscribeEvent
    public static void shaderRegistry(RegisterShadersEvent event) throws IOException {
      event.registerShader(new ShaderInstance(event.getResourceProvider(), new ResourceLocation(Zetter.MOD_ID, "rendertype_oklch_picker"), DefaultVertexFormat.BLIT_SCREEN), shaderInstance -> {
        ZetterRenderTypes.oklchShader = shaderInstance;
      });
    }
  }
}
