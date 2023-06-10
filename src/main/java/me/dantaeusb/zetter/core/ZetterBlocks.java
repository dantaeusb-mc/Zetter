package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.block.ArtistTableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ZetterBlocks
{
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Zetter.MOD_ID);

    public static final RegistryObject<ArtistTableBlock> ARTIST_TABLE = BLOCKS.register("artist_table", () -> new ArtistTableBlock(BlockBehaviour.Properties.of().strength(2.5F).mapColor(MapColor.WOOD).sound(SoundType.WOOD).ignitedByLava()));
    public static void init(IEventBus bus) {
        BLOCKS.register(bus);
    }
}