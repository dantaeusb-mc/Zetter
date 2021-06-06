package com.dantaeusb.zetter.core;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.entity.item.CustomPaintingEntity;
import com.dantaeusb.zetter.item.*;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModItems
{
    private static final List<Item> ITEMS = new ArrayList<>();

    public static final BlockItem EASEL = registerBlockItem("easel", ModBlocks.EASEL);
    public static final BlockItem ARTIST = registerBlockItem("artist_table", ModBlocks.ARTIST_TABLE);
    public static final CanvasItem CANVAS = (CanvasItem) register("canvas", new CanvasItem());
    public static final PaintsItem PAINTS = (PaintsItem) register("paints", new PaintsItem());
    public static final PaletteItem PALETTE = (PaletteItem) register("palette", new PaletteItem());

    public static final HashMap<String, Item> PAINTINGS = new HashMap<>();

    public static final CustomPaintingItem ACACIA_PAINTING = registerPainting("acacia_custom_painting", new CustomPaintingItem(CustomPaintingEntity.Materials.ACACIA));
    public static final CustomPaintingItem BIRCH_PAINTING = registerPainting("birch_custom_painting", new CustomPaintingItem(CustomPaintingEntity.Materials.BIRCH));
    public static final CustomPaintingItem DARK_OAK_PAINTING = registerPainting("dark_oak_custom_painting", new CustomPaintingItem(CustomPaintingEntity.Materials.DARK_OAK));
    public static final CustomPaintingItem JUNGLE_PAINTING = registerPainting("jungle_custom_painting", new CustomPaintingItem(CustomPaintingEntity.Materials.JUNGLE));
    public static final CustomPaintingItem OAK_PAINTING = registerPainting("oak_custom_painting", new CustomPaintingItem(CustomPaintingEntity.Materials.OAK));
    public static final CustomPaintingItem SPRUCE_PAINTING = registerPainting("spruce_custom_painting", new CustomPaintingItem(CustomPaintingEntity.Materials.SPRUCE));
    public static final CustomPaintingItem CRIMSON_PAINTING = registerPainting("crimson_custom_painting", new CustomPaintingItem(CustomPaintingEntity.Materials.CRIMSON));
    public static final CustomPaintingItem WARPED_PAINTING = registerPainting("warped_custom_painting", new CustomPaintingItem(CustomPaintingEntity.Materials.WARPED));

    private static BlockItem registerBlockItem(String name, Block block)
    {
        Item.Properties itemProps = new Item.Properties().group(ItemGroup.TOOLS);
        BlockItem blockItem = new BlockItem(block, itemProps);

        return (BlockItem) register(name, blockItem);
    }

    private static Item register(String name, Item item)
    {
        item.setRegistryName(Zetter.MOD_ID, name);
        ITEMS.add(item);

        return item;
    }

    private static CustomPaintingItem registerPainting(String name, CustomPaintingItem item)
    {
        item.setRegistryName(Zetter.MOD_ID, name);
        ITEMS.add(item);
        PAINTINGS.put(item.getMaterial().toString(), item);

        return item;
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void registerItems(final RegistryEvent.Register<Item> event)
    {
        ITEMS.forEach(item -> event.getRegistry().register(item));
        ITEMS.clear();
    }
}