package com.dantaeusb.immersivemp.locks.client.gui;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.inventory.container.LockTableContainer;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.BiFunction;

public class PaintingScreen extends ContainerScreen<LockTableContainer> {
    private final ByteBuffer canvas = ByteBuffer.allocateDirect(16 * 16 * 4);
    private final ByteBuffer palette = ByteBuffer.allocateDirect(14 * 4);

    protected final ITextComponent title = new TranslationTextComponent("container.immersivemp.lock_table");

    // This is the resource location for the background image
    private static final ResourceLocation PAINTING_RESOURCE = new ResourceLocation("immersivemp", "textures/paintings/gui/painting.png");

    private static final int PALETTE_SLOTS = 14;

    private int currentPaletteSlot = 0;

    private float sliderHuePercent = 0.0f;
    private float sliderSaturationPercent = 0.0f;
    private float sliderValuePercent = 0.0f;

    public PaintingScreen(LockTableContainer lockTableContainer, PlayerInventory playerInventory, ITextComponent title) {
        super(lockTableContainer, playerInventory, title);

        this.xSize = 176;
        this.ySize = 166;

        canvas.order(ByteOrder.BIG_ENDIAN);
    }

    @Override
    protected void init() {
        super.init();
        this.tempPropagateData();
    }

    protected void tempPropagateData() {
        this.setColor(this.palette, 0, 0xFFAA0000); //dark-red
        this.setColor(this.palette, 1, 0xFFFF5555); //red
        this.setColor(this.palette, 2, 0xFFFFAA00); //gold
        this.setColor(this.palette, 3, 0xFFFFFF55); //yellow
        this.setColor(this.palette, 4, 0xFF00AA00); //dark-green
        this.setColor(this.palette, 5, 0xFF55FF55); //green
        this.setColor(this.palette, 6, 0xFF55FFFF); //aqua
        this.setColor(this.palette, 7, 0xFF00AAAA); //dark-aqua
        this.setColor(this.palette, 8, 0xFF0000AA); //dark-blue
        this.setColor(this.palette, 9, 0xFF5555FF); //blue
        this.setColor(this.palette, 10, 0xFFFF55FF); //light-purple
        this.setColor(this.palette, 11, 0xFFAA00AA); //purple
        this.setColor(this.palette, 12, 0xFFAAAAAA); //gray
        this.setColor(this.palette, 13, 0xFF555555); //dark-gray

        for (int i = 0; i < 16 * 16; i++) {
            canvas.putInt(i * 4, 0xFF000000);
        }
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    public void tick() {
        super.tick();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int iMouseX = (int) mouseX;
        int iMouseY = (int) mouseY;

        this.handleCanvasClick(iMouseX, iMouseY);
        this.handlePaletteClick(iMouseX, iMouseY);
        this.handleSliderClick(iMouseX, iMouseY);

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int x, int y) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bindTexture(PAINTING_RESOURCE);

        this.blit(matrixStack, this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);

        this.drawCanvas(matrixStack);

        this.drawPalette(matrixStack);
        this.drawPaletteSelector(matrixStack);

        this.drawSliders(matrixStack);
        this.drawHandlers(matrixStack);
    }

    /**
     * @param matrixStack
     * @param mouseX
     * @param mouseY
     */
    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack matrixStack, int mouseX, int mouseY) {
        // draw selected palette color

        // drawPaletteSelector(matrixStack);

        // draw HSV sliders
    }

    /**
     * Canvas
     */
    final int CANVAS_POSITION_X = 48;
    final int CANVAS_POSITION_Y = 9;
    final int CANVAS_SCALE_FACTOR = 5;

    protected void drawCanvas(MatrixStack matrixStack) {
        int canvasGlobalLeft = this.guiLeft + CANVAS_POSITION_X;
        int canvasGlobalTop = this.guiTop + CANVAS_POSITION_Y;

        for (int i = 0; i < 16 * 16; i++) {
            int localX = i % 16;
            int localY = i / 16;

            int color = this.getColor(this.canvas, i);
            int globalX = canvasGlobalLeft + localX * CANVAS_SCALE_FACTOR;
            int globalY = canvasGlobalTop + localY * CANVAS_SCALE_FACTOR;

            this.fillGradient(matrixStack, globalX, globalY, globalX + CANVAS_SCALE_FACTOR, globalY + CANVAS_SCALE_FACTOR, color, color);
        }
    }

    /**
     * You can draw on the left-hand side by clicking outside canvas on the right (rounding error)
     * @param mouseX
     * @param mouseY
     */
    protected void handleCanvasClick(final int mouseX, final int mouseY) {
        int canvasGlobalLeft = this.guiLeft + CANVAS_POSITION_X;
        int canvasGlobalTop = this.guiTop + CANVAS_POSITION_Y;

        if (!isInRect(canvasGlobalLeft, canvasGlobalTop, CANVAS_SCALE_FACTOR * 16, CANVAS_SCALE_FACTOR * 16, mouseX, mouseY)) {
            return;
        }

        ImmersiveMp.LOG.info("Canvas click!");

        int localX = mouseX - canvasGlobalLeft;
        int localY = mouseY - canvasGlobalTop;

        this.adjustCanvasPixel(localX / CANVAS_SCALE_FACTOR, localY / CANVAS_SCALE_FACTOR, this.getCurrentColor());
    }

    protected void adjustCanvasPixel(int pixelX, int pixelY, int color) {
        int index = pixelY * 16 + pixelX;

        this.canvas.putInt(index * 4, color);
    }

    /**
     * Palette
     */

    final int PALETTE_POSITION_X = 141;
    final int PALETTE_POSITION_Y = 11;
    final int PALETTE_SCALE_FACTOR = 10;
    final int PALETTE_OFFSET = PALETTE_SCALE_FACTOR + 1; // 1px border between slots

    protected void drawPalette(MatrixStack matrixStack) {
        int paletteGlobalLeft = this.guiLeft + PALETTE_POSITION_X;
        int paletteGlobalTop = this.guiTop + PALETTE_POSITION_Y;

        for (int i = 0; i < PALETTE_SLOTS; i++) {
            int fromX = paletteGlobalLeft + (i % 2) * PALETTE_OFFSET;
            int fromY = paletteGlobalTop + (i / 2) * PALETTE_OFFSET;

            int color = this.getColor(this.palette, i);

            this.fillGradient(matrixStack, fromX, fromY, fromX + PALETTE_SCALE_FACTOR, fromY + PALETTE_SCALE_FACTOR, color, color);
        }
    }

    protected void drawPaletteSelector(MatrixStack matrixStack) {
        final int SELECTOR_POSITION_U = 82;
        final int SELECTOR_POSITION_V = 166;

        final int PALETTE_BORDER = 3;

        int paletteGlobalLeft = this.guiLeft + PALETTE_POSITION_X;
        int paletteGlobalTop = this.guiTop + PALETTE_POSITION_Y;

        int selectorPositionX = paletteGlobalLeft + (currentPaletteSlot % 2 != 0 ? PALETTE_OFFSET : 0) - PALETTE_BORDER;
        int selectorPositionY = paletteGlobalTop + (currentPaletteSlot / 2) * PALETTE_OFFSET - PALETTE_BORDER;

        this.blit(matrixStack, selectorPositionX, selectorPositionY, SELECTOR_POSITION_U, SELECTOR_POSITION_V, PALETTE_SCALE_FACTOR + PALETTE_BORDER * 2, PALETTE_SCALE_FACTOR + PALETTE_BORDER * 2);
    }

    protected void handlePaletteClick(final int mouseX, final int mouseY) {
        int paletteGlobalLeft = this.guiLeft + PALETTE_POSITION_X;
        int paletteGlobalTop = this.guiTop + PALETTE_POSITION_Y;

        int slotIndex = -1;

        // Quick check
        if (!isInRect(this.guiLeft + PALETTE_POSITION_X, this.guiTop + PALETTE_POSITION_Y, PALETTE_OFFSET * 2 - 1, PALETTE_OFFSET * 7 - 1, mouseX, mouseY)) {
            return;
        }

        ImmersiveMp.LOG.info("Palette click!");

        for (int i = 0; i < PALETTE_SLOTS; i++) {
            int slotX = paletteGlobalLeft + (i % 2) * PALETTE_OFFSET;
            int slotY = paletteGlobalTop + (i / 2) * PALETTE_OFFSET;

            if (isInRect(slotX, slotY, PALETTE_SCALE_FACTOR, PALETTE_SCALE_FACTOR, mouseX, mouseY)) {
                slotIndex = i;
                break;
            }
        }

        // Should only happen if clicked on border
        if (slotIndex == -1) {
            ImmersiveMp.LOG.warn("Cannot find palete slot!");
            return;
        }

        this.setCurrentPaletteSlot(slotIndex);
    }

    protected void setCurrentPaletteSlot(int slotIndex) {
        this.currentPaletteSlot = slotIndex;

        this.updateSlidersWithCurrentColor();
    }

    protected int getCurrentColor() {
        return this.palette.getInt(currentPaletteSlot * 4);
    }

    /**
     * Sliders
     */

    final int SLIDER_POSITION_X = 13;
    final int SLIDER_OFFSET_Y = 117;

    final int SLIDER_WIDTH = 150;
    final int SLIDER_HEIGHT = 9;

    final int SLIDER_DISTANCE = 5; // distance between sliders

    protected void drawSliders(MatrixStack matrixStack) {
        drawSliderBackground(matrixStack, 0, false);
        drawSliderBackground(matrixStack, 1, true);
        drawSliderBackground(matrixStack, 2, false);

        drawSliderForeground(matrixStack, 0, PaintingScreen::getHue, false);
        drawSliderForeground(matrixStack, 1, PaintingScreen::getSaturation, true);
        drawSliderForeground(matrixStack, 2, PaintingScreen::getValue, false);
    }

    protected void drawSliderBackground(MatrixStack matrixStack, int verticalOffset, boolean active) {
        final int SLIDER_POSITION_U = 5;
        final int SLIDER_POSITION_V = 198;

        int sliderGlobalLeft = this.guiLeft + SLIDER_POSITION_X;
        int sliderGlobalTop = this.guiTop + SLIDER_OFFSET_Y + (verticalOffset * (SLIDER_HEIGHT + SLIDER_DISTANCE));

        int sliderV = SLIDER_POSITION_V;

        if (active) {
            sliderV += SLIDER_HEIGHT;
        }

        this.blit(matrixStack, sliderGlobalLeft, sliderGlobalTop, SLIDER_POSITION_U, sliderV, SLIDER_WIDTH, SLIDER_HEIGHT);
    }

    protected void drawSliderForeground(MatrixStack matrixStack, int verticalOffset, BiFunction<float[], Float, Integer> getColorLambda, boolean active) {
        int sliderContentGlobalLeft = this.guiLeft + SLIDER_POSITION_X + 3;
        int sliderContentGlobalTop = this.guiTop + SLIDER_OFFSET_Y + (verticalOffset * (SLIDER_HEIGHT + SLIDER_DISTANCE)) + 3;

        int sliderContentWidth = SLIDER_WIDTH - 6;
        int sliderContentHeight = 3;

        if (active) {
            sliderContentGlobalTop -= 2;
            sliderContentHeight += 4;
        }

        Color currentColor = new Color(this.getCurrentColor());
        float[] currentColorHSB = Color.RGBtoHSB(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), null);

        for (int i = 0; i < sliderContentWidth; i++) {
            int color = getColorLambda.apply(currentColorHSB, (float) i / sliderContentWidth);

            this.fillGradient(matrixStack, sliderContentGlobalLeft + i, sliderContentGlobalTop, sliderContentGlobalLeft + i + 1, sliderContentGlobalTop + sliderContentHeight, color, color);
        }
    }

    protected static Integer getHue(float[] inColorHSB, float percent) {
        return Color.HSBtoRGB(percent, 1.0f, 1.0f);
    }

    protected static Integer getSaturation(float[] inColorHSB, float percent) {
        return Color.HSBtoRGB(inColorHSB[0], 1.0f - percent, inColorHSB[2]);
    }

    protected static Integer getValue(float[] inColorHSB, float percent) {
        return Color.HSBtoRGB(inColorHSB[0], inColorHSB[1], 1.0f - percent);
    }

    protected void updateSlidersWithCurrentColor() {
        Color currentColor = new Color(this.getCurrentColor());
        float[] currentColorHSB = Color.RGBtoHSB(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), null);

        this.sliderHuePercent = 1.0f - currentColorHSB[0];
        this.sliderSaturationPercent = 1.0f - currentColorHSB[1];
        this.sliderValuePercent = 1.0f - currentColorHSB[2];
    }

    protected void handleSliderClick(final int mouseX, final int mouseY) {
        // Quick check
        if (!isInRect(this.guiLeft + SLIDER_POSITION_X, this.guiTop + SLIDER_OFFSET_Y, SLIDER_WIDTH, SLIDER_HEIGHT + (SLIDER_DISTANCE + SLIDER_WIDTH) * 2, mouseX, mouseY)) {
            return;
        }

        ImmersiveMp.LOG.info("Slider click!");

        int sliderIndex = -1;

        for (int i = 0; i < 3; i++) {
            int sliderGlobalLeft = this.guiLeft + SLIDER_POSITION_X;
            int sliderGlobalTop = this.guiTop + SLIDER_OFFSET_Y + (i * (SLIDER_HEIGHT + SLIDER_DISTANCE));

            if (isInRect(sliderGlobalLeft, sliderGlobalTop, SLIDER_WIDTH, SLIDER_HEIGHT, mouseX, mouseY)) {
                sliderIndex = i;
                break;
            }
        }

        // Should only happen if clicked on border
        if (sliderIndex == -1) {
            ImmersiveMp.LOG.warn("Cannot find slider!");
            return;
        }

        float percent = (float) (mouseX - SLIDER_POSITION_X - 3) / (SLIDER_WIDTH - 6);

        this.updateSliderPosition(sliderIndex, percent);
    }

    protected void updateSliderPosition(int sliderIndex, float percent) {
        Color currentColor = new Color(this.getCurrentColor());
        float[] currentColorHSB = Color.RGBtoHSB(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), null);

        if (sliderIndex == 0) {
            int newColor = Color.HSBtoRGB(percent, currentColorHSB[1], currentColorHSB[2]);
            this.setColor(this.palette, this.currentPaletteSlot, newColor);
            this.sliderHuePercent = percent;
        } else if (sliderIndex == 1) {
            int newColor = Color.HSBtoRGB(currentColorHSB[0], percent, currentColorHSB[2]);
            this.setColor(this.palette, this.currentPaletteSlot, newColor);
            this.sliderSaturationPercent = percent;
        } else if (sliderIndex == 2) {
            int newColor = Color.HSBtoRGB(currentColorHSB[0], currentColorHSB[1], percent);
            this.setColor(this.palette, this.currentPaletteSlot, newColor);
            this.sliderValuePercent = percent;
        }
    }

    /**
     * Handlers
     */

    protected void drawHandlers(MatrixStack matrixStack) {
        drawHandler(matrixStack, 0, this.sliderHuePercent, false);
        drawHandler(matrixStack, 1, this.sliderSaturationPercent, true);
        drawHandler(matrixStack, 2, this.sliderValuePercent, false);
    }

    protected void drawHandler(MatrixStack matrixStack, int verticalOffset, float percent, boolean active) {
        final int HANDLER_POSITION_U = 0;
        final int HANDLER_POSITION_V = 198;
        final int HANDLER_WIDTH = 5;
        final int HANDLER_HEIGHT = 11;

        int sliderContentWidth = SLIDER_WIDTH - 6;

        int sliderGlobalLeft = this.guiLeft + SLIDER_POSITION_X + (int) (sliderContentWidth * percent) + 3 - 2;
        int sliderGlobalTop = this.guiTop + SLIDER_OFFSET_Y + (verticalOffset * (SLIDER_HEIGHT + SLIDER_DISTANCE)) - 1;

        int sliderV = HANDLER_POSITION_V;

        if (active) {
            sliderV += HANDLER_HEIGHT;
        }

        this.blit(matrixStack, sliderGlobalLeft, sliderGlobalTop, HANDLER_POSITION_U, sliderV, HANDLER_WIDTH, HANDLER_HEIGHT);
    }

    /**
     * Helpers
     */

    /**
     * @todo remove transparency, add to bytebuffer.
     * @param byteBuffer
     * @param offset
     * @return
     */
    protected int getColor(ByteBuffer byteBuffer, int offset) {
        return byteBuffer.getInt(offset * 4);
    }

    protected void setColor(ByteBuffer byteBuffer, int offset, int color) {
        byteBuffer.putInt(offset * 4, color);
    }

    // Returns true if the given x,y coordinates are within the given rectangle
    public static boolean isInRect(int x, int y, int xSize, int ySize, final int mouseX, final int mouseY){
        return ((mouseX >= x && mouseX <= x+xSize) && (mouseY >= y && mouseY <= y+ySize));
    }
}
