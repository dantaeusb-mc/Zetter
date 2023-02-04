package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.menu.EaselMenu;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ZetterContainerMenus {
    private static final DeferredRegister<ContainerType<?>> MENUS = DeferredRegister.create(ForgeRegistries.CONTAINERS, Zetter.MOD_ID);

    public static RegistryObject<ContainerType<EaselMenu>> PAINTING = MENUS.register("painting_container", () -> IForgeContainerType.create(EaselMenu::createMenuClientSide));
    public static RegistryObject<ContainerType<EaselMenu>> EASEL = MENUS.register("easel_container", () -> IForgeContainerType.create(EaselMenu::createMenuClientSide));
    public static RegistryObject<ContainerType<ArtistTableMenu>> ARTIST_TABLE = MENUS.register("artist_table_container", () -> IForgeContainerType.create(ArtistTableMenu::createMenuClientSide));

    public static void init(IEventBus bus) {
        MENUS.register(bus);
    }
}
