package com.dantaeusb.immersivemp.locks.item;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.core.Helper;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

import java.util.UUID;

public class CanvasItem extends AbstractLockItem
{
    public static final String NBT_TAG_NAME_CANVAS_DATA = "canvasData";
    public static int CANVAS_SIZE = 16 * 16;
    public static int CANVAS_BYTE_SIZE = CANVAS_SIZE * 4;

    public CanvasItem() {
        super(new Properties().maxStackSize(1).group(ItemGroup.TOOLS));
    }

    public static byte[] getCanvasData(ItemStack stack, boolean forceCreate)
    {
        CompoundNBT compoundNBT = stack.getTag();

        byte[] canvasData = null;
        if (compoundNBT != null && compoundNBT.contains(NBT_TAG_NAME_CANVAS_DATA)) {
            canvasData = compoundNBT.getByteArray(NBT_TAG_NAME_CANVAS_DATA);
        } else {
            canvasData = new byte[CANVAS_BYTE_SIZE];

            for (int i = 0; i < CANVAS_SIZE; i++) {
                int iterator = i * 4;
                canvasData[iterator] = (byte) 0xFF;
                canvasData[iterator + 1] = (byte) 0x00;
                canvasData[iterator + 2] = (byte) 0x00;
                canvasData[iterator + 3] = (byte) 0x00;
            }
        }

        return canvasData;
    }

    /**
     * Sets key id
     *
     * @param stack the stack
     * @param keyId the new UUID
     */
    public static void setCanvasData(ItemStack stack, byte[] canvasData)
    {
        CompoundNBT compoundNBT = stack.getOrCreateTag();
        compoundNBT.putByteArray(NBT_TAG_NAME_CANVAS_DATA, canvasData);
    }
}