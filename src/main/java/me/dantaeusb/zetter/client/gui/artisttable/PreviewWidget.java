package me.dantaeusb.zetter.client.gui.artisttable;

import com.mojang.blaze3d.matrix.MatrixStack;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.client.gui.ArtistTableScreen;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.Tuple;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class PreviewWidget extends AbstractArtistTableWidget implements IRenderable, IGuiEventListener {
    private static final ITextComponent EMPTY_TITLE = new TranslationTextComponent("container.zetter.artist_table.combined_canvas.empty");
    private static final ITextComponent INVALID_TITLE = new TranslationTextComponent("container.zetter.artist_table.combined_canvas.invalid");
    private static final ITextComponent LOADING_TITLE = new TranslationTextComponent("container.zetter.artist_table.combined_canvas.loading");

    private static final int size = 64;

    public PreviewWidget(ArtistTableScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, size, size, new TranslationTextComponent("container.zetter.artist_table.combined_canvas"));
    }

    public void render(MatrixStack matrixStack) {
        matrixStack.pushPose();
        matrixStack.translate(this.x, this.y, 1.0F);

        DummyCanvasData canvasData = this.parentScreen.getMenu().getAction().getCanvasData();

        this.drawCanvas(matrixStack, canvasData);

        matrixStack.popPose();
    }

    private void drawCanvas(MatrixStack matrixStack, @Nullable DummyCanvasData canvasData) {
        if (canvasData != null) {
            int scale = getScale(canvasData);
            Tuple<Integer, Integer> displacement = getDisplacement(canvasData, scale);

            matrixStack.pushPose();
            matrixStack.translate(displacement.getA(), displacement.getB(), 1.0F);
            matrixStack.scale(scale, scale, 1.0F);

            IRenderTypeBuffer.Impl renderTypeBufferImpl = IRenderTypeBuffer.immediate(Tessellator.getInstance().getBuilder());
            CanvasRenderer.getInstance().renderCanvas(matrixStack, renderTypeBufferImpl, Helper.COMBINED_CANVAS_CODE, canvasData, 0xF000F0);
            renderTypeBufferImpl.endBatch();

            matrixStack.popPose();
        }
    }

    public static Tuple<Integer, Integer> getDisplacement(@Nullable DummyCanvasData canvasData, int scale) {
        if (canvasData != null) {
            int xSize = (canvasData.getWidth() / Helper.getResolution().getNumeric()) * Helper.getBasicResolution().getNumeric() * scale;
            int ySize = (canvasData.getHeight() / Helper.getResolution().getNumeric()) * Helper.getBasicResolution().getNumeric() * scale;

            int width = Helper.getBasicResolution().getNumeric() * ArtistTableMenu.CANVAS_COLUMN_COUNT;
            int height = Helper.getBasicResolution().getNumeric() * ArtistTableMenu.CANVAS_ROW_COUNT;

            return new Tuple<>((width - xSize) / 2, (height - ySize) / 2);
        }

        return new Tuple<>(0, 0);
    }

    public static int getScale(@Nullable DummyCanvasData canvasData) {
        if (canvasData != null) {
            int xScale = ArtistTableMenu.CANVAS_COLUMN_COUNT / (canvasData.getWidth() / Helper.getResolution().getNumeric());
            int yScale = ArtistTableMenu.CANVAS_ROW_COUNT / (canvasData.getHeight() / Helper.getResolution().getNumeric());

            return Math.min(xScale, yScale);
        }

        return 1;
    }

    @Nullable
    public static CanvasData getCanvasData(World world, String canvasName) {
        CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(world);

        return canvasTracker.getCanvasData(canvasName);
    }

    public @Nullable
    ITextComponent getTooltip(int mouseX, int mouseY) {
        switch (this.parentScreen.getMenu().getActionState()) {
            case EMPTY:
                return EMPTY_TITLE;
            case INVALID:
                return INVALID_TITLE;
            case NOT_LOADED:
                return LOADING_TITLE;
        }

        return this.getMessage();
    }
}
