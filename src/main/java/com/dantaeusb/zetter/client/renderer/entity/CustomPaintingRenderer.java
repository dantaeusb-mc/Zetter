package com.dantaeusb.zetter.client.renderer.entity;

import com.dantaeusb.zetter.Zetter;
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
import jdk.nashorn.internal.ir.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelRenderer;
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
    public static final String[] MODEL_CODES = {
        "1x1",
        "top_left",
        "top",
        "top_right",
        "left",
        "right",
        "bottom_left",
        "bottom",
        "bottom_right",
        "top_u",
        "bottom_u",
        "left_u",
        "right_u",
        "center",
        "center_horizontal",
        "center_vertical"
    };

    public static final HashMap<String, ModelResourceLocation> FRAME_MODELS = new HashMap<String, ModelResourceLocation>();
    public static final HashMap<String, ResourceLocation> PLATE_TEXTURES = new HashMap<String, ResourceLocation>();

    static {
        for (String modelCode: CustomPaintingRenderer.MODEL_CODES) {
            for (CustomPaintingEntity.Materials material: CustomPaintingEntity.Materials.values()) {
                CustomPaintingRenderer.FRAME_MODELS.put(material + "/" + modelCode, new ModelResourceLocation("zetter:frame/" + material + "/" + modelCode));
            }
        }

        for (CustomPaintingEntity.Materials material: CustomPaintingEntity.Materials.values()) {
            CustomPaintingRenderer.PLATE_TEXTURES.put(material.toString(), new ResourceLocation(Zetter.MOD_ID, "textures/paintings/entity/frame/plate/" + material + ".png"));
        }
    }

    private final ModelRenderer plate;

    public CustomPaintingRenderer(EntityRendererManager renderManager) {
        super(renderManager);

        this.plate = new ModelRenderer(16, 16, 0, 0);
        this.plate.setRotationPoint(0.0F, 0, 0.0F);
        this.plate.addBox(-3.0F, -1.0F, -2.0F, 6.0F, 2.0F, 2.0F, 0.0F, false);
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

        int blockWidth = entity.getBlockWidth();
        int blockHeight = entity.getBlockHeight();

        if (!flag && canvasData != null) {
            matrixStack.push();
            matrixStack.translate(renderOffset[0] - 1.0F, renderOffset[1] - 1.0F, 0.5D - (1.0D / 16.0D));

            if (blockWidth == 1 && blockHeight == 1) {
                this.renderModel(entity, "1x1", matrixStack, renderBuffers, combinedLight);
            } else if (blockWidth == 1) {
                for (int v = 0; v < blockHeight; v++) {
                    matrixStack.translate(0, -v, 0D);

                    int offsetCombinedLight = WorldRenderer.getCombinedLight(entity.world, CustomPaintingRenderer.getOffsetBlockPos(entity, 0, v));

                    if (v == 0) {
                        this.renderModel(entity, "top_u", matrixStack, renderBuffers, offsetCombinedLight);
                    } else if (v + 1 == blockHeight) {
                        this.renderModel(entity, "bottom_u", matrixStack, renderBuffers, offsetCombinedLight);
                    } else {
                        this.renderModel(entity, "center_vertical", matrixStack, renderBuffers, offsetCombinedLight);
                    }

                    matrixStack.translate(0, v, 0D);
                }
            } else if (blockHeight == 1) {
                for (int h = 0; h < blockWidth; h++) {
                    matrixStack.translate(-h, 0, 0D);

                    int offsetCombinedLight = WorldRenderer.getCombinedLight(entity.world, CustomPaintingRenderer.getOffsetBlockPos(entity, h, 0));

                    if (h == 0) {
                        this.renderModel(entity, "left_u", matrixStack, renderBuffers, offsetCombinedLight);
                    } else if (h + 1 == blockWidth) {
                        this.renderModel(entity, "right_u", matrixStack, renderBuffers, offsetCombinedLight);
                    } else {
                        this.renderModel(entity, "center_horizontal", matrixStack, renderBuffers, offsetCombinedLight);
                    }

                    matrixStack.translate(h, 0, 0D);
                }
            } else {
                /**
                 * @todo: use block pos
                 */
                for (int v = 0; v < blockHeight; v++) {
                    for (int h = 0; h < blockWidth; h++) {
                        matrixStack.translate(-h, -v, 0D);

                        int offsetCombinedLight = WorldRenderer.getCombinedLight(entity.world, CustomPaintingRenderer.getOffsetBlockPos(entity, h, v));

                        if (v == 0) {
                            if (h == 0) {
                                this.renderModel(entity, "top_left", matrixStack, renderBuffers, offsetCombinedLight);
                            } else if (h + 1 == blockWidth) {
                                this.renderModel(entity, "top_right", matrixStack, renderBuffers, offsetCombinedLight);
                            } else {
                                this.renderModel(entity, "top", matrixStack, renderBuffers, offsetCombinedLight);
                            }
                        } else if (v + 1 == blockHeight) {
                            if (h == 0) {
                                this.renderModel(entity, "bottom_left", matrixStack, renderBuffers, offsetCombinedLight);
                            } else if (h + 1 == blockWidth) {
                                this.renderModel(entity, "bottom_right", matrixStack, renderBuffers, offsetCombinedLight);
                            } else {
                                this.renderModel(entity, "bottom", matrixStack, renderBuffers, offsetCombinedLight);
                            }
                        } else {
                            if (h == 0) {
                                this.renderModel(entity, "left", matrixStack, renderBuffers, offsetCombinedLight);
                            } else if (h + 1 == blockWidth) {
                                this.renderModel(entity, "right", matrixStack, renderBuffers, offsetCombinedLight);
                            } else {
                                this.renderModel(entity, "center", matrixStack, renderBuffers, offsetCombinedLight);
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

        if (canvasData != null && entity.hasPlate()) {
            matrixStack.push();

            matrixStack.translate(0.0D, blockHeight / -2.0D, 0.5D);

            final String material = entity.getMaterial().toString();
            IVertexBuilder vertexBuilder = renderBuffers.getBuffer(RenderType.getEntityCutout(PLATE_TEXTURES.get(material)));
            this.plate.render(matrixStack, vertexBuilder, combinedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

            matrixStack.pop();
        }

        matrixStack.pop();

        super.render(entity, entityYaw, partialTicks, matrixStack, renderBuffers, combinedLight);
    }

    private void renderModel(CustomPaintingEntity entity, String key, MatrixStack matrixStack, IRenderTypeBuffer renderBuffers, int combinedLight) {
        ModelResourceLocation modelResourceLocation = FRAME_MODELS.get(entity.getMaterial() + "/" + key);

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

        int xOffset = ((entity.getBlockWidth() + 1) / 2) - 1;
        int yOffset = ((entity.getBlockHeight() + 1) / 2) - 1;

        return entity.getHangingPosition().add(
                (xOffset + h) * facingDirection.getXOffset(),
                yOffset + v,
                (xOffset + h) * facingDirection.getZOffset()
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
        return PLATE_TEXTURES.get("oak");
    }
}
