package me.dantaeusb.zetter.event;

import me.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class CanvasViewEvent extends Event {
    public final Player player;
    public final String canvasCode;
    public final AbstractCanvasData canvasData;
    public final InteractionHand hand;

    public CanvasViewEvent(Player player, String canvasCode, AbstractCanvasData canvasData, InteractionHand hand) {
        this.player = player;
        this.canvasCode = canvasCode;
        this.canvasData = canvasData;
        this.hand = hand;
    }
}
