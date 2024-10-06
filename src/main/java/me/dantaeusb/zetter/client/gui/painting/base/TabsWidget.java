package me.dantaeusb.zetter.client.gui.painting.base;

import me.dantaeusb.zetter.client.gui.EaselScreen;
import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.AbstractPaintingWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class TabsWidget extends AbstractPaintingWidget implements Renderable {
  final static int WIDTH = 164;
  final static int HEIGHT = 13;

  private final Tab[] tabs;
  private final Consumer<String> tabConsumer;
  private String activeTabCode;

  public TabsWidget(PaintingScreen parentScreen, int x, int y, Component translatableComponent, Consumer<String> tabConsumer, Tab[] tabs, String defaultTabCode) {
    super(parentScreen, x, y, WIDTH, HEIGHT, translatableComponent);

    this.tabs = tabs;
    this.activeTabCode = defaultTabCode;
    this.tabConsumer = tabConsumer;
  }

  public TabsWidget(PaintingScreen parentScreen, int x, int y, Component translatableComponent, Consumer<String> tabConsumer, Tab[] tabs) {
    this(parentScreen, x, y, translatableComponent, tabConsumer, tabs, tabs[0].code);
  }

  public String getActiveTabCode() {
    return this.activeTabCode;
  }

  @Override
  public void onClick(double mouseX, double mouseY) {
    int totalWidth = 5;

    for (Tab tab: this.tabs) {
      int fromX = this.getX() + totalWidth;

      if (EaselScreen.isInRect(this.getX(), fromX, tab.width, tab.height, (int)mouseX, (int)mouseY)) {
        this.activeTabCode = tab.code;
        this.tabConsumer.accept(tab.code);
        this.playDownSound(Minecraft.getInstance().getSoundManager());
        return;
      }

      totalWidth += tab.width;
    }
  }

  @Override
  protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
    final int TAB_U = 90;
    final int TAB_V = 151;

    final int PADDING_H = 4;
    final int PADDING_V = 3;

    final int SLICE = 2;

    guiGraphics.blit(PAINTING_WIDGETS_TEXTURE_RESOURCE, this.getX(), this.getY(), this.getWidth(), this.getHeight(), TAB_U, TAB_V, 1, 13, 256, 256);

    int totalWidth = 5;

    for (Tab tab : this.tabs) {
      int length = this.parentScreen.getFont().width(tab.title);

      if (tab.code.equals(this.activeTabCode)) {
        guiGraphics.blitNineSliced(PAINTING_WIDGETS_TEXTURE_RESOURCE, this.getX() + totalWidth, this.getY(), length + PADDING_H * 2, 13, SLICE, SLICE, 5, 13, TAB_U + 1, TAB_V);
      } else {
        guiGraphics.blitNineSliced(PAINTING_WIDGETS_TEXTURE_RESOURCE, this.getX() + totalWidth, this.getY(), length + PADDING_H * 2, 13, SLICE, SLICE, 5, 13, TAB_U + 6, TAB_V);
      }

      guiGraphics.drawString(this.parentScreen.getFont(), tab.title, this.getX() + totalWidth + PADDING_H, this.getY() + PADDING_V, 0xFF404040, false);

      totalWidth += length + PADDING_H * 2 - 1;
    }
  }

  public static class Tab {
    public final String code;
    public final Component title;
    public final int height = 13;
    public final int width;

    public Tab(String code, Component title, Font font) {
      this.code = code;
      this.title = title;
      this.width = font.width(title);
    }
  }
}
