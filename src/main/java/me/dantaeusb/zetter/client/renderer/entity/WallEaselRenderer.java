package me.dantaeusb.zetter.client.renderer.entity;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.model.WallEaselModel;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.entity.item.WallEaselEntity;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class WallEaselRenderer extends EntityWithCanvasRenderer<WallEaselEntity> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(Zetter.MOD_ID, "textures/entity/wall_easel.png");
    public static final ResourceLocation CANVAS_TEXTURE = new ResourceLocation(Zetter.MOD_ID, "textures/entity/canvas.png");

    protected WallEaselModel<WallEaselEntity> model;
    protected final List<RenderLayer<WallEaselEntity, EntityModel<WallEaselEntity>>> layers = Lists.newArrayList();
    protected int canvasRequestTimeout = 0;

    public WallEaselRenderer(EntityRendererProvider.Context context) {
        super(context);

        this.model = new WallEaselModel<>(context.bakeLayer(WallEaselModel.WALL_EASEL_BODY_LAYER));
    }

    public final boolean addLayer(RenderLayer<WallEaselEntity, EntityModel<WallEaselEntity>> layer) {
        return this.layers.add(layer);
    }

    public void render(WallEaselEntity easelEntity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        VertexConsumer vertexBuilder = buffer.getBuffer(RenderType.entityCutout(TEXTURE));

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));

        // last are r, g, b, a
        this.model.renderToBuffer(poseStack, vertexBuilder, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        if (easelEntity.hasCanvas()) {
            // Doesn't make sense to get CanvasData from item since we're on client, requesting directly from capability
            CanvasData canvasData = getCanvasData(easelEntity.level(), easelEntity.getCanvasCode());

            if (canvasData != null) {
                this.renderCanvas(easelEntity, canvasData, partialTicks, poseStack, buffer, packedLight);
            } else {
                CanvasRenderer.getInstance().queueCanvasTextureUpdate(easelEntity.getCanvasCode());
            }
        }

        poseStack.popPose();
    }

    /**
     * Returns the location of an entity's texture.
     * @todo: do something with this
     */
    @Override
    public ResourceLocation getTextureLocation(WallEaselEntity entity) {
        return TEXTURE;
    }
}
