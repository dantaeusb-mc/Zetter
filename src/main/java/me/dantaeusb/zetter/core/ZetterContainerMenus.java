package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.menu.EaselContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ZetterContainerMenus {
    public static MenuType<EaselContainerMenu> PAINTING;
    public static MenuType<ArtistTableMenu> ARTIST_TABLE;

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onContainerRegistry(final RegistryEvent.Register<MenuType<?>> event)
    {
        PAINTING = IForgeMenuType.create(EaselContainerMenu::createContainerClientSide);
        PAINTING.setRegistryName(Zetter.MOD_ID, "painting_container");
        event.getRegistry().register(PAINTING);

        ARTIST_TABLE = IForgeMenuType.create(ArtistTableMenu::createContainerClientSide);
        ARTIST_TABLE.setRegistryName(Zetter.MOD_ID, "artist_table_container");
        event.getRegistry().register(ARTIST_TABLE);
    }
}
