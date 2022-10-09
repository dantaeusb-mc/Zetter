package me.dantaeusb.zetter.item;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.canvastracker.ICanvasTracker;
import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.core.ClientHelper;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterNetwork;
import me.dantaeusb.zetter.network.packet.CCanvasRequestViewPacket;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.StringUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class PaintingItem extends CanvasItem
{
    public static final String NBT_TAG_PAINTING_CODE = "PaintingCode";

    public static final String NBT_TAG_CACHED_PAINTING_TITLE = "CachedPaintingName";
    public static final String NBT_TAG_CACHED_AUTHOR_NAME = "CachedAuthorName";
    public static final String NBT_TAG_GENERATION = "Generation";

    public static final int GENERATION_ORIGINAL = 0;
    public static final int GENERATION_COPY = 1;
    public static final int GENERATION_COPY_OF_COPY = 2;

    public PaintingItem() {
        super();
    }

    // @todo: [HIGH] Canvas data could be null!!!
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack painting = player.getItemInHand(hand);

        if (world.isClientSide()) {
            ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(world);
            String paintingCode = getPaintingCode(painting);
            PaintingData canvasData = getPaintingData(painting, player.getLevel());

            if (canvasData != null) {
                // If data is loaded, just show screen
                ClientHelper.openPaintingScreen(
                        player,
                        paintingCode,
                        canvasData,
                        hand
                );
            } else {
                // If data is not loaded, request and show screen after
                CCanvasRequestViewPacket requestViewPacket = new CCanvasRequestViewPacket(AbstractCanvasData.Type.PAINTING, paintingCode, hand);
                Zetter.LOG.debug("Sending request view packet: " + requestViewPacket);
                ZetterNetwork.simpleChannel.sendToServer(requestViewPacket);
            }
        }

        player.awardStat(Stats.ITEM_USED.get(this));

        return InteractionResultHolder.sidedSuccess(painting, world.isClientSide());
    }

    /**
     * Set painting data for this item, cache name, author and
     * size in item's NBT data
     *
     * @param stack
     * @param paintingCode
     * @param paintingData
     * @param generation
     */
    public static void storePaintingData(ItemStack stack, String paintingCode, PaintingData paintingData, int generation) {
        stack.getOrCreateTag().putString(NBT_TAG_PAINTING_CODE, paintingCode);

        stack.getOrCreateTag().putString(NBT_TAG_CACHED_AUTHOR_NAME, paintingData.getAuthorName());
        stack.getOrCreateTag().putString(NBT_TAG_CACHED_PAINTING_TITLE, paintingData.getPaintingTitle());

        int widthBlocks = paintingData.getWidth() / paintingData.getResolution().getNumeric();
        int heightBlocks = paintingData.getHeight() / paintingData.getResolution().getNumeric();

        final int[] size = new int[]{widthBlocks, heightBlocks};
        Component blockSizeString = (Component.translatable("item.zetter.painting.size", Integer.toString(widthBlocks), Integer.toString(heightBlocks)));

        stack.getOrCreateTag().putIntArray(NBT_TAG_CACHED_BLOCK_SIZE, size);
        stack.getOrCreateTag().putString(NBT_TAG_CACHED_STRING_SIZE, blockSizeString.getString());
        stack.getOrCreateTag().putInt(NBT_TAG_GENERATION, generation);
    }

    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        if (stack.hasTag()) {
            String authorName = getCachedAuthorName(stack);

            if (StringUtil.isNullOrEmpty(authorName)) {
                authorName = Component.translatable("item.zetter.painting.unknown").getString();
            }

            tooltip.add((Component.translatable("book.byAuthor", authorName)).withStyle(ChatFormatting.GRAY));

            String stringSize = getCachedStringSize(stack);

            if (!StringUtil.isNullOrEmpty(stringSize)) {
                tooltip.add((Component.literal(stringSize)).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        if (stack.hasTag()) {
            String paintingName = getCachedPaintingName(stack);

            if (StringUtil.isNullOrEmpty(paintingName)) {
                if (StringUtil.isNullOrEmpty(getPaintingCode(stack))) {
                    return super.getName(stack);
                }

                paintingName = Component.translatable("item.zetter.painting.unnamed").getString();
            }

            if (!net.minecraft.util.StringUtil.isNullOrEmpty(paintingName)) {
                return Component.literal(paintingName);
            }
        }

        return super.getName(stack);
    }

    /**
     * Names are public for artist table preview
     * Getters for cached data
     */

    @Nullable
    public static String getPaintingCode(ItemStack stack) {
        CompoundTag compoundNBT = stack.getTag();

        if (compoundNBT == null) {
            return null;
        }

        return compoundNBT.getString(NBT_TAG_PAINTING_CODE);
    }

    /**
     *
     * @see {net.minecraft.world.item.MapItem#getCustomMapData(ItemStack, Level)}
     * @param stack
     * @param world
     * @return
     */
    @Nullable
    public static PaintingData getPaintingData(ItemStack stack, Level world) {
        Item painting = stack.getItem();

        if (painting instanceof PaintingItem || painting instanceof FrameItem) {
            String paintingCode = getPaintingCode(stack);

            if (paintingCode == null) {
                return null;
            }

            ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(world);

            return canvasTracker.getCanvasData(paintingCode, PaintingData.class);
        }

        return null;
    }

    @Nullable
    public static String getCachedAuthorName(ItemStack stack) {
        CompoundTag compoundNBT = stack.getTag();

        if (compoundNBT == null) {
            return null;
        }

        return compoundNBT.getString(NBT_TAG_CACHED_AUTHOR_NAME);
    }

    @Nullable
    public static String getCachedPaintingName(ItemStack stack) {
        CompoundTag compoundNBT = stack.getTag();

        if (compoundNBT == null) {
            return null;
        }

        return compoundNBT.getString(NBT_TAG_CACHED_PAINTING_TITLE);
    }

    @Nullable
    public static int[] getBlockSize(ItemStack stack) {
        CompoundTag compoundNBT = stack.getTag();

        if (compoundNBT == null) {
            return null;
        }

        return compoundNBT.getIntArray(NBT_TAG_CACHED_BLOCK_SIZE);
    }

    @Nullable
    public static String getCachedStringSize(ItemStack stack) {
        CompoundTag compoundNBT = stack.getTag();

        if (compoundNBT == null) {
            return null;
        }

        return compoundNBT.getString(NBT_TAG_CACHED_STRING_SIZE);
    }

    public static int getGeneration(ItemStack stack) {
        CompoundTag compoundNBT = stack.getTag();

        if (compoundNBT == null) {
            return 0;
        }

        return compoundNBT.getInt(NBT_TAG_GENERATION);
    }
}