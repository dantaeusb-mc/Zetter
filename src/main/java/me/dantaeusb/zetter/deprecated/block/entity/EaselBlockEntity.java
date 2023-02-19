package me.dantaeusb.zetter.deprecated.block.entity;

import me.dantaeusb.zetter.core.ZetterBlockEntities;
import me.dantaeusb.zetter.core.ZetterEntities;
import me.dantaeusb.zetter.deprecated.block.EaselBlock;
import me.dantaeusb.zetter.entity.item.EaselEntity;
import me.dantaeusb.zetter.entity.item.container.EaselContainer;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public class EaselBlockEntity extends TileEntity implements ITickableTileEntity {
    private final EaselContainer easelContainer; // two items: canvas and palette

    private static final String NBT_TAG_EASEL_STORAGE = "storage";

    public EaselBlockEntity() {
        super(ZetterBlockEntities.EASEL_BLOCK_ENTITY.get());

        this.easelContainer = new EaselContainer();
    }

    public void tick() {
        if (this.getType() == ZetterBlockEntities.EASEL_BLOCK_ENTITY.get()) {
            final ItemStack canvasStack = this.getEaselContainer().getCanvasStack();
            final ItemStack paletteStack = this.getEaselContainer().getPaletteStack();

            BlockState state = this.getBlockState();
            BlockState airState = Blocks.AIR.defaultBlockState();
            this.level.setBlock(this.getBlockPos(), airState, 1 & 2);

            float f = MathHelper.abs(180.0F - state.getValue(EaselBlock.FACING).toYRot());

            Vector3d vec3 = Vector3d.atBottomCenterOf(this.getBlockPos());
            EaselEntity easelEntity = new EaselEntity(ZetterEntities.EASEL_ENTITY.get(), this.level);
            easelEntity.setPos(vec3.x, vec3.y, vec3.z);
            easelEntity.yRot = f;

            this.level.addFreshEntity(easelEntity);

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
    public AxisAlignedBB getRenderBoundingBox()
    {
        return new AxisAlignedBB(this.getBlockPos(), this.getBlockPos().offset(1, 2, 1));
    }

    // NBT stack

    /**
     * Copied from forge helper as it was used in previous versions
     * @see ItemStackHandler#serializeNBT()
     */

    @Override
    public CompoundNBT save(CompoundNBT compoundTag)
    {
        super.save(compoundTag); // The super call is required to save and load the tileEntity's location

        CompoundNBT inventoryNBT = this.easelContainer.serializeNBT();
        compoundTag.put(NBT_TAG_EASEL_STORAGE, inventoryNBT);

        return compoundTag;
    }

    @Override
    public void load(BlockState blockState, CompoundNBT compoundTag)
    {
        super.load(blockState, compoundTag);

        CompoundNBT easelStorage = compoundTag.getCompound(NBT_TAG_EASEL_STORAGE);
        this.easelContainer.deserializeNBT(easelStorage);
    }

    // network stack

    @Override
    @Nullable
    public SUpdateTileEntityPacket getUpdatePacket()
    {
        CompoundNBT nbtTagCompound = new CompoundNBT();
        save(nbtTagCompound);

        int tileEntityType = 42;
        return new SUpdateTileEntityPacket(this.worldPosition, tileEntityType, nbtTagCompound);
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket packet) {
        BlockState blockState = level.getBlockState(worldPosition);
        load(blockState, packet.getTag());
    }

    @Override
    public CompoundNBT getUpdateTag()
    {
        CompoundNBT nbtTagCompound = new CompoundNBT();
        save(nbtTagCompound);
        return nbtTagCompound;
    }

    @Override
    public void handleUpdateTag(BlockState blockState, CompoundNBT tag)
    {
        this.load(blockState, tag);
    }
}
