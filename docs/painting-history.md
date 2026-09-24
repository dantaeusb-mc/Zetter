# Painting history and sync

How a stroke gets from a click on the easel to a saved canvas, and how undo works on
top of that. Lives in `CanvasState`, ticked by `CanvasHolderEntity#tick` on both sides.

## Two queues

The pixels are derived state: a **log of actions** replayed over periodic **snapshots**.

| | `actions` | `snapshots` |
|---|---|---|
| holds | `CanvasAction` — one stroke: tool, colour, parameters, points | `CanvasSnapshot` — a full copy of the pixels |
| ordered by | `startTime` | `timestamp` |
| trimmed to | 512 (`ACTION_HISTORY_SIZE`) | 10 server / 50 client |
| mutable | yes, the `canceled` flag | no |

An action groups consecutive points of one stroke. It stays open while the author,
tool, colour and parameters hold, and **commits** at 5 s old (`MAX_TIME`), 750 ms
idle (`MAX_INACTIVE_TIME`), or a full point buffer. Only committed actions travel.

### Points are samples, not the stroke

A point is recorded wherever the pointer happened to be reported, so consecutive
points can be any distance apart. Each one carries a **continuity** flag in its meta
byte: set when the pointer dragged here, clear when it started a stroke here. A tool
that draws a continuous line gets handed the point the stroke came from and fills the
gap itself: `Pencil` walks it with Bresenham, `Brush` paints the whole gap as one
capsule so that every pixel is weighed against the segment and touched once.

Two rules keep the client and a replay in step, and both are easy to break:

- A stroke carries on only from a point **the same action already holds**. An action
  never reaches back past a commit, so a stroke long enough to be split starts over
  at the boundary. Accept the seam or the carry-over point has to go in the header.
- The client **records before it applies**. Recording is what decides whether a point
  extends the open action or starts a new one, and replay asks the same question. Ask
  it in the other order and a point that lands first in a fresh action still gets
  joined to the old one on the drawing client, and nowhere else.

An action that is open and one that was committed then replayed therefore paint the
same pixels, which is what makes the point a *sample* rather than a brush stamp: the
gap filling is derived, never transmitted, so a fast stroke costs the same bytes as a
slow one.

## The contract

> A snapshot stamped **T** holds the canvas after every non-canceled action with
> `startTime < T` has been applied.

Two rules follow, and both were bugs before they were rules.

**One clock field decides everything.** Actions are ordered by `startTime`, so
`startTime` also decides which snapshot to start from and which actions are missing
from it. Compare a `commitTime` against a snapshot `timestamp` instead, and a stroke
straddling the checkpoint counts as both applied and not — it gets painted twice.

**Flipping `canceled` invalidates every later snapshot**, in either direction, since
whether an action was applied determines their pixels. Undo and redo both call
`discardSnapshotsAfter`.

## Rebuilding

`recollectPaintingData(timestamp)` restores the canvas as it was at a moment:

```
actions:    A1      A2        A3       A4        A5
            |       |         |        |         |
    --------+-------+---------+--------+---------+------> startTime
                         ^                            ^
                      snapshot S                  timestamp

    canvas = S.colors  then replay  A3, A4       (A5 is past the boundary)
                                    ^  ^
                                    |  skipped if canceled
                                    started at or after S
```

Pick the newest snapshot older than `timestamp`; if anything is canceled walk further
down, to one older than the first canceled action. Replay forward, skipping what the
snapshot already holds and what is canceled. Stop at `timestamp`.

The no-argument form passes `Long.MAX_VALUE` — current state, stroke in progress
included.

## The round trip

```
CLIENT                                        SERVER

  useTool()
    paint locally, update texture
    recordAction() ──────► actions

  tick, every 1 s
    performHistorySyncClient()
      commit what is due
      ──── CCanvasActionPacket ─────────────►  processActionServer()
                (chronological)                  drop duplicates and stale
                                                 insertAction()  (by startTime)
                                                 recollectPaintingData()
                                                   └─ tools write pixels
                                                      → setDirty → world save

                                               tick, every 5 s
                                                 updateSnapshots()

                                               tick, every 1 s
  processHistorySyncClient()  ◄──── SEaselStateSyncPacket ──┘
    take the server's copy of what we have      (snapshot + actions since this
    insert the rest in startTime order           player's last sync)
    recollectPaintingData()
```

The client paints immediately and reconciles afterwards; on a conflict the server's
copy of an action replaces the client's. Undo and redo travel separately, as
`CCanvasHistoryActionPacket` / `SCanvasHistoryActionPacket` carrying an action id and
a flag, since the action itself is already on both sides.

Actions only go to players **using** the easel. Everyone **tracking** the canvas —
anyone whose client asked for it in order to render it — instead gets the whole canvas
from `CanvasServerTracker`, once a second at most and only when it changed. Painters
are in both sets, so `markCanvasDesync` carries the list of players already following
along action by action, and the tracker skips them.

Serverbound payloads are capped at 32767 bytes and Forge only splits clientbound ones,
so `performHistorySyncClient` measures each action and sends as many packets as it
takes to stay under `CCanvasActionPacket.MAX_PAYLOAD_SIZE`.

## Snapshots

Cut on the server once 20 settled actions have accumulated (`MAX_ACTIONS_BEFORE_SNAPSHOT`),
checked every 5 s, and never while something is undone — the next redo would discard it.

`makeSnapshot` stamps the checkpoint at `now - PROCESSING_WINDOW`, not now:

```
PROCESSING_WINDOW = SYNC_INTERVAL + MAX_LATENCY + CanvasAction.MAX_TIME
                    1000 ms         500 ms        5000 ms        = 6500 ms
```

That is the age past which no action can still arrive belonging *before* the
checkpoint: a stroke lasts at most 5 s, is sent within 1 s of committing, and arrives
within 0.5 s. The same arithmetic keeps a checkpoint from ever landing mid-stroke.

The client takes one "weak" snapshot of the canvas when the screen opens, so it can
rebuild before the first server snapshot arrives.

## Undo and redo

Neither removes anything. `applyHistoryTraversing` flips `canceled` — undo from the
newest back to the target, redo from the oldest forward to it — discards the
invalidated snapshots, and rebuilds. `canUndo`/`canRedo` refuse when no snapshot
survives that far back; that is the history horizon.

Undo is **global, not per-player**: the target is the last non-canceled action by
anyone, so at a shared easel either painter can undo the other's stroke.

Painting after an undo makes history non-linear, and
`wipeCanceledActionsAndDiscardSnapshots` deletes the canceled tail and its snapshots
for good.

## Lifecycle

The state starts frozen and empty. `addPlayer` unfreezes it and takes the first
snapshot; `removePlayer` flushes the action buffer, then freezes the client copy.
After 30 minutes with nobody painting (`FREEZE_TIMEOUT`) the history is dropped. None
of this touches the pixels — those live in the world's canvas storage and are saved
whenever a tool writes one.

## Known limits

- **Tools are not idempotent.** `BlendingPipe` reads the pixel underneath, so a brush
  stroke or any intensity below 1 differs if applied twice or out of order. Replay is
  exactly-once and ordered, so this is a constraint rather than a bug — but a lost or
  reordered action shows up as silent colour drift. A checksum per snapshot would make
  it a log line instead.
- **Undo reaches only as far as the oldest snapshot** — nominally 200 actions, matching
  the log. If it greys out sooner, `SNAPSHOT_HISTORY_SIZE` × `MAX_ACTIONS_BEFORE_SNAPSHOT`
  is the pairing to loosen.
