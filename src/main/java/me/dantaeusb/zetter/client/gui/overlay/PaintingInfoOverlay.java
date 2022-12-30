package me.dantaeusb.zetter.client.gui.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.FastColor;
import net.minecraft.util.StringUtil;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.gui.IIngameOverlay;

public class PaintingInfoOverlay implements IIngameOverlay {
    private static final Component BANNED_TEXT = new TranslatableComponent("painting.zetter.banned");

    protected PaintingData paintingData = null;
    protected int overlayMessageTime = 0;

    public void setPainting(PaintingData paintingData) {
        this.paintingData = paintingData;
        this.overlayMessageTime = 15 * 20;
    }

    public void hide() {
        this.paintingData = null;
    }

    @Override
    public void render(ForgeIngameGui gui, PoseStack poseStack, float partialTick, int screenWidth, int screenHeight) {
        if (this.paintingData == null) {
            return;
        }

        if (this.overlayMessageTime <= 0) {
            this.paintingData = null;
            return;
        }

        Component title;

        if (!this.paintingData.isBanned()) {
            String paintingName = this.paintingData.getPaintingName();
            String authorName = this.paintingData.getAuthorName();

            if (StringUtil.isNullOrEmpty(paintingName)) {
                paintingName = new TranslatableComponent("item.zetter.painting.unnamed").getString();
            }

            if (StringUtil.isNullOrEmpty(authorName)) {
                authorName = new TranslatableComponent("item.zetter.painting.unknown").getString();
            }

            title = new TranslatableComponent("item.zetter.customPaintingByAuthor", paintingName, authorName);
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

            int titleLength = gui.getFont().width(title);
            this.drawBackdrop(poseStack, gui.getFont(), -4, titleLength, 0xFFFFFF | transparencyMask);
            gui.getFont().drawShadow(poseStack, title, (float)(-titleLength / 2), -4.0F, textColor | transparencyMask);
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
    protected void drawBackdrop(PoseStack poseStack, Font font, int heightOffset, int messageWidth, int color) {
        int backgroundColor = Minecraft.getInstance().options.getBackgroundColor(0.0F);

        if (backgroundColor != 0) {
            int horizontalOffset = -messageWidth / 2;
            GuiComponent.fill(poseStack, horizontalOffset - 2, heightOffset - 2, horizontalOffset + messageWidth + 2, heightOffset + 9 + 2, FastColor.ARGB32.multiply(backgroundColor, color));
        }
    }

    public void tick() {
        if (this.overlayMessageTime > 0) {
            --this.overlayMessageTime;
        }
    }
}
