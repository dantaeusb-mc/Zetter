package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.item.FrameItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

@EventBusSubscriber(modid = Zetter.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ZetterCreativeTabs
{
    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ZetterItems.PALETTE.get());
            event.accept(ZetterItems.CANVAS.get());
        } else if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ZetterItems.ARTIST_TABLE.get());
            event.accept(ZetterItems.EASEL.get());

            for (DeferredHolder<Item, FrameItem> frameItem : ZetterItems.FRAMES.values()) {
                event.accept(frameItem.get());
            }
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ZetterItems.PAINTS.get());
        }
    }
}