package me.dantaeusb.zetter.item;

import me.dantaeusb.zetter.core.ZetterEntities;
import me.dantaeusb.zetter.entity.item.EaselEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class EaselItem extends Item
{
    public EaselItem() {
        super(new Properties());
    }

    public InteractionResult useOn(UseOnContext context) {
        BlockPos blockPos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        BlockPos facePos = blockPos.relative(direction);
        ItemStack easelItem = context.getItemInHand();
        Player player = context.getPlayer();

        if (direction == Direction.DOWN || (player != null && !this.canPlace(player, direction, easelItem, facePos))) {
            return InteractionResult.FAIL;
        } else {
            Level world = context.getLevel();
            BlockPlaceContext placeContext = new BlockPlaceContext(context);
            BlockPos pos = placeContext.getClickedPos();
            Vec3 vec3 = Vec3.atBottomCenterOf(pos);
            AABB aabb = ZetterEntities.EASEL_ENTITY.get().getDimensions().makeBoundingBox(vec3.x(), vec3.y(), vec3.z());

            if (
                world.noCollision(null, aabb) &&
                world.getEntities(null, aabb).isEmpty()
            ) {
                if (world instanceof ServerLevel) {
                    /*EaselEntity easel = ModEntities.EASEL_ENTITY.create(
                            (ServerLevel)world, easelItem.getTag(), (Component)null, context.getPlayer(), pos, MobSpawnType.SPAWN_EGG, true, true
                    );*/

                    EaselEntity easel = new EaselEntity(ZetterEntities.EASEL_ENTITY.get(), world);

                    if (easel == null) {
                        return InteractionResult.FAIL;
                    }

                    // Rotate properly
                    float f = (float) Mth.floor((Mth.wrapDegrees(context.getRotation() - 180.0F) + 22.5F) / 45.0F) * 45.0F;
                    easel.setPos(vec3);
                    easel.setYRot(f);

                    world.addFreshEntity(easel);

                    world.playSound(null, easel.getX(), easel.getY(), easel.getZ(), SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75F, 0.8F);
                    easel.gameEvent(GameEvent.ENTITY_PLACE, context.getPlayer());
                }

                easelItem.shrink(1);
                return InteractionResult.sidedSuccess(world.isClientSide);
            } else {
                return InteractionResult.FAIL;
            }
        }
    }

    protected boolean canPlace(Player playerIn, Direction directionIn, ItemStack itemStackIn, BlockPos posIn) {
        return directionIn.getAxis().isVertical() && playerIn.mayUseItemAt(posIn, directionIn, itemStackIn);
    }
}