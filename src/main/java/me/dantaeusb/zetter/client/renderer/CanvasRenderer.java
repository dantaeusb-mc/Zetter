package me.dantaeusb.zetter.client.renderer;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterNetwork;
import me.dantaeusb.zetter.network.packet.CCanvasRequestPacket;
import me.dantaeusb.zetter.network.packet.CCanvasUnloadRequestPacket;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasData;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Timer;
import com.mojang.math.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;

/**
 * @todo: [MED] It's probably inefficient to check things on every render
 * Instead, some advanced tracking would be good.
 */
public class CanvasRenderer implements AutoCloseable {
    private static CanvasRenderer instance;
    private final TextureManager textureManager;
    private final Map<String, CanvasRenderer.Instance> canvasRendererInstances = Maps.newHashMap();

    private final Timer timer = new Timer(20.0F, 0L);

    /**
     * Canvasses marked as managed will be unloaded after some time
     * If canvas is not managed, it should not be added to this map
     */
    private final Map<String, Integer> ticksSinceRenderRequested = Maps.newHashMap();

    private final  Map<String, TextureRequest> textureRequestTimeout = Maps.newHashMap();

    public CanvasRenderer(TextureManager textureManager) {
        this.textureManager = textureManager;
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
    public void updateCanvasTexture(String canvasCode, CanvasData canvas) {
        this.getCanvasRendererInstance(canvasCode, canvas, true).updateCanvasTexture(canvas);
    }

    public void addCanvas(String canvasCode, AbstractCanvasData canvasData) {
        this.canvasRendererInstances.remove(canvasCode);

        this.createCanvasRendererInstance(canvasCode, canvasData);
    }

    // @todo: Why do we need both code and data?
    public void renderCanvas(PoseStack matrixStack, MultiBufferSource renderTypeBuffer, String canvasCode, AbstractCanvasData canvas, int combinedLight) {
        // We won't ever render or request 0 canvas, as 0 is a fallback value
        if (canvasCode.equals(CanvasData.getCanvasCode(0))) return;

        if (canvas.isManaged()) {
            this.ticksSinceRenderRequested.put(canvasCode, 0);
        }

        CanvasRenderer.Instance rendererInstance = this.getCanvasRendererInstance(canvasCode, canvas, false);

        if (rendererInstance == null) {
            this.queueCanvasTextureUpdate(canvas.getType(), canvasCode);
            return;
        }

        rendererInstance.render(matrixStack, renderTypeBuffer, combinedLight);
    }

    /*
     * Track textures state
     */

    /**
     * @todo: [LOW] Not sure if this timer needed on ClientTick event
     * @param gameTime
     */
    public void update(long gameTime) {
        int partialTicks = this.timer.advanceTime(gameTime);

        if (partialTicks > 0) {
            this.updateTicksSinceRender(partialTicks);
            this.updateTextureRequestTimeout(partialTicks);
        }
    }

    /**
     * @param partialTicks
     */
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
            if (textureRequestEntry.getKey().equals(Helper.FALLBACK_CANVAS_CODE)) {
                continue;
            }

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
    public void unloadCanvas(String canvasCode) {
        Zetter.LOG.debug("Unloading canvas " + canvasCode);

        // Free the texture
        this.canvasRendererInstances.get(canvasCode).close();

        this.canvasRendererInstances.remove(canvasCode);
        this.textureRequestTimeout.remove(canvasCode);

        // Not needed cause called from its iterator
        // this.ticksSinceRenderRequested.remove(canvasCode);

        // Notifying server that we're no longer tracking it
        // @todo [LOW] better just check tile entity who's around
        CCanvasUnloadRequestPacket unloadPacket = new CCanvasUnloadRequestPacket(canvasCode);
        ZetterNetwork.simpleChannel.sendToServer(unloadPacket);
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
        CCanvasRequestPacket requestSyncPacket = new CCanvasRequestPacket(request.getCanvasType(), request.getCanvasCode());
        ZetterNetwork.simpleChannel.sendToServer(requestSyncPacket);

        request.update();
    }

    /*
     * Renderer instances
     */

    private @Nullable CanvasRenderer.Instance getCanvasRendererInstance(String canvasCode, AbstractCanvasData canvasData, boolean create) {
        CanvasRenderer.Instance canvasRendererInstance = this.canvasRendererInstances.get(canvasCode);

        if (create && canvasRendererInstance == null) {
            this.createCanvasRendererInstance(canvasCode, canvasData);
        }

        return canvasRendererInstance;
    }

    private void createCanvasRendererInstance(String canvasCode, AbstractCanvasData canvas) {
        CanvasRenderer.Instance canvasRendererInstance = new CanvasRenderer.Instance(
                canvasCode, canvas.getWidth(), canvas.getHeight(), canvas.getResolution()
        );
        canvasRendererInstance.updateCanvasTexture(canvas);
        this.canvasRendererInstances.put(canvasCode, canvasRendererInstance);
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

        private final int blockPixelWidth;
        private final int blockPixelHeight;

        private Instance(String canvasCode, int width, int height, AbstractCanvasData.Resolution resolution) {
            this.code = canvasCode;
            this.canvasTexture = new DynamicTexture(width, height, true);
            ResourceLocation dynamicTextureLocation = CanvasRenderer.this.textureManager.register("canvas/" + canvasCode, this.canvasTexture);
            this.renderType = RenderType.text(dynamicTextureLocation);

            this.width = width;
            this.height = height;

            final int downScale = resolution.getNumeric() / Helper.getBasicResolution().getNumeric();

            this.blockPixelWidth = width / downScale;
            this.blockPixelHeight = height / downScale;
        }

        /*
         * Updates a map {@link net.minecraft.client.gui.MapItemRenderer.Instance#mapTexture texture}
         */

        private void updateCanvasTexture(AbstractCanvasData canvas) {
            for(int pixelY = 0; pixelY < canvas.getHeight(); pixelY++) {
                for(int pixelX = 0; pixelX < canvas.getWidth(); pixelX++) {
                    int color = canvas.getColorAt(pixelX, pixelY);
                    this.canvasTexture.getPixels().setPixelRGBA(pixelX, pixelY, this.ARGBtoABGR(color));
                }
            }

            this.canvasTexture.upload();
        }

        private int ARGBtoABGR(int x)
        {
            return ((x & 0xFF000000)) |       //AA______
                   ((x & 0x00FF0000) >> 16) | //______RR
                   ((x & 0x0000FF00)) |       //____GG__
                   ((x & 0x000000FF) << 16);  //__BB____
            // Return value is in format:  0xAABBGGRR
        }

        private void render(PoseStack matrixStack, MultiBufferSource renderTypeBuffer, int combinedLight) {
            Matrix4f matrix4f = matrixStack.last().pose();
            VertexConsumer ivertexbuilder = renderTypeBuffer.getBuffer(this.renderType);

            ivertexbuilder.vertex(matrix4f, 0.0F, (float) this.blockPixelHeight, 0F).color(255, 255, 255, 255).uv(0.0F, 1.0F).uv2(combinedLight).endVertex();
            ivertexbuilder.vertex(matrix4f, (float) this.blockPixelWidth, (float) this.blockPixelHeight, 0F).color(255, 255, 255, 255).uv(1.0F, 1.0F).uv2(combinedLight).endVertex();
            ivertexbuilder.vertex(matrix4f, (float) this.blockPixelWidth, 0.0F, 0F).color(255, 255, 255, 255).uv(1.0F, 0.0F).uv2(combinedLight).endVertex();
            ivertexbuilder.vertex(matrix4f, 0.0F, 0.0F, 0F).color(255, 255, 255, 255).uv(0.0F, 0.0F).uv2(combinedLight).endVertex();
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
