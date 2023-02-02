package me.dantaeusb.zetter.client.renderer.entity;

import com.mojang.math.Axis;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.entity.item.PaintingEntity;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.HashMap;

public class FramedPaintingRenderer extends EntityRenderer<PaintingEntity> {
    public static ModelLayerLocation PAINTING_PLATE_LAYER = new ModelLayerLocation(new ResourceLocation(Zetter.MOD_ID, "custom_painting"), "plate_layer");

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

    public static final HashMap<String, ModelResourceLocation> FRAME_MODELS = new HashMap<>();
    public static final HashMap<String, ResourceLocation> PLATE_TEXTURES = new HashMap<>();

    private final ModelPart platePart;

    public FramedPaintingRenderer(EntityRendererProvider.Context context) {
        super(context);

        this.platePart = context.bakeLayer(FramedPaintingRenderer.PAINTING_PLATE_LAYER);
    }

    static {
        for (String modelCode: FramedPaintingRenderer.MODEL_CODES) {
            for (PaintingEntity.Materials material: PaintingEntity.Materials.values()) {
                // I don't know why vanilla uses "inventory" even for non-inventory models, but we copy that
                FramedPaintingRenderer.FRAME_MODELS.put(material + "/" + modelCode, new ModelResourceLocation(new ResourceLocation(Zetter.MOD_ID, "frame/" + material + "/" + modelCode), ""));
            }
        }

        for (PaintingEntity.Materials material: PaintingEntity.Materials.values()) {
            if (material.canHavePlate()) {
                // @todo: [HIGH] Use baked parts
                FramedPaintingRenderer.PLATE_TEXTURES.put(material.toString(), new ResourceLocation(Zetter.MOD_ID, "textures/entity/frame/plate/" + material + ".png"));
            }
        }
    }

    public static LayerDefinition createPlateLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("main", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -1.0F, -2.0F, 6.0F, 2.0F, 2.0F), PartPose.ZERO);
        return LayerDefinition.create(meshdefinition, 16, 16);
    }

    public void render(PaintingEntity entity, float entityYaw, float partialTicks, PoseStack matrixStack, MultiBufferSource renderBuffers, int combinedLight) {
        Level world = entity.getCommandSenderWorld();

        matrixStack.pushPose();

        /**
         * @todo: use this offset
         */
        Vec3 vector3d = this.getRenderOffset(entity, partialTicks);
        matrixStack.translate(-vector3d.x(), -vector3d.y(), -vector3d.z());

        Direction facingDirection = entity.getDirection();

        // Position is bind to the bounding box center, paintings are 1/16 thick, therefore we
        // need to divide that by to to get correct offset
        final double offsetAlignment = 0.5D - (1.0D / 32.0D);

        // On directions perpendicular to the facing it would be just 0
        matrixStack.translate((double)facingDirection.getStepX() * offsetAlignment, (double)facingDirection.getStepY() * offsetAlignment, (double)facingDirection.getStepZ() * offsetAlignment);
        matrixStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));

        // Copied from ItemFrameRenderer
        final boolean flag = entity.isInvisible();
        final double[] renderOffset = entity.getRenderOffset();

        int blockWidth = entity.getBlockWidth();
        int blockHeight = entity.getBlockHeight();

        if (!flag) {
            matrixStack.pushPose();
            matrixStack.translate(renderOffset[0] - 1.0F, renderOffset[1] - 1.0F, 0.5D - (1.0D / 16.0D));

            if (blockWidth == 1 && blockHeight == 1) {
                this.renderModel(entity, "1x1", matrixStack, renderBuffers, combinedLight);
            } else if (blockWidth == 1) {
                for (int v = 0; v < blockHeight; v++) {
                    matrixStack.translate(0, -v, 0D);

                    int offsetCombinedLight = LevelRenderer.getLightColor(entity.level, FramedPaintingRenderer.getOffsetBlockPos(entity, 0, v));

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

                    int offsetCombinedLight = LevelRenderer.getLightColor(entity.level, FramedPaintingRenderer.getOffsetBlockPos(entity, h, 0));

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

                        int offsetCombinedLight = LevelRenderer.getLightColor(entity.level, FramedPaintingRenderer.getOffsetBlockPos(entity, h, v));

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

            matrixStack.popPose();
        }

        // Doesn't make sense to get CanvasData from item since we're on client, requesting directly from capability
        AbstractCanvasData canvasData = getCanvasData(world, entity.getPaintingCode());

        // @todo: Has painting - render fallback, no painting - disable canvas render
        if (canvasData != null) {
            matrixStack.pushPose();
            double canvasOffsetZ = entity.getMaterial().hasOffset() ? 0.5D - (1.0D / 32.0D) : 0.5D - (1.0D / 16);

            // We want to move picture one pixel in facing direction
            // And half a block towards top left
            matrixStack.translate(renderOffset[0] - 1.0D, renderOffset[1] - 1.0D, canvasOffsetZ);

            final float scaleFactor = 1.0F / 16.0F;

            // Scale and prepare
            matrixStack.scale(scaleFactor, scaleFactor, scaleFactor);
            matrixStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            matrixStack.translate(-16.0D, -16.0D, 0D);

            CanvasRenderer.getInstance().renderCanvas(matrixStack, renderBuffers, entity.getPaintingCode(), canvasData, combinedLight);
            matrixStack.popPose();
        } else {
            CanvasRenderer.getInstance().queueCanvasTextureUpdate(entity.getPaintingCode());
        }

        // Render plate
        if (canvasData != null && entity.hasPlate()) {
            matrixStack.pushPose();

            matrixStack.translate(0.0D, blockHeight / -2.0D, 0.5D);

            final String material = entity.getMaterial().toString();
            VertexConsumer vertexBuilder = renderBuffers.getBuffer(RenderType.entityCutout(PLATE_TEXTURES.get(material)));
            this.platePart.render(matrixStack, vertexBuilder, combinedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

            matrixStack.popPose();
        }

        matrixStack.popPose();

        super.render(entity, entityYaw, partialTicks, matrixStack, renderBuffers, combinedLight);
    }

    private void renderModel(PaintingEntity entity, String key, PoseStack matrixStack, MultiBufferSource renderBuffers, int combinedLight) {
        ModelResourceLocation modelResourceLocation = FRAME_MODELS.get(entity.getMaterial() + "/" + key);

        PoseStack.Pose currentMatrix = matrixStack.last();
        VertexConsumer vertexBuffer = renderBuffers.getBuffer(RenderType.solid());

        BakedModel frameModel = Minecraft.getInstance().getModelManager().getModel(modelResourceLocation);

        BlockRenderDispatcher rendererDispatcher = Minecraft.getInstance().getBlockRenderer();
        rendererDispatcher.getModelRenderer().renderModel(
                currentMatrix, vertexBuffer, null, frameModel,
                1.0F, 1.0F, 1.0F, combinedLight, OverlayTexture.NO_OVERLAY,
                net.minecraftforge.client.model.data.ModelData.EMPTY,
                RenderType.cutout()
        );
    }

    public static BlockPos getOffsetBlockPos(PaintingEntity entity, int h, int v) {
        Direction facingDirection = entity.getDirection();
        facingDirection = facingDirection.getCounterClockWise();

        int xOffset = ((entity.getBlockWidth() + 1) / 2) - 1;
        int yOffset = ((entity.getBlockHeight() + 1) / 2) - 1;

        return entity.getPos().offset(
                (xOffset + h) * facingDirection.getStepX(),
                yOffset + v,
                (xOffset + h) * facingDirection.getStepZ()
        );
    }

    @Nullable
    public static PaintingData getCanvasData(Level world, String canvasName) {
        CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(world);

        if (canvasTracker == null) {
            return null;
        }

        return canvasTracker.getCanvasData(canvasName);
    }

    /**
     * Returns the location of an entity's texture.
     * @todo: do something with this
     */
    public ResourceLocation getTextureLocation(PaintingEntity entity) {
        return PLATE_TEXTURES.get("oak");
    }
}
