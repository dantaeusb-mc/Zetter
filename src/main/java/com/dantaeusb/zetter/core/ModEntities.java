package com.dantaeusb.zetter.core;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.client.renderer.tileentity.CustomPaintingTileEntityRenderer;
import com.dantaeusb.zetter.tileentity.EaselTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModTileEntities
{
    public static TileEntityType<EaselTileEntity> EASEL_TILE_ENTITY;

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onTileEntityTypeRegistration(final RegistryEvent.Register<TileEntityType<?>> event) {
        EASEL_TILE_ENTITY =
                TileEntityType.Builder.create(EaselTileEntity::new, ModBlocks.EASEL).build(null);
        EASEL_TILE_ENTITY.setRegistryName(Zetter.MOD_ID, "easel_tile_entity");
        event.getRegistry().register(EASEL_TILE_ENTITY);
    }
}