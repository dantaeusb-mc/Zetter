package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.menu.EaselContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ZetterContainerMenus {
    private static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, Zetter.MOD_ID);

    public static RegistryObject<MenuType<EaselContainerMenu>> PAINTING = CONTAINERS.register("painting_container", () -> IForgeMenuType.create(EaselContainerMenu::createContainerClientSide));
    public static RegistryObject<MenuType<ArtistTableMenu>> ARTIST_TABLE = CONTAINERS.register("artist_table_container", () -> IForgeMenuType.create(ArtistTableMenu::createContainerClientSide));

    public static void init(IEventBus bus) {
        CONTAINERS.register(bus);
    }
}
