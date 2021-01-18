package com.dantaeusb.immersivemp.locks.core;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.block.LockableDoorBlock;
import com.dantaeusb.immersivemp.locks.network.packet.CLockDoorOpen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.tileentity.LockableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * @todo: This doesn't work either. Any priority doesn't do shit.
 * Quark handles event anyway, and opening next door.
 */
public class ModLockGameEvents {
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent.RightClickBlock event) {
        if(event.getHand() == Hand.OFF_HAND || event.getPlayer().isDiscrete() || event.isCanceled() || event.getResult() == Event.Result.DENY || event.getUseBlock() == Event.Result.DENY) {
            return;
        }

        World world = event.getWorld();
        BlockPos pos = event.getPos();

        if(world.getBlockState(pos).getBlock() instanceof LockableDoorBlock) {
            // Send message on client, do nothing on server
            if (event.getWorld().isRemote) {
                CLockDoorOpen doorPacket = new CLockDoorOpen(pos);
                ModLockNetwork.simpleChannel.sendToServer(doorPacket);
            }

            // Consume event on both to prevent Quark open
            event.setCanceled(true);
            event.setResult(Event.Result.DEFAULT);
        }
    }
}
