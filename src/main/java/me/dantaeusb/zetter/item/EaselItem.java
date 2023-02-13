package me.dantaeusb.zetter.item;

import me.dantaeusb.zetter.core.ZetterEntities;
import me.dantaeusb.zetter.entity.item.EaselEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public class EaselItem extends Item
{
    public EaselItem() {
        super(new Properties().tab(ItemGroup.TAB_DECORATIONS));
    }

    public ActionResultType useOn(ItemUseContext context) {
        BlockPos blockPos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        BlockPos facePos = blockPos.relative(direction);
        ItemStack easelItem = context.getItemInHand();
        PlayerEntity player = context.getPlayer();

        if (direction == Direction.DOWN || (player != null && !this.canPlace(player, direction, easelItem, facePos))) {
            return ActionResultType.FAIL;
        } else {
            World world = context.getLevel();;
            BlockPos pos = context.getClickedPos();
            Vector3d vec3 = Vector3d.atBottomCenterOf(pos);
            AxisAlignedBB aabb = ZetterEntities.EASEL_ENTITY.get().getDimensions().makeBoundingBox(vec3.x(), vec3.y(), vec3.z());

            if (
                world.noCollision(null, aabb) &&
                world.getEntities(null, aabb).isEmpty()
            ) {
                if (world instanceof ServerWorld) {
                    /*EaselEntity easel = ModEntities.EASEL_ENTITY.create(
                            (ServerWorld)world, easelItem.getTag(), (ITextComponent)null, context.getPlayer(), pos, MobSpawnType.SPAWN_EGG, true, true
                    );*/

                    // @todo: [HIGH] Could have bounding box issues
                    EaselEntity easel = new EaselEntity(ZetterEntities.EASEL_ENTITY.get(), world);

                    if (easel == null) {
                        return ActionResultType.FAIL;
                    }

                    // Rotate properly
                    float f = (float) MathHelper.floor((MathHelper.wrapDegrees(context.getRotation() - 180.0F) + 22.5F) / 45.0F) * 45.0F;
                    easel.setPos(vec3.x, vec3.y, vec3.z);
                    easel.yRot = f;

                    world.addFreshEntity(easel);
                    easel.playPlacementSound();
                }

                easelItem.shrink(1);
                return ActionResultType.sidedSuccess(world.isClientSide);
            } else {
                return ActionResultType.FAIL;
            }
        }
    }

    protected boolean canPlace(PlayerEntity playerIn, Direction directionIn, ItemStack itemStackIn, BlockPos posIn) {
        return directionIn.getAxis().isVertical() && playerIn.mayUseItemAt(posIn, directionIn, itemStackIn);
    }
}