package me.dantaeusb.zetter.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.Tesselator;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterNetwork;
import me.dantaeusb.zetter.network.packet.CSignPaintingPacket;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.chat.ITextComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.Hand;
import net.minecraft.world.entity.player.PlayerEntity;
import org.lwjgl.glfw.GLFW;

public class PaintingScreen extends Screen {
    private static final ITextComponent DEFAULT_TITLE = new TranslationTextComponent("item.zetter.painting.unnamed");

    private static final FormattedCharSequence BLACK_CURSOR = FormattedCharSequence.forward("_", Style.EMPTY.withColor(TextFormatting.BLACK));
    private static final FormattedCharSequence GRAY_CURSOR = FormattedCharSequence.forward("_", Style.EMPTY.withColor(TextFormatting.GRAY));

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

    private final TextFieldHelper titleEdit = new TextFieldHelper(() -> {
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

        this.signButton = this.addRenderableWidget(Button.builder(new TranslationTextComponent("book.signButton"), (p_98177_) -> {
            this.signPainting();
        }).bounds(
            this.screenOffsetX + this.screenWidth - BUTTON_WIDTH - SCREEN_PADDING,
            this.paintingOffsetY + this.paintingHeight + SCREEN_PADDING,
            BUTTON_WIDTH,
            BUTTON_HEIGHT
        ).build());

        this.signButton.visible = this.editable;
    }

    private void signPainting() {
        int slot = this.hand == Hand.MAIN_HAND ? this.owner.getInventory().selected : 40;

        CSignPaintingPacket signPaintingPacket = new CSignPaintingPacket(slot, this.title);
        ZetterNetwork.simpleChannel.sendToServer(signPaintingPacket);

        this.minecraft.player.closeContainer();
    }

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
        this.setFocused((GuiEventListener)null);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
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

        MultiBufferSource.BufferSource renderTypeBufferImpl = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        CanvasRenderer.getInstance().renderCanvas(matrixStack, renderTypeBufferImpl, this.canvasCode, this.canvasData, 0xF000F0);
        renderTypeBufferImpl.endBatch();

        matrixStack.popPose();

        String title = this.title.isEmpty() ? DEFAULT_TITLE.getString() : this.title;
        FormattedCharSequence formattedTitle = FormattedCharSequence.forward(title, Style.EMPTY);

        if (this.editable) {
            boolean cursorTick = this.tick / 6 % 2 == 0;
            formattedTitle = FormattedCharSequence.composite(formattedTitle, cursorTick ? BLACK_CURSOR : GRAY_CURSOR);
        }

        this.font.draw(matrixStack, formattedTitle, (float) this.screenOffsetX + SCREEN_PADDING, (float) this.paintingOffsetY + paintingHeight + 7, TEXT_COLOR);
        this.font.draw(matrixStack, new TranslationTextComponent("book.byAuthor", this.authorName), (float) this.screenOffsetX + SCREEN_PADDING, (float) this.paintingOffsetY + paintingHeight + 17, TEXT_COLOR);

        super.render(matrixStack, mouseX, mouseY, partialTick);
    }

    private void setClipboard(String p_98148_) {
        if (this.minecraft != null) {
            TextFieldHelper.setClipboardContents(this.minecraft, p_98148_);
        }

    }

    private String getClipboard() {
        return this.minecraft != null ? TextFieldHelper.getClipboardContents(this.minecraft) : "";
    }
}
