package me.dantaeusb.zetter.client.gui;

import me.dantaeusb.zetter.client.gui.painting.*;
import me.dantaeusb.zetter.menu.EaselContainerMenu;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

import java.util.List;

public class PaintingScreen extends AbstractContainerScreen<EaselContainerMenu> implements ContainerListener {
    // This is the resource location for the background image
    public static final ResourceLocation PAINTING_RESOURCE = new ResourceLocation("zetter", "textures/gui/painting.png");

    protected final List<AbstractPaintingWidget> paintingWidgets = Lists.newArrayList();

    private ToolsWidget toolsWidget;
    private CanvasWidget canvasWidget;
    private PaletteWidget paletteWidget;
    private ColorCodeWidget colorCodeWidget;
    private SlidersWidget slidersWidget;
    private HelpWidget helpWidget;

    private final Player player;

    public PaintingScreen(EaselContainerMenu paintingContainer, Inventory playerInventory, Component title) {
        super(paintingContainer, playerInventory, title);

        this.player = playerInventory.player;

        this.imageWidth = 176;
        this.imageHeight = 185;
    }

    @Override
    protected void init() {
        super.init();

        final int CANVAS_POSITION_X = 48;
        final int CANVAS_POSITION_Y = 9;

        final int TOOLS_POSITION_X = 7;
        final int TOOLS_POSITION_Y = 8;

        final int PALETTE_POSITION_X = 147;
        final int PALETTE_POSITION_Y = 13;

        final int SLIDER_POSITION_X = 13;
        final int SLIDER_OFFSET_Y = 117;

        final int TEXTBOX_POSITION_X = 47;
        final int TEXTBOX_POSITION_Y = 95;

        final int HELP_POSITION_X = 165;
        final int HELP_POSITION_Y = 0;

        this.canvasWidget = new CanvasWidget(this, this.getGuiLeft() + CANVAS_POSITION_X, this.getGuiTop() + CANVAS_POSITION_Y);
        this.paletteWidget = new PaletteWidget(this, this.getGuiLeft() + PALETTE_POSITION_X, this.getGuiTop() + PALETTE_POSITION_Y);
        this.toolsWidget = new ToolsWidget(this, this.getGuiLeft() + TOOLS_POSITION_X, this.getGuiTop() + TOOLS_POSITION_Y);
        this.slidersWidget = new SlidersWidget(this, this.getGuiLeft() + SLIDER_POSITION_X, this.getGuiTop() + SLIDER_OFFSET_Y);
        this.colorCodeWidget = new ColorCodeWidget(this, this.getGuiLeft() + TEXTBOX_POSITION_X, this.getGuiTop() + TEXTBOX_POSITION_Y);
        this.helpWidget = new HelpWidget(this, this.getGuiLeft() + HELP_POSITION_X, this.getGuiTop() + HELP_POSITION_Y);

        this.colorCodeWidget.initFields();

        this.addPaintingWidget(this.canvasWidget);
        this.addPaintingWidget(this.paletteWidget);
        this.addPaintingWidget(this.toolsWidget);
        this.addPaintingWidget(this.slidersWidget);
        this.addPaintingWidget(this.colorCodeWidget);
        this.addPaintingWidget(this.helpWidget);

        this.getMenu().setFirstLoadNotification(this::firstLoadUpdate);

        this.minecraft.keyboardHandler.setSendRepeatsToGui(true);

        this.menu.addSlotListener(this);
    }

    public void addPaintingWidget(AbstractPaintingWidget widget) {
        this.paintingWidgets.add(widget);
        this.addWidget(widget);
    }

    /**
     * Make add widget "public" so painting widgets can pipe their components to this screen
     */
    public <T extends GuiEventListener & NarratableEntry> void pipeWidget(T widget) {
        this.addWidget(widget);
    }

    public void removed() {
        super.removed();
    }

    /**
     * Expose some methods for widgets
     */

    public Font getFont() {
        return this.font;
    }

    public boolean isCanvasAvailable() {
        return this.menu.isCanvasAvailable();
    }

    public boolean isPaletteAvailable() {
        return this.menu.isPaletteAvailable();
    }

    public int getColorAt(int pixelIndex) {
        return this.menu.getCanvasData().getColorAt(pixelIndex);
    }

    public int getPaletteColor(int slot) {
        return this.getMenu().getPaletteColor(slot);
    }

    public void firstLoadUpdate() {
        this.updateSlidersWithCurrentColor();
    }

    // @todo: not a screen thing
    public void updateCurrentPaletteColor(int color) {
        this.getMenu().setPaletteColor(color);
        this.slidersWidget.updateSlidersWithCurrentColor();
    }

    public void updateSlidersWithCurrentColor() {
        this.slidersWidget.updateSlidersWithCurrentColor();
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
    }

    @Override
    public void containerTick() {
        super.containerTick();

        this.getMenu().tick();
        this.colorCodeWidget.tick();
    }

    @Override
    protected void renderBg(PoseStack matrixStack, float partialTicks, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, PAINTING_RESOURCE);

        this.blit(matrixStack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        this.toolsWidget.render(matrixStack);
        this.canvasWidget.render(matrixStack, x, y, partialTicks);
        this.paletteWidget.render(matrixStack);
        this.slidersWidget.render(matrixStack);
        this.helpWidget.render(matrixStack, x, y, partialTicks);
        // @todo: If color code goes not last, it may stop others from drawing. Is there an exception maybe?
        this.colorCodeWidget.render(matrixStack, x, y, partialTicks);
    }

    @Override
    protected void renderTooltip(PoseStack matrixStack, int x, int y) {

        super.renderTooltip(matrixStack, x, y);

        for (AbstractPaintingWidget widget : this.paintingWidgets) {
            if (widget.isMouseOver(x, y)) {
                Component tooltip = widget.getTooltip(x, y);

                if (tooltip != null) {
                    this.renderTooltip(matrixStack, tooltip, x, y);
                }
            }
        }
    }

    /**
     * @param matrixStack
     * @param mouseX
     * @param mouseY
     */
    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        // Do not draw titles to save some extra space
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
            this.minecraft.player.closeContainer();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Unfortunately this event is not passed to children
     * @param mouseX
     * @param mouseY
     * @param button
     * @param dragX
     * @param dragY
     * @return
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        this.canvasWidget.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        this.slidersWidget.mouseDragged(mouseX, mouseY, button, dragX, dragY);

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /**
     * This one not passed when out of widget bounds but we need to track this event to release slider/pencil
     * @param mouseX
     * @param mouseY
     * @param button
     * @return
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.canvasWidget.mouseReleased(mouseX, mouseY, button);
        this.slidersWidget.mouseReleased(mouseX, mouseY, button);

        return super.mouseReleased(mouseX, mouseY, button);
    }

    public Player getPlayer() {
        return this.player;
    }

    @Override
    public void dataChanged(AbstractContainerMenu p_150524_, int p_150525_, int p_150526_) {

    }

    /**
     * Sends the contents of an inventory slot to the client-side Container. This doesn't have to match the actual
     * contents of that slot.
     */
    public void slotChanged(AbstractContainerMenu containerToSend, int slotInd, ItemStack stack) {

    }

    /**
     * Helpers
     */

    /**
     * @todo: use this.isPointInRegion
     *
     * @param x
     * @param y
     * @param xSize
     * @param ySize
     * @param mouseX
     * @param mouseY
     * @return
     */
    // Returns true if the given x,y coordinates are within the given rectangle
    public static boolean isInRect(int x, int y, int xSize, int ySize, final int mouseX, final int mouseY){
        return ((mouseX >= x && mouseX <= x+xSize) && (mouseY >= y && mouseY <= y+ySize));
    }
}
