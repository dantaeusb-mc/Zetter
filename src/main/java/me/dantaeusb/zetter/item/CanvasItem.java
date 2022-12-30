package me.dantaeusb.zetter.item;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.core.ClientHelper;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.core.ZetterNetwork;
import me.dantaeusb.zetter.network.packet.CCanvasRequestViewPacket;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.stats.Stats;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.security.InvalidParameterException;
import java.util.List;

public class CanvasItem extends Item
{
    public static final String NBT_TAG_CANVAS_CODE = "CanvasCode";
    public static final String NBT_TAG_CACHED_STRING_SIZE = "CachedStringSize";
    public static final String NBT_TAG_CACHED_BLOCK_SIZE = "CachedBlockSize";

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
            CanvasData canvasData = CanvasItem.getCanvasData(canvas, player.getLevel());

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
                return new TranslatableComponent("item.zetter.canvas.painted");
            }
        }

        return new TranslatableComponent("item.zetter.canvas.blank");
    }

    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        if (stack.hasTag()) {
            String stringSize = getCachedStringSize(stack);

            if (!StringUtil.isNullOrEmpty(stringSize)) {
                tooltip.add((Component.literal(stringSize)).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    /**
     * @see {net.minecraft.world.item.MapItem#getCustomMapData(ItemStack, Level)}
     */
    public static void createEmpty(ItemStack stack, Level world) {
        if (world.isClientSide()) {
            throw new InvalidParameterException("Create canvas called on client");
        }

        String canvasCode = createNewCanvasData(world);
        CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(world);

        CanvasData canvasData = canvasTracker.getCanvasData(canvasCode);
        assert canvasData != null;

        storeCanvasData(stack, canvasCode, canvasData);
    }

    public static void storeCanvasData(ItemStack stack, String canvasCode, CanvasData canvasData) {
        setCanvasCode(stack, canvasCode);

        int widthBlocks = canvasData.getWidth() / canvasData.getResolution().getNumeric();
        int heightBlocks = canvasData.getHeight() / canvasData.getResolution().getNumeric();

        final int[] size = new int[]{widthBlocks, heightBlocks};
        Component blockSizeString = (new TranslatableComponent("item.zetter.painting.size", Integer.toString(widthBlocks), Integer.toString(heightBlocks)));

        stack.getOrCreateTag().putIntArray(NBT_TAG_CACHED_BLOCK_SIZE, size);
        stack.getOrCreateTag().putString(NBT_TAG_CACHED_STRING_SIZE, blockSizeString.getString());
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

            if (canvasCode == null && world instanceof ServerLevel) {
                createEmpty(stack, world);
                canvasCode = getCanvasCode(stack);
            }

            CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(world);

            return canvasTracker.getCanvasData(canvasCode);
        }

        return null;
    }

    /**
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

    @Nullable
    public static String getCachedStringSize(ItemStack stack) {
        CompoundTag compoundNBT = stack.getTag();

        if (compoundNBT == null) {
            return null;
        }

        return compoundNBT.getString(NBT_TAG_CACHED_STRING_SIZE);
    }

    /**
     *
     * @see {net.minecraft.world.item.MapItem#createNewSavedData(Level, int, int, int, boolean, boolean, ResourceKey)}
     * @param level
     * @return
     */
    private static String createNewCanvasData(Level level) {
        if (level.isClientSide()) {
            throw new InvalidParameterException("Create canvas called on client");
        }

        CanvasServerTracker canvasTracker = (CanvasServerTracker) Helper.getLevelCanvasTracker(level);
        final int numericResolution = Helper.getResolution().getNumeric();

        CanvasData canvasData = CanvasData.BUILDER.createFresh(Helper.getResolution(), numericResolution, numericResolution);
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