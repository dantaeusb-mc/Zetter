package me.dantaeusb.zetter.item;

import io.netty.util.internal.StringUtil;
import me.dantaeusb.zetter.entity.item.PaintingEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Optional;

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
     *
     * @param stack
     * @return
     */
    @Override
    public ITextComponent getName(ItemStack stack) {
        if (stack.hasTag()) {
            String paintingName = getCachedPaintingName(stack);

            if (StringUtil.isNullOrEmpty(paintingName)) {
                if (StringUtil.isNullOrEmpty(getPaintingCode(stack))) {
                    return new TranslationTextComponent(this.getDescriptionId(stack));
                }

                paintingName = new TranslationTextComponent("item.zetter.painting.unnamed").getString();
            }

            if (!StringUtils.isNullOrEmpty(paintingName)) {
                return new TranslationTextComponent(paintingName);
            }
        }

        return new TranslationTextComponent(this.getDescriptionId(stack));
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
    public static byte getHasPaintingPropertyOverride(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity livingEntity)
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

    public ActionResultType useOn(ItemUseContext context) {
        BlockPos blockPos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        BlockPos facePos = blockPos.relative(direction);
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player != null && !this.canPlace(player, direction, stack, facePos)) {
            return ActionResultType.FAIL;
        } else {
            if (StringUtil.isNullOrEmpty(getPaintingCode(stack))) {
                return ActionResultType.FAIL;
            }

            World world = context.getLevel();

            PaintingEntity paintingEntity = new PaintingEntity(
                    world, facePos, direction, this.material, this.hasPlate, getPaintingCode(stack), getBlockSize(stack), getGeneration(stack)
            );

            if (!paintingEntity.survives()) {
                return ActionResultType.CONSUME;
            }

            if (!world.isClientSide) {
                paintingEntity.playPlacementSound();
                world.addFreshEntity(paintingEntity);
            }

            player.setItemInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
            return ActionResultType.sidedSuccess(world.isClientSide);
        }
    }

    protected boolean canPlace(PlayerEntity playerIn, Direction directionIn, ItemStack itemStackIn, BlockPos posIn) {
        return !directionIn.getAxis().isVertical() && playerIn.mayUseItemAt(posIn, directionIn, itemStackIn);
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
        public String getSerializedName()
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