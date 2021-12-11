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
     * All easels should be converted to entities
     */
    @Deprecated
    public static BlockEntityType<EaselBlockEntity> EASEL_BLOCK_ENTITY;
    public static BlockEntityType<ArtistTableBlockEntity> ARTIST_TABLE_BLOCK_ENTITY;

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onTileEntityTypeRegistration(final RegistryEvent.Register<BlockEntityType<?>> event) {
        // Do not change names, it'll break compatibility
        EASEL_BLOCK_ENTITY =
                BlockEntityType.Builder.of(EaselBlockEntity::new, ModBlocks.EASEL).build(null);
        EASEL_BLOCK_ENTITY.setRegistryName(Zetter.MOD_ID, "easel_tile_entity");
        event.getRegistry().register(EASEL_BLOCK_ENTITY);

        ARTIST_TABLE_BLOCK_ENTITY =
                BlockEntityType.Builder.of(ArtistTableBlockEntity::new, ModBlocks.ARTIST_TABLE).build(null);
        ARTIST_TABLE_BLOCK_ENTITY.setRegistryName(Zetter.MOD_ID, "artist_table_tile_entity");
        event.getRegistry().register(ARTIST_TABLE_BLOCK_ENTITY);
    }
}