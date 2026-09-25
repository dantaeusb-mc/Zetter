package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.entity.item.BlackboardEntity;
import me.dantaeusb.zetter.entity.item.PaintingEntity;
import me.dantaeusb.zetter.item.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;

public class ZetterItems
{
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Zetter.MOD_ID);

    public static final RegistryObject<EaselItem> EASEL = ITEMS.register("easel", () -> new EaselItem());
    public static final RegistryObject<WallEaselItem> WALL_EASEL = ITEMS.register("wall_easel", () -> new WallEaselItem());
    public static final RegistryObject<BlockItem> ARTIST_TABLE = ITEMS.register("artist_table", () -> new BlockItem(ZetterBlocks.ARTIST_TABLE.get(), new Item.Properties()));
    public static final RegistryObject<CanvasItem> CANVAS = ITEMS.register("canvas", () -> new CanvasItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<PaintingItem> PAINTING = ITEMS.register("painting", () -> new PaintingItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<PaintsItem> PAINTS = ITEMS.register("paints", () -> new PaintsItem());
    public static final RegistryObject<PaletteItem> PALETTE = ITEMS.register("palette", () -> new PaletteItem());
    public static final RegistryObject<SpongeItem> SMALL_SPONGE = ITEMS.register("small_sponge", () -> new SpongeItem(new Item.Properties()));
    public static final RegistryObject<ChalkBoxItem> CHALK_BOX = ITEMS.register("chalk_box", () -> new ChalkBoxItem(new Item.Properties()));

    public static final HashMap<String, RegistryObject<FrameItem>> FRAMES = new HashMap<>();

    public static final RegistryObject<FrameItem> ACACIA_FRAME = registerFrame("acacia_basic_frame", PaintingEntity.Materials.ACACIA, false);
    public static final RegistryObject<FrameItem> BIRCH_FRAME = registerFrame("birch_basic_frame", PaintingEntity.Materials.BIRCH, false);
    public static final RegistryObject<FrameItem> DARK_OAK_FRAME = registerFrame("dark_oak_basic_frame", PaintingEntity.Materials.DARK_OAK, false);
    public static final RegistryObject<FrameItem> JUNGLE_FRAME = registerFrame("jungle_basic_frame", PaintingEntity.Materials.JUNGLE, false);
    public static final RegistryObject<FrameItem> OAK_FRAME = registerFrame("oak_basic_frame", PaintingEntity.Materials.OAK, false);
    public static final RegistryObject<FrameItem> SPRUCE_FRAME = registerFrame("spruce_basic_frame", PaintingEntity.Materials.SPRUCE, false);
    public static final RegistryObject<FrameItem> MANGROVE_FRAME = registerFrame("mangrove_basic_frame", PaintingEntity.Materials.MANGROVE, false);
    public static final RegistryObject<FrameItem> CRIMSON_FRAME = registerFrame("crimson_basic_frame", PaintingEntity.Materials.CRIMSON, false);
    public static final RegistryObject<FrameItem> WARPED_FRAME = registerFrame("warped_basic_frame", PaintingEntity.Materials.WARPED, false);

    public static final RegistryObject<FrameItem> ACACIA_PLATED_FRAME = registerFrame("acacia_plated_frame", PaintingEntity.Materials.ACACIA, true);
    public static final RegistryObject<FrameItem> BIRCH_PLATED_FRAME = registerFrame("birch_plated_frame", PaintingEntity.Materials.BIRCH, true);
    public static final RegistryObject<FrameItem> DARK_OAK_PLATED_FRAME = registerFrame("dark_oak_plated_frame", PaintingEntity.Materials.DARK_OAK, true);
    public static final RegistryObject<FrameItem> JUNGLE_PLATED_FRAME = registerFrame("jungle_plated_frame", PaintingEntity.Materials.JUNGLE, true);
    public static final RegistryObject<FrameItem> OAK_PLATED_FRAME = registerFrame("oak_plated_frame", PaintingEntity.Materials.OAK, true);
    public static final RegistryObject<FrameItem> SPRUCE_PLATED_FRAME = registerFrame("spruce_plated_frame", PaintingEntity.Materials.SPRUCE, true);
    public static final RegistryObject<FrameItem> MANGROVE_PLATED_FRAME = registerFrame("mangrove_plated_frame", PaintingEntity.Materials.MANGROVE, true);
    public static final RegistryObject<FrameItem> CRIMSON_PLATED_FRAME = registerFrame("crimson_plated_frame", PaintingEntity.Materials.CRIMSON, true);
    public static final RegistryObject<FrameItem> WARPED_PLATED_FRAME = registerFrame("warped_plated_frame", PaintingEntity.Materials.WARPED, true);

    public static final RegistryObject<FrameItem> IRON_FRAME = registerFrame("iron_frame", PaintingEntity.Materials.IRON, false);

    public static final RegistryObject<FrameItem> GOLD_FRAME = registerFrame("gold_basic_frame", PaintingEntity.Materials.GOLD, false);
    public static final RegistryObject<FrameItem> GOLD_PLATE_FRAME = registerFrame("gold_plated_frame", PaintingEntity.Materials.GOLD, true);

    public static final HashMap<String, RegistryObject<BlackboardItem>> BLACKBOARDS = new HashMap<>();
    public static final HashMap<DyeColor, RegistryObject<ChalkItem>> CHALKS = new HashMap<>();

    static {
        for (BlackboardEntity.Materials material : BlackboardEntity.Materials.values()) {
            BLACKBOARDS.put(
                material.toString(),
                ITEMS.register(material + "_blackboard", () -> new BlackboardItem(new Item.Properties(), material))
            );
        }

        for (DyeColor color : DyeColor.values()) {
            CHALKS.put(
                color,
                ITEMS.register(color.getName() + "_chalk", () -> new ChalkItem(new Item.Properties(), color))
            );
        }
    }

    private static RegistryObject<FrameItem> registerFrame(String name, PaintingEntity.Materials material, boolean plated)
    {
        RegistryObject<FrameItem> frameRegistryObject = ITEMS.register(name, () -> new FrameItem(new Item.Properties().stacksTo(64), material, plated));
        FRAMES.put(Helper.getFrameKey(material, plated), frameRegistryObject);

        return frameRegistryObject;
    }

    public static void init(IEventBus bus) {
        ITEMS.register(bus);
    }
}