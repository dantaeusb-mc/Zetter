package me.dantaeusb.zetter.painting.tools;

import me.dantaeusb.zetter.menu.EaselContainerMenu;
import me.dantaeusb.zetter.painting.parameters.PencilParameters;
import me.dantaeusb.zetter.painting.pipes.BlendingPipe;
import me.dantaeusb.zetter.painting.pipes.DitheringPipe;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Tuple;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;

public class Pencil extends AbstractTool<PencilParameters> {
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
        return this.shapes.get(Math.round(params.getSize()));
    }

    @Override
    public TranslatableComponent getTranslatableComponent() {
        return this.translatableComponent;
    }

    @Override
    public int useTool(CanvasData canvas, PencilParameters params, int color, float posX, float posY) {
        final int width = canvas.getWidth();
        final int height = canvas.getHeight();

        final int index = (int) (Math.floor(posY) * width + Math.floor(posX));

        this.pixelChange(canvas, params, color, index, 1f);

        // Only if changed
        return 1;
    }

    private HashMap<Integer, ToolShape> shapes = new HashMap<>() {{
        put(1, new ToolShape());
        put(2, new ToolShape(new ArrayList<Tuple<Integer, Integer>>() {{
            add(new Tuple<>(-1, -1));
            add(new Tuple<>(1, -1));
            add(new Tuple<>(1, 1));
            add(new Tuple<>(-1, 1));
        }}));
        put(3, new ToolShape(new ArrayList<>() {{
            add(new Tuple<>(0, 0));
            add(new Tuple<>(0, -1));
            add(new Tuple<>(1, -1));
            add(new Tuple<>(1, 0));
            add(new Tuple<>(2, 0));
            add(new Tuple<>(2, 1));
            add(new Tuple<>(1, 1));
            add(new Tuple<>(1, 2));
            add(new Tuple<>(0, 2));
            add(new Tuple<>(0, 1));
            add(new Tuple<>(-1, 1));
            add(new Tuple<>(-1, 0));
        }}));
        put(4, new ToolShape(new ArrayList<Tuple<Integer, Integer>>() {{
            add(new Tuple<>(-1, -1));
            add(new Tuple<>(2, -1));
            add(new Tuple<>(2, 2));
            add(new Tuple<>(-1, 2));
        }}));
        put(5, new ToolShape(new ArrayList<Tuple<Integer, Integer>>() {{
            add(new Tuple<>(-1, -1));
            add(new Tuple<>(-1, -2));
            add(new Tuple<>(2, -2));
            add(new Tuple<>(2, -1));
            add(new Tuple<>(3, -1));
            add(new Tuple<>(3, 2));
            add(new Tuple<>(2, 2));
            add(new Tuple<>(2, 3));
            add(new Tuple<>(-1, 3));
            add(new Tuple<>(-1, 2));
            add(new Tuple<>(-2, 2));
            add(new Tuple<>(-2, -1));
        }}));
        put(6, new ToolShape(new ArrayList<Tuple<Integer, Integer>>() {{
            add(new Tuple<>(0, 0));
            add(new Tuple<>(0, 1));
            add(new Tuple<>(1, 1));
            add(new Tuple<>(1, 0));
        }}));
    }};
}
