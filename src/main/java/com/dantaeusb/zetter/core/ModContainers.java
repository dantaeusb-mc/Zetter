package com.dantaeusb.zetter.core;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.menu.ArtistTableMenu;
import com.dantaeusb.zetter.menu.EaselContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModContainers {
    public static MenuType<EaselContainerMenu> PAINTING;
    public static MenuType<ArtistTableMenu> ARTIST_TABLE;

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onContainerRegistry(final RegistryEvent.Register<MenuType<?>> event)
    {
        PAINTING = IForgeContainerType.create(EaselContainerMenu::createContainerClientSide);
        PAINTING.setRegistryName(Zetter.MOD_ID, "painting_container");
        event.getRegistry().register(PAINTING);

        ARTIST_TABLE = IForgeContainerType.create(ArtistTableMenu::createContainerClientSide);
        ARTIST_TABLE.setRegistryName(Zetter.MOD_ID, "artist_table_container");
        event.getRegistry().register(ARTIST_TABLE);
    }
}
