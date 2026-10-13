package me.dantaeusb.zetter.entity.item;

import io.netty.buffer.Unpooled;
import me.dantaeusb.zetter.capability.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.entity.item.container.EaselContainer;
import me.dantaeusb.zetter.entity.item.state.EaselState;
import me.dantaeusb.zetter.entity.item.state.representation.CanvasAction;
import me.dantaeusb.zetter.entity.item.state.representation.CanvasSnapshot;
import me.dantaeusb.zetter.network.packet.CCanvasActionPacket;
import me.dantaeusb.zetter.painting.Tools;
import me.dantaeusb.zetter.painting.parameters.AbstractToolParameters;
import me.dantaeusb.zetter.painting.parameters.PencilParameters;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.util.CanvasHolder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EaselStateTest {
    private final Level level = mock(Level.class);
    private final EaselEntity easel = mock(EaselEntity.class);
    private final EaselContainer container = mock(EaselContainer.class);
    private final CanvasData canvas = CanvasData.BUILDER.createFresh(AbstractCanvasData.Resolution.x16, 16, 16);

    private EaselState state() {
        when(easel.level()).thenReturn(level);
        when(easel.getEaselContainer()).thenReturn(container);
        when(easel.getCanvasStack()).thenReturn(new ItemStack(Items.PAPER));
        when(container.getCanvas()).thenReturn(new CanvasHolder<>("test_canvas", canvas));
        return new EaselState(easel) {
            @Override public boolean isCanvasInitialized() { return true; }
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> list(EaselState state, String name) throws Exception {
        Field field = EaselState.class.getDeclaredField(name);
        field.setAccessible(true);
        return (List<T>) field.get(state);
    }

    private static CanvasAction action(int id, long start, long commit, int color) {
        return action(id, start, commit, color, new PencilParameters());
    }

    private static CanvasAction action(int id, long start, long commit, int color, PencilParameters parameters) {
        FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.writeInt(id);
            packet.writeUtf(Tools.PENCIL.name(), 32);
            packet.writeInt(color);
            packet.writeLong(start);
            packet.writeLong(commit);
            packet.writeBoolean(false);
            AbstractToolParameters.writePacketData(parameters, packet);
            ByteBuffer frame = ByteBuffer.allocate(11);
            CanvasAction.CanvasSubAction.writeToBuffer(new CanvasAction.CanvasSubAction(0, 5.5f, 5.5f), frame);
            packet.writeInt(11);
            packet.writeBytes(frame.flip());
            CanvasAction decoded = CanvasAction.readPacketData(packet);
            decoded.setSync(); // Palette damage is outside these pixel-history tests.
            return decoded;
        } finally {
            packet.release();
        }
    }

    @Test
    void sendsUnsentActionsInDrawingOrder() throws Exception {
        EaselState state = state();
        CanvasAction first = new CanvasAction(UUID.randomUUID(), Tools.PENCIL, 0xFF000000, new PencilParameters());
        CanvasAction second = new CanvasAction(UUID.randomUUID(), Tools.PENCIL, 0xFFFFFFFF, new PencilParameters());
        first.addFrame(1, 1); first.commit();
        second.addFrame(1, 1); second.commit();
        EaselStateTest.<CanvasAction>list(state, "actions").addAll(List.of(first, second));
        AtomicReference<CCanvasActionPacket> sent = new AtomicReference<>();
        try (var network = mockStatic(PacketDistributor.class)) {
            network.when(() -> PacketDistributor.sendToServer(any())).thenAnswer(call -> {
                sent.set(call.getArgument(0)); return null;
            });
            state.performHistorySyncClient(true);
        }
        assertEquals(List.of(first.id, second.id), sent.get().paintingActions.stream().map(a -> a.id).toList());
    }

    @Test
    void serverOrdersReversedBatchAndIgnoresDuplicates() throws Exception {
        EaselState state = state();
        EaselStateTest.<CanvasSnapshot>list(state, "snapshots").add(CanvasSnapshot.createServerSnapshot(canvas.getColorData(), 1000));
        long now = System.currentTimeMillis() - 100;
        CanvasAction first = action(1, now - 20, now - 10, 0xFFFF0000);
        CanvasAction second = action(2, now - 5, now, 0xFF0000FF);
        try (var helper = mockStatic(Helper.class)) {
            helper.when(() -> Helper.getLevelCanvasTracker(level)).thenReturn(mock(CanvasServerTracker.class));
            state.processActionServer(new ArrayDeque<>(List.of(second, first)));
            assertEquals(0xFF0000FF, canvas.getColorAt(5 * 16 + 5));
            state.processActionServer(new ArrayDeque<>(List.of(first, second)));
            assertEquals(List.of(1, 2), EaselStateTest.<CanvasAction>list(state, "actions").stream().map(a -> a.id).toList());
            assertEquals(0xFF0000FF, canvas.getColorAt(5 * 16 + 5));
        }
    }

    @Test
    void historicalSnapshotDoesNotIncludeFutureStrokes() throws Exception {
        EaselState state = state();
        int blank = canvas.getColorAt(5 * 16 + 5);
        EaselStateTest.<CanvasSnapshot>list(state, "snapshots").add(CanvasSnapshot.createServerSnapshot(canvas.getColorData(), 1000));
        EaselStateTest.<CanvasAction>list(state, "actions").add(action(1, 2000, 2100, 0xFFFF0000));
        try (var helper = mockStatic(Helper.class)) {
            helper.when(() -> Helper.getLevelCanvasTracker(level)).thenReturn(mock(CanvasServerTracker.class));
            state.recollectPaintingData(1500);
            assertEquals(blank, canvas.getColorAt(5 * 16 + 5));
            state.recollectPaintingData(2200);
            assertEquals(0xFFFF0000, canvas.getColorAt(5 * 16 + 5));
        }
    }

    @Test
    void clientInsertsRemoteStrokesBeforeLaterLocalStrokes() throws Exception {
        when(level.isClientSide()).thenReturn(true);
        EaselState state = state();
        CanvasAction later = action(2, 2000, 2100, 0xFF0000FF);
        EaselStateTest.<CanvasAction>list(state, "actions").add(later);
        CanvasSnapshot base = CanvasSnapshot.createServerSnapshot(canvas.getColorData(), 1000);
        try (var renderer = mockStatic(CanvasRenderer.class)) {
            renderer.when(CanvasRenderer::getInstance).thenReturn(mock(CanvasRenderer.class));
            state.processHistorySyncClient("test_canvas", true, base,
                    new ArrayList<>(List.of(action(1, 1500, 1600, 0xFFFF0000), later)));
            assertEquals(List.of(1, 2), EaselStateTest.<CanvasAction>list(state, "actions").stream().map(a -> a.id).toList());
            assertEquals(0xFF0000FF, canvas.getColorAt(5 * 16 + 5));
            byte[] expected = canvas.getColorData();
            state.processHistorySyncClient("test_canvas", true, base,
                    new ArrayList<>(List.of(action(1, 1500, 1600, 0xFFFF0000), later)));
            assertArrayEquals(expected, canvas.getColorData());
            assertEquals(2, list(state, "actions").size());
        }
    }

    @Test
    void changingPencilSizeAtSamePixelSurvivesServerReplay() throws Exception {
        when(level.isClientSide()).thenReturn(true);
        EaselState state = state();
        Player player = mock(Player.class);
        when(player.getUUID()).thenReturn(UUID.randomUUID());
        when(player.isCreative()).thenReturn(true);
        when(container.getPaletteStack()).thenReturn(new ItemStack(Items.PAPER));
        PencilParameters parameters = new PencilParameters();
        try (var renderer = mockStatic(CanvasRenderer.class)) {
            renderer.when(CanvasRenderer::getInstance).thenReturn(mock(CanvasRenderer.class));
            state.useTool(player, Tools.PENCIL, 5.5f, 5.5f, 0xFF0000FF, parameters);
            parameters.setSize(3);
            state.useTool(player, Tools.PENCIL, 5.5f, 5.5f, 0xFF0000FF, parameters);
        }
        assertEquals(2, list(state, "actions").size());
        CanvasData replay = CanvasData.BUILDER.createFresh(AbstractCanvasData.Resolution.x16, 16, 16);
        for (CanvasAction action : EaselStateTest.<CanvasAction>list(state, "actions")) {
            action.getSubActionStream().forEach(frame -> action.tool.getTool().apply(
                    replay, action.parameters, action.color, frame.posX, frame.posY));
        }
        assertArrayEquals(canvas.getColorData(), replay.getColorData());
        assertEquals(0xFF0000FF, replay.getColorAt(4 * 16 + 5));
    }

    @Test
    void undoRedoAndHistoricalSnapshotPreservePixelBytes() throws Exception {
        EaselState state = state();
        CanvasSnapshot base = CanvasSnapshot.createServerSnapshot(canvas.getColorData(), 1000);
        EaselStateTest.<CanvasSnapshot>list(state, "snapshots").add(base);
        CanvasAction first = action(1, 2000, 2100, 0xFFFF0000);
        CanvasAction second = action(2, 3000, 3100, 0xFF0000FF);
        EaselStateTest.<CanvasAction>list(state, "actions").addAll(List.of(first, second));
        try (var helper = mockStatic(Helper.class)) {
            helper.when(() -> Helper.getLevelCanvasTracker(level)).thenReturn(mock(CanvasServerTracker.class));
            state.recollectPaintingData();
            byte[] expected = canvas.getColorData();
            assertTrue(state.undo());
            assertEquals(0xFFFF0000, canvas.getColorAt(5 * 16 + 5));
            assertTrue(state.redo());
            assertArrayEquals(expected, canvas.getColorData());
            state.recollectPaintingData(2500);
            EaselStateTest.<CanvasSnapshot>list(state, "snapshots").add(CanvasSnapshot.createServerSnapshot(canvas.getColorData(), 2500));
            state.recollectPaintingData();
            assertArrayEquals(expected, canvas.getColorData());
            state.recollectPaintingData();
            assertArrayEquals(expected, canvas.getColorData());
        }
    }

    @Test
    void snapshotOfOverlappingStrokesKeepsLayerOrderAndSavedPixels() throws Exception {
        EaselState state = state();
        long now = System.currentTimeMillis();
        EaselStateTest.<CanvasSnapshot>list(state, "snapshots").add(
                CanvasSnapshot.createServerSnapshot(canvas.getColorData(), now - 20000));
        PencilParameters parameters = new PencilParameters();
        parameters.setIntensity(0.5f);
        CanvasAction longStroke = action(1, now - 10000, now - 5000, 0xFFFF0000, parameters);
        CanvasAction shortStroke = action(2, now - 9000, now - 8000, 0xFF0000FF, parameters);
        EaselStateTest.<CanvasAction>list(state, "actions").addAll(List.of(longStroke, shortStroke));
        try (var helper = mockStatic(Helper.class)) {
            helper.when(() -> Helper.getLevelCanvasTracker(level)).thenReturn(mock(CanvasServerTracker.class));
            state.recollectPaintingData();
            byte[] expected = canvas.getColorData();
            var makeSnapshot = EaselState.class.getDeclaredMethod("makeSnapshot");
            makeSnapshot.setAccessible(true);
            makeSnapshot.invoke(state);
            assertArrayEquals(expected, canvas.getColorData());
            state.recollectPaintingData();
            assertArrayEquals(expected, canvas.getColorData());
            CompoundTag saved = canvas.save(new CompoundTag(), RegistryAccess.EMPTY);
            CanvasData loaded = CanvasData.BUILDER.load(saved);
            assertArrayEquals(expected, loaded.getColorData());
        }
    }

    @Test
    void clientUsesServerOrderForEqualTimestampsAndKeepsUnsentFrames() throws Exception {
        when(level.isClientSide()).thenReturn(true);
        EaselState state = state();
        CanvasAction own = action(2, 2000, 2100, 0xFF0000FF);
        CanvasAction pending = new CanvasAction(UUID.randomUUID(), Tools.PENCIL, 0xFF00FF00, new PencilParameters());
        pending.addFrame(10.5f, 10.5f);
        EaselStateTest.<CanvasAction>list(state, "actions").addAll(List.of(own, pending));
        CanvasSnapshot base = CanvasSnapshot.createServerSnapshot(canvas.getColorData(), 1000);
        try (var renderer = mockStatic(CanvasRenderer.class)) {
            renderer.when(CanvasRenderer::getInstance).thenReturn(mock(CanvasRenderer.class));
            state.processHistorySyncClient("test_canvas", true, base,
                    new ArrayList<>(List.of(action(1, 2000, 2100, 0xFFFF0000), own)));
            assertEquals(List.of(1, 2, pending.id), EaselStateTest.<CanvasAction>list(state, "actions").stream().map(a -> a.id).toList());
            assertEquals(0xFF0000FF, canvas.getColorAt(5 * 16 + 5));
            assertEquals(0xFF00FF00, canvas.getColorAt(10 * 16 + 10));
            assertFalse(pending.isCommitted());
            pending.addFrame(11.5f, 10.5f);
            assertEquals(2, pending.countActions());
        }
    }
}
