package me.dantaeusb.zetter.item;

import me.dantaeusb.zetter.core.ZetterEntities;
import me.dantaeusb.zetter.entity.item.AbstractBoardEntity;
import me.dantaeusb.zetter.entity.item.StandBoardEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class StandBoardItem extends Item {
    private final AbstractBoardEntity.Materials material;

    public StandBoardItem(Properties properties, AbstractBoardEntity.Materials material) {
        super(properties);

        this.material = material;
    }

    public AbstractBoardEntity.Materials getMaterial() {
        return this.material;
    }

    public InteractionResult useOn(UseOnContext context) {
        final Direction direction = context.getClickedFace();
        final BlockPos facePos = context.getClickedPos().relative(direction);
        final ItemStack standBoardStack = context.getItemInHand();
        final Player player = context.getPlayer();

        // Stands on a floor, like an easel
        if (direction != Direction.UP) {
            return InteractionResult.FAIL;
        }

        if (player != null && !player.mayUseItemAt(facePos, direction, standBoardStack)) {
            return InteractionResult.FAIL;
        }

        final Level level = context.getLevel();
        final Vec3 vec3 = Vec3.atBottomCenterOf(facePos);
        final AABB aabb = ZetterEntities.STANDING_BOARD_ENTITY.get().getDimensions().makeBoundingBox(vec3.x(), vec3.y(), vec3.z());

        if (!level.noCollision(null, aabb) || !level.getEntities(null, aabb).isEmpty()) {
            return InteractionResult.FAIL;
        }

        if (level instanceof ServerLevel) {
            final StandBoardEntity standBoard = new StandBoardEntity(ZetterEntities.STANDING_BOARD_ENTITY.get(), level);

            // Facing the player, rounded to the nearest eighth, the same as an easel
            final float rotation = (float) Mth.floor((Mth.wrapDegrees(context.getRotation() - 180.0F) + 22.5F) / 45.0F) * 45.0F;

            standBoard.setMaterial(this.material);
            standBoard.setPos(vec3);
            standBoard.setYRot(rotation);

            level.addFreshEntity(standBoard);

            level.playSound(null, standBoard.getX(), standBoard.getY(), standBoard.getZ(), SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75F, 0.8F);
            standBoard.gameEvent(GameEvent.ENTITY_PLACE, player);
        }

        standBoardStack.shrink(1);

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
