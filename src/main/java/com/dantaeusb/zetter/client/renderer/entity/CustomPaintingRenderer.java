package com.dantaeusb.zetter.client.renderer.entity;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.canvastracker.ICanvasTracker;
import com.dantaeusb.zetter.client.gui.CanvasRenderer;
import com.dantaeusb.zetter.core.Helper;
import com.dantaeusb.zetter.entity.item.CustomPaintingEntity;
import com.dantaeusb.zetter.storage.CanvasData;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Atlases;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelManager;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.item.ItemFrameEntity;
import net.minecraft.item.FilledMapItem;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.World;
import net.minecraftforge.client.model.data.EmptyModelData;

import javax.annotation.Nullable;

public class CustomPaintingRenderer extends EntityRenderer<CustomPaintingEntity> {
    //public static final ModelResourceLocation LOCATION_MODEL = new ModelResourceLocation((new ResourceLocation(Zetter.MOD_ID, "block/custom_painting")).toString());
    public static final ModelResourceLocation LOCATION_MODEL = new ModelResourceLocation("zetter:block/custom_painting");

    public CustomPaintingRenderer(EntityRendererManager renderManager) {
        super(renderManager);
    }

    public void render(CustomPaintingEntity entity, float entityYaw, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer renderBuffers, int combinedLight) {
        World world = entity.getEntityWorld();

        matrixStack.push();

        Vector3d vector3d = this.getRenderOffset(entity, partialTicks);
        matrixStack.translate(-vector3d.getX(), -vector3d.getY(), -vector3d.getZ());

        Direction facingDirection = entity.getHorizontalFacing();

        // Position is bind to the bounding box center, paintings are 1/16 thick, therefore we
        // need to divide that by to to get correct offset
        final double offsetAlignment = 0.5D - (1.0D / 32.0D);

        // On directions perpendicular to the facing it would be just 0
        matrixStack.translate((double)facingDirection.getXOffset() * offsetAlignment, (double)facingDirection.getYOffset() * offsetAlignment, (double)facingDirection.getZOffset() * offsetAlignment);
        matrixStack.rotate(Vector3f.YP.rotationDegrees(180.0F - entity.rotationYaw));

        // Copied from ItemFrameRenderer
        boolean flag = entity.isInvisible();
        if (!flag) {
            BlockRendererDispatcher rendererDispatcher = Minecraft.getInstance().getBlockRendererDispatcher();
            ModelManager modelManager = Minecraft.getInstance().getModelManager();
            matrixStack.push();
            matrixStack.translate(-0.5D, -0.5D, -0.5D);

            MatrixStack.Entry currentMatrix = matrixStack.getLast();
            IVertexBuilder vertexBuffer = renderBuffers.getBuffer(RenderType.getSolid());
            IBakedModel frameModel = Minecraft.getInstance().getModelManager().getModel(LOCATION_MODEL);

            rendererDispatcher.getBlockModelRenderer().renderModel(currentMatrix, vertexBuffer, null, frameModel,
                    1.0F, 1.0F, 1.0F, combinedLight, OverlayTexture.NO_OVERLAY, EmptyModelData.INSTANCE);

            matrixStack.pop();
        }

        // Doesn't make sense to get CanvasData from item since we're on client, requesting directly from capability
        CanvasData canvasData = getCanvasData(world, entity.getCanvasName());

        if (canvasData != null) {
            // We want to move picture one pixel in facing direction
            // And half a block towards top left
            matrixStack.translate(-0.5D, -0.5D, 0.5D - (1.0D / 16.0D));

            final float scaleFactor = 1.0F / 16.0F;

            // Scale and prepare
            matrixStack.scale(scaleFactor, scaleFactor, scaleFactor);
            matrixStack.rotate(Vector3f.ZP.rotationDegrees(180.0F));
            matrixStack.translate(-16.0D, -16.0D, 0D);

            CanvasRenderer.getInstance().renderCanvas(matrixStack, renderBuffers, canvasData, combinedLight);
        } else {
            CanvasRenderer.getInstance().queueCanvasTextureUpdate(entity.getCanvasName());
        }

        matrixStack.pop();

        super.render(entity, entityYaw, partialTicks, matrixStack, renderBuffers, combinedLight);
    }

    @Nullable
    public static CanvasData getCanvasData(World world, String canvasName) {
        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(world);

        if (canvasTracker == null) {
            return null;
        }

        return canvasTracker.getCanvasData(canvasName);
    }

    /**
     * Returns the location of an entity's texture.
     */
    public ResourceLocation getEntityTexture(CustomPaintingEntity entity) {
        return Minecraft.getInstance().getPaintingSpriteUploader().getBackSprite().getAtlasTexture().getTextureLocation();
    }
}
