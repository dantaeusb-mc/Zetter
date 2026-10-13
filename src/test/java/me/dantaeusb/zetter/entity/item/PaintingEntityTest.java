package me.dantaeusb.zetter.entity.item;

import io.netty.buffer.Unpooled;
import me.dantaeusb.zetter.core.ZetterEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class PaintingEntityTest {
    private PaintingEntity painting(BlockPos anchor, Direction direction, int width, int height) {
        return new PaintingEntity(mock(Level.class), anchor, direction,
                PaintingEntity.Materials.OAK, false, "position_probe", new int[]{width, height}, 0);
    }

    @Test
    void trackingUpdatesKeepAnchorCenterAndBoundsForEverySizeAndDirection() {
        int[][] sizes = {{1, 1}, {1, 2}, {2, 1}, {2, 2}, {3, 3}, {4, 4}};
        for (BlockPos anchor : new BlockPos[]{new BlockPos(20, 70, 20), new BlockPos(-20, -10, -20)}) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                for (int[] size : sizes) {
                    PaintingEntity painting = painting(anchor, direction, size[0], size[1]);
                    Vec3 center = painting.position();
                    AABB bounds = painting.getBoundingBox();
                    Entity receiver = painting;
                    for (int repeat = 0; repeat < 10; repeat++) {
                        Vec3 tracked = painting.trackingPosition();
                        assertEquals(Vec3.atLowerCornerOf(anchor), tracked);
                        receiver.lerpTo(tracked.x, tracked.y, tracked.z, painting.getYRot(), painting.getXRot(), 3);
                        assertEquals(anchor, painting.getPos());
                        assertEquals(center, painting.position());
                        assertEquals(bounds, painting.getBoundingBox());
                    }
                    BlockPos moved = anchor.offset(-3, 2, 4);
                    receiver.lerpTo(moved.getX(), moved.getY(), moved.getZ(), 0, 0, 3);
                    assertEquals(moved, painting.getPos());
                    assertEquals(center.add(-3, 2, 4), painting.position());
                }
            }
        }
    }

    @Test
    void spawnPacketAndAdditionalDataUseTheSameAnchor() {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            PaintingEntity original = painting(new BlockPos(-20, 70, 20), direction, 2, 2);
            ClientboundAddEntityPacket packet = (ClientboundAddEntityPacket) original.getAddEntityPacket(null);
            assertEquals(-20, packet.getX());
            assertEquals(70, packet.getY());
            assertEquals(20, packet.getZ());
            assertEquals(direction.get3DDataValue(), packet.getData());
            PaintingEntity received = new PaintingEntity(ZetterEntities.FRAMED_PAINTING_ENTITY.get(), mock(Level.class));
            received.recreateFromPacket(packet);
            RegistryFriendlyByteBuf data = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            try {
                original.writeSpawnData(data);
                received.readSpawnData(data);
                assertEquals(original.getPos(), received.getPos());
                assertEquals(original.position(), received.position());
                assertEquals(original.getBoundingBox(), received.getBoundingBox());
                assertEquals(original.trackingPosition(), received.trackingPosition());
            } finally {
                data.release();
            }
        }
    }
}
