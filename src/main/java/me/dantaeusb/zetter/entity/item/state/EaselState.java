package me.dantaeusb.zetter.entity.item.state;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterNetwork;
import me.dantaeusb.zetter.entity.item.EaselEntity;
import me.dantaeusb.zetter.menu.painting.PaintingActionBuffer;
import me.dantaeusb.zetter.menu.painting.PaintingSnapshot;
import me.dantaeusb.zetter.network.packet.CCanvasActionBufferPacket;
import me.dantaeusb.zetter.painting.Tools;
import me.dantaeusb.zetter.painting.parameters.AbstractToolParameters;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.*;

/**
 * This class is responsible for all canvas interactions for easels
 *
 * @todo: make it capability?
 */
public class EaselState {
    private final EaselEntity easel;

    private int tick;

    /*
     * State and networking
     */

    // Only player's buffer on client, all users on server
    private final ArrayDeque<PaintingActionBuffer> actionBuffers = new ArrayDeque<>();

    // Saved painting states
    private final ArrayDeque<PaintingSnapshot> snapshots = new ArrayDeque<>();

    public EaselState(EaselEntity entity) {
        this.easel = entity;

        if (this.getCanvasData() != null) {
            this.snapshots.add(new PaintingSnapshot(this.getCanvasData().getColorData()));
        }
    }

    /**
     * Tick periodically to clean up queue
     * and do network things
     *
     * @todo: freeze ticks
     */
    public void tick() {
        this.tick++;

        if (this.tick % 20 == 0 && this.easel.getLevel().isClientSide()) {
            this.poolActionsQueueClient(false);
        }

        if (this.tick % 200 == 0 && !this.easel.getLevel().isClientSide()) {
            this.poolSnapshotsServer();
        }
    }

    /**
     * @todo: dumb
     * @return
     */
    private CanvasData getCanvasData() {
        if (this.easel.getEaselContainer().getCanvas() == null) {
            return null;
        }

        return this.easel.getEaselContainer().getCanvas().data;
    }

    private String getCanvasCode() {
        if (this.easel.getEaselContainer().getCanvas() == null) {
            return null;
        }

        return this.easel.getEaselContainer().getCanvas().code;
    }

    /*
     * Actions
     */

    /**
     * Apply current tool at certain position and record action if sucessful
     * @param posX
     * @param posY
     */
    public void useTool(UUID playerId, Tools tool, float posX, float posY, int color, AbstractToolParameters parameters) {
        ItemStack paletteStack = this.easel.getEaselContainer().getPaletteStack();

        // No palette or no paints left
        if (paletteStack.isEmpty() || paletteStack.getDamageValue() >= paletteStack.getMaxDamage() - 1) {
            return;
        }

        if (this.getCanvasData() == null) {
            return;
        }

        PaintingActionBuffer lastAction = this.actionBuffers.peekLast();

        Float lastX = null, lastY = null;

        // @todo: when we will do tick/commit, it will be resetting after .5s allowing to do dot dot dot?
        if (lastAction != null && lastAction.tool == tool && !lastAction.isCommitted()) {
            final PaintingActionBuffer.PaintingAction lastSubAction = lastAction.getLastAction();

            if (lastSubAction != null) {
                lastX = lastSubAction.posX;
                lastY = lastSubAction.posY;
            }
        }

        if (tool.getTool().shouldAddAction(this.getCanvasData(), parameters, posX, posY, lastX, lastY)) {
            int damage = tool.getTool().apply(this.getCanvasData(), parameters, color, posX, posY);

            if (tool.getTool().publishable()) {
                this.recordAction(playerId, tool, color, parameters, posX, posY);
            }

            this.easel.getEaselContainer().damagePalette(damage);
        }
    }

    private void recordAction(UUID playerId, Tools tool, int color, AbstractToolParameters parameters, float posX, float posY) {
        PaintingActionBuffer lastAction = this.actionBuffers.peekLast();

        if (lastAction == null) {
            lastAction = this.createAction(playerId, tool, color, parameters);
        } else if (!lastAction.canContinue(playerId, tool, parameters)) {
            lastAction.commit();
            lastAction = this.createAction(playerId, tool, color, parameters);
        }

        lastAction.addFrame(posX, posY);
    }

    private PaintingActionBuffer createAction(UUID playerId, Tools tool, int color, AbstractToolParameters parameters) {
        final PaintingActionBuffer lastAction = this.actionBuffers.peekLast();

        if (!tool.getTool().publishable()) {
            throw new IllegalStateException("Cannot create non-publishable action");
        }

        if (lastAction != null && !lastAction.isCommitted()) {
            lastAction.commit();
        }

        final PaintingActionBuffer newAction = new PaintingActionBuffer(playerId, tool, color, parameters);
        this.actionBuffers.add(newAction);

        return newAction;
    }

    /*
     * State: snapshots and actions
     */

    public boolean canUndo(UUID playerId) {
        return this.getLastNonCanceledAction(playerId) != null;
    }

    public boolean canRedo(UUID playerId) {
        return this.getLastCanceledAction(playerId) != null;
    }

    public boolean undo(UUID playerId) {
        PaintingActionBuffer lastNonCanceledAction = this.getLastNonCanceledAction(playerId);

        if (lastNonCanceledAction == null) {
            return false;
        }

        lastNonCanceledAction.setCanceled(true);
        this.restoreSinceSnapshot();

        return true;
    }

    public boolean redo(UUID playerId) {
        PaintingActionBuffer lastCanceledAction = this.getLastCanceledAction(playerId);

        if (lastCanceledAction == null) {
            return false;
        }

        lastCanceledAction.setCanceled(false);
        this.restoreSinceSnapshot();

        return true;
    }

    private PaintingActionBuffer getLastNonCanceledAction(UUID playerId) {
        return this.getLastActionOfCanceledState(playerId, false);
    }

    private PaintingActionBuffer getLastCanceledAction(UUID playerId) {
        return this.getLastActionOfCanceledState(playerId, true);
    }
    private @Nullable PaintingActionBuffer getLastActionOfCanceledState(UUID playerId, boolean canceled) {
        Iterator<PaintingActionBuffer> actionsIterator = this.actionBuffers.descendingIterator();
        PaintingActionBuffer stateActionBuffer = null;

        while(actionsIterator.hasNext()) {
            PaintingActionBuffer paintingActionBuffer = actionsIterator.next();

            if (paintingActionBuffer.authorId != playerId || paintingActionBuffer.isCanceled() != canceled) {
                continue;
            }

            stateActionBuffer = paintingActionBuffer;
            break;
        }

        return stateActionBuffer;
    }

    /**
     * This action will get the latest available snapshot for the current action set
     * (might lookup down the snapshot deque if the actions are canceled)
     * From that snapshot, canvas state will be restored and actions applied
     * in order
     *
     * @todo: clear canceled action if new action made
     * @todo: when several players editing, last action can be non-canceled,
     * but some previous by another author are
     */
    public void restoreSinceSnapshot() {
        PaintingActionBuffer latestNonCanceledActionBuffer = this.actionBuffers.peekLast();
        Iterator<PaintingActionBuffer> actionsIterator = this.actionBuffers.descendingIterator();

        while(actionsIterator.hasNext()) {
            PaintingActionBuffer paintingActionBuffer = actionsIterator.next();

            if (paintingActionBuffer.isCanceled()) {
                break;
            }

            latestNonCanceledActionBuffer = paintingActionBuffer;
        }

        if (latestNonCanceledActionBuffer == null) {
            Zetter.LOG.error("Unable to find any action to restore from snapshot");
            return;
        }

        PaintingSnapshot latestSnapshot = this.getSnapshotBefore(latestNonCanceledActionBuffer.startTime);

        if (latestSnapshot == null) {
            Zetter.LOG.error("Unable to find snapshot before latest canceled action");
            return;
        }

        this.applySnapshot(latestSnapshot);
        Iterator<PaintingActionBuffer> actionBufferIterator = this.actionBuffers.descendingIterator();

        // @todo: inefficient, but ok for deque
        while(actionBufferIterator.hasNext()) {
            PaintingActionBuffer actionBuffer = actionBufferIterator.next();

            if (actionBuffer.startTime < latestSnapshot.timestamp) {
                continue;
            }

            // @todo: calls client sync too!!!
            this.processActionBuffer(actionBuffer);
        }

        // @todo: this
        if (!this.easel.getLevel().isClientSide()) {
            ((CanvasServerTracker) Helper.getWorldCanvasTracker(this.easel.getLevel())).markCanvasDesync(this.getCanvasCode());
        }
    }

    protected @Nullable PaintingSnapshot getSnapshotBefore(Long timestamp) {
        Iterator<PaintingSnapshot> snapshotIterator = this.snapshots.descendingIterator();

        while(snapshotIterator.hasNext()) {
            PaintingSnapshot snapshot = snapshotIterator.next();

            if (snapshot.timestamp < timestamp) {
                return snapshot;
            }
        }

        return null;
    }

    protected void applySnapshot(PaintingSnapshot snapshot) {
        this.getCanvasData().updateColorData(snapshot.colors);
    }

    /*
     *
     * Networking
     *
     */

    /**
     * Checks and sends action buffer on client
     * Called locally time to time and when menu is closed to
     * force sync from client to server
     */
    public void poolActionsQueueClient(boolean forceCommit) {
        final Queue<PaintingActionBuffer> unsentActions = new LinkedList<>();
        Iterator<PaintingActionBuffer> actionsIterator = this.actionBuffers.descendingIterator();

        while(actionsIterator.hasNext()) {
            PaintingActionBuffer paintingActionBuffer = actionsIterator.next();

            if (!paintingActionBuffer.isCommitted()) {
                if (forceCommit || paintingActionBuffer.shouldCommit()) {
                    paintingActionBuffer.commit();
                } else {
                    continue;
                }
            }

            if (paintingActionBuffer.isSent()) {
                break;
            }

            unsentActions.add(paintingActionBuffer);
        }

        if (!unsentActions.isEmpty()) {
            CCanvasActionBufferPacket paintingFrameBufferPacket = new CCanvasActionBufferPacket(this.easel.getId(), unsentActions);
            ZetterNetwork.simpleChannel.sendToServer(paintingFrameBufferPacket);

            for (PaintingActionBuffer unsentAction : unsentActions) {
                unsentAction.setSent();
            }
        }
    }

    protected void poolSnapshotsServer() {
        if (this.needSnapshot() && this.getCanvasData() != null) {
            this.snapshots.add(new PaintingSnapshot(this.getCanvasData().getColorData()));
        }
    }

    protected boolean needSnapshot() {
        final int MAX_ACTIONS_BEFORE_SNAPSHOT = 100;

        if (this.snapshots.isEmpty()) {
            return true;
        }

        PaintingSnapshot lastSnapshot = this.snapshots.peekLast();
        int actionsSinceSnapshot = 0;

        Iterator<PaintingActionBuffer> actionsIterator = this.actionBuffers.descendingIterator();

        while(actionsIterator.hasNext()) {
            PaintingActionBuffer paintingActionBuffer = actionsIterator.next();

            if (paintingActionBuffer.startTime < lastSnapshot.timestamp) {
                break;
            }

            actionsSinceSnapshot += paintingActionBuffer.countActions();
        }

        if (actionsSinceSnapshot >= MAX_ACTIONS_BEFORE_SNAPSHOT) {
            return true;
        }

        return false;
    }

    /**
     * Called from network - process player's work (only on server)
     * @todo: move to entity?
     * @param actionBuffer
     */
    public void processActionBuffer(PaintingActionBuffer actionBuffer) {
        actionBuffer.getActionStream().forEach((PaintingActionBuffer.PaintingAction action) -> {
            actionBuffer.tool.getTool().apply(
                    this.getCanvasData(),
                    actionBuffer.parameters,
                    actionBuffer.color,
                    action.posX,
                    action.posY
            );
        });

        // @todo: this
        if (!this.easel.getLevel().isClientSide()) {
            ((CanvasServerTracker) Helper.getWorldCanvasTracker(this.easel.getLevel())).markCanvasDesync(this.getCanvasCode());
        }
    }

    /**
     * Add all newest pixels to the canvas when syncing to keep recent player's changes
     * @param canvasCode
     * @param canvasData
     * @param packetTimestamp
     */
    public void processSyncClient(String canvasCode, CanvasData canvasData, long packetTimestamp) {
        this.snapshots.add(new PaintingSnapshot(UUID.randomUUID(), new byte[canvasData.getColorDataBuffer().remaining()], packetTimestamp, false));
    }
}
