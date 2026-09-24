package me.dantaeusb.zetter.item;

import me.dantaeusb.zetter.painting.Tool;
import me.dantaeusb.zetter.painting.parameters.AbstractToolParameters;
import net.minecraft.world.item.ItemStack;

/**
 * Something a board is worked on with directly, in the world, rather than through
 * the painting screen.
 *
 * The item says what it does: which tool it drives, how that tool is set up and what
 * color it lays down. The board only asks whether it takes the item at all, and the
 * input handler only asks the item what to do with it, so neither of them has to
 * know what chalk or a sponge is.
 */
public interface BlackboardImplement {
    Tool getTool();

    /**
     * Settings the tool runs with, read off the stack.
     *
     * These travel inside the action and are captured when it starts, so whatever the
     * stack said at that moment is what the whole stroke replays with on every
     * client. An implement whose settings change as it is used — a sponge drying out
     * — therefore changes between strokes rather than during one, which is both what
     * it should look like and the only way it could stay in sync.
     *
     * @param stack
     * @return
     */
    AbstractToolParameters getToolParameters(ItemStack stack);

    /**
     * Color laid down, for the tools that lay one down at all
     *
     * @param stack
     * @return
     */
    int getToolColor(ItemStack stack);

    /**
     * Whether it has anything left to give.
     *
     * A stick of chalk wears down to nothing, but a sponge that has run out of water
     * is not finished — it stops lifting chalk and starts pushing it around, which is
     * a different tool rather than no tool.
     *
     * @param stack
     * @return
     */
    default boolean isWornOut(ItemStack stack) {
        return stack.getDamageValue() >= stack.getMaxDamage() - 1;
    }
}
