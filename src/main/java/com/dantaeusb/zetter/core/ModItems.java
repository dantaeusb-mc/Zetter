package com.dantaeusb.zetter.core;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.entity.item.CustomPaintingEntity;
import com.dantaeusb.zetter.item.*;
import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModItems
{
    private static final List<Item> ITEMS = new ArrayList<>();

    public static final BlockItem EASEL = registerBlockItem("easel", ModBlocks.EASEL);
    public static final BlockItem ARTIST = registerBlockItem("artist_table", ModBlocks.ARTIST_TABLE);
    public static final CanvasItem CANVAS = (CanvasItem) register("canvas", new CanvasItem());
    public static final PaintingItem PAINTING = (PaintingItem) register("painting", new PaintingItem());
    public static final PaintsItem PAINTS = (PaintsItem) register("paints", new PaintsItem());
    public static final PaletteItem PALETTE = (PaletteItem) register("palette", new PaletteItem());

    public static final HashMap<String, Item> FRAMES = new HashMap<>();

    public static final FrameItem ACACIA_FRAME = registerFrame("acacia_basic_frame", new FrameItem(CustomPaintingEntity.Materials.ACACIA, false));
    public static final FrameItem BIRCH_FRAME = registerFrame("birch_basic_frame", new FrameItem(CustomPaintingEntity.Materials.BIRCH, false));
    public static final FrameItem DARK_OAK_FRAME = registerFrame("dark_oak_basic_frame", new FrameItem(CustomPaintingEntity.Materials.DARK_OAK, false));
    public static final FrameItem JUNGLE_FRAME = registerFrame("jungle_basic_frame", new FrameItem(CustomPaintingEntity.Materials.JUNGLE, false));
    public static final FrameItem OAK_FRAME = registerFrame("oak_basic_frame", new FrameItem(CustomPaintingEntity.Materials.OAK, false));
    public static final FrameItem SPRUCE_FRAME = registerFrame("spruce_basic_frame", new FrameItem(CustomPaintingEntity.Materials.SPRUCE, false));
    public static final FrameItem CRIMSON_FRAME = registerFrame("crimson_basic_frame", new FrameItem(CustomPaintingEntity.Materials.CRIMSON, false));
    public static final FrameItem WARPED_FRAME = registerFrame("warped_basic_frame", new FrameItem(CustomPaintingEntity.Materials.WARPED, false));

    public static final FrameItem ACACIA_PLATED_FRAME = registerFrame("acacia_plated_frame", new FrameItem(CustomPaintingEntity.Materials.ACACIA, true));
    public static final FrameItem BIRCH_PLATED_FRAME = registerFrame("birch_plated_frame", new FrameItem(CustomPaintingEntity.Materials.BIRCH, true));
    public static final FrameItem DARK_OAK_PLATED_FRAME = registerFrame("dark_oak_plated_frame", new FrameItem(CustomPaintingEntity.Materials.DARK_OAK, true));
    public static final FrameItem JUNGLE_PLATED_FRAME = registerFrame("jungle_plated_frame", new FrameItem(CustomPaintingEntity.Materials.JUNGLE, true));
    public static final FrameItem OAK_PLATED_FRAME = registerFrame("oak_plated_frame", new FrameItem(CustomPaintingEntity.Materials.OAK, true));
    public static final FrameItem SPRUCE_PLATED_FRAME = registerFrame("spruce_plated_frame", new FrameItem(CustomPaintingEntity.Materials.SPRUCE, true));
    public static final FrameItem CRIMSON_PLATED_FRAME = registerFrame("crimson_plated_frame", new FrameItem(CustomPaintingEntity.Materials.CRIMSON, true));
    public static final FrameItem WARPED_PLATED_FRAME = registerFrame("warped_plated_frame", new FrameItem(CustomPaintingEntity.Materials.WARPED, true));

    private static BlockItem registerBlockItem(String name, Block block)
    {
        Item.Properties itemProps = new Item.Properties().group(ItemGroup.TOOLS);
        BlockItem blockItem = new BlockItem(block, itemProps);

        return (BlockItem) register(name, blockItem);
    }

    private static FrameItem registerFrame(String name, FrameItem item)
    {
        item.setRegistryName(Zetter.MOD_ID, name);
        ITEMS.add(item);

        FRAMES.put(Helper.getFrameKey(item.getMaterial(), item.hasPlate()), item);
        ItemTags.bind

        return item;
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