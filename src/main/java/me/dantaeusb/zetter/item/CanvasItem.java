package me.dantaeusb.zetter.item;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.core.ClientHelper;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.core.ZetterNetwork;
import me.dantaeusb.zetter.network.packet.CCanvasRequestViewPacket;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.security.InvalidParameterException;
import java.util.List;

public class CanvasItem extends Item
{
    /*
     * How to find actual painting data
     */
    public static final String NBT_TAG_CANVAS_CODE = "CanvasCode";

    /*
     * For placement check and tooltips
     */
    public static final String NBT_TAG_CACHED_BLOCK_SIZE = "CachedBlockSize";

    /*
     * Because player's settings might've changed,
     * and we need resolution for crafting, i.e. copying a painting
     */
    public static final String NBT_TAG_CACHED_RESOLUTION = "CachedResolution";

    public CanvasItem(Properties properties) {
        super(properties);
    }

    // @todo: [HIGH] Canvas data could be null!!!
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        if (world.isClientSide()) {
            ItemStack canvas = player.getItemInHand(hand);

            if (isEmpty(canvas)) {
                return InteractionResultHolder.consume(canvas);
            }

            String canvasCode = getCanvasCode(canvas);
            CanvasData canvasData = CanvasItem.getCanvasData(canvas, player.level());

            if (canvasData != null) {
                // If data is loaded, just show screen
                ClientHelper.openCanvasScreen(
                        player,
                        canvasCode,
                        canvasData,
                        hand
                );
            } else {
                // If data is not loaded, request and show screen after
                CCanvasRequestViewPacket requestViewPacket = new CCanvasRequestViewPacket(canvasCode, hand);
                Zetter.LOG.debug("Sending request view packet: " + requestViewPacket);
                ZetterNetwork.simpleChannel.sendToServer(requestViewPacket);
            }

        }
        ItemStack itemstack = player.getItemInHand(hand);
        player.openItemGui(itemstack, hand);

        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(itemstack, world.isClientSide());
    }

    @Override
    public Component getName(ItemStack stack) {
        if (stack.hasTag()) {
            String canvasCode = getCanvasCode(stack);

            if (!StringUtil.isNullOrEmpty(canvasCode)) {
                return Component.translatable("item.zetter.canvas.painted");
            }
        }

        return Component.translatable("item.zetter.canvas.blank");
    }

    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        String stringSize = getStringSize(stack);

        if (!StringUtil.isNullOrEmpty(stringSize)) {
            tooltip.add((Component.literal(stringSize)).withStyle(ChatFormatting.GRAY));
        }
    }

    /**
     * @see {net.minecraft.world.item.MapItem#getCustomMapData(ItemStack, Level)}
     */
    public static CanvasData createEmpty(ItemStack stack, AbstractCanvasData.Resolution resolution, int widthBlock, int heightBlock, Level world) {
        if (world.isClientSide()) {
            throw new InvalidParameterException("Create canvas called on client");
        }

        String canvasCode = createNewCanvasData(resolution, widthBlock, heightBlock, world);
        CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(world);

        CanvasData canvasData = canvasTracker.getCanvasData(canvasCode);
        assert canvasData != null;

        storeCanvasData(stack, canvasCode, canvasData);

        return canvasData;
    }

    public static void storeCanvasData(ItemStack stack, String canvasCode, CanvasData canvasData) {
        setCanvasCode(stack, canvasCode);

        int widthBlocks = canvasData.getWidth() / canvasData.getResolution().getNumeric();
        int heightBlocks = canvasData.getHeight() / canvasData.getResolution().getNumeric();

        CanvasItem.setBlockSize(stack, widthBlocks, heightBlocks);
        stack.getOrCreateTag().putInt(NBT_TAG_CACHED_RESOLUTION, canvasData.getResolution().getNumeric());
    }

    /**
     * Compound means that this canvas consists of
     * several combined canvases (larger than 1x1 canvas)
     * @param stack
     * @return
     */
    public static boolean isCompound(ItemStack stack) {
        int[] size = getBlockSize(stack);

        // No NBT data
        if (size != null && size.length != 0) {
            return size[0] != 1 || size[1] != 1;
        }

        return false;
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
        String canvasCode = getCanvasCode(stack);

        return canvasCode == null;
    }

    /**
     *
     * @see {net.minecraft.world.item.MapItem#getCustomMapData(ItemStack, Level)}
     * @param stack
     * @param world
     * @return
     */
    @Nullable
    public static CanvasData getCanvasData(ItemStack stack, Level world) {
        Item canvas = stack.getItem();

        if (canvas instanceof CanvasItem) {
            String canvasCode = getCanvasCode(stack);

            CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(world);

            return canvasTracker.getCanvasData(canvasCode);
        }

        return null;
    }

    /**
     * Canvas code only exists when there's data on canvas
     * It should be able to be displayed even with no data,
     * just fallback to the default
     *
     * This method however does not return the default,
     * as it is item's logic. This case should be handled
     * by whatever requests the code.
     *
     * @see {@link net.minecraft.world.item.MapItem#getMapId(ItemStack)}
     * @return
     */
    public static @Nullable String getCanvasCode(ItemStack stack) {
        if (!stack.is(ZetterItems.CANVAS.get())) {
            return null;
        }

        CompoundTag compoundNBT = stack.getTag();

        String canvasCode = null;

        if (compoundNBT != null && compoundNBT.contains(NBT_TAG_CANVAS_CODE)) {
            canvasCode = compoundNBT.getString(NBT_TAG_CANVAS_CODE);
        }

        return canvasCode;
    }

    /**
     * In one case, when we loaded only easel entity,
     * we would like to sync only canvas code for that
     * entity, and nothing more. This way, we're setting
     * only canvas code directly, instead of setting all
     * data from CanvasData
     *
     * @param stack
     */
    public static void setCanvasCode(ItemStack stack, String canvasCode) {
        stack.getOrCreateTag().putString(NBT_TAG_CANVAS_CODE, canvasCode);
    }

    /**
     * @todo: It is fallback, better to save prefix and id separately
     * @param stack
     * @return
     */
    public static @Nullable Integer getCanvasId(@Nullable ItemStack stack) {
        String canvasCode = getCanvasCode(stack);

        if (canvasCode == null) {
            return null;
        }

        return Integer.parseInt(canvasCode.substring(CanvasData.CODE_PREFIX.length()));
    }

    public static void setBlockSize(ItemStack stack, int widthBlocks, int heightBlocks) {
        final int[] size = new int[]{widthBlocks, heightBlocks};

        stack.getOrCreateTag().putIntArray(NBT_TAG_CACHED_BLOCK_SIZE, size);
    }

    /**
     * Keeps track of canvas size even if it's not initialized
     * with color data
     *
     * It's not compatible with other items as it has
     * a fallback that makes sense only for non-initialized canvas
     * @param stack
     * @return
     */
    @Nullable
    public static int[] getBlockSize(ItemStack stack) {
        if (!stack.is(ZetterItems.CANVAS.get())) {
            return null;
        }

        CompoundTag compoundNBT = stack.getTag();

        if (compoundNBT == null || !compoundNBT.contains(NBT_TAG_CACHED_BLOCK_SIZE)) {
            return new int[]{1, 1};
        }

        return compoundNBT.getIntArray(NBT_TAG_CACHED_BLOCK_SIZE);
    }

    public static String getStringSize(ItemStack stack) {
        int[] size = getBlockSize(stack);

        if (size == null || size.length != 2) {
            return Component.translatable("item.zetter.painting.size", "1", "1").getString();
        }

        return Component.translatable("item.zetter.painting.size", Integer.toString(size[0]), Integer.toString(size[1])).getString();
    }

    public static int getResolution(ItemStack stack) {
        CompoundTag compoundNBT = stack.getTag();

        if (compoundNBT == null) {
            return Helper.getResolution().getNumeric();
        }

        if (!compoundNBT.contains(NBT_TAG_CACHED_RESOLUTION)) {
            compoundNBT.putInt(NBT_TAG_CACHED_RESOLUTION, Helper.getResolution().getNumeric());
        }

        return compoundNBT.getInt(NBT_TAG_CACHED_RESOLUTION);
    }

    /**
     *
     * @see {net.minecraft.world.item.MapItem#createNewSavedData(Level, int, int, int, boolean, boolean, ResourceKey)}
     * @param level
     * @return
     */
    private static String createNewCanvasData(AbstractCanvasData.Resolution resolution, int widthBlock, int heightBlock, Level level) {
        if (level.isClientSide()) {
            throw new InvalidParameterException("Create canvas called on client");
        }

        CanvasServerTracker canvasTracker = (CanvasServerTracker) Helper.getLevelCanvasTracker(level);

        CanvasData canvasData = CanvasData.BUILDER.createFresh(
            resolution,
            widthBlock * resolution.getNumeric(),
            heightBlock * resolution.getNumeric()
        );

        String canvasCode = CanvasData.getCanvasCode(canvasTracker.getFreeCanvasId());
        canvasTracker.registerCanvasData(canvasCode, canvasData);

        return canvasCode;
    }

    /*
    Alternative renderer
    @Override
    public void initializeClient(Consumer<IItemRenderProperties> consumer)
    {
        if (Minecraft.getInstance() == null) return;

        consumer.accept(new IItemRenderProperties()
        {
            private final CanvasItemRenderer stackRenderer = Minecraft.getInstance() != null ? new CanvasItemRenderer(
                    Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                    Minecraft.getInstance().getEntityModels()
            ) : null;

            @Override
            public BlockEntityWithoutLevelRenderer getItemStackRenderer() { return stackRenderer; }
        });
    }*/
}