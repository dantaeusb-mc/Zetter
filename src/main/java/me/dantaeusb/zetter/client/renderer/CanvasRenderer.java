package me.dantaeusb.zetter.client.renderer;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterNetwork;
import me.dantaeusb.zetter.network.packet.CCanvasRequestPacket;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.client.Timer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

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
     * @todo: [MED] It is possible that Race Condition happens here
     */
    private final Map<String, Integer> ticksSinceRenderRequested = Maps.newHashMap();

    /**
     * To avoid request thrashing, textures are requested from server
     * with some timeout/request limiter. Texture will be requested
     * again if it's not loaded yet and this timeout reaches 0 or below
     */
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

    /**
     * Tries to render canvas by it's code, if no code provided
     * creates renderer from provided data
     *
     * @todo: [LOW] Make data optional, provide extra interface
     *
     * @param poseStack
     * @param renderTypeBuffer
     * @param canvasCode
     * @param canvas
     * @param combinedLight
     */
    public void renderCanvas(PoseStack poseStack, MultiBufferSource renderTypeBuffer, String canvasCode, AbstractCanvasData canvas, int combinedLight) {
        // 0 is a reserved fallback value
        if (canvasCode.equals(CanvasData.getCanvasCode(0))) return;

        CanvasRenderer.Instance rendererInstance = this.getCanvasRendererInstance(canvasCode, canvas, false);

        if (canvas.isManaged()) {
            this.ticksSinceRenderRequested.put(canvasCode, 0);

            if (rendererInstance == null) {
                this.queueCanvasTextureUpdate(canvasCode);
                return;
            }
        }

        if (rendererInstance == null) {
            Zetter.LOG.error("Will not render " + canvasCode + ": non-managed canvas is empty!");
            return;
        }

        rendererInstance.render(poseStack, renderTypeBuffer, combinedLight);
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
     * Method for public access: also removes tracking
     * @param canvasCode
     */
    public void removeCanvas(String canvasCode) {
        // To keep it from reloading if for some reason it was not received from server yet
        this.textureRequestTimeout.remove(canvasCode);

        // To disable tracking so it won't be removed twice
        this.ticksSinceRenderRequested.remove(canvasCode);

        if (this.canvasRendererInstances.containsKey(canvasCode)) {
            this.unloadCanvas(canvasCode);
        } else {
            Zetter.LOG.debug("Canvas " + canvasCode + " is not loaded, so can't unregister");
        }
    }

    /**
     * Saying to the server that we no longer want to recieve updates
     * on this canvas since we're not using it
     * @param canvasCode
     */
    private void unloadCanvas(String canvasCode) {
        if (Zetter.DEBUG_CLIENT) {
            Zetter.LOG.debug("Unloading canvas " + canvasCode);
        }

        this.textureRequestTimeout.remove(canvasCode);

        if (!this.canvasRendererInstances.containsKey(canvasCode)) {
            Zetter.LOG.error("Cannot unload canvas " + canvasCode + ", it's not loaded!");
            return;
        }

        // Free the texture
        this.canvasRendererInstances.get(canvasCode).close();
        this.canvasRendererInstances.remove(canvasCode);
    }

    /**
     * @todo: [HIGH] Still makes double-request on first load, markDirty called before update
     * @param canvasCode
     */
    public void queueCanvasTextureUpdate(String canvasCode) {
        if (canvasCode == null) {
            Zetter.LOG.debug("Tried to queue null canvas");
            return;
        }

        if (this.textureRequestTimeout.containsKey(canvasCode)) {
            TextureRequest textureRequest = this.textureRequestTimeout.get(canvasCode);

            // Already requested
            if (textureRequest.isNeedUpdate()) return;

            textureRequest.markDirty();
        } else {
            this.textureRequestTimeout.put(canvasCode, new TextureRequest(canvasCode));
        }
    }

    protected void requestCanvasTexture(TextureRequest request) {
        CCanvasRequestPacket requestSyncPacket = new CCanvasRequestPacket(request.getCanvasCode());
        ZetterNetwork.simpleChannel.sendToServer(requestSyncPacket);

        request.update();
    }

    /*
     * Renderer instances
     */

    private @Nullable CanvasRenderer.Instance getCanvasRendererInstance(String canvasCode, AbstractCanvasData canvasData, boolean create) {
        CanvasRenderer.Instance canvasRendererInstance = this.canvasRendererInstances.get(canvasCode);

        if (create && canvasRendererInstance == null) {
            return this.createCanvasRendererInstance(canvasCode, canvasData);
        }

        return canvasRendererInstance;
    }

    private CanvasRenderer.Instance createCanvasRendererInstance(String canvasCode, AbstractCanvasData canvas) {
        CanvasRenderer.Instance canvasRendererInstance = new CanvasRenderer.Instance(
                canvasCode, canvas.getWidth(), canvas.getHeight(), canvas.getResolution()
        );
        canvasRendererInstance.updateCanvasTexture(canvas);
        this.canvasRendererInstances.put(canvasCode, canvasRendererInstance);

        return canvasRendererInstance;
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
        this.textureRequestTimeout.clear();
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
         * Maybe update with asByteArray() instead of pixel-by-pixel?
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
        private final int TEXTURE_REQUEST_TIMEOUT = 30; // Not often than once in a second and a half

        private final String code;
        private boolean needUpdate = true;
        private int timeout = 0;

        TextureRequest(String canvasCode) {
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
