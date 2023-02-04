package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.block.entity.ArtistTableBlockEntity;
import me.dantaeusb.zetter.deprecated.block.entity.EaselBlockEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ZetterBlockEntities
{
    private static final DeferredRegister<TileEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, Zetter.MOD_ID);

    /**
     * All easels should be converted to entities
     */
    @Deprecated
    public static TileEntityType<EaselBlockEntity> EASEL_BLOCK_ENTITY = BLOCK_ENTITIES.register("easel_tile_entity", () -> TileEntityType.Builder.of(EaselBlockEntity::new, ZetterBlocks.EASEL).build(null));
    public static TileEntityType<TileEntity> ARTIST_TABLE_BLOCK_ENTITY = BLOCK_ENTITIES.register("artist_table_tile_entity", () -> TileEntityType.Builder.of(ArtistTableBlockEntity::new, ZetterBlocks.ARTIST_TABLE).build(null));

    public static void init(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}