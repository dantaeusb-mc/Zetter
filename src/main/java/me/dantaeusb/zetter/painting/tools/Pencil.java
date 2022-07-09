package me.dantaeusb.zetter.painting.tools;

import me.dantaeusb.zetter.menu.EaselContainerMenu;
import me.dantaeusb.zetter.painting.parameters.PencilParameters;
import me.dantaeusb.zetter.painting.pipes.BlendingPipe;
import me.dantaeusb.zetter.painting.pipes.DitheringPipe;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.network.chat.TranslatableComponent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

public class Pencil extends AbstractTool<PencilParameters> {
    public static final String CODE = "pencil";

    public static final int HOTKEY = GLFW.GLFW_KEY_P;

    private final TranslatableComponent translatableComponent = new TranslatableComponent("container.zetter.painting.tools.pencil");

    public Pencil() {
        super(new ArrayList<>() {{
            add(new DitheringPipe());
            add(new BlendingPipe());
        }});
    }

    @Override
    public ToolShape getShape(PencilParameters params) {
        return null;
    }

    @Override
    public TranslatableComponent getTranslatableComponent() {
        return this.translatableComponent;
    }

    @Override
    public int apply(CanvasData canvas, PencilParameters params, int color, float posX, float posY) {
        final int width = canvas.getWidth();
        final int height = canvas.getHeight();

        final int index = (int) (Math.floor(posY) * width + Math.floor(posX));

        this.pixelChange(canvas, params, color, index, 1f);

        // Only if changed
        return 1;
    }
}
