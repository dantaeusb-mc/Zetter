package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.item.BlackboardItem;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.item.ChalkItem;
import me.dantaeusb.zetter.item.FrameItem;
import me.dantaeusb.zetter.item.SpongeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ZetterCreativeTabs
{
    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ZetterItems.PALETTE);
            event.accept(CanvasItem.createBlank(1, 1));
            event.accept(ZetterItems.SMALL_SPONGE);
            event.accept(driedSponge());

            for (RegistryObject<ChalkItem> chalkItem : ZetterItems.CHALKS.values()) {
                event.accept(chalkItem);
            }
        } else if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ZetterItems.ARTIST_TABLE);
            event.accept(ZetterItems.EASEL);
            event.accept(ZetterItems.WALL_EASEL);

            for (RegistryObject<FrameItem> frameItem : ZetterItems.FRAMES.values()) {
                event.accept(frameItem);
            }

            for (RegistryObject<BlackboardItem> blackboardItem : ZetterItems.BLACKBOARDS.values()) {
                event.accept(blackboardItem);
            }
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ZetterItems.PAINTS);
        }
    }

    /**
     * Wet and dry are the same item, so the dry one is only reachable in creative by
     * handing one out with its water already gone
     *
     * @return
     */
    private static ItemStack driedSponge() {
        final ItemStack spongeStack = new ItemStack(ZetterItems.SMALL_SPONGE.get());
        spongeStack.setDamageValue(SpongeItem.CAPACITY - 1);

        return spongeStack;
    }
}