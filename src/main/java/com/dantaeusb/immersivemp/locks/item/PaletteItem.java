package com.dantaeusb.immersivemp.locks.item;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

public class PaletteItem extends AbstractLockItem
{
    public static final String NBT_TAG_NAME_PALETTE_COLORS = "paletteColors";
    public static int PALETTE_SIZE = 14;

    public PaletteItem() {
        super(new Properties().maxStackSize(1).group(ItemGroup.TOOLS));
    }

    public static int[] getPaletteColors(ItemStack stack, boolean forceCreate)
    {
        CompoundNBT compoundNBT = stack.getTag();

        int[] paletteColors = null;
        if (compoundNBT != null && compoundNBT.contains(NBT_TAG_NAME_PALETTE_COLORS)) {
            paletteColors = compoundNBT.getIntArray(NBT_TAG_NAME_PALETTE_COLORS);
        } else {
            paletteColors = new int[PALETTE_SIZE];

            
        }

        return paletteColors;
    }
    
    public static int[] getDefaultPaletteColors() 
    {
        int[] paletteColors = new int[PALETTE_SIZE];

        paletteColors[0] = 0xFFAA0000; //dark-red
        paletteColors[1] = 0xFFFF5555; //red
        paletteColors[2] = 0xFFFFAA00; //gold
        paletteColors[3] = 0xFFFFFF55; //yellow
        paletteColors[4] = 0xFF00AA00; //dark-green
        paletteColors[5] = 0xFF55FF55; //green
        paletteColors[6] = 0xFF55FFFF; //aqua
        paletteColors[7] = 0xFF00AAAA; //dark-aqua
        paletteColors[8] = 0xFF0000AA; //dark-blue
        paletteColors[9] = 0xFF5555FF; //blue
        paletteColors[10] = 0xFFFF55FF; //light-purple
        paletteColors[11] = 0xFFAA00AA; //purple
        paletteColors[12] = 0xFFAAAAAA; //gray
        paletteColors[13] = 0xFF555555; //dark-gray

        return paletteColors;
    }

    /**
     * Sets key id
     *
     * @param stack the stack
     * @param keyId the new UUID
     */
    public static void setPaletteColors(ItemStack stack, int[] paletteColors)
    {
        CompoundNBT compoundNBT = stack.getOrCreateTag();
        compoundNBT.putIntArray(NBT_TAG_NAME_PALETTE_COLORS, paletteColors);
    }
}