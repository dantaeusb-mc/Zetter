package me.dantaeusb.zetter.capability.paintingregistry;

import me.dantaeusb.zetter.Zetter;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PaintingRegistryStorage implements Capability.IStorage<PaintingRegistry> {
    private static final String NBT_TAG_PAINTING_LIST = "PaintingCanvasCodeList";

    @Override
    public INBT writeNBT(Capability<PaintingRegistry> capability, PaintingRegistry instance, @Nullable Direction side) {
        CompoundNBT compound = new CompoundNBT();

        StringBuilder canvasCodeListBuilder = new StringBuilder();

        for (String canvasCode : instance.getPaintingCanvasCodes()) {
            canvasCodeListBuilder.append(canvasCode);
            canvasCodeListBuilder.append(PaintingRegistry.SEPARATOR);
        }

        compound.putByteArray(NBT_TAG_PAINTING_LIST, canvasCodeListBuilder.toString().getBytes(StandardCharsets.UTF_8));

        return compound;
    }

    @Override
    public void readNBT(Capability<PaintingRegistry> capability, PaintingRegistry instance, Direction side, @Nullable INBT tag) {
        List<String> paintingCanvasCodeList = new ArrayList<>();

        if (tag.getType() == CompoundNBT.TYPE) {
            CompoundNBT compoundTag = (CompoundNBT) tag;

            if (!compoundTag.contains(NBT_TAG_PAINTING_LIST)) {
                return;
            }

            ByteBuffer canvasCodesBuffer = ByteBuffer.wrap(compoundTag.getByteArray(NBT_TAG_PAINTING_LIST));
            int lastZeroBytePosition = 0;

            while (canvasCodesBuffer.hasRemaining()) {
                byte check = canvasCodesBuffer.get();

                if (check != PaintingRegistry.BYTE_SEPARATOR) {
                    continue;
                }

                // Do not get byte[], it'll share the reference to the full array I suppose
                final int lastPos = canvasCodesBuffer.position();
                final int lastLimit = canvasCodesBuffer.limit();

                canvasCodesBuffer.position(lastZeroBytePosition);
                canvasCodesBuffer.limit(lastPos - 1);
                ByteBuffer canvasCodeBuffer = canvasCodesBuffer.slice();
                canvasCodesBuffer.limit(lastLimit);
                canvasCodesBuffer.position(lastPos);

                String canvasCode = StandardCharsets.UTF_8.decode(canvasCodeBuffer).toString();

                if (canvasCode.isEmpty() || canvasCode.contains(PaintingRegistry.SEPARATOR)) {
                    Zetter.LOG.warn("Cannot deserialize canvas code from painting registry");
                } else {
                    paintingCanvasCodeList.add(canvasCode);
                }

                lastZeroBytePosition = canvasCodesBuffer.position();
            }
        }

        instance.setPaintingCanvasCodes(paintingCanvasCodeList);
    }
}