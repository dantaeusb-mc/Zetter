package me.dantaeusb.zetter.event;

import me.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class CanvasViewEvent extends Event {
    public final PlayerEntity player;
    public final String canvasCode;
    public final AbstractCanvasData canvasData;
    public final Hand hand;

    public CanvasViewEvent(PlayerEntity player, String canvasCode, AbstractCanvasData canvasData, Hand hand) {
        this.player = player;
        this.canvasCode = canvasCode;
        this.canvasData = canvasData;
        this.hand = hand;
    }
}
