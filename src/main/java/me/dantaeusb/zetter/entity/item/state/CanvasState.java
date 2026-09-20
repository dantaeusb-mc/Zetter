package me.dantaeusb.zetter.entity.item.state;

import com.google.common.collect.Lists;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.core.EaselStateListener;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterNetwork;
import me.dantaeusb.zetter.entity.item.CanvasHolderEntity;
import me.dantaeusb.zetter.entity.item.state.representation.CanvasAction;
import me.dantaeusb.zetter.entity.item.state.representation.CanvasSnapshot;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.network.packet.*;
import me.dantaeusb.zetter.painting.Tool;
import me.dantaeusb.zetter.painting.parameters.AbstractToolParameters;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Canvas history for easels: the pixels are derived state, kept as a log of actions
 * replayed over periodic snapshots and reconciled between client and server.
 *
 * A snapshot stamped T holds the canvas after every non-canceled action with a start
 * time before T. Actions are ordered by that same field, so it alone decides what a
 * snapshot already contains, and flipping an action's canceled flag invalidates every
 * snapshot taken after it started.
 *
 * See docs/painting-history.md
 *
 * Maybe we should create it only with canvas and destroy when canvas removed
 */
public class CanvasState {
    public static int SNAPSHOT_HISTORY_SIZE = 10;
    public static int ACTION_HISTORY_SIZE = 512;

    // Keep history and snapshots for 30 minutes of inactivity
    public static int FREEZE_TIMEOUT = (Zetter.DEBUG_MODE ? 1 : 30) * 60 * 1000;

    /**
     * Counted in sub-actions, not actions: a snapshot is cut once this many painted
     * points have settled, which is what bounds both replay cost and how far undo
     * can reach back
     */
    private final int MAX_SUB_ACTIONS_BEFORE_SNAPSHOT = ACTION_HISTORY_SIZE / SNAPSHOT_HISTORY_SIZE;

    public static int CLIENT_SNAPSHOT_HISTORY_SIZE = 50;
    private static int SYNC_INTERVAL = 1000;
    private static int TICK_INTERVAL = 50; // 20 TPM
    private static long MAX_LATENCY = 500;
    private static long PROCESSING_WINDOW = SYNC_INTERVAL + MAX_LATENCY + CanvasAction.MAX_TIME;

    private static int SYNC_TICKS = SYNC_INTERVAL / TICK_INTERVAL;

    private final CanvasHolderEntity canvasHolder;
    private List<EaselStateListener> listeners;

    /**
     * Flag for client if it assumes that current state
     * is actual state. Used for history packets:
     * if the last history packed is marked as "actual"
     * (no more unsent actions at the time of generating packet)
     * then we have our easel state sync and can use snapshot
     * restoration
     */
    private boolean sync = false;
    private int tick;

    /**
     * Frozen means that easel was not updated for a while, and
     * there's no reason to keep history and snapshots
     */
    private boolean frozen = true;
    private long lastActivity = 0L;

    /*
     * State and networking
     */

    private final ArrayList<Player> players = new ArrayList<>();
    // Allocate 110%
    private final ArrayList<CanvasAction> actions = new ArrayList<>(ACTION_HISTORY_SIZE + (int) (ACTION_HISTORY_SIZE * 0.1f));

    // Check the last items we sent to player, to avoid re-sending
    private final HashMap<UUID, Integer> playerLastSyncedAction = new HashMap<>();
    private final HashMap<UUID, Integer> playerLastSyncedSnapshot = new HashMap<>();

    /*
     * Saved painting states
     * Server max memory allocation:
     * 16x: 64kb x SNAPSHOT_HISTORY_SIZE = 640kb
     * 64x: 1mb x SNAPSHOT_HISTORY_SIZE = 10mb
     * Client max memory allocation
     * 16x: 64kb x CLIENT_SNAPSHOT_HISTORY_SIZE = 6.4mb
     * 64x: 1mb x CLIENT_SNAPSHOT_HISTORY_SIZE = 100mb
     * Could take 100mb but only once on client
     * But client snapshots created more often
     * Cleaned up over time by freezing mechanism
     */
    private final ArrayList<CanvasSnapshot> snapshots;

    /**
     * This flag means that the history
     * traversing was made. For that reason,
     * on new action, wiping might be needed.
     */
    private boolean historyDirty = false;

    /**
     * Undo and redo only flip flags, the canvas is rebuilt from them. Several of those
     * can land in one tick, and rebuilding once at the end of it gives the same result
     * for a fraction of the work.
     */
    private boolean pendingRecollect = false;

    public CanvasState(CanvasHolderEntity entity) {
        this.canvasHolder = entity;

        if (entity.level().isClientSide()) {
            this.snapshots = new ArrayList<>(CLIENT_SNAPSHOT_HISTORY_SIZE + 1);
        } else {
            this.snapshots = new ArrayList<>(SNAPSHOT_HISTORY_SIZE + 1);
            // Server is always synchronized with itself, technically
            this.sync = true;
        }
    }

    // Activity listeners

    /**
     * Add user that will be updated with history
     * and new snapshot (history sync)
     * @param player
     */
    public void addPlayer(Player player) {
        this.players.add(player);

        this.unfreeze();

        this.updateSnapshots();
        if (!this.canvasHolder.level().isClientSide() && this.getCanvasCode() != null) {
            this.performHistorySyncForServerPlayer(player);
        }
    }

    /**
     * Remove user from history sync
     * (when menu is closed)
     * @param player
     */
    public void removePlayer(Player player) {
        this.players.remove(player);

        if (this.canvasHolder.level().isClientSide()) {
            this.freeze();
        } else {
            this.playerLastSyncedAction.remove(player.getUUID());
            this.playerLastSyncedSnapshot.remove(player.getUUID());
        }
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
    public void reset(boolean sync) {
        // To avoid removing data from current stroke
        if (this.canvasHolder.level().isClientSide()) {
            this.performHistorySyncClient(true);
        }

        this.actions.clear();
        this.snapshots.clear();
        this.playerLastSyncedAction.clear();
        this.playerLastSyncedSnapshot.clear();

        if (!this.canvasHolder.level().isClientSide() && sync && this.getCanvasData() != null) {
            SEaselResetPacket resetPacket = new SEaselResetPacket(this.canvasHolder.getId());

            for (Player player : this.players) {
                ZetterNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), resetPacket);
            }
        }

        this.updateSnapshots();
        this.onStateChanged();
    }

    public void reset() {
        this.reset(true);
    }

    // Freezing

    protected void freeze() {
        this.reset();
        this.frozen = true;
    }

    protected void unfreeze() {
        this.frozen = false;
        this.lastActivity = System.currentTimeMillis();
    }

    // Ticking

    /**
     * Tick periodically to clean up queue
     * and do network things
     */
    public void tick() {
        if (this.frozen) {
            return;
        }

        this.tick++;

        // No one used for a while
        if (this.players.isEmpty() && System.currentTimeMillis() - this.lastActivity > FREEZE_TIMEOUT) {
            this.freeze();
        }

        if (this.canvasHolder.level().isClientSide()) {
            if (this.tick % SYNC_TICKS == 0) {
                this.performHistorySyncClient(false);
            }
        } else {
            // Before the check below: the last player may have left mid-traversal
            if (this.pendingRecollect) {
                this.recollectPaintingData();
            }

            // No need to tick if no one's using
            if (this.players.size() == 0 || this.getCanvasCode() == null) {
                return;
            }

            // Every 5 seconds check if we need a snapshot
            if (this.tick % (SYNC_TICKS * 5) == 0) {
                this.updateSnapshots();
            }

            // Every second send sync
            if (this.tick % SYNC_TICKS == 0) {
                this.performHistorySyncServer();
            }
        }
    }

    /**
     * Get canvas from canvas holder in container
     * @return
     */
    private CanvasData getCanvasData() {
        return this.canvasHolder.getCanvasData();
    }

    /**
     * Get canvas code from canvas holder in container
     * @return
     */
    private @Nullable String getCanvasCode() {
        return this.canvasHolder.getCanvasCode();
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
    public void useTool(Player player, Tool tool, float posX, float posY, int color, AbstractToolParameters parameters) {
        ItemStack paletteStack = this.canvasHolder.getPaletteStack(player);

        // No palette or no paints left and player is not creative mode player
        if (paletteStack == null || paletteStack.isEmpty() ||
            (!player.isCreative() &&
                (paletteStack.getDamageValue() >= paletteStack.getMaxDamage() - 1)
            )
        ) {
            return;
        }

        if (this.getCanvasData() == null) {
            return;
        }

        CanvasAction lastAction = this.getLastActionOfCanceledState(false);

        Float lastX = null, lastY = null;

        if (lastAction != null && lastAction.tool == tool && !lastAction.isCommitted()) {
            final CanvasAction.CanvasSubAction lastSubAction = lastAction.getLastAction();

            if (lastSubAction != null) {
                lastX = lastSubAction.posX;
                lastY = lastSubAction.posY;
            }
        }

        if (tool.getTool().shouldAddAction(this.getCanvasData(), parameters, posX, posY, lastX, lastY)) {
            this.wipeCanceledActionsAndDiscardSnapshots();

            if (tool.getTool().hasEffect()) {
                this.unfreeze();
                final boolean initialized = this.isCanvasInitialized();

                if (initialized) {
                    int damage = tool.getTool().apply(this.getCanvasData(), parameters, color, posX, posY);

                    if (!player.isCreative()) {
                        this.canvasHolder.damagePalette(player, damage);
                    }

                    CanvasRenderer.getInstance().updateCanvasTexture(this.getCanvasCode(), this.getCanvasData());
                }

                this.recordAction(player.getUUID(), tool, color, parameters, posX, posY);

                if (!initialized) {
                    // Forcefully push changes immediately so canvas will be initialized on server and synced back
                    this.performHistorySyncClient(true);
                }
            } else {
                tool.getTool().apply(this.getCanvasData(), parameters, color, posX, posY);
            }
        }
    }

    /**
     * When new action is created, history becomes non-linear
     * All canceled actions get discarded
     *
     * Client and server
     */
    private void wipeCanceledActionsAndDiscardSnapshots() {
        if (!this.historyDirty) {
            return;
        }

        ListIterator<CanvasAction> actionsIterator = this.getActionsEndIterator();
        CanvasAction firstRemovedAction = null;
        CanvasAction firstNonRemovedAction = null;

        while(actionsIterator.hasPrevious()) {
            CanvasAction action = actionsIterator.previous();

            if (action.isCanceled()) {
                firstRemovedAction = action;
                actionsIterator.remove();
            } else if(firstNonRemovedAction == null) {
                firstNonRemovedAction = action;
            } else {
                break;
            }
        }

        if (firstRemovedAction == null) {
            this.historyDirty = false;
            return;
        }

        if (!this.canvasHolder.level().isClientSide()) {
            if (firstNonRemovedAction != null) {
                // Update last synced actions tracker
                // Set false for players that need to have the last synced action updated
                List<UUID> playersNeedToUpdateLastSyncedAction = this.playerLastSyncedAction.entrySet().stream()
                    .filter(lastSyncedActionEntry -> this.actions.stream().noneMatch(canvasAction -> canvasAction.id == lastSyncedActionEntry.getValue()))
                    .map(Map.Entry::getKey).toList();

                for (UUID playerUuid : playersNeedToUpdateLastSyncedAction) {
                    this.playerLastSyncedAction.put(playerUuid, firstNonRemovedAction.id);
                }
            } else {
                this.playerLastSyncedAction.clear();
            }
        }

        ListIterator<CanvasSnapshot> snapshotIterator = this.getSnapshotsEndIterator();
        CanvasSnapshot firstRemovedSnapshot = null;
        CanvasSnapshot firstNonRemovedSnapshot = null;

        while(snapshotIterator.hasPrevious()) {
            CanvasSnapshot snapshot = snapshotIterator.previous();

            if (snapshot.timestamp > firstRemovedAction.getStartTime()) {
                firstRemovedSnapshot = snapshot;
                snapshotIterator.remove();
            } else if (firstNonRemovedSnapshot == null) {
                firstNonRemovedSnapshot = snapshot;
            } else {
                break;
            }
        }

        if (firstRemovedSnapshot == null) {
            this.historyDirty = false;
            return;
        }

        if (!this.canvasHolder.level().isClientSide()) {
            if (firstNonRemovedSnapshot != null) {
                // Update last synced snapshot tracker
                // Set false for players that need to have the last synced snapshot updated
                List<UUID> playersNeedToUpdateLastSyncedSnapshot = this.playerLastSyncedSnapshot.entrySet().stream()
                    .filter(lastSyncedSnapshotEntry -> this.snapshots.stream().noneMatch(canvasAction -> canvasAction.id == lastSyncedSnapshotEntry.getValue()))
                    .map(Map.Entry::getKey).toList();

                for (UUID playerUuid : playersNeedToUpdateLastSyncedSnapshot) {
                    this.playerLastSyncedSnapshot.put(playerUuid, firstNonRemovedSnapshot.id);
                }
            } else {
                Zetter.LOG.warn("Removed all snapshots, that should not be happening!");
                this.playerLastSyncedSnapshot.clear();
                this.makeSnapshot();
            }
        }

        if (Zetter.DEBUG_MODE) {
            this.logActions();
        }

        this.historyDirty = false;
    }

    /**
     * Write down the action that was successfully performed -- either to current buffer if possible,
     * or to the new buffer if cannot extend
     *
     * Client-only
     * @param playerId
     * @param tool
     * @param color
     * @param parameters
     * @param posX
     * @param posY
     */
    private void recordAction(UUID playerId, Tool tool, int color, AbstractToolParameters parameters, float posX, float posY) {
        CanvasAction lastAction = this.getLastAction();

        if (lastAction == null || lastAction.isCommitted()) {
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
    private CanvasAction createAction(UUID playerId, Tool tool, int color, AbstractToolParameters parameters) {
        final CanvasAction lastAction = this.getLastAction();

        if (!tool.getTool().hasEffect()) {
            throw new IllegalStateException("Cannot create non-publishable action");
        }

        if (lastAction != null && !lastAction.isCommitted()) {
            lastAction.commit();
        }

        CanvasAction newAction;
        try {
            newAction = new CanvasAction(playerId, tool, color, parameters.clone());
            this.actions.add(newAction);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot copy parameters for action: " + e.getMessage());
        }

        this.onStateChanged();

        return newAction;
    }

    /*
     * Initialization
     */

    /**
     * Check if we can draw on canvas or we should initialize
     * canvas data first
     *
     * As it checks item, it will work on both client and server
     *
     * @return
     */
    public boolean isCanvasInitialized() {
        final String canvasCode = this.canvasHolder.getCanvasCode();

        // A canvas with no data of its own still reports a default code so it can be rendered blank
        return canvasCode != null && !CanvasData.isDefaultCanvasCode(canvasCode);
    }

    /**
     * When canvas is empty, ask canvas item
     * to initialize data before start drawing
     *
     * Server-only
     *
     * @return boolean True if initialization is successful
     */
    public boolean initializeCanvas(long timestamp) {
        ItemStack canvasStack = this.canvasHolder.getCanvasStack();

        if (canvasStack == null) {
            throw new IllegalStateException("Cannot initialize canvas: no item in container");
        }

        String canvasCode = CanvasItem.getCanvasCode(canvasStack);

        if (canvasCode != null) {
            // Already initialized
            return false;
        }

        if (this.listeners != null) {
            for(EaselStateListener listener : this.listeners) {
                listener.stateCanvasInitializationStart(this);
            }
        }

        int resolution = CanvasItem.getResolution(canvasStack);
        int[] size = CanvasItem.getBlockSize(canvasStack);

        assert size != null && size.length == 2; // @todo: Stop menu updates to prevent sending change before initialization packet

        CanvasData canvasData = CanvasItem.createEmpty(canvasStack, AbstractCanvasData.Resolution.get(resolution), size[0], size[1], this.canvasHolder.level());
        canvasCode = CanvasItem.getCanvasCode(canvasStack);

        /*
         * createEmpty writes the code into the stack's tag directly, which does not go
         * through the container, so the holder would keep reporting the default code
         * and we would initialize the canvas again on the next action
         */
        this.canvasHolder.getEaselContainer().changed();

        SEaselCanvasInitializationPacket initPacket = new SEaselCanvasInitializationPacket(this.canvasHolder.getId(), canvasCode,canvasData, System.currentTimeMillis());

        for (Player player : this.canvasHolder.getPlayersUsing()) {
            ZetterNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), initPacket);
        }

        // Drop things but not sync yet
        this.reset(false);

        // reset() leaves a snapshot stamped "now", we need one older than the first action
        this.snapshots.clear();
        this.insertSnapshot(CanvasSnapshot.createServerSnapshot(canvasData.getColorData(), timestamp - MAX_LATENCY - 1L));

        if (this.listeners != null) {
            for(EaselStateListener listener : this.listeners) {
                listener.stateCanvasInitializationEnd(this);
            }
        }

        return true;
    }

    /*
     * Action state
     */

    /**
     * Update "canceled" state of an action and all actions before or after (depends on state)
     * notify network and history about that change and update textures.
     *
     * @param tillAction
     * @param cancel
     * @return
     */
    private boolean applyHistoryTraversing(CanvasAction tillAction, boolean cancel) {
        // We will discard every snapshot after it, so we need an older one to rebuild from
        final CanvasAction earliestAffectedAction = cancel ? tillAction : this.getFirstActionOfCanceledState(true);

        if (earliestAffectedAction == null) {
            return false;
        }

        if (this.getSnapshotBefore(earliestAffectedAction.getStartTime()) == null) {
            Zetter.LOG.warn("No snapshot old enough to traverse history to action " + earliestAffectedAction.id);
            return false;
        }

        @Nullable CanvasAction earliestChangedAction = null;

        // Cancel all after, un-cancel all before
        if (cancel) {
            ListIterator<CanvasAction> actionsIterator = this.getActionsEndIterator();

            while(actionsIterator.hasPrevious()) {
                CanvasAction currentAction = actionsIterator.previous();

                if (!currentAction.isCanceled()) {
                    currentAction.setCanceled(true);
                    // Walking backwards, so every next hit is an earlier one
                    earliestChangedAction = currentAction;
                }

                if (currentAction.id == tillAction.id) {
                    break;
                }
            }
        } else {
            ListIterator<CanvasAction> actionsIterator = this.actions.listIterator();

            while(actionsIterator.hasNext()) {
                CanvasAction currentAction = actionsIterator.next();

                if (currentAction.isCanceled()) {
                    currentAction.setCanceled(false);

                    if (earliestChangedAction == null) {
                        earliestChangedAction = currentAction;
                    }
                }

                if (currentAction.id == tillAction.id) {
                    break;
                }
            }
        }

        if (earliestChangedAction == null) {
            return false;
        }

        this.discardSnapshotsAfter(earliestChangedAction.getStartTime());

        if (this.canvasHolder.level().isClientSide()) {
            // Local feedback has to be immediate
            this.recollectPaintingData();
        } else {
            this.pendingRecollect = true;
        }

        this.onStateChanged();

        if (Zetter.DEBUG_MODE) {
            this.logActions();
        }

        // If tillAction is not sent, just checking flag will be enough, it will be sent with proper canceled status
        if (this.canvasHolder.level().isClientSide()) {
            if (tillAction.isSent()) {
                CCanvasHistoryActionPacket historyPacket = new CCanvasHistoryActionPacket(this.canvasHolder.getId(), tillAction.id, cancel);
                ZetterNetwork.simpleChannel.sendToServer(historyPacket);
            }
        } else {
            for (Player player : this.players) {
                // If not sent any actions, it will send canceled already
                if (this.playerLastSyncedAction.containsKey(player.getUUID())) {
                    int lastSyncedActionUuid = this.playerLastSyncedAction.get(player.getUUID());
                    ListIterator<CanvasAction> actionsIterator = this.getActionsEndIterator();
                    boolean found = false;

                    // If we sent action (found last sent action before tillAction), then we need to send history packet
                    while(actionsIterator.hasPrevious()) {
                        CanvasAction action = actionsIterator.previous();

                        if (lastSyncedActionUuid == tillAction.id) {
                            found = true;
                            break;
                        }

                        if (action.id == tillAction.id) {
                            break;
                        }
                    }

                    if (found) {
                        SCanvasHistoryActionPacket historyPacket = new SCanvasHistoryActionPacket(this.canvasHolder.getId(), tillAction.id, cancel);
                        ZetterNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), historyPacket);
                    }
                }
            }
        }

        this.historyDirty = true;
        this.unfreeze();

        return true;
    }

    /**
     * @param canceled
     * @return
     */
    private @Nullable CanvasAction getLastActionOfCanceledState(boolean canceled) {
        return this.getLastActionOfCanceledState(canceled, null);
    }

    private @Nullable CanvasAction getLastActionOfCanceledState(boolean canceled, @Nullable UUID playerId) {
        return this.getActionOfCanceledState(canceled, true, playerId);
    }

    /**
     * @param canceled
     * @return
     */
    private @Nullable CanvasAction getFirstActionOfCanceledState(boolean canceled) {
        return this.getFirstActionOfCanceledState(canceled, null);
    }

    private @Nullable CanvasAction getFirstActionOfCanceledState(boolean canceled, @Nullable UUID playerId) {
        return this.getActionOfCanceledState(canceled, false, playerId);
    }

    /**
     * Get the last action of canceled or non-canceled state
     * Used for undoing and redoing
     *
     * @param canceled
     * @param fromEnd
     * @param playerId If provided, look for that player's acitons
     * @return
     */
    private @Nullable CanvasAction getActionOfCanceledState(boolean canceled, boolean fromEnd, @Nullable UUID playerId) {
        CanvasAction stateAction = null;

        if (fromEnd) {
            ListIterator<CanvasAction> actionsIterator = this.getActionsEndIterator();
            while(actionsIterator.hasPrevious()) {
                CanvasAction currentAction = actionsIterator.previous();

                final boolean differentPlayer = playerId != null && currentAction.getAuthorUUID() != null && !currentAction.getAuthorUUID().equals(playerId);
                if (differentPlayer || currentAction.isCanceled() != canceled) {
                    continue;
                }

                stateAction = currentAction;
                break;
            }
        } else {
            ListIterator<CanvasAction> actionsIterator = this.actions.listIterator();
            while(actionsIterator.hasNext()) {
                CanvasAction currentAction = actionsIterator.next();

                final boolean differentPlayer = playerId != null && currentAction.getAuthorUUID() != null && !currentAction.getAuthorUUID().equals(playerId);
                if (differentPlayer || currentAction.isCanceled() != canceled) {
                    continue;
                }

                stateAction = currentAction;
                break;
            }
        }

        return stateAction;
    }

    /*
     * State: undo-redo
     *
     * Previously we were using player id to remove only own actions
     * But now it's removed since it's excessive and super confusing.
     * It's still possible though but hard to work with.
     */

    public boolean canUndo() {
        final CanvasAction lastAction = this.getLastActionOfCanceledState(false);

        return lastAction != null && this.getSnapshotBefore(lastAction.getStartTime()) != null;
    }

    public boolean canRedo() {
        final CanvasAction firstCanceledAction = this.getFirstActionOfCanceledState(true);

        return firstCanceledAction != null && this.getSnapshotBefore(firstCanceledAction.getStartTime()) != null;
    }

    public boolean undo() {
        CanvasAction lastNonCanceledAction = this.getLastActionOfCanceledState(false);

        if (lastNonCanceledAction == null) {
            return false;
        }

        return this.undo(lastNonCanceledAction);
    }

    public boolean undo(int tillActionId) {
        @Nullable CanvasAction tillAction = this.findAction(tillActionId);

        if (tillAction != null) {
            return this.undo(tillAction);
        }

        return false;
    }

    public boolean undo(CanvasAction tillAction) {
        return this.applyHistoryTraversing(tillAction, true);
    }

    public boolean redo() {
        CanvasAction firstCanceledAction = this.getFirstActionOfCanceledState(true);

        if (firstCanceledAction == null) {
            return false;
        }

        return this.redo(firstCanceledAction);
    }

    public boolean redo(int tillActionUuid) {
        @Nullable CanvasAction tillAction = this.findAction(tillActionUuid);

        if (tillAction != null) {
            return this.redo(tillAction);
        }

        return false;
    }

    public boolean redo(CanvasAction tillAction) {
        return this.applyHistoryTraversing(tillAction, false);
    }

    private @Nullable CanvasAction findAndReplaceAction(int actionId, CanvasAction action) {
        ListIterator<CanvasAction> actionsIterator = this.getActionsEndIterator();
        @Nullable CanvasAction tillAction = null;

        while(actionsIterator.hasPrevious()) {
            CanvasAction currentAction = actionsIterator.previous();

            if (currentAction.id == actionId) {
                tillAction = currentAction;
                actionsIterator.set(action);
                break;
            }
        }

        return tillAction;
    }

    /**
     * Insert an action keeping the list ordered by start time
     *
     * @param addedAction
     */
    private void insertAction(CanvasAction addedAction) {
        final ListIterator<CanvasAction> actionsIterator = this.getActionsEndIterator();

        while (actionsIterator.hasPrevious()) {
            final CanvasAction currentAction = actionsIterator.previous();

            if (currentAction.getStartTime() <= addedAction.getStartTime()) {
                actionsIterator.next();
                actionsIterator.add(addedAction);

                return;
            }
        }

        // Older than everything we have
        actionsIterator.add(addedAction);
    }

    private @Nullable CanvasAction findAction(int actionId) {
        ListIterator<CanvasAction> actionsIterator = this.getActionsEndIterator();
        @Nullable CanvasAction tillAction = null;

        while(actionsIterator.hasPrevious()) {
            CanvasAction currentAction = actionsIterator.previous();

            if (currentAction.id == actionId) {
                tillAction = currentAction;
                break;
            }
        }

        return tillAction;
    }

    /*
     * Snapshots
     */

    /**
     * Restore the canvas to the state it had at a given moment: take the newest
     * snapshot made before it and replay every action started since.
     *
     * @param timestamp replay actions that started before this moment, exclusive
     */
    public void recollectPaintingData(long timestamp) {
        this.pendingRecollect = false;

        final CanvasAction firstCanceledAction = this.getFirstActionOfCanceledState(true);

        // Snapshots taken after a canceled action started hold pixels that are no longer there
        final long snapshotBoundary = firstCanceledAction == null
            ? timestamp
            : Math.min(timestamp, firstCanceledAction.getStartTime());

        final CanvasSnapshot latestSnapshot = this.getSnapshotBefore(snapshotBoundary);

        if (latestSnapshot == null) {
            Zetter.LOG.error("Unable to find a snapshot to restore canvas from");
            return;
        }

        // @todo: [MED] No need to update data before collected: apply changes on buffer, then update data and texture
        this.applySnapshot(latestSnapshot);

        for (CanvasAction action : this.actions) {
            // Ordered by start time, so there is nothing left past the boundary
            if (action.getStartTime() >= timestamp) {
                break;
            }

            // Already part of the snapshot
            if (action.getStartTime() < latestSnapshot.timestamp) {
                continue;
            }

            if (action.isCanceled()) {
                continue;
            }

            this.applyAction(action, false);
        }

        if (this.canvasHolder.level().isClientSide()) {
            CanvasRenderer.getInstance().updateCanvasTexture(this.getCanvasCode(), this.getCanvasData());
        }

        if (Zetter.DEBUG_MODE && Zetter.DEBUG_CLIENT) {
            Zetter.LOG.debug("Restored since snapshot");
        }

        this.markDesync();
    }

    /**
     * Restore the canvas to its current state, including the stroke in progress
     */
    public void recollectPaintingData() {
        this.recollectPaintingData(Long.MAX_VALUE);
    }

    /**
     * Get a snapshot that was made before certain timestamp
     * @param timestamp
     * @return
     */
    protected @Nullable CanvasSnapshot getSnapshotBefore(Long timestamp) {
        ListIterator<CanvasSnapshot> snapshotIterator = this.getSnapshotsEndIterator();

        while(snapshotIterator.hasPrevious()) {
            CanvasSnapshot snapshot = snapshotIterator.previous();

            if (snapshot.timestamp < timestamp) {
                return snapshot;
            }
        }

        return null;
    }

    /**
     * Drop every snapshot taken after the given moment, their contents depend on
     * actions that have been canceled or restored since
     *
     * @param startTime
     */
    private void discardSnapshotsAfter(long startTime) {
        final ListIterator<CanvasSnapshot> snapshotIterator = this.getSnapshotsEndIterator();

        while (snapshotIterator.hasPrevious()) {
            final CanvasSnapshot snapshot = snapshotIterator.previous();

            if (snapshot.timestamp <= startTime) {
                return;
            }

            snapshotIterator.remove();
        }
    }

    /**
     * Apply color data from snapshot
     * @param snapshot
     */
    protected void applySnapshot(CanvasSnapshot snapshot) {
        this.getCanvasData().updateColorData(snapshot.colors);
    }

    /**
     * Check if we need to create a new snapshot
     * clean up older snapshots and make new if needed
     *
     * Used once on client when menu opened
     */
    protected void updateSnapshots() {
        if (this.getCanvasData() == null) {
            return;
        }

        if (this.canvasHolder.level().isClientSide()) {
            if (this.snapshots.isEmpty()) {
                this.makeSnapshot();
            }
        } else {
            if (this.needSnapshot()) {
                this.cleanupSnapshotHistory();
                this.makeSnapshot();
            }
        }

    }

    /**
     * Should we create a new snapshot
     * we should if players made more than
     * MAX_SUB_ACTIONS_BEFORE_SNAPSHOT since last snapshot was made
     *
     * Server-only
     * @return
     */
    private boolean needSnapshot() {
        if (this.snapshots.isEmpty()) {
            return true;
        }

        // A snapshot taken while something is undone gets discarded by the next redo
        if (this.getFirstActionOfCanceledState(true) != null) {
            return false;
        }

        long authoritativeTimestamp = System.currentTimeMillis() - PROCESSING_WINDOW;

        CanvasSnapshot lastSnapshot = this.getLastSnapshot();
        assert lastSnapshot != null;

        int actionsSinceSnapshot = 0;

        ListIterator<CanvasAction> actionsIterator = this.getActionsEndIterator();

        while(actionsIterator.hasPrevious()) {
            CanvasAction paintingActionBuffer = actionsIterator.previous();

            if (paintingActionBuffer.getStartTime() < lastSnapshot.timestamp) {
                break;
            }

            if (paintingActionBuffer.getStartTime() > authoritativeTimestamp) {
                continue;
            }

            actionsSinceSnapshot += paintingActionBuffer.countActions();
        }

        return actionsSinceSnapshot >= MAX_SUB_ACTIONS_BEFORE_SNAPSHOT;
    }

    /**
     * Make a snapshot
     *
     * Used only once on client when easel opened
     * (because server snapshots sync from the start)
     */
    private void makeSnapshot() {
        assert this.getCanvasData() != null;
        if (this.canvasHolder.level().isClientSide()) {
            this.insertSnapshot(CanvasSnapshot.createWeakSnapshot(this.getCanvasData().getColorData(), System.currentTimeMillis()));
        } else if (this.snapshots.isEmpty()) {
            this.insertSnapshot(CanvasSnapshot.createServerSnapshot(this.getCanvasData().getColorData(), System.currentTimeMillis()));
        } else {
            // Restore painting to state at which it could not be changed
            // (as processing window defines timeout for new actions, we're
            // sure that no new actions will be added to buffer before that timestamp)
            long authoritativeTimestamp = System.currentTimeMillis() - PROCESSING_WINDOW;
            this.recollectPaintingData(authoritativeTimestamp);
            this.insertSnapshot(CanvasSnapshot.createServerSnapshot(this.getCanvasData().getColorData(), authoritativeTimestamp));
            // Get back to the current state
            this.recollectPaintingData();
        }
    }

    /**
     * Removes all older snapshots and history
     */
    private void cleanupSnapshotHistory() {
        int maxSize = SNAPSHOT_HISTORY_SIZE;

        if (this.canvasHolder.level().isClientSide) {
            maxSize = CLIENT_SNAPSHOT_HISTORY_SIZE;
        }

        if (this.snapshots.size() > maxSize) {
            int i = 0;
            ListIterator<CanvasSnapshot> canvasSnapshotIterator = this.getSnapshotsEndIterator();

            while(canvasSnapshotIterator.hasPrevious()) {
                canvasSnapshotIterator.previous();

                if (i++ > maxSize - 1) {
                    canvasSnapshotIterator.remove();
                }
            }
        }
    }

    /**
     * Removes all older actions
     */
    private void cleanupActionHistory() {
        int maxSize = ACTION_HISTORY_SIZE;

        if (this.actions.size() > maxSize) {
            int i = 0;
            ListIterator<CanvasAction> canvasActionIterator = this.getActionsEndIterator();

            while(canvasActionIterator.hasPrevious()) {
                canvasActionIterator.previous();

                if (i++ > maxSize - 1) {
                    canvasActionIterator.remove();
                }
            }
        }
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
    public void performHistorySyncClient(boolean forceCommit) {
        final ArrayDeque<CanvasAction> unsentActions = new ArrayDeque<>();
        ListIterator<CanvasAction> actionsIterator = this.getActionsEndIterator();

        while(actionsIterator.hasPrevious()) {
            CanvasAction paintingActionBuffer = actionsIterator.previous();

            if (!paintingActionBuffer.isCommitted()) {
                if (forceCommit || paintingActionBuffer.shouldCommit()) {
                    paintingActionBuffer.commit();

                    this.onStateChanged();
                } else {
                    continue;
                }
            }

            // Cannot stop here: actions synced from other players interleave with ours
            if (paintingActionBuffer.isSent()) {
                continue;
            }

            // Walking backwards, but the server merges by start time, so keep it chronological
            unsentActions.addFirst(paintingActionBuffer);
        }

        while (!unsentActions.isEmpty()) {
            final ArrayDeque<CanvasAction> batch = new ArrayDeque<>();
            int batchSize = 0;

            while (!unsentActions.isEmpty()) {
                final int actionSize = CanvasAction.getPacketSize(unsentActions.peek());

                // Always send at least one action, however big it turns out to be
                if (!batch.isEmpty() && batchSize + actionSize > CCanvasActionPacket.MAX_PAYLOAD_SIZE) {
                    break;
                }

                batch.add(unsentActions.poll());
                batchSize += actionSize;
            }

            ZetterNetwork.simpleChannel.sendToServer(new CCanvasActionPacket(this.canvasHolder.getId(), batch));

            for (CanvasAction sentAction : batch) {
                sentAction.setSent();
            }
        }
    }

    /**
     * Send sync packet to player: latest snapshot and new
     * history created since last sync
     */
    public void performHistorySyncServer() {
        for (Player player : this.players) {
            this.performHistorySyncForServerPlayer(player);
        }
    }

    /**
     * Send player a history update packet,
     * including history actions and latest
     * verified snapshot
     *
     * @todo: [MED]: First check should be redundant, it SHOULD never happen.
     * But it still happen sometimes, for example when easel is removed in painting process
     *
     * @param player
     */
    public void performHistorySyncForServerPlayer(Player player) {
        if (this.getCanvasCode() == null) {
            Zetter.LOG.error("Trying to perform sync with unavailable canvas code");
            return;
        }

        ArrayList<CanvasAction> unsyncedActions = this.getUnsyncedActionsForPlayer(player);
        boolean hasUnsyncedActions = unsyncedActions != null && !unsyncedActions.isEmpty();

        CanvasSnapshot unsyncedSnapshot = this.getFirstUnsyncedSnapshotForPlayer(player);
        boolean hasUnsyncedSnapshot = unsyncedSnapshot != null;

        // Nothing to sync
        if (!hasUnsyncedActions && !hasUnsyncedSnapshot) {
            return;
        }

        /* When we are sending the last snapshot/actions and have no more actions/snapshots to sync, we consider state
         * Sync; this does not mean that we are fully synchronized, but we can use previous snapshot as an authoritative
         * state
         * If we have unsynchronized actions/snapshots, we have last actions/snapshots, ignore errors */
        boolean actionsSync = !hasUnsyncedActions || unsyncedActions.get(unsyncedActions.size() - 1).id == this.getLastAction().id;
        boolean snapshotsSync = !hasUnsyncedSnapshot || unsyncedSnapshot.id == this.getLastSnapshot().id;

        SEaselStateSyncPacket syncMessage = new SEaselStateSyncPacket(
                this.canvasHolder.getId(), this.getCanvasCode(), actionsSync && snapshotsSync, unsyncedSnapshot, unsyncedActions
        );

        ZetterNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), syncMessage);

        if (hasUnsyncedActions) {
            this.playerLastSyncedAction.put(player.getUUID(), unsyncedActions.get(unsyncedActions.size() - 1).id);
        }

        if (hasUnsyncedSnapshot) {
            this.playerLastSyncedSnapshot.put(player.getUUID(), unsyncedSnapshot.id);
        }

        if (Zetter.DEBUG_MODE && Zetter.DEBUG_SERVER) {
            Zetter.LOG.debug("Sent history to player " + player.getUUID());
        }
    }

    /**
     * Get list of actions in history that was not synced with the
     * player since the last sync, to keep history consistent between
     * players
     *
     * Amount of action is limited by the packet SEaselStateSyncPacket
     * @param player
     * @return
     */
    private @Nullable ArrayList<CanvasAction> getUnsyncedActionsForPlayer(Player player) {
        // Sync whole history if we haven't synced any actions before
        if (!this.playerLastSyncedAction.containsKey(player.getUUID())) {
            return this.actions;
        }

        final int lastSyncedActionId = this.playerLastSyncedAction.get(player.getUUID());
        final ListIterator<CanvasAction> actionBufferIterator = this.getActionsEndIterator();

        ArrayList<CanvasAction> unsyncedActions = new ArrayList<>();

        while(actionBufferIterator.hasPrevious()) {
            CanvasAction action = actionBufferIterator.previous();
            // Because we're using reverse iterator, we add to the front
            if (action.id == lastSyncedActionId) {
                while(actionBufferIterator.hasNext() && unsyncedActions.size() < SEaselStateSyncPacket.MAX_ACTIONS) {
                    action = actionBufferIterator.next();
                    unsyncedActions.add(action);
                }

                break;
            }
        }

        if (unsyncedActions.size() == 1 && unsyncedActions.get(0).id == lastSyncedActionId) {
            return null;
        }

        return unsyncedActions;
    }

    /**
     * Get list of actions in history that was not synced with the
     * player since the last sync, to keep history consistent between
     * players
     *
     * @todo: [HIGH] It's expensive to iterate from the beginning
     *
     * @param player
     * @return
     */
    private @Nullable CanvasSnapshot getFirstUnsyncedSnapshotForPlayer(Player player) {
        if (this.snapshots.isEmpty()) {
            return null;
        }

        // Sync whole history if we haven't synced any actions before
        if (!this.playerLastSyncedSnapshot.containsKey(player.getUUID())) {
            return this.snapshots.get(0);
        }

        final int lastSyncedSnapshotId = this.playerLastSyncedSnapshot.get(player.getUUID());
        final ListIterator<CanvasSnapshot> snapshotIterator = this.getSnapshotsEndIterator();

        @Nullable CanvasSnapshot nextSnapshot = null;

        while (snapshotIterator.hasPrevious()) {
            final CanvasSnapshot snapshot = snapshotIterator.previous();

            // Walking back from the newest, so the one we passed is the one to send
            if (snapshot.id == lastSyncedSnapshotId) {
                return nextSnapshot;
            }

            nextSnapshot = snapshot;
        }

        // The one we synced last was cleaned up since, catch the player up from the oldest
        return this.snapshots.get(0);
    }

    /**
     * Called from network - process player's work
     * And drop canceled actions on server if needed
     *
     * @todo: [MED] Then rewind and make sure that playerLastSyncedAction is not after the first inserted action
     * @todo: [LOW] It is possible that this and processHistorySyncClient could be unified, because they're doing essentially the same thing
     *
     * Server-only
     * @param newActions
     */
    public void processActionServer(Queue<CanvasAction> newActions) {
        if (this.canvasHolder.getCanvasStack().isEmpty()) {
            Zetter.LOG.warn("Got action buffer but no canvas found on easel");
            return;
        }

        if (newActions.isEmpty()) {
            return;
        }

        if (!this.isCanvasInitialized()) {
            this.initializeCanvas(newActions.peek().getStartTime());
        }

        // We don't want to process actions that are too old
        final long processingAfterTimestamp = System.currentTimeMillis() - PROCESSING_WINDOW;
        this.wipeCanceledActionsAndDiscardSnapshots();

        for (CanvasAction newAction : newActions) {
            if (newAction.getStartTime() < processingAfterTimestamp) {
                Zetter.LOG.warn("Got action that is too old, ignoring");
                continue;
            }

            // Re-applying an action we already have would paint the stroke twice
            if (this.findAction(newAction.id) != null) {
                Zetter.LOG.warn("Got already synced action, ignoring");
                continue;
            }

            this.insertAction(newAction);

            if (newAction.isCanceled()) {
                this.historyDirty = true;
            }
        }

        this.unfreeze();

        this.cleanupActionHistory();
        this.recollectPaintingData();
        this.onStateChanged();
        this.markDesync();

        if (Zetter.DEBUG_MODE && Zetter.DEBUG_SERVER) {
            Zetter.LOG.debug("Processed actions sync from client");
        }
    }

    /**
     * Apply action from history or network - don't save it
     * @param action
     * @param doDamageClient apply damage to palette on client when applying action, server determines it by "sync" state
     */
    public void applyAction(CanvasAction action, boolean doDamageClient) {
        final boolean client = this.canvasHolder.level().isClientSide();

        final Player actingPlayer = this.players.stream()
                .filter((player) -> player.getUUID().equals(action.getAuthorUUID()))
                .findFirst()
                .orElse(null);

        final boolean chargePalette = actingPlayer != null
                && this.canvasHolder.getPaletteStack(actingPlayer) != null
                && (client ? doDamageClient : !action.isSync() && !actingPlayer.isCreative());

        action.getSubActionStream().forEach((CanvasAction.CanvasSubAction subAction) -> {
            // Apply subAction directly
            int damage = action.tool.getTool().apply(
                    this.getCanvasData(),
                    action.parameters,
                    action.color,
                    subAction.posX,
                    subAction.posY
            );

            if (chargePalette) {
                this.canvasHolder.damagePalette(actingPlayer, damage);
            }
        });

        if (!client) {
            action.setSync();
        }

        this.unfreeze();
    }

    /**
     * Add all newest pixels to the canvas when syncing to keep recent player's changes
     * Additionally writes state to recover history
     *
     * @todo: [LOW] It is possible that this and processActionServer could be unified, because they're doing essentially the same thing
     *
     * Client-only
     * @param canvasCode
     * @param snapshot
     * @param actions
     */
    public void processHistorySyncClient(String canvasCode, boolean sync, @Nullable CanvasSnapshot snapshot, @Nullable ArrayList<CanvasAction> actions) {
        if (!canvasCode.equals(this.getCanvasCode())) {
            Zetter.LOG.error("Different canvas code in history sync packet, ignoring");
            return;
        }

        if (snapshot != null) {
            if (this.getLastSnapshot() == null) {
                this.insertSnapshot(snapshot);
            } else if (this.getLastSnapshot().id != snapshot.id) {
                if (this.snapshots.size() >= SNAPSHOT_HISTORY_SIZE) {
                    this.snapshots.remove(0);
                }

                this.insertSnapshot(snapshot);

                if (Zetter.DEBUG_MODE && Zetter.DEBUG_CLIENT) {
                    Zetter.LOG.debug("Processed server snapshot");
                }
            }
        }


        if (actions == null || actions.isEmpty()) {
            this.sync = sync;

            this.recollectPaintingData();

            if (Zetter.DEBUG_MODE && Zetter.DEBUG_CLIENT) {
                Zetter.LOG.debug("Processed actions sync from server");
            }

            return;
        }

        int addedActions = 0;

        for (CanvasAction unsyncedAction : actions) {
            // Server is authoritative, take its copy: the canceled flag may have changed
            if (this.findAndReplaceAction(unsyncedAction.id, unsyncedAction) != null) {
                unsyncedAction.setSync();

                continue;
            }

            this.insertAction(unsyncedAction);
            addedActions++;
        }

        this.unfreeze();

        if (addedActions > 0) {
            this.wipeCanceledActionsAndDiscardSnapshots();
            this.cleanupActionHistory();
        }

        this.sync = sync;

        this.recollectPaintingData();
        this.onStateChanged();

        if (Zetter.DEBUG_MODE && Zetter.DEBUG_CLIENT) {
            Zetter.LOG.debug("Processed actions sync from server");
        }
    }

    /**
     * Insert snapshot keeping the list sorted
     *
     * @param addedSnapshot
     */
    private void insertSnapshot(CanvasSnapshot addedSnapshot) {
        final ListIterator<CanvasSnapshot> canvasSnapshotIterator = this.getSnapshotsEndIterator();

        while(canvasSnapshotIterator.hasPrevious()) {
            CanvasSnapshot currentSnapshot = canvasSnapshotIterator.previous();

            // Sometimes server can send existing snapshot
            if (
                addedSnapshot.timestamp.equals(currentSnapshot.timestamp) &&
                addedSnapshot.id == currentSnapshot.id
            ) {
                Zetter.LOG.error("This snapshot already exists, ignoring");
                return;
            }

            // When iterated to the snapshot that was created before added one
            // Can just add a snapshot
            if (addedSnapshot.timestamp > currentSnapshot.timestamp) {
                canvasSnapshotIterator.next();
                canvasSnapshotIterator.add(addedSnapshot);
                this.cleanupSnapshotHistory();
                return;
            }
        }

        // If no snapshots exist, just add
        canvasSnapshotIterator.add(addedSnapshot);
    }

    /**
     * Tell other players who are tracking canvas that canvas
     * is no longer up to date (tracker will decide when to sync)
     */
    private void markDesync() {
        if (!this.canvasHolder.level().isClientSide()) {
            final List<UUID> playersReceivingActions = this.players.stream().map(Player::getUUID).toList();

            ((CanvasServerTracker) Helper.getLevelCanvasTracker(this.canvasHolder.level()))
                .markCanvasDesync(this.getCanvasCode(), playersReceivingActions);
        }
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

    private ListIterator<CanvasAction> getActionsEndIterator() {
        if (this.actions.isEmpty()) {
            return this.actions.listIterator();
        } else {
            return this.actions.listIterator(this.actions.size());
        }
    }

    private @Nullable CanvasAction getLastAction() {
        if (this.actions.isEmpty()) {
            return null;
        } else {
            return this.actions.get(this.actions.size() - 1);
        }
    }

    private ListIterator<CanvasSnapshot> getSnapshotsEndIterator() {
        if (this.snapshots.isEmpty()) {
            return this.snapshots.listIterator();
        } else {
            return this.snapshots.listIterator(this.snapshots.size());
        }
    }

    private @Nullable CanvasSnapshot getLastSnapshot() {
        if (this.snapshots.isEmpty()) {
            return null;
        } else {
            return this.snapshots.get(this.snapshots.size() - 1);
        }
    }

    private void logActions() {
        Zetter.LOG.debug("= Start actions =");

        this.actions.stream().forEach((action) -> {
            Zetter.LOG.debug(action.id + ": " + (action.isCommitted() ? 'M' : '_') + (action.isCanceled() ? 'C' : '_') + (action.isSent() ? 'S' : '_') + (action.isSync() ? 'Y' : '_'));
        });

        Zetter.LOG.debug("= End actions =");
    }
}
