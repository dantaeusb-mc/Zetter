package com.dantaeusb.zetter.item;

import com.dantaeusb.zetter.core.Helper;
import com.dantaeusb.zetter.entity.item.CustomPaintingEntity;
import com.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class FrameItem extends Item {
    private CustomPaintingEntity.Materials material;
    private boolean hasPlate;

    public static final String NBT_TAG_CACHED_PAINTING_NAME = "CachedPaintingName";
    public static final String NBT_TAG_CACHED_AUTHOR_NAME = "CachedAuthorName";
    public static final String NBT_TAG_CACHED_STRING_SIZE = "CachedStringSize";

    public FrameItem(CustomPaintingEntity.Materials material, boolean plated) {
        super(new Properties().maxStackSize(1).group(ItemGroup.TOOLS));

        this.material = material;
        this.hasPlate = plated;
    }

    public ITextComponent getDisplayName(ItemStack stack) {
        if (stack.hasTag()) {
            // No painting found, use frame name
            if (StringUtils.isNullOrEmpty(getPaintingCode(stack))) {
                return super.getDisplayName(stack);
            }

            String paintingName = getCachedPaintingName(stack);

            if (StringUtils.isNullOrEmpty(paintingName)) {
                paintingName = new TranslationTextComponent("item.zetter.painting.unnamed").getString();
            }

            if (!net.minecraft.util.StringUtils.isNullOrEmpty(paintingName)) {
                return new StringTextComponent(paintingName);
            }
        }

        return super.getDisplayName(stack);
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
        EnumFrameHasPainting hasPainting;

        if (StringUtils.isNullOrEmpty(getPaintingCode(stack))) {
            hasPainting = EnumFrameHasPainting.MISSING;
        } else {
            hasPainting = EnumFrameHasPainting.FRAMED;
        }

        return hasPainting.getPropertyOverrideValue();
    }

    public static void setPaintingData(ItemStack stack, PaintingData paintingData) {
        setPaintingCode(stack, paintingData.getName());

        setCachedAuthorName(stack, paintingData.getAuthorName());
        setCachedPaintingName(stack, paintingData.getPaintingName());

        String widthBlocks = Integer.toString((paintingData.getWidth() / paintingData.getResolution().getNumeric()));
        String heightBlocks = Integer.toString((paintingData.getHeight() / paintingData.getResolution().getNumeric()));
        TranslationTextComponent blockSizeString = (new TranslationTextComponent("item.zetter.painting.size", widthBlocks, heightBlocks));

        setCachedStringSize(stack, blockSizeString.getString());
    }

    /**
     * It's there but we probably should avoid this
     * @param stack
     * @return
     */
    @Deprecated
    @Nullable
    public static PaintingData getPaintingData(ItemStack stack) {
        return Helper.getWorldCanvasTracker().getCanvasData(getPaintingCode(stack), PaintingData.class);
    }

    public static void setPaintingCode(ItemStack stack, String canvasCode) {
        stack.getOrCreateTag().putString(CustomPaintingEntity.NBT_TAG_PAINTING_CODE, canvasCode);
    }

    @Nullable
    public static String getPaintingCode(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();

        if (compoundNBT == null) {
            return null;
        }

        return compoundNBT.getString(CustomPaintingEntity.NBT_TAG_PAINTING_CODE);
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

    public static void setCachedAuthorName(ItemStack stack, String authorName) {
        stack.getOrCreateTag().putString(NBT_TAG_CACHED_AUTHOR_NAME, authorName);
    }

    @Nullable
    public static String getCachedAuthorName(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();

        if (compoundNBT == null) {
            return null;
        }

        return compoundNBT.getString(NBT_TAG_CACHED_AUTHOR_NAME);
    }

    public static void setCachedPaintingName(ItemStack stack, String paintingName) {
        stack.getOrCreateTag().putString(NBT_TAG_CACHED_PAINTING_NAME, paintingName);
    }

    @Nullable
    public static String getCachedPaintingName(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();

        if (compoundNBT == null) {
            return null;
        }

        return compoundNBT.getString(NBT_TAG_CACHED_PAINTING_NAME);
    }

    public static void setCachedStringSize(ItemStack stack, String stringSize) {
        stack.getOrCreateTag().putString(NBT_TAG_CACHED_STRING_SIZE, stringSize);
    }

    @Nullable
    public static String getCachedStringSize(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();

        if (compoundNBT == null) {
            return null;
        }

        return compoundNBT.getString(NBT_TAG_CACHED_STRING_SIZE);
    }

    // @todo: Restore caching of painting name and author name

    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        if (stack.hasTag()) {
            String authorName = getCachedAuthorName(stack);

            if (StringUtils.isNullOrEmpty(authorName)) {
                authorName = new TranslationTextComponent("item.zetter.painting.unknown").getString();
            }

            tooltip.add((new TranslationTextComponent("book.byAuthor", authorName)).mergeStyle(TextFormatting.GRAY));

            String stringSize = getCachedStringSize(stack);

            if (!StringUtils.isNullOrEmpty(stringSize)) {
                tooltip.add((new StringTextComponent(stringSize)).mergeStyle(TextFormatting.GRAY));
            }
        }
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

    public enum EnumFrameHasPainting implements IStringSerializable
    {
        MISSING(0, "missing", "missing"),
        FRAMED(1, "framed", "framed");

        private final byte nbtId;
        private final String name;
        private final String description;

        EnumFrameHasPainting(int nbtId, String name, String description)
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

        public static EnumFrameHasPainting fromNBT(CompoundNBT compoundNBT, String tagname)
        {
            byte hasPaintingValue = 0;

            if (compoundNBT != null && compoundNBT.contains(tagname)) {
                hasPaintingValue = compoundNBT.getByte(tagname);
            }
            Optional<EnumFrameHasPainting> hasPainting = getEnumFromValue(hasPaintingValue);
            return hasPainting.orElse(MISSING);
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

        private static Optional<EnumFrameHasPainting> getEnumFromValue(byte ID) {
            for (EnumFrameHasPainting fullness : EnumFrameHasPainting.values()) {
                if (fullness.nbtId == ID) return Optional.of(fullness);
            }

            return Optional.empty();
        }
    }
}