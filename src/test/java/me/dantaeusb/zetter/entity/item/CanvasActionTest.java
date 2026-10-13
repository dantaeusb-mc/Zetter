package me.dantaeusb.zetter.entity.item;

import me.dantaeusb.zetter.entity.item.state.representation.CanvasAction;
import me.dantaeusb.zetter.painting.Tools;
import me.dantaeusb.zetter.painting.parameters.PencilParameters;
import me.dantaeusb.zetter.painting.parameters.AbstractToolParameters;
import me.dantaeusb.zetter.painting.parameters.BrushParameters;
import me.dantaeusb.zetter.painting.parameters.BucketParameters;
import me.dantaeusb.zetter.painting.pipes.DitheringPipe;
import me.dantaeusb.zetter.painting.pipes.BlendingPipe;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.network.packet.CCanvasActionPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.ArrayDeque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CanvasActionTest {
    @Test
    void parameterChangesStartANewAction() throws Exception {
        UUID author = UUID.randomUUID();
        PencilParameters original = new PencilParameters();
        CanvasAction action = new CanvasAction(author, Tools.PENCIL, 0xFF000000, original.clone());
        assertTrue(action.canContinue(UUID.fromString(author.toString()), Tools.PENCIL, 0xFF000000, original));
        original.setSize(3);
        assertFalse(action.canContinue(author, Tools.PENCIL, 0xFF000000, original));
        original.setSize(1);
        original.setIntensity(0.5f);
        assertFalse(action.canContinue(author, Tools.PENCIL, 0xFF000000, original));
    }

    @Test
    void subActionTimeAndCoordinatesRoundTrip() {
        for (int time : new int[]{0, 127, 128, 255, 256, 5000, 65535}) {
            ByteBuffer bytes = ByteBuffer.allocate(11);
            CanvasAction.CanvasSubAction.writeToBuffer(new CanvasAction.CanvasSubAction(time, -1.25f, 31.75f), bytes);
            bytes.flip();
            CanvasAction.CanvasSubAction decoded = CanvasAction.CanvasSubAction.readFromBuffer(bytes);
            assertEquals(time, decoded.time);
            assertEquals(-1.25f, decoded.posX);
            assertEquals(31.75f, decoded.posY);
        }
    }

    @Test
    void replayStreamsHaveIndependentCursors() {
        CanvasAction action = new CanvasAction(UUID.randomUUID(), Tools.PENCIL, 0xFF000000, new PencilParameters());
        action.addFrame(1, 2);
        action.addFrame(3, 4);
        action.commit();
        var first = action.getSubActionStream().iterator();
        var second = action.getSubActionStream().iterator();
        assertEquals(1, first.next().posX);
        assertEquals(1, second.next().posX);
        assertEquals(3, first.next().posX);
        assertEquals(3, second.next().posX);
        assertEquals(2, action.countActions());
    }

    @Test
    void fullPacketRoundTripPreservesPixelsForAllResolutions() {
        for (AbstractCanvasData.Resolution resolution : AbstractCanvasData.Resolution.values()) {
            int size = resolution.getNumeric();
            CanvasData immediate = CanvasData.BUILDER.createFresh(resolution, size, size);
            CanvasData replay = CanvasData.BUILDER.createFresh(resolution, size, size);
            AbstractToolParameters[] parameters = {
                    new PencilParameters(3, 0.5f, BlendingPipe.BlendingOption.DEFAULT, DitheringPipe.DitheringOption.DENSE_DITHERING),
                    new BrushParameters(4f, 0.75f, BlendingPipe.BlendingOption.DEFAULT, DitheringPipe.DitheringOption.DEFAULT),
                    new BucketParameters()
            };
            Tools[] tools = {Tools.PENCIL, Tools.BRUSH, Tools.BUCKET};
            ArrayDeque<CanvasAction> actions = new ArrayDeque<>();
            for (int toolIndex = 0; toolIndex < tools.length; toolIndex++) {
                CanvasAction action = new CanvasAction(UUID.randomUUID(), tools[toolIndex], 0xFF336699, parameters[toolIndex]);
                for (int i = 0; i < 100; i++) {
                    float x = (i % (size - 2)) + 0.25f;
                    float y = (i / (size - 2)) + 0.75f;
                    action.addFrame(x, y);
                    tools[toolIndex].getTool().apply(immediate, parameters[toolIndex], action.color, x, y);
                }
                action.commit();
                actions.add(action);
            }
            CCanvasActionPacket original = new CCanvasActionPacket(123, actions);
            for (int repeat = 0; repeat < 2; repeat++) {
                FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
                CCanvasActionPacket decoded;
                try {
                    original.writePacketData(buffer);
                    decoded = CCanvasActionPacket.readPacketData(buffer);
                    assertEquals(0, buffer.readableBytes());
                } finally {
                    buffer.release();
                }
                replay.updateColorData(CanvasData.BUILDER.createFresh(resolution, size, size).getColorData());
                assertEquals(123, decoded.easelEntityId);
                assertEquals(actions.stream().map(a -> a.id).toList(), decoded.paintingActions.stream().map(a -> a.id).toList());
                for (CanvasAction action : decoded.paintingActions) {
                    assertEquals(100, action.countActions());
                    action.getSubActionStream().forEach(frame -> action.tool.getTool().apply(
                            replay, action.parameters, action.color, frame.posX, frame.posY));
                }
                assertArrayEquals(immediate.getColorData(), replay.getColorData());
                assertTrue(actions.stream().allMatch(a -> a.countActions() == 100));
            }
        }
    }
}
