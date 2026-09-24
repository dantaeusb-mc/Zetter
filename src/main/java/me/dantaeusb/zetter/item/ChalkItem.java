package me.dantaeusb.zetter.item;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A stick of chalk, one per dye color. Draws straight onto a blackboard rather
 * than opening a screen, and wears down as it goes the way a palette does.
 */
public class ChalkItem extends Item {
    /**
     * A blackboard is a thousand pixels of slate, and chalk should last longer than
     * one boardful, so it is counted in pixels rather than in strokes
     */
    public static final int DURABILITY = 8192;

    /**
     * Tinting with pastel colors for better legibility.
     */
    private static final float BINDER_TINT = 0.45f;

    private final DyeColor color;

    public ChalkItem(Properties properties, DyeColor color) {
        super(properties.durability(DURABILITY));

        this.color = color;
    }

    public DyeColor getDyeColor() {
        return this.color;
    }

    /**
     * Color laid down on the slate: the dye's text color, tinted toward white by
     * the binder.
     * @return
     */
    public int getColor() {
        final int textColor = this.color.getTextColor();

        return 0xFF000000
            | tintChannel(textColor >> 16 & 0xFF) << 16
            | tintChannel(textColor >> 8 & 0xFF) << 8
            | tintChannel(textColor & 0xFF);
    }

    /**
     * Mixed in sRGB and not in the mod's linear RGB: the binder is a wash of white
     * over the pigment, which is a perceptual mix rather than a physical one. Mixing
     * the same amount in linear space comes out noticeably lighter and takes the hue
     * with it.
     */
    private static int tintChannel(int value) {
        return Math.round(value + (255 - value) * BINDER_TINT);
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }
}
