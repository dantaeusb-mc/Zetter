package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.entity.item.CustomPaintingEntity;
import me.dantaeusb.zetter.item.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ZetterItems
{
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Zetter.MOD_ID);

    public static final RegistryObject<EaselItem> EASEL = ITEMS.register("easel", () -> new EaselItem());
    public static final RegistryObject<BlockItem> ARTIST = ITEMS.register("artist_table", () -> new BlockItem(ZetterBlocks.ARTIST_TABLE.get(), new Item.Properties().tab(CreativeModeTab.TAB_TOOLS)));
    public static final RegistryObject<CanvasItem> CANVAS = ITEMS.register("canvas", () -> new CanvasItem());
    public static final RegistryObject<PaintingItem> PAINTING = ITEMS.register("painting", () -> new PaintingItem());
    public static final RegistryObject<PaintsItem> PAINTS = ITEMS.register("paints", () -> new PaintsItem());
    public static final RegistryObject<PaletteItem> PALETTE = ITEMS.register("palette", () -> new PaletteItem());

    public static final HashMap<String, RegistryObject<FrameItem>> FRAMES = new HashMap<>();

    public static final RegistryObject<FrameItem> ACACIA_FRAME = ITEMS.register("acacia_basic_frame", () -> new FrameItem(CustomPaintingEntity.Materials.ACACIA, false));
    public static final RegistryObject<FrameItem> BIRCH_FRAME = ITEMS.register("birch_basic_frame", () -> new FrameItem(CustomPaintingEntity.Materials.BIRCH, false));
    public static final RegistryObject<FrameItem> DARK_OAK_FRAME = ITEMS.register("dark_oak_basic_frame", () -> new FrameItem(CustomPaintingEntity.Materials.DARK_OAK, false));
    public static final RegistryObject<FrameItem> JUNGLE_FRAME = ITEMS.register("jungle_basic_frame", () -> new FrameItem(CustomPaintingEntity.Materials.JUNGLE, false));
    public static final RegistryObject<FrameItem> OAK_FRAME = ITEMS.register("oak_basic_frame", () -> new FrameItem(CustomPaintingEntity.Materials.OAK, false));
    public static final RegistryObject<FrameItem> SPRUCE_FRAME = ITEMS.register("spruce_basic_frame", () -> new FrameItem(CustomPaintingEntity.Materials.SPRUCE, false));
    public static final RegistryObject<FrameItem> CRIMSON_FRAME = ITEMS.register("crimson_basic_frame", () -> new FrameItem(CustomPaintingEntity.Materials.CRIMSON, false));
    public static final RegistryObject<FrameItem> WARPED_FRAME = ITEMS.register("warped_basic_frame", () -> new FrameItem(CustomPaintingEntity.Materials.WARPED, false));

    public static final RegistryObject<FrameItem> ACACIA_PLATED_FRAME = registerFrame("acacia_plated_frame", CustomPaintingEntity.Materials.ACACIA, true);
    public static final RegistryObject<FrameItem> BIRCH_PLATED_FRAME = registerFrame("birch_plated_frame", CustomPaintingEntity.Materials.BIRCH, true);
    public static final RegistryObject<FrameItem> DARK_OAK_PLATED_FRAME = registerFrame("dark_oak_plated_frame", CustomPaintingEntity.Materials.DARK_OAK, true);
    public static final RegistryObject<FrameItem> JUNGLE_PLATED_FRAME = registerFrame("jungle_plated_frame", CustomPaintingEntity.Materials.JUNGLE, true);
    public static final RegistryObject<FrameItem> OAK_PLATED_FRAME = registerFrame("oak_plated_frame", CustomPaintingEntity.Materials.OAK, true);
    public static final RegistryObject<FrameItem> SPRUCE_PLATED_FRAME = registerFrame("spruce_plated_frame", CustomPaintingEntity.Materials.SPRUCE, true);
    public static final RegistryObject<FrameItem> CRIMSON_PLATED_FRAME = registerFrame("crimson_plated_frame", CustomPaintingEntity.Materials.CRIMSON, true);
    public static final RegistryObject<FrameItem> WARPED_PLATED_FRAME = registerFrame("warped_plated_frame", CustomPaintingEntity.Materials.WARPED, true);

    public static final RegistryObject<FrameItem> IRON_FRAME = registerFrame("iron_frame", CustomPaintingEntity.Materials.IRON, false);

    public static final RegistryObject<FrameItem> GOLD_FRAME = registerFrame("gold_basic_frame", CustomPaintingEntity.Materials.GOLD, false);
    public static final RegistryObject<FrameItem> GOLD_PLATE_FRAME = registerFrame("gold_plated_frame", CustomPaintingEntity.Materials.GOLD, true);

    private static RegistryObject<FrameItem> registerFrame(String name, CustomPaintingEntity.Materials material, boolean plated)
    {
        RegistryObject<FrameItem> frameRegistryObject = ITEMS.register(name, () -> new FrameItem(material, plated));
        FRAMES.put(Helper.getFrameKey(material, plated), frameRegistryObject);

        return frameRegistryObject;
    }

    public static void init(IEventBus bus) {
        ITEMS.register(bus);
    }
}