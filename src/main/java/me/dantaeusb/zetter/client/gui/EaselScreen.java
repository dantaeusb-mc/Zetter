package me.dantaeusb.zetter.client.gui;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.gui.easel.*;
import me.dantaeusb.zetter.client.gui.easel.tabs.*;
import me.dantaeusb.zetter.core.tools.Color;
import me.dantaeusb.zetter.menu.EaselMenu;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import me.dantaeusb.zetter.painting.Tools;
import me.dantaeusb.zetter.painting.parameters.AbstractToolParameters;
import me.dantaeusb.zetter.painting.parameters.BrushParameters;
import me.dantaeusb.zetter.painting.parameters.PencilParameters;
import me.dantaeusb.zetter.painting.parameters.SizeInterface;
import me.dantaeusb.zetter.painting.tools.*;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;

public class EaselScreen extends AbstractContainerScreen<EaselMenu> implements ContainerListener, ActionListener {
    // This is the resource location for the background image
    public static final ResourceLocation PAINTING_RESOURCE = new ResourceLocation(Zetter.MOD_ID, "textures/gui/easel.png");

    private final List<AbstractPaintingWidget> paintingWidgets = Lists.newArrayList();

    private HashMap<TabsWidget.Tab, AbstractTab> tabs;

    private ToolsWidget toolsWidget;
    private HistoryWidget historyWidget;
    private ZoomWidget zoomWidget;
    private TabsWidget tabsWidget;
    private CanvasWidget canvasWidget;
    private PaletteWidget paletteWidget;
    private HelpWidget helpWidget;

    private final Player player;

    public EaselScreen(EaselMenu paintingContainer, Inventory playerInventory, Component title) {
        super(paintingContainer, playerInventory, title);

        this.player = playerInventory.player;

        this.imageWidth = 206;
        this.imageHeight = 238;
    }

    @Override
    protected void init() {
        super.init();

        final int CANVAS_POSITION_X = 39;
        final int CANVAS_POSITION_Y = 9;

        final int TOOLS_POSITION_X = 4;
        final int TOOLS_POSITION_Y = 4;

        final int HISTORY_POSITION_X = 4;
        final int HISTORY_POSITION_Y = 112;

        final int ZOOM_POSITION_X = 4;
        final int ZOOM_POSITION_Y = 142;

        final int TABS_POSITION_X = 4;
        final int TABS_POSITION_Y = 158;

        final int PALETTE_POSITION_X = 175;
        final int PALETTE_POSITION_Y = 38;

        final int HELP_POSITION_X = 197;
        final int HELP_POSITION_Y = 0;

        // @todo: [LOW] Use rebuildWidgets in Screen
        this.paintingWidgets.clear();

        // Widgets

        this.canvasWidget = new CanvasWidget(this, this.getGuiLeft() + CANVAS_POSITION_X, this.getGuiTop() + CANVAS_POSITION_Y);
        this.paletteWidget = new PaletteWidget(this, this.getGuiLeft() + PALETTE_POSITION_X, this.getGuiTop() + PALETTE_POSITION_Y);
        this.toolsWidget = new ToolsWidget(this, this.getGuiLeft() + TOOLS_POSITION_X, this.getGuiTop() + TOOLS_POSITION_Y);
        this.historyWidget = new HistoryWidget(this, this.getGuiLeft() + HISTORY_POSITION_X, this.getGuiTop() + HISTORY_POSITION_Y);
        this.zoomWidget = new ZoomWidget(this, this.getGuiLeft() + ZOOM_POSITION_X, this.getGuiTop() + ZOOM_POSITION_Y);
        this.tabsWidget = new TabsWidget(this, this.getGuiLeft() + TABS_POSITION_X, this.getGuiTop() + TABS_POSITION_Y);
        this.helpWidget = new HelpWidget(this, this.getGuiLeft() + HELP_POSITION_X, this.getGuiTop() + HELP_POSITION_Y);

        this.addPaintingWidget(this.canvasWidget);
        this.addPaintingWidget(this.paletteWidget);
        this.addPaintingWidget(this.toolsWidget);
        this.addPaintingWidget(this.historyWidget);
        this.addPaintingWidget(this.zoomWidget);
        this.addPaintingWidget(this.tabsWidget);
        this.addPaintingWidget(this.helpWidget);

        // Tabs

        this.tabs = new HashMap<>();

        final ColorTab colorTab = new ColorTab(this, this.getGuiLeft(), this.getGuiTop());

        final PencilParametersTab pencilParametersTab = new PencilParametersTab(this, this.getGuiLeft(), this.getGuiTop());
        final BrushParametersTab brushParametersTab = new BrushParametersTab(this, this.getGuiLeft(), this.getGuiTop());
        final BucketParametersTab bucketParametersTab = new BucketParametersTab(this, this.getGuiLeft(), this.getGuiTop());

        final InventoryTab inventoryTab = new InventoryTab(this, this.getGuiLeft(), this.getGuiTop());

        this.tabs.put(TabsWidget.Tab.COLOR, colorTab);
        this.tabs.put(TabsWidget.Tab.PENCIL_PARAMETERS, pencilParametersTab);
        this.tabs.put(TabsWidget.Tab.BRUSH_PARAMETERS, brushParametersTab);
        this.tabs.put(TabsWidget.Tab.BUCKET_PARAMETERS, bucketParametersTab);
        this.tabs.put(TabsWidget.Tab.INVENTORY, inventoryTab);

        // Other

        assert this.minecraft != null;
        this.minecraft.keyboardHandler.setSendRepeatsToGui(true);

        this.menu.addToolUpdateListener(this::updateCurrentTool);
        this.menu.addColorUpdateListener(this::updateCurrentColor);
        this.menu.addSlotListener(this);

        Tools.EYEDROPPER.getTool().addActionListener(this);
        Tools.HAND.getTool().addActionListener(this);

        //this.updateCurrentColor(this.getMenu().getCurrentColor());
    }

    public void addPaintingWidget(AbstractPaintingWidget widget) {
        this.paintingWidgets.add(widget);
        this.addWidget(widget);
    }

    /**
     * We do not do that directly because widget do not control
     * life cycle. We need to remove reference when screen re-created.
     * @param parameters
     */
    public void updateCurrentTool(AbstractToolParameters parameters) {
        this.tabsWidget.updateTabs();
        this.getMenu().setCurrentTab(TabsWidget.Tab.COLOR);

        switch (this.getMenu().getCurrentTool()) {
            case PENCIL:
                ((PencilParametersTab) this.tabs.get(TabsWidget.Tab.PENCIL_PARAMETERS)).update(parameters);
                break;
            case BRUSH:
                ((BrushParametersTab) this.tabs.get(TabsWidget.Tab.BRUSH_PARAMETERS)).update(parameters);
                break;
            case BUCKET:
                ((BucketParametersTab) this.tabs.get(TabsWidget.Tab.BUCKET_PARAMETERS)).update(parameters);
                break;
        }
    }

    /**
     * We do not do that directly because widget do not control
     * life cycle. We need to remove reference when screen re-created.
     * @param color
     */
    public void updateCurrentColor(Integer color) {
        ((ColorTab) this.tabs.get(TabsWidget.Tab.COLOR)).update(color);
    }

    /**
     * Make add widget "public" so painting widgets can pipe their components to this screen
     */
    public <T extends GuiEventListener & NarratableEntry> void pipeWidget(T widget) {
        this.addWidget(widget);
    }

    /**
     * Called only when container is closed
     *
     * Does not get called when Screen is re-rendered
     * i.e. due to resize
     */
    public void removed() {
        super.removed();
        this.menu.removeToolUpdateListener(this::updateCurrentTool);
        this.menu.removeColorUpdateListener(this::updateCurrentColor);
        this.menu.removeSlotListener(this);

        Tools.EYEDROPPER.getTool().removeActionListener(this);
        Tools.HAND.getTool().removeActionListener(this);
    }

    /**
     * Expose some methods for widgets
     */

    public Font getFont() {
        return this.font;
    }

    /**
     * Get currently selected tab
     * @return
     */
    public AbstractTab getCurrentTab() {
        return this.tabs.get(this.getMenu().getCurrentTab());
    }

    public int getColorAt(int pixelIndex) {
        return this.menu.getCanvasData().getColorAt(pixelIndex);
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
    }

    @Override
    protected void renderBg(PoseStack matrixStack, float partialTicks, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, PAINTING_RESOURCE);

        this.blit(matrixStack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        this.canvasWidget.render(matrixStack, x, y, partialTicks);

        RenderSystem.setShaderTexture(0, PAINTING_RESOURCE);

        this.toolsWidget.render(matrixStack);
        this.tabsWidget.render(matrixStack);
        this.historyWidget.render(matrixStack);
        this.zoomWidget.render(matrixStack);
        this.paletteWidget.render(matrixStack);
        this.helpWidget.render(matrixStack, x, y, partialTicks);

        this.getCurrentTab().render(matrixStack, x, y, partialTicks);
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
        //final int LABEL_XPOS = 5;
        //final int LABEL_YPOS = 5;
        //this.font.draw(matrixStack, this.title, LABEL_XPOS, LABEL_YPOS, Color.darkGray.getRGB());

        final int FONT_Y_SPACING = 12;
        final int TAB_LABEL_XPOS = EaselMenu.PLAYER_INVENTORY_XPOS - 1;
        final int TAB_LABEL_YPOS = EaselMenu.PLAYER_INVENTORY_YPOS - FONT_Y_SPACING;

        // draw the label for the player inventory slots
        this.font.draw(matrixStack, this.getMenu().getCurrentTab().translatableComponent,
                TAB_LABEL_XPOS, TAB_LABEL_YPOS, Color.darkGray.getRGB());

        this.getCurrentTab().renderLabels(matrixStack, mouseX, mouseY);
    }

    @Override
    public void containerTick() {
        super.containerTick();

        this.canvasWidget.tick();
    }

    private double[] dragStart;
    private double[] dragCurrent;
    private int[] dragStartCanvasOffset;

    /**
     * For handling client-only interactions (non publishable)
     * @param canvas
     * @param tool
     * @param parameters
     * @param color
     * @param posX
     * @param posY
     */
    public void useToolCallback(CanvasData canvas, AbstractTool<?> tool, AbstractToolParameters parameters, int color, float posX, float posY) {
        if (tool.equals(Tools.EYEDROPPER.getTool())) {
            int canvasPosX = (int) Math.min(Math.max(posX, 0), canvas.getWidth());
            int canvasPosY = (int) Math.min(Math.max(posY, 0), canvas.getHeight());

            final int newColor = canvas.getColorAt(canvasPosX, canvasPosY);
            this.getMenu().setPaletteColor(newColor);
        } else if (tool.equals(Tools.HAND.getTool())) {
            if (this.dragStart == null || this.dragCurrent == null || this.dragStartCanvasOffset == null) {
                return;
            }

            double dragX = this.dragStart[0] - this.dragCurrent[0];
            double dragY = this.dragStart[1] - this.dragCurrent[1];

            int offsetX = (int) Math.round(dragX);
            int offsetY = (int) Math.round(dragY);

            this.getMenu().updateCanvasOffset(
                this.dragStartCanvasOffset[0] - offsetX,
                this.dragStartCanvasOffset[1] - offsetY
            );
        }
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
        if (this.getCurrentTab().keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE:
                assert this.minecraft != null;
                assert this.minecraft.player != null;
                this.minecraft.player.closeContainer();
                return true;
            case Pencil.HOTKEY:
                this.getMenu().setCurrentTool(Tools.PENCIL);
                return true;
            case Brush.HOTKEY:
                this.getMenu().setCurrentTool(Tools.BRUSH);
                return true;
            case Eyedropper.HOTKEY:
                this.getMenu().setCurrentTool(Tools.EYEDROPPER);
                return true;
            case Eyedropper.QUICK_TOOL_KEY:
                this.activateQuickTool(Tools.EYEDROPPER);
                return true;
            case Bucket.HOTKEY:
                this.getMenu().setCurrentTool(Tools.BUCKET);
                return true;
            case Hand.HOTKEY:
                this.getMenu().setCurrentTool(Tools.HAND);
                return true;
            case Hand.QUICK_TOOL_KEY:
                this.activateQuickTool(Tools.HAND);
                return true;
            case PaletteWidget.SWAP_HOTKEY: {
                final int row = (this.getMenu().getCurrentPaletteSlot() / 2) * 2;
                final int offset = this.getMenu().getCurrentPaletteSlot() % 2 == 0 ? 1 : 0;
                this.getMenu().setCurrentPaletteSlot(row + offset);
                return true;
            }
            case GLFW.GLFW_KEY_UP: {
                final int row = this.getMenu().getCurrentPaletteSlot() / 2;
                final int offset = this.getMenu().getCurrentPaletteSlot() % 2;

                if (row <= 0) {
                    return false;
                }

                this.getMenu().setCurrentPaletteSlot((row - 1) * 2 + offset);

                return true;
            }
            case ZoomWidget.ZOOM_OUT_HOTKEY:
                this.getMenu().decreaseCanvasScale();
                return true;
            case ZoomWidget.ZOOM_IN_HOTKEY:
                this.getMenu().increaseCanvasScale();
                return true;
            case GLFW.GLFW_KEY_DOWN: {
                final int row = this.getMenu().getCurrentPaletteSlot() / 2;
                final int offset = this.getMenu().getCurrentPaletteSlot() % 2;

                if (row >= 7) {
                    return false;
                }

                this.getMenu().setCurrentPaletteSlot((row + 1) * 2 + offset);

                return true;
            }
            case HistoryWidget.UNDO_HOTKEY:
                if (Screen.hasControlDown()) {
                    this.getMenu().undo();
                    return true;
                }
            case HistoryWidget.REDO_HOTKEY:
                if (Screen.hasControlDown()) {
                    this.getMenu().redo();
                    return true;
                }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Handle quick tool key release
     * @param keyCode
     * @param scanCode
     * @param modifiers
     * @return
     */
    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (this.getCurrentTab().keyReleased(keyCode, scanCode, modifiers)) {
            return true;
        }

        switch (keyCode) {
            case Hand.QUICK_TOOL_KEY:
            case Eyedropper.QUICK_TOOL_KEY:
                this.deactivateQuickTool();
                return true;
        }

        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        this.getCurrentTab().charTyped(codePoint, modifiers);

        return super.charTyped(codePoint, modifiers);
    }

    /**
     * We have a little complicated logic with active tabs here
     * @param mouseX
     * @param mouseY
     * @param button
     * @return
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.dragStart == null) {
            this.dragStart = new double[]{mouseX, mouseY};
            this.dragStartCanvasOffset = new int[]{this.getMenu().getCanvasOffsetX(), this.getMenu().getCanvasOffsetY()};
        }

        super.mouseClicked(mouseX, mouseY, button);

        return this.getCurrentTab().mouseClicked(mouseX, mouseY, button);
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
        this.dragCurrent = new double[]{mouseX, mouseY};

        this.canvasWidget.mouseDragged(mouseX, mouseY, button, dragX, dragY);

        this.getCurrentTab().mouseDragged(mouseX, mouseY, button, dragX, dragY);

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

        this.getCurrentTab().mouseReleased(mouseX, mouseY, button);

        // Reset dragging
        if (this.dragStart != null || this.dragCurrent != null || this.dragStartCanvasOffset != null) {
            this.dragStart = null;
            this.dragStartCanvasOffset = null;
            this.dragCurrent = null;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (hasControlDown()) {
            if (this.getMenu().getCurrentTool() == Tools.BRUSH) {
                AbstractToolParameters parameters = this.getMenu().getCurrentToolParameters();

                if (parameters instanceof SizeInterface) {
                    float newSize = ((SizeInterface) parameters).getSize() + (float) delta;
                    newSize = Math.min(Math.max(newSize, BrushParameters.MIN_SIZE), BrushParameters.MAX_SIZE);

                    ((SizeInterface) parameters).setSize(newSize);
                    return true;
                }
            } else if (this.getMenu().getCurrentTool() == Tools.PENCIL) {
                AbstractToolParameters parameters = this.getMenu().getCurrentToolParameters();

                if (parameters instanceof SizeInterface) {
                    float newSize = ((SizeInterface) parameters).getSize() + (delta > 0 ? 1 : -1);
                    newSize = Math.min(Math.max(newSize, PencilParameters.MIN_SIZE), PencilParameters.MAX_SIZE);

                    ((SizeInterface) parameters).setSize(newSize);
                    return true;
                }
            }
        }

        if (this.canvasWidget.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }

        return false;
    }

    public Player getPlayer() {
        return this.player;
    }

    @Override
    public void dataChanged(AbstractContainerMenu pContainerMenu, int pDataSlotIndex, int pValue) {

    }

    /**
     * Sends the contents of an inventory slot to the client-side Container. This doesn't have to match the actual
     * contents of that slot.
     */
    public void slotChanged(AbstractContainerMenu containerToSend, int slotInd, ItemStack stack) {
        this.updateCurrentColor(this.getMenu().getCurrentColor());
    }

    /*
     * Quick tool feature: for accessing tools like color
     * picker with alt hotkey or hand with space
     */

    private @Nullable Tools quickToolCache;

    public void activateQuickTool(Tools tool) {
        if (this.getMenu().getCurrentTool() != tool) {
            this.quickToolCache = this.getMenu().getCurrentTool();
            this.getMenu().setCurrentTool(tool);
        }
    }

    public void deactivateQuickTool() {
        if (this.quickToolCache != null) {
            this.getMenu().setCurrentTool(this.quickToolCache);
            this.quickToolCache = null;
        }
    }

    /*
     * Helpers
     */

    /**
     * @todo: [LOW] Use this.isPointInRegion
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
