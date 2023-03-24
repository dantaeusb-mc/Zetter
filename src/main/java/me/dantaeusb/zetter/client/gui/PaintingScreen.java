package me.dantaeusb.zetter.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterNetwork;
import me.dantaeusb.zetter.network.packet.CSignPaintingPacket;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.fonts.TextInputUtil;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import org.lwjgl.glfw.GLFW;

public class PaintingScreen extends Screen {
    private static final ITextComponent DEFAULT_TITLE = new TranslationTextComponent("item.zetter.painting.unnamed");

    private final PlayerEntity owner;
    private final Hand hand;

    private final String canvasCode;
    private final AbstractCanvasData canvasData;

    private final String authorName;
    private String title = "";
    private int tick = 0;
    private boolean editable = true;

    private int paintingOffsetX;
    private int paintingOffsetY;
    private int paintingWidth;
    private int paintingHeight;

    private float paintingScale;

    private int screenOffsetX;
    private int screenOffsetY;
    private int screenWidth;
    private int screenHeight;

    private Button signButton;

    private final TextInputUtil titleEdit = new TextInputUtil(() -> {
        return this.title;
    }, (String input) -> {
        this.title = input;
    }, this::getClipboard, this::setClipboard, (String input) -> {
        return input.length() <= Helper.PAINTING_TITLE_MAX_LENGTH;
    });

    public static PaintingScreen createScreenForCanvas(PlayerEntity player, String canvasCode, CanvasData canvasData, Hand hand) {
        return new PaintingScreen(player, hand, canvasCode, canvasData, player.getName().getString(), "", true);
    }

    public static PaintingScreen createScreenForPainting(PlayerEntity player, String canvasCode, PaintingData paintingData, Hand hand) {
        return new PaintingScreen(player, hand, canvasCode, paintingData, paintingData.getAuthorName(), paintingData.getPaintingName(), false);
    }

    // @todo: [HIGH] Canvas data could be null!!!
    private PaintingScreen(PlayerEntity player, Hand hand, String canvasCode, AbstractCanvasData canvasData, String authorName, String paintingTitle, boolean editable) {
        super(new TranslationTextComponent("container.zetter.painting"));

        this.owner = player;
        this.hand = hand;

        this.canvasCode = canvasCode;
        this.canvasData = canvasData;

        this.authorName = authorName;
        this.title = paintingTitle;

        this.editable = editable;
    }

    private static final int SCREEN_MARGIN = 10;
    private static final int SCREEN_PADDING = 5;
    private static final int SCREEN_BOTTOM_TEXT = 30;

    private static final int TEXT_COLOR_ACTIVE = 0xFF000000;
    private static final int TEXT_COLOR = 0xFF2B200B;

    private static final int BUTTON_WIDTH = 98;
    private static final int BUTTON_HEIGHT = 20;

    @Override
    public void init() {
        this.calculatePaintingOffset();

        this.signButton = this.addButton(new Button(
            this.screenOffsetX + this.screenWidth - BUTTON_WIDTH - SCREEN_PADDING,
            this.paintingOffsetY + this.paintingHeight + SCREEN_PADDING,
            BUTTON_WIDTH,
            BUTTON_HEIGHT,
            new TranslationTextComponent("book.signButton"),
            (button) -> {
                this.signPainting();
            }
        ));

        this.signButton.visible = this.editable;
    }

    /**
     * Sign painting, creates painting from canvas
     * Uses translated "Unnamed" title by default
     * Sends packet to server
     */
    private void signPainting() {
        int slot = this.hand == Hand.MAIN_HAND ? this.owner.inventory.selected : 40;
        String title = this.title.isEmpty() ? DEFAULT_TITLE.getString() : this.title;

        CSignPaintingPacket signPaintingPacket = new CSignPaintingPacket(slot, title);
        ZetterNetwork.simpleChannel.sendToServer(signPaintingPacket);

        this.minecraft.player.closeContainer();
    }

    /**
     * Calculate offset at which render the painting texture
     */
    private void calculatePaintingOffset() {
        float paintingAspectRatio = this.canvasData.getWidth() / (float) this.canvasData.getHeight();
        float windowAspectRatio = this.width / (float) this.height;

        // Painting is wider than screen: we're limited by width
        if (paintingAspectRatio > windowAspectRatio) {
            paintingWidth = this.width - SCREEN_MARGIN * 2 - SCREEN_PADDING * 2;
            paintingHeight = (int) (paintingWidth / paintingAspectRatio);
        // Painting is taller than screen: we're limited by height
        } else {
            paintingHeight = this.height - SCREEN_MARGIN * 2 - SCREEN_PADDING * 2 - SCREEN_BOTTOM_TEXT;
            paintingWidth = (int) (paintingHeight * paintingAspectRatio);
        }

        this.paintingOffsetX = (this.width - paintingWidth) / 2;
        this.paintingOffsetY = (this.height - (paintingHeight + SCREEN_BOTTOM_TEXT)) / 2;

        this.paintingScale = (float) paintingWidth / this.canvasData.getWidth();

        int minWidth = 6 * 32;

        // If we can edit title, we need to fit title and a button, otherwise
        // we can use the size of the painting itself
        if (this.editable) {
            minWidth += BUTTON_WIDTH + 5;
        }

        this.screenWidth = Math.max(minWidth, paintingWidth) + SCREEN_PADDING * 2;
        this.screenHeight = paintingHeight + SCREEN_BOTTOM_TEXT + SCREEN_PADDING * 2;

        this.screenOffsetX = (this.width - screenWidth) / 2;
        this.screenOffsetY = (this.height - (paintingHeight + SCREEN_BOTTOM_TEXT + SCREEN_PADDING * 2)) / 2;
    }

    @Override
    public void tick() {
        this.tick++;
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
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.minecraft.player.closeContainer();
            return true;
        }

        if (this.editable) {
            return this.titleEdit.keyPressed(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.editable) {
            return this.titleEdit.charTyped(codePoint) || super.charTyped(codePoint, modifiers);
        }

        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(matrixStack);
        this.setFocused((IGuiEventListener)null);

        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        // Border
        fill(
            matrixStack,
            this.screenOffsetX,
            this.screenOffsetY,
            this.screenOffsetX + this.screenWidth,
            this.screenOffsetY + this.screenHeight,
            0xFFCAC3B4
        );

        fill(
            matrixStack,
            this.screenOffsetX + 1,
            this.screenOffsetY + 1,
            this.screenOffsetX + this.screenWidth - 1,
            this.screenOffsetY + this.screenHeight - 1,
            Helper.CANVAS_COLOR
        );

        //this.blit(matrixStack, , 2, 0, 0, 192, 192);

        matrixStack.pushPose();
        matrixStack.translate(this.paintingOffsetX, this.paintingOffsetY, 1.0F);
        matrixStack.scale(this.paintingScale, this.paintingScale, 1.0F);

        IRenderTypeBuffer.Impl renderTypeBufferImpl = IRenderTypeBuffer.immediate(Tessellator.getInstance().getBuilder());
        CanvasRenderer.getInstance().renderCanvas(matrixStack, renderTypeBufferImpl, this.canvasCode, this.canvasData, 0xF000F0);
        renderTypeBufferImpl.endBatch();

        matrixStack.popPose();

        String title = this.title.isEmpty() ? DEFAULT_TITLE.getString() : this.title;

        IReorderingProcessor formattedTitle = IReorderingProcessor.forward(title, this.title.isEmpty() ? Style.EMPTY.withColor(TextFormatting.GRAY) : Style.EMPTY.withColor(TextFormatting.BLACK));

        if (this.editable) {
            this.renderCursor(matrixStack, this.titleEdit.getCursorPos(), this.titleEdit.getCursorPos() == this.title.length());
        }

        this.font.draw(matrixStack, formattedTitle, (float) this.screenOffsetX + SCREEN_PADDING, (float) this.paintingOffsetY + paintingHeight + 7, TEXT_COLOR);
        this.font.draw(matrixStack, new TranslationTextComponent("book.byAuthor", this.authorName), (float) this.screenOffsetX + SCREEN_PADDING, (float) this.paintingOffsetY + paintingHeight + 17, TEXT_COLOR);

        super.render(matrixStack, mouseX, mouseY, partialTick);
    }

    private void renderCursor(MatrixStack poseStack, int cursorPos, boolean underscore) {
        if (this.tick / 6 % 2 == 0) {
            int cursorX = this.screenOffsetX + SCREEN_PADDING + this.font.width(this.title.substring(0, cursorPos));
            int cursorY = this.paintingOffsetY + paintingHeight + 7;

            if (!underscore) {
                fill(poseStack, cursorX, cursorY - 1, cursorX + 1, cursorY + 9, 0xFF000000);
            } else {
                this.font.draw(poseStack, "_", (float) cursorX, (float) cursorY, 0xFF000000);
            }
        }

    }

    private void setClipboard(String p_98148_) {
        if (this.minecraft != null) {
            TextInputUtil.setClipboardContents(this.minecraft, p_98148_);
        }

    }

    private String getClipboard() {
        return this.minecraft != null ? TextInputUtil.getClipboardContents(this.minecraft) : "";
    }
}
