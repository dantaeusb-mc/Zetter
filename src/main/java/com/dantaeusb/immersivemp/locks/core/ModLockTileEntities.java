package com.dantaeusb.immersivemp.locks.core;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.tileentity.EaselTileEntity;
import com.dantaeusb.immersivemp.locks.tileentity.KeyLockableTileEntity;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ImmersiveMp.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModLockTileEntities
{
    public static TileEntityType<KeyLockableTileEntity> LOCKING_TILE_ENTITY;
    public static TileEntityType<EaselTileEntity> EASEL_TILE_ENTITY;

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onTileEntityTypeRegistration(final RegistryEvent.Register<TileEntityType<?>> event) {
        Block[] lockableDoorBlockArray = ModLockBlocks.LOCKABLE_DOORS.toArray(new Block[ModLockBlocks.LOCKABLE_DOORS.size()]);

        LOCKING_TILE_ENTITY =
                TileEntityType.Builder.create(KeyLockableTileEntity::new, lockableDoorBlockArray).build(null);
        LOCKING_TILE_ENTITY.setRegistryName(ImmersiveMp.MOD_ID, "lockable_tile_entity");
        event.getRegistry().register(LOCKING_TILE_ENTITY);

        EASEL_TILE_ENTITY =
                TileEntityType.Builder.create(EaselTileEntity::new, ModLockBlocks.EASEL).build(null);
        EASEL_TILE_ENTITY.setRegistryName(ImmersiveMp.MOD_ID, "easel_tile_entity");
        event.getRegistry().register(EASEL_TILE_ENTITY);
    }
}