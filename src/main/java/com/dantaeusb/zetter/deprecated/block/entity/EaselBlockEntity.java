package com.dantaeusb.zetter.deprecated.block.entity;

import com.dantaeusb.zetter.core.ModEntities;
import com.dantaeusb.zetter.deprecated.block.EaselBlock;
import com.dantaeusb.zetter.entity.item.EaselEntity;
import com.dantaeusb.zetter.core.ModBlockEntities;
import com.dantaeusb.zetter.tileentity.container.EaselContainer;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
        super(ModBlockEntities.EASEL_BLOCK_ENTITY, pos, state);

        this.easelContainer = new EaselContainer();
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, EaselBlockEntity easelBlockEntity) {
        if (world.getBlockEntity(pos).getType() == ModBlockEntities.EASEL_BLOCK_ENTITY) {
            final ItemStack canvasStack = easelBlockEntity.getEaselContainer().getCanvasStack();
            final ItemStack paletteStack = easelBlockEntity.getEaselContainer().getPaletteStack();

            BlockState blockState = Blocks.AIR.defaultBlockState();
            world.setBlock(pos, blockState, 1 & 2);

            float f = Mth.abs(180.0F - state.getValue(EaselBlock.FACING).toYRot());

            Vec3 vec3 = Vec3.atBottomCenterOf(pos);
            EaselEntity easelEntity = new EaselEntity(ModEntities.EASEL_ENTITY, world);
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
    public CompoundTag save(CompoundTag compoundTag)
    {
        super.save(compoundTag);

        CompoundTag easelStorage = new CompoundTag();
        ListTag easelContainerItems = this.easelContainer.createTag();
        easelStorage.put("Items", easelContainerItems);

        compoundTag.put(EASEL_STORAGE_TAG, easelStorage);

        return compoundTag;
    }

    @Override
    public void load(CompoundTag compoundTag)
    {
        super.load(compoundTag);

        CompoundTag easelStorage = compoundTag.getCompound(EASEL_STORAGE_TAG);
        ListTag easelContainerItems = easelStorage.getList("Items", Tag.TAG_COMPOUND);

        this.easelContainer.fromTag(easelContainerItems);
    }

    // network stack

    @Override
    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {
        CompoundTag nbtTagCompound = new CompoundTag();
        save(nbtTagCompound);

        int tileEntityType = 42;
        return new ClientboundBlockEntityDataPacket(this.worldPosition, tileEntityType, nbtTagCompound);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        load(packet.getTag());
    }

    @Override
    public CompoundTag getUpdateTag()
    {
        CompoundTag nbtTagCompound = new CompoundTag();
        save(nbtTagCompound);
        return nbtTagCompound;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag)
    {
        this.load(tag);
    }
}
