package me.dantaeusb.zetter.client.gui.overlay;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.util.StringUtils;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class PaintingInfoOverlay extends AbstractGui {
    private static final ITextComponent BANNED_TEXT = new TranslationTextComponent("painting.zetter.banned");

    protected PaintingData paintingData = null;
    protected int overlayMessageTime = 0;

    public void setPainting(PaintingData paintingData) {
        this.paintingData = paintingData;
        this.overlayMessageTime = 15 * 20;
    }

    public void hide() {
        this.paintingData = null;
    }

    public void render(IngameGui gui, MatrixStack poseStack, float partialTick, int screenWidth, int screenHeight) {
        if (this.paintingData == null) {
            return;
        }

        if (this.overlayMessageTime <= 0) {
            this.paintingData = null;
            return;
        }

        ITextComponent title;

        if (!this.paintingData.isBanned()) {
            String paintingName = this.paintingData.getPaintingName();
            String authorName = this.paintingData.getAuthorName();

            if (StringUtils.isNullOrEmpty(paintingName)) {
                paintingName = new TranslationTextComponent("item.zetter.painting.unnamed").getString();
            }

            if (StringUtils.isNullOrEmpty(authorName)) {
                authorName = new TranslationTextComponent("item.zetter.painting.unknown").getString();
            }

            title = new TranslationTextComponent("item.zetter.customPaintingByAuthor", paintingName, authorName);
        } else {
            title = BANNED_TEXT;
        }

        float ticksLeft = (float)this.overlayMessageTime - partialTick;
        int msLeft = (int)(ticksLeft * 255.0F / 20.0F);
        if (msLeft > 255) {
            msLeft = 255;
        }

        if (msLeft > 8) {
            poseStack.pushPose();
            poseStack.translate(screenWidth / 2, screenHeight - 68, 0.0D);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            int textColor = 0xFFFFFF;
            int transparencyMask = msLeft << 24 & 0xFF000000;

            FontRenderer fontRenderer = gui.getFont();

            int titleLength = gui.getFont().width(title);
            this.drawBackdrop(poseStack, gui.getFont(), -4, titleLength, 0xFFFFFF | transparencyMask);
            gui.getFont().drawShadow(poseStack, title, (float) (-titleLength / 2), -4.0F, textColor | transparencyMask);
            RenderSystem.disableBlend();

            poseStack.popPose();
        }
    }

    /**
     * Copied from @linkplain {net.minecraft.client.gui.Gui#drawBackdrop Gui#drawBackdrop} method
     * @param poseStack
     * @param font
     * @param heightOffset
     * @param messageWidth
     * @param color
     */
    protected void drawBackdrop(MatrixStack poseStack, FontRenderer font, int heightOffset, int messageWidth, int color) {
        int backgroundColor = Minecraft.getInstance().options.getBackgroundColor(0.0F);

        if (backgroundColor != 0) {
            int horizontalOffset = -messageWidth / 2;
            fill(
                poseStack,
                horizontalOffset - 2,
                heightOffset - 2,
                horizontalOffset + messageWidth + 2,
                heightOffset + 9 + 2,
                ARGB32.multiply(backgroundColor, color)
            );
        }
    }

    public void tick() {
        if (this.overlayMessageTime > 0) {
            --this.overlayMessageTime;
        }
    }

    public static class ARGB32 {
        public static int alpha(int packedColor) {
            return packedColor >>> 24;
        }

        public static int red(int packedColor) {
            return packedColor >> 16 & 255;
        }

        public static int green(int packedColor) {
            return packedColor >> 8 & 255;
        }

        public static int blue(int packedColor) {
            return packedColor & 255;
        }

        public static int color(int alpha, int red, int green, int blue) {
            return alpha << 24 | red << 16 | green << 8 | blue;
        }

        public static int multiply(int packedColourA, int packedColorB) {
            return color(alpha(packedColourA) * alpha(packedColorB) / 255, red(packedColourA) * red(packedColorB) / 255, green(packedColourA) * green(packedColorB) / 255, blue(packedColourA) * blue(packedColorB) / 255);
        }
    }
}
