package me.dantaeusb.zetter.client.gui.easel;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zetter.client.gui.EaselScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TabsWidget extends AbstractPaintingWidget implements Widget {
    private List<TabButton> tabs;
    final static int TAB_BUTTON_WIDTH = 28;
    final static int TAB_BUTTON_HEIGHT = 23;
    final static int TAB_BUTTON_OFFSET = TAB_BUTTON_HEIGHT + 3;

    final static int WIDTH = TAB_BUTTON_WIDTH;
    final static int HEIGHT = TAB_BUTTON_HEIGHT + (TAB_BUTTON_OFFSET) * 2;
    public TabsWidget(EaselScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, WIDTH, HEIGHT, new TranslatableComponent("container.zetter.painting.tabs"));

        this.updateTabs();
    }

    public void updateTabs() {
        final int TAB_BUTTONS_U = 200;
        final int TAB_BUTTON_COLOR_V = 0;
        final int TAB_BUTTON_PENCIL_V = 23;
        final int TAB_BUTTON_BRUSH_V = 46;
        final int TAB_BUTTON_BUCKET_V = 69;
        final int TAB_BUTTON_INVENTORY_V = 92;

        final ArrayList<TabButton> tabs = new ArrayList<>();

        tabs.add(new TabButton(Tab.COLOR, TAB_BUTTONS_U, TAB_BUTTON_COLOR_V, TAB_BUTTON_WIDTH, TAB_BUTTON_HEIGHT));

        switch (this.parentScreen.getMenu().getCurrentTool()) {
            case PENCIL:
                tabs.add(new TabButton(Tab.PENCIL_PARAMETERS, TAB_BUTTONS_U, TAB_BUTTON_PENCIL_V, TAB_BUTTON_WIDTH, TAB_BUTTON_HEIGHT));
                break;
            case BRUSH:
                tabs.add(new TabButton(Tab.BRUSH_PARAMETERS, TAB_BUTTONS_U, TAB_BUTTON_BRUSH_V, TAB_BUTTON_WIDTH, TAB_BUTTON_HEIGHT));
                break;
            case BUCKET:
                tabs.add(new TabButton(Tab.BUCKET_PARAMETERS, TAB_BUTTONS_U, TAB_BUTTON_BUCKET_V, TAB_BUTTON_WIDTH, TAB_BUTTON_HEIGHT));
                break;
        }

        tabs.add(new TabButton(Tab.INVENTORY, TAB_BUTTONS_U, TAB_BUTTON_INVENTORY_V, TAB_BUTTON_WIDTH, TAB_BUTTON_HEIGHT));

        this.tabs = tabs;
    }

    @Override
    public @Nullable
    Component getTooltip(int mouseX, int mouseY) {
        int i = 0;
        for (TabButton tab: this.tabs) {
            int fromY = this.y + i * TAB_BUTTON_OFFSET;

            if (EaselScreen.isInRect(this.x, fromY, TAB_BUTTON_WIDTH, TAB_BUTTON_HEIGHT, mouseX, mouseY)) {
                return tab.getTooltip();
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
        for (TabButton tabButton: this.tabs) {
            int fromY = this.y + i * TAB_BUTTON_OFFSET;

            if (EaselScreen.isInRect(this.x, fromY, tabButton.width, tabButton.height, iMouseX, iMouseY) && this.isValidClickButton(button)) {
                this.parentScreen.getMenu().setCurrentTab(tabButton.tab);
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
        for (TabButton tab: this.tabs) {
            int fromY = this.y + i * TAB_BUTTON_OFFSET;
            int uOffset = tab.uPosition + (this.parentScreen.getMenu().getCurrentTab() == tab.tab ? TAB_BUTTON_WIDTH : 0);

            this.blit(matrixStack, this.x, fromY, uOffset, tab.vPosition, tab.width, tab.height);
            i++;
        }
    }

    public class TabButton {
        private final Tab tab;
        public final int uPosition;
        public final int vPosition;
        public final int height;
        public final int width;

        TabButton(Tab tab, int uPosition, int vPosition, int width, int height) {
            this.tab = tab;
            this.uPosition = uPosition;
            this.vPosition = vPosition;
            this.height = height;
            this.width = width;
        }

        public Component getTooltip() {
            return this.tab.translatableComponent;
        }

        public String getTabCode() {
            return this.tab.code;
        }
    }

    public enum Tab {
        COLOR("color", new TranslatableComponent("container.zetter.painting.tabs.color")),
        PENCIL_PARAMETERS("pencil_parameters", new TranslatableComponent("container.zetter.painting.tabs.pencil_parameters")),
        BRUSH_PARAMETERS("brush_parameters", new TranslatableComponent("container.zetter.painting.tabs.brush_parameters")),
        BUCKET_PARAMETERS("bucket_parameters", new TranslatableComponent("container.zetter.painting.tabs.bucket_parameters")),
        INVENTORY("inventory", new TranslatableComponent("container.zetter.painting.tabs.inventory"));

        public final String code;

        // @todo: [LOW] Remove?
        public final Component translatableComponent;

        Tab(String code, Component translatableComponent) {
            this.code = code;
            this.translatableComponent = translatableComponent;
        }
    }
}
