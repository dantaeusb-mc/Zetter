package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.entity.item.CustomPaintingEntity;
import me.dantaeusb.zetter.item.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.HashMap;

import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ZetterItems
{
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Zetter.MOD_ID);

    public static final RegistryObject<EaselItem> EASEL = ITEMS.register("easel", () -> new EaselItem());
    public static final RegistryObject<BlockItem> ARTIST = ITEMS.register("artist_table", () -> new BlockItem(ZetterBlocks.ARTIST_TABLE.get(), new Item.Properties().tab(CreativeModeTab.TAB_TOOLS)));
    public static final RegistryObject<CanvasItem> CANVAS = ITEMS.register("canvas", () -> new CanvasItem(new Item.Properties().stacksTo(1).tab(CreativeModeTab.TAB_TOOLS)));
    public static final RegistryObject<PaintingItem> PAINTING = ITEMS.register("painting", () -> new PaintingItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<PaintsItem> PAINTS = ITEMS.register("paints", () -> new PaintsItem());
    public static final RegistryObject<PaletteItem> PALETTE = ITEMS.register("palette", () -> new PaletteItem());

    public static final HashMap<String, RegistryObject<FrameItem>> FRAMES = new HashMap<>();

    public static final RegistryObject<FrameItem> ACACIA_FRAME = registerFrame("acacia_basic_frame", CustomPaintingEntity.Materials.ACACIA, false);
    public static final RegistryObject<FrameItem> BIRCH_FRAME = registerFrame("birch_basic_frame", CustomPaintingEntity.Materials.BIRCH, false);
    public static final RegistryObject<FrameItem> DARK_OAK_FRAME = registerFrame("dark_oak_basic_frame", CustomPaintingEntity.Materials.DARK_OAK, false);
    public static final RegistryObject<FrameItem> JUNGLE_FRAME = registerFrame("jungle_basic_frame", CustomPaintingEntity.Materials.JUNGLE, false);
    public static final RegistryObject<FrameItem> OAK_FRAME = registerFrame("oak_basic_frame", CustomPaintingEntity.Materials.OAK, false);
    public static final RegistryObject<FrameItem> SPRUCE_FRAME = registerFrame("spruce_basic_frame", CustomPaintingEntity.Materials.SPRUCE, false);
    public static final RegistryObject<FrameItem> CRIMSON_FRAME = registerFrame("crimson_basic_frame", CustomPaintingEntity.Materials.CRIMSON, false);
    public static final RegistryObject<FrameItem> WARPED_FRAME = registerFrame("warped_basic_frame", CustomPaintingEntity.Materials.WARPED, false);

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
        RegistryObject<FrameItem> frameRegistryObject = ITEMS.register(name, () -> new FrameItem(new Item.Properties().stacksTo(64).tab(CreativeModeTab.TAB_TOOLS), material, plated));
        FRAMES.put(Helper.getFrameKey(material, plated), frameRegistryObject);

        return frameRegistryObject;
    }

    public static void init(IEventBus bus) {
        ITEMS.register(bus);
    }
}