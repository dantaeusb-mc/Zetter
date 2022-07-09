package me.dantaeusb.zetter.painting.tools;

import me.dantaeusb.zetter.menu.EaselContainerMenu;
import me.dantaeusb.zetter.painting.parameters.NoParameters;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.network.chat.TranslatableComponent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

public class Eyedropper extends AbstractTool<NoParameters> {
    public static final String CODE = "eyedropper";

    public static final int HOTKEY = GLFW.GLFW_KEY_I;

    private final TranslatableComponent translatableComponent = new TranslatableComponent("container.zetter.painting.tools.eyedropper");

    private final ToolShape shape = new ToolShape();

    public Eyedropper() {
        // Eyedropper has no parameters
        super(new ArrayList<>());
    }

    @Override
    public ToolShape getShape(NoParameters params) {
        return this.shape;
    }

    @Override
    public TranslatableComponent getTranslatableComponent() {
        return this.translatableComponent;
    }

    @Override
    public int apply(CanvasData canvas, NoParameters params, int color, float posX, float posY) {
        int newColor = canvas.getColorAt((int) posX, (int) posY);

        //this.menu.setPaletteColor(newColor);

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
