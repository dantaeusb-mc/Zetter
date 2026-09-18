package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.block.entity.ArtistTableBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ZetterBlockEntities
{
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Zetter.MOD_ID);

    /**
     * All easels should be converted to entities
     */
    @Deprecated
    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<ArtistTableBlockEntity>> ARTIST_TABLE_BLOCK_ENTITY = BLOCK_ENTITIES.register("artist_table_tile_entity", () -> BlockEntityType.Builder.of(ArtistTableBlockEntity::new, ZetterBlocks.ARTIST_TABLE.get()).build(null));

    public static void init(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}