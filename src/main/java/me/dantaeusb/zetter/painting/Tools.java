package me.dantaeusb.zetter.painting;

import me.dantaeusb.zetter.painting.tools.*;
public enum Tools {
    PENCIL(new Pencil()),
    BRUSH(new Brush()),
    EYEDROPPER(new Eyedropper()),
    BUCKET(new Bucket()),
    HAND(new Hand());

    private final AbstractTool tool;

    Tools(AbstractTool tool) {
        this.tool = tool;
    }

    public AbstractTool getTool() {
        return this.tool;
    }
}
