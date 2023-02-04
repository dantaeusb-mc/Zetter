package me.dantaeusb.zetter.event;

import me.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.world.World;
import net.minecraftforge.eventbus.api.Event;

public abstract class CanvasUnregisterEvent extends Event {
    public final String canvasCode;
    public final AbstractCanvasData canvasData;
    public final World level;
    public final long timestamp;

    public CanvasUnregisterEvent(String canvasCode, AbstractCanvasData canvasData, World level, long timestamp) {
        this.canvasCode = canvasCode;
        this.canvasData = canvasData;
        this.level = level;
        this.timestamp = timestamp;
    }

    public static class Pre extends CanvasUnregisterEvent {
        public Pre(String canvasCode, AbstractCanvasData canvasData, World level, long timestamp) {
            super(canvasCode, canvasData, level, timestamp);
        }
    }

    public static class Post extends CanvasUnregisterEvent {
        public Post(String canvasCode, AbstractCanvasData canvasData, World level, long timestamp) {
            super(canvasCode, canvasData, level, timestamp);
        }
    }
}
