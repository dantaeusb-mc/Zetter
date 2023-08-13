package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.item.FrameItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ZetterCreativeTabs
{
    @SubscribeEvent
    public static void buildContents(CreativeModeTabEvent.BuildContents event) {
        if (event.getTab() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ZetterItems.PALETTE);
            event.accept(ZetterItems.CANVAS);
        } else if (event.getTab() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ZetterItems.ARTIST_TABLE);
            event.accept(ZetterItems.EASEL);

            for (RegistryObject<FrameItem> frameItem : ZetterItems.FRAMES.values()) {
                event.accept(frameItem);
            }
        } else if (event.getTab() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ZetterItems.PAINTS);
        }
    }
}