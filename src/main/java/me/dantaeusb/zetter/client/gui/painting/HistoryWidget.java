package me.dantaeusb.zetter.client.gui.painting;

import me.dantaeusb.zetter.client.gui.EaselScreen;
import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.easel.AbstractEaselWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class HistoryWidget extends AbstractPaintingWidget implements Renderable {
    private final List<HistoryButton> buttons;

    public static final int UNDO_HOTKEY = GLFW.GLFW_KEY_Z;
    public static final int REDO_HOTKEY = GLFW.GLFW_KEY_Y;

    final static int HISTORY_BUTTON_WIDTH = 22;
    final static int HISTORY_BUTTON_HEIGHT = 13;

    final static int HISTORY_BUTTONS_U = 209;
    final static int HISTORY_BUTTONS_V = 210;

    public HistoryWidget(PaintingScreen parentScreen, int x, int y) {
        // Add borders
        super(parentScreen, x, y, HISTORY_BUTTON_WIDTH + 2, HISTORY_BUTTON_HEIGHT * 2 + 3, Component.translatable("container.zetter.painting.history"));


        this.buttons = new ArrayList<>() {{
            add(new HistoryButton(
                    parentScreen::canUndo, parentScreen::undo,
                    HISTORY_BUTTONS_U, HISTORY_BUTTONS_V, HISTORY_BUTTON_WIDTH, HISTORY_BUTTON_HEIGHT,
                    Component.translatable("container.zetter.painting.history.undo"))
            );
            add(new HistoryButton(parentScreen::canRedo, parentScreen::redo,
                    HISTORY_BUTTONS_U, HISTORY_BUTTONS_V + HISTORY_BUTTON_HEIGHT + 1, HISTORY_BUTTON_WIDTH, HISTORY_BUTTON_HEIGHT,
                    Component.translatable("container.zetter.painting.history.redo"))
            );
        }};
    }

    @Override
    public @Nullable
    Component getTooltip(int mouseX, int mouseY) {
        int i = 0;
        for (HistoryButton historyButton: this.buttons) {
            int fromY = this.getY() + 1 + i * HISTORY_BUTTON_HEIGHT + i;

            if (EaselScreen.isInRect(this.getX() + 1, fromY, HISTORY_BUTTON_WIDTH, HISTORY_BUTTON_HEIGHT, mouseX, mouseY)) {
                return historyButton.getTooltip();
            }

            i++;
        }

        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int iMouseX = (int) mouseX;
        int iMouseY = (int) mouseY;

        // Quick check
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }

        int i = 0;
        for (HistoryButton historyButton: this.buttons) {
            int fromY = this.getY() + 1 + i * HISTORY_BUTTON_HEIGHT + i;

            if (EaselScreen.isInRect(this.getX() + 1, fromY, historyButton.width, historyButton.height, iMouseX, iMouseY) && this.isValidClickButton(button)) {
                historyButton.action.run();

                this.playDownSound(Minecraft.getInstance().getSoundManager());
                return true;
            }

            i++;
        }

        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
    }

    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.blit(AbstractEaselWidget.EASEL_WIDGETS_TEXTURE_RESOURCE, this.getX(), this.getY(), HISTORY_BUTTONS_U - HISTORY_BUTTON_WIDTH - 3, HISTORY_BUTTONS_V - 1, HISTORY_BUTTON_WIDTH + 2, HISTORY_BUTTON_HEIGHT * this.buttons.size() + 3);

        int i = 0;
        for (HistoryButton historyButton: this.buttons) {
            int fromY = this.getY() + 1 + i * HISTORY_BUTTON_HEIGHT + i;
            int uOffset = historyButton.uPosition + (historyButton.active.get() ? 0 : HISTORY_BUTTON_WIDTH + 2);

            guiGraphics.blit(AbstractEaselWidget.EASEL_WIDGETS_TEXTURE_RESOURCE, this.getX() + 1, fromY, uOffset, historyButton.vPosition, historyButton.width, historyButton.height);
            i++;
        }
    }

    public boolean undo() {
        return false;
    }

    public boolean redo() {
        return false;
    }

    public class HistoryButton {
        public final Supplier<Boolean> active;
        public final Runnable action;
        public final int uPosition;
        public final int vPosition;
        public final int height;
        public final int width;
        public final Component label;

        HistoryButton(Supplier<Boolean> active, Runnable action, int uPosition, int vPosition, int width, int height, Component label) {
            this.active = active;
            this.action = action;
            this.uPosition = uPosition;
            this.vPosition = vPosition;
            this.height = height;
            this.width = width;
            this.label = label;
        }

        public Component getTooltip() {
            return this.label;
        }
    }
}
