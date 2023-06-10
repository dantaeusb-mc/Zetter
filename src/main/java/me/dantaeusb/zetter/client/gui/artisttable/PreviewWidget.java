package me.dantaeusb.zetter.client.gui.artisttable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.client.gui.ArtistTableScreen;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class PreviewWidget extends AbstractArtistTableWidget implements Renderable, GuiEventListener {
    private static final Component EMPTY_TITLE = Component.translatable("container.zetter.artist_table.combined_canvas.empty");
    private static final Component INVALID_TITLE = Component.translatable("container.zetter.artist_table.combined_canvas.invalid");
    private static final Component LOADING_TITLE = Component.translatable("container.zetter.artist_table.combined_canvas.loading");

    private static final int size = 64;

    public PreviewWidget(ArtistTableScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, size, size, Component.translatable("container.zetter.artist_table.combined_canvas"));
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(this.getX(), this.getY(), 1.0F);

        DummyCanvasData canvasData = this.parentScreen.getMenu().getAction().getCanvasData();

        this.drawCanvas(guiGraphics, canvasData);

        poseStack.popPose();
    }

    private void drawCanvas(GuiGraphics guiGraphics, @Nullable DummyCanvasData canvasData) {
        if (canvasData != null) {
            PoseStack poseStack = guiGraphics.pose();

            int scale = getScale(canvasData);
            Tuple<Integer, Integer> displacement = getDisplacement(canvasData, scale);

            poseStack.pushPose();
            poseStack.translate(displacement.getA(), displacement.getB(), 1.0F);
            poseStack.scale(scale, scale, 1.0F);

            MultiBufferSource.BufferSource renderTypeBufferImpl = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
            CanvasRenderer.getInstance().renderCanvas(poseStack, renderTypeBufferImpl, Helper.COMBINED_CANVAS_CODE, canvasData, 0xF000F0);
            renderTypeBufferImpl.endBatch();

            poseStack.popPose();
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
        CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(world);

        return canvasTracker.getCanvasData(canvasName);
    }

    public @Nullable
    Component getTooltip(int mouseX, int mouseY) {
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

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
    }
}
