package me.dantaeusb.zetter.item;

import me.dantaeusb.zetter.entity.item.PaintingEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Optional;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

public class FrameItem extends PaintingItem {
    private PaintingEntity.Materials material;
    private boolean hasPlate;

    public FrameItem(Properties properties, PaintingEntity.Materials material, boolean plated) {
        super(properties);

        this.material = material;
        this.hasPlate = plated;
    }

    /**
     * Use fallback to default behavior for frame
     * @param stack
     * @return
     */
    @Override
    public Component getName(ItemStack stack) {
        if (stack.hasTag()) {
            String paintingName = getCachedPaintingName(stack);

            if (StringUtil.isNullOrEmpty(paintingName)) {
                if (StringUtil.isNullOrEmpty(getPaintingCode(stack))) {
                    return new TranslatableComponent(this.getDescriptionId(stack));
                }

                paintingName = new TranslatableComponent("item.zetter.painting.unnamed").getString();
            }

            if (!net.minecraft.util.StringUtil.isNullOrEmpty(paintingName)) {
                return new TranslatableComponent(paintingName);
            }
        }

        return new TranslatableComponent(this.getDescriptionId(stack));
    }

    public PaintingEntity.Materials getMaterial() {
        return this.material;
    }

    public boolean hasPlate() {
        return this.hasPlate;
    }

    /**
     * @param stack
     * @param world
     * @param livingEntity
     * @return
     */
    public static byte getHasPaintingPropertyOverride(ItemStack stack, @Nullable Level world, @Nullable LivingEntity livingEntity, int weirdInt)
    {
        EnumFrameStyle hasPainting;

        if (StringUtil.isNullOrEmpty(getPaintingCode(stack))) {
            hasPainting = EnumFrameStyle.EMPTY;
        } else {
            hasPainting = EnumFrameStyle.PAINTING;
        }

        return hasPainting.getPropertyOverrideValue();
    }

    /**
     * Hanging painting
     */

    public InteractionResult useOn(UseOnContext context) {
        BlockPos blockPos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        BlockPos facePos = blockPos.relative(direction);
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player != null && !this.canPlace(player, direction, stack, facePos)) {
            return InteractionResult.FAIL;
        } else {
            if (StringUtil.isNullOrEmpty(getPaintingCode(stack))) {
                return InteractionResult.FAIL;
            }

            Level world = context.getLevel();

            PaintingEntity paintingEntity = new PaintingEntity(
                    world, facePos, direction, this.material, this.hasPlate, getPaintingCode(stack), getBlockSize(stack), getGeneration(stack)
            );

            if (!paintingEntity.survives()) {
                return InteractionResult.CONSUME;
            }

            if (!world.isClientSide) {
                paintingEntity.playPlacementSound();
                world.addFreshEntity(paintingEntity);
            }

            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
    }

    protected boolean canPlace(Player playerIn, Direction directionIn, ItemStack itemStackIn, BlockPos posIn) {
        return !directionIn.getAxis().isVertical() && playerIn.mayUseItemAt(posIn, directionIn, itemStackIn);
    }

    public enum EnumFrameStyle implements StringRepresentable
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
        public String getSerializedName()
        {
            return this.name;
        }

        public String getDescription() { return this.description; }

        public byte getPropertyOverrideValue() { return nbtId; }

        public static EnumFrameStyle fromNBT(CompoundTag compoundNBT, String tagname)
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
        public void putIntoNBT(CompoundTag compoundNBT, String tagName)
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