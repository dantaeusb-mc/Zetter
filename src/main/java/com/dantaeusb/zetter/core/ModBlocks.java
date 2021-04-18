package com.dantaeusb.zetter.core;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.block.ArtistTableBlock;
import com.dantaeusb.zetter.block.EaselBlock;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModBlocks
{
    public static final List<Block> BLOCK_ITEMS = new ArrayList<>();

    private static final List<Block> BLOCKS = new ArrayList<>();

    public static final Block ARTIST_TABLE = registerBlockItem("artist_table", new ArtistTableBlock(AbstractBlock.Properties.create(Material.WOOD).hardnessAndResistance(2.5F).sound(SoundType.WOOD)));

    public static final Block EASEL = registerBlockItem("easel", new EaselBlock(AbstractBlock.Properties.create(Material.WOOD).hardnessAndResistance(1.5F).sound(SoundType.WOOD).notSolid()));

    private static Block registerBlockItem(String name, Block block)
    {
        Block blockItem = register(name, block);
        BLOCK_ITEMS.add(blockItem);

        return blockItem;
    }

    private static Block register(String name, Block block)
    {
        block.setRegistryName(Zetter.MOD_ID, name);
        BLOCKS.add(block);

        return block;
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void registerBlocks(final RegistryEvent.Register<Block> event)
    {
        BLOCKS.forEach(block -> event.getRegistry().register(block));
        BLOCKS.clear();
    }
}