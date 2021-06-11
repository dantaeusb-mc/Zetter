package com.dantaeusb.zetter.client.renderer;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.core.Helper;
import com.dantaeusb.zetter.core.ModNetwork;
import com.dantaeusb.zetter.network.packet.CanvasRequestPacket;
import com.dantaeusb.zetter.network.packet.CanvasUnloadRequestPacket;
import com.dantaeusb.zetter.storage.AbstractCanvasData;
import com.dantaeusb.zetter.storage.CanvasData;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Timer;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class CanvasRenderer implements AutoCloseable {
    private static CanvasRenderer instance;
    private final TextureManager textureManager;
    private final Map<String, CanvasRenderer.Instance> canvasRendererInstances = Maps.newHashMap();

    private final Timer timer = new Timer(20.0F, 0L);

    private final Map<String, Integer> ticksSinceRenderRequested = Maps.newHashMap();
    private final  Map<String, TextureRequest> textureRequestTimeout = Maps.newHashMap();

    public CanvasRenderer(TextureManager textureManagerIn) {
        this.textureManager = textureManagerIn;
        instance = this;
    }

    public static CanvasRenderer getInstance() {
        return instance;
    }

    /**
     * Update canvas data directly when manipulating, for real updates like
     * server sync it's better to re-initialize renderer
     *
     * Only updates texture
     * @param canvas
     */
    public void updateCanvasTexture(CanvasData canvas) {
        this.getCanvasRendererInstance(canvas, true).updateCanvasTexture(canvas);
    }

    public void addCanvas(AbstractCanvasData canvas) {
        this.canvasRendererInstances.remove(canvas.getName());

        this.createCanvasRendererInstance(canvas);
    }

    public void renderCanvas(MatrixStack matrixStack, IRenderTypeBuffer renderTypeBuffer, AbstractCanvasData canvas, int combinedLight) {
        // We won't ever render or request 0 canvas, as 0 is a fallback value
        if (canvas.getName().equals(CanvasData.getCanvasCode(0))) return;

        this.ticksSinceRenderRequested.put(canvas.getName(), 0);

        CanvasRenderer.Instance rendererInstance = this.getCanvasRendererInstance(canvas, false);

        if (rendererInstance == null) {
            this.queueCanvasTextureUpdate(canvas.getType(), canvas.getName());
            return;
        }

        rendererInstance.render(matrixStack, renderTypeBuffer, combinedLight);
    }

    /*
     * Track textures state
     */

    /**
     *
     * @param gameTime
     */
    public void update(long gameTime) {
        // @todo: [LOW] Not sure if this timer needed on ClientTick event
        int partialTicks = this.timer.getPartialTicks(gameTime);

        if (partialTicks > 0) {
            this.updateTicksSinceRender(partialTicks);
            this.updateTextureRequestTimeout(partialTicks);
        }
    }

    private void updateTicksSinceRender(int partialTicks) {
        Iterator<Map.Entry<String, Integer>> iterator = this.ticksSinceRenderRequested.entrySet().iterator();

        while (iterator.hasNext()) {
            String canvasCode = iterator.next().getKey();

            int timeSinceRenderRequested = this.ticksSinceRenderRequested.getOrDefault(canvasCode, 0);
            timeSinceRenderRequested += partialTicks;

            // Keep 3 minutes
            if (timeSinceRenderRequested < 20.0f * 60 * 3) {
                this.ticksSinceRenderRequested.put(canvasCode, timeSinceRenderRequested);
            } else {
                this.unloadCanvas(canvasCode);
                iterator.remove();
            }
        }
    }

    private void updateTextureRequestTimeout(int partialTicks) {
        for (Map.Entry<String, TextureRequest> textureRequestEntry : this.textureRequestTimeout.entrySet()) {
            TextureRequest textureRequest = textureRequestEntry.getValue();

            if (textureRequest.canUpdate()) {
                this.requestCanvasTexture(textureRequest);
            } else {
                textureRequest.tick(partialTicks);
            }
        }
    }

    /**
     * Saying to the server that we no longer want to recieve updates
     * on this canvas since we're not using it
     * @param canvasCode
     */
    protected void unloadCanvas(String canvasCode) {
        Zetter.LOG.info("Unloading canvas " + canvasCode);

        this.canvasRendererInstances.remove(canvasCode);

        this.textureRequestTimeout.remove(canvasCode);
        // Not needed cause called from its iterator
        // this.ticksSinceRenderRequested.remove(canvasCode);

        // Notifying server that we're no longer tracking it
        // @todo [LOW] better just check tile entity who's around
        CanvasUnloadRequestPacket unloadPacket = new CanvasUnloadRequestPacket(canvasCode);
        ModNetwork.simpleChannel.sendToServer(unloadPacket);
    }

    /**
     * @todo: Still makes double-request on first load, markDirty called before update
     * @param canvasCode
     */
    public void queueCanvasTextureUpdate(AbstractCanvasData.Type type, String canvasCode) {
        if (canvasCode == null) {
            Zetter.LOG.debug("Tried to queue null canvas");
            return;
        }

        if (type == AbstractCanvasData.Type.DUMMY) {
            Zetter.LOG.debug("Tried to queue dummy canvas");
            return;
        }

        if (this.textureRequestTimeout.containsKey(canvasCode)) {
            TextureRequest textureRequest = this.textureRequestTimeout.get(canvasCode);

            // Already requested
            if (textureRequest.isNeedUpdate()) return;

            textureRequest.markDirty();
        } else {
            this.textureRequestTimeout.put(canvasCode, new TextureRequest(type, canvasCode));
        }
    }

    protected void requestCanvasTexture(TextureRequest request) {
        CanvasRequestPacket requestSyncPacket = new CanvasRequestPacket(request.getCanvasType(), request.getCanvasCode());
        ModNetwork.simpleChannel.sendToServer(requestSyncPacket);

        request.update();
    }

    /*
     * Renderer instances
     */

    private @Nullable CanvasRenderer.Instance getCanvasRendererInstance(AbstractCanvasData canvas, boolean create) {
        CanvasRenderer.Instance canvasRendererInstance = this.canvasRendererInstances.get(canvas.getName());

        if (create && canvasRendererInstance == null) {
            this.createCanvasRendererInstance(canvas);
        }

        return canvasRendererInstance;
    }

    private void createCanvasRendererInstance(AbstractCanvasData canvas) {
        CanvasRenderer.Instance canvasRendererInstance = new CanvasRenderer.Instance(canvas.getName(), canvas.getWidth(), canvas.getHeight());
        canvasRendererInstance.updateCanvasTexture(canvas);
        this.canvasRendererInstances.put(canvas.getName(), canvasRendererInstance);
    }

    /*
     * Clears the currently loaded maps and removes their corresponding textures
     */

    public void clearLoadedCanvases() {
        for(CanvasRenderer.Instance canvasRendererInstance : this.canvasRendererInstances.values()) {
            canvasRendererInstance.close();
        }

        this.canvasRendererInstances.clear();
    }

    public void close() {
        this.clearLoadedCanvases();
    }

    @OnlyIn(Dist.CLIENT)
    class Instance implements AutoCloseable {
        private final String code;
        private final DynamicTexture canvasTexture;
        private final RenderType renderType;

        private final int width;
        private final int height;

        private Instance(String canvasCode) {
            this(canvasCode, Helper.CANVAS_TEXTURE_RESOLUTION, Helper.CANVAS_TEXTURE_RESOLUTION);
        }

        private Instance(String canvasCode, int width, int height) {
            this.code = canvasCode;
            this.canvasTexture = new DynamicTexture(width, height, true);
            ResourceLocation dynamicTextureLocation = CanvasRenderer.this.textureManager.getDynamicTextureLocation("canvas/" + canvasCode, this.canvasTexture);
            this.renderType = RenderType.getText(dynamicTextureLocation);

            this.width = width;
            this.height = height;
        }

        /*
         * Updates a map {@link net.minecraft.client.gui.MapItemRenderer.Instance#mapTexture texture}
         */

        private void updateCanvasTexture(AbstractCanvasData canvas) {
            for(int pixelY = 0; pixelY < canvas.getHeight(); pixelY++) {
                for(int pixelX = 0; pixelX < canvas.getWidth(); pixelX++) {
                    int color = canvas.getColorAt(pixelX, pixelY);
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

            ivertexbuilder.pos(matrix4f, 0.0F, (float) this.height, 0F).color(255, 255, 255, 255).tex(0.0F, 1.0F).lightmap(combinedLight).endVertex();
            ivertexbuilder.pos(matrix4f, (float) this.width, (float) this.height, 0F).color(255, 255, 255, 255).tex(1.0F, 1.0F).lightmap(combinedLight).endVertex();
            ivertexbuilder.pos(matrix4f, (float) this.width, 0.0F, 0F).color(255, 255, 255, 255).tex(1.0F, 0.0F).lightmap(combinedLight).endVertex();
            ivertexbuilder.pos(matrix4f, 0.0F, 0.0F, 0F).color(255, 255, 255, 255).tex(0.0F, 0.0F).lightmap(combinedLight).endVertex();
        }

        public void close() {
            this.canvasTexture.close();
        }
    }

    static class TextureRequest {
        private final int TEXTURE_REQUEST_TIMEOUT = 20; // Not often than once in a second

        private final AbstractCanvasData.Type type;
        private final String code;
        private boolean needUpdate = true;
        private int timeout = 0;

        TextureRequest(AbstractCanvasData.Type type, String canvasCode) {
            this.type = type;
            this.code = canvasCode;
        }

        public void markDirty() {
            this.needUpdate = true;
        }

        public boolean isNeedUpdate() {
            return this.needUpdate;
        }

        public void update() {
            this.needUpdate = false;
            this.timeout = TEXTURE_REQUEST_TIMEOUT;
        }

        public String getCanvasCode() {
            return this.code;
        }

        public AbstractCanvasData.Type getCanvasType() {
            return this.type;
        }

        public void tick(int ticks) {
            // We don't need to tick that one
            if (!this.needUpdate &&  this.timeout <= 0) return;

            this.timeout -= ticks;
        }

        public boolean canUpdate() {
            return this.needUpdate && this.timeout < 0;
        }
    }
}
