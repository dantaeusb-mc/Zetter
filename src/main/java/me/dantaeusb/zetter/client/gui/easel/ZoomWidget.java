package me.dantaeusb.zetter.client.gui.easel;

import com.mojang.blaze3d.systems.RenderSystem;
import me.dantaeusb.zetter.client.gui.EaselScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @todo: [LOW] Add paddings as on history buttons
 */
public class ZoomWidget extends AbstractEaselWidget implements Renderable {
    private final List<ZoomButton> buttons;

    public static final int ZOOM_OUT_HOTKEY = GLFW.GLFW_KEY_MINUS;
    public static final int ZOOM_IN_HOTKEY = GLFW.GLFW_KEY_EQUAL;

    final static int ZOOM_BUTTON_WIDTH = 12;
    final static int ZOOM_BUTTON_HEIGHT = 12;

    final static int ZOOM_BUTTONS_U = 208;
    final static int ZOOM_BUTTONS_V = 197;

    public ZoomWidget(EaselScreen parentScreen, int x, int y) {
        // Add borders
        super(parentScreen, x, y, ZOOM_BUTTON_WIDTH * 2, ZOOM_BUTTON_HEIGHT, Component.translatable("container.zetter.painting.zoom"));


        this.buttons = new ArrayList<>() {{
            add(new ZoomButton(
                    parentScreen.getMenu()::canIncreaseCanvasScale, parentScreen.getMenu()::increaseCanvasScale,
                    ZOOM_BUTTONS_U, ZOOM_BUTTONS_V, ZOOM_BUTTON_WIDTH, ZOOM_BUTTON_HEIGHT,
                    Component.translatable("container.zetter.painting.zoom.in"))
            );
            add(new ZoomButton(parentScreen.getMenu()::canDecreaseCanvasScale, parentScreen.getMenu()::decreaseCanvasScale,
                    ZOOM_BUTTONS_U + ZOOM_BUTTON_WIDTH, ZOOM_BUTTONS_V, ZOOM_BUTTON_WIDTH, ZOOM_BUTTON_HEIGHT,
                    Component.translatable("container.zetter.painting.zoom.out"))
            );
        }};
    }

    @Override
    public @Nullable
    Component getTooltip(int mouseX, int mouseY) {
        int i = 0;
        for (ZoomButton zoomButton: this.buttons) {
            int fromX = this.getX() + i * ZOOM_BUTTON_WIDTH;

            if (EaselScreen.isInRect(fromX, this.getY(), ZOOM_BUTTON_WIDTH, ZOOM_BUTTON_HEIGHT, mouseX, mouseY)) {
                return zoomButton.getTooltip();
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
        for (ZoomButton zoomButton: this.buttons) {
            int fromX = this.getX() + i * ZOOM_BUTTON_WIDTH;

            if (EaselScreen.isInRect(fromX, this.getY(), ZOOM_BUTTON_WIDTH, ZOOM_BUTTON_HEIGHT, iMouseX, iMouseY)) {
                zoomButton.action.get();

                this.playDownSound(Minecraft.getInstance().getSoundManager());
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
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, AbstractEaselWidget.EASEL_WIDGETS_TEXTURE_RESOURCE);

        guiGraphics.blit(AbstractEaselWidget.EASEL_WIDGETS_TEXTURE_RESOURCE, this.getX(), this.getY(), ZOOM_BUTTONS_U - ZOOM_BUTTON_WIDTH * 2, ZOOM_BUTTONS_V, ZOOM_BUTTON_WIDTH * this.buttons.size(), ZOOM_BUTTON_HEIGHT);

        int i = 0;
        for (ZoomButton zoomButton: this.buttons) {
            int fromX = this.getX() + i * ZOOM_BUTTON_WIDTH;
            int uOffset = zoomButton.uPosition + (zoomButton.active.get() ? 0 : ZOOM_BUTTON_WIDTH * 2);

            guiGraphics.blit(AbstractEaselWidget.EASEL_WIDGETS_TEXTURE_RESOURCE, fromX, this.getY(), uOffset, zoomButton.vPosition, zoomButton.width, zoomButton.height);
            i++;
        }
    }

    public boolean undo() {
        return false;
    }

    public boolean redo() {
        return false;
    }

    public class ZoomButton {
        public final Supplier<Boolean> active;
        public final Supplier<Boolean> action;
        public final int uPosition;
        public final int vPosition;
        public final int height;
        public final int width;
        public final Component label;

        ZoomButton(Supplier<Boolean> active, Supplier<Boolean> action, int uPosition, int vPosition, int width, int height, Component label) {
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
