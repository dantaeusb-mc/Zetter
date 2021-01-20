package com.dantaeusb.immersivemp.locks.client.gui;

import com.dantaeusb.immersivemp.locks.core.ModLockNetwork;
import com.dantaeusb.immersivemp.locks.inventory.container.LockTableContainer;
import com.dantaeusb.immersivemp.locks.network.packet.CLockTableModePacket;
import com.dantaeusb.immersivemp.locks.network.packet.CLockTableRenameItemPacket;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.IContainerListener;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;

public class LockTableContainerScreen extends ContainerScreen<LockTableContainer> implements IContainerListener {
    private LockTableContainer lockTableContainer;
    private TextFieldWidget nameField;
    private boolean keyMode = true;

    protected final ITextComponent title = new TranslationTextComponent("container.immersivemp.lock_table");

    // This is the resource location for the background image
    private static final ResourceLocation LOCK_TABLE_RESOURCE = new ResourceLocation("immersivemp", "textures/locks/gui/lock_table.png");

    final static  int FONT_Y_SPACING = 10;
    final static int PLAYER_INV_LABEL_XPOS = LockTableContainer.PLAYER_INVENTORY_XPOS;
    final static int PLAYER_INV_LABEL_YPOS = LockTableContainer.PLAYER_INVENTORY_YPOS - FONT_Y_SPACING;

    public LockTableContainerScreen(LockTableContainer lockTableContainer, PlayerInventory playerInventory, ITextComponent title) {
        super(lockTableContainer, playerInventory, title);
        this.lockTableContainer = lockTableContainer;

        // Set the width and height of the gui.  Should match the size of the texture!
        this.xSize = 176;
        this.ySize = 166;

        this.playerInventoryTitleY = this.ySize - 110;
    }

    @Override
    protected void init() {
        super.init();
        this.initFields();
    }

    protected void initFields() {
        this.minecraft.keyboardListener.enableRepeatEvents(true);

        this.nameField = new TextFieldWidget(this.font, this.guiLeft + 53, this.guiTop + 24, 94, 12, new TranslationTextComponent("container.immersivemp.lock_table"));
        this.nameField.setCanLoseFocus(false);
        this.nameField.setTextColor(-1);
        this.nameField.setDisabledTextColour(-1);
        this.nameField.setEnableBackgroundDrawing(false);
        this.nameField.setMaxStringLength(35);
        this.nameField.setResponder(this::renameItem);
        this.children.add(this.nameField);
        this.setFocusedDefault(this.nameField);
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderNameField(matrixStack, mouseX, mouseY, partialTicks);
        this.renderHoveredTooltip(matrixStack, mouseX, mouseY);
    }

    public void tick() {
        super.tick();
        this.nameField.tick();
    }

    // Draw the Tool tip text if hovering over something of interest on the screen
    // renderHoveredToolTip
    @Override
    protected void renderHoveredTooltip(MatrixStack matrixStack, int mouseX, int mouseY) {
        super.renderHoveredTooltip(matrixStack, mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int x, int y) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bindTexture(LOCK_TABLE_RESOURCE);

        this.blit(matrixStack, this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);

        // Draw input

        this.blit(matrixStack, this.guiLeft + 50, this.guiTop + 20, 0, this.ySize + (this.allowedToNameItem() ? 0 : 16), 101, 16);

        // Draw button

        //this.setBlitOffset(0);
        int buttonUOffset = 0;
        int buttonVOffset = 198;

        if (keyMode) {
            buttonUOffset += 21;
        }

        if (isInRect(this.guiLeft + 26, this.guiTop + 18, 20, 20, x, y)) {
            buttonVOffset += 21;
        }

        this.blit(matrixStack, this.guiLeft + 26, this.guiTop + 18, buttonUOffset, buttonVOffset, 20, 20);
    }

    /**
     * @todo: use default playerInventoryTitleY and titleY fields
     * @param matrixStack
     * @param mouseX
     * @param mouseY
     */
    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack matrixStack, int mouseX, int mouseY) {
        // draw the label for the top of the screen
        final int LABEL_XPOS = 5;
        final int LABEL_YPOS = 5;
        this.font.func_243248_b(matrixStack, this.title, LABEL_XPOS, LABEL_YPOS, Color.darkGray.getRGB());

        // draw the label for the player inventory slots
        this.font.func_243248_b(matrixStack, this.playerInventory.getDisplayName(),
                PLAYER_INV_LABEL_XPOS, PLAYER_INV_LABEL_YPOS, Color.darkGray.getRGB());
    }

    // Returns true if the given x,y coordinates are within the given rectangle
    public static boolean isInRect(int x, int y, int xSize, int ySize, int mouseX, int mouseY){
        return ((mouseX >= x && mouseX <= x+xSize) && (mouseY >= y && mouseY <= y+ySize));
    }

    // Mode button

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isInRect(this.guiLeft + 26, this.guiTop + 18, 20, 20, (int)mouseX, (int)mouseY)) {
            this.toggleMode();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void toggleMode() {
        this.keyMode = !this.keyMode;

        this.updateNameField();
        this.nameField.setEnabled(this.allowedToNameItem());

        CLockTableModePacket modePacket = new CLockTableModePacket((this.container).windowId, this.keyMode);
        ModLockNetwork.simpleChannel.sendToServer(modePacket);
    }

    // Name field

    /**
     * Cancel closing screen when pressing "E", handle input properly
     * @param keyCode
     * @param scanCode
     * @param modifiers
     * @return
     */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.minecraft.player.closeScreen();
        }

        return this.nameField.keyPressed(keyCode, scanCode, modifiers) || this.nameField.canWrite() || super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void renderNameField(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.nameField.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    private void updateNameField() {
        this.nameField.setEnabled(this.allowedToNameItem());

        if (!this.nameField.canWrite()) {
            this.nameField.setText("");
        }
    }

    /**
     * Copy of container method, needed cause conainer for client is just a dummy instance
     * @see LockTableContainer#allowedToNameItem()
     * @return
     */
    public boolean allowedToNameItem() {
        boolean haveMaterial = this.getContainer().getSlot(0).getHasStack();
        boolean haveTemplate = this.getContainer().getSlot(1).getHasStack();

        return this.keyMode && haveMaterial && !haveTemplate;
    }

    // Listener interface - to track name field availability

    private void renameItem(String name) {
        this.container.updateItemName(name);

        CLockTableRenameItemPacket renameItemPacket = new CLockTableRenameItemPacket(name);
        ModLockNetwork.simpleChannel.sendToServer(renameItemPacket);
    }

    public void sendAllContents(Container containerToSend, NonNullList<ItemStack> itemsList) {
        this.sendSlotContents(containerToSend, 0, containerToSend.getSlot(0).getStack());
    }

    public void sendSlotContents(Container containerToSend, int slotInd, ItemStack stack) {
        if (slotInd == 0) {
            this.nameField.setText(stack.isEmpty() ? "" : stack.getDisplayName().getString());
            this.updateNameField();
            this.setListener(this.nameField);
        }
    }

    public void sendWindowProperty(Container containerIn, int varToUpdate, int newValue) {
    }

    private static final Logger LOGGER = LogManager.getLogger();
}
