package me.dantaeusb.zetter.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.model.StandBoardModel;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.entity.item.AbstractBoardEntity;
import me.dantaeusb.zetter.entity.item.CanvasHolderEntity;
import me.dantaeusb.zetter.entity.item.StandBoardEntity;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

public class StandBoardRenderer extends EntityWithCanvasRenderer<StandBoardEntity> {
    /**
     * One per wood, slate included: the slate is part of the model, so it comes in
     * the colour the wood is paired with rather than being drawn separately
     */
    private static final Map<AbstractBoardEntity.Materials, ResourceLocation> TEXTURES =
        new EnumMap<>(AbstractBoardEntity.Materials.class);

    static {
        for (AbstractBoardEntity.Materials material : AbstractBoardEntity.Materials.values()) {
            TEXTURES.put(material, new ResourceLocation(Zetter.MOD_ID, "textures/entity/standing_board/" + material + ".png"));
        }
    }

    public StandBoardRenderer(EntityRendererProvider.Context context) {
        super(context, new StandBoardModel<>(context.bakeLayer(StandBoardModel.STAND_BOARD_BODY_LAYER)), TEXTURES.get(AbstractBoardEntity.Materials.OAK));
    }

    @Override
    public ResourceLocation getTextureLocation(StandBoardEntity entity) {
        return TEXTURES.get(entity.getMaterial());
    }

    /**
     * The model is in Blockbench's export space, see {@link StandBoardModel}: flip it
     * the way a living entity renderer would, which puts its feet on the ground, then
     * turn it a quarter so the face that looks along -X looks along -Z, which is the
     * front in the space the canvas is placed in
     */
    @Override
    protected void renderBody(StandBoardEntity entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.5F, 0.0F);

        super.renderBody(entity, partialTicks, poseStack, buffer, packedLight);

        poseStack.popPose();
    }

    /**
     * The slate is part of the model and the frame covers its edges, so only the
     * face of the drawing is needed, unlike a canvas standing on an easel
     */
    @Override
    public void renderCanvas(CanvasHolderEntity entity, CanvasData canvasData, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.mulPoseMatrix(entity.getCanvasMatrixTransform(partialTicks));

        CanvasRenderer.getInstance().renderCanvas(poseStack, buffer, entity.getCanvasCode(), canvasData, packedLight);
    }
}
