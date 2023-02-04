package me.dantaeusb.zetter.item;

import io.netty.util.internal.StringUtil;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.core.ClientHelper;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterNetwork;
import me.dantaeusb.zetter.network.packet.CCanvasRequestViewPacket;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.StringUtils;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
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

    public PaintingItem(Properties properties) {
        super(properties);
    }

    // @todo: [HIGH] Canvas data could be null!!!
    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack paintingStack = player.getItemInHand(hand);

        if (world.isClientSide()) {
            CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(world);
            String paintingCode = getPaintingCode(paintingStack);

            if (paintingCode == null) {
                return ActionResult.fail(paintingStack);
            }

            PaintingData canvasData = getPaintingData(paintingStack, player.level);

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
                CCanvasRequestViewPacket requestViewPacket = new CCanvasRequestViewPacket(paintingCode, hand);
                Zetter.LOG.debug("Sending request view packet: " + requestViewPacket);
                ZetterNetwork.simpleChannel.sendToServer(requestViewPacket);
            }
        }

        player.awardStat(Stats.ITEM_USED.get(this));

        return ActionResult.sidedSuccess(paintingStack, world.isClientSide());
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
        stack.getOrCreateTag().putString(NBT_TAG_CACHED_PAINTING_TITLE, paintingData.getPaintingName());

        int widthBlocks = paintingData.getWidth() / paintingData.getResolution().getNumeric();
        int heightBlocks = paintingData.getHeight() / paintingData.getResolution().getNumeric();

        final int[] size = new int[]{widthBlocks, heightBlocks};

        stack.getOrCreateTag().putIntArray(NBT_TAG_CACHED_BLOCK_SIZE, size);
        stack.getOrCreateTag().putInt(NBT_TAG_GENERATION, generation);
    }

    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        if (stack.hasTag()) {
            String authorName = getCachedAuthorName(stack);

            if (StringUtil.isNullOrEmpty(authorName)) {
                authorName = new TranslationTextComponent("item.zetter.painting.unknown").getString();
            }

            tooltip.add((new TranslationTextComponent("book.byAuthor", authorName)).withStyle(TextFormatting.GRAY));

            ITextComponent generationLabel = getGenerationLabel(stack);
            String stringSize = getStringSize(stack);

            if (StringUtil.isNullOrEmpty(stringSize)) {
                tooltip.add(new StringTextComponent(generationLabel.getString()).withStyle(TextFormatting.GRAY));
            } else {
                tooltip.add(new StringTextComponent(generationLabel.getString() + ", " + stringSize).withStyle(TextFormatting.GRAY));
            }
        }
    }

    @Override
    public ITextComponent getName(ItemStack stack) {
        if (stack.hasTag()) {
            String paintingName = getCachedPaintingName(stack);

            if (StringUtil.isNullOrEmpty(paintingName)) {
                if (StringUtil.isNullOrEmpty(getPaintingCode(stack))) {
                    return super.getName(stack);
                }

                paintingName = new TranslationTextComponent("item.zetter.painting.unnamed").getString();
            }

            if (!StringUtils.isNullOrEmpty(paintingName)) {
                return new StringTextComponent(paintingName);
            }
        }

        return new TranslationTextComponent(this.getDescriptionId(stack));
    }

    /**
     * Names are public for artist table preview
     * Getters for cached data
     */

    @Nullable
    public static String getPaintingCode(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();

        if (compoundNBT == null) {
            return null;
        }

        return compoundNBT.getString(NBT_TAG_PAINTING_CODE);
    }

    /**
     * Check if canvas has data
     * As all canvases initialized with id,
     * no id means canvas was not initialized
     *
     * @param stack
     * @return
     */
    public static boolean isEmpty(ItemStack stack) {
        String paintingCode = getPaintingCode(stack);

        return paintingCode == null;
    }

    /**
     *
     * @see {net.minecraft.world.item.MapItem#getCustomMapData(ItemStack, World)}
     * @param stack
     * @param world
     * @return
     */
    @Nullable
    public static PaintingData getPaintingData(ItemStack stack, World world) {
        Item painting = stack.getItem();

        if (painting instanceof PaintingItem || painting instanceof FrameItem) {
            String paintingCode = getPaintingCode(stack);

            if (paintingCode == null) {
                return null;
            }

            CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(world);

            return canvasTracker.getCanvasData(paintingCode);
        }

        return null;
    }

    @Nullable
    public static String getCachedAuthorName(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();

        if (compoundNBT == null) {
            return null;
        }

        return compoundNBT.getString(NBT_TAG_CACHED_AUTHOR_NAME);
    }

    @Nullable
    public static String getCachedPaintingName(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();

        if (compoundNBT == null) {
            return null;
        }

        return compoundNBT.getString(NBT_TAG_CACHED_PAINTING_TITLE);
    }

    @Nullable
    public static int[] getBlockSize(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();

        if (compoundNBT == null) {
            return null;
        }

        return compoundNBT.getIntArray(NBT_TAG_CACHED_BLOCK_SIZE);
    }

    @Nullable
    public static String getStringSize(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();

        if (compoundNBT == null) {
            return null;
        }

        int[] size = getBlockSize(stack);

        if (size == null || size.length != 2) {
            return new TranslationTextComponent("item.zetter.painting.size.unknown").getString();
        }

        return new TranslationTextComponent("item.zetter.painting.size", Integer.toString(size[0]), Integer.toString(size[1])).getString();
    }

    public static void setGeneration(ItemStack stack, int generation) {
        stack.getOrCreateTag().putInt(NBT_TAG_GENERATION, generation);
    }

    public static int getGeneration(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();

        if (compoundNBT == null) {
            return 0;
        }

        return compoundNBT.getInt(NBT_TAG_GENERATION);
    }

    public static ITextComponent getGenerationLabel(ItemStack stack) {
        int generation = getGeneration(stack);

        if (generation < 0 || generation > 2) {
            generation = 1;
        }

        return new TranslationTextComponent("item.zetter.painting.generation." + generation);
    }
}