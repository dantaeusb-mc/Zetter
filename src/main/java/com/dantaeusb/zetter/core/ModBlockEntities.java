package com.dantaeusb.zetter.core;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.block.entity.ArtistTableBlockEntity;
import com.dantaeusb.zetter.deprecated.block.entity.EaselBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModBlockEntities
{
    /**
     * All easels should be converted to tile entities
     */
    @Deprecated
    public static BlockEntityType<EaselBlockEntity> EASEL_TILE_ENTITY;
    public static BlockEntityType<ArtistTableBlockEntity> ARTIST_TABLE_TILE_ENTITY;

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onTileEntityTypeRegistration(final RegistryEvent.Register<BlockEntityType<?>> event) {
        EASEL_TILE_ENTITY =
                BlockEntityType.Builder.of(EaselBlockEntity::new, ModBlocks.EASEL).build(null);
        EASEL_TILE_ENTITY.setRegistryName(Zetter.MOD_ID, "easel_tile_entity");
        event.getRegistry().register(EASEL_TILE_ENTITY);

        ARTIST_TABLE_TILE_ENTITY =
                BlockEntityType.Builder.of(ArtistTableBlockEntity::new, ModBlocks.ARTIST_TABLE).build(null);
        ARTIST_TABLE_TILE_ENTITY.setRegistryName(Zetter.MOD_ID, "artist_table_tile_entity");
        event.getRegistry().register(ARTIST_TABLE_TILE_ENTITY);
    }
}