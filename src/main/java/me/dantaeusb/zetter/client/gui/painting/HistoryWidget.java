package me.dantaeusb.zetter.client.gui.painting;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zetter.client.gui.PaintingScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class HistoryWidget extends AbstractPaintingWidget implements Widget {
    private final List<HistoryButton> buttons;

    final static int HISTORY_BUTTON_WIDTH = 24;

    final static int HISTORY_BUTTON_HEIGHT = 15;
    final static int HISTORY_OFFSET = HISTORY_BUTTON_HEIGHT - 1; // 1px border between slots

    public HistoryWidget(PaintingScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, HISTORY_BUTTON_WIDTH, HISTORY_BUTTON_HEIGHT + HISTORY_OFFSET * 3, new TranslatableComponent("container.zetter.painting.history"));

        final int TOOL_BUTTON_U = 208;
        final int TOOL_BUTTON_V = 149;

        this.buttons = new ArrayList<>() {{
            add(new HistoryButton(
                    parentScreen.getMenu()::canUndo, parentScreen.getMenu()::undo,
                    TOOL_BUTTON_U, TOOL_BUTTON_V, HISTORY_BUTTON_WIDTH, HISTORY_BUTTON_HEIGHT,
                    new TranslatableComponent("container.zetter.painting.history.undo"))
            );
            add(new HistoryButton(parentScreen.getMenu()::canRedo, parentScreen.getMenu()::redo,
                    TOOL_BUTTON_U, TOOL_BUTTON_V + HISTORY_BUTTON_HEIGHT, HISTORY_BUTTON_WIDTH, HISTORY_BUTTON_HEIGHT,
                    new TranslatableComponent("container.zetter.painting.history.redo"))
            );
        }};
    }

    @Override
    public @Nullable
    Component getTooltip(int mouseX, int mouseY) {
        int i = 0;
        for (HistoryButton historyButton: this.buttons) {
            int fromY = this.y + i * HISTORY_OFFSET;

            if (PaintingScreen.isInRect(this.x, fromY, HISTORY_BUTTON_WIDTH, HISTORY_BUTTON_HEIGHT, mouseX, mouseY)) {
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
            int fromY = this.y + i * HISTORY_OFFSET;

            if (PaintingScreen.isInRect(this.x, fromY, historyButton.width, historyButton.height, iMouseX, iMouseY) && this.isValidClickButton(button)) {
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                return true;
            }

            i++;
        }

        return false;
    }

    public void render(PoseStack matrixStack) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, AbstractPaintingWidget.PAINTING_WIDGETS_RESOURCE);

        int i = 0;
        for (HistoryButton historyButton: this.buttons) {
            int fromY = this.y + i * HISTORY_OFFSET;
            int uOffset = historyButton.uPosition + (historyButton.active.get() ? 0 : -HISTORY_BUTTON_WIDTH);

            this.blit(matrixStack, this.x, fromY, uOffset, historyButton.vPosition, historyButton.width, historyButton.height);
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
        public final Supplier<Boolean> action;
        public final int uPosition;
        public final int vPosition;
        public final int height;
        public final int width;
        public final TranslatableComponent label;

        HistoryButton(Supplier<Boolean> active, Supplier<Boolean> action, int uPosition, int vPosition, int width, int height, TranslatableComponent label) {
            this.active = active;
            this.action = action;
            this.uPosition = uPosition;
            this.vPosition = vPosition;
            this.height = height;
            this.width = width;
            this.label = label;
        }

        public TranslatableComponent getTooltip() {
            return this.label;
        }
    }
}
