package me.dantaeusb.zetter.item;

import me.dantaeusb.zetter.painting.Tool;
import me.dantaeusb.zetter.painting.parameters.AbstractToolParameters;
import me.dantaeusb.zetter.painting.parameters.SpongeParameters;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A piece of sponge for wiping a blackboard down.
 * A soaked sponge lifts chalk off cleanly; as it dries it starts pushing the chalk
 * around instead of taking it away.
 */
public class SpongeItem extends Item implements BlackboardImplement {
    /**
     * Water it holds, counted in the same painted pixels chalk is counted in.
     */
    public static final int CAPACITY = 16384;

    /**
     * Below this much water left, the sponge reads as dry: it takes its own name and
     * icon, and it will smear rather than wipe.
     */
    public static final float DRY_THRESHOLD = 0.1f;

    /**
     * The bar is water rather than wear, and green-to-red would say the opposite
     */
    private static final int BAR_COLOR = 0x3C6DF0;

    public SpongeItem(Properties properties) {
        super(properties.durability(CAPACITY));
    }

    /**
     * How much water is left, from none to full
     *
     * @param stack
     * @return
     */
    public static float getWetness(ItemStack stack) {
        return 1f - (float) stack.getDamageValue() / stack.getMaxDamage();
    }

    public static boolean isDry(ItemStack stack) {
        return getWetness(stack) <= DRY_THRESHOLD;
    }

    public static void soak(ItemStack stack) {
        stack.setDamageValue(0);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(isDry(stack) ? this.getDescriptionId() + ".dry" : this.getDescriptionId());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOR;
    }

    /**
     * Dipped in open water.
     *
     * @param level
     * @param player
     * @param hand
     * @return
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        final ItemStack spongeStack = player.getItemInHand(hand);

        if (spongeStack.getDamageValue() == 0) {
            return InteractionResultHolder.pass(spongeStack);
        }

        final BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(spongeStack);
        }

        final FluidState fluidState = level.getFluidState(hitResult.getBlockPos());

        if (!fluidState.is(FluidTags.WATER) || !fluidState.isSource()) {
            return InteractionResultHolder.pass(spongeStack);
        }

        if (!level.isClientSide()) {
            soak(spongeStack);
        }

        level.playSound(player, player.blockPosition(), SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0f, 1.0f);
        player.awardStat(Stats.ITEM_USED.get(this));

        return InteractionResultHolder.sidedSuccess(spongeStack, level.isClientSide());
    }

    /**
     * Dipped in a cauldron, which gives up a layer for it the way it does for
     * anything else washed in one
     *
     * @param context
     * @return
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        final ItemStack spongeStack = context.getItemInHand();

        if (spongeStack.getDamageValue() == 0) {
            return InteractionResult.PASS;
        }

        final Level level = context.getLevel();
        final BlockPos blockPos = context.getClickedPos();
        final BlockState blockState = level.getBlockState(blockPos);

        if (!blockState.is(Blocks.WATER_CAULDRON)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            soak(spongeStack);
            LayeredCauldronBlock.lowerFillLevel(blockState, level, blockPos);
        }

        level.playSound(context.getPlayer(), blockPos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public Tool getTool() {
        return Tool.SPONGE;
    }

    @Override
    public AbstractToolParameters getToolParameters(ItemStack stack) {
        return new SpongeParameters(SpongeParameters.DEFAULT_SIZE, getWetness(stack));
    }

    @Override
    public int getToolColor(ItemStack stack) {
        return 0;
    }

    @Override
    public boolean isWornOut(ItemStack stack) {
        return false;
    }
}
