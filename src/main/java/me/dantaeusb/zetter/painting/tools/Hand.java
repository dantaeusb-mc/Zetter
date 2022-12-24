package me.dantaeusb.zetter.painting.tools;

import me.dantaeusb.zetter.painting.parameters.NoParameters;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

public class Hand extends AbstractTool<NoParameters> {
    public static final int HOTKEY = GLFW.GLFW_KEY_H;
    public static final int QUICK_TOOL_KEY = GLFW.GLFW_KEY_SPACE;

    private final Component translatableComponent = Component.translatable("container.zetter.painting.tools.hand");

    private final ToolShape shape = new ToolShape();

    public Hand() {
        // Eyedropper has no parameters
        super(new ArrayList<>());
    }

    @Override
    public ToolShape getShape(NoParameters params) {
        return this.shape;
    }

    @Override
    public Component getTranslatableComponent() {
        return this.translatableComponent;
    }

    @Override
    public int useTool(CanvasData canvas, NoParameters params, int color, float posX, float posY) {
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
