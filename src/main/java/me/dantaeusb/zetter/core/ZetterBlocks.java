package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.block.ArtistTableBlock;
import me.dantaeusb.zetter.deprecated.block.EaselBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraftforge.eventbus.api.IEventBus;

import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ZetterBlocks
{
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Zetter.MOD_ID);

    // Easel block is deprecated, but kept for backward compatibility
    public static final RegistryObject<EaselBlock> EASEL = BLOCKS.register("easel", () -> new EaselBlock(AbstractBlock.Properties.of(Material.WOOD).strength(1.5F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<ArtistTableBlock> ARTIST_TABLE = BLOCKS.register("artist_table", () -> new ArtistTableBlock(AbstractBlock.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));

    public static void init(IEventBus bus) {
        BLOCKS.register(bus);
    }
}