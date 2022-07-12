package me.dantaeusb.zetter.client.gui.artisttable;

import me.dantaeusb.zetter.canvastracker.ICanvasTracker;
import me.dantaeusb.zetter.client.gui.ArtistTableScreen;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.util.Tuple;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class CombinedCanvasWidget extends AbstractArtistTableWidget implements Widget, GuiEventListener {
    private static final int size = Helper.getBasicResolution().getNumeric();

    public CombinedCanvasWidget(ArtistTableScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, size, size, new TranslatableComponent("container.zetter.artist_table.combined_canvas"));
    }

    // @todo: [HIGH] Why double-push?
    public void render(PoseStack matrixStack) {
        matrixStack.pushPose();
        matrixStack.translate(this.x, this.y, 1.0F);

        DummyCanvasData canvasData = this.parentScreen.getMenu().getCanvasCombination().canvasData;

        this.drawCanvas(matrixStack, canvasData);

        matrixStack.popPose();
    }

    private void drawCanvas(PoseStack matrixStack, @Nullable DummyCanvasData canvasData) {
        if (canvasData != null) {
            int scale = getScale(canvasData);
            Tuple<Integer, Integer> displacement = getDisplacement(canvasData, scale);

            matrixStack.pushPose();
            matrixStack.translate(displacement.getA(), displacement.getB(), 1.0F);
            matrixStack.scale(scale, scale, 1.0F);

            MultiBufferSource.BufferSource renderTypeBufferImpl = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
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
    public static CanvasData getCanvasData(Level world, String canvasName) {
        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(world);

        if (canvasTracker == null) {
            return null;
        }

        return canvasTracker.getCanvasData(canvasName, CanvasData.class);
    }

    @Override
    public void updateNarration(NarrationElementOutput p_169152_) {

    }
}
