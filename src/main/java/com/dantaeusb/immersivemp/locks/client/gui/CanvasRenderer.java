package com.dantaeusb.immersivemp.locks.client.gui;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.item.CanvasItem;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class CanvasRenderer implements AutoCloseable {
    private static CanvasRenderer instance;

    private final TextureManager textureManager;
    private final Map<String, CanvasRenderer.Instance> loadedCanvases = Maps.newHashMap();

    public CanvasRenderer(TextureManager textureManagerIn) {
        this.textureManager = textureManagerIn;
        instance = this;
    }

    public static CanvasRenderer getInstance() {
        return instance;
    }

    /*
     * Updates a map texture
     */

    public void updateCanvas(ByteBuffer canvas) {
        this.getCanvasRendererInstance(canvas).updateMapTexture();
    }

    public void renderCanvas(MatrixStack matrixStack, IRenderTypeBuffer renderTypeBuffer, ByteBuffer canvas, int combinedLight) {
        this.getCanvasRendererInstance(canvas).render(matrixStack, renderTypeBuffer, combinedLight);
    }

    /*
     * Returns {@link net.minecraft.client.gui.MapItemRenderer.Instance MapItemRenderer.Instance} with given map data
     */

    private CanvasRenderer.Instance getCanvasRendererInstance(ByteBuffer canvas) {
        CanvasRenderer.Instance canvasRendererInstance = this.loadedCanvases.get("test");

        if (canvasRendererInstance == null) {
            canvasRendererInstance = new CanvasRenderer.Instance(canvas);
            this.loadedCanvases.put("test", canvasRendererInstance);
        }

        return canvasRendererInstance;
    }

    @Nullable
    public CanvasRenderer.Instance getCanvasInstanceIfExists(String canvasName) {
        return this.loadedCanvases.get(canvasName);
    }

    /*
     * Clears the currently loaded maps and removes their corresponding textures
     */

    public void clearLoadedMaps() {
        for(CanvasRenderer.Instance canvasRendererInstance : this.loadedCanvases.values()) {
            canvasRendererInstance.close();
        }

        this.loadedCanvases.clear();
    }

    @Nullable
    public ByteBuffer getData(@Nullable CanvasRenderer.Instance canvasRendererInstance) {
        return canvasRendererInstance != null ? canvasRendererInstance.canvas : null;
    }

    public void close() {
        this.clearLoadedMaps();
    }

    @OnlyIn(Dist.CLIENT)
    class Instance implements AutoCloseable {
        private final ByteBuffer canvas;
        private final DynamicTexture canvasTexture;
        private final RenderType renderType;

        private Instance(ByteBuffer canvas) {
            this.canvas = canvas;
            this.canvasTexture = new DynamicTexture(CanvasItem.CANVAS_SIZE, CanvasItem.CANVAS_SIZE, true);
            ResourceLocation dynamicTextureLocation = CanvasRenderer.this.textureManager.getDynamicTextureLocation("canvas/" + "test", this.canvasTexture);
            this.renderType = RenderType.getText(dynamicTextureLocation);
        }

        /*
         * Updates a map {@link net.minecraft.client.gui.MapItemRenderer.Instance#mapTexture texture}
         */

        private void updateMapTexture() {
            this.canvas.rewind();

            for(int row = 0; row < CanvasItem.CANVAS_SIZE; row++) {
                for(int col = 0; col < CanvasItem.CANVAS_SIZE; col++) {
                    int intIndex = row * CanvasItem.CANVAS_SIZE + col;
                    int color = this.canvas.getInt(intIndex * 4);
                    this.canvasTexture.getTextureData().setPixelRGBA(col, row, color);
                }
            }

            ImmersiveMp.LOG.warn("Updated texture");

            this.canvasTexture.updateDynamicTexture();
        }

        private void render(MatrixStack matrixStack, IRenderTypeBuffer renderTypeBuffer, int combinedLight) {
            Matrix4f matrix4f = matrixStack.getLast().getMatrix();
            IVertexBuilder ivertexbuilder = renderTypeBuffer.getBuffer(this.renderType);
            ivertexbuilder.pos(matrix4f, 0.0F, 16.0F, -0.01F).color(255, 255, 255, 255).tex(0.0F, 1.0F).lightmap(combinedLight).endVertex();
            ivertexbuilder.pos(matrix4f, 16.0F, 16.0F, -0.01F).color(255, 255, 255, 255).tex(1.0F, 1.0F).lightmap(combinedLight).endVertex();
            ivertexbuilder.pos(matrix4f, 16.0F, 0.0F, -0.01F).color(255, 255, 255, 255).tex(1.0F, 0.0F).lightmap(combinedLight).endVertex();
            ivertexbuilder.pos(matrix4f, 0.0F, 0.0F, -0.01F).color(255, 255, 255, 255).tex(0.0F, 0.0F).lightmap(combinedLight).endVertex();
        }

        public void close() {
            this.canvasTexture.close();
        }
    }
}
