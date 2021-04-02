package com.dantaeusb.zetter.core;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.item.*;
import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModItems
{
    private static final List<Item> ITEMS = new ArrayList<>();

    public static final BlockItem EASEL = registerBlockItem("easel", ModBlocks.EASEL);
    public static final CanvasItem CANVAS_ITEM = (CanvasItem) register("canvas", new CanvasItem());
    public static final PaintsItem PAINTS_ITEM = (PaintsItem) register("paints", new PaintsItem());
    public static final PaletteItem PALETTE_ITEM = (PaletteItem) register("palette", new PaletteItem());

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

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void registerItems(final RegistryEvent.Register<Item> event)
    {
        ITEMS.forEach(item -> event.getRegistry().register(item));
        ITEMS.clear();
    }
}