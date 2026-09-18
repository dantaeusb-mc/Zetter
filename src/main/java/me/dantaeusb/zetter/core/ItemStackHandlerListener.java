package me.dantaeusb.zetter.core;

import net.neoforged.neoforge.items.ItemStackHandler;

public interface ItemStackHandlerListener {
    void containerChanged(ItemStackHandler container, int slot);
}
