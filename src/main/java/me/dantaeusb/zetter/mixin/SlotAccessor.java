package me.dantaeusb.zetter.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @todo: remove
 */
@Mixin(net.minecraft.world.inventory.Slot.class)
public interface SlotAccessor {
    @Accessor
    @Mutable
    void setX(int x);

    @Accessor
    @Mutable
    void setY(int y);
}
