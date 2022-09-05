package me.dantaeusb.zetter.item;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.canvastracker.ICanvasTracker;
import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.stats.Stats;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.IItemRenderProperties;

import java.util.function.Consumer;

public class CanvasItem extends Item
{
    public static final String NBT_TAG_CANVAS_CODE = "CanvasCode";

    public CanvasItem() {
        super(new Properties().stacksTo(1).tab(CreativeModeTab.TAB_TOOLS));
    }

    // @todo: [HIGH] Canvas data could be null!!!
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        if (world.isClientSide()) {
            ItemStack canvas = player.getItemInHand(hand);
            Minecraft.getInstance().setScreen(
                    PaintingScreen.createScreenForCanvas(
                            player,
                            getCanvasCode(canvas),
                            getCanvasData(canvas, world),
                            hand
                    )
            );
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

    /**
     *
     * @see {@link net.minecraft.world.item.MapItem#getCustomMapData(ItemStack, Level)}
     * @param stack
     * @param world
     * @return
     */
    @Nullable
    public static CanvasData getCanvasData(ItemStack stack, @Nullable Level world) {
        Item canvas = stack.getItem();

        if (canvas instanceof CanvasItem) {
            return ((CanvasItem)canvas).getCustomCanvasData(stack, world);
        }

        return null;
    }

    /**
     * @see {@link net.minecraft.world.item.MapItem#getCustomMapData(ItemStack, Level)}
     */
    @Nullable
    protected CanvasData getCustomCanvasData(ItemStack stack, Level world) {
        CanvasData canvasData = null;
        String canvasCode = getCanvasCode(stack);

        if (canvasCode == null && world instanceof ServerLevel) {
            canvasCode = createNewCanvasData(world);
            setCanvasCode(stack, canvasCode);
        }

        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(world);

        if (canvasTracker != null) {
            canvasData = canvasTracker.getCanvasData(canvasCode, CanvasData.class);
        } else {
            Zetter.LOG.error("Unable to find CanvasTracker capability");
        }

        if (canvasData == null && world instanceof ServerLevel) {
            Zetter.LOG.error("Unable to find canvas data after creation");
        }

        return canvasData;
    }

    /**
     * @see {@link net.minecraft.world.item.MapItem#getMapId(ItemStack)}
     * @return
     */
    public static @Nullable String getCanvasCode(@Nullable ItemStack stack) {
        if (stack == null || !stack.is(ZetterItems.CANVAS.get())) {
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
     *
     * @see {@link net.minecraft.world.item.MapItem#getName(ItemStack)}
     * @param stack
     * @return
     */
    public static void setCanvasCode(ItemStack stack, String canvasCode) {
        stack.getOrCreateTag().putString(NBT_TAG_CANVAS_CODE, canvasCode);
    }

    private static void createAndStoreCanvasData(ItemStack stack, Level world) {
        String canvasCode = createNewCanvasData(world);
        setCanvasCode(stack, canvasCode);
    }

    /**
     *
     * @see {@link net.minecraft.world.item.MapItem#createNewSavedData(Level, int, int, int, boolean, boolean, ResourceKey)}
     * @param world
     * @return
     */
    private static String createNewCanvasData(Level world) {
        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(world);
        final int numericResolution = Helper.getResolution().getNumeric();

        CanvasData canvasData = CanvasData.createFresh(Helper.getResolution(), numericResolution, numericResolution);
        String canvasCode = CanvasData.getCanvasCode(canvasTracker.getNextCanvasId());
        canvasTracker.registerCanvasData(canvasCode, canvasData);

        return canvasCode;
    }

    /*@Override
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