package me.dantaeusb.zetter.menu.painting.tools;

import me.dantaeusb.zetter.menu.EaselContainerMenu;
import me.dantaeusb.zetter.menu.painting.parameters.AbstractToolParameter;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.network.chat.TranslatableComponent;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.LinkedList;

public class Eyedropper extends AbstractTool {
    public static final String CODE = "eyedropper";

    public static final int HOTKEY = GLFW.GLFW_KEY_I;

    private final TranslatableComponent translatableComponent = new TranslatableComponent("container.zetter.painting.tools.eyedropper");

    public Eyedropper(EaselContainerMenu menu) {
        // Eyedropper has no parameters
        super(Eyedropper.CODE, menu, new LinkedList<>());
    }

    @Override
    public ToolShape getShape(HashMap<String, AbstractToolParameter> params) {
        return null;
    }

    @Override
    public TranslatableComponent getTranslatableComponent() {
        return this.translatableComponent;
    }

    @Override
    public int apply(CanvasData canvas, HashMap<String, AbstractToolParameter> params, int color, float posX, float posY) {
        int newColor = canvas.getColorAt((int) posX, (int) posY);

        this.menu.setPaletteColor(newColor);

        return 0;
    }

    /**
     * Eyedropper does not modify canvas hence there's
     * no need to add it to action stack
     * @return
     */
    public boolean publishable() {
        return false;
    }
}
