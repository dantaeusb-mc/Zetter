package me.dantaeusb.zetter.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A box of chalk: somewhere to keep sticks without spending a slot on each one.
 *
 * Chalk cannot stack — nothing with durability can — so sixteen colors would
 * otherwise cost sixteen slots forever. The box holds them instead, and they come
 * out one at a time to be drawn with. It is not itself a drawing tool: there is
 * always a stick in your hand when you are writing on a board.
 *
 * Built on the bundle's shape rather than on {@link net.minecraft.world.item.BundleItem}.
 * A bundle measures what it holds by weight, at 64 / maxStackSize per item, so an
 * unstackable stick weighs the whole bundle and exactly one would fit. All of that
 * arithmetic is private, so only the storage format and the tooltip are borrowed.
 */
public class ChalkBoxItem extends Item {
    /**
     * Sticks it holds. Chalk never stacks, so this is as much a number of slots as a
     * count, and one per dye color is a coincidence rather than a promise.
     */
    public static final int CAPACITY = 16;

    private static final String NBT_TAG_CHALK = "Chalk";

    /**
     * Chalk rather than the bundle's blue, since that is what the bar is counting
     */
    private static final int BAR_COLOR = 0xDADADA;

    public ChalkBoxItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /*
     * Contents
     */

    private static ListTag getChalkTag(ItemStack boxStack) {
        final CompoundTag tag = boxStack.getTag();

        return tag == null ? new ListTag() : tag.getList(NBT_TAG_CHALK, Tag.TAG_COMPOUND);
    }

    public static Stream<ItemStack> getContents(ItemStack boxStack) {
        return getChalkTag(boxStack).stream().map(CompoundTag.class::cast).map(ItemStack::of);
    }

    public static int getCount(ItemStack boxStack) {
        return getChalkTag(boxStack).size();
    }

    /**
     * Drives the model, so an empty box looks like one
     *
     * @param boxStack
     * @return
     */
    public static float getFullness(ItemStack boxStack) {
        return (float) getCount(boxStack) / CAPACITY;
    }

    /**
     * Takes one stick out of the given stack and puts it in the box.
     *
     * Two sticks are never merged into one entry the way a bundle merges matching
     * items: the box is sixteen slots, and two pieces of chalk worn to exactly the
     * same point are still two pieces of chalk.
     *
     * @param boxStack
     * @param chalkStack drawn from, and left one shorter
     * @return whether anything was taken
     */
    public static boolean add(ItemStack boxStack, ItemStack chalkStack) {
        if (chalkStack.isEmpty() || !(chalkStack.getItem() instanceof ChalkItem)) {
            return false;
        }

        final CompoundTag tag = boxStack.getOrCreateTag();
        final ListTag chalk = tag.getList(NBT_TAG_CHALK, Tag.TAG_COMPOUND);

        if (chalk.size() >= CAPACITY) {
            return false;
        }

        final CompoundTag stored = new CompoundTag();
        chalkStack.split(1).save(stored);

        // Newest to the front, so taking one back out returns what was just put in
        chalk.add(0, stored);
        tag.put(NBT_TAG_CHALK, chalk);

        return true;
    }

    private static Optional<ItemStack> removeOne(ItemStack boxStack) {
        final CompoundTag tag = boxStack.getTag();

        if (tag == null || !tag.contains(NBT_TAG_CHALK)) {
            return Optional.empty();
        }

        final ListTag chalk = tag.getList(NBT_TAG_CHALK, Tag.TAG_COMPOUND);

        if (chalk.isEmpty()) {
            return Optional.empty();
        }

        final ItemStack chalkStack = ItemStack.of(chalk.getCompound(0));
        chalk.remove(0);

        // An empty box carries no tag at all, so empty boxes stack with each other
        if (chalk.isEmpty()) {
            boxStack.removeTagKey(NBT_TAG_CHALK);
        }

        return Optional.of(chalkStack);
    }

    /*
     * Taking chalk out and putting it back
     */

    /**
     * Right clicking pulls one stick out into the inventory. No crouch: the box is
     * held to get chalk out of it and for nothing else, and needing a modifier to do
     * the only thing it does would be a tax on every use.
     *
     * @param level
     * @param player
     * @param hand
     * @return
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        final ItemStack boxStack = player.getItemInHand(hand);

        if (getCount(boxStack) == 0) {
            return InteractionResultHolder.fail(boxStack);
        }

        if (!level.isClientSide()) {
            removeOne(boxStack).ifPresent(chalkStack -> player.getInventory().placeItemBackInInventory(chalkStack));
        }

        playRemoveOneSound(player);
        player.awardStat(Stats.ITEM_USED.get(this));

        return InteractionResultHolder.sidedSuccess(boxStack, level.isClientSide());
    }

    /**
     * Box on the cursor, right clicked onto a slot: an empty slot takes a stick out
     * of the box, and a stick in the slot goes into it.
     *
     * @param boxStack
     * @param slot
     * @param action
     * @param player
     * @return
     */
    @Override
    public boolean overrideStackedOnOther(ItemStack boxStack, Slot slot, ClickAction action, Player player) {
        if (boxStack.getCount() != 1 || action != ClickAction.SECONDARY) {
            return false;
        }

        final ItemStack slotStack = slot.getItem();

        if (slotStack.isEmpty()) {
            removeOne(boxStack).ifPresent(chalkStack -> {
                playRemoveOneSound(player);

                // Whatever the slot would not take goes back where it came from
                add(boxStack, slot.safeInsert(chalkStack));
            });

            return true;
        }

        if (!(slotStack.getItem() instanceof ChalkItem) || getCount(boxStack) >= CAPACITY) {
            // Anything else, and the click means what it usually means
            return false;
        }

        if (add(boxStack, slot.safeTake(1, 1, player))) {
            playInsertSound(player);
        }

        return true;
    }

    /**
     * Box sitting in a slot, right clicked with something on the cursor: nothing on
     * the cursor takes a stick out onto it, a stick goes in.
     *
     * @param boxStack
     * @param otherStack
     * @param slot
     * @param action
     * @param player
     * @param access
     * @return
     */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack boxStack, ItemStack otherStack, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (boxStack.getCount() != 1 || action != ClickAction.SECONDARY || !slot.allowModification(player)) {
            return false;
        }

        if (otherStack.isEmpty()) {
            removeOne(boxStack).ifPresent(chalkStack -> {
                playRemoveOneSound(player);
                access.set(chalkStack);
            });

            return true;
        }

        if (!add(boxStack, otherStack)) {
            return false;
        }

        playInsertSound(player);

        return true;
    }

    /*
     * Presentation
     */

    @Override
    public boolean isBarVisible(ItemStack boxStack) {
        return getCount(boxStack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack boxStack) {
        return Math.min(1 + 12 * getCount(boxStack) / CAPACITY, 13);
    }

    @Override
    public int getBarColor(ItemStack boxStack) {
        return BAR_COLOR;
    }

    /**
     * The grid of sticks under the tooltip, drawn by vanilla's own bundle tooltip —
     * which is why each stick shows how worn it is without anything being asked for.
     *
     * Its weight is only read to decide whether to draw the blocked-slot art, so it
     * is scaled to the sixty-four that expects rather than being a count of anything.
     *
     * @param boxStack
     * @return
     */
    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack boxStack) {
        final NonNullList<ItemStack> contents = NonNullList.create();
        getContents(boxStack).forEach(contents::add);

        return Optional.of(new BundleTooltip(contents, getCount(boxStack) * 64 / CAPACITY));
    }

    @Override
    public void appendHoverText(ItemStack boxStack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(
            Component.translatable("item.zetter.chalk_box.fullness", getCount(boxStack), CAPACITY)
                .withStyle(ChatFormatting.GRAY)
        );
    }

    /**
     * Burned or otherwise destroyed on the ground, the chalk spills rather than going
     * with it
     *
     * @param itemEntity
     */
    @Override
    public void onDestroyed(ItemEntity itemEntity) {
        ItemUtils.onContainerDestroyed(itemEntity, getContents(itemEntity.getItem()));
    }

    private static void playRemoveOneSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    private static void playInsertSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }
}
