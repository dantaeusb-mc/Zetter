package com.dantaeusb.zetter.client.renderer.tileentity;

import com.dantaeusb.zetter.block.EaselBlock;
import com.dantaeusb.zetter.canvastracker.ICanvasTracker;
import com.dantaeusb.zetter.client.gui.CanvasRenderer;
import com.dantaeusb.zetter.core.Helper;
import com.dantaeusb.zetter.core.ModBlocks;
import com.dantaeusb.zetter.tileentity.EaselTileEntity;
import com.dantaeusb.zetter.storage.CanvasData;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.block.*;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class EaselTileEntityRenderer extends TileEntityRenderer<EaselTileEntity> {
    private final ModelRenderer rack;
    private final ModelRenderer canvas;
    private final ModelRenderer topPlank;
    private final ModelRenderer backLeg;
    private final ModelRenderer frontLegs;

    public static final ResourceLocation TEXTURE = new ResourceLocation("zetter:textures/paintings/entity/easel.png");

    public EaselTileEntityRenderer(TileEntityRendererDispatcher rendererDispatcherIn) {
        super(rendererDispatcherIn);

        this.rack = new ModelRenderer(64, 64, 0, 0);
        this.rack.setRotationPoint(0.0F, 0, 0.0F);
        this.rack.addBox(1.0F, 11.5F, 3.5F, 14.0F, 1.0F, 4.0F, 0.0F, false);

        this.canvas = new ModelRenderer(64, 64, 6, 5);
        this.canvas.setRotationPoint(0.0F, 0.0F, 0.0F);
        setRotationAngle(this.canvas, 0.1745F, 0.0F, 0.0F);
        this.canvas.addBox(0.0F, 12.0F, 3.0F, 16.0F, 18.0F, 1.0F, 0.0F, false);

        this.topPlank = new ModelRenderer(64, 64, 0, 0);
        this.topPlank.setRotationPoint(0.0F, 0.0F, 0.0F);
        setRotationAngle(topPlank, 0.1745F, 0.0F, 0.0F);
        this.topPlank.addBox(1.0F, 26.0F, 5.0F, 14.0F, 2.0F, 1.0F, 0.0F, false);

        this.backLeg = new ModelRenderer(64, 64, 0, 6);
        this.backLeg.setRotationPoint(0.0F, 0.0F, 15.0F);
        setRotationAngle(backLeg, -0.2182F, 0.0F, 0.0F);
        this.backLeg.addBox(7.0F, 0.0F, 0.0F, 2.0F, 30.0F, 1.0F, 0.0F, false);

        this.frontLegs = new ModelRenderer(64, 64, 0, 6);
        this.frontLegs.setRotationPoint(0.0F, 0.0F, -3.0F);
        setRotationAngle(frontLegs, 0.1745F, 0.0F, 0.0F);
        this.frontLegs.addBox(12.0F, 1.0F, 7.0F, 2.0F, 31.0F, 1.0F, 0.0F, false);
        this.frontLegs.addBox(2.0F, 1.0F, 7.0F, 2.0F, 31.0F, 1.0F, 0.0F, false);

        //this.canvasTexture = new DynamicTexture(CanvasItem.CANVAS_SIZE, CanvasItem.CANVAS_SIZE, true);
        //this.textureManager = Minecraft.getInstance().getTextureManager();
        //this.canvasRenderType = this.textureManager.getDynamicTextureLocation("canvas/" + mapdataIn.getName(), this.canvasTexture);
    }

    /**
     * @todo: replace with TE packet
     */
    private boolean stoopidUpdate = false;

    public void render(EaselTileEntity tileEntity, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer renderTypeBuffer, int combinedLight, int combinedOverlay) {
        World world = tileEntity.getWorld();
        boolean flag = world != null;
        BlockState blockState = flag ? tileEntity.getBlockState() : ModBlocks.EASEL.getDefaultState().with(EaselBlock.FACING, Direction.SOUTH);

        IVertexBuilder vertexBuilder = renderTypeBuffer.getBuffer(RenderType.getEntityCutout(TEXTURE));

        matrixStack.push();

        float facingAngle = blockState.get(ChestBlock.FACING).getHorizontalAngle();
        matrixStack.translate(0.5D, 0.5D, 0.5D);
        matrixStack.rotate(Vector3f.YP.rotationDegrees(-facingAngle));
        matrixStack.translate(-0.5D, -0.5D, -0.5D);

        rack.render(matrixStack, vertexBuilder, combinedLight, combinedOverlay);
        topPlank.render(matrixStack, vertexBuilder, combinedLight, combinedOverlay);
        backLeg.render(matrixStack, vertexBuilder, combinedLight, combinedOverlay);
        frontLegs.render(matrixStack, vertexBuilder, combinedLight, combinedOverlay);
        canvas.render(matrixStack, vertexBuilder, combinedLight, combinedOverlay);

        if (tileEntity.hasCanvas()) {
            // Doesn't make sense to get CanvasData from item since we're on client, requesting directly from capability
            CanvasData canvasData = getCanvasData(world, tileEntity.getCanvasName());

            if (canvasData != null) {
                /**
                 * Copied from {@link net.minecraft.client.renderer.entity.ItemFrameRenderer#render}
                 */

                final float scaleFactor = 1.0F / 16.0F;

                // Scale and prepare
                matrixStack.scale(scaleFactor, scaleFactor, scaleFactor);
                matrixStack.translate(0.0D, 12.5D, 5.0D);
                matrixStack.rotate(Vector3f.XP.rotation(0.1745F));

                matrixStack.rotate(Vector3f.ZP.rotationDegrees(180.0F));
                matrixStack.translate(-16.0D, -16.0D, 0.0D);
                matrixStack.translate(0.0D, 0.0D, 0.1D);

                CanvasRenderer.getInstance().renderCanvas(matrixStack, renderTypeBuffer, canvasData, combinedLight);
            } else {
                CanvasRenderer.getInstance().queueCanvasTextureUpdate(tileEntity.getCanvasName());
            }
        }

        matrixStack.pop();
    }

    public static void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.rotateAngleX = x;
        modelRenderer.rotateAngleY = y;
        modelRenderer.rotateAngleZ = z;
    }

    @Nullable
    public static CanvasData getCanvasData(World world, String canvasName) {
        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(world);

        if (canvasTracker == null) {
            return null;
        }

        return canvasTracker.getCanvasData(canvasName);
    }
}
