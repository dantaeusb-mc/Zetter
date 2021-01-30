package com.dantaeusb.immersivemp.locks.core;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.item.*;
import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = ImmersiveMp.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModLockItems
{
    private static final List<Item> ITEMS = new ArrayList<>();

    public static final KeyItem KEY_ITEM = (KeyItem) register("key", new KeyItem());
    public static final LockItem LOCK_ITEM = (LockItem) register("lock", new LockItem());
    public static final BlockItem LOCK_TABLE = registerBlockItem("lock_table", ModLockBlocks.LOCK_TABLE);
    public static final BlockItem EASEL = registerBlockItem("easel", ModLockBlocks.EASEL);
    public static final CanvasItem CANVAS_ITEM = (CanvasItem) register("canvas", new CanvasItem());
    public static final PaintsItem PAINTS_ITEM = (PaintsItem) register("paints", new PaintsItem());
    public static final PaletteItem PALETTE_ITEM = (PaletteItem) register("palette", new PaletteItem());

    public static final Item ACACIA_LOCKABLE_DOOR = registerLockableDoor("acacia_lockable_door", ModLockBlocks.ACACIA_LOCKABLE_DOOR);
    public static final Item BIRCH_LOCKABLE_DOOR = registerLockableDoor("birch_lockable_door", ModLockBlocks.BIRCH_LOCKABLE_DOOR);
    public static final Item DARK_OAK_LOCKABLE_DOOR = registerLockableDoor("dark_oak_lockable_door", ModLockBlocks.DARK_OAK_LOCKABLE_DOOR);
    public static final Item JUNGLE_LOCKABLE_DOOR = registerLockableDoor("jungle_lockable_door", ModLockBlocks.JUNGLE_LOCKABLE_DOOR);
    public static final Item OAK_LOCKABLE_DOOR = registerLockableDoor("oak_lockable_door", ModLockBlocks.OAK_LOCKABLE_DOOR);
    public static final Item SPRUCE_LOCKABLE_DOOR = registerLockableDoor("spruce_lockable_door", ModLockBlocks.SPRUCE_LOCKABLE_DOOR);

    public static final String LOCKABLE_DOOR_TAG_ID = "locks/doors";
    // @todo: [LOW] Tag probably have to be moved somewhere else
    public static ITag<Item> lockableDoorTag;

    private static Item registerLockableDoor(String name, Block block)
    {
        Item.Properties lockingDoorProps = new Item.Properties().maxStackSize(AbstractLockItem.MAX_STACK_SIZE).group(ItemGroup.TOOLS);
        LockableDoorItem doorItem = new LockableDoorItem(block, lockingDoorProps);

        return register(name, doorItem);
    }

    private static BlockItem registerBlockItem(String name, Block block)
    {
        Item.Properties itemProps = new Item.Properties().group(ItemGroup.TOOLS);
        BlockItem blockItem = new BlockItem(block, itemProps);

        return (BlockItem) register(name, blockItem);
    }

    private static Item register(String name, Item item)
    {
        item.setRegistryName(ImmersiveMp.MOD_ID, name);
        ITEMS.add(item);

        return item;
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void registerItems(final RegistryEvent.Register<Item> event)
    {
        ITEMS.forEach(item -> event.getRegistry().register(item));
        ITEMS.clear();

        lockableDoorTag = ItemTags.createOptional(new ResourceLocation(ImmersiveMp.MOD_ID, LOCKABLE_DOOR_TAG_ID));
    }
}