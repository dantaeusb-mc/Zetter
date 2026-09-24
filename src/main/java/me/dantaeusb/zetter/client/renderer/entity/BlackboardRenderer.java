package me.dantaeusb.zetter.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.entity.item.BlackboardEntity;
import me.dantaeusb.zetter.entity.item.CanvasHolderEntity;
import me.dantaeusb.zetter.entity.item.PaintingEntity;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Matrix4f;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * A board is a painting frame with slate behind it instead of a canvas, so it is
 * assembled the same way: one block model per block of the frame, tiled, with the
 * middle left open.
 */
public class BlackboardRenderer extends EntityWithCanvasRenderer<BlackboardEntity> {
    private static final Map<BlackboardEntity.Surfaces, ResourceLocation> SURFACES =
        new EnumMap<>(BlackboardEntity.Surfaces.class);

    /**
     * Boards are made of every wood in the game, but the painting frames stop short
     * of the newest two, so those borrow the closest frame they can until their own
     * artwork exists
     */
    private static final Map<BlackboardEntity.Materials, PaintingEntity.Materials> FRAME_MATERIALS =
        new EnumMap<>(BlackboardEntity.Materials.class);

    /**
     * Frame tiles are a pixel deep and hang with their backs flat on the wall
     */
    private static final double FRAME_DEPTH = 1.0D / 16.0D;

    /**
     * Block pixels the slate sits behind the chalk, in the canvas plane's own space
     */
    private static final float SLATE_DEPTH = 0.125F;

    /**
     * Frame tiles, minus the backing plane the painting frames carry: that plane is
     * what a canvas is mounted on, and a board brings its own slate for the job
     */
    private static final Map<String, ModelResourceLocation> FRAME_MODELS = new HashMap<>();

    public static final String[] TILE_CODES = {
        "top_left", "top", "top_right", "bottom_left", "bottom", "bottom_right"
    };

    static {
        for (BlackboardEntity.Surfaces surface : BlackboardEntity.Surfaces.values()) {
            SURFACES.put(surface, new ResourceLocation(Zetter.MOD_ID, "textures/entity/blackboard/" + surface + ".png"));
        }

        for (BlackboardEntity.Materials material : BlackboardEntity.Materials.values()) {
            final PaintingEntity.Materials frame = PaintingEntity.Materials.fromString(material.toString());

            if (frame != null) {
                FRAME_MATERIALS.put(material, frame);
                continue;
            }

            // Cherry and bamboo, which take the nearest frame in tone until they have one
            FRAME_MATERIALS.put(material, material == BlackboardEntity.Materials.BAMBOO
                ? PaintingEntity.Materials.BIRCH
                : PaintingEntity.Materials.OAK);
        }

        for (PaintingEntity.Materials material : FRAME_MATERIALS.values()) {
            for (String tileCode : TILE_CODES) {
                final String key = material + "/" + tileCode;

                FRAME_MODELS.put(key, new ModelResourceLocation(
                    new ResourceLocation(Zetter.MOD_ID, "blackboard/" + key), ""));
            }
        }
    }

    /**
     * Registered by {@link me.dantaeusb.zetter.core.ZetterModels}, which has no other
     * way of knowing these models are wanted — nothing references them from a
     * blockstate
     */
    public static Collection<ModelResourceLocation> getFrameModels() {
        return FRAME_MODELS.values();
    }

    public BlackboardRenderer(EntityRendererProvider.Context context) {
        super(context, SURFACES.get(BlackboardEntity.Surfaces.GREEN));
    }

    /**
     * The slate, not the frame
     */
    @Override
    public ResourceLocation getTextureLocation(BlackboardEntity entity) {
        return SURFACES.get(entity.getMaterial().getSurface());
    }

    @Override
    protected void renderBody(BlackboardEntity entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        this.renderFrame(entity, poseStack, buffer, packedLight);
        this.renderSlate(entity, partialTicks, poseStack, buffer, packedLight);
    }

    /**
     * A slate in a frame has no back and no edges on show, unlike a canvas standing
     * on an easel, so only the face of it is drawn
     */
    @Override
    public void renderCanvas(CanvasHolderEntity entity, CanvasData canvasData, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.mulPoseMatrix(entity.getCanvasMatrixTransform(partialTicks));

        CanvasRenderer.getInstance().renderCanvas(poseStack, buffer, entity.getCanvasCode(), canvasData, packedLight);
    }

    private void renderFrame(BlackboardEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        final PaintingEntity.Materials material = FRAME_MATERIALS.get(entity.getMaterial());

        poseStack.pushPose();

        /*
         * Frame tiles are block models: a block each, with their origin in the bottom
         * left corner and their pixel of depth running towards the wall. The board's
         * own origin is the middle of its bottom edge, and model X runs to the left
         * of anyone looking at it, so the first tile starts one block in from the end.
         */
        poseStack.translate(BlackboardEntity.BLOCK_WIDTH / 2.0D - 1.0D, 0.0D, 0.5D - FRAME_DEPTH);

        for (int v = 0; v < BlackboardEntity.BLOCK_HEIGHT; v++) {
            for (int h = 0; h < BlackboardEntity.BLOCK_WIDTH; h++) {
                poseStack.pushPose();
                poseStack.translate(-h, v, 0.0D);

                this.renderTile(material, tileCode(h, v), poseStack, buffer, packedLight);

                poseStack.popPose();
            }
        }

        poseStack.popPose();
    }

    /**
     * Which of the sixteen frame tiles belongs at this block of the board. The board
     * is two blocks tall, so every tile is an edge and none of the middles are
     * reachable; a taller board would need them.
     */
    private static String tileCode(int h, int v) {
        final String vertical = v + 1 == BlackboardEntity.BLOCK_HEIGHT ? "top" : "bottom";

        if (h == 0) {
            return vertical + "_left";
        }

        if (h + 1 == BlackboardEntity.BLOCK_WIDTH) {
            return vertical + "_right";
        }

        return vertical;
    }

    /**
     * @see FramedPaintingRenderer#renderModel
     */
    private void renderTile(PaintingEntity.Materials material, String tileCode, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        final ModelResourceLocation modelLocation = FRAME_MODELS.get(material + "/" + tileCode);

        if (modelLocation == null) {
            return;
        }

        final BakedModel frameModel = Minecraft.getInstance().getModelManager().getModel(modelLocation);
        final BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();

        dispatcher.getModelRenderer().renderModel(
            poseStack.last(), buffer.getBuffer(RenderType.solid()), null, frameModel,
            1.0F, 1.0F, 1.0F, packedLight, OverlayTexture.NO_OVERLAY,
            ModelData.EMPTY, RenderType.cutout()
        );
    }

    private void renderSlate(BlackboardEntity entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPoseMatrix(entity.getCanvasMatrixTransform(partialTicks));

        final float width = BlackboardEntity.BLOCK_WIDTH * 16.0F;
        final float height = BlackboardEntity.BLOCK_HEIGHT * 16.0F;

        final Matrix4f matrix4f = poseStack.last().pose();
        final VertexConsumer consumer = buffer.getBuffer(RenderType.text(this.getTextureLocation(entity)));

        // Wound the same way as the canvas itself, see CanvasRenderer.Instance#render
        consumer.vertex(matrix4f, 0.0F, height, SLATE_DEPTH).color(255, 255, 255, 255).uv(0.0F, 1.0F).uv2(packedLight).endVertex();
        consumer.vertex(matrix4f, width, height, SLATE_DEPTH).color(255, 255, 255, 255).uv(1.0F, 1.0F).uv2(packedLight).endVertex();
        consumer.vertex(matrix4f, width, 0.0F, SLATE_DEPTH).color(255, 255, 255, 255).uv(1.0F, 0.0F).uv2(packedLight).endVertex();
        consumer.vertex(matrix4f, 0.0F, 0.0F, SLATE_DEPTH).color(255, 255, 255, 255).uv(0.0F, 0.0F).uv2(packedLight).endVertex();

        poseStack.popPose();
    }
}
