package com.dantaeusb.zetter.client.gui;

import com.dantaeusb.zetter.client.gui.artisttable.CombinedCanvasWidget;
import com.dantaeusb.zetter.container.ArtistTableContainer;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.awt.*;

public class ArtistTableScreen extends ContainerScreen<ArtistTableContainer>/* implements IContainerListener*/ {
    protected final ITextComponent title = new TranslationTextComponent("container.zetter.artistTable");
    private TextFieldWidget nameField;

    // This is the resource location for the background image
    private static final ResourceLocation ARTIST_TABLE_RESOURCE = new ResourceLocation("zetter", "textures/paintings/gui/artist_table.png");

    private CombinedCanvasWidget combinedCanvasWidget;

    private int tick = 0;

    public ArtistTableScreen(ArtistTableContainer artistTableContainer, PlayerInventory playerInventory, ITextComponent title) {
        super(artistTableContainer, playerInventory, title);

        this.xSize = 176;
        this.ySize = 209;
    }

    @Override
    protected void init() {
        super.init();

        final int COMBINED_CANVAS_POSITION_X = 105;
        final int COMBINED_CANVAS_POSITION_Y = 41;

        this.combinedCanvasWidget = new CombinedCanvasWidget(this, this.getGuiLeft() + COMBINED_CANVAS_POSITION_X, this.getGuiTop() + COMBINED_CANVAS_POSITION_Y);

        this.children.add(this.combinedCanvasWidget);

        this.initFields();
    }

    protected void initFields() {
        this.minecraft.keyboardListener.enableRepeatEvents(true);

        this.nameField = new TextFieldWidget(this.font, this.guiLeft + 10, this.guiTop + 103, 94, 12, new TranslationTextComponent("container.immersivemp.lock_table"));
        this.nameField.setCanLoseFocus(false);
        this.nameField.setTextColor(-1);
        this.nameField.setDisabledTextColour(-1);
        this.nameField.setEnableBackgroundDrawing(false);
        this.nameField.setMaxStringLength(35);
        this.nameField.setResponder(this::renameItem);
        this.children.add(this.nameField);
        this.setFocusedDefault(this.nameField);
    }

    // Listener interface - to track name field availability

    private void renameItem(String name) {
        //this.container.updateItemName(name);

        //CLockTableRenameItemPacket renameItemPacket = new CLockTableRenameItemPacket(name);
        //ModLockNetwork.simpleChannel.sendToServer(renameItemPacket);
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);

        super.render(matrixStack, mouseX, mouseY, partialTicks);

        this.renderNameField(matrixStack, mouseX, mouseY, partialTicks);
        this.renderHoveredTooltip(matrixStack, mouseX, mouseY);
    }

    public void renderNameField(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.nameField.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public void tick() {
        super.tick();

        this.tick++;
        this.nameField.tick();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int x, int y) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bindTexture(ARTIST_TABLE_RESOURCE);

        this.blit(matrixStack, this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);

        // Draw input

        this.blit(matrixStack, this.guiLeft + 7, this.guiTop + 99, 0, this.ySize + (this.allowedToNameItem() && this.nameField.isFocused() ? 0 : 16), 100, 16);

        final int SIGN_BUTTON_XPOS = 111;
        final int SIGN_BUTTON_YPOS = 99;
        final int SIGN_BUTTON_UPOS = 176;
        final int SIGN_BUTTON_VPOS = 0;
        final int SIGN_BUTTON_WIDTH = 36;
        final int SIGN_BUTTON_HEIGHT = 16;

        if (this.getContainer().canvasReady()) {
            int buttonVOffset = SIGN_BUTTON_VPOS;

            if (isInRect(this.guiLeft + SIGN_BUTTON_XPOS, this.guiTop + SIGN_BUTTON_YPOS, SIGN_BUTTON_WIDTH, SIGN_BUTTON_HEIGHT, x, y)) {
                buttonVOffset += SIGN_BUTTON_HEIGHT * 2;
            }

            this.blit(matrixStack, this.guiLeft + SIGN_BUTTON_XPOS, this.guiTop + SIGN_BUTTON_YPOS, SIGN_BUTTON_UPOS, buttonVOffset, SIGN_BUTTON_WIDTH, SIGN_BUTTON_HEIGHT);
        } else {
            this.blit(matrixStack, this.guiLeft + SIGN_BUTTON_XPOS, this.guiTop + SIGN_BUTTON_YPOS, SIGN_BUTTON_UPOS, SIGN_BUTTON_VPOS + SIGN_BUTTON_HEIGHT, SIGN_BUTTON_WIDTH, SIGN_BUTTON_HEIGHT);
        }

        final int LOADING_XPOS = 129;
        final int LOADING_YPOS = 60;
        final int LOADING_UPOS = 100;
        final int LOADING_VPOS = this.ySize;
        final int LOADING_WIDTH = 16;
        final int LOADING_HEIGHT = 10;

        if (this.getContainer().canvasLoading()) {
            final int animation = this.tick % 40;
            int frame = animation / 10; // 0-3

            frame = frame > 2 ? 1 : frame; // 3rd frame is the same as 1st frame

            this.blit(matrixStack, this.guiLeft + LOADING_XPOS, this.guiTop + LOADING_YPOS, LOADING_UPOS, LOADING_VPOS + LOADING_HEIGHT * frame, LOADING_WIDTH, LOADING_HEIGHT);
        }

        if (this.getContainer().canvasReady()) {
            this.combinedCanvasWidget.render(matrixStack);
        }
    }

    /**
     * @param matrixStack
     * @param mouseX
     * @param mouseY
     */
    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack matrixStack, int mouseX, int mouseY) {
        final int LABEL_XPOS = 5;
        final int LABEL_YPOS = 5;
        this.font.func_243248_b(matrixStack, this.title, LABEL_XPOS, LABEL_YPOS, Color.darkGray.getRGB());

        final int FONT_Y_SPACING = 10;
        final int PLAYER_INV_LABEL_XPOS = ArtistTableContainer.PLAYER_INVENTORY_XPOS;
        final int PLAYER_INV_LABEL_YPOS = ArtistTableContainer.PLAYER_INVENTORY_YPOS - FONT_Y_SPACING;

        // draw the label for the player inventory slots
        this.font.func_243248_b(matrixStack, this.playerInventory.getDisplayName(),
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
            this.minecraft.player.closeScreen();
            return true;
        }

        return this.nameField.keyPressed(keyCode, scanCode, modifiers) || this.nameField.canWrite() || super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Copy of container method, needed cause conainer for client is just a dummy instance
     * @see LockTableContainer#allowedToNameItem()
     * @return
     */
    public boolean allowedToNameItem() {
        return this.container.canvasReady();
    }

    // Returns true if the given x,y coordinates are within the given rectangle
    public static boolean isInRect(int x, int y, int xSize, int ySize, int mouseX, int mouseY){
        return ((mouseX >= x && mouseX <= x+xSize) && (mouseY >= y && mouseY <= y+ySize));
    }
}
