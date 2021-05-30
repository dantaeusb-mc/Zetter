package com.dantaeusb.zetter.client.renderer.entity;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.canvastracker.ICanvasTracker;
import com.dantaeusb.zetter.client.renderer.CanvasRenderer;
import com.dantaeusb.zetter.client.renderer.entity.model.SmallFrameModel;
import com.dantaeusb.zetter.core.Helper;
import com.dantaeusb.zetter.entity.item.CustomPaintingEntity;
import com.dantaeusb.zetter.storage.CanvasData;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.World;
import net.minecraftforge.client.model.data.EmptyModelData;

import javax.annotation.Nullable;
import java.util.HashMap;

public class CustomPaintingRenderer extends EntityRenderer<CustomPaintingEntity> {
    public static final HashMap<String, ModelResourceLocation> SMALL_FRAME_MODELS = new HashMap<String, ModelResourceLocation>() {{
        put("1x1", new ModelResourceLocation("zetter:block/custom_painting/1x1"));
        put("1x2", new ModelResourceLocation("zetter:block/custom_painting/1x2"));
        put("2x1", new ModelResourceLocation("zetter:block/custom_painting/2x1"));
        put("2x2", new ModelResourceLocation("zetter:block/custom_painting/2x2"));
        put("top_left", new ModelResourceLocation("zetter:block/custom_painting/top_left"));
        put("top", new ModelResourceLocation("zetter:block/custom_painting/top"));
        put("top_right", new ModelResourceLocation("zetter:block/custom_painting/top_right"));
        put("left", new ModelResourceLocation("zetter:block/custom_painting/left"));
        put("right", new ModelResourceLocation("zetter:block/custom_painting/right"));
        put("bottom_left", new ModelResourceLocation("zetter:block/custom_painting/bottom_left"));
        put("bottom", new ModelResourceLocation("zetter:block/custom_painting/bottom"));
        put("bottom_right", new ModelResourceLocation("zetter:block/custom_painting/bottom_right"));
    }};

    private static final ResourceLocation[] FRAME_TEXTURES = new ResourceLocation[] {
            new ResourceLocation(Zetter.MOD_ID, "textures/paintings/entity/frame/small.png"),
    };

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

        // Doesn't make sense to get CanvasData from item since we're on client, requesting directly from capability
        CanvasData canvasData = getCanvasData(world, entity.getCanvasName());

        // Copied from ItemFrameRenderer
        boolean flag = entity.isInvisible();

        if (!flag && canvasData != null) {
            matrixStack.push();
            matrixStack.translate(-0.5D, -0.5D, 0.5D - (1.0D / 16.0D));

            int iHeight = (int) (canvasData.getHeight() / 16.0F);
            int iWidth = (int) (canvasData.getWidth() / 16.0F);

            if (iWidth < 3 && iHeight < 3) {
                final String key = iWidth + "x" + iHeight;

                this.renderModel(key, matrixStack, renderBuffers, combinedLight);
            } else {

                for (int v = 0; v < iHeight; v++) {
                    for (int h = 0; h < iWidth; h++) {
                        // composite model
                        if (v == 0) {
                            if (h == 0) {
                                this.renderModel("top_left", matrixStack, renderBuffers, combinedLight);
                            } else if (h + 1 == iWidth) {
                                this.renderModel("top_right", matrixStack, renderBuffers, combinedLight);
                            } else {
                                this.renderModel("top", matrixStack, renderBuffers, combinedLight);
                            }
                        } else if (v + 1 == iHeight) {
                            if (h == 0) {
                                this.renderModel("bottom_left", matrixStack, renderBuffers, combinedLight);
                            } else if (h + 1 == iWidth) {
                                this.renderModel("bottom_right", matrixStack, renderBuffers, combinedLight);
                            } else {
                                this.renderModel("bottom", matrixStack, renderBuffers, combinedLight);
                            }
                        } else {
                            if (h == 0) {
                                this.renderModel("left", matrixStack, renderBuffers, combinedLight);
                            } else if (h + 1 == iWidth) {
                                this.renderModel("right", matrixStack, renderBuffers, combinedLight);
                            } else {
                                //this.renderModel("bottom", matrixStack, renderBuffers, combinedLight);
                            }
                        }
                    }
                }
            }

            matrixStack.pop();
        }

        if (canvasData != null) {
            matrixStack.push();
            // We want to move picture one pixel in facing direction
            // And half a block towards top left
            matrixStack.translate(-0.5D, -0.5D, 0.5D - (1.0D / 32.0D));

            final float scaleFactor = 1.0F / 16.0F;

            // Scale and prepare
            matrixStack.scale(scaleFactor, scaleFactor, scaleFactor);
            matrixStack.rotate(Vector3f.ZP.rotationDegrees(180.0F));
            matrixStack.translate(-16.0D, -16.0D, 0D);

            CanvasRenderer.getInstance().renderCanvas(matrixStack, renderBuffers, canvasData, combinedLight);
            matrixStack.pop();
        } else {
            CanvasRenderer.getInstance().queueCanvasTextureUpdate(entity.getCanvasName());
        }

        matrixStack.pop();

        super.render(entity, entityYaw, partialTicks, matrixStack, renderBuffers, combinedLight);
    }

    private void renderModel(String key, MatrixStack matrixStack, IRenderTypeBuffer renderBuffers, int combinedLight) {
        ModelResourceLocation modelResourceLocation = SMALL_FRAME_MODELS.get(key);

        MatrixStack.Entry currentMatrix = matrixStack.getLast();
        IVertexBuilder vertexBuffer = renderBuffers.getBuffer(RenderType.getSolid());

        IBakedModel frameModel = Minecraft.getInstance().getModelManager().getModel(modelResourceLocation);

        BlockRendererDispatcher rendererDispatcher = Minecraft.getInstance().getBlockRendererDispatcher();
        rendererDispatcher.getBlockModelRenderer().renderModel(currentMatrix, vertexBuffer, null, frameModel,
                1.0F, 1.0F, 1.0F, combinedLight, OverlayTexture.NO_OVERLAY, EmptyModelData.INSTANCE);
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
        return FRAME_TEXTURES[0];
    }
}
