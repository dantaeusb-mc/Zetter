package com.dantaeusb.zetter.item;

import com.dantaeusb.zetter.core.ModItems;
import com.dantaeusb.zetter.entity.item.CustomPaintingEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Optional;

public class FrameItem extends PaintingItem {
    private CustomPaintingEntity.Materials material;
    private boolean hasPlate;

    public FrameItem(CustomPaintingEntity.Materials material, boolean plated) {
        super(new Properties().maxStackSize(1).group(ItemGroup.TOOLS).containerItem(ModItems.PAINTING));

        this.material = material;
        this.hasPlate = plated;
    }

    public CustomPaintingEntity.Materials getMaterial() {
        return this.material;
    }

    public boolean hasPlate() {
        return this.hasPlate;
    }

    /**
     * gets the fullness property override, used in mbe11_item_variants_registry_name.json to select which model should
     *   be rendered
     * @param stack
     * @param world
     * @param livingEntity
     * @return
     */
    public static byte getHasPaintingPropertyOverride(ItemStack stack, @Nullable World world, @Nullable LivingEntity livingEntity)
    {
        EnumFrameStyle hasPainting;

        if (StringUtils.isNullOrEmpty(getPaintingCode(stack))) {
            hasPainting = EnumFrameStyle.EMPTY;
        } else {
            hasPainting = EnumFrameStyle.PAINTING;
        }

        return hasPainting.getPropertyOverrideValue();
    }

    public static void setBlockSize(ItemStack stack, int[] blockSize) {
        stack.getOrCreateTag().putIntArray(CustomPaintingEntity.NBT_TAG_BLOCK_SIZE, blockSize);
    }

    @Nullable
    public static int[] getBlockSize(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();

        if (compoundNBT == null) {
            return null;
        }

        return compoundNBT.getIntArray(CustomPaintingEntity.NBT_TAG_BLOCK_SIZE);
    }

    /**
     * Hanging painting
     */

    public ActionResultType onItemUse(ItemUseContext context) {
        BlockPos blockPos = context.getPos();
        Direction direction = context.getFace();
        BlockPos facePos = blockPos.offset(direction);
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getItem();

        if (player != null && !this.canPlace(player, direction, stack, facePos)) {
            return ActionResultType.FAIL;
        } else {
            if (StringUtils.isNullOrEmpty(FrameItem.getPaintingCode(stack))) {
                return ActionResultType.FAIL;
            }

            World world = context.getWorld();

            CustomPaintingEntity paintingEntity = new CustomPaintingEntity(
                    world, facePos, direction, this.material, this.hasPlate, getPaintingCode(stack), getBlockSize(stack)
            );

            if (paintingEntity.onValidSurface()) {
                if (!world.isRemote) {
                    paintingEntity.playPlaceSound();
                    world.addEntity(paintingEntity);
                }

                player.setHeldItem(Hand.MAIN_HAND, ItemStack.EMPTY);
                return ActionResultType.func_233537_a_(world.isRemote);
            } else {
                return ActionResultType.CONSUME;
            }
        }
    }

    protected boolean canPlace(PlayerEntity playerIn, Direction directionIn, ItemStack itemStackIn, BlockPos posIn) {
        return !directionIn.getAxis().isVertical() && playerIn.canPlayerEdit(posIn, directionIn, itemStackIn);
    }

    public enum EnumFrameStyle implements IStringSerializable
    {
        EMPTY(0, "empty", "Missing painting"),
        PAINTING(1, "painting", "Framed painting");

        private final byte nbtId;
        private final String name;
        private final String description;

        EnumFrameStyle(int nbtId, String name, String description)
        {
            this.nbtId = (byte)nbtId;
            this.name = name;
            this.description = description;
        }

        @Override
        public String toString()
        {
            return this.description;
        }

        @Override
        public String getString()
        {
            return this.name;
        }

        public String getDescription() { return this.description; }

        public byte getPropertyOverrideValue() { return nbtId; }

        public static EnumFrameStyle fromNBT(CompoundNBT compoundNBT, String tagname)
        {
            byte hasPaintingValue = 0;

            if (compoundNBT != null && compoundNBT.contains(tagname)) {
                hasPaintingValue = compoundNBT.getByte(tagname);
            }
            Optional<EnumFrameStyle> hasPainting = getEnumFromValue(hasPaintingValue);
            return hasPainting.orElse(EMPTY);
        }

        /**
         * Write this enum to NBT
         * @param compoundNBT
         * @param tagName
         */
        public void putIntoNBT(CompoundNBT compoundNBT, String tagName)
        {
            compoundNBT.putByte(tagName, this.nbtId);
        }

        private static Optional<EnumFrameStyle> getEnumFromValue(byte ID) {
            for (EnumFrameStyle fullness : EnumFrameStyle.values()) {
                if (fullness.nbtId == ID) return Optional.of(fullness);
            }

            return Optional.empty();
        }
    }
}