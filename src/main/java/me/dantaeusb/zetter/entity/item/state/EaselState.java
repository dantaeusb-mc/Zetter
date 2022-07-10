package me.dantaeusb.zetter.entity.item.state;

import com.google.common.collect.Lists;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.core.EaselStateListener;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterNetwork;
import me.dantaeusb.zetter.entity.item.EaselEntity;
import me.dantaeusb.zetter.entity.item.state.representation.CanvasAction;
import me.dantaeusb.zetter.entity.item.state.representation.CanvasSnapshot;
import me.dantaeusb.zetter.network.packet.CCanvasActionBufferPacket;
import me.dantaeusb.zetter.network.packet.CCanvasHistoryPacket;
import me.dantaeusb.zetter.network.packet.SCanvasSnapshotSync;
import me.dantaeusb.zetter.painting.Tools;
import me.dantaeusb.zetter.painting.parameters.AbstractToolParameters;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

/**
 * This class is responsible for all canvas interactions for easels
 * It keeps painting states at different moments (snapshots)
 * action history, processes sync signals
 *
 * @todo: [LOW] Make it capability?
 * @todo: [HIGH] Clear canceled action if new action made
 */
public class EaselState {
    private final EaselEntity easel;
    private List<EaselStateListener> listeners;

    private int tick;

    /*
     * State and networking
     */

    // Only player's buffer on client, all users on server
    private final ArrayDeque<CanvasAction> actions = new ArrayDeque<>();

    // Saved painting states
    private final ArrayDeque<CanvasSnapshot> snapshots = new ArrayDeque<>();

    public EaselState(EaselEntity entity) {
        this.easel = entity;
    }

    public void addListener(EaselStateListener listener) {
        if (this.listeners == null) {
            this.listeners = Lists.newArrayList();
        }

        this.listeners.add(listener);
    }

    public void removeListener(EaselStateListener listener) {
        this.listeners.remove(listener);
    }

    /**
     * Drops all buffers and removes all snapshots,
     * i.e. when canvas removed
     */
    public void reset() {
        this.actions.clear();
        this.snapshots.clear();

        if (this.getCanvasData() != null) {
            this.createServerSnapshot();
        }

        this.onStateChanged();
    }

    /**
     * Tick periodically to clean up queue
     * and do network things
     *
     * @todo: [MED] Freeze ticks
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
     * Get canvas from canvas holder in container
     * @return
     */
    private CanvasData getCanvasData() {
        if (this.easel.getEaselContainer().getCanvas() == null) {
            return null;
        }

        return this.easel.getEaselContainer().getCanvas().data;
    }

    /**
     * Get canvas code from canvas holder in container
     * @return
     */
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
     * Apply current tool at certain position and record action if successful
     *
     * Client-only
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

        CanvasAction lastAction = this.actions.peekLast();

        Float lastX = null, lastY = null;

        // @todo: [HIGH] When we will do tick/commit, it will be resetting after .5s allowing to do dot dot dot?
        if (lastAction != null && lastAction.tool == tool && !lastAction.isCommitted()) {
            final CanvasAction.CanvasSubAction lastSubAction = lastAction.getLastAction();

            if (lastSubAction != null) {
                lastX = lastSubAction.posX;
                lastY = lastSubAction.posY;
            }
        }

        if (tool.getTool().shouldAddAction(this.getCanvasData(), parameters, posX, posY, lastX, lastY)) {
            this.wipeCanceledActions(playerId);

            int damage = tool.getTool().apply(this.getCanvasData(), parameters, color, posX, posY);

            if (tool.getTool().publishable()) {
                this.recordAction(playerId, tool, color, parameters, posX, posY);
            }

            this.easel.getEaselContainer().damagePalette(damage);
        }
    }

    private void wipeCanceledActions(UUID playerId) {
        Iterator<CanvasAction> actionsIterator = this.actions.descendingIterator();

        while(actionsIterator.hasNext()) {
            CanvasAction paintingActionBuffer = actionsIterator.next();

            if (paintingActionBuffer.authorId.equals(playerId) && paintingActionBuffer.isCanceled()) {
                actionsIterator.remove();
            }
        }
    }

    /**
     * Write down the action that was successfully performed -- either to current buffer if possible,
     * or to new buffer if cannot extend
     *
     * Client-only
     * @param playerId
     * @param tool
     * @param color
     * @param parameters
     * @param posX
     * @param posY
     */
    private void recordAction(UUID playerId, Tools tool, int color, AbstractToolParameters parameters, float posX, float posY) {
        CanvasAction lastAction = this.actions.peekLast();

        if (lastAction == null) {
            lastAction = this.createAction(playerId, tool, color, parameters);
        } else if (!lastAction.canContinue(playerId, tool, color, parameters)) {
            lastAction.commit();
            lastAction = this.createAction(playerId, tool, color, parameters);
        }

        lastAction.addFrame(posX, posY);
    }

    /**
     * Create new action buffer, if we can't write to existing, and
     * submit previous to the server
     *
     * Client-only
     * @param playerId
     * @param tool
     * @param color
     * @param parameters
     * @return
     */
    private CanvasAction createAction(UUID playerId, Tools tool, int color, AbstractToolParameters parameters) {
        final CanvasAction lastAction = this.actions.peekLast();

        if (!tool.getTool().publishable()) {
            throw new IllegalStateException("Cannot create non-publishable action");
        }

        if (lastAction != null && !lastAction.isCommitted()) {
            lastAction.commit();
        }

        final CanvasAction newAction = new CanvasAction(playerId, tool, color, parameters);
        this.actions.add(newAction);

        this.onStateChanged();

        return newAction;
    }

    /*
     * State: snapshots and actions
     */

    public boolean canUndo(UUID playerId) {
        return this.getLastActionOfCanceledState(playerId, false) != null;
    }

    public boolean canRedo(UUID playerId) {
        return this.getLastActionOfCanceledState(playerId, true) != null;
    }

    public boolean undo(UUID playerId) {
        CanvasAction lastNonCanceledAction = this.getLastActionOfCanceledState(playerId, false);

        if (lastNonCanceledAction == null) {
            return false;
        }

        return this.updateActionCanceledState(lastNonCanceledAction, playerId, true);
    }

    public boolean redo(UUID playerId) {
        CanvasAction firstCanceledAction = this.getFirstActionOfCanceledState(playerId, true);

        if (firstCanceledAction == null) {
            return false;
        }

        return this.updateActionCanceledState(firstCanceledAction, playerId, false);
    }

    public boolean updateActionCanceledState(UUID actionId, UUID playerId, boolean canceled) {
        Iterator<CanvasAction> actionsIterator = this.actions.descendingIterator();
        CanvasAction action = null;

        while(actionsIterator.hasNext()) {
            CanvasAction currentAction = actionsIterator.next();

            if (currentAction.uuid.equals(actionId)) {
                action = currentAction;
                break;
            }
        }

        if (action == null) {
            Zetter.LOG.error("Unable to find action to cancel");
            return false;
        }

        return this.updateActionCanceledState(action, playerId, canceled);
    }

    /**
     * Update "canceled" state of an action, and notify network and histroy about that change:
     * Restore painting from snapshot with or without canceled actions
     *
     * @param action
     * @param playerId
     * @param canceled
     * @return
     */
    public boolean updateActionCanceledState(CanvasAction action, UUID playerId, boolean canceled) {
        if (!action.authorId.equals(playerId)) {
            Zetter.LOG.error("Unable to cancel other player's action");
            return false;
        }

        action.setCanceled(canceled);
        this.restoreSinceSnapshot();
        this.onStateChanged();

        // If action is not sent, just checking flag will be enough, it will be sent with proper canceled status
        if (this.easel.getLevel().isClientSide() && action.isSent()) {
            CCanvasHistoryPacket historyPacket = new CCanvasHistoryPacket(this.easel.getId(), action.uuid, canceled);
            Zetter.LOG.debug("Sending History Update: " + historyPacket);
            ZetterNetwork.simpleChannel.sendToServer(historyPacket);
        }

        return true;
    }

    private @Nullable CanvasAction getLastActionOfCanceledState(@Nullable UUID playerId, boolean canceled) {
        return this.getActionOfCanceledState(playerId, canceled, true);
    }

    /**
     * @todo: [MED] Optimal solution would be to find an action for every active player from the end of the queue
     * @param playerId
     * @param canceled
     * @return
     */
    private @Nullable CanvasAction getFirstActionOfCanceledState(@Nullable UUID playerId, boolean canceled) {
        return this.getActionOfCanceledState(playerId, canceled, false);
    }

    private @Nullable CanvasAction getActionOfCanceledState(@Nullable UUID playerId, boolean canceled, boolean fromEnd) {
        Iterator<CanvasAction> actionsIterator;

        if (fromEnd) {
            actionsIterator = this.actions.descendingIterator();
        } else {
            actionsIterator = this.actions.iterator();
        }

        CanvasAction stateAction = null;

        while(actionsIterator.hasNext()) {
            CanvasAction currentAction = actionsIterator.next();

            final boolean differentPlayer = playerId != null && currentAction.authorId != playerId;
            if (differentPlayer || currentAction.isCanceled() != canceled) {
                continue;
            }

            stateAction = currentAction;
            break;
        }

        return stateAction;
    }

    /**
     * This action will get the latest available snapshot for the current action set
     * (might lookup down the snapshot deque if the actions are canceled)
     * From that snapshot, canvas state will be restored and actions applied
     * in order
     *
     * @todo: [HIGH] When several players editing, last action can be non-canceled,
     * but some previous by another author are
     */
    public void restoreSinceSnapshot() {
        CanvasAction firstCanceledAction = this.getFirstActionOfCanceledState(null, true);
        CanvasSnapshot latestSnapshot;

        if (firstCanceledAction != null) {
            // @todo: [HIGH] Not sure about - 100
            latestSnapshot = this.getSnapshotBefore(firstCanceledAction.startTime - 100);
        } else {
            // Nothing canceled - just use last snapshot!
            latestSnapshot = this.snapshots.peekLast();
        }

        if (latestSnapshot == null) {
            Zetter.LOG.error("Unable to find snapshot before first canceled action");
            return;
        }

        this.applySnapshot(latestSnapshot);
        Iterator<CanvasAction> actionBufferIterator = this.actions.iterator();

        // @todo: [LOW] Inefficient, but ok for deque
        // Perfectly, should go back, then reverse iterator, then continue applying action
        while(actionBufferIterator.hasNext()) {
            CanvasAction action = actionBufferIterator.next();

            // We apply non-committed always, as server cannot have idea of the new actions that were not pushed
            // (except if canceled, but it should never happen)
            if (action.isCommitted() && action.startTime < latestSnapshot.timestamp) {
                continue;
            }

            if (!action.isCanceled()) {
                this.applyAction(action);
            }
        }

        this.markDesync();
    }

    protected @Nullable CanvasSnapshot getSnapshotBefore(Long timestamp) {
        Iterator<CanvasSnapshot> snapshotIterator = this.snapshots.descendingIterator();

        while(snapshotIterator.hasNext()) {
            CanvasSnapshot snapshot = snapshotIterator.next();

            if (snapshot.timestamp < timestamp) {
                return snapshot;
            }
        }

        return null;
    }

    /**
     * Apply color data from snapshot
     * @param snapshot
     */
    protected void applySnapshot(CanvasSnapshot snapshot) {
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
     *
     * Client-only
     */
    public void poolActionsQueueClient(boolean forceCommit) {
        final Queue<CanvasAction> unsentActions = new ArrayDeque<>();
        Iterator<CanvasAction> actionsIterator = this.actions.descendingIterator();

        while(actionsIterator.hasNext()) {
            CanvasAction paintingActionBuffer = actionsIterator.next();

            if (!paintingActionBuffer.isCommitted()) {
                if (forceCommit || paintingActionBuffer.shouldCommit()) {
                    paintingActionBuffer.commit();

                    this.onStateChanged();
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

            for (CanvasAction unsentAction : unsentActions) {
                unsentAction.setSent();
            }
        }
    }

    /**
     * Check if we need to create a new snapshot
     *
     * Server-onlu
     */
    protected void poolSnapshotsServer() {
        if (this.needSnapshot() && this.getCanvasData() != null) {
            this.createServerSnapshot();
            // @todo: [HIGH] Remove old snapshots
        }
    }

    /**
     * Should we create a new snapshot
     *
     * Server-only
     * @return
     */
    protected boolean needSnapshot() {
        final int MAX_ACTIONS_BEFORE_SNAPSHOT = 100;

        if (this.snapshots.isEmpty()) {
            return true;
        }

        CanvasSnapshot lastSnapshot = this.snapshots.peekLast();
        int actionsSinceSnapshot = 0;

        Iterator<CanvasAction> actionsIterator = this.actions.descendingIterator();

        while(actionsIterator.hasNext()) {
            CanvasAction paintingActionBuffer = actionsIterator.next();

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
     * Create snapshot on server, clear older snapshots and send
     * message to all tracking players
     */
    private void createServerSnapshot() {
        CanvasSnapshot snapshot = CanvasSnapshot.createServerSnapshot(this.getCanvasData().getColorData());

        this.snapshots.add(snapshot);

        SCanvasSnapshotSync syncMessage = new SCanvasSnapshotSync(
                this.easel.getId(), this.getCanvasCode(), this.getCanvasData(), snapshot.timestamp
        );

        for (Player usingPlayer : this.easel.getPlayersUsing()) {
            ZetterNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) usingPlayer), syncMessage);
        }

        this.markDesync();
    }

    /**
     * Called from network - process player's work
     * And drop canceled actions on server if needed
     *
     * Server-only
     * @param action
     */
    public void processNetworkAction(CanvasAction action) {
        this.applyAction(action);

        this.wipeCanceledActions(action.authorId);
        this.actions.add(action);

        // @todo: [MED] It should use another mechanism I believe, or else it'll spam network events
        this.markDesync();
    }

    /**
     * Apply action from history or network - don't save it
     * @param action
     */
    public void applyAction(CanvasAction action) {
        action.getSubActionStream().forEach((CanvasAction.CanvasSubAction subAction) -> {
            // Apply subAction directly
            action.tool.getTool().apply(
                    this.getCanvasData(),
                    action.parameters,
                    action.color,
                    subAction.posX,
                    subAction.posY
            );
        });
    }

    /**
     * Add all newest pixels to the canvas when syncing to keep recent player's changes
     *
     * Client-only
     * @param canvasCode
     * @param canvasData
     * @param packetTimestamp
     */
    public void processWeakSnapshotClient(String canvasCode, CanvasData canvasData, long packetTimestamp) {
        this.snapshots.add(CanvasSnapshot.createWeakSnapshot(canvasData.getColorData(), packetTimestamp));
        this.restoreSinceSnapshot();
    }

    private void markDesync() {
        if (!this.easel.getLevel().isClientSide()) {
            ((CanvasServerTracker) Helper.getWorldCanvasTracker(this.easel.getLevel())).markCanvasDesync(this.getCanvasCode());
        }
    }

    /**
     * Add all newest pixels to the canvas when syncing to keep recent player's changes
     * Additionally writes state to recover history
     *
     * Client-only
     * @param canvasCode
     * @param canvasData
     * @param packetTimestamp
     */
    public void processSnapshotSyncClient(String canvasCode, CanvasData canvasData, long packetTimestamp) {
        this.snapshots.add(CanvasSnapshot.createNetworkSnapshot(UUID.randomUUID(), canvasData.getColorData(), packetTimestamp));
        this.restoreSinceSnapshot();
    }

    /**
     * Notify subscribers that history state was changed
     */
    protected void onStateChanged()
    {
        if (this.listeners != null) {
            for(EaselStateListener listener : this.listeners) {
                listener.stateChanged(this);
            }
        }
    }
}
