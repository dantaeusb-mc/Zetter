package me.dantaeusb.zetter.client.renderer.entity;

import com.mojang.math.Matrix4f;
import me.dantaeusb.zetter.canvastracker.ICanvasTracker;
import me.dantaeusb.zetter.client.model.EaselModel;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.entity.item.EaselEntity;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasData;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class EaselRenderer extends EntityRenderer<EaselEntity> {
    public static final ResourceLocation TEXTURE = new ResourceLocation("zetter:textures/entity/easel.png");
    public static final ResourceLocation CANVAS_TEXTURE = new ResourceLocation("zetter:textures/entity/canvas.png");

    protected EaselModel model;
    protected final List<RenderLayer<EaselEntity, EntityModel<EaselEntity>>> layers = Lists.newArrayList();
    protected int canvasRequestTimeout = 0;

    public EaselRenderer(EntityRendererProvider.Context context) {
        super(context);

        this.model = new EaselModel<>(context.bakeLayer(EaselModel.EASEL_BODY_LAYER));
    }

    public final boolean addLayer(RenderLayer<EaselEntity, EntityModel<EaselEntity>> layer) {
        return this.layers.add(layer);
    }

    public void render(EaselEntity easelEntity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Minecraft minecraft = Minecraft.getInstance();
        VertexConsumer vertexBuilder = buffer.getBuffer(RenderType.entityCutout(TEXTURE));

        poseStack.pushPose();
        poseStack.mulPose(Vector3f.YP.rotationDegrees(180.0F - entityYaw));

        // last are r, g, b, a
        this.model.renderToBuffer(poseStack, vertexBuilder, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        if (easelEntity.hasCanvas()) {
            // Doesn't make sense to get CanvasData from item since we're on client, requesting directly from capability
            CanvasData canvasData = getCanvasData(easelEntity.level, easelEntity.getEntityCanvasCode());

            if (canvasData != null) {
                this.renderCanvas(easelEntity, canvasData, partialTicks, poseStack, buffer, packedLight);
            } else {
                CanvasRenderer.getInstance().queueCanvasTextureUpdate(AbstractCanvasData.Type.CANVAS, easelEntity.getEntityCanvasCode());
            }
        }

        poseStack.popPose();
    }

    private void renderCanvas(EaselEntity easelEntity, CanvasData canvasData, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        /**
         * Rendering front side
         * Copied from {@link net.minecraft.client.renderer.entity.ItemFrameRenderer#render}
         */

        final float scaleFactor = 1.0F / 16.0F;

        final int canvasBlockWidth = canvasData.getWidth() / canvasData.getResolution().getNumeric();
        final int canvasBlockHeight = canvasData.getHeight() / canvasData.getResolution().getNumeric();

        // Scale and prepare
        poseStack.scale(scaleFactor, scaleFactor, scaleFactor);
        poseStack.translate(-8.0D, 12.5D, -3.0D);
        poseStack.mulPose(Vector3f.XP.rotation(0.1745F));
        poseStack.mulPose(Vector3f.ZP.rotationDegrees(180.0F));
        poseStack.translate(-8.0D - (8.0D * canvasBlockWidth), -16.0D * canvasBlockHeight, 0.0D);

        CanvasRenderer.getInstance().renderCanvas(poseStack, buffer, easelEntity.getEntityCanvasCode(), canvasData, packedLight);

        /**
         * Rendering canvas back and sides
         */
        final float canvasPixelWidth = canvasBlockWidth * 16.0F;
        final float canvasPixelHeight = canvasBlockHeight * 16.0F;

        final RenderType renderType = RenderType.text(CANVAS_TEXTURE);

        VertexConsumer vertexConsumer = buffer.getBuffer(renderType);

        Matrix4f matrix4f = poseStack.last().pose();

        // Back
        this.renderFace(matrix4f, vertexConsumer, canvasPixelWidth, 0.0F, canvasPixelHeight, 0.0F, 1.0F, 1.0F, packedLight);


        this.renderFace(matrix4f, vertexConsumer, 0.0F, 0.0F, canvasPixelHeight, 0.0F, 0.0F, 1.0F, packedLight);

        this.renderFace(matrix4f, vertexConsumer, canvasPixelWidth, canvasPixelWidth, canvasPixelHeight, 0.0F, 0.0F, 1.0F, packedLight);
    }

    private void renderFace(Matrix4f matrix4f, VertexConsumer vertexConsumer, float x0, float x1, float y0, float y1, float z0, float z1, int packedLight) {
        vertexConsumer.vertex(matrix4f, x0, y0, z0).color(255, 255, 255, 255).uv(0.0F, 1.0F).uv2(packedLight).endVertex();
        vertexConsumer.vertex(matrix4f, x1, y0, z1).color(255, 255, 255, 255).uv(1.0F, 1.0F).uv2(packedLight).endVertex();
        vertexConsumer.vertex(matrix4f, x1, y1, z1).color(255, 255, 255, 255).uv(1.0F, 0.0F).uv2(packedLight).endVertex();
        vertexConsumer.vertex(matrix4f, x0, y1, z0).color(255, 255, 255, 255).uv(0.0F, 0.0F).uv2(packedLight).endVertex();
    }

    private float[] getUV(int x, int y, int width, int height, Direction direction) {
        final float textureWidth = 64.0F;
        final float textureHeight = 64.0F;

        int u = 0;
        int v = 0;

        if (width > 1) {
            if (height > 1) {
                // depends on x, y.
            } else {

            }
        } else if (height > 1) {

        }

        return new float[]{
                u * 16.0F / textureWidth,
                (u + 1) * 16.0F / textureWidth,
                v * 16.0F / textureWidth,
                (v + 1) * 16.0F / textureWidth
        };
    }

    @Nullable
    public static CanvasData getCanvasData(Level world, String canvasName) {
        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(world);

        if (canvasTracker == null) {
            return null;
        }

        return canvasTracker.getCanvasData(canvasName, CanvasData.class);
    }

    /**
     * Returns the location of an entity's texture.
     * @todo: do something with this
     */
    @Override
    public ResourceLocation getTextureLocation(EaselEntity entity) {
        return TEXTURE;
    }
}
