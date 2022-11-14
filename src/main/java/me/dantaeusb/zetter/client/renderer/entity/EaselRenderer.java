package me.dantaeusb.zetter.client.renderer.entity;

import com.mojang.math.Matrix4f;
import me.dantaeusb.zetter.Zetter;
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
    public static final ResourceLocation TEXTURE = new ResourceLocation(Zetter.MOD_ID, "textures/entity/easel.png");
    public static final ResourceLocation CANVAS_TEXTURE = new ResourceLocation(Zetter.MOD_ID, "textures/entity/canvas.png");

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
                CanvasRenderer.getInstance().queueCanvasTextureUpdate(easelEntity.getEntityCanvasCode());
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

        final RenderType renderType = RenderType.text(CANVAS_TEXTURE);

        VertexConsumer vertexConsumer = buffer.getBuffer(renderType);

        Matrix4f matrix4f = poseStack.last().pose();

        this.renderBack(matrix4f, vertexConsumer, canvasBlockWidth, canvasBlockHeight, packedLight);
        this.renderSidesH(matrix4f, vertexConsumer, canvasBlockWidth, canvasBlockHeight, packedLight);
        this.renderSidesV(matrix4f, vertexConsumer, canvasBlockWidth, canvasBlockHeight, packedLight);
    }

    private void renderBack(Matrix4f matrix4f, VertexConsumer vertexConsumer, int canvasBlockWidth, int canvasBlockHeight, int packedLight) {

        for (int x = 0; x < canvasBlockWidth; x++) {
            for (int y = 0; y < canvasBlockHeight; y++) {
                float[] uv = getUV(x, y, canvasBlockWidth, canvasBlockHeight, Direction.SOUTH);

                this.renderFace(
                        matrix4f, vertexConsumer,
                        x * 16.0F + 16.0F, x * 16.0F,
                        y * 16.0F + 16.0F, y * 16.0F,
                        1.0F, 1.0F, 1.0F, 1.0F,
                        uv[1], uv[0], uv[2], uv[3],
                        packedLight
                );
            }
        }
    }

    private void renderSidesH(Matrix4f matrix4f, VertexConsumer vertexConsumer, int canvasBlockWidth, int canvasBlockHeight, int packedLight) {
        final float canvasPixelWidth = canvasBlockWidth * 16.0F;

        for (int y = 0; y < canvasBlockHeight; y++) {
            float[] uvLeft = getUV(0, y, canvasBlockWidth, canvasBlockHeight, Direction.EAST);

            // Left
            this.renderFace(
                    matrix4f, vertexConsumer,
                    0.0F, 0.0F,
                    y * 16.0F + 16.0F, y * 16.0F,
                    1.0F, 0.0F, 0.0F, 1.0F,
                    uvLeft[0], uvLeft[1], uvLeft[2], uvLeft[3],
                    packedLight
            );

            float[] uvRight = getUV(canvasBlockWidth - 1, y, canvasBlockWidth, canvasBlockHeight, Direction.WEST);

            // Right
            this.renderFace(
                    matrix4f, vertexConsumer,
                    canvasPixelWidth, canvasPixelWidth,
                    y * 16.0F + 16.0F, y * 16.0F,
                    0.0F, 1.0F, 1.0F, 0.0F,
                    uvRight[0], uvRight[1], uvRight[2], uvRight[3],
                    packedLight
            );
        }
    }
    private void renderSidesV(Matrix4f matrix4f, VertexConsumer vertexConsumer, int canvasBlockWidth, int canvasBlockHeight, int packedLight) {
        final float canvasPixelHeight = canvasBlockHeight * 16.0F;

        for (int x = 0; x < canvasBlockWidth; x++) {
            float[] uvTop = getUV(x, 0, canvasBlockWidth, canvasBlockHeight, Direction.UP);

            // Top
            this.renderFace(
                    matrix4f, vertexConsumer,
                    x * 16.0F, x * 16.0F + 16.0F,
                    canvasPixelHeight, canvasPixelHeight,
                    1.0F, 1.0F, 0.0F, 0.0F,
                    uvTop[0], uvTop[1], uvTop[2], uvTop[3],
                    packedLight
            );

            float[] uvBottom = getUV(x, canvasBlockHeight - 1, canvasBlockWidth, canvasBlockHeight, Direction.DOWN);

            // Bottom
            this.renderFace(
                    matrix4f, vertexConsumer,
                    x * 16.0F, x * 16.0F + 16.0F,
                    0.0F, 0.0F,
                    0.0F, 0.0F, 1.0F, 1.0F,
                    uvBottom[0], uvBottom[1], uvBottom[2], uvBottom[3],
                    packedLight
            );
        }
    }

    private void renderFace(Matrix4f matrix4f, VertexConsumer vertexConsumer, float x0, float x1, float y0, float y1, float z0, float z1, float z2, float z3, float u0, float u1, float v0, float v1, int packedLight) {
        vertexConsumer.vertex(matrix4f, x0, y0, z0).color(255, 255, 255, 255).uv(u0, v1).uv2(packedLight).endVertex();
        vertexConsumer.vertex(matrix4f, x1, y0, z1).color(255, 255, 255, 255).uv(u1, v1).uv2(packedLight).endVertex();
        vertexConsumer.vertex(matrix4f, x1, y1, z2).color(255, 255, 255, 255).uv(u1, v0).uv2(packedLight).endVertex();
        vertexConsumer.vertex(matrix4f, x0, y1, z3).color(255, 255, 255, 255).uv(u0, v0).uv2(packedLight).endVertex();
    }

    private float[] getUV(int x, int y, int width, int height, Direction direction) {
        final float textureWidth = 64.0F;
        final float textureHeight = 64.0F;

        int uBlock = 0;
        int vBlock = 0;

        if (width > 1) {
            if (x == 0) {
                uBlock = 1;
            } else if (x == width - 1) {
                uBlock = 3;
            } else {
                uBlock = 2;
            }

            if (height > 1) {
                if (y == 0) {
                    vBlock = 1;
                } else if (y == height - 1) {
                    vBlock = 3;
                } else {
                    vBlock = 2;
                }
            }
        } else if (height > 1) {
            if (y == 0) {
                vBlock = 1;
            } else if (y == height - 1) {
                vBlock = 3;
            } else {
                vBlock = 2;
            }
        }
        // If nothing checks, it's 1x1, so uv is correct

        float uLength = 16.0F;
        float vLength = 16.0F;

        if (direction.getAxis() == Direction.Axis.X) {
            uLength = 1.0F;
        }

        if (direction.getAxis() == Direction.Axis.Y) {
            vLength = 1.0F;
        }

        float u0 = uBlock * 16.0F;
        float v0 = vBlock * 16.0F;

        if (direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
            if (direction.getAxis() == Direction.Axis.X) {
                u0 = (uBlock + 1) * 16.0F - uLength;
            }

            if (direction.getAxis() == Direction.Axis.Y) {
                v0 = (vBlock + 1) * 16.0F - vLength;
            }
        }

        float u1 = u0 + uLength;
        float v1 = v0 + vLength;

        return new float[]{
                u0 / textureWidth,
                u1 / textureWidth,
                v0 / textureWidth,
                v1 / textureHeight
        };
    }

    @Nullable
    public static CanvasData getCanvasData(Level world, String canvasName) {
        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(world);

        if (canvasTracker == null) {
            return null;
        }

        return canvasTracker.getCanvasData(canvasName);
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
