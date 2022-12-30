package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.menu.EaselMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ZetterContainerMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.CONTAINERS, Zetter.MOD_ID);

    public static RegistryObject<MenuType<EaselMenu>> PAINTING = MENUS.register("painting_container", () -> IForgeMenuType.create(EaselMenu::createMenuClientSide));
    public static RegistryObject<MenuType<EaselMenu>> EASEL = MENUS.register("easel_container", () -> IForgeMenuType.create(EaselMenu::createMenuClientSide));
    public static RegistryObject<MenuType<ArtistTableMenu>> ARTIST_TABLE = MENUS.register("artist_table_container", () -> IForgeMenuType.create(ArtistTableMenu::createMenuClientSide));

    public static void init(IEventBus bus) {
        MENUS.register(bus);
    }
}
