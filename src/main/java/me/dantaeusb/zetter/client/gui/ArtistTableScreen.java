package me.dantaeusb.zetter.client.gui;

import com.google.common.collect.Lists;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.block.entity.ArtistTableBlockEntity;
import me.dantaeusb.zetter.client.gui.artisttable.AbstractArtistTableWidget;
import me.dantaeusb.zetter.client.gui.artisttable.ChangeActionWidget;
import me.dantaeusb.zetter.client.gui.artisttable.HelpWidget;
import me.dantaeusb.zetter.client.gui.artisttable.PreviewWidget;
import me.dantaeusb.zetter.core.tools.Color;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ArtistTableScreen extends AbstractContainerScreen<ArtistTableMenu> implements ContainerListener {
    public static final Component TITLE = Component.translatable("container.zetter.artist_table");

    private static final Component SPLIT_MODE_TITLE = Component.translatable("container.zetter.artist_table.mode.split");
    private static final Component COMBINE_MODE_TITLE = Component.translatable("container.zetter.artist_table.mode.combine");

    protected final Component titleLabel = TITLE;

    // This is the resource location for the background image
    public static final ResourceLocation ARTIST_TABLE_GUI_TEXTURE_RESOURCE = new ResourceLocation(Zetter.MOD_ID, "textures/gui/artist_table.png");

    private final List<AbstractArtistTableWidget> artistTableWidgets = Lists.newArrayList();

    private PreviewWidget previewWidget;
    private ChangeActionWidget changeActionWidget;
    private HelpWidget helpWidget;

    private int tick = 0;

    final int WIDTH = 230;
    final int HEIGHT = 192;

    final int COMBINED_CANVAS_POSITION_COMBINE_X = 147;
    final int COMBINED_CANVAS_POSITION_COMBINE_Y = 23;

    final int COMBINED_CANVAS_POSITION_SPLIT_X = 17;
    final int COMBINED_CANVAS_POSITION_SPLIT_Y = 23;

    public ArtistTableScreen(ArtistTableMenu artistTableMenu, Inventory playerInventory, Component title) {
        super(artistTableMenu, playerInventory, title);

        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
    }

    @Override
    protected void init() {
        super.init();

        final int CHANGE_ACTION_BUTTON_POSITION_X = WIDTH / 2 - 10;
        final int CHANGE_ACTION_BUTTON_POSITION_Y = 28;

        final int HELP_POSITION_X = 219;
        final int HELP_POSITION_Y = 0;

        // @todo: [LOW] Use rebuildWidgets in Screen
        this.artistTableWidgets.clear();

        this.previewWidget = new PreviewWidget(this, this.getGuiLeft() + COMBINED_CANVAS_POSITION_COMBINE_X, this.getGuiTop() + COMBINED_CANVAS_POSITION_COMBINE_Y);
        this.changeActionWidget = new ChangeActionWidget(this, this.getGuiLeft() + CHANGE_ACTION_BUTTON_POSITION_X, this.getGuiTop() + CHANGE_ACTION_BUTTON_POSITION_Y);
        this.helpWidget = new HelpWidget(this, this.getGuiLeft() + HELP_POSITION_X, this.getGuiTop() + HELP_POSITION_Y);

        this.addArtistTableWidget(this.previewWidget);
        this.addArtistTableWidget(this.changeActionWidget);
        this.addArtistTableWidget(this.helpWidget);

        this.getMenu().addSlotListener(this);
    }

    public void updateCombinedCanvasPosition() {
        if (this.getMenu().getMode() == ArtistTableMenu.Mode.COMBINE) {
            this.previewWidget.setX(this.getGuiLeft() + COMBINED_CANVAS_POSITION_COMBINE_X);
            this.previewWidget.setY(this.getGuiTop() + COMBINED_CANVAS_POSITION_COMBINE_Y);
        } else {
            this.previewWidget.setX(this.getGuiLeft() + COMBINED_CANVAS_POSITION_SPLIT_X);
            this.previewWidget.setY(this.getGuiTop() + COMBINED_CANVAS_POSITION_SPLIT_Y);
        }
    }

    public void addArtistTableWidget(AbstractArtistTableWidget widget) {
        this.artistTableWidgets.add(widget);
        this.addWidget(widget);
    }

    // Listener interface - to track name field availability

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public void containerTick() {
        super.containerTick();

        this.tick++;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        guiGraphics.blit(ARTIST_TABLE_GUI_TEXTURE_RESOURCE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 512, 256);

        final int GRID_XPOS_COMBINE = ArtistTableMenu.COMBINATION_SLOTS_COMBINE_X - 5;
        final int GRID_XPOS_SPLIT = ArtistTableMenu.COMBINATION_SLOTS_SPLIT_X - 5;
        final int GRID_YPOS_COMBINE = ArtistTableMenu.COMBINATION_SLOTS_COMBINE_Y - 5;
        final int GRID_YPOS_SPLIT = ArtistTableMenu.COMBINATION_SLOTS_SPLIT_Y - 5;
        final int GRID_UPOS = 250;
        final int GRID_VPOS = 26;
        final int GRID_SIZE = 80;

        if (this.menu.getMode() == ArtistTableMenu.Mode.COMBINE) {
            // Combination grid
            guiGraphics.blit(ARTIST_TABLE_GUI_TEXTURE_RESOURCE, this.leftPos + GRID_XPOS_COMBINE, this.topPos + GRID_YPOS_COMBINE, GRID_UPOS, GRID_VPOS, GRID_SIZE, GRID_SIZE, 512, 256);
            // Preview bg
            guiGraphics.blit(ARTIST_TABLE_GUI_TEXTURE_RESOURCE, this.leftPos + GRID_XPOS_SPLIT, this.topPos + GRID_YPOS_SPLIT, GRID_UPOS, GRID_VPOS + GRID_SIZE, GRID_SIZE, GRID_SIZE, 512, 256);
        } else {
            // Combination grid
            guiGraphics.blit(ARTIST_TABLE_GUI_TEXTURE_RESOURCE, this.leftPos + GRID_XPOS_SPLIT, this.topPos + GRID_YPOS_SPLIT, GRID_UPOS, GRID_VPOS, GRID_SIZE, GRID_SIZE, 512, 256);
            // Preview bg
            guiGraphics.blit(ARTIST_TABLE_GUI_TEXTURE_RESOURCE, this.leftPos + GRID_XPOS_COMBINE, this.topPos + GRID_YPOS_COMBINE, GRID_UPOS, GRID_VPOS + GRID_SIZE, GRID_SIZE, GRID_SIZE, 512, 256);
        }

        final int COMBINED_SLOT_YPOS = 62;

        final int COMBINED_SLOT_SIZE = 26;

        final int COMBINED_SLOT_UPOS = 250;
        final int COMBINED_SLOT_VPOS = 0;

        final int ARROWS_YPOS = 34;

        final int ARROWS_WIDTH = 45;
        final int ARROWS_HEIGHT = 26;

        final int ARROWS_UPOS = 0;
        final int ARROWS_VPOS = 192;

        if (this.menu.getMode() == ArtistTableMenu.Mode.COMBINE) {
            guiGraphics.blit(ARTIST_TABLE_GUI_TEXTURE_RESOURCE, this.leftPos + this.imageWidth / 2 - ARROWS_WIDTH / 2, this.topPos + ARROWS_YPOS, ARROWS_UPOS, ARROWS_VPOS, ARROWS_WIDTH, ARROWS_HEIGHT, 512, 256);
            guiGraphics.blit(ARTIST_TABLE_GUI_TEXTURE_RESOURCE, this.leftPos + this.imageWidth / 2 - COMBINED_SLOT_SIZE / 2, this.topPos + COMBINED_SLOT_YPOS, COMBINED_SLOT_UPOS, COMBINED_SLOT_VPOS, COMBINED_SLOT_SIZE, COMBINED_SLOT_SIZE, 512, 256);
        } else {
            guiGraphics.blit(ARTIST_TABLE_GUI_TEXTURE_RESOURCE, this.leftPos + this.imageWidth / 2 - ARROWS_WIDTH / 2, this.topPos + ARROWS_YPOS, ARROWS_UPOS + ARROWS_WIDTH, ARROWS_VPOS, ARROWS_WIDTH, ARROWS_HEIGHT, 512, 256);
            guiGraphics.blit(ARTIST_TABLE_GUI_TEXTURE_RESOURCE, this.leftPos + this.imageWidth / 2 - COMBINED_SLOT_SIZE / 2, this.topPos + COMBINED_SLOT_YPOS, COMBINED_SLOT_UPOS + COMBINED_SLOT_SIZE, COMBINED_SLOT_VPOS, COMBINED_SLOT_SIZE, COMBINED_SLOT_SIZE, 512, 256);
        }

        final int LOADING_UPOS = 230;
        final int LOADING_VPOS = 108;
        final int LOADING_WIDTH = 16;
        final int LOADING_HEIGHT = 10;

        final int INVALID_UPOS = 230;
        final int INVALID_VPOS = 138;
        final int INVALID_WIDTH = 10;
        final int INVALID_HEIGHT = 10;

        this.changeActionWidget.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.helpWidget.render(guiGraphics, mouseX, mouseY, partialTicks);

        // @todo: [MED] Move to preview widget
        switch (this.getMenu().getActionState()) {
            case EMPTY:
                break;
            case INVALID:
                int xPosInvalid = (GRID_SIZE - INVALID_WIDTH) / 2;
                int yPosInvalid = (GRID_SIZE - INVALID_HEIGHT) / 2;

                if (this.menu.getMode() == ArtistTableMenu.Mode.COMBINE) {
                    xPosInvalid += GRID_XPOS_SPLIT;
                    yPosInvalid += GRID_YPOS_SPLIT;
                } else {
                    xPosInvalid += GRID_XPOS_COMBINE;
                    yPosInvalid += GRID_YPOS_COMBINE;
                }

                guiGraphics.blit(ARTIST_TABLE_GUI_TEXTURE_RESOURCE, this.leftPos + xPosInvalid, this.topPos + yPosInvalid, INVALID_UPOS, INVALID_VPOS, INVALID_WIDTH, INVALID_HEIGHT, 512, 256);
                break;
            case NOT_LOADED:
                int xPosLoading = (GRID_SIZE - LOADING_WIDTH) / 2;
                int yPosLoading = (GRID_SIZE - LOADING_HEIGHT) / 2;

                if (this.menu.getMode() == ArtistTableMenu.Mode.COMBINE) {
                    xPosLoading += GRID_XPOS_SPLIT;
                    yPosLoading += GRID_YPOS_SPLIT;
                } else {
                    xPosLoading += GRID_XPOS_COMBINE;
                    yPosLoading += GRID_YPOS_COMBINE;
                }

                final int animation = this.tick % 40;
                int frame = animation / 10; // 0-3

                frame = frame > 2 ? 1 : frame; // 3rd frame is the same as 1st frame

                guiGraphics.blit(ARTIST_TABLE_GUI_TEXTURE_RESOURCE, this.leftPos + xPosLoading, this.topPos + yPosLoading, LOADING_UPOS, LOADING_VPOS + LOADING_HEIGHT * frame, LOADING_WIDTH, LOADING_HEIGHT, 512, 256);
                break;
            case READY:
                this.previewWidget.render(guiGraphics, mouseX, mouseY, partialTicks);
                break;
        }
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics guiGraphics, int x, int y) {
        super.renderTooltip(guiGraphics, x, y);

        for (AbstractArtistTableWidget widget : this.artistTableWidgets) {
            if (widget.isMouseOver(x, y)) {
                widget.renderTooltip(guiGraphics, x, y);
            }
        }
    }

    /**
     * @param guiGraphics
     * @param mouseX
     * @param mouseY
     */
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        final int LABEL_XPOS = 5;
        final int LABEL_YPOS = 5;

        Component artistTableModelLabel = this.getMenu().getMode() == ArtistTableMenu.Mode.COMBINE ? COMBINE_MODE_TITLE : SPLIT_MODE_TITLE;

        Component artistTableLabel = Component.translatable("container.zetter.artist_table.mode", this.titleLabel, artistTableModelLabel);

        guiGraphics.drawString(this.font, artistTableLabel, LABEL_XPOS, LABEL_YPOS, Color.darkGray.getRGB(), false);

        final int FONT_Y_SPACING = 10;
        final int PLAYER_INV_LABEL_XPOS = ArtistTableMenu.PLAYER_INVENTORY_XPOS;
        final int PLAYER_INV_LABEL_YPOS = ArtistTableMenu.PLAYER_INVENTORY_YPOS - FONT_Y_SPACING;

        // draw the label for the player inventory slots
        guiGraphics.drawString(this.font, this.playerInventoryTitle, PLAYER_INV_LABEL_XPOS, PLAYER_INV_LABEL_YPOS, Color.darkGray.getRGB(), false);
    }

    @Override
    public void slotChanged(AbstractContainerMenu menu, int slotId, ItemStack stack) {

    }


    /**
     * Expose some methods for widgets
     */

    public Font getFont() {
        return this.font;
    }

    /**
     * Update widget position on mode change
     * @param menu
     * @param dataSlotIndex
     * @param value
     */
    @Override
    public void dataChanged(AbstractContainerMenu menu, int dataSlotIndex, int value) {
        if (dataSlotIndex == ArtistTableBlockEntity.DATA_MODE) {
            this.updateCombinedCanvasPosition();
        }
    }

    @Override
    public void removed() {
        super.removed();

        this.getMenu().removeSlotListener(this);
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
        return super.mouseClicked(mouseX, mouseY, button);
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

    // Returns true if the given x,y coordinates are within the given rectangle
    public static boolean isInRect(int x, int y, int xSize, int ySize, int mouseX, int mouseY){
        return ((mouseX >= x && mouseX <= x+xSize) && (mouseY >= y && mouseY <= y+ySize));
    }
}
