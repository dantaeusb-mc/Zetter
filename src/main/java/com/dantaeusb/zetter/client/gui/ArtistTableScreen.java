package com.dantaeusb.zetter.client.gui;

import com.dantaeusb.zetter.client.gui.artisttable.AbstractArtistTableWidget;
import com.dantaeusb.zetter.client.gui.artisttable.CombinedCanvasWidget;
import com.dantaeusb.zetter.client.gui.artisttable.HelpWidget;
import com.dantaeusb.zetter.menu.ArtistTableMenu;
import com.dantaeusb.zetter.core.ModNetwork;
import com.dantaeusb.zetter.network.packet.CUpdatePaintingPacket;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

import java.awt.*;
import java.util.List;

public class ArtistTableScreen extends AbstractContainerScreen<ArtistTableMenu> {
    protected final Component title = new TranslatableComponent("container.zetter.artistTable");
    private EditBox nameField;

    // This is the resource location for the background image
    private static final ResourceLocation ARTIST_TABLE_RESOURCE = new ResourceLocation("zetter", "textures/gui/artist_table.png");

    protected final List<AbstractArtistTableWidget> artistTableWidgets = Lists.newArrayList();

    private CombinedCanvasWidget combinedCanvasWidget;
    private HelpWidget helpWidget;

    private int tick = 0;

    public ArtistTableScreen(ArtistTableMenu artistTableMenu, Inventory playerInventory, Component title) {
        super(artistTableMenu, playerInventory, title);

        this.imageWidth = 176;
        this.imageHeight = 220;
    }

    final int INPUT_XPOS = 7;
    final int INPUT_YPOS = 107;
    final int INPUT_WIDTH = 95;
    final int INPUT_HEIGHT = 12;

    @Override
    protected void init() {
        super.init();

        final int COMBINED_CANVAS_POSITION_X = 104;
        final int COMBINED_CANVAS_POSITION_Y = 26;

        final int HELP_POSITION_X = 165;
        final int HELP_POSITION_Y = 0;

        this.combinedCanvasWidget = new CombinedCanvasWidget(this, this.getGuiLeft() + COMBINED_CANVAS_POSITION_X, this.getGuiTop() + COMBINED_CANVAS_POSITION_Y);
        this.helpWidget = new HelpWidget(this, this.getGuiLeft() + HELP_POSITION_X, this.getGuiTop() + HELP_POSITION_Y);

        this.addWidget(this.combinedCanvasWidget);
        this.addWidget(this.helpWidget);

        this.initFields();
    }

    public void addPaintingWidget(AbstractArtistTableWidget widget) {
        this.artistTableWidgets.add(widget);
        this.addWidget(widget);
    }

    protected void initFields() {
        this.minecraft.keyboardHandler.setSendRepeatsToGui(true);

        this.nameField = new EditBox(this.font, this.leftPos + this.INPUT_XPOS + 4, this.topPos + INPUT_YPOS + 4, INPUT_WIDTH, INPUT_HEIGHT, new TranslatableComponent("container.zetter.artist_table"));
        this.nameField.setCanLoseFocus(false);
        this.nameField.setTextColor(-1);
        this.nameField.setTextColorUneditable(-1);
        this.nameField.setBordered(false);
        this.nameField.setMaxLength(35);
        this.nameField.setResponder(this::renameItem);
        this.addWidget(this.nameField);
        this.setInitialFocus(this.nameField);
    }

    // Listener interface - to track name field availability

    private void renameItem(String name) {
        this.menu.updatePaintingName(name);

        if (this.getMenu().isCanvasReady()) {
            this.sendRenamePacket(name);
        }
    }

    private void sendRenamePacket(String name) {
        CUpdatePaintingPacket modePacket = new CUpdatePaintingPacket(
                this.menu.containerId,
                this.nameField.getValue(),
                this.menu.getCanvasCombination().canvasData
        );

        ModNetwork.simpleChannel.sendToServer(modePacket);
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);

        super.render(matrixStack, mouseX, mouseY, partialTicks);

        this.renderNameField(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
    }

    public void renderNameField(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.nameField.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public void containerTick() {
        super.containerTick();

        this.tick++;
        this.nameField.tick();
    }

    @Override
    protected void renderBg(PoseStack matrixStack, float partialTicks, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, ARTIST_TABLE_RESOURCE);

        this.blit(matrixStack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Draw input
        final int INPUT_UPOS = 0;
        final int INPUT_VPOS = this.imageHeight;
        final int INPUT_WIDTH = 100;
        final int INPUT_HEIGHT = 16;

        this.blit(matrixStack, this.leftPos + INPUT_XPOS, this.topPos + INPUT_YPOS, INPUT_UPOS, INPUT_VPOS + (this.allowedToNameItem() && this.nameField.isFocused() ? 0 : INPUT_HEIGHT), INPUT_WIDTH, INPUT_HEIGHT);

        final int LOADING_XPOS = 128;
        final int LOADING_YPOS = 54;
        final int LOADING_UPOS = 100;
        final int LOADING_VPOS = this.imageHeight;
        final int LOADING_WIDTH = 16;
        final int LOADING_HEIGHT = 10;

        if (this.getMenu().canvasLoading()) {
            final int animation = this.tick % 40;
            int frame = animation / 10; // 0-3

            frame = frame > 2 ? 1 : frame; // 3rd frame is the same as 1st frame

            this.blit(matrixStack, this.leftPos + LOADING_XPOS, this.topPos + LOADING_YPOS, LOADING_UPOS, LOADING_VPOS + LOADING_HEIGHT * frame, LOADING_WIDTH, LOADING_HEIGHT);
        }

        this.helpWidget.render(matrixStack, x, y, partialTicks);

        if (this.getMenu().isCanvasReady()) {
            this.combinedCanvasWidget.render(matrixStack);
        }
    }

    @Override
    protected void renderTooltip(PoseStack matrixStack, int x, int y) {
        super.renderTooltip(matrixStack, x, y);

        for (AbstractArtistTableWidget widget : this.artistTableWidgets) {
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
        final int LABEL_XPOS = 5;
        final int LABEL_YPOS = 5;
        this.font.draw(matrixStack, this.title, LABEL_XPOS, LABEL_YPOS, Color.darkGray.getRGB());

        final int FONT_Y_SPACING = 10;
        final int PLAYER_INV_LABEL_XPOS = ArtistTableMenu.PLAYER_INVENTORY_XPOS;
        final int PLAYER_INV_LABEL_YPOS = ArtistTableMenu.PLAYER_INVENTORY_YPOS - FONT_Y_SPACING;

        // draw the label for the player inventory slots
        this.font.draw(matrixStack, this.playerInventoryTitle,
                PLAYER_INV_LABEL_XPOS, PLAYER_INV_LABEL_YPOS, Color.darkGray.getRGB());
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

        return this.nameField.keyPressed(keyCode, scanCode, modifiers) || this.nameField.canConsumeInput() || super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Copy of container method, needed cause conainer for client is just a dummy instance
     * @see LockTableContainer#allowedToNameItem()
     * @return
     */
    public boolean allowedToNameItem() {
        return this.menu.isCanvasReady();
    }

    // Returns true if the given x,y coordinates are within the given rectangle
    public static boolean isInRect(int x, int y, int xSize, int ySize, int mouseX, int mouseY){
        return ((mouseX >= x && mouseX <= x+xSize) && (mouseY >= y && mouseY <= y+ySize));
    }
}
