package com.dantaeusb.immersivemp.locks.client.gui;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.core.ModLockNetwork;
import com.dantaeusb.immersivemp.locks.item.CanvasItem;
import com.dantaeusb.immersivemp.locks.network.packet.painting.CanvasRequestPacket;
import com.dantaeusb.immersivemp.locks.network.packet.painting.CanvasUnloadRequestPacket;
import com.dantaeusb.immersivemp.locks.world.storage.CanvasData;
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
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

@OnlyIn(Dist.CLIENT)
public class CanvasRenderer implements AutoCloseable {
    private static CanvasRenderer instance;
    private final TextureManager textureManager;
    private final Map<String, CanvasRenderer.Instance> loadedCanvases = Maps.newHashMap();
    private final Map<String, Float> ticksSinceRenderRequested = Maps.newHashMap();
    private final Vector<String> reloadRequests = new Vector<>();

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

    public void updateCanvas(CanvasData canvas) {
        this.getCanvasRendererInstance(canvas, true).updateCanvasTexture();
    }

    public void renderCanvas(MatrixStack matrixStack, IRenderTypeBuffer renderTypeBuffer, CanvasData canvas, int combinedLight) {
        // We won't ever render or request 0 canvas, as 0 is a fallback value
        if (canvas.getName().equals(CanvasData.getCanvasName(0))) return;

        this.ticksSinceRenderRequested.put(canvas.getName(), 0.0f);

        CanvasRenderer.Instance rendererInstance = this.getCanvasRendererInstance(canvas, false);

        if (rendererInstance == null) {
            this.requestCanvasTexture(canvas.getName());
            return;
        }

        rendererInstance.render(matrixStack, renderTypeBuffer, combinedLight);
    }

    public void update(float renderTickTime) {
        Iterator<Map.Entry<String, Float>> iterator = this.ticksSinceRenderRequested.entrySet().iterator();

        while (iterator.hasNext()) {
            String canvasName = iterator.next().getKey();

            float timeSinceCanvasRenderRequested = this.ticksSinceRenderRequested.getOrDefault(canvasName, 0.0f);
            timeSinceCanvasRenderRequested += renderTickTime;

            // Keep 3 minutes
            if (timeSinceCanvasRenderRequested < 20.0f * 60 * 3) {
                this.ticksSinceRenderRequested.put(canvasName, timeSinceCanvasRenderRequested);
            } else {
                this.unloadCanvas(canvasName);
                iterator.remove();
            }
        }
    }

    /**
     * Saying to the server that we no longer want to recieve updates
     * on this canvas since we're not using it
     * @param canvasName
     */
    protected void unloadCanvas(String canvasName) {
        ImmersiveMp.LOG.info("Unloading canvas " + canvasName);

        this.loadedCanvases.remove(canvasName);

        // Notifying server that we're no longer tracking it
        // @todo [LOW] better just check tile entity who's around
        CanvasUnloadRequestPacket unloadPacket = new CanvasUnloadRequestPacket(canvasName);
        ModLockNetwork.simpleChannel.sendToServer(unloadPacket);
    }

    /**
     * @todo: usually called from both TE renderer and internally. Probably should be avoiding one of the calls
     * @todo: not every texture request is render request. Bit misleading
     * @param canvasName
     */
    public void requestCanvasTexture(String canvasName) {
        // We won't ever render or request 0 canvas, as 0 is a fallback value
        if (canvasName.equals(CanvasData.getCanvasName(0))) return;

        float tickSinceLastRequest = 0.0f;

        if (this.ticksSinceRenderRequested.containsKey(canvasName)) {
            tickSinceLastRequest = this.ticksSinceRenderRequested.get(canvasName);
        } else {
            this.ticksSinceRenderRequested.put(canvasName, tickSinceLastRequest);
        }

        // Wait 5 seconds before asking for texture again
        if (tickSinceLastRequest == 0.0f || tickSinceLastRequest > 20.0f * 5) {
            CanvasRequestPacket requestSyncPacket = new CanvasRequestPacket(canvasName);
            ModLockNetwork.simpleChannel.sendToServer(requestSyncPacket);

            this.ticksSinceRenderRequested.put(canvasName, 0.0f);
        }
    }

    /*
     * Returns {@link net.minecraft.client.gui.MapItemRenderer.Instance MapItemRenderer.Instance} with given map data
     */

    private @Nullable CanvasRenderer.Instance getCanvasRendererInstance(CanvasData canvas, boolean create) {
        CanvasRenderer.Instance canvasRendererInstance = this.loadedCanvases.get(canvas.getName());

        if (create && canvasRendererInstance == null) {
            canvasRendererInstance = new CanvasRenderer.Instance(canvas);
            this.loadedCanvases.put(canvas.getName(), canvasRendererInstance);

        }

        return canvasRendererInstance;
    }

    /*
     * Clears the currently loaded maps and removes their corresponding textures
     */

    public void clearLoadedCanvases() {
        for(CanvasRenderer.Instance canvasRendererInstance : this.loadedCanvases.values()) {
            canvasRendererInstance.close();
        }

        this.loadedCanvases.clear();
    }

    public void close() {
        this.clearLoadedCanvases();
    }

    @OnlyIn(Dist.CLIENT)
    class Instance implements AutoCloseable {
        private final CanvasData canvas;
        private final DynamicTexture canvasTexture;
        private final RenderType renderType;

        private Instance(CanvasData canvas) {
            this.canvas = canvas;
            this.canvasTexture = new DynamicTexture(CanvasItem.CANVAS_SIZE, CanvasItem.CANVAS_SIZE, true);
            ResourceLocation dynamicTextureLocation = CanvasRenderer.this.textureManager.getDynamicTextureLocation("canvas/" + this.canvas.getName(), this.canvasTexture);
            this.renderType = RenderType.getText(dynamicTextureLocation);
        }

        /*
         * Updates a map {@link net.minecraft.client.gui.MapItemRenderer.Instance#mapTexture texture}
         */

        private void updateCanvasTexture() {
            for(int pixelY = 0; pixelY < this.canvas.getHeight(); pixelY++) {
                for(int pixelX = 0; pixelX < this.canvas.getWidth(); pixelX++) {
                    int color = this.canvas.getColorAt(pixelX, pixelY);
                    this.canvasTexture.getTextureData().setPixelRGBA(pixelX, pixelY, this.ARGBtoABGR(color));
                }
            }

            this.canvasTexture.updateDynamicTexture();
        }

        private int ARGBtoABGR(int x)
        {
            return ((x & 0xFF000000)) |       //AA______
                   ((x & 0x00FF0000) >> 16) | //______RR
                   ((x & 0x0000FF00)) |       //____GG__
                   ((x & 0x000000FF) << 16);  //__BB____
            // Return value is in format:  0xAABBGGRR
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
