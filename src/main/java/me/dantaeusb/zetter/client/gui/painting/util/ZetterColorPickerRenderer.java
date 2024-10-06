package me.dantaeusb.zetter.client.gui.painting.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import me.dantaeusb.zetter.core.ZetterRenderTypes;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ZetterColorPickerRenderer {
    public static void renderColorPicker(GuiGraphics guiGraphics, ZetterRenderTypes.RenderMode renderMode, Vector3f hsl, int x, int y, int width, int height) {
      RenderSystem.setShader(ZetterRenderTypes::getColorPickerShader);
      ZetterRenderTypes.setColorPickerRenderMode(renderMode);
      ZetterRenderTypes.setColorPickerHsl(hsl);

      Matrix4f matrix4f = guiGraphics.pose().last().pose();

      BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
      bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      bufferbuilder.vertex(matrix4f, x, y, 0.0f).uv(0, 0).endVertex();
      bufferbuilder.vertex(matrix4f, x, y + height, 0.0f).uv(0, 1).endVertex();
      bufferbuilder.vertex(matrix4f, x + width, y + height, 0.0f).uv(1, 1).endVertex();
      bufferbuilder.vertex(matrix4f, x + width, y, 0.0f).uv(1, 0).endVertex();
      BufferUploader.drawWithShader(bufferbuilder.end());
    }
}
