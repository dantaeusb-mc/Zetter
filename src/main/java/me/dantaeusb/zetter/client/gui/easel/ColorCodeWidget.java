package me.dantaeusb.zetter.client.gui.easel;

import com.mojang.blaze3d.systems.RenderSystem;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.gui.EaselScreen;
import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zetter.core.tools.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.StringUtil;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @todo: [LOW] Disable if no palette
 */
public class ColorCodeWidget extends AbstractPaintingWidget implements Widget {
    private static final int INACTIVE_COLOR = 0xFF6D634D;

    final static int TEXTBOX_WIDTH = 82;
    final static int TEXTBOX_HEIGHT = 16;
    final static int TEXTBOX_TITLE_HEIGHT = 11;

    final static int TEXTBOX_TEXT_OFFSET = 10;

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("\\p{XDigit}{1,6}");
    private static final Pattern HEX_COLOR_STRICT_PATTERN = Pattern.compile("(\\p{XDigit}{3}|\\p{XDigit}{6})");

    EditBox textField;

    Predicate<String> hexColorValidator = (text) -> {
        if (StringUtil.isNullOrEmpty(text)) {
            return true;
        } else {
            Matcher matcher = HEX_COLOR_PATTERN.matcher(text);
            return matcher.matches();
        }
    };

    public ColorCodeWidget(EaselScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, TEXTBOX_WIDTH, TEXTBOX_HEIGHT + TEXTBOX_TITLE_HEIGHT, new TranslatableComponent("container.zetter.painting.color_code"));
    }

    @Override
    public @Nullable
    Component getTooltip(int mouseX, int mouseY) {
        return null;
    }

    public void initFields() {
        this.textField = new EditBox(
                this.parentScreen.getFont(),
                this.x + TEXTBOX_TEXT_OFFSET + 4,
                this.y + TEXTBOX_TITLE_HEIGHT + 4,
                TEXTBOX_WIDTH - 7,
                12,
                new TranslatableComponent("container.zetter.easel")
        ) {
            public void insertText(String text) {
                text = text.replaceAll("[^\\p{XDigit}]", "");

                super.insertText(text);
            }
        };

        this.textField.setCanLoseFocus(false);
        this.textField.setTextColor(INACTIVE_COLOR);
        this.textField.setTextColorUneditable(INACTIVE_COLOR);
        this.textField.setBordered(false);
        this.textField.setMaxLength(6);
        this.textField.setResponder(this::applyColor);

        this.textField.setFilter(this.hexColorValidator);

        //this.parentScreen.pipeWidget(this.textField);
    }

    public void tick() {
        this.textField.tick();
    }

    public void updateColorValue(int color) {
        // Drop alpha channel
        color = color & 0x00FFFFFF;
        this.textField.setValue(String.format("%1$06X", color));
    }

    private void applyColor(String text) {
        if (!this.textField.isFocused()) {
            return;
        }

        Matcher matcher = HEX_COLOR_STRICT_PATTERN.matcher(text);
        if (!matcher.matches()) {
            return;
        }

        try {
            // Get #AABBCC from #ABC
            if (text.length() == 3) {
                StringBuilder longText = new StringBuilder();
                for (int i = 0; i < 3; i++)
                {
                    longText.append(text.charAt(i));
                    longText.append(text.charAt(i));
                }

                text = longText.toString();
            }

            int color = Integer.parseInt(text, 16) | 0xFF000000;
            this.parentScreen.getMenu().setPaletteColor(color);
        } catch (NumberFormatException exception) {
            Zetter.LOG.error("Invalid color number");
            return;
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
        if (this.textField.isFocused()) {
            // When pasting, just reset field first. It's never combined.
            if (Screen.isPaste(keyCode)) {
                this.textField.setValue("");
            }

            return this.textField.keyPressed(keyCode, scanCode, modifiers) || this.textField.canConsumeInput();
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Quick check
        if (EaselScreen.isInRect(this.x, this.y + TEXTBOX_TITLE_HEIGHT, TEXTBOX_WIDTH, TEXTBOX_HEIGHT, (int) mouseX, (int) mouseY)) {
            this.setFocused(true);
            this.textField.setFocus(true);
            this.textField.setTextColor(Color.WHITE.getRGB());
            return true;
        }

        this.setFocused(false);
        this.textField.setFocus(false);
        this.textField.setTextColor(INACTIVE_COLOR);
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return this.textField.charTyped(codePoint, modifiers);
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, AbstractPaintingWidget.PAINTING_WIDGETS_RESOURCE);

        drawTextbox(matrixStack);
        //drawModeButtons(matrixStack);

        this.textField.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        this.parentScreen.getFont().draw(
                matrixStack,
                this.getMessage(),
                (float) this.x - this.parentScreen.getGuiLeft(),
                (float) this.y - this.parentScreen.getGuiTop(),
                Color.darkGray.getRGB()
        );
    }

    protected void drawTextbox(PoseStack matrixStack) {
        final int TEXTBOX_POSITION_U = 0;
        final int TEXTBOX_POSITION_V = 0;

        int textboxV = TEXTBOX_POSITION_V + (this.textField.isFocused() ? TEXTBOX_HEIGHT : 0);

        this.blit(matrixStack, this.x, this.y + TEXTBOX_TITLE_HEIGHT, TEXTBOX_POSITION_U, textboxV, TEXTBOX_WIDTH, TEXTBOX_HEIGHT);
    }
}
