package me.dantaeusb.zetter.menu.painting.tools;

import me.dantaeusb.zetter.menu.EaselContainerMenu;
import me.dantaeusb.zetter.menu.painting.parameters.AbstractToolParameter;
import me.dantaeusb.zetter.menu.painting.pipes.BlendingPipe;
import me.dantaeusb.zetter.menu.painting.pipes.DitheringPipe;
import me.dantaeusb.zetter.menu.painting.pipes.Pipe;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.network.chat.TranslatableComponent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;

public class Pencil extends AbstractTool {
    public static final String CODE = "pencil";

    public static final int HOTKEY = GLFW.GLFW_KEY_P;

    private final TranslatableComponent translatableComponent = new TranslatableComponent("container.zetter.painting.tools.pencil");

    public Pencil(EaselContainerMenu menu) {
        super(Pencil.CODE, menu, new ArrayList<>() {{
            add(new DitheringPipe());
            add(new BlendingPipe());
        }});
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
        final int width = canvas.getWidth();
        final int height = canvas.getHeight();

        final int index = (int) (Math.floor(posY) * width + Math.floor(posX));

        this.pixelChange(canvas, params, color, index);

        // Only if changed
        return 1;
    }
}
