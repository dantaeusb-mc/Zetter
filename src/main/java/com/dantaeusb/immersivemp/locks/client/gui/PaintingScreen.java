package com.dantaeusb.immersivemp.locks.client.gui;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.inventory.container.EaselContainer;
import com.dantaeusb.immersivemp.locks.item.CanvasItem;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.function.BiFunction;

public class PaintingScreen extends ContainerScreen<EaselContainer> {

    protected final ITextComponent title = new TranslationTextComponent("container.immersivemp.lock_table");

    // This is the resource location for the background image
    private static final ResourceLocation PAINTING_RESOURCE = new ResourceLocation("immersivemp", "textures/paintings/gui/painting.png");

    private int currentPaletteSlot = 0;

    private float sliderHuePercent = 0.0f;
    private float sliderSaturationPercent = 0.0f;
    private float sliderValuePercent = 0.0f;

    private Integer sliderDraggingIndex;
    private boolean canvasDragging = false;
    private float sinceLastTick = .0f;

    private Tool currentTool = Tool.PENCIL;
    private Mode textAreaMode = Mode.COLOR;
    private TextFieldWidget textField;

    public PaintingScreen(EaselContainer paintingContainer, PlayerInventory playerInventory, ITextComponent title) {
        super(paintingContainer, playerInventory, title);

        this.xSize = 176;
        this.ySize = 185;
    }

    @Override
    protected void init() {
        super.init();
        this.initFields();
        this.updateSlidersWithCurrentColor();
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTextField(matrixStack, mouseX, mouseY, partialTicks);

        this.sinceLastTick += partialTicks;

        if (this.sinceLastTick > 1) {
            this.getContainer().tick();
            this.sinceLastTick = 0;
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.textField.tick();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int iMouseX = (int) mouseX;
        int iMouseY = (int) mouseY;

        this.handleToolButtonClick(iMouseX, iMouseY);
        this.handleCanvasInteraction(iMouseX, iMouseY);
        this.handlePaletteClick(iMouseX, iMouseY);
        this.handleSliderInteraction(iMouseX, iMouseY);
        this.handleTextboxClick(iMouseX, iMouseY);

        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.sliderDraggingIndex != null) {
            int iMouseX = (int) mouseX;
            int iMouseY = (int) mouseY;

            this.handleSliderInteraction(iMouseX, iMouseY, this.sliderDraggingIndex);
            return true;
        }

        if (this.canvasDragging) {
            int iMouseX = (int) mouseX;
            int iMouseY = (int) mouseY;

            this.handleCanvasInteraction(iMouseX, iMouseY);
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.sliderDraggingIndex != null) {
            // If we were changing palette colors, sync them with server
            this.container.sendPaletteUpdatePacket(this.currentPaletteSlot, this.getCurrentColor());
        }

        this.sliderDraggingIndex = null;
        this.canvasDragging = false;

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int x, int y) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bindTexture(PAINTING_RESOURCE);

        this.blit(matrixStack, this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);

        this.drawToolButtons(matrixStack);

        this.drawCanvas(matrixStack);

        this.drawPalette(matrixStack);
        this.drawPaletteSelector(matrixStack);

        this.drawSliders(matrixStack);
        this.drawHandlers(matrixStack);

        this.drawTextarea(matrixStack);
    }

    /**
     * @param matrixStack
     * @param mouseX
     * @param mouseY
     */
    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack matrixStack, int mouseX, int mouseY) {

    }

    /**
     * Cancel closing screen when pressing "E", handle input properly
     * @param keyCode
     * @param scanCode
     * @param modifiers
     * @return
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.minecraft.player.closeScreen();
        }

        if (this.textField.isFocused()) {
            return this.textField.keyPressed(keyCode, scanCode, modifiers) || this.textField.canWrite() || super.keyPressed(keyCode, scanCode, modifiers);
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * @todo: move every section to own class
     */

    /**
     * Canvas
     */
    final int CANVAS_POSITION_X = 48;
    final int CANVAS_POSITION_Y = 9;
    final int CANVAS_SCALE_FACTOR = 5;

    protected void drawCanvas(MatrixStack matrixStack) {
        if (!this.container.isCanvasAvailable()) {
            return;
        }

        int canvasGlobalLeft = this.guiLeft + CANVAS_POSITION_X;
        int canvasGlobalTop = this.guiTop + CANVAS_POSITION_Y;

        for (int i = 0; i < CanvasItem.CANVAS_SQUARE; i++) {
            int localX = i % 16;
            int localY = i / 16;

            int color = this.container.getCanvasData().getColorAt(i);
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
    protected void handleCanvasInteraction(final int mouseX, final int mouseY) {
        int canvasGlobalLeft = this.guiLeft + CANVAS_POSITION_X;
        int canvasGlobalTop = this.guiTop + CANVAS_POSITION_Y;

        if (!isInRect(canvasGlobalLeft, canvasGlobalTop, CANVAS_SCALE_FACTOR * 16, CANVAS_SCALE_FACTOR * 16, mouseX, mouseY)) {
            return;
        }

        this.canvasDragging = true;

        int canvasX = (mouseX - canvasGlobalLeft) / CANVAS_SCALE_FACTOR;
        int canvasY = (mouseY - canvasGlobalTop) / CANVAS_SCALE_FACTOR;

        switch (this.currentTool) {
            case PENCIL:
                this.getContainer().writePixelOnCanvasClientSide(canvasX, canvasY, this.getCurrentColor(), this.playerInventory.player.getUniqueID());
                break;
            case EYEDROPPER:
                this.getContainer().eyedropper(this.currentPaletteSlot, canvasX, canvasY);
        }
    }

    /**
     * Tools
     */

    final int TOOLS_POSITION_X = 7;
    final int TOOLS_POSITION_Y = 8;
    final int TOOLS_SIZE = 16;
    final int TOOLS_OFFSET = TOOLS_SIZE + 1; // 1px border between slots

    protected void drawToolButtons(MatrixStack matrixStack) {
        int toolsGlobalLeft = this.guiLeft + TOOLS_POSITION_X;
        int toolsGlobalTop = this.guiTop + TOOLS_POSITION_Y;

        int i = 0;
        for (Tool tool: Tool.values()) {
            int fromY = toolsGlobalTop + i * TOOLS_OFFSET;
            int vOffset = tool.vPosition + (this.currentTool == tool ? TOOLS_SIZE : 0);

            this.blit(matrixStack, toolsGlobalLeft, fromY, tool.uPosition, vOffset, TOOLS_SIZE, TOOLS_SIZE);
            i++;
        }
    }

    protected void handleToolButtonClick(final int mouseX, final int mouseY) {
        int toolsGlobalLeft = this.guiLeft + TOOLS_POSITION_X;
        int toolsGlobalTop = this.guiTop + TOOLS_POSITION_Y;

        // Quick check
        if (!isInRect(toolsGlobalLeft, toolsGlobalTop, TOOLS_SIZE, TOOLS_OFFSET * Tool.values().length - 1, mouseX, mouseY)) {
            return;
        }

        ImmersiveMp.LOG.info("Tool click!");

        int i = 0;
        for (Tool tool: Tool.values()) {
            int fromY = toolsGlobalTop + i * TOOLS_OFFSET;

            if (isInRect(toolsGlobalLeft, fromY, TOOLS_SIZE, TOOLS_SIZE, mouseX, mouseY)) {
                this.setCurrentTool(tool);
                return;
            }

            i++;
        }
    }

    protected void setCurrentTool(Tool tool) {
        this.currentTool = tool;

        this.updateSlidersWithCurrentColor();
    }

    /**
     * Palette
     */

    final int PALETTE_POSITION_X = 147;
    final int PALETTE_POSITION_Y = 9;
    final int PALETTE_SCALE_FACTOR = 10;
    final int PALETTE_OFFSET = PALETTE_SCALE_FACTOR + 1; // 1px border between slots

    protected void drawPalette(MatrixStack matrixStack) {
        if (!this.container.isPaletteAvailable()) {
            return;
        }

        int paletteGlobalLeft = this.guiLeft + PALETTE_POSITION_X;
        int paletteGlobalTop = this.guiTop + PALETTE_POSITION_Y;

        for (int i = 0; i < EaselContainer.PALETTE_SLOTS; i++) {
            int fromX = paletteGlobalLeft + (i % 2) * PALETTE_OFFSET;
            int fromY = paletteGlobalTop + (i / 2) * PALETTE_OFFSET;

            int color = this.container.getPaletteColor(i);

            this.fillGradient(matrixStack, fromX, fromY, fromX + PALETTE_SCALE_FACTOR, fromY + PALETTE_SCALE_FACTOR, color, color);
        }
    }

    protected void drawPaletteSelector(MatrixStack matrixStack) {
        if (!this.container.isPaletteAvailable()) {
            return;
        }

        final int SELECTOR_POSITION_U = 82;
        final int SELECTOR_POSITION_V = 185;

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
        if (!isInRect(paletteGlobalLeft, paletteGlobalTop, PALETTE_OFFSET * 2 - 1, PALETTE_OFFSET * 7 - 1, mouseX, mouseY)) {
            return;
        }

        ImmersiveMp.LOG.info("Palette click!");

        for (int i = 0; i < EaselContainer.PALETTE_SLOTS; i++) {
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
        return this.container.getPaletteColor(currentPaletteSlot);
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
        drawSliderBackground(matrixStack, 0, this.isDraggingSlider(0));
        drawSliderBackground(matrixStack, 1, this.isDraggingSlider(1));
        drawSliderBackground(matrixStack, 2, this.isDraggingSlider(2));

        drawSliderForeground(matrixStack, 0, PaintingScreen::getHue, this.isDraggingSlider(0));
        drawSliderForeground(matrixStack, 1, PaintingScreen::getSaturation, this.isDraggingSlider(1));
        drawSliderForeground(matrixStack, 2, PaintingScreen::getValue, this.isDraggingSlider(2));
    }

    protected void drawSliderBackground(MatrixStack matrixStack, int verticalOffset, boolean active) {
        final int SLIDER_POSITION_U = 5;
        final int SLIDER_POSITION_V = 217;

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

        this.sliderHuePercent = currentColorHSB[0];
        this.sliderSaturationPercent = 1.0f - currentColorHSB[1];
        this.sliderValuePercent = 1.0f - currentColorHSB[2];
    }

    protected void handleSliderInteraction(final int mouseX, final int mouseY) {
        this.handleSliderInteraction(mouseX, mouseY, null);
    }

    protected boolean isDraggingSlider(int index) {
        return this.sliderDraggingIndex != null && this.sliderDraggingIndex == index;
    }

    /**
     *
     * @param mouseX
     * @param mouseY
     * @param sliderIndex do not lookup if not null, just update slider based on mouse position - should be provided when dragging
     */
    protected void handleSliderInteraction(final int mouseX, final int mouseY, @Nullable Integer sliderIndex) {
        if (sliderIndex == null) {
            // Quick check
            if (!isInRect(this.guiLeft + SLIDER_POSITION_X, this.guiTop + SLIDER_OFFSET_Y, SLIDER_WIDTH, SLIDER_HEIGHT + (SLIDER_DISTANCE + SLIDER_HEIGHT) * 2, mouseX, mouseY)) {
                return;
            }

            ImmersiveMp.LOG.info("Slider click!");

            for (int i = 0; i < 3; i++) {
                int sliderGlobalLeft = this.guiLeft + SLIDER_POSITION_X;
                int sliderGlobalTop = this.guiTop + SLIDER_OFFSET_Y + (i * (SLIDER_HEIGHT + SLIDER_DISTANCE));

                if (isInRect(sliderGlobalLeft, sliderGlobalTop, SLIDER_WIDTH, SLIDER_HEIGHT, mouseX, mouseY)) {
                    sliderIndex = i;
                    break;
                }
            }

            // Should only happen if clicked on border
            if (sliderIndex == null) {
                ImmersiveMp.LOG.warn("Cannot find slider!");
                return;
            }
        }

        this.sliderDraggingIndex = sliderIndex;

        float percent = (float) (mouseX - this.guiLeft - SLIDER_POSITION_X - 3) / (SLIDER_WIDTH - 6);
        float percentC = MathHelper.clamp(percent, 0.0f, 1.0f);

        this.updateSliderPosition(sliderIndex, percentC);
    }

    protected void updateSliderPosition(int sliderIndex, float percent) {
        Color currentColor = new Color(this.getCurrentColor());
        float[] currentColorHSB = Color.RGBtoHSB(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), null);

        // @todo: ugly, refactor
        if (sliderIndex == 0) {
            int newColor = Color.HSBtoRGB(percent, currentColorHSB[1], currentColorHSB[2]);
            this.container.setPaletteColor(this.currentPaletteSlot, newColor);
            this.sliderHuePercent = percent;
        } else if (sliderIndex == 1) {
            // We use this.sliderHuePercent cause we can lose hue data on greyscale colors
            int newColor = Color.HSBtoRGB(this.sliderHuePercent, 1.0f - percent, currentColorHSB[2]);
            this.container.setPaletteColor(this.currentPaletteSlot, newColor);
            this.sliderSaturationPercent = percent;
        } else {
            int newColor = Color.HSBtoRGB(this.sliderHuePercent, currentColorHSB[1], 1.0f - percent);
            this.container.setPaletteColor(this.currentPaletteSlot, newColor);
            this.sliderValuePercent = percent;
        }
    }

    /**
     * Handlers
     */

    protected void drawHandlers(MatrixStack matrixStack) {
        drawHandler(matrixStack, 0, this.sliderHuePercent, this.isDraggingSlider(0));
        drawHandler(matrixStack, 1, this.sliderSaturationPercent, this.isDraggingSlider(1));
        drawHandler(matrixStack, 2, this.sliderValuePercent, this.isDraggingSlider(2));
    }

    protected void drawHandler(MatrixStack matrixStack, int verticalOffset, float percent, boolean active) {
        final int HANDLER_POSITION_U = 0;
        final int HANDLER_POSITION_V = 216;
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
     * Textbox
     */

    final int TEXTBOX_POSITION_X = 47;
    final int TEXTBOX_POSITION_Y = 95;
    final int TEXTBOX_WIDTH = 82;
    final int TEXTBOX_HEIGHT = 16;

    protected void initFields() {
        this.minecraft.keyboardListener.enableRepeatEvents(true);

        this.textField = new TextFieldWidget(this.font, this.guiLeft + TEXTBOX_POSITION_X + 4, this.guiTop + TEXTBOX_POSITION_Y + 4, TEXTBOX_WIDTH - 7, 12, new TranslationTextComponent("container.immersivemp.lock_table"));
        this.textField.setCanLoseFocus(false);
        this.textField.setTextColor(-1);
        this.textField.setDisabledTextColour(-1);
        this.textField.setEnableBackgroundDrawing(false);
        this.textField.setMaxStringLength(32);
        //this.textField.setResponder(this::renameItem);
        this.children.add(this.textField);
    }

    protected void drawTextarea(MatrixStack matrixStack) {
        drawTextbox(matrixStack);
        drawModeButtons(matrixStack);
    }

    protected void drawTextbox(MatrixStack matrixStack) {
        final int TEXTBOX_POSITION_U = 0;
        final int TEXTBOX_POSITION_V = 185;

        int textboxGlobalLeft = this.guiLeft + TEXTBOX_POSITION_X;
        int textboxGlobalTop = this.guiTop + TEXTBOX_POSITION_Y;

        int textboxV = TEXTBOX_POSITION_V + (this.textField.isFocused() ? TEXTBOX_HEIGHT : 0);

        this.blit(matrixStack, textboxGlobalLeft, textboxGlobalTop, TEXTBOX_POSITION_U, textboxV, TEXTBOX_WIDTH, TEXTBOX_HEIGHT);
    }

    protected void handleTextboxClick(final int mouseX, final int mouseY) {
        int textboxGlobalLeft = this.guiLeft + TEXTBOX_POSITION_X;
        int textboxGlobalTop = this.guiTop + TEXTBOX_POSITION_Y;

        // Quick check
        if (isInRect(textboxGlobalLeft, textboxGlobalTop, TEXTBOX_WIDTH, TEXTBOX_HEIGHT, mouseX, mouseY)) {
            ImmersiveMp.LOG.info("Textbox click!");
            this.textField.setFocused2(true);
        } else {
            this.textField.setFocused2(false);
        }
    }

    protected void drawModeButtons(MatrixStack matrixStack) {
        final int MODE_BUTTON_POSITION_U = 176;
        final int MODE_BUTTON_POSITION_V = 64;

        final int MODE_BUTTON_WIDTH = 17;
        final int MODE_BUTTON_HEIGHT = 16;

        int textboxGlobalLeft = this.guiLeft + TEXTBOX_POSITION_X;
        int textboxGlobalTop = this.guiTop + TEXTBOX_POSITION_Y;

        int modeNameV = MODE_BUTTON_POSITION_V + (this.textAreaMode == Mode.NAME ? MODE_BUTTON_WIDTH : 0);
        int modeColorV = MODE_BUTTON_POSITION_V + (this.textAreaMode == Mode.COLOR ? MODE_BUTTON_HEIGHT : 0);

        this.blit(matrixStack, textboxGlobalLeft - MODE_BUTTON_WIDTH - 1, textboxGlobalTop, MODE_BUTTON_POSITION_U, modeNameV, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT);
        this.blit(matrixStack, textboxGlobalLeft + TEXTBOX_WIDTH + 1, textboxGlobalTop, MODE_BUTTON_POSITION_U + MODE_BUTTON_WIDTH, modeColorV, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT);
    }

    public void renderTextField(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.textField.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    public enum Mode {
        NAME,
        COLOR
    }


    /**
     * Helpers
     */

    // Returns true if the given x,y coordinates are within the given rectangle
    public static boolean isInRect(int x, int y, int xSize, int ySize, final int mouseX, final int mouseY){
        return ((mouseX >= x && mouseX <= x+xSize) && (mouseY >= y && mouseY <= y+ySize));
    }

    public enum Tool implements IStringSerializable {
        PENCIL("Pencil", 176, 0),
        EYEDROPPER("Eyedropper", 192, 0),
        BUCKET("Bucket", 208, 0);

        private final String name;

        public final int uPosition;
        public final int vPosition;

        Tool(String name, int uPosition, int vPosition) {
            this.name = name;
            this.uPosition = uPosition;
            this.vPosition = vPosition;
        }

        public String toString() {
            return this.name;
        }

        public String getString() {
            return this.name;
        }
    }
}
