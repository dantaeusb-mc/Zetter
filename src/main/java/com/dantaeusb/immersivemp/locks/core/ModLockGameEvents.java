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
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ModLockGameEvents {
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPlayerInteract(PlayerInteractEvent.RightClickBlock event) {
        if(!event.getWorld().isRemote || event.getPlayer().isDiscrete() || event.isCanceled() || event.getResult() == Result.DENY || event.getUseBlock() == Result.DENY) {
            return;
        }

        World world = event.getWorld();
        BlockPos pos = event.getPos();

        // Only when we're interacting with our doors
        if(world.getBlockState(pos).getBlock() instanceof LockableDoorBlock) {
            // Cancel both hands interactions, cause Quark utilizes main hand
            event.setCanceled(true);
            event.setResult(Result.DENY);

            ImmersiveMp.LOG.info("Sending from hand " + event.getHand().name());

            // We, however, will consume & ignore event on main hand and proceed with OFF_HAND
            if (event.getHand() == Hand.MAIN_HAND) {
                return;
            }

            CLockDoorOpen doorPacket = new CLockDoorOpen(pos);
            ModLockNetwork.simpleChannel.sendToServer(doorPacket);

        }
    }
}
