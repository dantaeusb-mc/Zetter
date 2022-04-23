package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.block.ArtistTableBlock;
import me.dantaeusb.zetter.deprecated.block.EaselBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ZetterBlocks
{
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Zetter.MOD_ID);

    // Easel block is deprecated, but kept for backward compatibility
    public static final RegistryObject<EaselBlock> EASEL = BLOCKS.register("easel", () -> new EaselBlock(BlockBehaviour.Properties.of(Material.WOOD).strength(1.5F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<ArtistTableBlock> ARTIST_TABLE = BLOCKS.register("artist_table", () -> new ArtistTableBlock(BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD)));

    public static void init(IEventBus bus) {
        BLOCKS.register(bus);
    }
}