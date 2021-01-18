package com.dantaeusb.immersivemp.locks.core;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.block.LockTableBlock;
import com.dantaeusb.immersivemp.locks.block.LockableDoorBlock;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.block.material.PushReaction;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = ImmersiveMp.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModLockBlocks
{
    public static final Material WOOD = new Material(MaterialColor.WOOD, false, false, false, false, true, true, PushReaction.NORMAL);
    public static final Material STONE = new Material(MaterialColor.STONE, false, false, false, false, false, true, PushReaction.NORMAL);
    public static final Material WOOL = new Material(MaterialColor.WOOL, false, false, false, false, false, true, PushReaction.NORMAL);

    public static final List<Block> BLOCK_ITEMS = new ArrayList<>();
    public static final List<Block> LOCKABLE_DOORS = new ArrayList<>();

    private static final List<Block> BLOCKS = new ArrayList<>();

    public static final Block LOCK_TABLE = registerBlockItem("lock_table", new LockTableBlock(Block.Properties.create(STONE).hardnessAndResistance(0.5F).sound(SoundType.STONE)));

    public static final Block ACACIA_LOCKABLE_DOOR = registerLockableDoor("acacia_lockable_door", new LockableDoorBlock(AbstractBlock.Properties.create(Material.WOOD, Blocks.ACACIA_PLANKS.getMaterialColor()).hardnessAndResistance(3.0F).sound(SoundType.WOOD).notSolid()));
    public static final Block BIRCH_LOCKABLE_DOOR = registerLockableDoor("birch_lockable_door", new LockableDoorBlock(AbstractBlock.Properties.create(Material.WOOD, Blocks.BIRCH_PLANKS.getMaterialColor()).hardnessAndResistance(3.0F).sound(SoundType.WOOD).notSolid()));
    public static final Block DARK_OAK_LOCKABLE_DOOR = registerLockableDoor("dark_oak_lockable_door", new LockableDoorBlock(AbstractBlock.Properties.create(Material.WOOD, Blocks.DARK_OAK_PLANKS.getMaterialColor()).hardnessAndResistance(3.0F).sound(SoundType.WOOD).notSolid()));
    public static final Block JUNGLE_LOCKABLE_DOOR = registerLockableDoor("jungle_lockable_door", new LockableDoorBlock(AbstractBlock.Properties.create(Material.WOOD, Blocks.JUNGLE_PLANKS.getMaterialColor()).hardnessAndResistance(3.0F).sound(SoundType.WOOD).notSolid()));
    public static final Block OAK_LOCKABLE_DOOR = registerLockableDoor("oak_lockable_door", new LockableDoorBlock(AbstractBlock.Properties.create(Material.WOOD, Blocks.OAK_PLANKS.getMaterialColor()).hardnessAndResistance(3.0F).sound(SoundType.WOOD).notSolid()));
    public static final Block SPRUCE_LOCKABLE_DOOR = registerLockableDoor("spruce_lockable_door", new LockableDoorBlock(AbstractBlock.Properties.create(Material.WOOD, Blocks.SPRUCE_PLANKS.getMaterialColor()).hardnessAndResistance(3.0F).sound(SoundType.WOOD).notSolid()));

    private static Block registerLockableDoor(String name, Block block)
    {
        Block lockableDoor = register(name, block);
        LOCKABLE_DOORS.add(lockableDoor);

        return lockableDoor;
    }

    private static Block registerBlockItem(String name, Block block)
    {
        Block blockItem = register(name, block);
        BLOCK_ITEMS.add(blockItem);

        return blockItem;
    }

    private static Block register(String name, Block block)
    {
        block.setRegistryName(ImmersiveMp.MOD_ID, name);
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