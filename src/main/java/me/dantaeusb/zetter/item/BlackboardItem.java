package me.dantaeusb.zetter.item;

import me.dantaeusb.zetter.core.ZetterEntities;
import me.dantaeusb.zetter.entity.item.AbstractBoardEntity;
import me.dantaeusb.zetter.entity.item.BlackboardEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BlackboardItem extends Item {
    private final AbstractBoardEntity.Materials material;

    public BlackboardItem(Properties properties, AbstractBoardEntity.Materials material) {
        super(properties);

        this.material = material;
    }

    public AbstractBoardEntity.Materials getMaterial() {
        return this.material;
    }

    public InteractionResult useOn(UseOnContext context) {
        final Direction direction = context.getClickedFace();
        final BlockPos facePos = context.getClickedPos().relative(direction);
        final ItemStack blackboardStack = context.getItemInHand();
        final Player player = context.getPlayer();

        // A board hangs on a wall, so a floor or a ceiling is nothing to hang it from
        if (!direction.getAxis().isHorizontal()) {
            return InteractionResult.FAIL;
        }

        if (player != null && !this.canPlace(player, direction, blackboardStack, facePos)) {
            return InteractionResult.FAIL;
        }

        final Level level = context.getLevel();

        /*
         * Flat on the wall it was clicked, so the face picks the rotation outright
         * rather than rounding off where the player happened to be standing. The back
         * of the board sits half a block behind its origin, which lands it exactly on
         * the boundary between the wall and the block in front of it.
         */
        final Vec3 vec3 = Vec3.atBottomCenterOf(facePos);
        final float rotation = direction.toYRot();
        final AABB aabb = BlackboardEntity.makeBoundingBox(vec3, rotation);

        if (!level.noCollision(null, aabb) || !level.getEntities(null, aabb).isEmpty()) {
            return InteractionResult.FAIL;
        }

        // Checked here too, or the board would hang there for a few seconds and fall
        if (!BlackboardEntity.isSupported(level, facePos, rotation)) {
            return InteractionResult.FAIL;
        }

        if (level instanceof ServerLevel) {
            final BlackboardEntity blackboard = new BlackboardEntity(ZetterEntities.BLACKBOARD_ENTITY.get(), level);

            blackboard.setMaterial(this.material);
            blackboard.setPos(vec3);
            blackboard.setYRot(rotation);

            level.addFreshEntity(blackboard);

            level.playSound(null, blackboard.getX(), blackboard.getY(), blackboard.getZ(), SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75F, 0.8F);
            blackboard.gameEvent(GameEvent.ENTITY_PLACE, player);
        }

        blackboardStack.shrink(1);

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    protected boolean canPlace(Player player, Direction direction, ItemStack stack, BlockPos pos) {
        return player.mayUseItemAt(pos, direction, stack);
    }
}
