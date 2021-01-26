package com.dantaeusb.immersivemp.locks.core;

import com.dantaeusb.immersivemp.ImmersiveMp;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;

/**
 * @todo: This doesn't work. No idea why.
 * Using `MinecraftForge.EVENT_BUS.register(new ModLockIMCEvents());` will subscribe handler
 * but it won't do anything. Timing is the issue, probably
 */
@Mod.EventBusSubscriber(modid = ImmersiveMp.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModLockIMCEvents {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void serverLoad(FMLServerStartingEvent event)
    {
        ImmersiveMp.LOG.info("Sending IMCs to CarryOn");

        InterModComms.sendTo("carryon", "blacklistBlock", ModLockBlocks.ACACIA_LOCKABLE_DOOR::getRegistryName);
        InterModComms.sendTo("carryon", "blacklistBlock", ModLockBlocks.BIRCH_LOCKABLE_DOOR::getRegistryName);
        InterModComms.sendTo("carryon", "blacklistBlock", ModLockBlocks.DARK_OAK_LOCKABLE_DOOR::getRegistryName);
        InterModComms.sendTo("carryon", "blacklistBlock", ModLockBlocks.JUNGLE_LOCKABLE_DOOR::getRegistryName);
        InterModComms.sendTo("carryon", "blacklistBlock", ModLockBlocks.OAK_LOCKABLE_DOOR::getRegistryName);
        InterModComms.sendTo("carryon", "blacklistBlock", ModLockBlocks.SPRUCE_LOCKABLE_DOOR::getRegistryName);
        InterModComms.sendTo("carryon", "blacklistEntity", ModLockTileEntities.LOCKING_TILE_ENTITY::getRegistryName);
    }
}
