package com.dantaeusb.zetter.client.gui;

import com.dantaeusb.zetter.client.gui.artisttable.CombinedCanvasWidget;
import com.dantaeusb.zetter.client.gui.painting.*;
import com.dantaeusb.zetter.client.renderer.CanvasRenderer;
import com.dantaeusb.zetter.container.ArtistTableContainer;
import com.dantaeusb.zetter.storage.CanvasData;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.IContainerListener;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.storage.MapData;

import javax.annotation.Nullable;

public class ArtistTableScreen extends ContainerScreen<ArtistTableContainer>/* implements IContainerListener*/ {
    protected final ITextComponent title = new TranslationTextComponent("container.zetter.artistTable");
    private TextFieldWidget nameField;

    // This is the resource location for the background image
    private static final ResourceLocation ARTIST_TABLE_RESOURCE = new ResourceLocation("zetter", "textures/paintings/gui/artist_table.png");

    private CombinedCanvasWidget combinedCanvasWidget;

    private Button buttonSign;

    public ArtistTableScreen(ArtistTableContainer artistTableContainer, PlayerInventory playerInventory, ITextComponent title) {
        super(artistTableContainer, playerInventory, title);

        this.xSize = 176;
        this.ySize = 202;
    }

    @Override
    protected void init() {
        super.init();

        final int COMBINED_CANVAS_POSITION_X = 105;
        final int COMBINED_CANVAS_POSITION_Y = 41;

        this.combinedCanvasWidget = new CombinedCanvasWidget(this, this.getGuiLeft() + COMBINED_CANVAS_POSITION_X, this.getGuiTop() + COMBINED_CANVAS_POSITION_Y);

        this.children.add(this.combinedCanvasWidget);

        this.minecraft.keyboardListener.enableRepeatEvents(true);

        final int SIGN_BUTTON_XPOS = 125;
        final int SIGN_BUTTON_YPOS = 99;

        this.buttonSign = this.addButton(new Button(this.guiLeft + SIGN_BUTTON_XPOS, this.guiTop + SIGN_BUTTON_YPOS, 44, 20, DialogTexts.GUI_DONE, (p_214201_1_) -> {

        }));
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
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int x, int y) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bindTexture(ARTIST_TABLE_RESOURCE);

        this.blit(matrixStack, this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);

        // Draw input

        this.blit(matrixStack, this.guiLeft + 7, this.guiTop + 99, 0, this.ySize + (this.allowedToNameItem() && this.nameField.isFocused() ? 0 : 16), 110, 16);

        if (this.getContainer().checkCanvasLayout()) {
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

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Copy of container method, needed cause conainer for client is just a dummy instance
     * @see LockTableContainer#allowedToNameItem()
     * @return
     */
    public boolean allowedToNameItem() {
        return false;
    }
}
