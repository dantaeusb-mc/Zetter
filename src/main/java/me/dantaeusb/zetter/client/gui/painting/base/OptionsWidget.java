package me.dantaeusb.zetter.client.gui.painting.base;

import com.mojang.blaze3d.systems.RenderSystem;
import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.AbstractPaintingWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A row of square buttons where exactly one option is picked at a time. Buttons sit
 * side by side in the widgets texture in the order the options are given, and the
 * pressed state of each one sits directly below it.
 */
public class OptionsWidget<T> extends AbstractPaintingWidget implements Renderable {
    public final static int BUTTON_SIZE = 20;

    private final static int PRESSED_V_OFFSET = BUTTON_SIZE;

    private final T[] options;
    private final Function<T, Component> tooltipLambda;
    private final @NotNull Supplier<T> valueSupplier;
    private final Consumer<T> valueConsumer;

    private final int u;
    private final int v;

    public OptionsWidget(
        PaintingScreen parentScreen, int x, int y, Component translatableComponent,
        T[] options, Function<T, Component> tooltipLambda,
        @NotNull Supplier<T> valueSupplier, Consumer<T> valueConsumer,
        int u, int v
    ) {
        super(parentScreen, x, y, BUTTON_SIZE * options.length, BUTTON_SIZE, translatableComponent);

        this.options = options;
        this.tooltipLambda = tooltipLambda;
        this.valueSupplier = valueSupplier;
        this.valueConsumer = valueConsumer;

        this.u = u;
        this.v = v;
    }

    private @Nullable T getOptionAt(double mouseX, double mouseY) {
        if (!this.isMouseOver(mouseX, mouseY)) {
            return null;
        }

        final int index = ((int) mouseX - this.getX()) / BUTTON_SIZE;

        return index >= 0 && index < this.options.length ? this.options[index] : null;
    }

    @Override
    public @Nullable Component getTooltip(int mouseX, int mouseY) {
        final T option = this.getOptionAt(mouseX, mouseY);

        return option == null ? null : this.tooltipLambda.apply(option);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.isValidClickButton(button)) {
            return false;
        }

        final T option = this.getOptionAt(mouseX, mouseY);

        if (option == null) {
            return false;
        }

        this.valueConsumer.accept(option);
        this.playDownSound(Minecraft.getInstance().getSoundManager());

        return true;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, PAINTING_WIDGETS_TEXTURE_RESOURCE);

        final T selected = this.valueSupplier.get();

        for (int i = 0; i < this.options.length; i++) {
            final int vOffset = Objects.equals(this.options[i], selected) ? PRESSED_V_OFFSET : 0;

            guiGraphics.blit(
                PAINTING_WIDGETS_TEXTURE_RESOURCE,
                this.getX() + i * BUTTON_SIZE, this.getY(),
                this.u + i * BUTTON_SIZE, this.v + vOffset,
                BUTTON_SIZE, BUTTON_SIZE
            );
        }
    }
}
