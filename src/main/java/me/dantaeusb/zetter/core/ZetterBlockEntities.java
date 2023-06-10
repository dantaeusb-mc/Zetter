package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.block.entity.ArtistTableBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ZetterBlockEntities
{
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Zetter.MOD_ID);

    /**
     * All easels should be converted to entities
     */
    @Deprecated
    public static RegistryObject<BlockEntityType<ArtistTableBlockEntity>> ARTIST_TABLE_BLOCK_ENTITY = BLOCK_ENTITIES.register("artist_table_tile_entity", () -> BlockEntityType.Builder.of(ArtistTableBlockEntity::new, ZetterBlocks.ARTIST_TABLE.get()).build(null));

    public static void init(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}