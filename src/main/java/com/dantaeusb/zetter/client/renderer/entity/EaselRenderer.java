package com.dantaeusb.zetter.client.renderer.entity;

import com.dantaeusb.zetter.canvastracker.ICanvasTracker;
import com.dantaeusb.zetter.client.model.EaselModel;
import com.dantaeusb.zetter.client.renderer.CanvasRenderer;
import com.dantaeusb.zetter.core.Helper;
import com.dantaeusb.zetter.core.ModEntities;
import com.dantaeusb.zetter.entity.item.EaselEntity;
import com.dantaeusb.zetter.storage.AbstractCanvasData;
import com.dantaeusb.zetter.storage.CanvasData;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class EaselRenderer extends EntityRenderer<EaselEntity> {
    public static final ResourceLocation TEXTURE = new ResourceLocation("zetter:textures/entity/easel.png");
    protected EaselModel model;
    protected final List<RenderLayer<EaselEntity, EntityModel<EaselEntity>>> layers = Lists.newArrayList();
    protected int canvasRequestTimeout = 0;

    public EaselRenderer(EntityRendererProvider.Context context) {
        super(context);

        this.model = new EaselModel<>(context.bakeLayer(ModEntities.EASEL_BODY_LAYER));
    }

    public final boolean addLayer(RenderLayer<EaselEntity, EntityModel<EaselEntity>> layer) {
        return this.layers.add(layer);
    }

    public void render(EaselEntity easelEntity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Minecraft minecraft = Minecraft.getInstance();
        VertexConsumer vertexBuilder = buffer.getBuffer(RenderType.entityCutout(TEXTURE));

        poseStack.pushPose();
        poseStack.mulPose(Vector3f.YP.rotationDegrees(180.0F - entityYaw));

        boolean visible = !easelEntity.isInvisible() && !easelEntity.isInvisibleTo(minecraft.player);

        // last are r, g, b, a
        this.model.renderToBuffer(poseStack, vertexBuilder, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, visible ? 0.01F : 1.0F);

        if (easelEntity.hasCanvas()) {
            // Doesn't make sense to get CanvasData from item since we're on client, requesting directly from capability
            CanvasData canvasData = getCanvasData(easelEntity.level, easelEntity.getCanvasCode());

            if (canvasData != null) {
                /**
                 * Copied from {@link net.minecraft.client.renderer.entity.ItemFrameRenderer#render}
                 */

                final float scaleFactor = 1.0F / 16.0F;

                // Scale and prepare
                poseStack.scale(scaleFactor, scaleFactor, scaleFactor);
                poseStack.translate(-8.0D, 12.5D, -3.0D);
                poseStack.mulPose(Vector3f.XP.rotation(0.1745F));
                poseStack.mulPose(Vector3f.ZP.rotationDegrees(180.0F));
                poseStack.translate(-16.0D, -16.0D, 0.1D);

                CanvasRenderer.getInstance().renderCanvas(poseStack, buffer, easelEntity.getCanvasCode(), canvasData, packedLight);
            } else {
                CanvasRenderer.getInstance().queueCanvasTextureUpdate(AbstractCanvasData.Type.CANVAS, easelEntity.getCanvasCode());
            }
        }

        poseStack.popPose();
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
