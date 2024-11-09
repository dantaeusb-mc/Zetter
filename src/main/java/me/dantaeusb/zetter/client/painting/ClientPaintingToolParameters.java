package me.dantaeusb.zetter.client.painting;

import me.dantaeusb.zetter.painting.Tool;
import me.dantaeusb.zetter.painting.parameters.*;

import java.util.HashMap;

public class ClientPaintingToolParameters {
    private static ClientPaintingToolParameters instance;

    private final HashMap<Tool, AbstractToolParameters> toolParameters = new HashMap<>(){{
        put(Tool.PENCIL, new PencilParameters());
        put(Tool.BRUSH, new BrushParameters());
        put(Tool.EYEDROPPER, new NoParameters());
        put(Tool.BUCKET, new BucketParameters());
    }};

    public AbstractToolParameters getToolParameters(Tool tool) {
        return this.toolParameters.get(tool);
    }

    public ClientPaintingToolParameters() {
        instance = this;
    }

    public static ClientPaintingToolParameters getInstance() {
        return instance;
    }
}
