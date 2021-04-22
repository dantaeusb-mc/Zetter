package com.dantaeusb.zetter.client.gui.artisttable;

import com.dantaeusb.zetter.canvastracker.ICanvasTracker;
import com.dantaeusb.zetter.client.gui.ArtistTableScreen;
import com.dantaeusb.zetter.client.gui.PaintingScreen;
import com.dantaeusb.zetter.client.gui.painting.AbstractPaintingWidget;
import com.dantaeusb.zetter.client.renderer.CanvasRenderer;
import com.dantaeusb.zetter.core.Helper;
import com.dantaeusb.zetter.core.ModItems;
import com.dantaeusb.zetter.item.CanvasItem;
import com.dantaeusb.zetter.storage.CanvasData;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class CombinedCanvasWidget extends AbstractArtistTableWidget implements IRenderable, IGuiEventListener {
    private static final int CANVAS_SCALE_FACTOR = 1;
    private static final int size = Helper.CANVAS_TEXTURE_RESOLUTION * CANVAS_SCALE_FACTOR;

    public CombinedCanvasWidget(ArtistTableScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, size, size, new TranslationTextComponent("container.zetter.painting.canvas"));
    }

    public void render(MatrixStack matrixStack) {
        matrixStack.push();
        matrixStack.translate(this.x, this.y, 1.0F);

        CanvasData canvasData = this.parentScreen.getContainer().getCanvasCombination().canvasData;

        this.drawCanvas(matrixStack, canvasData, 0, 0, 1.0F);

        matrixStack.pop();
    }

    private void drawCanvas(MatrixStack matrixStack, @Nullable CanvasData canvasData, float x, float y, float scale) {
        if (canvasData != null) {
            matrixStack.push();
            matrixStack.translate(x, y, 1.0F);
            matrixStack.scale(scale, scale, 1.0F);

            IRenderTypeBuffer.Impl renderTypeBufferImpl = IRenderTypeBuffer.getImpl(Tessellator.getInstance().getBuffer());
            CanvasRenderer.getInstance().renderCanvas(matrixStack, renderTypeBufferImpl, canvasData, 15728880);
            renderTypeBufferImpl.finish();

            matrixStack.pop();
        }
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
