package me.dantaeusb.zetter.client.painting;

import me.dantaeusb.zetter.painting.Tools;
import me.dantaeusb.zetter.painting.parameters.*;

import java.util.HashMap;

public class ClientPaintingToolParameters {
    private static ClientPaintingToolParameters instance;

    private final HashMap<Tools, AbstractToolParameters> toolParameters = new HashMap<>(){{
        put(Tools.PENCIL, new PencilParameters());
        put(Tools.BRUSH, new BrushParameters());
        put(Tools.EYEDROPPER, new NoParameters());
        put(Tools.BUCKET, new BucketParameters());
    }};

    public AbstractToolParameters getToolParameters(Tools tool) {
        return this.toolParameters.get(tool);
    }

    public ClientPaintingToolParameters() {
        instance = this;
    }

    public static ClientPaintingToolParameters getInstance() {
        return instance;
    }
}
