package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.entity.item.PaintingEntity;
import me.dantaeusb.zetter.item.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;

public class ZetterItems
{
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, Zetter.MOD_ID);

    public static final DeferredHolder<Item, EaselItem> EASEL = ITEMS.register("easel", () -> new EaselItem());
    public static final DeferredHolder<Item, BlockItem> ARTIST_TABLE = ITEMS.register("artist_table", () -> new BlockItem(ZetterBlocks.ARTIST_TABLE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, CanvasItem> CANVAS = ITEMS.register("canvas", () -> new CanvasItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, PaintingItem> PAINTING = ITEMS.register("painting", () -> new PaintingItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, PaintsItem> PAINTS = ITEMS.register("paints", () -> new PaintsItem());
    public static final DeferredHolder<Item, PaletteItem> PALETTE = ITEMS.register("palette", () -> new PaletteItem());

    public static final HashMap<String, DeferredHolder<Item, FrameItem>> FRAMES = new HashMap<>();

    public static final DeferredHolder<Item, FrameItem> ACACIA_FRAME = registerFrame("acacia_basic_frame", PaintingEntity.Materials.ACACIA, false);
    public static final DeferredHolder<Item, FrameItem> BIRCH_FRAME = registerFrame("birch_basic_frame", PaintingEntity.Materials.BIRCH, false);
    public static final DeferredHolder<Item, FrameItem> DARK_OAK_FRAME = registerFrame("dark_oak_basic_frame", PaintingEntity.Materials.DARK_OAK, false);
    public static final DeferredHolder<Item, FrameItem> JUNGLE_FRAME = registerFrame("jungle_basic_frame", PaintingEntity.Materials.JUNGLE, false);
    public static final DeferredHolder<Item, FrameItem> OAK_FRAME = registerFrame("oak_basic_frame", PaintingEntity.Materials.OAK, false);
    public static final DeferredHolder<Item, FrameItem> SPRUCE_FRAME = registerFrame("spruce_basic_frame", PaintingEntity.Materials.SPRUCE, false);
    public static final DeferredHolder<Item, FrameItem> MANGROVE_FRAME = registerFrame("mangrove_basic_frame", PaintingEntity.Materials.MANGROVE, false);
    public static final DeferredHolder<Item, FrameItem> CRIMSON_FRAME = registerFrame("crimson_basic_frame", PaintingEntity.Materials.CRIMSON, false);
    public static final DeferredHolder<Item, FrameItem> WARPED_FRAME = registerFrame("warped_basic_frame", PaintingEntity.Materials.WARPED, false);

    public static final DeferredHolder<Item, FrameItem> ACACIA_PLATED_FRAME = registerFrame("acacia_plated_frame", PaintingEntity.Materials.ACACIA, true);
    public static final DeferredHolder<Item, FrameItem> BIRCH_PLATED_FRAME = registerFrame("birch_plated_frame", PaintingEntity.Materials.BIRCH, true);
    public static final DeferredHolder<Item, FrameItem> DARK_OAK_PLATED_FRAME = registerFrame("dark_oak_plated_frame", PaintingEntity.Materials.DARK_OAK, true);
    public static final DeferredHolder<Item, FrameItem> JUNGLE_PLATED_FRAME = registerFrame("jungle_plated_frame", PaintingEntity.Materials.JUNGLE, true);
    public static final DeferredHolder<Item, FrameItem> OAK_PLATED_FRAME = registerFrame("oak_plated_frame", PaintingEntity.Materials.OAK, true);
    public static final DeferredHolder<Item, FrameItem> SPRUCE_PLATED_FRAME = registerFrame("spruce_plated_frame", PaintingEntity.Materials.SPRUCE, true);
    public static final DeferredHolder<Item, FrameItem> MANGROVE_PLATED_FRAME = registerFrame("mangrove_plated_frame", PaintingEntity.Materials.MANGROVE, true);
    public static final DeferredHolder<Item, FrameItem> CRIMSON_PLATED_FRAME = registerFrame("crimson_plated_frame", PaintingEntity.Materials.CRIMSON, true);
    public static final DeferredHolder<Item, FrameItem> WARPED_PLATED_FRAME = registerFrame("warped_plated_frame", PaintingEntity.Materials.WARPED, true);

    public static final DeferredHolder<Item, FrameItem> IRON_FRAME = registerFrame("iron_frame", PaintingEntity.Materials.IRON, false);

    public static final DeferredHolder<Item, FrameItem> GOLD_FRAME = registerFrame("gold_basic_frame", PaintingEntity.Materials.GOLD, false);
    public static final DeferredHolder<Item, FrameItem> GOLD_PLATE_FRAME = registerFrame("gold_plated_frame", PaintingEntity.Materials.GOLD, true);

    private static DeferredHolder<Item, FrameItem> registerFrame(String name, PaintingEntity.Materials material, boolean plated)
    {
        DeferredHolder<Item, FrameItem> frameRegistryObject = ITEMS.register(name, () -> new FrameItem(new Item.Properties().stacksTo(64), material, plated));
        FRAMES.put(Helper.getFrameKey(material, plated), frameRegistryObject);

        return frameRegistryObject;
    }

    public static void init(IEventBus bus) {
        ITEMS.register(bus);
    }
}