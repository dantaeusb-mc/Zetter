package me.dantaeusb.zetter.event;

import me.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Event;

public abstract class CanvasRegisterEvent extends Event {
    public final String canvasCode;
    public final AbstractCanvasData canvasData;
    public final Level level;
    public final long timestamp;

    public CanvasRegisterEvent(String canvasCode, AbstractCanvasData canvasData, Level level, long timestamp) {
        this.canvasCode = canvasCode;
        this.canvasData = canvasData;
        this.level = level;
        this.timestamp = timestamp;
    }

    public static class Pre extends CanvasRegisterEvent {
        public Pre(String canvasCode, AbstractCanvasData canvasData, Level level, long timestamp) {
            super(canvasCode, canvasData, level, timestamp);
        }
    }

    public static class Post extends CanvasRegisterEvent {
        public Post(String canvasCode, AbstractCanvasData canvasData, Level level, long timestamp) {
            super(canvasCode, canvasData, level, timestamp);
        }
    }
}
