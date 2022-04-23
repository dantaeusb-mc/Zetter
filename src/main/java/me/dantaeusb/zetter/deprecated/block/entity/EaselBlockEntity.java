package me.dantaeusb.zetter.deprecated.block.entity;

import me.dantaeusb.zetter.core.ZetterEntities;
import me.dantaeusb.zetter.core.ZetterBlockEntities;
import me.dantaeusb.zetter.deprecated.block.EaselBlock;
import me.dantaeusb.zetter.entity.item.EaselEntity;
import me.dantaeusb.zetter.entity.item.container.EaselContainer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public class EaselBlockEntity extends BlockEntity {
    private final EaselContainer easelContainer; // two items: canvas and palette

    private static final String EASEL_STORAGE_TAG = "storage";

    public EaselBlockEntity(BlockPos pos, BlockState state) {
        super(ZetterBlockEntities.EASEL_BLOCK_ENTITY.get(), pos, state);

        this.easelContainer = new EaselContainer();
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, EaselBlockEntity easelBlockEntity) {
        if (world.getBlockEntity(pos).getType() == ZetterBlockEntities.EASEL_BLOCK_ENTITY.get()) {
            final ItemStack canvasStack = easelBlockEntity.getEaselContainer().getCanvasStack();
            final ItemStack paletteStack = easelBlockEntity.getEaselContainer().getPaletteStack();

            BlockState blockState = Blocks.AIR.defaultBlockState();
            world.setBlock(pos, blockState, 1 & 2);

            float f = Mth.abs(180.0F - state.getValue(EaselBlock.FACING).toYRot());

            Vec3 vec3 = Vec3.atBottomCenterOf(pos);
            EaselEntity easelEntity = new EaselEntity(ZetterEntities.EASEL_ENTITY.get(), world);
            easelEntity.setPos(vec3);
            easelEntity.setYRot(f);

            world.addFreshEntity(easelEntity);

            easelEntity.getEaselContainer().setCanvasStack(canvasStack);
            easelEntity.getEaselContainer().setPaletteStack(paletteStack);
        }
    }

    // specific

    public EaselContainer getEaselContainer() {
        return this.easelContainer;
    }

    // render

    @Override
    public AABB getRenderBoundingBox()
    {
        return new AABB(this.getBlockPos(), this.getBlockPos().offset(1, 2, 1));
    }

    // NBT stack

    /**
     * Copied from forge helper as it was used in previous versions
     * @see ItemStackHandler#serializeNBT()
     */

    @Override
    public void saveAdditional(CompoundTag compoundTag)
    {
        CompoundTag easelStorage = this.easelContainer.serializeNBT();
        compoundTag.put(EASEL_STORAGE_TAG, easelStorage);
    }

    @Override
    public void load(CompoundTag compoundTag)
    {
        super.load(compoundTag);

        CompoundTag easelStorage = compoundTag.getCompound(EASEL_STORAGE_TAG);
        this.easelContainer.deserializeNBT(easelStorage);
    }

    // network stack

    @Override
    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        load(packet.getTag());
    }

    @Override
    public CompoundTag getUpdateTag()
    {
        CompoundTag nbtTagCompound = new CompoundTag();
        this.saveAdditional(nbtTagCompound);
        return nbtTagCompound;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag)
    {
        this.load(tag);
    }
}
