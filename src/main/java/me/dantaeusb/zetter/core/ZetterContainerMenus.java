package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.menu.EaselMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ZetterContainerMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Zetter.MOD_ID);

    public static DeferredHolder<MenuType<?>, MenuType<EaselMenu>> PAINTING = MENUS.register("painting_container", () -> IMenuTypeExtension.create(EaselMenu::createMenuClientSide));
    public static DeferredHolder<MenuType<?>, MenuType<EaselMenu>> EASEL = MENUS.register("easel_container", () -> IMenuTypeExtension.create(EaselMenu::createMenuClientSide));
    public static DeferredHolder<MenuType<?>, MenuType<ArtistTableMenu>> ARTIST_TABLE = MENUS.register("artist_table_container", () -> IMenuTypeExtension.create(ArtistTableMenu::createMenuClientSide));

    public static void init(IEventBus bus) {
        MENUS.register(bus);
    }
}
