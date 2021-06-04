package com.dantaeusb.zetter.client.renderer.entity;

import com.dantaeusb.zetter.canvastracker.ICanvasTracker;
import com.dantaeusb.zetter.client.renderer.CanvasRenderer;
import com.dantaeusb.zetter.core.Helper;
import com.dantaeusb.zetter.entity.item.CustomPaintingEntity;
import com.dantaeusb.zetter.storage.AbstractCanvasData;
import com.dantaeusb.zetter.storage.CanvasData;
import com.dantaeusb.zetter.storage.DummyCanvasData;
import com.dantaeusb.zetter.storage.PaintingData;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.World;
import net.minecraftforge.client.model.data.EmptyModelData;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;

public class CustomPaintingRenderer extends EntityRenderer<CustomPaintingEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/bat.png");
    
    public static final HashMap<String, ModelResourceLocation> FRAME_MODELS = new HashMap<String, ModelResourceLocation>() {{
        put("1x1", new ModelResourceLocation("zetter:block/custom_painting/1x1"));
        put("top_left", new ModelResourceLocation("zetter:block/custom_painting/top_left"));
        put("top", new ModelResourceLocation("zetter:block/custom_painting/top"));
        put("top_right", new ModelResourceLocation("zetter:block/custom_painting/top_right"));
        put("left", new ModelResourceLocation("zetter:block/custom_painting/left"));
        put("right", new ModelResourceLocation("zetter:block/custom_painting/right"));
        put("bottom_left", new ModelResourceLocation("zetter:block/custom_painting/bottom_left"));
        put("bottom", new ModelResourceLocation("zetter:block/custom_painting/bottom"));
        put("bottom_right", new ModelResourceLocation("zetter:block/custom_painting/bottom_right"));
        put("top_u", new ModelResourceLocation("zetter:block/custom_painting/top_u"));
        put("left_u", new ModelResourceLocation("zetter:block/custom_painting/left_u"));
        put("right_u", new ModelResourceLocation("zetter:block/custom_painting/right_u"));
        put("bottom_u", new ModelResourceLocation("zetter:block/custom_painting/bottom_u"));
        put("center", new ModelResourceLocation("zetter:block/custom_painting/center"));
        put("center_horizontal", new ModelResourceLocation("zetter:block/custom_painting/center_horizontal"));
        put("center_vertical", new ModelResourceLocation("zetter:block/custom_painting/center_vertical"));
    }};

    public CustomPaintingRenderer(EntityRendererManager renderManager) {
        super(renderManager);
    }

    public void render(CustomPaintingEntity entity, float entityYaw, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer renderBuffers, int combinedLight) {
        World world = entity.getEntityWorld();

        matrixStack.push();

        /**
         * @todo: use this offset
         */
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
        AbstractCanvasData canvasData = getCanvasData(world, entity.getCanvasCode());

        // Copied from ItemFrameRenderer
        final boolean flag = entity.isInvisible();
        final double[] renderOffset = entity.getRenderOffset();

        if (!flag && canvasData != null) {
            matrixStack.push();
            matrixStack.translate(renderOffset[0] - 1.0F, renderOffset[1] - 1.0F, 0.5D - (1.0D / 16.0D));

            /**
             * @todo: Use entity data, as it's always available, and we won't need to check canvas data
             */
            int iHeight = (int) (canvasData.getHeight() / 16.0F);
            int iWidth = (int) (canvasData.getWidth() / 16.0F);

            if (iWidth == 1 && iHeight == 1) {
                this.renderModel("1x1", matrixStack, renderBuffers, combinedLight);
            } else if (iWidth == 1) {
                for (int v = 0; v < iHeight; v++) {
                    matrixStack.translate(0, -v, 0D);

                    int offsetCombinedLight = WorldRenderer.getCombinedLight(entity.world, CustomPaintingRenderer.getOffsetBlockPos(entity, 0, v));

                    if (v == 0) {
                        this.renderModel("top_u", matrixStack, renderBuffers, offsetCombinedLight);
                    } else if (v + 1 == iHeight) {
                        this.renderModel("bottom_u", matrixStack, renderBuffers, offsetCombinedLight);
                    } else {
                        this.renderModel("center_vertical", matrixStack, renderBuffers, offsetCombinedLight);
                    }

                    matrixStack.translate(0, v, 0D);
                }
            } else if (iHeight == 1) {
                for (int h = 0; h < iWidth; h++) {
                    matrixStack.translate(-h, 0, 0D);

                    int offsetCombinedLight = WorldRenderer.getCombinedLight(entity.world, CustomPaintingRenderer.getOffsetBlockPos(entity, h, 0));

                    if (h == 0) {
                        this.renderModel("left_u", matrixStack, renderBuffers, offsetCombinedLight);
                    } else if (h + 1 == iWidth) {
                        this.renderModel("right_u", matrixStack, renderBuffers, offsetCombinedLight);
                    } else {
                        this.renderModel("center_horizontal", matrixStack, renderBuffers, offsetCombinedLight);
                    }

                    matrixStack.translate(h, 0, 0D);
                }
            } else {
                /**
                 * @todo: use block pos
                 */
                for (int v = 0; v < iHeight; v++) {
                    for (int h = 0; h < iWidth; h++) {
                        matrixStack.translate(-h, -v, 0D);

                        int offsetCombinedLight = WorldRenderer.getCombinedLight(entity.world, CustomPaintingRenderer.getOffsetBlockPos(entity, h, v));

                        if (v == 0) {
                            if (h == 0) {
                                this.renderModel("top_left", matrixStack, renderBuffers, offsetCombinedLight);
                            } else if (h + 1 == iWidth) {
                                this.renderModel("top_right", matrixStack, renderBuffers, offsetCombinedLight);
                            } else {
                                this.renderModel("top", matrixStack, renderBuffers, offsetCombinedLight);
                            }
                        } else if (v + 1 == iHeight) {
                            if (h == 0) {
                                this.renderModel("bottom_left", matrixStack, renderBuffers, offsetCombinedLight);
                            } else if (h + 1 == iWidth) {
                                this.renderModel("bottom_right", matrixStack, renderBuffers, offsetCombinedLight);
                            } else {
                                this.renderModel("bottom", matrixStack, renderBuffers, offsetCombinedLight);
                            }
                        } else {
                            if (h == 0) {
                                this.renderModel("left", matrixStack, renderBuffers, offsetCombinedLight);
                            } else if (h + 1 == iWidth) {
                                this.renderModel("right", matrixStack, renderBuffers, offsetCombinedLight);
                            } else {
                                this.renderModel("center", matrixStack, renderBuffers, offsetCombinedLight);
                            }
                        }

                        matrixStack.translate(h, v, 0D);
                    }
                }
            }

            matrixStack.pop();
        }

        if (canvasData != null) {
            matrixStack.push();
            // We want to move picture one pixel in facing direction
            // And half a block towards top left
            matrixStack.translate(renderOffset[0] - 1.0D, renderOffset[1] - 1.0D, 0.5D - (1.0D / 32.0D));

            final float scaleFactor = 1.0F / 16.0F;

            // Scale and prepare
            matrixStack.scale(scaleFactor, scaleFactor, scaleFactor);
            matrixStack.rotate(Vector3f.ZP.rotationDegrees(180.0F));
            matrixStack.translate(-16.0D, -16.0D, 0D);

            CanvasRenderer.getInstance().renderCanvas(matrixStack, renderBuffers, canvasData, combinedLight);
            matrixStack.pop();
        } else {
            CanvasRenderer.getInstance().queueCanvasTextureUpdate(AbstractCanvasData.Type.PAINTING, entity.getCanvasCode());
        }

        matrixStack.pop();

        super.render(entity, entityYaw, partialTicks, matrixStack, renderBuffers, combinedLight);
    }

    private void renderModel(String key, MatrixStack matrixStack, IRenderTypeBuffer renderBuffers, int combinedLight) {
        ModelResourceLocation modelResourceLocation = FRAME_MODELS.get(key);

        MatrixStack.Entry currentMatrix = matrixStack.getLast();
        IVertexBuilder vertexBuffer = renderBuffers.getBuffer(RenderType.getSolid());

        IBakedModel frameModel = Minecraft.getInstance().getModelManager().getModel(modelResourceLocation);

        BlockRendererDispatcher rendererDispatcher = Minecraft.getInstance().getBlockRendererDispatcher();
        rendererDispatcher.getBlockModelRenderer().renderModel(currentMatrix, vertexBuffer, null, frameModel,
                1.0F, 1.0F, 1.0F, combinedLight, OverlayTexture.NO_OVERLAY, EmptyModelData.INSTANCE);
    }

    public static BlockPos getOffsetBlockPos(CustomPaintingEntity entity, int h, int v) {
        Direction facingDirection = entity.getHorizontalFacing();
        facingDirection = facingDirection.rotateYCCW();

        final int[] renderOffset = Arrays.stream(entity.getRenderOffset()).mapToInt(num -> (int) Math.floor(num)).toArray();

        return entity.getHangingPosition().add(
            (h - renderOffset[0]) * facingDirection.getXOffset(),
            (v - renderOffset[1]),
            (h - renderOffset[0]) * facingDirection.getZOffset()
        );
    }

    @Nullable
    public static PaintingData getCanvasData(World world, String canvasName) {
        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(world);

        if (canvasTracker == null) {
            return null;
        }

        return canvasTracker.getCanvasData(canvasName, PaintingData.class);
    }

    /**
     * Returns the location of an entity's texture.
     * @todo: do something with this
     */
    public ResourceLocation getEntityTexture(CustomPaintingEntity entity) {
        return TEXTURE;
    }
}
